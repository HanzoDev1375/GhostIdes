package ir.hanzodev1375.ghostide.codeeditors.langs.lsp;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.CustomLanguageServerDefinition;
import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.LanguageServerDefinition;
import io.github.rosemoe.sora.lsp.editor.LspEditor;
import io.github.rosemoe.sora.lsp.editor.LspLanguage;
import io.github.rosemoe.sora.lsp.editor.LspProject;
import io.github.rosemoe.sora.widget.CodeEditor;
import ir.hanzodev1375.ghostide.codeeditors.langs.cpp.CppLanguage;
import ir.hanzodev1375.ghostide.codeeditors.langs.formatHelp.DebianBootstrap;

/**
 * @author Ghost
 */
public class ClangdServer extends LspContentImpl {

  public static final ClangdServer INSTANCE = new ClangdServer();

  private ClangdServer() {
    super(
        "ClangdServer",
        "clangd",
        new HashSet<>(Arrays.asList("cpp", "cxx", "cc", "hpp", "hxx", "h")),
        new String[] {
          "/usr/bin/clangd",
          "/usr/local/bin/clangd",
          "/usr/bin/clangd-18",
          "/usr/bin/clangd-17",
          "/usr/bin/clangd-16",
          "/usr/bin/clangd-15",
          "/usr/bin/clangd-14",
          "/usr/bin/clangd-12"
        });
  }

  @Override
  public boolean isSupportedFile(String filePath) {
    return supportedExtensions.contains(extensionOf(filePath));
  }

  @Override
  protected LanguageServerDefinition createDefinition(
      Context context, String executablePath, String ext) {
    List<String> args = Arrays.asList("--background-index");
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
    var cpp = new CppLanguage(context, editor);
    lspEditor.setWrapperLanguage(cpp);
    lspEditor.setEditor(editor);
    var lang = (LspLanguage) editor.getEditorLanguage();
    lang.setFormatter(cpp.getFormatter());
    return lspEditor;
  }
}
