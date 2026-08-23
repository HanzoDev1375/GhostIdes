package ir.hanzodev1375.ghostide.ide.ui.api;

import ir.hanzodev1375.ghostide.plugin.api.ExtensionPoint;
import ir.hanzodev1375.ghostide.plugin.api.GlobalRegistry;

/** Extension points for IDE-wide events a plugin can subscribe to. */
public final class IdeEvents {

  /**
   * File lifecycle events (opened/saved/closed/deleted/renamed). Register a {@link
   * FileEventListener}; it is unregistered automatically when your plugin unloads.
   */
  public static final ExtensionPoint<FileEventListener> FILE_EVENT =
      new ExtensionPoint<>("ir.hanzodev1375.ghostide.ui.fileEvent", FileEventListener.class);

  private IdeEvents() {}

  /** Dispatches {@code event} to every registered listener, isolating listener failures. */
  public static void post(FileEvent event) {
    for (FileEventListener listener : GlobalRegistry.extensions().extensions(FILE_EVENT)) {
      try {
        listener.onFileEvent(event);
      } catch (Throwable ignored) {
        // A broken listener must never break the editor's own flow.
      }
    }
  }
}
