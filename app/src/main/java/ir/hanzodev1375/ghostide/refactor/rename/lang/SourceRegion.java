package ir.hanzodev1375.ghostide.refactor.rename.lang;

public final class SourceRegion {

  public enum Type {
    CODE,
    LINE_COMMENT,
    BLOCK_COMMENT,
    STRING,
    CHAR,
    TEXT_BLOCK
  }

  private final Type type;
  private final int start;
  private final int end;

  public SourceRegion(Type type, int start, int end) {
    this.type = type;
    this.start = start;
    this.end = end;
  }

  public Type getType() {
    return type;
  }

  public int getStart() {
    return start;
  }

  public int getEnd() {
    return end;
  }
}
