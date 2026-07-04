package ir.hanzodev1375.ghostide.codeeditors.preview;

public class Match {
  public String path;
  public int startColumn;
  public int endColumn;

  public Match(String path, int startColumn, int endColumn) {
    this.path = path;
    this.startColumn = startColumn;
    this.endColumn = endColumn;
  }
}
