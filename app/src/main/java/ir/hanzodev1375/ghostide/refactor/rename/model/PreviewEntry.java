package ir.hanzodev1375.ghostide.refactor.rename.model;

public final class PreviewEntry {

  private final String category;
  private final String description;
  private final String filePath;
  private final int changeCount;

  public PreviewEntry(String category, String description, String filePath, int changeCount) {
    this.category = category;
    this.description = description;
    this.filePath = filePath;
    this.changeCount = changeCount;
  }

  public String getCategory() {
    return category;
  }

  public String getDescription() {
    return description;
  }

  public String getFilePath() {
    return filePath;
  }

  public int getChangeCount() {
    return changeCount;
  }
}
