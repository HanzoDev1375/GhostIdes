package ir.hanzodev1375.ghostide.codeeditors.langs.lsp;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.CustomLanguageServerDefinition;
import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.LanguageServerDefinition;
import io.github.rosemoe.sora.lsp.editor.LspEditor;
import io.github.rosemoe.sora.lsp.editor.LspLanguage;
import io.github.rosemoe.sora.lsp.editor.LspProject;
import io.github.rosemoe.sora.widget.CodeEditor;
import ir.hanzodev1375.ghostide.codeeditors.langs.formatHelp.DebianBootstrap;
import ir.hanzodev1375.ghostide.codeeditors.langs.python3.Python3Language;

/** @author Ghost */
public class PylspServer extends LspContentImpl {

  public static final PylspServer INSTANCE = new PylspServer();

  private PylspServer() {
    super("PylspServer", "pylsp",
        Collections.singleton("py"),
        new String[]{"/usr/local/bin/pylsp", "/usr/bin/pylsp", "/root/.local/bin/pylsp"});
  }

  @Override
  public boolean isSupportedFile(String filePath) {
    return supportedExtensions.contains(extensionOf(filePath));
  }

  @Override
  protected LanguageServerDefinition createDefinition(Context context, String executablePath, String ext) {
    return new CustomLanguageServerDefinition(
        ext,
        workingDir -> new ProotStdioConnectionProvider(context, workingDir, executablePath, Collections.emptyList()),
        serverName,
        null,
        null);
  }

  @Override
  protected LspEditor onEditorCreated(LspEditor lspEditor, CodeEditor editor) {
    Context context = editor.getContext();
    var py = new Python3Language(context);
    lspEditor.setWrapperLanguage(py);
    lspEditor.setEditor(editor);
    lspEditor.setEnableInlayHint(true);
    lspEditor.setEnableSignatureHelp(true);
    lspEditor.setEnableHover(true);
    var lang = (LspLanguage) editor.getEditorLanguage();
    lang.setFormatter(py.getFormatter());
    return lspEditor;
  }
}
