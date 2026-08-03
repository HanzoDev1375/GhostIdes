package ir.hanzodev1375.ghostide.ide.api;

import java.io.File;

/**
 * Describes the file a language server connection is requested for.
 *
 * @param projectRoot absolute root directory of the open project
 * @param file absolute path of the file that triggered routing
 */
public record LspServerRequest(File projectRoot, File file) {

  public LspServerRequest {
    if (projectRoot == null) {
      throw new IllegalArgumentException("Project root must not be null");
    }
    if (file == null) {
      throw new IllegalArgumentException("File must not be null");
    }
  }

  public String extension() {
    String name = file.getName();
    int dot = name.lastIndexOf('.');
    if (dot < 0 || dot == name.length() - 1) {
      return "";
    }
    return name.substring(dot + 1).toLowerCase(java.util.Locale.ROOT);
  }
}
