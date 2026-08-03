package ir.hanzodev1375.ghostide.plugin.api;

/** Read-only lookup of services the host makes available to plugins. */
public interface ServiceRegistry {

  <T> T get(ServiceKey<T> key);

  default <T> T require(ServiceKey<T> key) {
    T service = get(key);
    if (service == null) {
      throw new IllegalStateException("Required service '" + key.name() + "' is not registered");
    }
    return service;
  }
}
