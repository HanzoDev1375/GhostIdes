package ir.hanzodev1375.ghostide.plugin.api;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/** In-memory {@link MutableExtensionRegistry}, safe for concurrent registration and lookup. */
public final class DefaultExtensionRegistry implements MutableExtensionRegistry {

  private final CopyOnWriteArrayList<ExtensionRegistration<?>> entries =
      new CopyOnWriteArrayList<>();

  @Override
  public <T> Disposable register(
      ExtensionPoint<T> point, T extension, String ownerPluginId, int priority) {
    if (!point.type().isInstance(extension)) {
      throw new IllegalArgumentException(
          "Extension "
              + extension.getClass().getName()
              + " does not implement "
              + point.type().getName());
    }
    if (ownerPluginId == null || ownerPluginId.isBlank()) {
      throw new IllegalArgumentException("Owner plugin id must not be blank");
    }
    ExtensionRegistration<T> registration =
        new ExtensionRegistration<>(point, extension, ownerPluginId, priority);
    entries.add(registration);
    return () -> entries.remove(registration);
  }

  @Override
  public void unregisterOwner(String ownerPluginId) {
    entries.removeIf(entry -> entry.ownerPluginId().equals(ownerPluginId));
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> List<ExtensionRegistration<T>> registrations(ExtensionPoint<T> point) {
    return entries.stream()
        .filter(entry -> entry.point().id().equals(point.id()) && point.type().isInstance(entry.extension()))
        .map(entry -> (ExtensionRegistration<T>) entry)
        .sorted(
            Comparator.<ExtensionRegistration<T>>comparingInt(ExtensionRegistration::priority)
                .reversed()
                .thenComparing(ExtensionRegistration::ownerPluginId))
        .collect(Collectors.toList());
  }
}
