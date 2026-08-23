package ir.hanzodev1375.ghostide.plugin.gpl;

import android.content.Context;
import android.content.SharedPreferences;

import ir.hanzodev1375.ghostide.plugin.api.PluginStorage;

/**
 * {@link PluginStorage} backed by a per-plugin {@link SharedPreferences} file named after the
 * plugin id. Instances handed to plugins are isolated by construction: each plugin's service
 * registry receives its own instance bound to its own prefs file.
 */
final class PluginStorageImpl implements PluginStorage {

  private static final String PREFS_PREFIX = "gpl_storage_";

  private final SharedPreferences prefs;

  PluginStorageImpl(Context appContext, String pluginId) {
    String safeId = pluginId.replaceAll("[^a-zA-Z0-9_.-]", "_");
    this.prefs = appContext.getSharedPreferences(PREFS_PREFIX + safeId, Context.MODE_PRIVATE);
  }

  @Override
  public String getString(String key, String defValue) {
    return prefs.getString(key, defValue);
  }

  @Override
  public void putString(String key, String value) {
    prefs.edit().putString(key, value).apply();
  }

  @Override
  public boolean getBoolean(String key, boolean defValue) {
    return prefs.getBoolean(key, defValue);
  }

  @Override
  public void putBoolean(String key, boolean value) {
    prefs.edit().putBoolean(key, value).apply();
  }

  @Override
  public int getInt(String key, int defValue) {
    return prefs.getInt(key, defValue);
  }

  @Override
  public void putInt(String key, int value) {
    prefs.edit().putInt(key, value).apply();
  }

  @Override
  public long getLong(String key, long defValue) {
    return prefs.getLong(key, defValue);
  }

  @Override
  public void putLong(String key, long value) {
    prefs.edit().putLong(key, value).apply();
  }

  @Override
  public float getFloat(String key, float defValue) {
    return prefs.getFloat(key, defValue);
  }

  @Override
  public void putFloat(String key, float value) {
    prefs.edit().putFloat(key, value).apply();
  }

  @Override
  public void remove(String key) {
    prefs.edit().remove(key).apply();
  }

  @Override
  public boolean contains(String key) {
    return prefs.contains(key);
  }

  @Override
  public void clear() {
    prefs.edit().clear().apply();
  }
}
