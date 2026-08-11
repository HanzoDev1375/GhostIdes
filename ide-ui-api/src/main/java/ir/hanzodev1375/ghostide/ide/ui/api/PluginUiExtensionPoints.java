package ir.hanzodev1375.ghostide.ide.ui.api;

import ir.hanzodev1375.ghostide.plugin.api.ExtensionPoint;

/** Extension points for UI contributions. */
public final class PluginUiExtensionPoints {

  public static final ExtensionPoint<PluginScreen> PLUGIN_SCREEN =
      new ExtensionPoint<>("ir.hanzodev1375.ghostide.ui.pluginScreen", PluginScreen.class);

  /**
   * Panels that slide out inside a host screen (currently the editor). Kept separate from {@link
   * #PLUGIN_SCREEN} so the previous, whole-screen contribution API keeps working unchanged.
   */
  public static final ExtensionPoint<EditorPanel> EDITOR_PANEL =
      new ExtensionPoint<>("ir.hanzodev1375.ghostide.ui.editorPanel", EditorPanel.class);

  private PluginUiExtensionPoints() {}
}
