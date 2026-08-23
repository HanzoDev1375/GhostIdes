package ir.hanzodev1375.ghostide.plugin.api;

/**
 * Persistent key-value storage scoped to a single plugin. Published by the host under {@link
 * CoreServices#PLUGIN_STORAGE}; every plugin sees its own isolated namespace keyed by its
 * descriptor id, and data survives IDE restarts and plugin reloads.
 *
 * <p>All values must be primitives or {@code String} — the backing store is not meant for large
 * blobs. For bigger payloads create files inside your own private directory instead.
 *
 * <p>This interface is deliberately free of Android types so {@code :plugin-api} stays a plain
 * JVM dependency. Implementations may be backed by {@code SharedPreferences}, files, or anything
 * else the host chooses.
 */
public interface PluginStorage {

  String getString(String key, String defValue);

  void putString(String key, String value);

  boolean getBoolean(String key, boolean defValue);

  void putBoolean(String key, boolean value);

  int getInt(String key, int defValue);

  void putInt(String key, int value);

  long getLong(String key, long defValue);

  void putLong(String key, long value);

  float getFloat(String key, float defValue);

  void putFloat(String key, float value);

  /** Removes a single entry. No-op when the key does not exist. */
  void remove(String key);

  /** Whether an entry exists under {@code key}. */
  boolean contains(String key);

  /** Removes every entry stored by this plugin. */
  void clear();
}
