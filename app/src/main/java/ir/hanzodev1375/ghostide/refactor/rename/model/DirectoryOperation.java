package ir.hanzodev1375.ghostide.refactor.rename.model;

import java.io.File;

public final class DirectoryOperation {

  public enum Kind {
    CREATE_DIRECTORY,
    MOVE_FILE,
    DELETE_EMPTY_DIRECTORY
  }

  private final Kind kind;
  private final File source;
  private final File destination;

  public DirectoryOperation(Kind kind, File source, File destination) {
    this.kind = kind;
    this.source = source;
    this.destination = destination;
  }

  public Kind getKind() {
    return kind;
  }

  public File getSource() {
    return source;
  }

  public File getDestination() {
    return destination;
  }
}
