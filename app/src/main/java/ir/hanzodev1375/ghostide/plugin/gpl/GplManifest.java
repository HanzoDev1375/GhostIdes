package ir.hanzodev1375.ghostide.plugin.gpl;

/**
 * Parsed {@code assets/plugin.json} from inside a {@code .gpl} package.
 *
 * @param id reverse-domain plugin id, matches {@code ir.hanzodev1375.ghostide.plugin.api.PluginDescriptor}
 * @param name user-facing plugin name
 * @param version plugin version string
 * @param entryClass fully qualified class implementing {@code GhostPlugin}, with a public
 *     no-argument constructor
 * @param description short description shown in the plugin manager
 * @param minHostVersion lowest host app version code this plugin supports, or 0 if unspecified
 */
public record GplManifest(
    String id, String name, String version, String entryClass, String description, int minHostVersion) {

  public GplManifest {
    if (id == null || id.isBlank()) {
      throw new IllegalArgumentException("gpl manifest id must not be blank");
    }
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("gpl manifest name must not be blank");
    }
    if (version == null || version.isBlank()) {
      throw new IllegalArgumentException("gpl manifest version must not be blank");
    }
    if (entryClass == null || entryClass.isBlank()) {
      throw new IllegalArgumentException("gpl manifest entryClass must not be blank");
    }
    if (description == null) {
      description = "";
    }
  }
}
