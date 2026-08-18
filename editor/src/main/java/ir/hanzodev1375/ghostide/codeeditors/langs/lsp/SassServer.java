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
import ir.hanzodev1375.ghostide.codeeditors.langs.sass.SassLanguage;

/**
 * @author Ghost
 */
public class SassServer extends LspContentImpl {

  public static final SassServer INSTANCE = new SassServer();

  private SassServer() {
    super(
        "SassServer",
        "some-sass-language-server",
        new HashSet<>(Arrays.asList("scss", "sass")),
        new String[] {
          "/usr/bin/some-sass-language-server", "/usr/local/bin/some-sass-language-server"
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
    SassLanguage sass = new SassLanguage(context);
    lspEditor.setWrapperLanguage(sass);
    lspEditor.setEditor(editor);
    ((LspLanguage) editor.getEditorLanguage()).setFormatter(sass.getFormatter());
    return lspEditor;
  }
}
