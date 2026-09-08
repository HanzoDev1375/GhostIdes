package com.termux.view.completion;

/**
 * Represents a single completion item for the terminal autocomplete.
 *
 * <p>A {@link TerminalCompletionItemKind} is attached so that it can later be merged with LSP
 * completions seamlessly.
 */
public class TerminalCompletionItem {

  /** The full text to be inserted when this item is selected (e.g. a whole word or a command). */
  private final String commitText;

  /** Display label shown in the completion list. */
  private final String label;

  /** Optional description shown below the label. */
  private final String description;

  /** The text already typed for this word (e.g. "gi" when completing "git"). */
  private final String typedText;

  /** The kind/type of this item. */
  private TerminalCompletionItemKind kind;

  /** Priority for sorting (lower value = shown higher). */
  private int priority;

  /* ------------------------------------------------------------------ */

  /**
   * Create a completion item.
   *
   * @param commitText The full text to insert when accepted.
   * @param label The display label.
   * @param description Optional description (may be null).
   * @param typedText The text already typed for this word.
   * @param kind The kind of this item.
   */
  public TerminalCompletionItem(
      String commitText,
      String label,
      String description,
      String typedText,
      TerminalCompletionItemKind kind) {
    this.commitText = commitText;
    this.label = label != null ? label : commitText;
    this.description = description;
    this.typedText = typedText != null ? typedText : "";
    this.kind = kind != null ? kind : TerminalCompletionItemKind.Text;
  }

  /** Create a completion item whose label equals its commit text. */
  public TerminalCompletionItem(
      String commitText, String typedText, TerminalCompletionItemKind kind) {
    this(commitText, commitText, null, typedText, kind);
  }

  /* ------------------------------------------------------------------ */

  public String getCommitText() {
    return commitText;
  }

  public String getLabel() {
    return label;
  }

  public String getDescription() {
    return description;
  }

  public String getTypedText() {
    return typedText;
  }

  public TerminalCompletionItemKind getKind() {
    return kind;
  }

  public TerminalCompletionItem setKind(TerminalCompletionItemKind kind) {
    this.kind = kind != null ? kind : TerminalCompletionItemKind.Text;
    return this;
  }

  public int getPriority() {
    return priority;
  }

  public TerminalCompletionItem setPriority(int priority) {
    this.priority = priority;
    return this;
  }

  /**
   * The text that should be appended to the currently-typed word to complete it. If {@link
   * #commitText} starts with the typed text, only the missing part is returned; otherwise the full
   * commit text is returned (e.g. when the typed text does not match).
   */
  public String getSuffix() {
    if (!typedText.isEmpty() && commitText.startsWith(typedText)) {
      return commitText.substring(typedText.length());
    }
    return commitText;
  }

  @Override
  public String toString() {
    return "TerminalCompletionItem{"
        + "commitText='"
        + commitText
        + '\''
        + ", label='"
        + label
        + '\''
        + ", typed='"
        + typedText
        + '\''
        + ", kind="
        + kind
        + '}';
  }
}
