package ir.hanzodev1375.ghostide.codeeditors.langs.lsp;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import io.github.rosemoe.sora.lsp.editor.LspProject;
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
public class HtmlServer extends LspContentImpl {

  public static final HtmlServer INSTANCE = new HtmlServer();

  private HtmlServer() {
    super(
        "HtmlServer",
        "vscode-html-language-server",
        new HashSet<>(Arrays.asList("html", "htm")),
        new String[] {
          "/usr/bin/vscode-html-language-server",
          "/usr/local/bin/vscode-html-language-server",
          "/usr/bin/html-languageserver",
          "/usr/local/bin/html-languageserver"
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
    var html = new HtmlLanguage(context, "");
    lspEditor.setWrapperLanguage(html);
    lspEditor.setEditor(editor);
    return lspEditor;
  }

  @Override
  public LspEditor connectFile(
      Context context, String projectRoot, String filePath, CodeEditor editor) {
    String htmlExecutable = findInstalledExecutable(context);
    if (htmlExecutable == null) {
      Log.e(tag, "vscode-html-language-server is not installed");
      return null;
    }

    String ext = extensionOf(filePath);
    LspProject project = getOrCreateProject(projectRoot);
    ensureDefinitionRegistered(project, context, htmlExecutable, projectRoot, ext);

    String emmetExecutable = EmmetServer.INSTANCE.findInstalledExecutable(context);
    if (emmetExecutable != null) {
      EmmetServer.INSTANCE.ensureDefinitionRegistered(
          project, context, emmetExecutable, projectRoot, ext);
      Log.d(tag, "Emmet Language Server registered for this project");
    } else {
      Log.d(tag, "Emmet Language Server not installed, using HTML Server only");
    }

    return super.connectFile(context, projectRoot, filePath, editor);
  }
}
