package com.termux.view.completion;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.termux.view.R;
import com.termux.view.TerminalView;
import com.termux.view.completion.providers.CommandCompletionProvider;
import com.termux.view.completion.providers.HistoryCompletionProvider;
import com.termux.view.completion.providers.TerminalCompletionProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ir.theme.M3Theme;

/**
 * The terminal auto-completion component.
 *
 * <p>Renders a {@link PopupWindow} with a {@link RecyclerView} on top of the {@link
 * com.termux.view.TerminalView} — mirroring how Sora's {@code EditorAutoCompletion} overlaid a
 * window on its {@code CodeEditor} view.
 *
 * <p>Completions come from a set of {@link TerminalCompletionProvider}s (commands, history, ...),
 * each producing {@link TerminalCompletionItem}s that carry a {@link TerminalCompletionItemKind} so
 * the results can later be merged with LSP completions.
 */
public class TerminalAutoCompletion {

  /** Debounce delay (ms) between keystrokes before (re)computing completions. */
  private static final long COMPLETION_DELAY_MS = 120;

  /** Max number of items shown per provider pass. */
  private static final int MAX_ITEMS = 30;

  private final Context context;
  private final View anchor; // the TerminalView
  private final Handler handler = new Handler(Looper.getMainLooper());
  private final Runnable debounceRunnable = this::computeCompletions;

  private final List<TerminalCompletionProvider> providers = new ArrayList<>();
  private final CommandCompletionProvider commandProvider = new CommandCompletionProvider();
  private final HistoryCompletionProvider historyProvider = new HistoryCompletionProvider();

  private PopupWindow popupWindow;
  private View popupView;
  private RecyclerView recyclerView;
  private TerminalCompletionAdapter adapter;

  private Callback callback;
  private boolean enabled = true;
  private boolean visible = false;

  /* Input state captured at the time completions were computed. */
  private TerminalSessionRef sessionRef;

  /* ------------------------------------------------------------------ */

  /** Callbacks from the auto-completion back to the terminal host. */
  public interface Callback {
    /** Called when the user accepts a completion (right after the suffix is committed). */
    void onItemSelected(TerminalCompletionItem item);

    /** Called when the popup is shown. */
    default void onCompletionShown() {}

    /** Called when the popup is hidden. */
    default void onCompletionHidden() {}
  }

  /** Lightweight holder so we do not hard-code a TerminalSession dependency here. */
  public interface TerminalSessionRef {
    /** Current text of the line the cursor is on (without the shell prompt). */
    String getCurrentLine();

    /** Cursor column within {@link #getCurrentLine()}. */
    int getCursorCol();

    /** Absolute screen row the cursor is on. */
    int getCursorRow();

    /** Insert the suffix text at the cursor position. */
    void commitSuffix(String text);
  }

  /* ------------------------------------------------------------------ */

  public TerminalAutoCompletion(@NonNull Context context, @NonNull View anchor) {
    this.context = context;
    this.anchor = anchor;

    providers.add(commandProvider);
    providers.add(historyProvider);

    buildPopup();
  }

  private void buildPopup() {
    LayoutInflater inflater = LayoutInflater.from(context);
    popupView = inflater.inflate(R.layout.terminal_completion_popup,null,false);
    popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);

    recyclerView = popupView.findViewById(R.id.completion_recycler);
    recyclerView.setLayoutManager(new LinearLayoutManager(context));

    adapter = new TerminalCompletionAdapter();
    adapter.setOnItemClickListener(this::selectItem);
    recyclerView.setAdapter(adapter);

    // Theme the popup with M3 values.
    applyTheme();

    popupWindow =
        new PopupWindow(
            popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    popupWindow.setFocusable(false);
    popupWindow.setOutsideTouchable(true);
    popupWindow.setBackgroundDrawable(
        new ColorDrawable(Color.TRANSPARENT));
    popupWindow.setElevation(8 * context.getResources().getDisplayMetrics().density);
  }

  private void applyTheme() {
    Integer bg = M3Theme.surfaceContainer();
    if (bg == null) {
      bg = M3Theme.surface() != null ? M3Theme.surface() : 0xFF1E1E1E;
    }
    popupView.setBackgroundColor(bg);
  }

  /* ------------------------------------------------------------------ */
  /* Public API                                                          */
  /* ------------------------------------------------------------------ */

  public void setCallback(@Nullable Callback callback) {
    this.callback = callback;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
    if (!enabled) {
      hide();
    }
  }

  public boolean isEnabled() {
    return enabled;
  }

  public boolean isShowing() {
    return visible && popupWindow != null && popupWindow.isShowing();
  }

  public void addProvider(@NonNull TerminalCompletionProvider provider) {
    if (provider != null) {
      providers.add(provider);
    }
  }

  public void removeProvider(@NonNull TerminalCompletionProvider provider) {
    providers.remove(provider);
  }

  /** Register a command into the history provider. */
  public void addHistoryCommand(String command) {
    historyProvider.addCommand(command);
  }

  /* ------------------------------------------------------------------ */
  /* Input-driven entry points                                          */
  /* ------------------------------------------------------------------ */

  /**
   * Notify that the user typed something (a printable code point). Schedules a debounced completion
   * pass.
   */
  public void onTextChanged(@Nullable TerminalSessionRef ref) {
    if (!enabled) {
      return;
    }
    this.sessionRef = ref;
    handler.removeCallbacks(debounceRunnable);
    handler.postDelayed(debounceRunnable, COMPLETION_DELAY_MS);
  }

  /**
   * Notify that a word-boundary / other trigger key was pressed (space, slash, etc.) so the
   * debounce restarts and the word context is re-evaluated.
   */
  public void onWordBoundary(@Nullable TerminalSessionRef ref) {
    if (!enabled) {
      return;
    }
    this.sessionRef = ref;
    handler.removeCallbacks(debounceRunnable);
    handler.postDelayed(debounceRunnable, COMPLETION_DELAY_MS);
  }

  /** The user asked to open the completion list manually (e.g. a long-press or hotkey). */
  public void triggerCompletion(@Nullable TerminalSessionRef ref) {
    if (!enabled) {
      return;
    }
    this.sessionRef = ref;
    handler.removeCallbacks(debounceRunnable);
    computeCompletions();
  }

  /** Clear any pending work and hide the popup. */
  public void cancel() {
    handler.removeCallbacks(debounceRunnable);
    hide();
  }

  /* ------------------------------------------------------------------ */
  /* Core                                                                */
  /* ------------------------------------------------------------------ */

  private void computeCompletions() {
    if (!enabled || sessionRef == null) {
      hide();
      return;
    }

    String line = sessionRef.getCurrentLine();
    int cursorCol = sessionRef.getCursorCol();
    if (line == null || cursorCol < 0) {
      hide();
      return;
    }

    // Number of times this computation should be validated against new input.
    String currentLine = line.substring(0, Math.min(cursorCol, line.length()));

    List<TerminalCompletionItem> collected = new ArrayList<>();
    for (TerminalCompletionProvider provider : providers) {
      if (provider == null) {
        continue;
      }
      List<TerminalCompletionItem> result = provider.getCompletions(currentLine, cursorCol);
      if (result != null) {
        collected.addAll(result);
      }
    }

    // Deduplicate by commitText + prefix, keep the highest-priority first occurrence.
    Collections.sort(
        collected,
        (a, b) -> {
          int p = Integer.compare(a.getPriority(), b.getPriority());
          if (p != 0) {
            return p;
          }
          return a.getLabel().compareToIgnoreCase(b.getLabel());
        });

    List<TerminalCompletionItem> dedup = new ArrayList<>();
    java.util.Set<String> seen = new java.util.HashSet<>();
    for (TerminalCompletionItem item : collected) {
      String key = item.getCommitText();
      if (seen.add(key)) {
        dedup.add(item);
        if (dedup.size() >= MAX_ITEMS) {
          break;
        }
      }
    }

    if (dedup.isEmpty()) {
      hide();
      return;
    }

    adapter.setItems(dedup);
    adapter.setCurrentSelection(-1);

    updatePopupSize(dedup.size());
    showPopup();

    if (callback != null) {
      callback.onCompletionShown();
    }
  }

  private void showPopup() {
    if (popupWindow == null || anchor == null) {
      return;
    }
    if (visible && popupWindow.isShowing()) {
      updatePopupLocation();
      return;
    }
    try {
      int[] location = new int[2];
      if (!computePopupLocation(location)) {
        int[] loc = new int[2];
        anchor.getLocationOnScreen(loc);
        location[0] = loc[0] + dpToPx(8);
        location[1] = loc[1] + dpToPx(8);
      }
      popupWindow.showAtLocation(anchor, Gravity.NO_GRAVITY, location[0], location[1]);
      visible = true;
    } catch (Exception ignored) {
      visible = false;
    }
  }

  private void updatePopupLocation() {
    if (popupWindow == null || !popupWindow.isShowing() || anchor == null) {
      return;
    }
    try {
      int[] location = new int[2];
      if (!computePopupLocation(location)) {
        int[] loc = new int[2];
        anchor.getLocationOnScreen(loc);
        location[0] = loc[0] + dpToPx(8);
        location[1] = loc[1] + dpToPx(8);
      }
      popupWindow.update(
          location[0], location[1], popupWindow.getWidth(), popupWindow.getHeight());
    } catch (Exception ignored) {
    }
  }

  /**
   * Computes the popup's top-left corner in screen coordinates, hugging the terminal cursor so the
   * completion list always appears right next to it. Returns {@code false} when the anchor is not a
   * {@link TerminalView} (callers then fall back to the old top-left placement).
   */
  private boolean computePopupLocation(int[] out) {
    if (!(anchor instanceof TerminalView)) {
      return false;
    }
    TerminalView terminal = (TerminalView) anchor;
    if (terminal.mRenderer == null) {
      return false;
    }
    int row = sessionRef != null ? sessionRef.getCursorRow() : 0;
    int col = sessionRef != null ? sessionRef.getCursorCol() : 0;

    int[] viewLoc = new int[2];
    terminal.getLocationOnScreen(viewLoc);

    int cursorX = terminal.getPointX(col, row);
    int cursorY = terminal.getPointY(row);
    int width = popupWindow.getWidth();
    int height = popupWindow.getHeight();

    // Keep the popup inside the terminal horizontally.
    int x = viewLoc[0] + Math.min(cursorX, Math.max(terminal.getWidth() - width, 0));

    // Prefer to show right below the cursor line…
    int belowCursor = cursorY + terminal.mRenderer.getFontLineSpacing() + dpToPx(2);
    int y = viewLoc[1] + belowCursor;

    // …and flip above it when there is not enough room below.
    if (width > 0 && height > 0 && belowCursor + height > terminal.getHeight()) {
      y = viewLoc[1] + Math.max(cursorY - height - dpToPx(4), 0);
    }

    out[0] = x;
    out[1] = y;
    return true;
  }

  private void updatePopupSize(int itemCount) {
    if (recyclerView == null) {
      return;
    }
    int itemHeight = dpToPx(44);
    int maxHeight = (int) (anchor.getHeight() * 0.5f);
    int height = itemCount * itemHeight;
    height = Math.min(height, maxHeight);
    height = Math.max(height, itemHeight);

    ViewGroup.LayoutParams lp = recyclerView.getLayoutParams();
    if (lp == null) {
      lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, height);
    }
    lp.height = height;
    recyclerView.setLayoutParams(lp);

    int width = Math.min(dpToPx(320), (int) (anchor.getWidth() * 0.7f));
    ViewGroup.LayoutParams rootLp = popupView.getLayoutParams();
    if (rootLp != null) {
      rootLp.width = width;
    } else {
      popupView.setLayoutParams(
          new ViewGroup.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    // Set explicit dimensions so the cursor-following positioning can resolve the popup size
    // before it is shown.
    if (popupWindow != null) {
      popupWindow.setWidth(width);
      popupWindow.setHeight(height + dpToPx(8));
    }
  }

  /* ------------------------------------------------------------------ */
  /* Selection / navigation                                              */
  /* ------------------------------------------------------------------ */

  public void moveDown() {
    if (!isShowing() || adapter == null) {
      return;
    }
    int sel = adapter.getCurrentSelection();
    int next = sel + 1;
    if (next >= adapter.getItemCount()) {
      next = 0;
    }
    adapter.setCurrentSelection(next);
    ensureVisible(next);
  }

  public void moveUp() {
    if (!isShowing() || adapter == null) {
      return;
    }
    int sel = adapter.getCurrentSelection();
    int next = sel - 1;
    if (next < 0) {
      next = adapter.getItemCount() - 1;
    }
    adapter.setCurrentSelection(next);
    ensureVisible(next);
  }

  /** Accept the currently highlighted (or first) item. */
  public boolean select() {
    return select(adapter != null ? adapter.getCurrentSelection() : -1);
  }

  /** Accept the item at the given position; {@code -1} selects the first item. */
  public boolean select(int position) {
    if (!isShowing() || adapter == null) {
      return false;
    }
    int index = position;
    if (index < 0) {
      index = adapter.getCurrentSelection();
    }
    if (index < 0) {
      index = 0;
    }
    TerminalCompletionItem item = adapter.getItem(index);
    if (item == null) {
      return false;
    }
    selectItem(item, index);
    return true;
  }

  private void selectItem(TerminalCompletionItem item, int position) {
    if (item == null || sessionRef == null) {
      return;
    }
    String suffix = item.getSuffix();
    if (suffix != null && !suffix.isEmpty()) {
      sessionRef.commitSuffix(suffix);
    }
    hide();
    if (callback != null) {
      callback.onItemSelected(item);
    }
  }

  public void hide() {
    handler.removeCallbacks(debounceRunnable);
    boolean wasShowing = isShowing();
    if (popupWindow != null && popupWindow.isShowing()) {
      popupWindow.dismiss();
    }
    visible = false;
    if (adapter != null) {
      adapter.setItems(new ArrayList<>());
      adapter.setCurrentSelection(-1);
    }
    if (wasShowing && callback != null) {
      callback.onCompletionHidden();
    }
  }

  private void ensureVisible(int position) {
    if (recyclerView != null) {
      recyclerView.smoothScrollToPosition(position);
    }
  }

  /* ------------------------------------------------------------------ */
  /* Misc                                                                */
  /* ------------------------------------------------------------------ */

  public List<TerminalCompletionProvider> getProviders() {
    return providers;
  }

  private int dpToPx(int dp) {
    return Math.round(dp * context.getResources().getDisplayMetrics().density);
  }
}
