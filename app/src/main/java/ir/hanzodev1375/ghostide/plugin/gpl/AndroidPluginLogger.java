package ir.hanzodev1375.ghostide.plugin.gpl;

import android.util.Log;

import ir.hanzodev1375.ghostide.plugin.api.PluginLogger;

/** Tags every log line with the owning plugin's id. */
final class AndroidPluginLogger implements PluginLogger {

  private final String tag;

  AndroidPluginLogger(String pluginId) {
    this.tag = "gpl:" + pluginId;
  }

  @Override
  public void debug(String message) {
    Log.d(tag, message);
  }

  @Override
  public void info(String message) {
    Log.i(tag, message);
  }

  @Override
  public void warn(String message, Throwable throwable) {
    Log.w(tag, message, throwable);
  }

  @Override
  public void error(String message, Throwable throwable) {
    Log.e(tag, message, throwable);
  }
}
