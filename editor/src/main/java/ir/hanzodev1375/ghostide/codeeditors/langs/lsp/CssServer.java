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
import ir.hanzodev1375.ghostide.codeeditors.langs.css.CssLanguage;
import ir.hanzodev1375.ghostide.codeeditors.langs.formatHelp.DebianBootstrap;

/**
 * @author Ghost
 */
public class CssServer extends LspContentImpl {

  public static final CssServer INSTANCE = new CssServer();

  private CssServer() {
    super(
        "CssServer",
        "vscode-css-language-server",
        new HashSet<>(Arrays.asList("css")),
        new String[] {
          "/usr/bin/vscode-css-language-server",
          "/usr/local/bin/vscode-css-language-server",
          "/usr/bin/css-languageserver",
          "/usr/local/bin/css-languageserver"
        });
  }

  @Override
  public boolean isSupportedFile(String filePath) {
    return supportedExtensions.contains(extensionOf(filePath));
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
    var css =
        new CssLanguage(
            context,
            editor.getContext().getExternalFilesDir(null) != null ? editor.toString() : "");
    lspEditor.setWrapperLanguage(css);
    lspEditor.setEditor(editor);
    return lspEditor;
  }
}
