package ir.hanzodev1375.ghostide.ide.ui.api;

/** Receiver of {@link FileEvent}s, registered at {@link IdeEvents#FILE_EVENT}. */
@FunctionalInterface
public interface FileEventListener {

  /**
   * Called when a file event occurs. May be invoked from any thread (saves and deletes happen on
   * background threads) — hop to the main thread yourself before touching UI.
   */
  void onFileEvent(FileEvent event);
}
