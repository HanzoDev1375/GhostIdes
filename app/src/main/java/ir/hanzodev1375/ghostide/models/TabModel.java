package ir.hanzodev1375.ghostide.models;

public class TabModel {
  private String filePath, fileName;
  private boolean pinned;
  private boolean readOnly;
  private transient boolean hasStar;
  private transient boolean hasError;

  public TabModel(String path, String name) {
    this(path, name, false);
  }

  public TabModel(String path, String name, boolean readOnly) {
    this.filePath = path;
    this.fileName = name;
    this.pinned = false;
    this.readOnly = readOnly;
  }

  public String getFilePath() {
    return filePath;
  }

  public String getFileName() {
    return fileName;
  }

  public void updatePath(String newFilePath, String newFileName) {
    this.filePath = newFilePath;
    this.fileName = newFileName;
  }

  public boolean isPinned() {
    return pinned;
  }

  public void setPinned(boolean pinned) {
    this.pinned = pinned;
  }

  public boolean isReadOnly() {
    return readOnly;
  }

  public void setReadOnly(boolean readOnly) {
    this.readOnly = readOnly;
  }

  public boolean getHasStar() {
    return hasStar;
  }

  public void setHasStar(boolean hasStar) {
    this.hasStar = hasStar;
  }

  public boolean getHasError() {
    return this.hasError;
  }

  public void setHasError(boolean hasError) {
    this.hasError = hasError;
  }
}
