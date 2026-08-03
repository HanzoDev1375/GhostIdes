package ir.hanzodev1375.ghostide.plugin.api;

/**
 * The contract being implemented by a group of extension contributions.
 *
 * @param <T> the extension contract type
 */
public record ExtensionPoint<T>(String id, Class<T> type) {

  public ExtensionPoint {
    if (id == null || id.isBlank()) {
      throw new IllegalArgumentException("Extension point id must not be blank");
    }
    if (type == null) {
      throw new IllegalArgumentException("Extension point type must not be null");
    }
  }
}
