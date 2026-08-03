package ir.hanzodev1375.ghostide.ide.ui.api;

import ir.hanzodev1375.ghostide.plugin.api.ExtensionPoint;

/** Extension points for UI contributions. */
public final class PluginUiExtensionPoints {

  public static final ExtensionPoint<PluginScreen> PLUGIN_SCREEN =
      new ExtensionPoint<>("ir.hanzodev1375.ghostide.ui.pluginScreen", PluginScreen.class);

  private PluginUiExtensionPoints() {}
}
