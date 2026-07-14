package ir.hanzodev1375.ghostide.refactor.rename.model;

import java.io.File;

public final class RollbackEntry {

  public enum Action {
    RESTORE_FILE_CONTENT,
    MOVE_FILE_BACK,
    DELETE_CREATED_DIRECTORY,
    RECREATE_DELETED_DIRECTORY
  }

  private final Action action;
  private final File target;
  private final File backup;
  private final byte[] originalContent;

  public RollbackEntry(Action action, File target, File backup, byte[] originalContent) {
    this.action = action;
    this.target = target;
    this.backup = backup;
    this.originalContent = originalContent;
  }

  public Action getAction() {
    return action;
  }

  public File getTarget() {
    return target;
  }

  public File getBackup() {
    return backup;
  }

  public byte[] getOriginalContent() {
    return originalContent;
  }
}
