package ir.hanzodev1375.ghostide.plugin.api;

/** Service registry that also supports registration, removal, and copying. */
public interface MutableServiceRegistry extends ServiceRegistry {

  <T> Disposable register(ServiceKey<T> key, T instance);

  void unregister(ServiceKey<?> key);

  MutableServiceRegistry copy();
}
