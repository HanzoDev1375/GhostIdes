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

  /**
   * Handlers that intercept LSP command actions locally (e.g. code actions sent by a plugin
   * language server) and run them inside the editor, with the raw {@code IdeEditor} available.
   */
  public static final ExtensionPoint<EditorActionHandler> EDITOR_ACTION_HANDLER =
      new ExtensionPoint<>(
          "ir.hanzodev1375.ghostide.ui.editorActionHandler", EditorActionHandler.class);

  /**
   * Custom file icons contributed by plugins. Consulted (by descending priority) before the
   * built-in icon set everywhere file icons are shown: file manager, editor tabs, history,
   * bookmarks.
   */
  public static final ExtensionPoint<FileIconContributor> FILE_ICON_CONTRIBUTOR =
      new ExtensionPoint<>(
          "ir.hanzodev1375.ghostide.ui.fileIconContributor", FileIconContributor.class);

  private PluginUiExtensionPoints() {}
}
