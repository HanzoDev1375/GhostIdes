package ir.hanzodev1375.ghostide.plugin.api;

/**
 * Handed to a plugin in {@link GhostPlugin#activate(PluginContext)}. Deliberately free of Android
 * framework types so {@code :plugin-api} stays a plain JVM dependency for plugin authors.
 */
public interface PluginContext {

  PluginDescriptor getDescriptor();

  MutableExtensionRegistry getExtensions();

  ServiceRegistry getServices();

  PluginLogger getLogger();

  Disposable registerDisposable(Disposable disposable);
}
