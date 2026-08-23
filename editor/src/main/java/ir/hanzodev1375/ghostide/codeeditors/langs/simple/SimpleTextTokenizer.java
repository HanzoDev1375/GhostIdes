/**
 * Comment by ghost ide
 *
 * @author: Ninjacoder
 */
package ir.hanzodev1375.ghostide.codeeditors.langs.simple;

public class SimpleTextTokenizer {

  private final SimpleLangConfig cfg;

  private CharSequence source;
  private int bufferLen;
  public int offset;
  public int length;

  public SimpleTextTokenizer(SimpleLangConfig cfg, CharSequence src) {
    if (cfg == null) throw new IllegalArgumentException("config cannot be null");
    if (src == null) throw new IllegalArgumentException("src cannot be null");
    this.cfg = cfg;
    this.source = src;
    init();
  }

  private void init() {
    length = 0;
    offset = 0;
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

  public SimpleTokens nextToken() {
    offset += length;
    if (offset >= bufferLen) return SimpleTokens.EOF;
    char ch = source.charAt(offset);
    length = 1;
    if (ch == '\n') return SimpleTokens.NEWLINE;
    if (ch == '\r') {
      if (offset + 1 < bufferLen && source.charAt(offset + 1) == '\n') length++;
      return SimpleTokens.NEWLINE;
    }
    if (isWhitespace(ch)) {
      while (offset + length < bufferLen && isWhitespace(source.charAt(offset + length))) {
        length++;
      }
      return SimpleTokens.WHITESPACE;
    }
    for (String marker : cfg.lineComments) {
      if (!marker.isEmpty() && marker.charAt(0) == ch && matchMarker(marker)) {
        return scanToLineEnd(SimpleTokens.LINE_COMMENT);
      }
    }
    if (cfg.blockComment
        && ch == '/'
        && offset + 1 < bufferLen
        && source.charAt(offset + 1) == '*') {
      length++;
      char pre = 0, cur = 0;
      boolean finished = false;
      while (offset + length < bufferLen) {
        pre = cur;
        cur = source.charAt(offset + length);
        if (pre == '*' && cur == '/') {
          length++;
          finished = true;
          break;
        }
        length++;
      }
      return finished ? SimpleTokens.BLOCK_COMMENT_COMPLETE : SimpleTokens.BLOCK_COMMENT_INCOMPLETE;
    }
    if (ch == '"') {
      if (cfg.tripleStrings
          && offset + 2 < bufferLen
          && source.charAt(offset + 1) == '"'
          && source.charAt(offset + 2) == '"') {
        return scanTripleString();
      }
      return scanQuoted('"', SimpleTokens.STRING_LITERAL);
    }
    if (ch == '\'') {
      return scanQuoted('\'', SimpleTokens.CHARACTER_LITERAL);
    }
    if (cfg.annotations
        && ch == '@'
        && offset + 1 < bufferLen
        && isIdentifierStart(source.charAt(offset + 1))) {
      length++;
      while (offset + length < bufferLen && isIdentifierPart(source.charAt(offset + length))) {
        length++;
      }
      return SimpleTokens.ANNOTATION;
    }
    if (cfg.hashDirectives
        && ch == '#'
        && offset + 1 < bufferLen
        && isIdentifierStart(source.charAt(offset + 1))) {
      length++;
      while (offset + length < bufferLen && isIdentifierPart(source.charAt(offset + length))) {
        length++;
      }
      return SimpleTokens.ANNOTATION;
    }
    if (cfg.sigilVars
        && (ch == '$' || ch == '@' || ch == '%')
        && offset + 1 < bufferLen
        && isIdentifierPart(source.charAt(offset + 1))) {
      length++;
      while (offset + length < bufferLen && isIdentifierPart(source.charAt(offset + length))) {
        length++;
      }
      return SimpleTokens.IDENTIFIER;
    }
    if (isDigit(ch)
        || (ch == '.' && offset + 1 < bufferLen && isDigit(source.charAt(offset + 1)))) {
      return scanNumber();
    }
    if (isIdentifierStart(ch)) {
      return scanIdentifier(ch);
    }
    switch (ch) {
      case '(':
        return SimpleTokens.LPAREN;
      case ')':
        return SimpleTokens.RPAREN;
      case '{':
        return SimpleTokens.LBRACE;
      case '}':
        return SimpleTokens.RBRACE;
      case '[':
        return SimpleTokens.LBRACK;
      case ']':
        return SimpleTokens.RBRACK;
      case ',':
        return SimpleTokens.COMMA;
      case ';':
        return SimpleTokens.SEMICOLON;
      case '~':
        return SimpleTokens.TILDE;
      case '?':
        return SimpleTokens.QUESTION;
      case '`':
        return SimpleTokens.BACKTICK;
      case '@':
        return SimpleTokens.AT;
      case '#':
        return SimpleTokens.HASH;
      case '$':
        return SimpleTokens.DOLLAR;
      case ':':
        if (offset + 1 < bufferLen && source.charAt(offset + 1) == ':') {
          length++;
          return SimpleTokens.DOUBLE_COLON;
        }
        return SimpleTokens.COLON;
      case '.':
        return SimpleTokens.DOT;
      case '+':
        if (offset + 1 < bufferLen) {
          char n = source.charAt(offset + 1);
          if (n == '+') {
            length++;
            return SimpleTokens.INC;
          }
          if (n == '=') {
            length++;
            return SimpleTokens.PLUS_ASSIGN;
          }
        }
        return SimpleTokens.PLUS;
      case '-':
        if (offset + 1 < bufferLen) {
          char n = source.charAt(offset + 1);
          if (n == '-') {
            length++;
            return SimpleTokens.DEC;
          }
          if (n == '=') {
            length++;
            return SimpleTokens.MINUS_ASSIGN;
          }
          if (n == '>') {
            length++;
            return SimpleTokens.ARROW;
          }
        }
        return SimpleTokens.MINUS;
      case '*':
        if (offset + 1 < bufferLen && source.charAt(offset + 1) == '=') {
          length++;
          return SimpleTokens.STAR_ASSIGN;
        }
        return SimpleTokens.STAR;
      case '/':
        if (offset + 1 < bufferLen && source.charAt(offset + 1) == '=') {
          length++;
          return SimpleTokens.SLASH_ASSIGN;
        }
        return SimpleTokens.SLASH;
      case '%':
        if (offset + 1 < bufferLen && source.charAt(offset + 1) == '=') {
          length++;
          return SimpleTokens.PERCENT_ASSIGN;
        }
        return SimpleTokens.PERCENT;
      case '&':
        if (offset + 1 < bufferLen) {
          char n = source.charAt(offset + 1);
          if (n == '&') {
            length++;
            return SimpleTokens.LOGICAL_AND;
          }
          if (n == '=') {
            length++;
            return SimpleTokens.AND_ASSIGN;
          }
        }
        return SimpleTokens.AMPERSAND;
      case '|':
        if (offset + 1 < bufferLen) {
          char n = source.charAt(offset + 1);
          if (n == '|') {
            length++;
            return SimpleTokens.LOGICAL_OR;
          }
          if (n == '=') {
            length++;
            return SimpleTokens.OR_ASSIGN;
          }
        }
        return SimpleTokens.PIPE;
      case '^':
        if (offset + 1 < bufferLen && source.charAt(offset + 1) == '=') {
          length++;
          return SimpleTokens.XOR_ASSIGN;
        }
        return SimpleTokens.CARET;
      case '=':
        if (offset + 1 < bufferLen) {
          char n = source.charAt(offset + 1);
          if (n == '=') {
            length++;
            return SimpleTokens.EQ;
          }
          if (n == '>') {
            length++;
            return SimpleTokens.FAT_ARROW;
          }
        }
        return SimpleTokens.ASSIGN;
      case '!':
        if (offset + 1 < bufferLen && source.charAt(offset + 1) == '=') {
          length++;
          return SimpleTokens.NOT_EQ;
        }
        return SimpleTokens.NOT;
      case '<':
        if (offset + 1 < bufferLen) {
          char n = source.charAt(offset + 1);
          if (n == '=') {
            length++;
            return SimpleTokens.LT_EQ;
          }
          if (n == '<') {
            length++;
            return SimpleTokens.SHIFT_LEFT;
          }
        }
        return SimpleTokens.LT;
      case '>':
        if (offset + 1 < bufferLen) {
          char n = source.charAt(offset + 1);
          if (n == '=') {
            length++;
            return SimpleTokens.GT_EQ;
          }
          if (n == '>') {
            length++;
            return SimpleTokens.SHIFT_RIGHT;
          }
        }
        return SimpleTokens.GT;
      default:
        return SimpleTokens.UNKNOWN;
    }
  }

  private boolean matchMarker(String marker) {
    for (int i = 0; i < marker.length(); i++) {
      if (offset + i >= bufferLen || source.charAt(offset + i) != marker.charAt(i)) {
        return false;
      }
    }
    length = marker.length();
    return true;
  }

  private SimpleTokens scanToLineEnd(SimpleTokens target) {
    while (offset + length < bufferLen && source.charAt(offset + length) != '\n') {
      length++;
    }
    return target;
  }

  private SimpleTokens scanQuoted(char quote, SimpleTokens target) {
    while (offset + length < bufferLen && source.charAt(offset + length) != quote) {
      if (source.charAt(offset + length) == '\\') {
        length++;
        if (offset + length < bufferLen) length++;
      } else {
        length++;
      }
    }
    if (offset + length < bufferLen) length++;
    return target;
  }

  private SimpleTokens scanTripleString() {
    length = 3;
    while (offset + length < bufferLen && source.charAt(offset + length) != '\n') {
      if (source.charAt(offset + length) == '"'
          && offset + length + 2 < bufferLen
          && source.charAt(offset + length + 1) == '"'
          && source.charAt(offset + length + 2) == '"') {
        length += 3;
        return SimpleTokens.STRING_BLOCK_COMPLETE;
      }
      length++;
    }
    return SimpleTokens.STRING_BLOCK_INCOMPLETE;
  }

  private SimpleTokens scanNumber() {
    boolean isFloat = false;
    if (offset + 1 < bufferLen && source.charAt(offset) == '0') {
      char next = source.charAt(offset + 1);
      if (next == 'x' || next == 'X') {
        length++;
        while (offset + length < bufferLen && isHexDigit(source.charAt(offset + length))) length++;
        return SimpleTokens.INTEGER_LITERAL;
      }
      if (next == 'b' || next == 'B') {
        length++;
        while (offset + length < bufferLen
            && (source.charAt(offset + length) == '0' || source.charAt(offset + length) == '1'))
          length++;
        return SimpleTokens.INTEGER_LITERAL;
      }
      if (next == 'o' || next == 'O') {
        length++;
        while (offset + length < bufferLen
            && source.charAt(offset + length) >= '0'
            && source.charAt(offset + length) <= '7') length++;
        return SimpleTokens.INTEGER_LITERAL;
      }
    }
    while (offset + length < bufferLen
        && (isDigit(source.charAt(offset + length)) || source.charAt(offset + length) == '_')) {
      length++;
    }
    if (offset + length < bufferLen && source.charAt(offset + length) == '.') {
      isFloat = true;
      length++;
      while (offset + length < bufferLen && isDigit(source.charAt(offset + length))) length++;
    }
    if (offset + length < bufferLen
        && (source.charAt(offset + length) == 'e' || source.charAt(offset + length) == 'E')) {
      int save = length;
      length++;
      if (offset + length < bufferLen
          && (source.charAt(offset + length) == '+' || source.charAt(offset + length) == '-'))
        length++;
      if (offset + length < bufferLen && isDigit(source.charAt(offset + length))) {
        isFloat = true;
        while (offset + length < bufferLen && isDigit(source.charAt(offset + length))) length++;
      } else {
        length = save;
      }
    }
    return isFloat ? SimpleTokens.FLOATING_LITERAL : SimpleTokens.INTEGER_LITERAL;
  }

  private SimpleTokens scanIdentifier(char first) {
    while (offset + length < bufferLen && isIdentifierPart(source.charAt(offset + length))) {
      length++;
    }
    String text = source.subSequence(offset, offset + length).toString();
    if (cfg.keywords.get(text, 0, text.length()) != null) return SimpleTokens.KEYWORD;
    if (cfg.types.get(text, 0, text.length()) != null) return SimpleTokens.TYPE_KEYWORD;
    if (cfg.builtins.get(text, 0, text.length()) != null) return SimpleTokens.BUILTIN;
    return SimpleTokens.IDENTIFIER;
  }

  private static boolean isWhitespace(char c) {
    return c == ' ' || c == '\t' || c == '\f';
  }

  private static boolean isDigit(char c) {
    return c >= '0' && c <= '9';
  }

  private static boolean isHexDigit(char c) {
    return isDigit(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
  }

  private static boolean isIdentifierStart(char c) {
    return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
  }

  private static boolean isIdentifierPart(char c) {
    return isIdentifierStart(c) || isDigit(c);
  }
}
