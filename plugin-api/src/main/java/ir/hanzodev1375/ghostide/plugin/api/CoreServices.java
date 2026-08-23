package ir.hanzodev1375.ghostide.plugin.api;

/**
 * Host service keys that live at the {@code :plugin-api} layer because their contracts are free of
 * Android types, so even LSP-only plugins (which never depend on {@code ide-ui-api}) can use them.
 */
public final class CoreServices {

  /**
   * Persistent key-value storage scoped to the requesting plugin. The host publishes a fresh,
   * isolated instance per plugin (namespaced by plugin id) into each plugin's service registry
   * before {@code activate()} runs.
   */
  public static final ServiceKey<PluginStorage> PLUGIN_STORAGE =
      new ServiceKey<>("ir.hanzodev1375.ghostide.core.pluginStorage", PluginStorage.class);

  private CoreServices() {}
}
