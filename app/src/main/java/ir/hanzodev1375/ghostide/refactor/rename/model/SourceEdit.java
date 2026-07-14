package ir.hanzodev1375.ghostide.refactor.rename.model;

import java.io.File;

public final class SourceEdit {

  private final File file;
  private final SourceEditType type;
  private final int startOffset;
  private final int endOffset;
  private final int line;
  private final int column;
  private final String oldText;
  private final String newText;

  public SourceEdit(
      File file,
      SourceEditType type,
      int startOffset,
      int endOffset,
      int line,
      int column,
      String oldText,
      String newText) {
    this.file = file;
    this.type = type;
    this.startOffset = startOffset;
    this.endOffset = endOffset;
    this.line = line;
    this.column = column;
    this.oldText = oldText;
    this.newText = newText;
  }

  public File getFile() {
    return file;
  }

  public SourceEditType getType() {
    return type;
  }

  public int getStartOffset() {
    return startOffset;
  }

  public int getEndOffset() {
    return endOffset;
  }

  public int getLine() {
    return line;
  }

  public int getColumn() {
    return column;
  }

  public String getOldText() {
    return oldText;
  }

  public String getNewText() {
    return newText;
  }
}
