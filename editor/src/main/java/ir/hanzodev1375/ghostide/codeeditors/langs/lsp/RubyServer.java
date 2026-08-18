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
import ir.hanzodev1375.ghostide.codeeditors.langs.ruby.RubyLanguage;

/**
 * @author Ghost
 */
public class RubyServer extends LspContentImpl {

  public static final RubyServer INSTANCE = new RubyServer();

  private RubyServer() {
    super(
        "RubyServer",
        "solargraph",
        new HashSet<>(Collections.singletonList("rb")),
        new String[] {"/usr/local/bin/solargraph", "/usr/bin/solargraph"});
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
                context, workingDir, executablePath, Collections.singletonList("stdio")),
        serverName,
        null,
        null);
  }

  @Override
  protected LspEditor onEditorCreated(LspEditor lspEditor, CodeEditor editor) {
    Context context = editor.getContext();
    var rubyLang = new RubyLanguage(context);
    lspEditor.setWrapperLanguage(rubyLang);
    lspEditor.setEditor(editor);
    var lang = (LspLanguage) editor.getEditorLanguage();
    lang.setFormatter(rubyLang.getFormatter());
    return lspEditor;
  }
}
