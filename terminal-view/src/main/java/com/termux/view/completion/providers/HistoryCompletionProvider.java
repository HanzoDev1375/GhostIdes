package com.termux.view.completion.providers;

import com.termux.view.completion.TerminalCompletionItem;
import com.termux.view.completion.TerminalCompletionItemKind;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Provides completions from the in-memory command history (most recent first). */
public class HistoryCompletionProvider implements TerminalCompletionProvider {

  private static final int MAX_HISTORY_SIZE = 500;
  private static final int MAX_COMPLETIONS = 20;

  private final Set<String> history = new LinkedHashSet<>();

  /** Record a command into the history. */
  public void addCommand(String command) {
    if (command == null || command.trim().isEmpty()) {
      return;
    }
    history.remove(command);
    history.add(command);
    while (history.size() > MAX_HISTORY_SIZE) {
      Iterator<String> it = history.iterator();
      it.next();
      it.remove();
    }
  }

  /** Add many commands at once. */
  public void addCommands(List<String> commands) {
    if (commands == null) {
      return;
    }
    for (String command : commands) {
      addCommand(command);
    }
  }

  @Override
  public List<TerminalCompletionItem> getCompletions(String currentLine, int cursorPosition) {
    List<TerminalCompletionItem> items = new ArrayList<>();
    if (currentLine == null || currentLine.isEmpty()) {
      return items;
    }

    String textBeforeCursor = currentLine.substring(0, clamp(cursorPosition, currentLine.length()));
    String currentWord = extractCurrentWord(textBeforeCursor);

    if (currentWord.isEmpty()) {
      return items;
    }

    // Iterate from the newest to the oldest.
    int count = 0;
    Iterator<String> it = history.iterator();
    while (it.hasNext() && count < MAX_COMPLETIONS) {
      String cmd = it.next();
      if (cmd.startsWith(currentWord)) {
        items.add(
            new TerminalCompletionItem(
                cmd, cmd, "history", currentWord, TerminalCompletionItemKind.Text));
        count++;
      }
    }
    return items;
  }

  @Override
  public String getProviderName() {
    return "HistoryCompletionProvider";
  }

  public Set<String> getHistory() {
    return new LinkedHashSet<>(history);
  }

  public void clearHistory() {
    history.clear();
  }

  private String extractCurrentWord(String textBeforeCursor) {
    int start = lastSeparator(textBeforeCursor) + 1;
    return textBeforeCursor.substring(start);
  }

  private static int lastSeparator(String text) {
    for (int i = text.length() - 1; i >= 0; i--) {
      char c = text.charAt(i);
      if (c == ' ' || c == '\t') {
        return i;
      }
    }
    return -1;
  }

  private static int clamp(int value, int max) {
    return Math.max(0, Math.min(value, max));
  }
}
