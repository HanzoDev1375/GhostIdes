package ir.hanzodev1375.ghostide.plugin.api;

/** Extension points at the {@code :plugin-api} layer, usable by Android-free plugins. */
public final class CoreExtensionPoints {

  /** Commands contributed by plugins for the host to list and invoke (e.g. a command palette). */
  public static final ExtensionPoint<PluginCommand> PLUGIN_COMMAND =
      new ExtensionPoint<>("ir.hanzodev1375.ghostide.core.pluginCommand", PluginCommand.class);

  private CoreExtensionPoints() {}
}
