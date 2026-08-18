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
import ir.hanzodev1375.ghostide.codeeditors.langs.csharp.CSharpLanguage;
import ir.hanzodev1375.ghostide.codeeditors.langs.formatHelp.DebianBootstrap;

/**
 * @author Ghost
 */
public class CsharpServer extends LspContentImpl {

  public static final CsharpServer INSTANCE = new CsharpServer();

  private CsharpServer() {
    super(
        "CsharpServer",
        "omnisharp",
        new HashSet<>(Collections.singletonList("cs")),
        new String[] {"/root/omnisharp/run", "/opt/omnisharp/run"});
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
                context, workingDir, executablePath, Arrays.asList("-lsp", "-s", workingDir)),
        serverName,
        null,
        null);
  }

  @Override
  protected LspEditor onEditorCreated(LspEditor lspEditor, CodeEditor editor) {
    lspEditor.setWrapperLanguage(new CSharpLanguage());
    lspEditor.setEditor(editor);
    return lspEditor;
  }
}
