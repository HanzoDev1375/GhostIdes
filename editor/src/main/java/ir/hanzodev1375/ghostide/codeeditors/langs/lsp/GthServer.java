package ir.hanzodev1375.ghostide.codeeditors.langs.lsp;

import android.content.Context;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.CustomLanguageServerDefinition;
import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.LanguageServerDefinition;
import io.github.rosemoe.sora.lsp.editor.LspEditor;
import io.github.rosemoe.sora.lsp.editor.LspLanguage;
import io.github.rosemoe.sora.widget.CodeEditor;
import ir.hanzodev1375.ghostide.codeeditors.langs.json.JsonLanguage;

/**
 * Language server for GhostIDE theme files ({@code .gth}).
 *
 * <p>The server is installed <i>inside the Debian proot rootfs</i> via npm, exactly like the other
 * node-based servers (intelephense, vscode-css-language-server, ...). Inside the Debian terminal
 * run:
 *
 * <pre>{@code
 * apt-get update && apt-get install -y nodejs npm
 * npm install -g ghost-theme-lsp
 * }</pre>
 *
 * This puts the {@code ghost-theme-lsp} launcher into the rootfs ({@code /usr/local/bin}), which
 * {@link ProotStdioConnectionProvider} then executes directly.
 *
 * <p>Features: JSON diagnostics, sections/key completions, {@code @section.key} reference
 * completions, hover with resolved values, go-to-definition for references, document colors, and
 * document/range formatting via Prettier. Prettier is optional: the server tries a one-time {@code
 * npm install -g prettier} in the background and falls back to a built-in JSON printer, so
 * formatting always works.
 */
public class GthServer extends LspContentImpl {

  public static final GthServer INSTANCE = new GthServer();

  private static final String TAG = "GthServer";

  private static final String[] NODE_PATHS = {
    "/usr/local/bin/ghost-theme-lsp", "/usr/bin/ghost-theme-lsp"
  };

  private GthServer() {
    super(
        "GthServer",
        "ghost-theme-lsp",
        new HashSet<>(Collections.singletonList("gth")),
        NODE_PATHS);
  }

  @Override
  public boolean isSupportedFile(String filePath) {
    return "gth".equals(extensionOf(filePath));
  }

  @Override
  protected LanguageServerDefinition createDefinition(
      Context context, String executablePath, String ext) {
    if (executablePath == null) return null;
    List<String> args = Arrays.asList("--stdio");
    return new CustomLanguageServerDefinition(
        ext,
        workingDir -> new ProotStdioConnectionProvider(context, workingDir, executablePath, args),
        serverName,
        null,
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
    LspLanguage lang = (LspLanguage) editor.getEditorLanguage();
    lang.setFormatter(json.getFormatter());
    return lspEditor;
  }
}