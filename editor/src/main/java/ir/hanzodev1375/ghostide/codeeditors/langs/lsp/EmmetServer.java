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
import ir.hanzodev1375.ghostide.codeeditors.langs.html.HtmlLanguage;

/**
 * @author Ghost
 */
public class EmmetServer extends LspContentImpl {

  public static final EmmetServer INSTANCE = new EmmetServer();

  private EmmetServer() {
    super(
        "EmmetServer",
        "emmet-language-server",
        new HashSet<>(Arrays.asList("html", "htm", "css", "scss", "less", "jsx", "tsx")),
        new String[] {"/usr/bin/emmet-language-server", "/usr/local/bin/emmet-language-server"});
  }

  @Override
  public boolean isSupportedFile(String filePath) {
    return supportedExtensions.contains(extensionOf(filePath));
  }

  @Override
  protected String definitionKey(String projectRoot, String ext) {
    return projectRoot + "::" + ext + "::emmet";
  }

  @Override
  protected LanguageServerDefinition createDefinition(
      Context context, String executablePath, String ext) {
    List<String> args = Arrays.asList("--stdio");
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
    var html = new HtmlLanguage(context, "");
    lspEditor.setWrapperLanguage(html);
    lspEditor.setEditor(editor);
    return lspEditor;
  }

  @Override
  public LspEditor connectFile(
      Context context, String projectRoot, String filePath, CodeEditor editor) {
    String executablePath = findInstalledExecutable(context);
    if (executablePath == null) {
      Log.e(tag, "emmet-language-server is not installed");
      return null;
    }

    if (!isSupportedFile(filePath)) {
      Log.w(tag, "Unsupported file extension: " + extensionOf(filePath));
      return null;
    }

    return super.connectFile(context, projectRoot, filePath, editor);
  }
}
