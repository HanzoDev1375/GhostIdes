package ir.hanzodev1375.ghostide.codeeditors.langs.json;

public class JsonTextTokenizer {

  private CharSequence source;
  private int bufferLen;
  public int offset;
  public int length;
  private JsonTokens currToken;

  public JsonTextTokenizer(CharSequence src) {
    if (src == null) throw new IllegalArgumentException("src cannot be null");
    this.source = src;
    init();
  }

  private void init() {
    length = 0;
    offset = 0;
    currToken = JsonTokens.WHITESPACE;
    this.bufferLen = source.length();
  }

  public void reset(CharSequence src) {
    if (src == null) throw new IllegalArgumentException("src cannot be null");
    this.source = src;
    this.bufferLen = src.length();
    init();
  }

  public CharSequence getTokenText() {
    return source.subSequence(offset, offset + length);
  }

  public int getTokenLength() {
    return length;
  }

  public JsonTokens nextToken() {
    return currToken = nextTokenInternal();
  }

  private JsonTokens nextTokenInternal() {
    offset += length;
    if (offset >= bufferLen) return JsonTokens.EOF;

    char ch = source.charAt(offset);
    length = 1;

    if (ch == '\n') return JsonTokens.NEWLINE;
    if (ch == '\r') {
      if (offset + 1 < bufferLen && source.charAt(offset + 1) == '\n') length++;
      return JsonTokens.NEWLINE;
    }
    if (ch == ' ' || ch == '\t') {
      while (offset + length < bufferLen && (source.charAt(offset + length) == ' '
          || source.charAt(offset + length) == '\t')) {
        length++;
      }
      return JsonTokens.WHITESPACE;
    }

    if (ch == '"') {
      while (offset + length < bufferLen) {
        char c = source.charAt(offset + length);
        if (c == '"') {
          length++;
          break;
        }
        if (c == '\\' && offset + length + 1 < bufferLen) {
          length += 2;
          continue;
        }
        length++;
      }
      return JsonTokens.STRING;
    }

    if (isDigit(ch) || (ch == '-' && offset + 1 < bufferLen && isDigit(source.charAt(offset + 1)))) {
      return scanNumber();
    }

    if (isIdentStart(ch)) {
      return scanIdentifier();
    }

    switch (ch) {
      case '{':
        return JsonTokens.LBRACE;
      case '}':
        return JsonTokens.RBRACE;
      case '[':
        return JsonTokens.LBRACKET;
      case ']':
        return JsonTokens.RBRACKET;
      case ':':
        return JsonTokens.COLON;
      case ',':
        return JsonTokens.COMMA;
      default:
        return JsonTokens.UNKNOWN;
    }
  }

  private JsonTokens scanNumber() {
    if (source.charAt(offset) == '-') {
      length++;
    }
    while (offset + length < bufferLen && isDigit(source.charAt(offset + length))) length++;
    if (offset + length < bufferLen && source.charAt(offset + length) == '.') {
      length++;
      while (offset + length < bufferLen && isDigit(source.charAt(offset + length))) length++;
    }
    if (offset + length < bufferLen
        && (source.charAt(offset + length) == 'e' || source.charAt(offset + length) == 'E')) {
      length++;
      if (offset + length < bufferLen
          && (source.charAt(offset + length) == '+' || source.charAt(offset + length) == '-')) {
        length++;
      }
      while (offset + length < bufferLen && isDigit(source.charAt(offset + length))) length++;
    }
    return JsonTokens.NUMBER;
  }

  private JsonTokens scanIdentifier() {
    int start = offset;
    while (offset + length < bufferLen && isIdentPart(source.charAt(offset + length))) length++;
    int len = length;
    if (matches(start, len, "true")) return JsonTokens.TRUE;
    if (matches(start, len, "false")) return JsonTokens.FALSE;
    if (matches(start, len, "null")) return JsonTokens.NULL;
    return JsonTokens.UNKNOWN;
  }

  private boolean matches(int start, int len, String word) {
    if (len != word.length()) return false;
    for (int i = 0; i < len; i++) {
      if (source.charAt(start + i) != word.charAt(i)) return false;
    }
    return true;
  }

  private static boolean isDigit(char c) {
    return c >= '0' && c <= '9';
  }

  private static boolean isIdentStart(char c) {
    return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
  }

  private static boolean isIdentPart(char c) {
    return isIdentStart(c) || isDigit(c);
  }
}
