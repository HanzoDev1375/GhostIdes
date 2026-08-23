package ir.hanzodev1375.ghostide.ide.ui.api;

import java.util.Objects;

/**
 * Immutable description of something that happened to a file in the IDE. Delivered to {@link
 * FileEventListener}s registered at {@link IdeEvents#FILE_EVENT}.
 *
 * <p>Use the static factories ({@link #opened(String)}, {@link #saved(String)}, ...) instead of
 * constructing directly. Paths are absolute filesystem paths.
 */
public final class FileEvent {

  /** The kind of change that occurred. */
  public enum Type {
    /** A file was opened in a new editor tab (not merely re-focused). */
    OPENED,
    /** A file's content was written to disk by the editor. */
    SAVED,
    /** An editor tab showing this file was closed. */
    CLOSED,
    /** A file or directory was deleted from disk. */
    DELETED,
    /** A file or directory was renamed/moved; {@link #path()} is the new location. */
    RENAMED
  }

  private final Type type;
  private final String path;
  private final String previousPath;

  private FileEvent(Type type, String path, String previousPath) {
    this.type = Objects.requireNonNull(type);
    this.path = Objects.requireNonNull(path);
    this.previousPath = previousPath;
  }

  public static FileEvent opened(String path) {
    return new FileEvent(Type.OPENED, path, null);
  }

  public static FileEvent saved(String path) {
    return new FileEvent(Type.SAVED, path, null);
  }

  public static FileEvent closed(String path) {
    return new FileEvent(Type.CLOSED, path, null);
  }

  public static FileEvent deleted(String path) {
    return new FileEvent(Type.DELETED, path, null);
  }

  /** @param newPath absolute path after the rename */
  public static FileEvent renamed(String oldPath, String newPath) {
    return new FileEvent(Type.RENAMED, newPath, oldPath);
  }

  public Type type() {
    return type;
  }

  /** Absolute path of the affected file (for {@code RENAMED}: the new path). */
  public String path() {
    return path;
  }

  /**
   * For {@code RENAMED}: the absolute path before the rename. {@code null} for every other event
   * type.
   */
  public String previousPath() {
    return previousPath;
  }

  @Override
  public String toString() {
    return "FileEvent{" + type + " path=" + path + (previousPath != null
            ? " from=" + previousPath
            : "")
        + "}";
  }
}
