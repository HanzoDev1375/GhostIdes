package ir.hanzodev1375.ghostide.ide.api;

import ir.hanzodev1375.ghostide.plugin.api.ExtensionPoint;

/** Extension point identifiers that built-in features and plugins register against. */
public final class EditorExtensionPoints {

  public static final ExtensionPoint<LspServerProvider> LSP_SERVER_PROVIDER =
      new ExtensionPoint<>(
          "ir.hanzodev1375.ghostide.editor.lspServerProvider", LspServerProvider.class);

  private EditorExtensionPoints() {}
}
