package ir.hanzodev1375.ghostide.plugin.api;

/**
 * A declared dependency of one plugin on another plugin id, checked before activation.
 *
 * @param id the required plugin id
 * @param minVersion the minimum required version, or {@code null} if any version is acceptable
 * @param optional whether activation may proceed if the dependency is missing
 */
public record PluginDependency(String id, String minVersion, boolean optional) {

  public PluginDependency {
    if (id == null || id.isBlank()) {
      throw new IllegalArgumentException("Plugin dependency id must not be blank");
    }
  }

  public static PluginDependency required(String id) {
    return new PluginDependency(id, null, false);
  }

  public static PluginDependency optional(String id) {
    return new PluginDependency(id, null, true);
  }
}
