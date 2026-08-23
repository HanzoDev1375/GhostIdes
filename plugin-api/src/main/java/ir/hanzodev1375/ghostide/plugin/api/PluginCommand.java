package ir.hanzodev1375.ghostide.plugin.api;

/**
 * A named, user-invokable action contributed by a plugin. Register implementations at {@link
 * CoreExtensionPoints#PLUGIN_COMMAND} so the host (and future command palette) can list and run
 * them.
 *
 * <p>{@link #getId()} must be unique and reverse-domain namespaced, e.g. {@code
 * "com.example.myplugin.reformatProject"}. {@link #execute()} may be called from any thread; move
 * to the main thread yourself before touching UI.
 */
public interface PluginCommand {

  /** Unique, reverse-domain id, stable across plugin versions. */
  String getId();

  /** Short human-readable label shown to the user. */
  String getTitle();

  /** Optional one-line description; empty string when there is nothing useful to say. */
  default String getDescription() {
    return "";
  }

  /** Runs the action. Must not block the calling thread for long. */
  void execute();
}
