package ir.hanzodev1375.ghostide.codeeditors.langs.lsp.gth;

import java.util.ArrayList;
import java.util.List;

/**
 * Tolerant JSON tokenizer for GhostIDE theme files. It does not require fully valid JSON: it walks
 * the characters, records every string token together with whether it is a key or a value, which
 * section it lives in, and which property the value owns. It also reports an approximate brace
 * balance so the diagnostic engine can warn about incomplete documents.
 */
public final class GthScanner {

  /** Result of {@link #scan}. */
  public static final class ScanResult {
    public final List<GthToken> tokens;
    public final int braceBalance;

    public ScanResult(List<GthToken> tokens, int braceBalance) {
      this.tokens = tokens;
      this.braceBalance = braceBalance;
    }
  }

  private static final class StackEntry {
    final String block;
    final boolean isRoot;

    StackEntry(String block, boolean isRoot) {
      this.block = block;
      this.isRoot = isRoot;
    }
  }

  private static final class ReadStringResult {
    final String value;
    final int end;

    ReadStringResult(String value, int end) {
      this.value = value;
      this.end = end;
    }
  }

  private GthScanner() {}

  public static int[] buildLineStarts(String text) {
    List<Integer> starts = new ArrayList<>();
    starts.add(0);
    for (int i = 0; i < text.length(); i++) {
      if (text.charAt(i) == '\n') starts.add(i + 1);
    }
    int[] out = new int[starts.size()];
    for (int i = 0; i < starts.size(); i++) out[i] = starts.get(i);
    return out;
  }

  public static ScanResult scan(String text) {
    List<GthToken> tokens = new ArrayList<>();
    List<StackEntry> stack = new ArrayList<>();
    int objectDepth = 0;
    String pendingOwner = null;
    int n = text.length();
    int i = 0;

    while (i < n) {
      char ch = text.charAt(i);
      if (ch == '"') {
        ReadStringResult read = readString(text, i);
        int valueEnd = read.end;
        int j = valueEnd;
        while (j < n && isWhitespace(text.charAt(j))) j++;
        boolean isKey = j < n && text.charAt(j) == ':';
        String block = nearestBlock(stack);
        GthToken tok =
            new GthToken(
                read.value,
                i,
                valueEnd,
                i + 1,
                Math.max(i + 1, valueEnd - 1),
                block,
                isKey,
                isKey ? null : pendingOwner);
        tokens.add(tok);
        pendingOwner = isKey ? read.value : null;
        i = valueEnd;
        continue;
      }
      if (ch == '{') {
        String block = null;
        if (objectDepth == 1 && pendingOwner != null && ThemeSchema.isBlock(pendingOwner)) {
          block = pendingOwner;
        }
        stack.add(new StackEntry(block, objectDepth == 0));
        objectDepth++;
        pendingOwner = null;
        i++;
        continue;
      }
      if (ch == '}') {
        if (!stack.isEmpty()) {
          stack.remove(stack.size() - 1);
          objectDepth = Math.max(0, objectDepth - 1);
        }
        pendingOwner = null;
        i++;
        continue;
      }
      if (ch == '[') {
        i++;
        continue;
      }
      if (ch == ']' || ch == ',') {
        pendingOwner = null;
      }
      i++;
    }

    int braceBalance = 0;
    for (int k = 0; k < n; k++) {
      if (text.charAt(k) == '{') braceBalance++;
      else if (text.charAt(k) == '}') braceBalance--;
    }
    return new ScanResult(tokens, braceBalance);
  }

  private static String nearestBlock(List<StackEntry> stack) {
    for (int i = stack.size() - 1; i >= 0; i--) {
      if (stack.get(i).block != null) return stack.get(i).block;
    }
    return null;
  }

  private static boolean isWhitespace(char ch) {
    return ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r';
  }

  /** Reads a quoted string starting at {@code start} (which must point at {@code "}). */
  private static ReadStringResult readString(String text, int start) {
    StringBuilder value = new StringBuilder();
    int n = text.length();
    int i = start + 1;
    while (i < n) {
      char ch = text.charAt(i);
      if (ch == '\\') {
        if (i + 1 < n) value.append(text.charAt(i + 1));
        i += 2;
        continue;
      }
      if (ch == '"') {
        i++;
        break;
      }
      value.append(ch);
      i++;
    }
    return new ReadStringResult(value.toString(), i);
  }
}