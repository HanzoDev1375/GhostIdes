package ir.hanzodev1375.ghostide.plugin.api;

/**
 * Identity of a host-provided service, such as process execution or command execution.
 *
 * @param <T> the service interface type
 */
public record ServiceKey<T>(String name, Class<T> type) {

  public ServiceKey {
    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("Service key name must not be blank");
    }
    if (type == null) {
      throw new IllegalArgumentException("Service key type must not be null");
    }
  }
}
