package ir.hanzodev1375.ghostide.plugin.api;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Read-only lookup of the extensions currently registered at a point, ordered by descending
 * priority.
 */
public interface ExtensionRegistry {

  <T> List<ExtensionRegistration<T>> registrations(ExtensionPoint<T> point);

  default <T> List<T> extensions(ExtensionPoint<T> point) {
    return registrations(point).stream()
        .map(ExtensionRegistration::extension)
        .collect(Collectors.toList());
  }
}
