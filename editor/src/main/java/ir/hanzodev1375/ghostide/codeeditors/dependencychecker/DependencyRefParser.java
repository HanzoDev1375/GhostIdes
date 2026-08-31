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

  /** Finds all Gradle dependencies in a line (used for highlighting). */
  public static List<DependencyMatch> findAllInGradle(String lineText) {
    List<DependencyMatch> out = new ArrayList<>();
    if (lineText == null) return out;
    Matcher m = GRADLE.matcher(lineText);
    while (m.find()) {
      out.add(
          new DependencyMatch(
              m.group(2), m.group(3), m.group(4), m.start(4), m.end(4), m.start(2), m.end(4)));
    }
    return out;
  }

  /**
   * Finds a Gradle dependency under the cursor column (if any). Returns null when nothing usable is
   * under the cursor.
   */
  public static DependencyMatch findInGradle(String lineText, int cursorColumn) {
    if (lineText == null) return null;
    for (DependencyMatch d : findAllInGradle(lineText)) {
      if (cursorColumn >= d.fullStart() && cursorColumn <= d.versionEnd()) {
        return d;
      }
    }
    return null;
  }

  /**
   * Finds the single TOML library entry in a line whose version is an inline literal (used for
   * highlighting). Group/name must be explicit so an online lookup is possible.
   *
   * <pre>
   *  material = { group = "com.google.android.material", name = "material", version = "1.14.0" }
   * </pre>
   */
  public static DependencyMatch findInToml(String lineText) {
    if (lineText == null) return null;

    String group = extractTomlValue(lineText, "group");
    String name = extractTomlValue(lineText, "name");
    String version = extractTomlValue(lineText, "version");

    if (group == null || name == null || version == null) return null;

    int versionQuoteStart = findTomlValueStart(lineText, "version", version);
    if (versionQuoteStart < 0) return null;
    // version token (without surrounding quotes)
    int vStart = versionQuoteStart;
    int vEnd = vStart + version.length();

    int groupIdx = lineText.indexOf(group);
    if (groupIdx < 0) groupIdx = 0;

    return new DependencyMatch(group, name, version, vStart, vEnd, groupIdx, vEnd);
  }

  /**
   * Finds a TOML library entry under the cursor column (only when the caret is on the inline
   * version literal). Returns null otherwise.
   */
  public static DependencyMatch findInToml(String lineText, int cursorColumn) {
    DependencyMatch d = findInToml(lineText);
    if (d == null) return null;
    if (cursorColumn >= d.versionStart() - 1 && cursorColumn <= d.versionEnd()) {
      return d;
    }
    return null;
  }

  private static String extractTomlValue(String line, String key) {
    int searchFrom = 0;
    while (true) {
      int idx = line.indexOf(key, searchFrom);
      if (idx < 0) return null;
      // The next character must not extend the key (rejects keys like "version.ref").
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
}
