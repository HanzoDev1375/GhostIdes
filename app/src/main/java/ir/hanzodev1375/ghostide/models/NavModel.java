package ir.hanzodev1375.ghostide.models;

import java.io.File;


public class NavModel {

  private String navName;
  private File navFile;
  
  public NavModel(String name, File file) {
    this.navName = name;
    this.navFile = file;
  }

  @Override
  public int hashCode() {
    int result = 18;
    result = 31 * result + (navName != null ? navName.hashCode() : 0);
    result = 31 * result + (navFile != null ? navFile.hashCode() : 0);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    NavModel nav = (NavModel) obj;
    return navFile.getAbsolutePath().equals(nav.getFile().getAbsolutePath());
  }

  public File getFile() {
    return this.navFile;
  }

  public void setFile(File file) {
    this.navFile = file;
  }

  public static NavModel fileTonav(File file) {
    return new NavModel(file.getName(), file);
  }

  public String getFilePath() {
    return this.navFile.getAbsolutePath();
  }

  public String getName() {
    return this.navName;
  }

  public void setName(String name) {
    this.navName = name;
  }
}
