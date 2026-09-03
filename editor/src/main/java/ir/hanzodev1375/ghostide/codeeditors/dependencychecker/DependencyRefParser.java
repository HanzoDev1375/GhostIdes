package ir.hanzodev1375.ghostide.codeeditors.dependencychecker;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts Maven dependency coordinates (group:name:version) from editor lines, in either Gradle or
 * TOML (libs.versions.toml) syntax, and maps them to character offsets.
 */
public final class DependencyRefParser {

  private DependencyRefParser() {}

  // implementation 'com.google.android.material:material:1.14.0'
  // implementation("androidx.core:core-ktx:1.6.0")
  private static final Pattern GRADLE =
      Pattern.compile("(['\"])([\\w.]+):([\\w.-]+):([^\\s'\"\\]]+)\\1", Pattern.CASE_INSENSITIVE);

  // implementation(libs.androidx.appcompat)
  // implementation libs.material
  // implementation platform(libs.androidx.compose.bom)
  private static final Pattern GRADLE_LIBS_REF = Pattern.compile("\\blibs\\.[A-Za-z0-9_.-]+");

  /** A library entry resolved from a Gradle version catalog (libs.versions.toml). */
  public record CatalogLibrary(String group, String name, String version) {}

  /** Resolves a version catalog accessor alias (the part after {@code libs.}). */
  public interface LibsResolver {
    CatalogLibrary resolve(String alias);
  }

  /** A {@code libs.<alias>} accessor together with its character span in the line. */
  public record LibsRef(String alias, int start, int end) {}

  /** Resolves a `[versions]` table key (used for TOML {@code version.ref} indirection). */
  public interface VersionResolver {
    ResolvedVersion resolve(String key);
  }

  /** Where a resolved version literal actually lives (possibly a different line). */
  public record ResolvedVersion(int line, int start, int end, String value) {}

  /** Finds all Gradle dependencies in a line (used for highlighting). */
  public static List<DependencyMatch> findAllInGradle(int line, String lineText) {
    List<DependencyMatch> out = new ArrayList<>();
    if (lineText == null) return out;
    Matcher m = GRADLE.matcher(lineText);
    while (m.find()) {
      out.add(
          new DependencyMatch(
              m.group(2), m.group(3), m.group(4), line, m.start(4), m.end(4), m.start(2),
              m.end(4)));
    }
    return out;
  }

  /**
   * Finds a Gradle dependency under the cursor column (if any). Returns null when nothing usable is
   * under the cursor.
   */
  public static DependencyMatch findInGradle(int line, String lineText, int cursorColumn) {
    if (lineText == null) return null;
    for (DependencyMatch d : findAllInGradle(line, lineText)) {
      if (cursorColumn >= d.fullStart() && cursorColumn <= d.versionEnd()) {
        return d;
      }
    }
    return null;
  }

  /** Collects every {@code libs.<alias>} accessor found in a line. */
  public static List<LibsRef> findAllLibsRefs(String lineText) {
    List<LibsRef> out = new ArrayList<>();
    if (lineText == null) return out;
    Matcher m = GRADLE_LIBS_REF.matcher(lineText);
    while (m.find()) {
      out.add(new LibsRef(m.group().substring("libs.".length()), m.start(), m.end()));
    }
    return out;
  }

  /**
   * Finds a Gradle version catalog reference under the cursor and resolves it into a dependency via
   * {@code resolver}.
   *
   * <p>The version token itself lives in {@code libs.versions.toml} and not in this file, so {@code
   * versionLine}/{@code versionStart}/{@code versionEnd} are all {@code -1}. The {@code
   * fullStart}/{@code fullEnd} span covers the whole {@code libs.<alias>} accessor so the reference
   * can be highlighted. Returns null when the caret is elsewhere or the alias can't be resolved.
   */
  public static DependencyMatch findLibsRefInGradle(
      int line, String lineText, int cursorColumn, LibsResolver resolver) {
    if (lineText == null) return null;
    for (LibsRef ref : findAllLibsRefs(lineText)) {
      if (cursorColumn >= ref.start() && cursorColumn <= ref.end()) {
        return toCatalogMatch(line, ref, resolver);
      }
    }
    return null;
  }

  /** Resolves every {@code libs.<alias>} accessor in a line and appends the matches to {@code into}. */
  public static void findAllLibsRefsInGradle(
      int line, String lineText, LibsResolver resolver, List<DependencyMatch> into) {
    if (lineText == null || into == null || resolver == null) return;
    for (LibsRef ref : findAllLibsRefs(lineText)) {
      DependencyMatch d = toCatalogMatch(line, ref, resolver);
      if (d != null) into.add(d);
    }
  }

  private static DependencyMatch toCatalogMatch(int line, LibsRef ref, LibsResolver resolver) {
    CatalogLibrary lib = resolver.resolve(ref.alias());
    if (lib == null || lib.group() == null || lib.name() == null || lib.version() == null) {
      return null;
    }
    return new DependencyMatch(
        lib.group(), lib.name(), lib.version(), -1, -1, -1, ref.start(), ref.end());
  }

  /**
   * Finds the single TOML library entry in a line (used for highlighting). Group/name must be
   * explicit so an online lookup is possible. The version may be an inline literal:
   *
   * <pre>
   *  material = { group = "com.google.android.material", name = "material", version = "1.14.0" }
   * </pre>
   *
   * or an indirection into the {@code [versions]} table, resolved via {@code resolver}:
   *
   * <pre>
   *  material = { group = "com.google.android.material", name = "material", version.ref = "material" }
   * </pre>
   *
   * Returns null when group/name are missing, or the version can't be determined (inline literal
   * absent and either no {@code version.ref} or the ref key doesn't resolve).
   */
  public static DependencyMatch findInToml(int line, String lineText, VersionResolver resolver) {
    if (lineText == null) return null;

    String group = extractTomlValue(lineText, "group");
    String name = extractTomlValue(lineText, "name");
    if (group == null || name == null) return null;

    int groupIdx = lineText.indexOf(group);
    if (groupIdx < 0) groupIdx = 0;
    int fullEnd = trimmedLength(lineText);

    // Inline literal version: version = "1.14.0"
    String version = extractTomlValue(lineText, "version");
    if (version != null) {
      int versionQuoteStart = findTomlValueStart(lineText, "version", version);
      if (versionQuoteStart < 0) return null;
      int vStart = versionQuoteStart;
      int vEnd = vStart + version.length();
      return new DependencyMatch(group, name, version, line, vStart, vEnd, groupIdx, fullEnd);
    }

    // Indirection: version.ref = "material", resolved against the [versions] table.
    String refKey = extractTomlValue(lineText, "version.ref");
    if (refKey != null && resolver != null) {
      ResolvedVersion resolved = resolver.resolve(refKey);
      if (resolved != null) {
        return new DependencyMatch(
            group,
            name,
            resolved.value(),
            resolved.line(),
            resolved.start(),
            resolved.end(),
            groupIdx,
            fullEnd);
      }
    }

    return null;
  }

  /**
   * Finds a TOML library entry under the cursor column (tapping anywhere on the dependency text,
   * inline literal or {@code version.ref} alike). Returns null otherwise.
   */
  public static DependencyMatch findInToml(
      int line, String lineText, int cursorColumn, VersionResolver resolver) {
    DependencyMatch d = findInToml(line, lineText, resolver);
    if (d == null) return null;
    if (cursorColumn >= d.fullStart() && cursorColumn <= d.fullEnd()) {
      return d;
    }
    return null;
  }

  /**
   * Parses a simple {@code key = "value"} assignment, as used for entries inside a TOML
   * {@code [versions]} table. Returns null when the line isn't an assignment to exactly {@code
   * key} (guards against prefixes, e.g. a lookup for "material" won't match "material-compat").
   */
  public static ResolvedVersion resolveSimpleValue(int lineNumber, String lineText, String key) {
    if (lineText == null) return null;
    String trimmed = lineText.trim();
    if (!trimmed.startsWith(key)) return null;

    int after = key.length();
    if (after < trimmed.length()) {
      char c = trimmed.charAt(after);
      if (Character.isLetterOrDigit(c) || c == '.' || c == '_' || c == '-') return null;
    }

    int eq = trimmed.indexOf('=', after);
    if (eq < 0) return null;
    String rest = trimmed.substring(eq + 1).trim();
    if (!rest.startsWith("\"")) return null;
    int end = rest.indexOf('"', 1);
    if (end < 0) return null;
    String value = rest.substring(1, end).trim();
    if (value.isEmpty()) return null;

    int quoteStart = lineText.indexOf('"' + value + '"');
    if (quoteStart < 0) return null;
    return new ResolvedVersion(lineNumber, quoteStart + 1, quoteStart + 1 + value.length(), value);
  }

  private static String extractTomlValue(String line, String key) {
    int searchFrom = 0;
    while (true) {
      int idx = line.indexOf(key, searchFrom);
      if (idx < 0) return null;
      // The next character must not extend the key (rejects "version" matching "version.ref").
      int after = idx + key.length();
      if (after < line.length()) {
        char c = line.charAt(after);
        if (Character.isLetterOrDigit(c) || c == '.' || c == '_' || c == '-') {
          searchFrom = after;
          continue;
        }
      }
      int eq = line.indexOf('=', after);
      if (eq < 0) return null;
      String rest = line.substring(eq + 1).trim();
      if (!rest.startsWith("\"")) return null;
      int end = rest.indexOf('"', 1);
      if (end < 0) return null;
      String value = rest.substring(1, end).trim();
      return value.isEmpty() ? null : value;
    }
  }

  private static int findTomlValueStart(String line, String key, String value) {
    int idx = 0;
    while (true) {
      int k = line.indexOf(key, idx);
      if (k < 0) return -1;
      int v = line.indexOf('"' + value + '"', k);
      if (v >= 0) return v + 1;
      idx = k + key.length();
    }
  }

  private static int trimmedLength(String line) {
    int end = line.length();
    while (end > 0 && Character.isWhitespace(line.charAt(end - 1))) end--;
    return end;
  }
}
