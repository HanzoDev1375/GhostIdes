package ir.hanzodev1375.ghostide.codeeditors.ui;

import android.annotation.SuppressLint;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.TooltipCompat;
import ir.hanzodev1375.components.sheet.customitemsheet.ui.DialogCompat;
import io.github.rosemoe.sora.event.ColorSchemeUpdateEvent;
import io.github.rosemoe.sora.event.HandleStateChangeEvent;
import io.github.rosemoe.sora.event.InterceptTarget;
import io.github.rosemoe.sora.event.LongPressEvent;
import io.github.rosemoe.sora.event.ScrollEvent;
import io.github.rosemoe.sora.event.SelectionChangeEvent;
import io.github.rosemoe.sora.event.Unsubscribe;
import io.github.rosemoe.sora.lsp.editor.LspEditor;
import io.github.rosemoe.sora.text.Cursor;
import io.github.rosemoe.sora.widget.EditorTouchEventHandler;
import io.github.rosemoe.sora.widget.component.EditorTextActionWindow;
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme;
import ir.hanzodev1375.ghostide.codeeditors.IdeEditor;
import ir.hanzodev1375.ghostide.codeeditors.R;
import ir.hanzodev1375.ghostide.codeeditors.langs.lsp.AndroidClasspathResolver;
import ir.hanzodev1375.ghostide.codeeditors.setting.PreferencesUtils;
import ir.hanzodev1375.ghostide.codeeditors.ui.model.OpenFileLocationEvent;
import ir.hanzodev1375.ghostide.codeeditors.util.TranslateTask;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.ReferenceContext;
import org.eclipse.lsp4j.ReferenceParams;
import org.eclipse.lsp4j.RenameParams;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.greenrobot.eventbus.EventBus;

public class CustomEditorTextActionWindow extends EditorTextActionWindow {

  private static final String TAG = "CustomEditorTextActionWindow";
  private static final long DELAY = 200;
  private final IdeEditor editor;
  private final ImageButton pasteBtn;
  private final ImageButton copyBtn;
  private final ImageButton cutBtn;
  private final ImageButton selectAllBtn;
  private final ImageButton longSelectBtn;
  private final ImageButton expandSelectionBtn;
  private final ImageButton formatBtn;
  private final ImageButton translateBtn;
  private final ImageButton lspDefinitionBtn;
  private final ImageButton lspReferencesBtn;
  private final ImageButton lspRenameBtn;
  private final ImageButton lspCodeActionBtn;
  private final View rootView;
  private final EditorTouchEventHandler handler;
  private long lastScroll;
  private int lastPosition;
  private int lastCause;

  private boolean enabled = true;
  private int windowCornerRadius = 8;

  public CustomEditorTextActionWindow(IdeEditor editor) {
    super(editor);
    this.editor = editor;
    handler = editor.getEventHandler();

    @SuppressLint("InflateParams")
    View root =
        LayoutInflater.from(editor.getContext())
            .inflate(R.layout.contextual_text_compose_panel, null);

    pasteBtn = root.findViewById(R.id.panel_btn_paste);
    copyBtn = root.findViewById(R.id.panel_btn_copy);
    cutBtn = root.findViewById(R.id.panel_btn_cut);
    selectAllBtn = root.findViewById(R.id.panel_btn_select_all);
    longSelectBtn = root.findViewById(R.id.panel_btn_long_select);
    expandSelectionBtn = root.findViewById(R.id.panel_btn_expand_selection);
    formatBtn = root.findViewById(R.id.panel_btn_format);
    translateBtn = root.findViewById(R.id.panel_btn_translate);
    lspDefinitionBtn = root.findViewById(R.id.panel_btn_lsp_definition);
    lspReferencesBtn = root.findViewById(R.id.panel_btn_lsp_references);
    lspRenameBtn = root.findViewById(R.id.panel_btn_lsp_rename);
    lspCodeActionBtn = root.findViewById(R.id.panel_btn_lsp_code_action);

    pasteBtn.setOnClickListener(this);
    copyBtn.setOnClickListener(this);
    cutBtn.setOnClickListener(this);
    selectAllBtn.setOnClickListener(this);
    longSelectBtn.setOnClickListener(this);
    expandSelectionBtn.setOnClickListener(this);
    formatBtn.setOnClickListener(this);
    translateBtn.setOnClickListener(this);
    lspDefinitionBtn.setOnClickListener(this);
    lspReferencesBtn.setOnClickListener(this);
    lspRenameBtn.setOnClickListener(this);
    lspCodeActionBtn.setOnClickListener(this);
    applyColorScheme(root, editor.getColorScheme());
    editor.subscribeEvent(
        ColorSchemeUpdateEvent.class,
        (event, unsubscribe) -> applyColorScheme(root, event.getColorScheme()));

    setContentView(root);
    setSize(0, (int) (this.editor.getDpUnit() * 48));
    rootView = root;

    editor.subscribeEvent(
        ScrollEvent.class,
        ((event, unsubscribe) -> {
          long last = lastScroll;
          lastScroll = System.currentTimeMillis();
          if (lastScroll - last < DELAY && lastCause != SelectionChangeEvent.CAUSE_SEARCH) {
            runPostDisplay();
          }
        }));
    editor.subscribeEvent(
        HandleStateChangeEvent.class,
        ((event, unsubscribe) -> {
          if (event.isHeld()) {
            runPostDisplay();
          }
        }));
    editor.subscribeEvent(
        LongPressEvent.class,
        ((event, unsubscribe) -> {
          if (editor.getCursor().isSelected() && lastCause == SelectionChangeEvent.CAUSE_SEARCH) {
            int idx = event.getIndex();
            if (idx >= editor.getCursor().getLeft() && idx <= editor.getCursor().getRight()) {
              lastCause = 0;
              displayWindow();
            }
            event.intercept(InterceptTarget.TARGET_EDITOR);
          }
        }));
    editor.subscribeEvent(
        HandleStateChangeEvent.class,
        ((event, unsubscribe) -> {
          if (!event.getEditor().getCursor().isSelected()
              && event.getHandleType() == HandleStateChangeEvent.HANDLE_TYPE_INSERT
              && !event.isHeld()) {
            displayWindow();
            editor.postDelayedInLifecycle(
                new Runnable() {
                  @Override
                  public void run() {
                    if (!editor.getEventHandler().shouldDrawInsertHandle()
                        && !editor.getCursor().isSelected()) {
                      dismiss();
                    } else if (!editor.getCursor().isSelected()) {
                      editor.postDelayedInLifecycle(this, 100);
                    }
                  }
                },
                100);
          }
        }));

    getPopup().setAnimationStyle(io.github.rosemoe.sora.R.style.text_action_popup_animation);
  }

  private void applyColorScheme(View root, EditorColorScheme scheme) {
    GradientDrawable gd = new GradientDrawable();
    gd.setCornerRadius(windowCornerRadius * editor.getDpUnit());
    gd.setColor(scheme.getColor(EditorColorScheme.COMPLETION_WND_BACKGROUND));
    gd.setStroke(1, scheme.getColor(EditorColorScheme.COMPLETION_WND_CORNER));
    root.setBackground(gd);

    int textColor = scheme.getColor(EditorColorScheme.COMPLETION_WND_TEXT_SECONDARY);
    setColorFilterById(textColor, pasteBtn);
    setColorFilterById(textColor, copyBtn);
    setColorFilterById(textColor, cutBtn);
    setColorFilterById(textColor, expandSelectionBtn);
    setColorFilterById(textColor, longSelectBtn);
    setColorFilterById(textColor, formatBtn);
    setColorFilterById(textColor, selectAllBtn);
    setColorFilterById(textColor, translateBtn);
    setColorFilterById(textColor, lspDefinitionBtn);
    setColorFilterById(textColor, lspReferencesBtn);
    setColorFilterById(textColor, lspRenameBtn);
    setColorFilterById(textColor, lspCodeActionBtn);
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
    if (!enabled) dismiss();
  }

  @Override
  public ViewGroup getView() {
    return (ViewGroup) getPopup().getContentView();
  }

  public void onReceive(@NonNull SelectionChangeEvent event, @NonNull Unsubscribe unsubscribe) {
    if (handler.hasAnyHeldHandle()) return;
    lastCause = event.getCause();

    if (event.isSelected()) {
      if (event.getCause() != SelectionChangeEvent.CAUSE_SEARCH) {
        editor.postInLifecycle(this::displayWindow);
      } else {
        dismiss();
      }
      lastPosition = -1;
    } else {
      boolean show = false;
      if (event.getCause() == SelectionChangeEvent.CAUSE_TAP
          && event.getLeft().index == lastPosition
          && !isShowing()
          && !editor.getText().isInBatchEdit()
          && editor.isEditable()) {
        editor.postInLifecycle(this::displayWindow);
        show = true;
      } else {
        dismiss();
      }
      if (event.getCause() == SelectionChangeEvent.CAUSE_TAP && !show) {
        lastPosition = event.getLeft().index;
      } else {
        lastPosition = -1;
      }
    }
  }

  @Override
  public void displayWindow() {
    updateButtonState();
    int top;
    Cursor cursor = editor.getCursor();
    if (cursor.isSelected()) {
      RectF leftRect = editor.getLeftHandleDescriptor().position;
      RectF rightRect = editor.getRightHandleDescriptor().position;
      top = Math.min(selectTopRect(leftRect), selectTopRect(rightRect));
    } else {
      top = selectTopRect(editor.getInsertHandleDescriptor().position);
    }
    top = Math.max(0, Math.min(top, editor.getHeight() - getHeight() - 5));
    float handleLeftX =
        editor.getOffset(editor.getCursor().getLeftLine(), editor.getCursor().getLeftColumn());
    float handleRightX =
        editor.getOffset(editor.getCursor().getRightLine(), editor.getCursor().getRightColumn());
    int panelX = (int) ((handleLeftX + handleRightX) / 2f - rootView.getMeasuredWidth() / 2f);
    setLocationAbsolutely(panelX, top);
    show();
  }

  private int selectTopRect(@NonNull RectF rect) {
    int rowHeight = editor.getRowHeight();
    if (rect.top - rowHeight * 3 / 2F > getHeight()) {
      return (int) (rect.top - (float) (rowHeight * 3) / 2 - getHeight());
    } else {
      return (int) (rect.bottom + (float) rowHeight / 2);
    }
  }

  private void updateButtonState() {
    pasteBtn.setEnabled(editor.hasClip());
    boolean isSelected = editor.getCursor().isSelected();
    boolean isEditable = editor.isEditable();

    copyBtn.setVisibility(isSelected ? View.VISIBLE : View.GONE);
    formatBtn.setVisibility(View.VISIBLE);
    cutBtn.setVisibility((isSelected && isEditable) ? View.VISIBLE : View.GONE);
    pasteBtn.setVisibility(isEditable ? View.VISIBLE : View.GONE);
    longSelectBtn.setVisibility((!isSelected && isEditable) ? View.VISIBLE : View.GONE);
    expandSelectionBtn.setVisibility(View.GONE);
    translateBtn.setVisibility(isSelected ? View.VISIBLE : View.GONE);

    boolean lspAvailable = isEditable && editor.isLspAvailableForCurrentFile();
    lspDefinitionBtn.setVisibility(lspAvailable ? View.VISIBLE : View.GONE);
    lspReferencesBtn.setVisibility(lspAvailable ? View.VISIBLE : View.GONE);
    lspRenameBtn.setVisibility(lspAvailable ? View.VISIBLE : View.GONE);
    lspCodeActionBtn.setVisibility(lspAvailable ? View.VISIBLE : View.GONE);

    rootView.measure(
        View.MeasureSpec.makeMeasureSpec(1000000, View.MeasureSpec.AT_MOST),
        View.MeasureSpec.makeMeasureSpec(100000, View.MeasureSpec.AT_MOST));
    setSize(Math.min(rootView.getMeasuredWidth(), (int) (editor.getDpUnit() * 230)), getHeight());
  }

  @Override
  public void show() {
    if (!enabled || editor.getSnippetController().isInSnippet()) return;
    super.show();
  }

  @Override
  public void onClick(@NonNull View view) {
    int id = view.getId();
    if (id == R.id.panel_btn_select_all) {
      attachTooltip(selectAllBtn, editor.getContext().getString(R.string.editor_select_all));
      editor.selectAll();
      return;
    } else if (id == R.id.panel_btn_cut) {
      attachTooltip(cutBtn, editor.getContext().getString(R.string.editor_cut_text));
      if (editor.getCursor().isSelected()) editor.cutText();
    } else if (id == R.id.panel_btn_paste) {
      attachTooltip(pasteBtn, editor.getContext().getString(R.string.editor_paste_text));
      editor.pasteText();
      editor.setSelection(editor.getCursor().getRightLine(), editor.getCursor().getRightColumn());
    } else if (id == R.id.panel_btn_copy) {
      attachTooltip(copyBtn, editor.getContext().getString(R.string.editor_copy_text));
      editor.copyText();
      editor.setSelection(editor.getCursor().getRightLine(), editor.getCursor().getRightColumn());
    } else if (id == R.id.panel_btn_long_select) {
      attachTooltip(longSelectBtn, editor.getContext().getString(R.string.editor_long_select));
      editor.beginLongSelect();
    } else if (id == R.id.panel_btn_format) {
      attachTooltip(formatBtn, editor.getContext().getString(R.string.editor_format));
      Cursor cursor = editor.getText().getCursor();
      if (cursor.isSelected()) {
        editor.formatCodeAsync(cursor.left(), cursor.right());
      } else {
        editor.formatCodeAsync();
      }
      return;
    } else if (id == R.id.panel_btn_expand_selection) {
      attachTooltip(
          expandSelectionBtn, editor.getContext().getString(R.string.editor_expand_selection));
      if (editor.getEditable()) {
        // TODO: Handle
      }
    } else if (id == R.id.panel_btn_translate) {
      attachTooltip(translateBtn, editor.getContext().getString(R.string.editor_translate));
      handleTranslate();
      return;
    } else if (id == R.id.panel_btn_lsp_definition) {
      attachTooltip(
          lspDefinitionBtn, editor.getContext().getString(R.string.editor_lsp_definition));
      handleGoToDefinition();
      return;
    } else if (id == R.id.panel_btn_lsp_references) {
      attachTooltip(
          lspReferencesBtn, editor.getContext().getString(R.string.editor_lsp_references));
      handleFindReferences();
      return;
    } else if (id == R.id.panel_btn_lsp_rename) {
      attachTooltip(lspRenameBtn, editor.getContext().getString(R.string.editor_lsp_rename));
      handleRenameSymbol();
      return;
    } else if (id == R.id.panel_btn_lsp_code_action) {
      attachTooltip(lspCodeActionBtn, editor.getContext().getString(R.string.code_action));
      handleCodeAction();
      return;
    }
    dismiss();
  }

  public String getSelectedText() {
    Cursor cursor = editor.getCursor();
    return editor
        .getText()
        .subContent(
            cursor.getLeftLine(),
            cursor.getLeftColumn(),
            cursor.getRightLine(),
            cursor.getRightColumn())
        .toString();
  }

  private void handleTranslate() {
    String selectedText = getSelectedText();
    if (selectedText == null || selectedText.isEmpty()) {
      Toast.makeText(editor.getContext(), R.string.dontselecttext, Toast.LENGTH_SHORT).show();
      dismiss();
      return;
    }

    PreferencesUtils prefs = new PreferencesUtils(editor.getContext());
    String targetLang = prefs.getTranslateTargetLang();

    dismiss();
    new TranslateTask(
            selectedText,
            targetLang,
            new TranslateTask.Callback() {
              @Override
              public void onSuccess(String translatedText) {
                editor.post(
                    () -> {
                      if (editor.getCursor().isSelected()) {
                        editor.deleteText();
                      }
                      editor.insertText(translatedText, translatedText.length());
                    });
              }

              @Override
              public void onFailure(String error) {
                editor.post(
                    () ->
                        Toast.makeText(
                                editor.getContext(),
                                editor.getContext().getString(R.string.lsp_translate_error_prefix)
                                    + error,
                                Toast.LENGTH_SHORT)
                            .show());
              }
            })
        .execute();
  }

  private void runLspAction(Runnable backgroundWork) {
    new Thread(backgroundWork, "GhostIDE-LspAction").start();
  }

  private void showLspToast(String message) {
    editor.postInLifecycle(
        () -> Toast.makeText(editor.getContext(), message, Toast.LENGTH_SHORT).show());
  }

  private void handleGoToDefinition() {
    final int line = editor.getCursor().getLeftLine();
    final int column = editor.getCursor().getLeftColumn();
    final String filePath = editor.getCurrentFilePath();
    dismiss();
    if (filePath == null) return;

    runLspAction(
        () -> {
          LspEditor lsp = editor.ensureLspConnected();
          var requestManager = lsp == null ? null : lsp.getRequestManager();
          if (requestManager == null) {
            showLspToast(editor.getContext().getString(R.string.error_toconnectedlsp));
            return;
          }
          try {
            String uri = getFileUri(filePath);
            DefinitionParams params = new DefinitionParams();
            params.setTextDocument(new TextDocumentIdentifier(uri));
            params.setPosition(new Position(line, column));

            var future = requestManager.definition(params);
            if (future == null) {
              showLspToast(editor.getContext().getString(R.string.lsp_definition_not_supported));
              return;
            }
            var result = future.get(12, TimeUnit.SECONDS);
            handleLocations(
                toLocations(result),
                uri,
                editor.getContext().getString(R.string.lsp_definition_not_found));
          } catch (TimeoutException e) {
            showLspToast(editor.getContext().getString(R.string.lsp_timeout));
          } catch (Exception e) {
            Log.e(TAG, "go to definition failed", e);
            showLspToast(editor.getContext().getString(R.string.lsp_definition_failed));
          }
        });
  }

  private void handleFindReferences() {
    final int line = editor.getCursor().getLeftLine();
    final int column = editor.getCursor().getLeftColumn();
    final String filePath = editor.getCurrentFilePath();
    dismiss();
    if (filePath == null) return;

    runLspAction(
        () -> {
          LspEditor lsp = editor.ensureLspConnected();
          var requestManager = lsp == null ? null : lsp.getRequestManager();
          if (requestManager == null) {
            showLspToast(editor.getContext().getString(R.string.error_toconnectedlsp));
            return;
          }
          try {

            String uri = getFileUri(filePath);
            ReferenceParams params = new ReferenceParams();
            params.setTextDocument(new TextDocumentIdentifier(uri));
            params.setPosition(new Position(line, column));
            ReferenceContext context = new ReferenceContext();
            context.setIncludeDeclaration(true);
            params.setContext(context);

            var future = requestManager.references(params);
            if (future == null) {
              showLspToast(editor.getContext().getString(R.string.lsp_references_not_supported));
              return;
            }
            List<Location> locations = future.get(12, TimeUnit.SECONDS);
            handleLocations(
                locations, uri, editor.getContext().getString(R.string.lsp_references_not_found));
          } catch (TimeoutException e) {
            showLspToast(editor.getContext().getString(R.string.lsp_timeout));
          } catch (Exception e) {
            Log.e(TAG, "find references failed", e);
            showLspToast(editor.getContext().getString(R.string.lsp_references_failed));
          }
        });
  }

  private void handleRenameSymbol() {
    final int line = editor.getCursor().getLeftLine();
    final int column = editor.getCursor().getLeftColumn();
    final String filePath = editor.getCurrentFilePath();
    dismiss();
    if (filePath == null) return;

    EditText input = new EditText(editor.getContext());
    String selected = getSelectedText();
    if (selected != null && !selected.isEmpty()) {
      input.setText(selected);
      input.setSelection(0, selected.length());
    }
    new DialogCompat(editor.getContext())
        .setTitle(R.string.lsp_rename_dialog_title)
        .setView(input)
        .setPositiveButton(
            android.R.string.ok,
            (dialog, which) -> {
              String newName = input.getText().toString().trim();
              if (newName.isEmpty()) return;
              runLspAction(() -> performRename(filePath, line, column, newName));
            })
        .setNegativeButton(R.string.lsp_cancel, null)
        .show();
  }

  private void performRename(String filePath, int line, int column, String newName) {
    LspEditor lsp = editor.ensureLspConnected();
    var requestManager = lsp == null ? null : lsp.getRequestManager();
    if (requestManager == null) {
      showLspToast(editor.getContext().getString(R.string.error_toconnectedlsp));
      return;
    }
    try {
      String uri = getFileUri(filePath);
      RenameParams params = new RenameParams();
      params.setTextDocument(new TextDocumentIdentifier(uri));
      params.setPosition(new Position(line, column));
      params.setNewName(newName);

      var future = requestManager.rename(params);
      if (future == null) {
        showLspToast(editor.getContext().getString(R.string.lsp_rename_not_supported));
        return;
      }
      WorkspaceEdit edit = future.get(12, TimeUnit.SECONDS);
      if (edit == null) {
        showLspToast(editor.getContext().getString(R.string.lsp_rename_no_changes));
        return;
      }
      applyWorkspaceEdit(edit, uri);
    } catch (TimeoutException e) {
      showLspToast(editor.getContext().getString(R.string.lsp_timeout));
    } catch (Exception e) {
      Log.e(TAG, "rename symbol failed", e);
      showLspToast(editor.getContext().getString(R.string.lsp_rename_failed));
    }
  }

  private void applyWorkspaceEdit(WorkspaceEdit edit, String currentUri) {
    Map<String, List<TextEdit>> changes = edit.getChanges();
    if (changes == null || changes.isEmpty()) {
      showLspToast(editor.getContext().getString(R.string.lsp_no_edits_to_apply));
      return;
    }

    String normalizedCurrent = normalizeUri(currentUri);
    List<TextEdit> currentFileEdits = null;
    for (Map.Entry<String, List<TextEdit>> entry : changes.entrySet()) {
      if (normalizeUri(entry.getKey()).equals(normalizedCurrent)) {
        currentFileEdits = entry.getValue();
        break;
      }
    }

    int otherFilesCount = changes.size() - (currentFileEdits != null ? 1 : 0);

    if (currentFileEdits != null && !currentFileEdits.isEmpty()) {
      List<TextEdit> sorted = new ArrayList<>(currentFileEdits);
      sorted.sort(
          (a, b) -> {
            int lineCompare =
                Integer.compare(
                    b.getRange().getStart().getLine(), a.getRange().getStart().getLine());
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

    if (otherFilesCount > 0) {
      showLspToast(
          editor.getContext().getString(R.string.lsp_other_files_need_update, otherFilesCount));
    }
  }

  private void handleCodeAction() {
    Cursor cursor = editor.getCursor();
    final int startLine = cursor.getLeftLine();
    final int startColumn = cursor.getLeftColumn();
    final int endLine = cursor.getRightLine();
    final int endColumn = cursor.getRightColumn();
    final String filePath = editor.getCurrentFilePath();
    dismiss();
    if (filePath == null) return;

    runLspAction(
        () -> {
          LspEditor lsp = editor.ensureLspConnected();
          var requestManager = lsp == null ? null : lsp.getRequestManager();
          if (requestManager == null) {
            showLspToast(editor.getContext().getString(R.string.error_toconnectedlsp));
            return;
          }
          try {
            String uri = getFileUri(filePath);
            Range range =
                new Range(new Position(startLine, startColumn), new Position(endLine, endColumn));

            List<Diagnostic> fileDiagnostics = lsp.getDiagnostics();
            List<Diagnostic> rangeDiagnostics = new ArrayList<>();
            if (fileDiagnostics != null) {
              for (Diagnostic d : fileDiagnostics) {
                if (rangesOverlap(d.getRange(), range)) {
                  rangeDiagnostics.add(d);
                }
              }
            }
            CodeActionParams params = new CodeActionParams();
            params.setTextDocument(new TextDocumentIdentifier(uri));
            params.setRange(range);
            params.setContext(new CodeActionContext(rangeDiagnostics));

            var future = requestManager.codeAction(params);
            if (future == null) {
              showLspToast(editor.getContext().getString(R.string.lsp_code_action_not_supported));
              return;
            }
            List<Either<Command, CodeAction>> actions = future.get(12, TimeUnit.SECONDS);
            if (actions == null || actions.isEmpty()) {
              showLspToast(editor.getContext().getString(R.string.lsp_code_action_not_found));
              return;
            }
            if (actions.size() == 1) {
              applyCodeAction(actions.get(0), uri);
            } else {
              editor.postInLifecycle(() -> showCodeActionsPicker(actions, uri));
            }
          } catch (TimeoutException e) {
            showLspToast(editor.getContext().getString(R.string.lsp_timeout));
          } catch (Exception e) {
            Log.e(TAG, "code action failed", e);
            showLspToast(editor.getContext().getString(R.string.lsp_code_action_failed));
          }
        });
  }

  private boolean rangesOverlap(Range a, Range b) {
    Position aStart = a.getStart();
    Position aEnd = a.getEnd();
    Position bStart = b.getStart();
    Position bEnd = b.getEnd();
    boolean aBeforeB =
        aEnd.getLine() < bStart.getLine()
            || (aEnd.getLine() == bStart.getLine() && aEnd.getCharacter() < bStart.getCharacter());
    boolean bBeforeA =
        bEnd.getLine() < aStart.getLine()
            || (bEnd.getLine() == aStart.getLine() && bEnd.getCharacter() < aStart.getCharacter());
    return !aBeforeB && !bBeforeA;
  }

  private void showCodeActionsPicker(List<Either<Command, CodeAction>> actions, String uri) {
    String[] labels = new String[actions.size()];
    for (int i = 0; i < actions.size(); i++) {
      Either<Command, CodeAction> item = actions.get(i);
      labels[i] = item.isRight() ? item.getRight().getTitle() : item.getLeft().getTitle();
    }
    new DialogCompat(editor.getContext())
        .setTitle(R.string.code_action)
        .setItems(labels, (dialog, which) -> applyCodeAction(actions.get(which), uri))
        .show();
  }

  private void applyCodeAction(Either<Command, CodeAction> item, String uri) {
    if (item.isRight()) {
      CodeAction action = item.getRight();
      if (action.getEdit() == null && action.getCommand() == null) {
        runLspAction(() -> resolveAndApplyCodeAction(action, uri));
        return;
      }
      boolean handled = false;
      if (action.getEdit() != null) {
        applyWorkspaceEdit(action.getEdit(), uri);
        handled = true;
      }
      if (action.getCommand() != null) {
        runLspAction(() -> executeLspCommand(action.getCommand()));
        handled = true;
      }
      if (!handled) {
        showLspToast(editor.getContext().getString(R.string.lsp_code_action_not_supported_2));
      }
    } else if (item.isLeft()) {
      runLspAction(() -> executeLspCommand(item.getLeft()));
    }
  }

  private void resolveAndApplyCodeAction(CodeAction action, String uri) {
    LspEditor lsp = editor.ensureLspConnected();
    if (lsp == null) {
      showLspToast(editor.getContext().getString(R.string.error_toconnectedlsp));
      return;
    }
    CodeAction resolved = null;
    for (var manager : lsp.getRequestManagers()) {
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
    if (resolved == null) {
      showLspToast(editor.getContext().getString(R.string.lsp_code_action_not_supported_2));
      return;
    }
    boolean handled = false;
    if (resolved.getEdit() != null) {
      applyWorkspaceEdit(resolved.getEdit(), uri);
      handled = true;
    }
    if (resolved.getCommand() != null) {
      executeLspCommand(resolved.getCommand());
      handled = true;
    }
    if (!handled) {
      showLspToast(editor.getContext().getString(R.string.lsp_code_action_not_supported_2));
    }
  }

  private void executeLspCommand(Command command) {
    if (command != null
        && command.getCommand() != null
        && CodeRunnerActions.execute(editor, command.getCommand(), command.getArguments())) {
      return;
    }
    LspEditor lsp = editor.ensureLspConnected();
    var requestManager = lsp == null ? null : lsp.getRequestManager();
    if (requestManager == null) {
      showLspToast(editor.getContext().getString(R.string.error_toconnectedlsp));
      return;
    }
    try {
      ExecuteCommandParams params = new ExecuteCommandParams();
      params.setCommand(command.getCommand());
      params.setArguments(command.getArguments());
      var future = requestManager.executeCommand(params);
      if (future == null) {
        showLspToast(editor.getContext().getString(R.string.lsp_execute_command_not_supported));
        return;
      }
      future.get(12, TimeUnit.SECONDS);
    } catch (TimeoutException e) {
      showLspToast(editor.getContext().getString(R.string.lsp_timeout));
    } catch (Exception e) {
      Log.e(TAG, "execute command failed", e);
      showLspToast(editor.getContext().getString(R.string.lsp_execute_command_failed));
    }
  }

  private static List<Location> toLocations(Either<List<Location>, List<LocationLink>> either) {
    List<Location> result = new ArrayList<>();
    if (either == null) return result;
    if (either.isLeft() && either.getLeft() != null) {
      result.addAll(either.getLeft());
    } else if (either.isRight() && either.getRight() != null) {
      for (LocationLink link : either.getRight()) {
        Location location = new Location();
        location.setUri(link.getTargetUri());
        location.setRange(
            link.getTargetSelectionRange() != null
                ? link.getTargetSelectionRange()
                : link.getTargetRange());
        result.add(location);
      }
    }
    return result;
  }

  private void handleLocations(List<Location> locations, String currentUri, String emptyMessage) {
    if (locations == null || locations.isEmpty()) {
      showLspToast(emptyMessage);
      return;
    }
    if (locations.size() == 1) {
      Location loc = locations.get(0);
      if (currentUri.equals(loc.getUri())) {
        jumpToLocation(loc);
      } else {
        String filePath = uriToFilePath(loc.getUri());
        if (filePath != null) {
          Position pos = loc.getRange().getStart();
          EventBus.getDefault()
              .post(new OpenFileLocationEvent(filePath, pos.getLine(), pos.getCharacter()));
        } else {
          showLspToast(editor.getContext().getString(R.string.lsp_invalid_file_path));
        }
      }
      return;
    }
    editor.postInLifecycle(() -> showLocationsPicker(locations, currentUri));
  }

  private void jumpToLocation(Location location) {
    editor.postInLifecycle(
        () -> {
          if (location.getRange() == null) return;
          Position start = location.getRange().getStart();
          editor.setSelection(start.getLine(), start.getCharacter());
          editor.ensureSelectionVisible();
        });
  }

  private void showLocationsPicker(List<Location> locations, String currentUri) {
    String[] labels = new String[locations.size()];
    for (int i = 0; i < locations.size(); i++) {
      Location location = locations.get(i);
      String name;
      try {
        String path = URI.create(location.getUri()).getPath();
        name = path != null ? new File(path).getName() : location.getUri();
      } catch (Exception e) {
        name = location.getUri();
      }
      int line = location.getRange() != null ? location.getRange().getStart().getLine() + 1 : 0;
      labels[i] = name + "  :  " + line;
    }
    new DialogCompat(editor.getContext())
        .setTitle(R.string.lsp_result_dialog_title)
        .setItems(
            labels,
            (dialog, which) -> {
              Location selected = locations.get(which);
              String selectedUri = selected.getUri();
              if (currentUri.equals(selectedUri)) {
                jumpToLocation(selected);
              } else {
                String filePath = uriToFilePath(selectedUri);
                if (filePath != null) {
                  Position pos = selected.getRange().getStart();
                  EventBus.getDefault()
                      .post(new OpenFileLocationEvent(filePath, pos.getLine(), pos.getCharacter()));
                } else {
                  Toast.makeText(
                          editor.getContext(),
                          editor.getContext().getString(R.string.lsp_invalid_file_path),
                          Toast.LENGTH_SHORT)
                      .show();
                }
              }
            })
        .show();
  }

  private void attachTooltip(View anchor, String text) {
    TooltipCompat.setTooltipText(anchor, text);
  }

  public void setWindowCornerRadius(final int radius) {
    windowCornerRadius = radius;
  }

  private void runPostDisplay() {
    if (!isShowing()) return;
    dismiss();
    if (!editor.getCursor().isSelected()) return;
    editor.postDelayedInLifecycle(
        new Runnable() {
          @Override
          public void run() {
            if (!handler.hasAnyHeldHandle()
                && !editor.getSnippetController().isInSnippet()
                && System.currentTimeMillis() - lastScroll > DELAY
                && editor.getScroller().isFinished()) {
              displayWindow();
            } else {
              editor.postDelayedInLifecycle(this, DELAY);
            }
          }
        },
        DELAY);
  }

  void setColorFilterById(int color, ImageButton btn) {
    btn.setColorFilter(color);
  }

  private String normalizeUri(String uri) {
    if (uri == null) return null;
    try {
      return URI.create(uri).normalize().toString();
    } catch (Exception e) {
      return uri;
    }
  }

  private String getFileUri(String filePath) {
    if (filePath == null) return null;
    
    return "file://" + new File(filePath).getAbsolutePath();
  }

  private String uriToFilePath(String uri) {
    try {
      String path = URI.create(uri).getPath();
      if (path == null) return null;
      // LSP داخل proot با ریشهی rootfs اجرا میشه؛ مسیر locationش guest هست و باید به مسیر
      // host روی اندروید نگاشت بشه، وگرنه فایل پیدا نمیشه و ادیتور خالی باز میشه.
      return AndroidClasspathResolver.toHostPath(editor.getContext(), path);
    } catch (Exception e) {
      return null;
    }
  }
}
