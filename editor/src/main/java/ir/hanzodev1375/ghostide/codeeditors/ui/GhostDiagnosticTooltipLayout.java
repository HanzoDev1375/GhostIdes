package ir.hanzodev1375.ghostide.codeeditors.ui;

import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.blankj.utilcode.util.ClipboardUtils;
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticDetail;
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticRegion;
import io.github.rosemoe.sora.lsp.client.languageserver.requestmanager.DefaultRequestManager;
import io.github.rosemoe.sora.lsp.editor.LspEditor;
import io.github.rosemoe.sora.widget.component.DiagnosticTooltipLayout;
import io.github.rosemoe.sora.widget.component.EditorDiagnosticTooltipWindow;
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme;
import ir.hanzodev1375.ghostide.codeeditors.IdeEditor;
import ir.hanzodev1375.ghostide.codeeditors.R;
import ir.hanzodev1375.ghostide.codeeditors.langs.lsp.LspUriBridge;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import kotlin.Pair;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.CodeActionTriggerKind;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

public class GhostDiagnosticTooltipLayout implements DiagnosticTooltipLayout {

  private static final String TAG = "GhostDiagnosticTooltip";

  private EditorDiagnosticTooltipWindow window;
  private IdeEditor editor;
  private View root;
  private View severityDot;
  private View divider;
  private TextView messageText;
  private TextView copyButton;
  private TextView quickFixesLabel;
  private ProgressBar quickFixesProgress;
  private TextView quickFixesEmptyText;
  private LinearLayout quickFixesList;

  private final AtomicInteger requestGeneration = new AtomicInteger(0);
  private DiagnosticDetail currentDiagnostic;
  private List<Either<Command, CodeAction>> currentActions = new ArrayList<>();
  private boolean menuShowing = false;

  @Override
  public void attach(EditorDiagnosticTooltipWindow window) {
    this.window = window;
    this.editor = (IdeEditor) window.getEditor();
  }

  @Override
  public View createView(LayoutInflater inflater) {
    root = inflater.inflate(R.layout.ghost_diagnostic_tooltip_window, null);
    severityDot = root.findViewById(R.id.diagnostic_severity_dot);
    divider = root.findViewById(R.id.diagnostic_divider);
    messageText = root.findViewById(R.id.diagnostic_message_text);
    copyButton = root.findViewById(R.id.diagnostic_copy_button);
    copyButton.setText(android.R.string.copy);
    copyButton.setOnClickListener(v -> copyMessage());
    quickFixesLabel = root.findViewById(R.id.quick_fixes_label);
    quickFixesLabel.setOnClickListener(v -> showQuickFixesMenu());
    quickFixesProgress = root.findViewById(R.id.quick_fixes_progress);
    quickFixesEmptyText = root.findViewById(R.id.quick_fixes_empty_text);
    quickFixesEmptyText.setText(R.string.lsp_code_action_not_found);
    quickFixesList = root.findViewById(R.id.quick_fixes_list);
    quickFixesList.setVisibility(View.GONE);
    applyColorScheme(editor.getColorScheme());
    return root;
  }

  @Override
  public void applyColorScheme(EditorColorScheme scheme) {
    if (root == null) return;
    GradientDrawable background = new GradientDrawable();
    background.setCornerRadius(10 * editor.getDpUnit());
    background.setColor(scheme.getColor(EditorColorScheme.DIAGNOSTIC_TOOLTIP_BACKGROUND));
    background.setStroke(1, scheme.getColor(EditorColorScheme.COMPLETION_WND_CORNER));
    root.setBackground(background);
    int textColor = scheme.getColor(EditorColorScheme.DIAGNOSTIC_TOOLTIP_DETAILED_MSG);
    messageText.setTextColor(textColor);
    copyButton.setTextColor(textColor);
    quickFixesLabel.setTextColor(textColor);
    quickFixesEmptyText.setTextColor(textColor);
    divider.setBackgroundColor(scheme.getColor(EditorColorScheme.COMPLETION_WND_CORNER));
  }

  @Override
  public void onTextSizeChanged(float oldSizePx, float newSizePx) {}

  @Override
  public void renderDiagnostic(DiagnosticDetail diagnostic) {
    renderDiagnostic(diagnostic, null);
  }

  @Override
  public void renderDiagnostic(DiagnosticDetail diagnostic, DiagnosticRegion region) {
    currentDiagnostic = diagnostic;
    int generation = requestGeneration.incrementAndGet();

    if (diagnostic == null) {
      messageText.setText("");
      setSeverityColor((short) 0);
      showQuickFixesHidden();
      return;
    }

    var message =
        diagnostic.getDetailedMessage() != null && !(diagnostic.getDetailedMessage().length() > 0)
            ? diagnostic.getDetailedMessage()
            : diagnostic.getBriefMessage();
    messageText.setText(message == null ? "" : message);
    setSeverityColor(region == null ? DiagnosticRegion.SEVERITY_NONE : region.severity);

    Object extraData = diagnostic.getExtraData();
    LspEditor lspEditor = editor.getLspEditor();
    if (!(extraData instanceof Diagnostic) || lspEditor == null || !lspEditor.isConnected()) {
      showQuickFixesHidden();
      return;
    }

    loadQuickFixes(lspEditor, (Diagnostic) extraData, generation);
  }

  @Override
  public Pair<Integer, Integer> measureContent(int maxWidth, int maxHeight) {
    root.measure(
        View.MeasureSpec.makeMeasureSpec(maxWidth, View.MeasureSpec.AT_MOST),
        View.MeasureSpec.makeMeasureSpec(maxHeight, View.MeasureSpec.AT_MOST));
    int width = Math.min(root.getMeasuredWidth(), maxWidth);
    int height = Math.min(root.getMeasuredHeight(), maxHeight);
    return new Pair<>(width, height);
  }

  @Override
  public boolean isPointerOverPopup() {
    return menuShowing;
  }

  @Override
  public boolean isMenuShowing() {
    return menuShowing;
  }

  @Override
  public void onWindowDismissed() {
    requestGeneration.incrementAndGet();
    menuShowing = false;
  }

  private void setSeverityColor(short severity) {
    int color;
    switch (severity) {
      case DiagnosticRegion.SEVERITY_ERROR:
        color = 0xFFFB7185;
        break;
      case DiagnosticRegion.SEVERITY_WARNING:
        color = 0xFFFACC15;
        break;
      case DiagnosticRegion.SEVERITY_TYPO:
        color = 0xFF38BDF8;
        break;
      default:
        color = 0xFF94A3B8;
        break;
    }
    GradientDrawable dot = new GradientDrawable();
    dot.setShape(GradientDrawable.OVAL);
    dot.setColor(color);
    severityDot.setBackground(dot);
  }

  private void showQuickFixesHidden() {
    quickFixesProgress.setVisibility(View.GONE);
    quickFixesEmptyText.setVisibility(View.GONE);
    quickFixesLabel.setVisibility(View.GONE);
    currentActions = new ArrayList<>();
  }

  private void showQuickFixesLoading() {
    quickFixesProgress.setVisibility(View.VISIBLE);
    quickFixesEmptyText.setVisibility(View.GONE);
    quickFixesLabel.setVisibility(View.GONE);
    currentActions = new ArrayList<>();
  }

  private void loadQuickFixes(LspEditor lspEditor, Diagnostic diagnostic, int generation) {
    showQuickFixesLoading();
    new Thread(
            () -> {
              List<Either<Command, CodeAction>> actions = new ArrayList<>();
              try {
                String uri = LspUriBridge.uri(lspEditor).toString();
                CodeActionParams params = new CodeActionParams();
                params.setTextDocument(new TextDocumentIdentifier(uri));
                params.setRange(diagnostic.getRange());
                CodeActionContext context =
                    new CodeActionContext(java.util.Collections.singletonList(diagnostic));
                context.setTriggerKind(CodeActionTriggerKind.Invoked);
                params.setContext(context);

                var requestManager = lspEditor.getRequestManager();
                if (requestManager != null) {
                  var future = requestManager.codeAction(params);
                  if (future != null) {
                    List<Either<Command, CodeAction>> result = future.get(8, TimeUnit.SECONDS);
                    if (result != null) actions = result;
                  }
                }
              } catch (Exception e) {
                Log.w(TAG, "failed to load quick fixes", e);
              }
              List<Either<Command, CodeAction>> finalActions = actions;
              editor.postInLifecycle(
                  () -> {
                    if (generation != requestGeneration.get() || currentDiagnostic == null) return;
                    renderQuickFixes(finalActions);
                  });
            },
            "GhostIDE-QuickFixes")
        .start();
  }

  private void renderQuickFixes(List<Either<Command, CodeAction>> actions) {
    List<Either<Command, CodeAction>> items = new ArrayList<>();
    for (Either<Command, CodeAction> action : actions) {
      if (action.isRight() && action.getRight().getDisabled() != null) continue;
      items.add(action);
    }
    items.sort(
        (a, b) -> {
          boolean preferredA = a.isRight() && Boolean.TRUE.equals(a.getRight().getIsPreferred());
          boolean preferredB = b.isRight() && Boolean.TRUE.equals(b.getRight().getIsPreferred());
          return Boolean.compare(preferredB, preferredA);
        });

    currentActions = items;
    quickFixesProgress.setVisibility(View.GONE);

    if (items.isEmpty()) {
      quickFixesLabel.setVisibility(View.GONE);
      quickFixesEmptyText.setVisibility(View.VISIBLE);
      if (window != null) window.getParentView().requestLayout();
      return;
    }

    quickFixesEmptyText.setVisibility(View.GONE);
    quickFixesLabel.setVisibility(View.VISIBLE);
    quickFixesLabel.setText(
        editor.getContext().getString(R.string.code_action) + " (" + items.size() + ")");
    if (window != null) {
      window.getParentView().requestLayout();
    }
  }

  private void showQuickFixesMenu() {
    if (currentActions.isEmpty()) return;
    PopupMenu popupMenu = new PopupMenu(root.getContext(), quickFixesLabel);
    for (int i = 0; i < currentActions.size(); i++) {
      Either<Command, CodeAction> action = currentActions.get(i);
      String title = action.isLeft() ? action.getLeft().getTitle() : action.getRight().getTitle();
      popupMenu.getMenu().add(0, i, i, title == null || title.isEmpty() ? "action" : title);
    }
    popupMenu.setOnMenuItemClickListener(
        item -> {
          int index = item.getItemId();
          if (index >= 0 && index < currentActions.size()) {
            applyAction(currentActions.get(index));
          }
          return true;
        });
    popupMenu.setOnDismissListener(menu -> menuShowing = false);
    menuShowing = true;
    popupMenu.show();
  }

  private void applyAction(Either<Command, CodeAction> action) {
    if (window != null) window.dismiss();
    LspEditor lspEditor = editor.getLspEditor();
    if (lspEditor == null) return;
    if (action.isLeft()) {
      new Thread(() -> executeCommand(lspEditor, action.getLeft()), "GhostIDE-LspAction").start();
      return;
    }
    CodeAction codeAction = action.getRight();
    String uri = LspUriBridge.uri(lspEditor).toString();
    if (codeAction.getEdit() == null && codeAction.getCommand() == null) {
      new Thread(() -> resolveAndApply(lspEditor, codeAction, uri), "GhostIDE-LspAction").start();
      return;
    }
    if (codeAction.getEdit() != null) {
      applyWorkspaceEdit(codeAction.getEdit(), uri);
    }
    if (codeAction.getCommand() != null) {
      new Thread(() -> executeCommand(lspEditor, codeAction.getCommand()), "GhostIDE-LspAction")
          .start();
    }
  }

  private void resolveAndApply(LspEditor lspEditor, CodeAction action, String uri) {
    CodeAction resolved = null;
    for (var manager : lspEditor.getRequestManagers()) {
      if (!(manager instanceof DefaultRequestManager)) continue;
      var service = manager.getTextDocumentService();
      if (service == null) continue;
      try {
        var future = service.resolveCodeAction(action);
        resolved = future.get(12, TimeUnit.SECONDS);
        if (resolved != null) break;
      } catch (Exception e) {
        Log.w(TAG, "resolve code action failed", e);
      }
    }
    if (resolved == null) return;
    if (resolved.getEdit() != null) {
      applyWorkspaceEdit(resolved.getEdit(), uri);
    }
    if (resolved.getCommand() != null) {
      executeCommand(lspEditor, resolved.getCommand());
    }
  }

  private void applyWorkspaceEdit(WorkspaceEdit edit, String currentUri) {
    Map<String, List<TextEdit>> changes = edit.getChanges();
    if (changes == null || changes.isEmpty()) {
      Toast.makeText(editor.getContext(), "این fix تغییری برای اعمال نداشت", Toast.LENGTH_SHORT)
          .show();
      return;
    }
    String currentPath = LspUriBridge.path(currentUri);
    List<TextEdit> currentFileEdits = null;
    for (Map.Entry<String, List<TextEdit>> entry : changes.entrySet()) {
      String entryPath = LspUriBridge.path(entry.getKey());
      if (entryPath != null && entryPath.equals(currentPath)) {
        currentFileEdits = entry.getValue();
        break;
      }
    }
    if (currentFileEdits == null || currentFileEdits.isEmpty()) {
      Toast.makeText(editor.getContext(), "تغییرات مربوط به فایل دیگ ای بود", Toast.LENGTH_SHORT)
          .show();
      return;
    }
    List<TextEdit> sorted = new ArrayList<>(currentFileEdits);
    sorted.sort(
        (a, b) -> {
          int lineCompare =
              Integer.compare(b.getRange().getStart().getLine(), a.getRange().getStart().getLine());
          if (lineCompare != 0) return lineCompare;
          return Integer.compare(
              b.getRange().getStart().getCharacter(), a.getRange().getStart().getCharacter());
        });
    editor.postInLifecycle(
        () -> {
          for (TextEdit textEdit : sorted) {
            Position start = textEdit.getRange().getStart();
            Position end = textEdit.getRange().getEnd();
            editor
                .getText()
                .replace(
                    start.getLine(),
                    start.getCharacter(),
                    end.getLine(),
                    end.getCharacter(),
                    textEdit.getNewText());
          }
        });
  }

  private void executeCommand(LspEditor lspEditor, Command command) {
    if (command == null || command.getCommand() == null) return;
    if (CodeRunnerActions.execute(editor, command.getCommand(), command.getArguments())) {
      return;
    }
    var requestManager = lspEditor.getRequestManager();
    if (requestManager == null || command == null) return;
    try {
      ExecuteCommandParams params = new ExecuteCommandParams();
      params.setCommand(command.getCommand());
      params.setArguments(command.getArguments());
      var future = requestManager.executeCommand(params);
      if (future != null) {
        future.get(12, TimeUnit.SECONDS);
      }
    } catch (Exception e) {
      Log.w(TAG, "execute command failed", e);
    }
  }

  private void copyMessage() {
    CharSequence text = messageText.getText();
    ClipboardUtils.copyText(text);
  }
}
