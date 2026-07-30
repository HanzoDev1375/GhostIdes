package ir.hanzodev1375.ghostide.codeeditors.langs.ruby;

import io.github.rosemoe.sora.util.TrieTree;

public class RubyTextTokenizer {

  private static TrieTree<RubyTokens> keywords;

  static {
    doStaticInit();
  }

  public static TrieTree<RubyTokens> getTree() {
    return keywords;
  }

  private CharSequence source;
  private int bufferLen;
  public int offset;
  public int length;
  private RubyTokens currToken;

  public RubyTextTokenizer(CharSequence src) {
    if (src == null) throw new IllegalArgumentException("src cannot be null");
    this.source = src;
    init();
  }

  private void init() {
    length = 0;
    offset = 0;
    currToken = RubyTokens.WHITESPACE;
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

  public RubyTokens nextToken() {
    return currToken = nextTokenInternal();
  }

  private RubyTokens nextTokenInternal() {
    offset += length;
    if (offset >= bufferLen) return RubyTokens.EOF;

    char ch = source.charAt(offset);
    length = 1;

    if (ch == '\n') return RubyTokens.NEWLINE;
    if (ch == '\r') {
      if (offset + 1 < bufferLen && source.charAt(offset + 1) == '\n') length++;
      return RubyTokens.NEWLINE;
    }
    if (ch == ' ' || ch == '\t') {
      while (offset + length < bufferLen
          && (source.charAt(offset + length) == ' ' || source.charAt(offset + length) == '\t')) {
        length++;
      }
      return RubyTokens.WHITESPACE;
    }

    if (ch == '#') {
      while (offset + length < bufferLen && source.charAt(offset + length) != '\n') length++;
      return RubyTokens.LINE_COMMENT;
    }

    if (offset == 0 && ch == '=' && startsWith(offset, "=begin")) {
      length = "=begin".length();
      int idx = indexOf(offset + length, "=end");
      if (idx >= 0) {
        length = (idx - offset) + "=end".length();
      } else {
        length = bufferLen - offset;
      }
      return RubyTokens.BLOCK_COMMENT;
    }

    if (ch == '"' || ch == '\'') {
      char quote = ch;
      while (offset + length < bufferLen) {
        char c = source.charAt(offset + length);
        if (c == quote) {
          length++;
          break;
        }
        if (c == '\\' && offset + length + 1 < bufferLen) {
          length += 2;
          continue;
        }
        length++;
      }
      return RubyTokens.LITERAL;
    }

    if (ch == '$') {
      length++;
      while (offset + length < bufferLen && isIdentPart(source.charAt(offset + length))) length++;
      return RubyTokens.ID_GLOBAL;
    }

    if (isDigit(ch)) {
      return scanNumber();
    }
    if (ch == '.' && offset + 1 < bufferLen && isDigit(source.charAt(offset + 1))) {
      return scanNumber();
    }

    if (isIdentStart(ch)) {
      return scanIdentifier(ch);
    }

    switch (ch) {
      case '+':
        return scanTwo('=', RubyTokens.PLUS_ASSIGN, RubyTokens.PLUS);
      case '-':
        return scanTwo('=', RubyTokens.MINUS_ASSIGN, RubyTokens.MINUS);
      case '*':
        return scanStar();
      case '/':
        return scanTwo('=', RubyTokens.DIV_ASSIGN, RubyTokens.DIV);
      case '%':
        return scanTwo('=', RubyTokens.MOD_ASSIGN, RubyTokens.MOD);
      case '=':
        return scanTwo('=', RubyTokens.EQUAL, RubyTokens.ASSIGN);
      case '!':
        return scanTwo('=', RubyTokens.NOT_EQUAL, RubyTokens.NOT);
      case '>':
        return scanGreater();
      case '<':
        return scanLess();
      case '&':
        return scanTwo('&', RubyTokens.AND, RubyTokens.BIT_AND);
      case '|':
        return scanTwo('|', RubyTokens.OR, RubyTokens.BIT_OR);
      case '^':
        return RubyTokens.BIT_XOR;
      case '~':
        return RubyTokens.BIT_NOT;
      case '(':
        return RubyTokens.LEFT_RBRACKET;
      case ')':
        return RubyTokens.RIGHT_RBRACKET;
      case '[':
        return RubyTokens.LEFT_SBRACKET;
      case ']':
        return RubyTokens.RIGHT_SBRACKET;
      case ',':
        return RubyTokens.COMMA;
      case ';':
        return RubyTokens.SEMICOLON;
      default:
        return RubyTokens.UNKNOWN;
    }
  }

  private RubyTokens scanTwo(char second, RubyTokens ifMatch, RubyTokens otherwise) {
    if (offset + 1 < bufferLen && source.charAt(offset + 1) == second) {
      length++;
      return ifMatch;
    }
    return otherwise;
  }

  private RubyTokens scanStar() {
    if (offset + 1 < bufferLen && source.charAt(offset + 1) == '*') {
      length++;
      if (offset + length < bufferLen && source.charAt(offset + length) == '=') {
        length++;
        return RubyTokens.EXP_ASSIGN;
      }
      return RubyTokens.EXP;
    }
    return scanTwo('=', RubyTokens.MUL_ASSIGN, RubyTokens.MUL);
  }

  private RubyTokens scanGreater() {
    if (offset + 1 < bufferLen) {
      char n = source.charAt(offset + 1);
      if (n == '=') {
        length++;
        return RubyTokens.GREATER_EQUAL;
      }
      if (n == '>') {
        length++;
        return RubyTokens.BIT_SHR;
      }
    }
    return RubyTokens.GREATER;
  }

  private RubyTokens scanLess() {
    if (offset + 1 < bufferLen) {
      char n = source.charAt(offset + 1);
      if (n == '=') {
        length++;
        return RubyTokens.LESS_EQUAL;
      }
      if (n == '<') {
        length++;
        return RubyTokens.BIT_SHL;
      }
    }
    return RubyTokens.LESS;
  }

  private RubyTokens scanNumber() {
    boolean isFloat = false;
    while (offset + length < bufferLen && isDigit(source.charAt(offset + length))) length++;
    if (offset + length < bufferLen
        && source.charAt(offset + length) == '.'
        && offset + length + 1 < bufferLen
        && isDigit(source.charAt(offset + length + 1))) {
      isFloat = true;
      length++;
      while (offset + length < bufferLen && isDigit(source.charAt(offset + length))) length++;
    }
    return isFloat ? RubyTokens.FLOAT : RubyTokens.INT;
  }

  private RubyTokens scanIdentifier(char first) {
    TrieTree.Node<RubyTokens> node = keywords.root.map.get(first);
    while (offset + length < bufferLen && isIdentPart(source.charAt(offset + length))) {
      char c = source.charAt(offset + length);
      node = (node == null) ? null : node.map.get(c);
      length++;
    }
    if (node != null && node.token != null) {
      return node.token;
    }
    if (offset + length < bufferLen && source.charAt(offset + length) == '?') {
      length++;
      return RubyTokens.ID_FUNCTION;
    }
    return RubyTokens.ID;
  }

  private boolean startsWith(int pos, String word) {
    if (pos + word.length() > bufferLen) return false;
    for (int i = 0; i < word.length(); i++) {
      if (source.charAt(pos + i) != word.charAt(i)) return false;
    }
    return true;
  }

  private int indexOf(int from, String word) {
    for (int i = from; i + word.length() <= bufferLen; i++) {
      if (startsWith(i, word)) return i;
    }
    return -1;
  }

  private static boolean isDigit(char c) {
    return c >= '0' && c <= '9';
  }

  private static boolean isIdentStart(char c) {
    return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
  }

  private static boolean isIdentPart(char c) {
    return isIdentStart(c) || isDigit(c);
  }

  private static void doStaticInit() {
    String[] words = {
      "require", "end", "def", "return", "pir", "if", "else", "elsif", "unless", "while", "retry",
      "break", "for", "true", "false", "and", "or", "not", "nil"
    };
    RubyTokens[] tokens = {
      RubyTokens.REQUIRE, RubyTokens.END, RubyTokens.DEF, RubyTokens.RETURN, RubyTokens.PIR,
      RubyTokens.IF, RubyTokens.ELSE, RubyTokens.ELSIF, RubyTokens.UNLESS, RubyTokens.WHILE,
      RubyTokens.RETRY, RubyTokens.BREAK, RubyTokens.FOR, RubyTokens.TRUE, RubyTokens.FALSE,
      RubyTokens.AND, RubyTokens.OR, RubyTokens.NOT, RubyTokens.NIL
    };
    keywords = new TrieTree<>();
    for (int i = 0; i < words.length; i++) {
      keywords.put(words[i], tokens[i]);
    }
  }
}
