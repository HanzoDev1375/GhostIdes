package ir.hanzodev1375.ghostide.plugin.api;

/** Extension registry that also supports registration and owner-based cleanup. */
public interface MutableExtensionRegistry extends ExtensionRegistry {

  <T> Disposable register(ExtensionPoint<T> point, T extension, String ownerPluginId, int priority);

  default <T> Disposable register(ExtensionPoint<T> point, T extension) {
    return register(point, extension, PluginIds.CORE, 0);
  }

  void unregisterOwner(String ownerPluginId);
}
