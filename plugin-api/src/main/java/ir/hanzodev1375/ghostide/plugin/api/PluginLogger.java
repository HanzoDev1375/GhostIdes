package ir.hanzodev1375.ghostide.plugin.api;

/** Logging facade handed to a plugin through its {@link PluginContext}. */
public interface PluginLogger {

  void debug(String message);

  void info(String message);

  void warn(String message, Throwable throwable);

  void error(String message, Throwable throwable);

  default void warn(String message) {
    warn(message, null);
  }

  default void error(String message) {
    error(message, null);
  }
}
