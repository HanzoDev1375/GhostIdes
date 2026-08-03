package ir.hanzodev1375.ghostide.plugin.api;

/**
 * Stable identity and user-facing metadata for one extension contribution. Every typed extension
 * contract (LSP provider, formatter provider, and so on) extends this interface.
 */
public interface ConfigurableExtension {

  String getId();

  default String getDisplayName() {
    return getId();
  }

  default String getDescription() {
    return "";
  }

  default boolean isEnabledByDefault() {
    return true;
  }

  default boolean isCanDisable() {
    return true;
  }
}
