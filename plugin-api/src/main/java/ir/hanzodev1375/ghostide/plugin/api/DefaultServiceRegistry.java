package ir.hanzodev1375.ghostide.plugin.api;

import java.util.concurrent.ConcurrentHashMap;

/** In-memory {@link MutableServiceRegistry}, safe for concurrent registration and lookup. */
public final class DefaultServiceRegistry implements MutableServiceRegistry {

  private final ConcurrentHashMap<ServiceKey<?>, Object> services = new ConcurrentHashMap<>();

  @Override
  public <T> Disposable register(ServiceKey<T> key, T instance) {
    if (!key.type().isInstance(instance)) {
      throw new IllegalArgumentException(
          "Service " + instance.getClass().getName() + " does not implement " + key.type().getName());
    }
    services.put(key, instance);
    return () -> services.remove(key, instance);
  }

  @Override
  public void unregister(ServiceKey<?> key) {
    services.remove(key);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T get(ServiceKey<T> key) {
    Object service = services.get(key);
    return service == null ? null : key.type().cast(service);
  }

  @Override
  public MutableServiceRegistry copy() {
    DefaultServiceRegistry copy = new DefaultServiceRegistry();
    copy.services.putAll(services);
    return copy;
  }
}
