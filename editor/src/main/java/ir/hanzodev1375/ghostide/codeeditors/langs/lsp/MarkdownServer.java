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
import ir.hanzodev1375.ghostide.codeeditors.langs.markdown.MarkdownLanguage;

/**
 * @author Ghost
 */
public class MarkdownServer extends LspContentImpl {

  public static final MarkdownServer INSTANCE = new MarkdownServer();

  private MarkdownServer() {
    super(
        "MarkdownServer",
        "vscode-markdown-language-server",
        new HashSet<>(Arrays.asList("md", "markdown", "mdown")),
        new String[] {
          "/usr/bin/vscode-markdown-language-server",
          "/usr/local/bin/vscode-markdown-language-server",
          "/usr/bin/markdown-languageserver",
          "/usr/local/bin/markdown-languageserver"
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
    var md = new MarkdownLanguage();
    lspEditor.setWrapperLanguage(md);
    lspEditor.setEditor(editor);
    var lang = (LspLanguage) editor.getEditorLanguage();
    lang.setFormatter(md.getFormatter());
    return lspEditor;
  }
}
