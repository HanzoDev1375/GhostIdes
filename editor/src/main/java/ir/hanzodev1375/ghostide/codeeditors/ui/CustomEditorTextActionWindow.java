/*
 * This file is part of CodeOps Studio.
 * CodeOps Studio - Code anywhere anytime
 * https://github.com/euptron/CodeOps-Studio
 * Copyright (C) 2024-2026 Etido Peter
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/
 */

package com.eup.codeopsstudio.editor.langs.widget.component;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.TooltipCompat;

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
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion;
import io.github.rosemoe.sora.widget.component.EditorTextActionWindow;
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme;

import ir.hanzodev1375.ghostide.codeeditors.IdeEditor;
import ir.hanzodev1375.ghostide.codeeditors.R;
import ir.hanzodev1375.ghostide.codeeditors.setting.PreferencesUtils;
import ir.hanzodev1375.ghostide.codeeditors.util.TranslateLanguages;

import ir.hanzodev1375.ghostide.codeeditors.util.TranslateTask;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.ReferenceContext;
import org.eclipse.lsp4j.ReferenceParams;
import org.eclipse.lsp4j.RenameParams;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import org.json.JSONArray;

/**
 * This window will show when selecting text to present text actions.
 *
 * @author Ghost
 */
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
    translateBtn = root.findViewById(R.id.panel_btn_translate); // ← جدید
    lspDefinitionBtn = root.findViewById(R.id.panel_btn_lsp_definition); // ← جدید
    lspReferencesBtn = root.findViewById(R.id.panel_btn_lsp_references); // ← جدید
    lspRenameBtn = root.findViewById(R.id.panel_btn_lsp_rename); // ← جدید

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

    // آیکون های LSP فقط وقتی نشون داده می شن که یک Language Server برای نوع فایل فعلی نصب باشه.
    // نیازی به انتخاب متن نیست؛ رفتن به تعریف/ارجاعات/تغییرنام روی نمادِ زیر مکان نما عمل می کنن.
    boolean lspAvailable = isEditable && editor.isLspAvailableForCurrentFile();
    lspDefinitionBtn.setVisibility(lspAvailable ? View.VISIBLE : View.GONE);
    lspReferencesBtn.setVisibility(lspAvailable ? View.VISIBLE : View.GONE);
    lspRenameBtn.setVisibility(lspAvailable ? View.VISIBLE : View.GONE);

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
      attachTooltip(selectAllBtn, "Select all");
      editor.selectAll();
      return;
    } else if (id == R.id.panel_btn_cut) {
      attachTooltip(cutBtn, "Cut");
      if (editor.getCursor().isSelected()) editor.cutText();
    } else if (id == R.id.panel_btn_paste) {
      attachTooltip(pasteBtn, "Paste");
      editor.pasteText();
      editor.setSelection(editor.getCursor().getRightLine(), editor.getCursor().getRightColumn());
    } else if (id == R.id.panel_btn_copy) {
      attachTooltip(copyBtn, "Copy");
      editor.copyText();
      editor.setSelection(editor.getCursor().getRightLine(), editor.getCursor().getRightColumn());
    } else if (id == R.id.panel_btn_long_select) {
      attachTooltip(longSelectBtn, "Long select");
      editor.beginLongSelect();
    } else if (id == R.id.panel_btn_format) {
      attachTooltip(formatBtn, "Format");
      Cursor cursor = editor.getText().getCursor();
      if (cursor.isSelected()) {
        editor.formatCodeAsync(cursor.left(), cursor.right());
      } else {
        editor.formatCodeAsync();
      }
      return;
    } else if (id == R.id.panel_btn_expand_selection) {
      attachTooltip(expandSelectionBtn, "Expand selection");
      if (editor.getEditable()) {
        // TODO: Handle
      }
    } else if (id == R.id.panel_btn_translate) {
      attachTooltip(translateBtn, "Translate");
      handleTranslate();
      return;
    } else if (id == R.id.panel_btn_lsp_definition) {
      attachTooltip(lspDefinitionBtn, "Go to definition");
      handleGoToDefinition();
      return;
    } else if (id == R.id.panel_btn_lsp_references) {
      attachTooltip(lspReferencesBtn, "Find references");
      handleFindReferences();
      return;
    } else if (id == R.id.panel_btn_lsp_rename) {
      attachTooltip(lspRenameBtn, "Rename symbol");
      handleRenameSymbol();
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
      Toast.makeText(editor.getContext(), "متنی انتخاب نشده است", Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(editor.getContext(), "error: " + error, Toast.LENGTH_SHORT)
                            .show());
              }
            })
        .execute();
  }

  // =====================================================================================
  // ویژگی های LSP: برو به تعریف، یافتن ارجاعات، تغییر نام نماد
  // =====================================================================================

  /** درخواست LSP باید همیشه روی یک ترد پس زمینه اجرا بشه، نه UI thread. */
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
            showLspToast("اتصال به سرور LSP برقرار نشد");
            return;
          }
          try {
            String uri = new File(filePath).toURI().toString();
            DefinitionParams params = new DefinitionParams();
            params.setTextDocument(new TextDocumentIdentifier(uri));
            params.setPosition(new Position(line, column));

            var future = requestManager.definition(params);
            if (future == null) {
              showLspToast("این سرور از «برو به تعریف» پشتیبانی نمی کند");
              return;
            }
            var result = future.get(12, TimeUnit.SECONDS);
            handleLocations(toLocations(result), uri, "تعریفی برای این نماد پیدا نشد");
          } catch (TimeoutException e) {
            showLspToast("پاسخ سرور LSP بیش از حد طول کشید");
          } catch (Exception e) {
            Log.e(TAG, "go to definition failed", e);
            showLspToast("خطا در دریافت تعریف نماد");
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
            showLspToast("اتصال به سرور LSP برقرار نشد");
            return;
          }
          try {
            
            String uri = new File(filePath).toURI().toString();
            ReferenceParams params = new ReferenceParams();
            params.setTextDocument(new TextDocumentIdentifier(uri));
            params.setPosition(new Position(line, column));
            ReferenceContext context = new ReferenceContext();
            context.setIncludeDeclaration(true);
            params.setContext(context);

            var future = requestManager.references(params);
            if (future == null) {
              showLspToast("این سرور از «یافتن ارجاعات» پشتیبانی نمی کند");
              return;
            }
            List<Location> locations = future.get(12, TimeUnit.SECONDS);
            handleLocations(locations, uri, "ارجاعی برای این نماد پیدا نشد");
          } catch (TimeoutException e) {
            showLspToast("پاسخ سرور LSP بیش از حد طول کشید");
          } catch (Exception e) {
            Log.e(TAG, "find references failed", e);
            showLspToast("خطا در یافتن ارجاعات");
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
    new AlertDialog.Builder(editor.getContext())
        .setTitle("تغییر نام نماد")
        .setView(input)
        .setPositiveButton(
            "تغییر نام",
            (dialog, which) -> {
              String newName = input.getText().toString().trim();
              if (newName.isEmpty()) return;
              runLspAction(() -> performRename(filePath, line, column, newName));
            })
        .setNegativeButton("انصراف", null)
        .show();
  }

  private void performRename(String filePath, int line, int column, String newName) {
    LspEditor lsp = editor.ensureLspConnected();
    var requestManager = lsp == null ? null : lsp.getRequestManager();
    if (requestManager == null) {
      showLspToast("اتصال به سرور LSP برقرار نشد");
      return;
    }
    try {
      String uri = new File(filePath).toURI().toString();
      RenameParams params = new RenameParams();
      params.setTextDocument(new TextDocumentIdentifier(uri));
      params.setPosition(new Position(line, column));
      params.setNewName(newName);

      var future = requestManager.rename(params);
      if (future == null) {
        showLspToast("این سرور از «تغییر نام نماد» پشتیبانی نمی کند");
        return;
      }
      WorkspaceEdit edit = future.get(12, TimeUnit.SECONDS);
      if (edit == null) {
        showLspToast("تغییری از سرور برنگشت");
        return;
      }
      applyWorkspaceEdit(edit, uri);
    } catch (TimeoutException e) {
      showLspToast("پاسخ سرور LSP بیش از حد طول کشید");
    } catch (Exception e) {
      Log.e(TAG, "rename symbol failed", e);
      showLspToast("خطا در تغییر نام نماد");
    }
  }

  /**
   * تغییرات WorkspaceEdit برگشتی از سرور رو اعمال می کنه. فقط تغییرات مربوط به فایل بازِ فعلی
   * مستقیم روی ادیتور اعمال می شن؛ چون این پنجره به فایل منیجر/تب های دیگه دسترسی نداره، اگه تغییر
   * نام روی فایل های دیگه هم اثر بذاره فقط با یک پیام به کاربر اطلاع داده می شه (نیاز به اتصال
   * این بخش به فایل منیجر برای اعمال خودکار روی همه ی فایل ها، خارج از اسکوپ همین پنجره است).
   */
  private void applyWorkspaceEdit(WorkspaceEdit edit, String currentUri) {
    Map<String, List<TextEdit>> changes = edit.getChanges();
    if (changes == null || changes.isEmpty()) {
      showLspToast("تغییری برای اعمال پیدا نشد");
      return;
    }

    List<TextEdit> currentFileEdits = changes.get(currentUri);
    int otherFilesCount = changes.size() - (currentFileEdits != null ? 1 : 0);

    if (currentFileEdits != null && !currentFileEdits.isEmpty()) {
      // مرتب سازی نزولی بر اساس موقعیت شروع، تا اعمال یک TextEdit آفستِ بقیه رو خراب نکنه
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
            // Content.replace(startLine, startColumn, endLine, endColumn, CharSequence) — همون
            // متدیه که subContent هم همینجا ازش استفاده می کنه؛ اگه امضاش با نسخه ی فعلی
            // sora-editor فرق داشت، جایگزینش یه insert/delete دستی با همون رنج هست.
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
          otherFilesCount
              + " فایل دیگر هم به این تغییر نیاز دارند؛ اعمال خودکار آن روی فایل های دیگر"
              + " فعلاً پشتیبانی نمی شود");
    }
  }

  /** نتایج Either سرور (Location یا LocationLink) رو به یک لیست یکدست از Location تبدیل می کنه. */
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
    if (locations.size() == 1 && currentUri.equals(locations.get(0).getUri())) {
      jumpToLocation(locations.get(0));
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

  /** برای چند نتیجه (یا نتیجه ای در یک فایل دیگر)، یک لیست انتخابی نشون می ده. */
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
    new AlertDialog.Builder(editor.getContext())
        .setTitle("نتایج")
        .setItems(
            labels,
            (dialog, which) -> {
              Location selected = locations.get(which);
              if (currentUri.equals(selected.getUri())) {
                jumpToLocation(selected);
              } else {
                Toast.makeText(
                        editor.getContext(),
                        "این نتیجه در فایل دیگری است: "
                            + labels[which]
                            + " — باز کردن خودکار فایل های دیگر فعلاً پشتیبانی نمی شود",
                        Toast.LENGTH_LONG)
                    .show();
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
}
