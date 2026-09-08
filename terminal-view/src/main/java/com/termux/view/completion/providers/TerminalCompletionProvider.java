package com.termux.view.completion.providers;

import com.termux.view.completion.TerminalCompletionItem;

import java.util.List;

/**
 * Interface for providing completion items to the terminal autocomplete.
 *
 * <p>Providers mirror Sora's {@code Language.requireAutoComplete} contract: given the current input
 * they return a list of {@link TerminalCompletionItem}s (each carrying a kind, so LSP results can
 * later be merged in the same list).
 */
public interface TerminalCompletionProvider {

  /**
   * Compute completion items for the given input.
   *
   * @param currentLine The current line text from the terminal prompt.
   * @param cursorPosition The cursor position within the line (0-indexed, in chars).
   * @return List of completion items, or an empty list if there are none.
   */
  List<TerminalCompletionItem> getCompletions(String currentLine, int cursorPosition);

  /** Optional: a short name for debugging / logging. */
  String getProviderName();
}
