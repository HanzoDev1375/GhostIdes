package ir.hanzodev1375.ghostide.codeeditors.langs.lsp;

import android.os.Build;
import android.util.Log;
import io.github.rosemoe.sora.lsp.client.languageserver.wrapper.LanguageServerWrapper;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.eclipse.lsp4j.*;
import org.lsposed.hiddenapibypass.HiddenApiBypass;
import top.canyie.pine.Pine;
import top.canyie.pine.callback.MethodHook;

public class LspInitParamsHook {

  private static final String TAG = "LspInitParamsHook";
  private static final AtomicBoolean installed = new AtomicBoolean(false);

  public static void install() {
    if (!installed.compareAndSet(false, true)) {
      return;
    }
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        HiddenApiBypass.addHiddenApiExemptions("");
      }
      Method method = LanguageServerWrapper.class.getDeclaredMethod("getInitParams");
      method.setAccessible(true);
      Pine.hook(
          method,
          new MethodHook() {
            @Override
            public void beforeCall(Pine.CallFrame callFrame) {
              LanguageServerWrapper wrapper = (LanguageServerWrapper) callFrame.thisObject;
              callFrame.setResult(buildInitParams(wrapper));
            }
          });
    } catch (Exception e) {
      installed.set(false);
      Log.e(TAG, "failed to install lsp init params hook", e);
    }
  }

  private static InitializeParams buildInitParams(LanguageServerWrapper wrapper) {
    InitializeParams initParams = new InitializeParams();
    URI projectUri = LspUriBridge.uri(wrapper.getProject()); 
    String rootUri = projectUri.toASCIIString();

    initParams.setRootUri(rootUri);

    WorkspaceClientCapabilities workspaceCapabilities = new WorkspaceClientCapabilities();
    workspaceCapabilities.setApplyEdit(true);
    workspaceCapabilities.setDidChangeWatchedFiles(new DidChangeWatchedFilesCapabilities());
    workspaceCapabilities.setExecuteCommand(new ExecuteCommandCapabilities());
    workspaceCapabilities.setWorkspaceEdit(new WorkspaceEditCapabilities());
    workspaceCapabilities.setSymbol(new SymbolCapabilities());
    workspaceCapabilities.setWorkspaceFolders(true);
    workspaceCapabilities.setConfiguration(false);

    WorkspaceFolder workspaceFolder = new WorkspaceFolder();
    workspaceFolder.setUri(rootUri);
    workspaceFolder.setName(new File(projectUri.getPath()).getName());
    initParams.setWorkspaceFolders(Collections.singletonList(workspaceFolder));

    List<String> markupKinds = Arrays.asList("markdown", "plaintext");

    TextDocumentClientCapabilities textDocumentCapabilities = new TextDocumentClientCapabilities();

    CodeActionCapabilities codeAction = new CodeActionCapabilities();
    CodeActionLiteralSupportCapabilities literalSupport =
        new CodeActionLiteralSupportCapabilities();
    literalSupport.setCodeActionKind(
        new CodeActionKindCapabilities(
            Arrays.asList(
                CodeActionKind.Empty,
                CodeActionKind.QuickFix,
                CodeActionKind.Refactor,
                CodeActionKind.RefactorExtract,
                CodeActionKind.RefactorInline,
                CodeActionKind.RefactorRewrite,
                CodeActionKind.Source,
                CodeActionKind.SourceOrganizeImports,
                CodeActionKind.SourceFixAll)));
    codeAction.setCodeActionLiteralSupport(literalSupport);
    textDocumentCapabilities.setCodeAction(codeAction);

    textDocumentCapabilities.setCompletion(
        new CompletionCapabilities(new CompletionItemCapabilities(true)));
    textDocumentCapabilities.setDefinition(new DefinitionCapabilities());
    textDocumentCapabilities.setDocumentHighlight(new DocumentHighlightCapabilities());
    textDocumentCapabilities.setColorProvider(new ColorProviderCapabilities());
    textDocumentCapabilities.setInlayHint(new InlayHintCapabilities());
    textDocumentCapabilities.setFormatting(new FormattingCapabilities());
    textDocumentCapabilities.setHover(new HoverCapabilities(markupKinds, true));
    textDocumentCapabilities.setOnTypeFormatting(new OnTypeFormattingCapabilities());
    textDocumentCapabilities.setRangeFormatting(new RangeFormattingCapabilities());
    textDocumentCapabilities.setReferences(new ReferencesCapabilities());
    textDocumentCapabilities.setRename(new RenameCapabilities(true, true));
    textDocumentCapabilities.setSignatureHelp(
        new SignatureHelpCapabilities(new SignatureInformationCapabilities(markupKinds), true));
    textDocumentCapabilities.setSynchronization(new SynchronizationCapabilities(true, true, true));
    textDocumentCapabilities.setPublishDiagnostics(new PublishDiagnosticsCapabilities(true));

    initParams.setCapabilities(
        new ClientCapabilities(workspaceCapabilities, textDocumentCapabilities, null));
    initParams.setInitializationOptions(
        wrapper
            .getServerDefinition()
            .getInitializationOptions(projectUri)); 

    return initParams;
  }
}
