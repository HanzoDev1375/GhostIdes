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
import ir.hanzodev1375.ghostide.codeeditors.langs.go.GoLanguage;

/**
 * @author Ghost
 */
public class GoServer extends LspContentImpl {

  public static final GoServer INSTANCE = new GoServer();

  private GoServer() {
    super(
        "GoServer",
        "gopls",
        new HashSet<>(Collections.singletonList("go")),
        new String[] {"/root/go/bin/gopls", "/usr/local/go/bin/gopls", "/usr/bin/gopls"});
  }

  @Override
  public boolean isSupportedFile(String filePath) {
    return supportedExtensions.contains(extensionOf(filePath));
  }

  @Override
  protected LanguageServerDefinition createDefinition(
      Context context, String executablePath, String ext) {
    return new CustomLanguageServerDefinition(
        ext,
        workingDir ->
            new ProotStdioConnectionProvider(
                context, workingDir, executablePath, Collections.emptyList()),
        serverName,
        null,
        null);
  }

  @Override
  protected LspEditor onEditorCreated(LspEditor lspEditor, CodeEditor editor) {
    Context context = editor.getContext();
    var go = new GoLanguage(context);
    lspEditor.setWrapperLanguage(go);
    lspEditor.setEditor(editor);
    var lang = (LspLanguage) editor.getEditorLanguage();
    lang.setFormatter(go.getFormatter());
    return lspEditor;
  }
}
