package ir.hanzodev1375.ghostide.codeeditors.langs.lsp;

import android.content.Context;

import java.util.Collections;
import java.util.HashSet;

import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.CustomLanguageServerDefinition;
import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.LanguageServerDefinition;
import io.github.rosemoe.sora.lsp.editor.LspEditor;
import io.github.rosemoe.sora.lsp.editor.LspLanguage;
import io.github.rosemoe.sora.widget.CodeEditor;
import ir.hanzodev1375.ghostide.codeeditors.langs.json.JsonLanguage;
import ir.hanzodev1375.ghostide.codeeditors.langs.lsp.gth.EmbeddedGthConnectionProvider;

/**
 * Language server for GhostIDE theme files ({@code .gth}).
 *
 * <p>Unlike the other servers (which launch a binary inside the Debian proot rootfs), this one runs
 * <b>fully in-process</b>: {@link EmbeddedGthConnectionProvider} hosts an LSP4j {@link
 * ir.hanzodev1375.ghostide.codeeditors.langs.lsp.gth.GhostThemeLanguageServer} inside the editor
 * and bridges it to the client through in-memory pipes. No node/npm/proot install is needed.
 *
 * <p>Features: JSON diagnostics, sections/key completions, {@code @section.key} reference
 * completions, hover with resolved values, go-to-definition for references, document colors, and
 * whole-document / range formatting with a tolerant built-in JSON printer.
 */
public class GthServer extends LspContentImpl {

  public static final GthServer INSTANCE = new GthServer();

  private static final String TAG = "GthServer";

  private GthServer() {
    super(
        "GthServer",
        "ghost-theme-lsp",
        new HashSet<>(Collections.singletonList("gth")),
        null);
  }

  @Override
  public boolean isSupportedFile(String filePath) {
    return "gth".equals(extensionOf(filePath));
  }

  /** The embedded server is always available; there is nothing to install. */
  @Override
  public boolean isInstalled(Context context) {
    return true;
  }

  /** No external binary exists for the in-process server. */
  @Override
  public String findInstalledExecutable(Context context) {
    return "";
  }

  @Override
  protected LanguageServerDefinition createDefinition(
      Context context, String executablePath, String ext) {
    return new CustomLanguageServerDefinition(
        ext,
        workingDir -> new EmbeddedGthConnectionProvider(),
        serverName,
        null);
  }

  @Override
  protected String definitionKey(String projectRoot, String ext) {
    // Single shared definition for the theme server across every project root.
    return "gth::all";
  }

  @Override
  protected LspEditor onEditorCreated(LspEditor lspEditor, CodeEditor editor) {
    Context context = editor.getContext();
    JsonLanguage json = new JsonLanguage(context, "");
    lspEditor.setWrapperLanguage(json);
    lspEditor.setEditor(editor);
    lspEditor.setEnableInlayHint(true);
    lspEditor.setEnableHover(true);
    LspLanguage lang = (LspLanguage) editor.getEditorLanguage();
    lang.setFormatter(json.getFormatter());
    return lspEditor;
  }
}