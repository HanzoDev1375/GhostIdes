package ir.hanzodev1375.ghostide.customui;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

import android.widget.LinearLayout;
import android.widget.PopupMenu;
import ir.hanzodev1375.components.views.GhostToast;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.core.util.Supplier;
import androidx.transition.Transition;
import com.google.android.material.transition.MaterialSharedAxis;
import androidx.transition.TransitionManager;
import ir.theme.M3Theme;
import io.github.rosemoe.sora.event.PublishSearchResultEvent;
import io.github.rosemoe.sora.widget.EditorSearcher;
import io.github.rosemoe.sora.widget.CodeEditor;
import io.github.rosemoe.sora.text.Content;
import io.github.rosemoe.sora.text.Cursor;
import ir.hanzodev1375.components.animators.AnimationManager;
import ir.hanzodev1375.components.databinding.DialogRenameBinding;
import ir.hanzodev1375.ghostide.databinding.LayoutSearcherBinding;
import ir.hanzodev1375.ghostide.codeeditors.IdeEditor;
import ir.hanzodev1375.ghostide.R;
import ir.hanzodev1375.components.sheet.customitemsheet.ui.DialogCompat;
public class GhostIdeEditorSearch extends LinearLayoutCompat {
  private LayoutSearcherBinding binding;
  private Supplier<IdeEditor> editorSupplier;
  protected onViewChange viewChange;
  public boolean isShowing = false;
  private boolean isRegexMode = false;
  private boolean isCaseSensitive = false;
  private boolean isWholeWord = false;
  private int currentMatchIndex = -1;
  private GradientDrawable gd;
  private int totalMatches = 0;

  public GhostIdeEditorSearch(Context context) {
    this(context, null);
  }

  public GhostIdeEditorSearch(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public GhostIdeEditorSearch(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    binding = LayoutSearcherBinding.inflate(LayoutInflater.from(getContext()));
    removeAllViews();
    addView(
        binding.getRoot(), new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

    setupTextWatcher();
    setupClickListeners();
    hide();
    gd = new GradientDrawable();
    gd.setColor(fallback(M3Theme.surface(), 0));
    gd.setStroke(2, fallback(M3Theme.onSurface(), 0));
    gd.setCornerRadius(50f);
    binding.getRoot().setBackground(gd);
  }

  private IdeEditor getEditor() {
    return editorSupplier != null ? editorSupplier.get() : null;
  }

  public void setBackgroundColorValue(int color) {
    if (gd != null) {
      gd.setColor(color);
    }
  }

  public void setStrokeColor(int color) {
    if (gd != null) {
      gd.setStroke(3, color);
    }
  }

  private void setupTextWatcher() {
    binding.searchText.addTextChangedListener(
        new TextWatcher() {
          @Override
          public void afterTextChanged(Editable editable) {
            if (getEditor() == null) return;
            String text = binding.searchText.getText().toString();
            if (!text.isEmpty()) {
              performSearch(text);
            } else {
              stopSearch();
            }
          }

          @Override
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });
  }

  private void setupClickListeners() {
    binding.btnMore.setOnClickListener(this::showPopupMenu);
    binding.gotoLast.setOnClickListener(v -> gotoPrevious());
    binding.gotoNext.setOnClickListener(v -> gotoNext());
    binding.replace.setOnClickListener(v -> replace());
    binding.btnClose.setOnClickListener(v -> showAndHide());
  }

  private void showPopupMenu(View view) {
    var popupMenu = new PopupMenu(getContext(), view);

    var regexItem = popupMenu.getMenu().add(0, 1, 0, "Regex Mode");
    var caseItem = popupMenu.getMenu().add(0, 2, 1, "Case Sensitive");
    var wordItem = popupMenu.getMenu().add(0, 3, 2, "Whole Word");

    regexItem.setCheckable(true).setChecked(isRegexMode);
    caseItem.setCheckable(true).setChecked(isCaseSensitive);
    wordItem.setCheckable(true).setChecked(isWholeWord);

    popupMenu.setOnMenuItemClickListener(
        item -> {
          int id = item.getItemId();

          if (id == 1) {
            isRegexMode = !isRegexMode;
            item.setChecked(isRegexMode);
            showToast("Regex mode " + (isRegexMode ? "enabled" : "disabled"));
            if (isRegexMode) {
              isWholeWord = false;
              wordItem.setChecked(false);
            }
          } else if (id == 2) {
            isCaseSensitive = !isCaseSensitive;
            item.setChecked(isCaseSensitive);
            showToast("Case sensitive " + (isCaseSensitive ? "enabled" : "disabled"));
          } else if (id == 3) {
            isWholeWord = !isWholeWord;
            item.setChecked(isWholeWord);
            showToast("Whole word " + (isWholeWord ? "enabled" : "disabled"));
            if (isWholeWord) {
              isRegexMode = false;
              regexItem.setChecked(false);
            }
          }

          String searchText = binding.searchText.getText().toString();
          if (!searchText.isEmpty()) {
            performSearch(searchText);
          }

          return true;
        });

    popupMenu.show();
  }

  private void showToast(String message) {
    GhostToast.makeText(getContext(), message, GhostToast.LENGTH_SHORT).show();
  }

  public void bindEditor(@Nullable Supplier<IdeEditor> supplier) {
    this.editorSupplier = supplier;

    IdeEditor editor = getEditor();
    if (editor == null) return;

    editor.subscribeEvent(
        PublishSearchResultEvent.class,
        (event, unsubscribe) -> {
          post(this::updateSearchResultInfo);
        });
  }

  private void updateSearchResultInfo() {
    if (getEditor() == null) return;

    try {
      String info = "Result:0";
      int textcolor = 0;
      var searcher = getEditor().getSearcher();
      if (searcher != null && searcher.hasQuery()) {
        totalMatches = searcher.getMatchedPositionCount();
        currentMatchIndex = searcher.getCurrentMatchedPositionIndex();

        if (totalMatches > 0) {

          if (currentMatchIndex >= 0) {
            info = String.format("%d/%d", currentMatchIndex + 1, totalMatches);
          } else {
            textcolor =
                fallback(M3Theme.onSurface(), 0);
            info = "Result: " + totalMatches;
          }

          binding.gotoNext.setEnabled(true);
          binding.gotoLast.setEnabled(true);
        } else {
          info = "Result 0";
          textcolor =
              fallback(M3Theme.errorContainer(), 0);
          binding.gotoNext.setEnabled(false);
          binding.gotoLast.setEnabled(false);
        }
      } else {
        binding.searchText.setHint("Search...");
        binding.gotoNext.setEnabled(true);
        binding.gotoLast.setEnabled(true);
        totalMatches = 0;
        currentMatchIndex = -1;
      }
      binding.tvresult.setTextColor(textcolor);
      binding.tvresult.setText(info);

    } catch (Exception e) {

    }
  }

  private int getSearchType() {
    if (isRegexMode) return EditorSearcher.SearchOptions.TYPE_REGULAR_EXPRESSION;
    if (isWholeWord) return EditorSearcher.SearchOptions.TYPE_WHOLE_WORD;
    return EditorSearcher.SearchOptions.TYPE_NORMAL;
  }

  private void performSearch(String text) {
    if (getEditor() == null) return;

    if (text == null || text.isEmpty()) {
      stopSearch();
      return;
    }

    try {
      var searcher = getEditor().getSearcher();
      if (searcher == null) {
        showToast("Searcher not available");
        return;
      }

      int searchType = getSearchType();

      var options = new EditorSearcher.SearchOptions(searchType, !isCaseSensitive);

      try {
        searcher.search(text, options);
      } catch (Exception e) {
        showToast("Invalid pattern: " + e.getMessage());
      }

    } catch (IllegalArgumentException e) {
      showToast("Invalid pattern: " + e.getMessage());
    } catch (Exception e) {
      showToast("Search error: " + e.getMessage());
    }
  }

  public void setTextColor(int color) {
    binding.btnClose.setTextColor(color);
    binding.gotoLast.setTextColor(color);
    binding.gotoNext.setTextColor(color);
    binding.replace.setTextColor(color);
  }

  public void setColorFilter(int color) {
    binding.btnMore.setColorFilter(color,PorterDuff.Mode.SRC_IN);
  }

  public void setTextSearchColor(int color) {
    binding.searchText.setTextColor(color);
  }

  private void stopSearch() {
    try {
      if (getEditor() != null && getEditor().getSearcher() != null) {
        getEditor().getSearcher().stopSearch();
      }
    } catch (Exception e) {

    }
    binding.searchText.setHint("Search...");
    binding.gotoNext.setEnabled(true);
    binding.gotoLast.setEnabled(true);
    totalMatches = 0;
    currentMatchIndex = -1;
  }

  public void showAndHide() {
    if (isShowing) {
      hide();
      if (viewChange != null) viewChange.onViewHide();
    } else {
      if (AnimationManager.getInstance(getContext()).areAnimationsEnabled()) {
        Transition sharedAxis = new MaterialSharedAxis(MaterialSharedAxis.Z, true);
        TransitionManager.beginDelayedTransition(this, sharedAxis);
        setVisibility(View.VISIBLE);
        isShowing = true;
      } else setVisibility(View.VISIBLE);
      if (viewChange != null) viewChange.onViewShow();
      binding.searchText.requestFocus();
    }

    if (getEditor() == null) return;
    stopSearch();
    binding.searchText.setText("");
  }

  public void hide() {
    if (AnimationManager.getInstance(getContext()).areAnimationsEnabled()) {
      Transition sharedAxis = new MaterialSharedAxis(MaterialSharedAxis.Z, true);
      TransitionManager.beginDelayedTransition(this, sharedAxis);
      setVisibility(View.GONE);
      isShowing = false;
    } else setVisibility(View.GONE);
  }

  private void gotoNext() {
    try {
      if (getEditor() == null) {
        showToast("Editor not available");
        return;
      }

      var searcher = getEditor().getSearcher();
      if (searcher == null) {
        showToast("Searcher not available");
        return;
      }

      if (!searcher.hasQuery()) {
        showToast("No active search");
        return;
      }

      boolean result = searcher.gotoNext();

      if (result) {
        updateSearchResultInfo();
      } else {
        showToast("No more matches");
        binding.gotoNext.setEnabled(false);
      }

    } catch (Exception e) {
      showToast("Error: " + e.getMessage());
    }
  }

  private void gotoPrevious() {
    try {
      if (getEditor() == null) {
        showToast("Editor not available");
        return;
      }

      var searcher = getEditor().getSearcher();
      if (searcher == null) {
        showToast("Searcher not available");
        return;
      }

      if (!searcher.hasQuery()) {
        showToast("No active search");
        return;
      }

      boolean result = searcher.gotoPrevious();

      if (result) {
        updateSearchResultInfo();
      } else {
        showToast("No more matches");
        binding.gotoLast.setEnabled(false);
      }

    } catch (Exception e) {
      showToast("Error: " + e.getMessage());
    }
  }

  private void replace() {
    if (getEditor() == null) return;

    String searchText = binding.searchText.getText().toString();
    if (searchText.isEmpty()) {
      showToast("Search text is empty");
      return;
    }

    var bind = DialogRenameBinding.inflate(LayoutInflater.from(getContext()));
    String dialogTitle = buildDialogTitle();

    new DialogCompat(getContext())
        .setTitle(dialogTitle)
        .setView(bind.getRoot())
        .setPositiveButton(
            "Replace",
            (c1, c2) -> {
              try {
                String replacement = bind.rename.getText().toString();
                var searcher = getEditor().getSearcher();

                if (searcher.isMatchedPositionSelected()) {
                  searcher.replaceCurrentMatch(replacement);
                  getEditor()
                      .postDelayed(
                          () -> {
                            searcher.gotoNext();
                            updateSearchResultInfo();
                          },
                          100);
                } else {
                  showToast("No match selected");
                }
              } catch (Exception e) {
                showToast("Replace failed: " + e.getMessage());
              }
            })
        .setNeutralButton(android.R.string.cancel, null)
        .setNegativeButton(
            "Replace All",
            (f1, f2) -> {
              try {
                String replacement = bind.rename.getText().toString();
                getEditor()
                    .getSearcher()
                    .replaceAll(
                        replacement,
                        () ->
                            post(
                                () -> {
                                  showToast("Replace all completed");
                                  performSearch(searchText);
                                }));
              } catch (Exception e) {
                showToast("Replace all failed: " + e.getMessage());
              }
            })
        .show();

    bind.rename.setHint("Replacement");
  }

  private String buildDialogTitle() {
    String title = isRegexMode ? "Replace with Regex" : "Replace";
    if (isWholeWord) title += " (Whole Word)";
    if (isCaseSensitive) title += " (Case Sensitive)";
    return title;
  }

  public void setCallBack(onViewChange viewChange) {
    this.viewChange = viewChange;
  }

  public interface onViewChange {
    void onViewShow();

    void onViewHide();
  }

  private static int fallback(Integer value, int def) {
    return value != null ? value : def;
  }
}
