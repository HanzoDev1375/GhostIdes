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
import ir.hanzodev1375.ghostide.codeeditors.langs.php.PhpLanguage;
import org.eclipse.lsp4j.ServerCapabilities;

/**
 * @author Ghost
 */
public class PhpServer extends LspContentImpl {

  public static final PhpServer INSTANCE = new PhpServer();

  private PhpServer() {
    super(
        "PhpServer",
        "intelephense",
        new HashSet<>(Arrays.asList("php", "php5", "phtml")),
        new String[] {"/usr/local/bin/intelephense", "/usr/bin/intelephense"});
  }

  @Override
  public boolean isSupportedFile(String filePath) {
    return supportedExtensions.contains(extensionOf(filePath));
  }

  @Override
  protected LanguageServerDefinition createDefinition(
      Context context, String executablePath, String ext) {
    List<String> args = Arrays.asList("--stdio");
    ServerCapabilities cap = new ServerCapabilities();
    cap.setCodeActionProvider(true);
    cap.setInlayHintProvider(true);
    cap.setRenameProvider(true);
    cap.setReferencesProvider(true);
    return new CustomLanguageServerDefinition(
        ext,
        workingDir -> new ProotStdioConnectionProvider(context, workingDir, executablePath, args),
        serverName,
        cap,
        null);
  }

  @Override
  protected LspEditor onEditorCreated(LspEditor lspEditor, CodeEditor editor) {
    Context context = editor.getContext();
    var php = new PhpLanguage(context);
    lspEditor.setWrapperLanguage(php);
    lspEditor.setEditor(editor);
    lspEditor.setEnableInlayHint(true);
    return lspEditor;
  }

  @Override
  public LspEditor connectFile(
      Context context, String projectRoot, String filePath, CodeEditor editor) {
    String executablePath = findInstalledExecutable(context);
    if (executablePath == null) {
      Log.e(tag, "intelephense is not installed. Run: npm install -g intelephense");
      return null;
    }

    String ext = extensionOf(filePath);
    LspProject project = getOrCreateProject(projectRoot);
    ensureDefinitionRegistered(project, context, executablePath, projectRoot, ext);

    String emmetExecutable = EmmetServer.INSTANCE.findInstalledExecutable(context);
    if (emmetExecutable != null) {
      EmmetServer.INSTANCE.ensureDefinitionRegistered(
          project, context, emmetExecutable, projectRoot, ext);
      Log.d(tag, "Emmet Language Server registered for this project");
    } else {
      Log.d(tag, "Emmet Language Server not installed");
    }

    return super.connectFile(context, projectRoot, filePath, editor);
  }
}
