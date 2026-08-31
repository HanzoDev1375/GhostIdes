package ir.hanzodev1375.ghostide.codeeditors.dependencychecker;

import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import io.github.rosemoe.sora.event.ContentChangeEvent;
import io.github.rosemoe.sora.event.SelectionChangeEvent;
import io.github.rosemoe.sora.lang.styling.HighlightTextContainer;
import io.github.rosemoe.sora.lang.styling.color.EditorColor;
import io.github.rosemoe.sora.widget.CodeEditor;
import io.github.rosemoe.sora.widget.base.EditorPopupWindow;
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme;
import ir.hanzodev1375.ghostide.codeeditors.R;
import ir.hanzodev1375.ghostide.codeeditors.colorscheme.GhostColorScheme;
import ir.hanzodev1375.ghostide.codeeditors.preview.EditorPopUp;

/**
 * Base class for the "new dependency version" checker.
 *
 * <p>Every declaration in the opened file is looked up online in the background. Only the version
 * token of dependencies that actually have a newer release is highlighted (with a subtle background,
 * like Android Studio); nothing is painted before a real update is confirmed. Tapping such a token
 * pops up a small window that shows the newest version and offers to update the text in place.
 * Subclasses only need to tell whether a file/language is theirs and how to collect the checkable
 * dependency tokens per line.
 */
public abstract class DependencyCheckerIde {

  private static final long PROCESS_DELAY_MS = 120;

  protected final CodeEditor editor;

  /** Currently open file path (may be null), used to restrict which files are checked. */
  private String currentFilePath;

  /** Latest known online version keyed by Maven coordinates, used to highlight updates. */
  private final Map<String, String> updateCache = new HashMap<>();

  private final Set<String> checking = new HashSet<>();

  private long lastProcessTime = 0;
  private String lastCoordinates = "";
  private int lastMatchLine = -1;
  private EditorPopupWindow activePopup;

  protected DependencyCheckerIde(CodeEditor editor) {
    this.editor = editor;
  }

  /** Sets the currently open file path, then refreshes highlights accordingly. */
  public void setFilePath(String path) {
    this.currentFilePath = path;
    refreshHighlights();
  }

  /** The path of the currently open file (may be null). */
  protected final String getFilePath() {
    return currentFilePath;
  }

  /**
   * Returns the base name of the currently open file, or {@code null} when no file is open.
   * Subclasses use it to restrict checking to specific files (e.g. libs.versions.toml).
   */
  protected final String getFileName() {
    if (currentFilePath == null) return null;
    String normalized = currentFilePath.replace('\\', '/');
    int idx = normalized.lastIndexOf('/');
    return idx < 0 ? normalized : normalized.substring(idx + 1);
  }

  /** Clears any cached online-check results. */
  public void resetUpdateCache() {
    updateCache.clear();
  }

  /** Returns true when the current editor language is handled by this checker. */
  protected abstract boolean isMyLanguage();

  /**
   * Parses the dependency under the cursor. Returns null when the caret is not on a checkable
   * version token.
   */
  protected abstract DependencyMatch findUnderCursor(SelectionChangeEvent event);

  /**
   * Collects every checkable dependency of one line so the version token can be highlighted.
   *
   * @param lineText text of the current line
   * @param into list receiving the matches
   */
  protected abstract void collectLineHighlights(
      int line, String lineText, List<DependencyMatch> into);

  public final void attach() {
    refreshHighlights();

    editor.subscribeEvent(
        ContentChangeEvent.class,
        (event, unsubscribe) -> {
          if (isMyLanguage()) {
            refreshHighlights();
          }
        });

    editor.subscribeEvent(
        SelectionChangeEvent.class,
        (event, unsubscribe) -> {
          if (!isMyLanguage()) return;
          if (event.getCause() != SelectionChangeEvent.CAUSE_TAP) return;

          long now = System.currentTimeMillis();
          if (now - lastProcessTime < PROCESS_DELAY_MS) return;
          lastProcessTime = now;

          editor.post(
              () -> {
                try {
                  handle(event);
                } catch (Exception e) {
                  dismissPopup();
                }
              });
        });

    editor.subscribeEvent(
        io.github.rosemoe.sora.event.ScrollEvent.class,
        (event, unsubscribe) -> {
          if (isMyLanguage()) {
            refreshHighlights();
          }
        });
  }

  /**
   * Collects every dependency of the opened file whose newest version is already known (from a
   * previous online check) and highlights only the version token of those that have an update.
   *
   * <p>For each line it first registers the dependency so its online version can be looked up in the
   * background. Tokens that have a known update are highlighted (background only). Everything else
   * stays untouched, so nothing is highlighted before a real update is found online.
   */
  public void refreshHighlights() {
    if (!isMyLanguage()) {
      editor.setHighlightTexts(null);
      return;
    }

    var container = new HighlightTextContainer();
    var text = editor.getText();

    int first = Math.max(0, editor.getFirstVisibleLine());
    int last = Math.min(text.getLineCount() - 1, editor.getLastVisibleLine());

    for (int line = first; line <= last; line++) {
      String lineText = text.getLineString(line);
      List<DependencyMatch> deps = new ArrayList<>();
      collectLineHighlights(line, lineText, deps);
      for (DependencyMatch d : deps) {
        if (d.versionStart() >= d.versionEnd()) continue;

        String coords = d.coordinates();
        String newest = updateCache.get(coords);

        if (newest != null && !newest.trim().equals(d.version().trim())) {
          container.add(
              new HighlightTextContainer.HighlightText(
                  line,
                  d.fullStart(),
                  line,
                  d.fullEnd(),
                  new EditorColor(GhostColorScheme.DEPENDENCY_UPDATE_AVAILABLE_BG),
                  new EditorColor(GhostColorScheme.DEPENDENCY_UPDATE_AVAILABLE)));
        }

        checkInBackground(coords, d.group(), d.name());
      }
    }

    editor.setHighlightTexts(container.isEmpty() ? null : container);
  }

  /**
   * Looks up the newest version of {@code coords} in the background (if not already checked) and
   * refreshes the highlights on the main thread when the result changes. Even failed lookups are
   * cached so they are not retried on every content change.
   */
  private void checkInBackground(String coords, String group, String name) {
    if (checking.contains(coords)) return;
    checking.add(coords);

    MavenVersionChecker.check(
        group,
        name,
        newest ->
            editor.post(
                () -> {
                  String safe = newest == null ? "" : newest;
                  updateCache.put(coords, safe);
                  checking.remove(coords);
                  if (isMyLanguage()) {
                    refreshHighlights();
                  }
                }));
  }

  // ── Popup: loading ───────────────────────────────────────────────────────

  private void showLoading(DependencyMatch match) {
    dismissPopup();
    LinearLayout root = makeRoot();
    root.addView(infoLine(R.string.dependency_checking));
    root.addView(infoLine(match.coordinates()));
    activePopup = EditorPopUp.showCustomViewAtCursor(editor, root);
  }

  // ── Popup: result ────────────────────────────────────────────────────────

  private void showResult(DependencyMatch match, String newest) {
    // Discard if the user moved to another token meanwhile.
    if (!match.coordinates().equals(lastCoordinates)) return;

    dismissPopup();

    String current = match.version();
    LinearLayout root = makeRoot();

    if (newest == null || newest.isEmpty()) {
      root.addView(titleLine(R.string.dependency_check_failed));
      root.addView(infoLine(R.string.dependency_current_version, current));
      activePopup = EditorPopUp.showCustomViewAtCursor(editor, root);
      return;
    }

    boolean hasUpdate = !current.trim().equals(newest.trim());

    root.addView(titleLine(R.string.dependency_current_version, current));
    if (hasUpdate) {
      root.addView(infoLine(R.string.dependency_new_version, newest));
      root.addView(buildUpdateButton(match, newest));
    } else {
      root.addView(infoLine(R.string.dependency_up_to_date));
    }

    activePopup = EditorPopUp.showCustomViewAtCursor(editor, root);
  }

  private View buildUpdateButton(DependencyMatch match, String newest) {
    EditorColorScheme scheme = editor.getColorScheme();
    int accent = scheme.getColor(GhostColorScheme.DEPENDENCY_UPDATE_AVAILABLE);
    if (accent == 0) accent = scheme.getColor(EditorColorScheme.COMPLETION_WND_TEXT_MATCHED);
    if (accent == 0) accent = scheme.getColor(EditorColorScheme.DIAGNOSTIC_TOOLTIP_ACTION);

    Button button = new Button(editor.getContext());
    button.setText(R.string.dependency_update);
    button.setTextColor(android.graphics.Color.WHITE);
    button.setAllCaps(false);
    GradientDrawable bg = new GradientDrawable();
    bg.setCornerRadius(dp(12));
    bg.setColor(accent);
    button.setBackground(bg);

    LinearLayout.LayoutParams lp =
        new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    lp.topMargin = dp(10);
    button.setLayoutParams(lp);
    button.setOnClickListener(v -> applyUpdate(match, newest));
    return button;
  }

  // ── Apply update: replace version token in the document ─────────────────

  protected final void applyUpdate(DependencyMatch match, String newVersion) {
    int line = lastMatchLine;
    if (line < 0) {
      line = editor.getCursor().left().getLine();
    }
    try {
      int startOffset = editor.getText().getIndexer().getCharIndex(line, match.versionStart());
      int endOffset = editor.getText().getIndexer().getCharIndex(line, match.versionEnd());

      editor.getText().beginBatchEdit();
      try {
        editor.getText().replace(startOffset, endOffset, newVersion);
      } finally {
        editor.getText().endBatchEdit();
      }

      editor.setSelection(line, match.versionStart() + newVersion.length());
      editor.invalidate();
    } catch (Exception ignored) {
    }
    dismissPopup();
  }

  // ── Event handling ───────────────────────────────────────────────────────

  private void handle(SelectionChangeEvent event) {
    if (!isMyLanguage() || editor.getCursor().isSelected()) {
      dismissPopup();
      lastCoordinates = "";
      return;
    }

    DependencyMatch match = findUnderCursor(event);
    if (match == null) {
      dismissPopup();
      lastCoordinates = "";
      return;
    }

    String coords = match.coordinates();
    if (coords.equals(lastCoordinates) && activePopup != null && activePopup.isShowing()) {
      return;
    }
    lastCoordinates = coords;
    lastMatchLine = event.getLeft().getLine();

    String cached = updateCache.get(coords);
    if (cached != null && !cached.isEmpty()) {
      showResult(match, cached);
      return;
    }

    showLoading(match);
    MavenVersionChecker.check(
        match.group(),
        match.name(),
        newest -> {
          if (newest != null && !newest.isEmpty()) {
            updateCache.put(coords, newest);
          }
          editor.post(() -> showResult(match, newest));
        });
  }

  // ── View helpers ─────────────────────────────────────────────────────────

  protected final void dismissPopup() {
    if (activePopup != null) {
      try {
        activePopup.dismiss();
      } catch (Exception ignored) {
      }
      activePopup = null;
    }
  }

  private LinearLayout makeRoot() {
    LinearLayout root = new LinearLayout(editor.getContext());
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(dp(14), dp(12), dp(14), dp(12));
    root.setGravity(Gravity.CENTER_HORIZONTAL);
    return root;
  }

  private TextView titleLine(int resId, Object... args) {
    EditorColorScheme scheme = editor.getColorScheme();
    TextView tv = new TextView(editor.getContext());
    tv.setGravity(Gravity.CENTER);
    tv.setTextSize(13);
    tv.setTypeface(Typeface.DEFAULT_BOLD);
    tv.setTextColor(scheme.getColor(EditorColorScheme.COMPLETION_WND_TEXT_PRIMARY));
    setTextArgs(tv, resId, args);
    return tv;
  }

  private TextView infoLine(int resId, Object... args) {
    EditorColorScheme scheme = editor.getColorScheme();
    TextView tv = new TextView(editor.getContext());
    tv.setGravity(Gravity.CENTER);
    tv.setTextSize(12);
    tv.setPadding(0, dp(4), 0, 0);
    tv.setTextColor(scheme.getColor(EditorColorScheme.COMPLETION_WND_TEXT_SECONDARY));
    setTextArgs(tv, resId, args);
    return tv;
  }

  private TextView infoLine(String resId, Object... args) {
    EditorColorScheme scheme = editor.getColorScheme();
    TextView tv = new TextView(editor.getContext());
    tv.setGravity(Gravity.CENTER);
    tv.setTextSize(12);
    tv.setPadding(0, dp(4), 0, 0);
    tv.setTextColor(scheme.getColor(EditorColorScheme.COMPLETION_WND_TEXT_SECONDARY));
    setTextArgs(tv, resId, args);
    return tv;
  }

  private void setTextArgs(TextView tv, String resId, Object... args) {
    String text = resId;
    if (args != null && args.length > 0) {
      text = String.format(Locale.ROOT, text, args);
    }
    tv.setText(text);
  }

  private void setTextArgs(TextView tv, int resId, Object... args) {
    String text = editor.getContext().getString(resId);
    if (args != null && args.length > 0) {
      text = String.format(Locale.ROOT, text, args);
    }
    tv.setText(text);
  }

  protected final int dp(int value) {
    float density = editor.getContext().getResources().getDisplayMetrics().density;
    return (int) (value * density);
  }
}
