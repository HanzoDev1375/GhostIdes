package ir.hanzodev1375.ghostide.plugin.api;

/**
 * One environment setup command owned by a plugin as a whole, such as installing a language
 * server inside the proot rootfs. The host runs setup commands only through an interactive
 * terminal handoff that the user confirms; it never runs them silently.
 *
 * @param id stable identity of this action within its plugin
 * @param label short user-facing label
 * @param command shell command text, passed as-is to a login shell
 * @param description longer explanation shown before the user confirms
 */
public record PluginSetupAction(String id, String label, String command, String description) {

  public PluginSetupAction {
    if (id == null || id.isBlank()) {
      throw new IllegalArgumentException("Plugin setup action id must not be blank");
    }
    if (label == null || label.isBlank()) {
      throw new IllegalArgumentException("Plugin setup action label must not be blank");
    }
    if (command == null || command.isBlank()) {
      throw new IllegalArgumentException("Plugin setup command must not be blank");
    }
    if (description == null) {
      description = "";
    }
  }

  public PluginSetupAction(String id, String label, String command) {
    this(id, label, command, "");
  }
}
