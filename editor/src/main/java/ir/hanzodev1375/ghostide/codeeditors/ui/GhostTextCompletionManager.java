package ir.hanzodev1375.ghostide.codeeditors.ui;

import androidx.annotation.Nullable;
import io.github.rosemoe.sora.lang.styling.inlayHint.GhostTextInlayHint;
import io.github.rosemoe.sora.lang.styling.inlayHint.InlayHint;
import io.github.rosemoe.sora.lang.styling.inlayHint.InlayHintsContainer;
import io.github.rosemoe.sora.text.CharPosition;
import ir.hanzodev1375.ghostide.codeeditors.IdeEditor;

/**
 * Renders a VS Code-style ghost text preview for the currently highlighted auto-completion item.
 *
 * <p>The ghost text is injected into the editor's {@link InlayHintsContainer}. To survive the fact
 * that color previews ({@code WebColorIde}) and the LSP layer replace the inlay hints container
 * while you type, this manager keeps the "base" container separate and only asks the editor to
 * re-inject it through {@link IdeEditor#setInlayHintsRaw} (bypassing the merge) while the merged
 * ghost text is only composed inside {@link #mergeGhost(InlayHintsContainer)}. Every external call
 * to {@code IdeEditor.setInlayHints} is intercepted and the ghost text is merged back on top.
 */
public class GhostTextCompletionManager {

  private final IdeEditor editor;

  @Nullable private InlayHintsContainer baseHints;
  @Nullable private String text;
  private boolean active;
  private boolean enabled = true;

  public GhostTextCompletionManager(IdeEditor editor) {
    this.editor = editor;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
    if (!enabled) {
      clearGhostText();
    }
  }

  public boolean isActive() {
    return active;
  }

  /**
   * Show (or update) the ghost text preview at the current cursor position. Passing {@code null} or
   * an empty string clears the preview.
   */
  public void showGhostText(@Nullable String text) {
    if (!enabled || text == null || text.isEmpty()) {
      clearGhostText();
      return;
    }
    if (!active) {
      baseHints = editor.getInlayHints();
      active = true;
    }
    this.text = text;
    editor.setInlayHintsRaw(mergeGhost(baseHints));
  }

  /** Remove the ghost text preview and restore the base inlay hints container. */
  public void clearGhostText() {
    if (!active) {
      return;
    }
    active = false;
    text = null;
    editor.setInlayHintsRaw(baseHints);
    baseHints = null;
  }

  /** Keep the latest base container set by external writers (color hints, LSP, ...). */
  public void setBaseHints(@Nullable InlayHintsContainer baseHints) {
    this.baseHints = baseHints;
  }

  /**
   * Build the container that should actually be assigned to the editor: all hints from {@code base}
   * plus the current ghost text anchored at the live cursor position (so it keeps tracking the
   * caret while the user types).
   */
  @Nullable
  public InlayHintsContainer mergeGhost(@Nullable InlayHintsContainer base) {
    if (!active || text == null) {
      return base;
    }
    InlayHintsContainer merged = new InlayHintsContainer();
    if (base != null) {
      for (int baseLine : base.getLineNumbers()) {
        for (InlayHint hint : base.getForLine(baseLine)) {
          merged.add(hint);
        }
      }
    }
    CharPosition cursor = editor.getCursor().left();
    GhostTextInlayHint.addGhostText(merged, cursor.line, cursor.column, text);
    return merged;
  }
}
