package ir.hanzodev1375.ghostide.codeeditors.ui.model;

public class OpenFileLocationEvent {
  public final String filePath;
  public final int line;
  public final int column;

  public OpenFileLocationEvent(String filePath, int line, int column) {
    this.filePath = filePath;
    this.line = line;
    this.column = column;
  }
}
