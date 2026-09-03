package ir.hanzodev1375.ghostide.codeeditors.dependencychecker;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.moandjiezana.toml.Toml;
import io.github.rosemoe.sora.event.SelectionChangeEvent;
import io.github.rosemoe.sora.widget.CodeEditor;
import ir.hanzodev1375.ghostide.codeeditors.langs.gradle.GradleLanguage;

/**
 * Checks Gradle build files for newer versions of declared dependencies.
 *
 * <p>Besides inline coordinates ({@code implementation 'g:n:v'}), dependencies declared with the
 * version catalog DSL ({@code implementation(libs.androidx.appcompat)}) are also checked: the alias
 * is resolved against the project's {@code gradle/libs.versions.toml} so the coordinates and the
 * currently declared version are known for the online lookup.
 */
public final class GradleDependencyCheckerIde extends DependencyCheckerIde
    implements DependencyRefParser.LibsResolver {

  private String catalogForFile;
  private String catalogPath;
  private long catalogLastModified = -1;
  private long catalogLength = -1;
  private final Map<String, String> catalogVersions = new HashMap<>();
  private final Map<String, DependencyRefParser.CatalogLibrary> catalogLibraries = new HashMap<>();
  // toml library key -> referenced [versions] key, only when the library uses version.ref.
  private final Map<String, String> catalogRefs = new HashMap<>();

  public GradleDependencyCheckerIde(CodeEditor editor) {
    super(editor);
  }

  @Override
  protected boolean isMyLanguage() {
    return editor.getEditorLanguage() instanceof GradleLanguage;
  }

  @Override
  protected DependencyMatch findUnderCursor(SelectionChangeEvent event) {
    int line = event.getLeft().getLine();
    int column = event.getLeft().getColumn();
    String lineText = editor.getText().getLineString(line);

    DependencyMatch literal = DependencyRefParser.findInGradle(line, lineText, column);
    if (literal != null) return literal;
    return DependencyRefParser.findLibsRefInGradle(line, lineText, column, this);
  }

  @Override
  protected void collectLineHighlights(int line, String lineText, List<DependencyMatch> into) {
    into.addAll(DependencyRefParser.findAllInGradle(line, lineText));
    DependencyRefParser.findAllLibsRefsInGradle(line, lineText, this, into);
  }

  /**
   * Resolves a {@code libs.<alias>} accessor into the {@code [libraries]} table of the nearest
   * {@code libs.versions.toml}. The alias uses dots in Gradle but dashes in the TOML keys
   * ({@code libs.androidx.appcompat} → {@code androidx-appcompat}); both the dotted form and a
   * kebab-case fallback are tried. Returns null when no catalog/inline version can be determined.
   */
  @Override
  public DependencyRefParser.CatalogLibrary resolve(String alias) {
    if (!ensureCatalogLoaded()) return null;

    String key = alias.replace('.', '-');
    DependencyRefParser.CatalogLibrary lib = catalogLibraries.get(key);
    if (lib == null) lib = catalogLibraries.get(alias);
    if (lib == null) {
      String kebab = toKebabCase(alias);
      if (!kebab.equals(key)) lib = catalogLibraries.get(kebab);
    }
    if (lib == null || lib.version() == null) return null;
    return lib;
  }

  /** Returns true when the catalog has been parsed and its cache is still valid for this file. */
  private boolean ensureCatalogLoaded() {
    String file = getFilePath();
    if (catalogForFile != null && catalogForFile.equals(file) && catalogPath != null) {
      File f = new File(catalogPath);
      if (f.isFile() && f.lastModified() == catalogLastModified && f.length() == catalogLength) {
        return true;
      }
    }
    String path = findCatalogPath();
    catalogForFile = file;
    catalogPath = path;
    catalogVersions.clear();
    catalogLibraries.clear();
    catalogRefs.clear();
    return path != null && parseCatalog(path);
  }

  /** Walks up from the opened file looking for the project's {@code libs.versions.toml}. */
  private String findCatalogPath() {
    String filePath = getFilePath();
    if (filePath == null) return null;
    File dir = new File(filePath).getParentFile();
    while (dir != null) {
      File standard = new File(dir, "gradle/libs.versions.toml");
      if (standard.isFile()) return standard.getAbsolutePath();
      File direct = new File(dir, "libs.versions.toml");
      if (direct.isFile()) return direct.getAbsolutePath();
      dir = dir.getParentFile();
    }
    return null;
  }

  /**
   * Parses the {@code [versions]} and {@code [libraries]} tables of a version catalog using the
   * bundled toml4j parser. Inline entries declaring a {@code version.ref} are resolved through the
   * versions table.
   */
  private boolean parseCatalog(String path) {
    File f = new File(path);
    try {
      Toml toml = new Toml().read(f);
      parseVersionsSection(toml.toMap().get("versions"));
      parseLibrariesSection(toml.toMap().get("libraries"));
      catalogLastModified = f.lastModified();
      catalogLength = f.length();
      return true;
    } catch (RuntimeException e) {
      catalogLastModified = -1;
      catalogLength = -1;
      return false;
    }
  }

  private void parseVersionsSection(Object raw) {
    if (!(raw instanceof Map)) return;
    for (Map.Entry<?, ?> entry : ((Map<?, ?>) raw).entrySet()) {
      Object value = entry.getValue();
      if (value instanceof String) {
        catalogVersions.put(String.valueOf(entry.getKey()), (String) value);
      }
    }
  }

  private void parseLibrariesSection(Object raw) {
    if (!(raw instanceof Map)) return;
    for (Map.Entry<?, ?> entry : ((Map<?, ?>) raw).entrySet()) {
      Object libRaw = entry.getValue();
      if (!(libRaw instanceof Map)) continue;
      Map<?, ?> lib = (Map<?, ?>) libRaw;
      Object group = lib.get("group");
      Object name = lib.get("name");
      if (!(group instanceof String) || !(name instanceof String)) continue;
      String version = null;
      Object direct = lib.get("version");
      if (direct instanceof String) {
        version = (String) direct;
      } else if (lib.get("version.ref") instanceof String) {
        String refName = (String) lib.get("version.ref");
        version = catalogVersions.get(refName);
        if (version != null) catalogRefs.put(String.valueOf(entry.getKey()), refName);
      }
      if (version != null) {
        catalogLibraries.put(
            String.valueOf(entry.getKey()),
            new DependencyRefParser.CatalogLibrary((String) group, (String) name, version));
      }
    }
  }

  /**
   * Persists the new version into the version catalog file. When the library uses {@code
   * version.ref} the corresponding {@code [versions]} entry is updated; otherwise the inline {@code
   * version} of the library itself is replaced. Returns false when the target line could not be
   * found or the file could not be rewritten.
   */
  @Override
  protected boolean applyCatalogUpdate(DependencyMatch match, String newVersion) {
    if (!ensureCatalogLoaded() || catalogPath == null) return false;
    String key = findCatalogKey(match.group(), match.name(), match.version());
    if (key == null) return false;
    return editCatalogVersion(key, catalogRefs.get(key), match.version(), newVersion);
  }

  /** Locates the catalog library key matching the resolved coordinates and current version. */
  private String findCatalogKey(String group, String name, String version) {
    for (Map.Entry<String, DependencyRefParser.CatalogLibrary> entry :
        catalogLibraries.entrySet()) {
      DependencyRefParser.CatalogLibrary lib = entry.getValue();
      if (lib.group().equals(group) && lib.name().equals(name) && lib.version().equals(version)) {
        return entry.getKey();
      }
    }
    return null;
  }

  private boolean editCatalogVersion(
      String key, String refKey, String oldVersion, String newVersion) {
    File file = new File(catalogPath);
    StringBuilder out = new StringBuilder();
    boolean inVersions = false;
    boolean inLibraries = false;
    boolean replaced = false;

    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
      String raw;
      while ((raw = reader.readLine()) != null) {
        String line = raw;
        if (!replaced) {
          String trimmed = raw.trim();
          if (trimmed.startsWith("[")) {
            inVersions = trimmed.equalsIgnoreCase("[versions]");
            inLibraries = trimmed.equalsIgnoreCase("[libraries]");
          } else if (refKey != null && inVersions && isSimpleKeyLine(trimmed, refKey)) {
            String updated = replaceQuotedVersion(line, oldVersion, newVersion);
            if (updated != null) {
              line = updated;
              replaced = true;
            }
          } else if (refKey == null && inLibraries && isLibraryLine(trimmed, key)) {
            String updated = replaceQuotedVersion(line, oldVersion, newVersion);
            if (updated != null) {
              line = updated;
              replaced = true;
            }
          }
        }
        out.append(line).append('\n');
      }
    } catch (IOException e) {
      return false;
    }
    if (!replaced) return false;

    try (FileOutputStream fos = new FileOutputStream(file)) {
      fos.write(out.toString().getBytes("UTF-8"));
    } catch (IOException e) {
      return false;
    }

    // Refresh the in-memory catalog so follow-up checks see the new version.
    if (refKey != null) {
      catalogVersions.put(refKey, newVersion);
      for (Map.Entry<String, DependencyRefParser.CatalogLibrary> entry :
          catalogLibraries.entrySet()) {
        if (refKey.equals(catalogRefs.get(entry.getKey()))) {
          DependencyRefParser.CatalogLibrary lib = entry.getValue();
          catalogLibraries.put(
              entry.getKey(),
              new DependencyRefParser.CatalogLibrary(lib.group(), lib.name(), newVersion));
        }
      }
    } else {
      DependencyRefParser.CatalogLibrary lib = catalogLibraries.get(key);
      if (lib != null) {
        catalogLibraries.put(
            key, new DependencyRefParser.CatalogLibrary(lib.group(), lib.name(), newVersion));
      }
    }
    catalogLastModified = file.lastModified();
    catalogLength = file.length();
    return true;
  }

  /** Replaces the first occurrence of the quoted old version on the line; null when absent. */
  private static String replaceQuotedVersion(String line, String oldVersion, String newVersion) {
    String wrapper = "\"" + oldVersion + "\"";
    int idx = line.indexOf(wrapper);
    if (idx < 0) return null;
    return line.substring(0, idx) + "\"" + newVersion + "\"" + line.substring(idx + wrapper.length());
  }

  private static boolean isSimpleKeyLine(String trimmed, String key) {
    if (!trimmed.startsWith(key)) return false;
    int after = key.length();
    if (after < trimmed.length()) {
      char c = trimmed.charAt(after);
      if (Character.isLetterOrDigit(c) || c == '.' || c == '_' || c == '-') return false;
    }
    return trimmed.indexOf('=', after) >= 0;
  }

  private static boolean isLibraryLine(String trimmed, String key) {
    if (!trimmed.startsWith(key)) return false;
    int after = key.length();
    return after < trimmed.length() && trimmed.charAt(after) == '=';
  }

  private static String toKebabCase(String alias) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < alias.length(); i++) {
      char c = alias.charAt(i);
      if (Character.isUpperCase(c)) {
        sb.append('-').append(Character.toLowerCase(c));
      } else {
        sb.append(c);
      }
    }
    return sb.toString();
  }
}