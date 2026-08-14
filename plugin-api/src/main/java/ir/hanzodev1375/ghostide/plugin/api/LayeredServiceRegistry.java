package ir.hanzodev1375.ghostide.plugin.api;

import java.util.concurrent.ConcurrentHashMap;

/**
 * A {@link MutableServiceRegistry} that layers plugin-local registrations on top of a shared base
 * registry. Lookup checks the local map first, then falls back to the base, so host services that
 * are registered <em>after</em> a plugin was created (e.g. {@code EDITOR_HOST}, published once an
 * editor screen exists) are still visible to that plugin.
 */
public final class LayeredServiceRegistry implements MutableServiceRegistry {

  private final ServiceRegistry base;
  private final ConcurrentHashMap<ServiceKey<?>, Object> local = new ConcurrentHashMap<>();

  public LayeredServiceRegistry(ServiceRegistry base) {
    this.base = base;
  }

  @Override
  public <T> Disposable register(ServiceKey<T> key, T instance) {
    if (!key.type().isInstance(instance)) {
      throw new IllegalArgumentException(
          "Service " + instance.getClass().getName() + " does not implement " + key.type().getName());
    }
    local.put(key, instance);
    return () -> local.remove(key, instance);
  }

  @Override
  public void unregister(ServiceKey<?> key) {
    local.remove(key);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T get(ServiceKey<T> key) {
    Object service = local.get(key);
    if (service != null) {
      return key.type().cast(service);
    }
    return base.get(key);
  }

  @Override
  public MutableServiceRegistry copy() {
    LayeredServiceRegistry copy = new LayeredServiceRegistry(base);
    copy.local.putAll(local);
    return copy;
  }
}
