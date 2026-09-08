package ir.hanzodev1375.ghostide.codeeditors.langs.lsp.jdt;

import android.content.Context;

import java.util.Collections;
import java.util.HashSet;

import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.CustomLanguageServerDefinition;
import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.LanguageServerDefinition;
import io.github.rosemoe.sora.lsp.editor.LspEditor;
import io.github.rosemoe.sora.lsp.editor.LspLanguage;
import io.github.rosemoe.sora.widget.CodeEditor;
import ir.hanzodev1375.ghostide.codeeditors.langs.java.JavaLanguage;
import ir.hanzodev1375.ghostide.codeeditors.langs.lsp.LspContentImpl;

/**
 * In-process Java language server built on Eclipse JDT, bundled with the app.
 *
 * <p>Unlike {@code JavaServer} (which spawns the external {@code jdtls} binary inside the Debian
 * proot rootfs and needs a full JVM installed), this server runs <b>fully in-process</b>: {@link
 * EmbeddedJdtConnectionProvider} hosts an LSP4j {@code JdtLanguageServer} backed by {@code
 * org.eclipse.jdt.core} inside the editor process. No node, no JVM, no proot, nothing to install —
 * the JDT engine ships dexed inside the app.
 *
 * <p>Features: error-tolerant parsing (works on broken code), ranked completion, diagnostics,
 * hover, and code formatting.
 */
public class JdtServer extends LspContentImpl {

  public static final JdtServer INSTANCE = new JdtServer();

  private static final String TAG = "JdtServer";

  private JdtServer() {
    super(
        "JdtServer",
        "ghost-java-lsp",
        new HashSet<>(Collections.singletonList("java")),
        null);
  }

  @Override
  public boolean isSupportedFile(String filePath) {
    return "java".equals(extensionOf(filePath));
  }

  /** The bundled engine is always available; there is nothing to install. */
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
        workingDir -> new EmbeddedJdtConnectionProvider(context, workingDir),
        serverName,
        null);
  }

  @Override
  protected String definitionKey(String projectRoot, String ext) {
    // Single shared definition for the bundled Java server across every project root.
    return "jdt::all";
  }

  @Override
  protected LspEditor onEditorCreated(LspEditor lspEditor, CodeEditor editor) {
    Context context = editor.getContext();
    JavaLanguage java = new JavaLanguage(context);
    lspEditor.setWrapperLanguage(java);
    lspEditor.setEditor(editor);
    lspEditor.setEnableInlayHint(true);
    lspEditor.setEnableHover(true);
    LspLanguage lang = (LspLanguage) editor.getEditorLanguage();
    lang.setFormatter(java.getFormatter());
    return lspEditor;
  }
}