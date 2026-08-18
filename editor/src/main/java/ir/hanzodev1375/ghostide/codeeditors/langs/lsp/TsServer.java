package ir.hanzodev1375.ghostide.codeeditors.langs.lsp;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.CustomLanguageServerDefinition;
import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.LanguageServerDefinition;
import io.github.rosemoe.sora.lsp.editor.LspEditor;
import io.github.rosemoe.sora.lsp.editor.LspLanguage;
import io.github.rosemoe.sora.widget.CodeEditor;
import ir.hanzodev1375.ghostide.codeeditors.langs.formatHelp.DebianBootstrap;
import ir.hanzodev1375.ghostide.codeeditors.langs.js.JsLanguage;

/**
 * @author Ghost
 */
public class TsServer extends LspContentImpl {

  public static final TsServer INSTANCE = new TsServer();

  private TsServer() {
    super(
        "TsServer",
        "tsc --lsp",
        new HashSet<>(Arrays.asList("js", "mjs", "cjs", "jsx", "ts", "tsx")),
        new String[] {"/usr/bin/tsc", "/usr/local/bin/tsc"});
  }

  @Override
  public boolean isSupportedFile(String filePath) {
    return supportedExtensions.contains(extensionOf(filePath));
  }

  @Override
  protected LanguageServerDefinition createDefinition(
      Context context, String executablePath, String ext) {
    List<String> args = Arrays.asList("--lsp", "-stdio");
    return new CustomLanguageServerDefinition(
        ext,
        workingDir -> new ProotStdioConnectionProvider(context, workingDir, executablePath, args),
        serverName,
        null,
        null);
  }

  @Override
  protected LspEditor onEditorCreated(LspEditor lspEditor, CodeEditor editor) {
    Context context = editor.getContext();
    var js = new JsLanguage(context, "");
    lspEditor.setWrapperLanguage(js);
    lspEditor.setEditor(editor);
    lspEditor.setEnableInlayHint(true);
    lspEditor.setEnableSignatureHelp(true);
    lspEditor.setEnableHover(true);
    var lang = (LspLanguage) editor.getEditorLanguage();
    lang.setFormatter(js.getFormatter());
    return lspEditor;
  }
}
