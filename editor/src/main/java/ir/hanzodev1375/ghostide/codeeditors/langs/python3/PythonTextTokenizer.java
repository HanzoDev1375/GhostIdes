/**
 * Comment by ghost ide
 *
 * @author: Ninjacoder
 */
package ir.hanzodev1375.ghostide.codeeditors.langs.python3;

import io.github.rosemoe.sora.util.MyCharacter;
import io.github.rosemoe.sora.util.TrieTree;

public class PythonTextTokenizer {

  private static TrieTree<PythonTokens> keywords;

  static {
    doStaticInit();
  }

  public static TrieTree<PythonTokens> getTree() {
    return keywords;
  }

  private CharSequence source;
  private int bufferLen;
  private int index;
  public int offset;
  public int length;
  private PythonTokens currToken;

  // 0 = normal code, 1 = scanning literal text of an f-string, 2 = scanning an f-string
  // embedded expression
  private int mode = 0;
  private char fq;
  private boolean fTriple;
  private boolean fRaw;
  private boolean fIsF;
  private int fExprDepth;

  public PythonTextTokenizer(CharSequence src) {
    if (src == null) throw new IllegalArgumentException("src cannot be null");
    this.source = src;
    init();
  }

  private void init() {
    length = 0;
    index = 0;
    offset = 0;
    currToken = PythonTokens.WHITESPACE;
    this.bufferLen = source.length();
    mode = 0;
  }

  public void reset(CharSequence src) {
    if (src == null) throw new IllegalArgumentException("src cannot be null");
    this.source = src;
    this.bufferLen = src.length();
    init();
  }

  public void resumeTripleQuoteContinuation(
      CharSequence src, char quoteChar, boolean isF, boolean raw) {
    reset(src);
    mode = 1;
    fq = quoteChar;
    fTriple = true;
    fRaw = raw;
    fIsF = isF;
  }

  public int getMode() {
    return mode;
  }

  public char getPendingQuoteChar() {
    return fq;
  }

  public boolean isPendingTriple() {
    return fTriple;
  }

  public boolean isPendingRaw() {
    return fRaw;
  }

  public boolean isPendingFString() {
    return fIsF;
  }

  public CharSequence getTokenText() {
    return source.subSequence(offset, offset + length);
  }

  public int getTokenLength() {
    return length;
  }

  public PythonTokens nextToken() {
    return currToken = nextTokenInternal();
  }

  private PythonTokens nextTokenInternal() {
    index += length;
    offset += length;
    if (offset >= bufferLen) return PythonTokens.EOF;

    if (mode == 1) {
      length = 0;
      return continueFStringText();
    }

    char ch = source.charAt(offset);
    length = 1;

    if (mode == 2 && ch == '}') {
      if (fExprDepth == 0) {
        mode = 1;
        return PythonTokens.FSTRING_EXPR_END;
      }
      fExprDepth--;
      return PythonTokens.RBRACE;
    }
    if (mode == 2 && ch == '{') {
      fExprDepth++;
      return PythonTokens.LBRACE;
    }

    if (ch == '\n') return PythonTokens.NEWLINE;
    if (ch == '\r') {
      if (offset + 1 < bufferLen && source.charAt(offset + 1) == '\n') length++;
      return PythonTokens.NEWLINE;
    }
    if (isWhitespace(ch)) {
      while (offset + length < bufferLen && isWhitespace(source.charAt(offset + length))) {
        length++;
      }
      return PythonTokens.WHITESPACE;
    }

    if (ch == '#') {
      while (offset + length < bufferLen && source.charAt(offset + length) != '\n') length++;
      return PythonTokens.LINE_COMMENT;
    }

    if (ch == '\'' || ch == '"') {
      return scanStringStart(ch, false, false, 0);
    }

    if (isDigit(ch)
        || (ch == '.' && offset + 1 < bufferLen && isDigit(source.charAt(offset + 1)))) {
      return scanNumber();
    }

    if (isIdentifierStart(ch)) {
      return scanIdentifierOrString(ch);
    }

    switch (ch) {
      case '+':
        return scanOperatorTwo(PythonTokens.PLUS, '=', PythonTokens.PLUS_ASSIGN);
      case '-':
        return scanMinus();
      case '*':
        return scanStar();
      case '/':
        return scanSlash();
      case '%':
        return scanOperatorTwo(PythonTokens.PERCENT, '=', PythonTokens.PERCENT_ASSIGN);
      case '@':
        return scanOperatorTwo(PythonTokens.AT, '=', PythonTokens.AT_ASSIGN);
      case '&':
        return scanOperatorTwo(PythonTokens.BIT_AND, '=', PythonTokens.AND_ASSIGN);
      case '|':
        return scanOperatorTwo(PythonTokens.BIT_OR, '=', PythonTokens.OR_ASSIGN);
      case '^':
        return scanOperatorTwo(PythonTokens.BIT_XOR, '=', PythonTokens.XOR_ASSIGN);
      case '~':
        return PythonTokens.BIT_NOT;
      case '=':
        return scanOperatorTwo(PythonTokens.ASSIGN, '=', PythonTokens.EQ);
      case '!':
        if (offset + 1 < bufferLen && source.charAt(offset + 1) == '=') {
          length++;
          return PythonTokens.NOT_EQ;
        }
        return PythonTokens.UNKNOWN;
      case '<':
        return scanLT();
      case '>':
        return scanGT();
      case '.':
        return scanDot();
      case ':':
        return scanOperatorTwo(PythonTokens.COLON, '=', PythonTokens.WALRUS);
      case '{':
        return PythonTokens.LBRACE;
      case '}':
        return PythonTokens.RBRACE;
      case '(':
        return PythonTokens.LPAREN;
      case ')':
        return PythonTokens.RPAREN;
      case '[':
        return PythonTokens.LBRACK;
      case ']':
        return PythonTokens.RBRACK;
      case ';':
        return PythonTokens.SEMICOLON;
      case ',':
        return PythonTokens.COMMA;
      default:
        return PythonTokens.UNKNOWN;
    }
  }

  private PythonTokens continueFStringText() {
    while (offset + length < bufferLen) {
      char c = source.charAt(offset + length);
      if (c == '\n') {
        return PythonTokens.STRING_LITERAL;
      }
      if (!fRaw && c == '\\') {
        length++;
        if (offset + length < bufferLen) length++;
        continue;
      }
      if (fRaw && c == '\\' && offset + length + 1 < bufferLen) {
        length += 2;
        continue;
      }
      if (c == fq) {
        if (fTriple) {
          if (offset + length + 2 < bufferLen
              && source.charAt(offset + length + 1) == fq
              && source.charAt(offset + length + 2) == fq) {
            length += 3;
            mode = 0;
            return PythonTokens.STRING_LITERAL;
          }
        } else {
          length++;
          mode = 0;
          return PythonTokens.STRING_LITERAL;
        }
        length++;
        continue;
      }
      if (c == '{') {
        if (!fIsF) {
          length++;
          continue;
        }
        if (offset + length + 1 < bufferLen && source.charAt(offset + length + 1) == '{') {
          length += 2;
          continue;
        }
        if (length == 0) {
          length = 1;
          mode = 2;
          fExprDepth = 0;
          return PythonTokens.FSTRING_EXPR_START;
        }
        return PythonTokens.STRING_LITERAL;
      }
      if (c == '}') {
        if (!fIsF) {
          length++;
          continue;
        }
        if (offset + length + 1 < bufferLen && source.charAt(offset + length + 1) == '}') {
          length += 2;
          continue;
        }
        length++;
        continue;
      }
      length++;
    }
    return PythonTokens.STRING_LITERAL;
  }

  private PythonTokens scanStringStart(char quote, boolean raw, boolean isF, int prefixLen) {
    int quotePos = offset + prefixLen;
    boolean triple =
        quotePos + 2 < bufferLen
            && source.charAt(quotePos + 1) == quote
            && source.charAt(quotePos + 2) == quote;
    length = prefixLen + (triple ? 3 : 1);
    if (isF) {
      mode = 1;
      fq = quote;
      fTriple = triple;
      fRaw = raw;
      fIsF = true;
      return continueFStringText();
    }
    while (offset + length < bufferLen) {
      char c = source.charAt(offset + length);
      if (!triple && c == '\n') {
        return PythonTokens.STRING_LITERAL;
      }
      if (!raw && c == '\\') {
        length++;
        if (offset + length < bufferLen) length++;
        continue;
      }
      if (raw && c == '\\' && offset + length + 1 < bufferLen) {
        length += 2;
        continue;
      }
      if (c == quote) {
        if (triple) {
          if (offset + length + 2 < bufferLen
              && source.charAt(offset + length + 1) == quote
              && source.charAt(offset + length + 2) == quote) {
            length += 3;
            return PythonTokens.STRING_LITERAL;
          }
          length++;
          continue;
        } else {
          length++;
          return PythonTokens.STRING_LITERAL;
        }
      }
      length++;
    }
    return triple ? PythonTokens.STRING_INCOMPLETE : PythonTokens.STRING_LITERAL;
  }

  private PythonTokens scanIdentifierOrString(char first) {
    int startOffset = offset;
    TrieTree.Node<PythonTokens> node = keywords.root.map.get(first);
    while (offset + length < bufferLen && isIdentifierPart(source.charAt(offset + length))) {
      char c = source.charAt(offset + length);
      node = (node == null) ? null : node.map.get(c);
      length++;
    }
    PythonTokens result =
        (node != null && node.token != null) ? node.token : PythonTokens.IDENTIFIER;
    if (result == PythonTokens.IDENTIFIER && length <= 2 && offset + length < bufferLen) {
      char next = source.charAt(offset + length);
      if (next == '\'' || next == '"') {
        String prefix = source.subSequence(startOffset, startOffset + length).toString();
        String lower = prefix.toLowerCase();
        boolean validPrefix =
            lower.equals("r")
                || lower.equals("b")
                || lower.equals("u")
                || lower.equals("f")
                || lower.equals("rb")
                || lower.equals("br")
                || lower.equals("rf")
                || lower.equals("fr");
        if (validPrefix) {
          boolean isRaw = lower.indexOf('r') >= 0;
          boolean isF = lower.indexOf('f') >= 0;
          return scanStringStart(next, isRaw, isF, length);
        }
      }
    }
    return result;
  }

  private PythonTokens scanNumber() {
    boolean isFloat = false;
    if (source.charAt(offset) == '0'
        && offset + 1 < bufferLen
        && (source.charAt(offset + 1) == 'x' || source.charAt(offset + 1) == 'X')) {
      length++;
      while (offset + length < bufferLen && isHexDigitOrSeparator(source.charAt(offset + length)))
        length++;
      return finishNumberLiteral(false);
    }
    if (source.charAt(offset) == '0'
        && offset + 1 < bufferLen
        && (source.charAt(offset + 1) == 'o' || source.charAt(offset + 1) == 'O')) {
      length++;
      while (offset + length < bufferLen
          && (isDigit(source.charAt(offset + length)) || source.charAt(offset + length) == '_'))
        length++;
      return finishNumberLiteral(false);
    }
    if (source.charAt(offset) == '0'
        && offset + 1 < bufferLen
        && (source.charAt(offset + 1) == 'b' || source.charAt(offset + 1) == 'B')) {
      length++;
      while (offset + length < bufferLen
          && (source.charAt(offset + length) == '0'
              || source.charAt(offset + length) == '1'
              || source.charAt(offset + length) == '_')) length++;
      return finishNumberLiteral(false);
    }
    while (offset + length < bufferLen && isDigitOrSeparator(source.charAt(offset + length)))
      length++;
    if (offset + length < bufferLen && source.charAt(offset + length) == '.') {
      isFloat = true;
      length++;
      while (offset + length < bufferLen && isDigitOrSeparator(source.charAt(offset + length)))
        length++;
    }
    if (offset + length < bufferLen
        && (source.charAt(offset + length) == 'e' || source.charAt(offset + length) == 'E')) {
      isFloat = true;
      length++;
      if (offset + length < bufferLen
          && (source.charAt(offset + length) == '+' || source.charAt(offset + length) == '-')) {
        length++;
      }
      while (offset + length < bufferLen && isDigit(source.charAt(offset + length))) length++;
    }
    return finishNumberLiteral(isFloat);
  }

  private PythonTokens finishNumberLiteral(boolean isFloat) {
    if (offset + length < bufferLen
        && (source.charAt(offset + length) == 'j' || source.charAt(offset + length) == 'J')) {
      length++;
      return PythonTokens.IMAGINARY_LITERAL;
    }
    return isFloat ? PythonTokens.FLOATING_LITERAL : PythonTokens.INTEGER_LITERAL;
  }

  private PythonTokens scanMinus() {
    if (offset + 1 < bufferLen) {
      char n = source.charAt(offset + 1);
      if (n == '=') {
        length++;
        return PythonTokens.MINUS_ASSIGN;
      }
      if (n == '>') {
        length++;
        return PythonTokens.ARROW;
      }
    }
    return PythonTokens.MINUS;
  }

  private PythonTokens scanStar() {
    if (offset + 1 < bufferLen && source.charAt(offset + 1) == '*') {
      length++;
      if (offset + length < bufferLen && source.charAt(offset + length) == '=') {
        length++;
        return PythonTokens.DOUBLE_STAR_ASSIGN;
      }
      return PythonTokens.DOUBLE_STAR;
    }
    return scanOperatorTwo(PythonTokens.STAR, '=', PythonTokens.STAR_ASSIGN);
  }

  private PythonTokens scanSlash() {
    if (offset + 1 < bufferLen && source.charAt(offset + 1) == '/') {
      length++;
      if (offset + length < bufferLen && source.charAt(offset + length) == '=') {
        length++;
        return PythonTokens.DOUBLE_SLASH_ASSIGN;
      }
      return PythonTokens.DOUBLE_SLASH;
    }
    return scanOperatorTwo(PythonTokens.SLASH, '=', PythonTokens.SLASH_ASSIGN);
  }

  private PythonTokens scanLT() {
    if (offset + 1 < bufferLen) {
      char n = source.charAt(offset + 1);
      if (n == '=') {
        length++;
        return PythonTokens.LT_EQ;
      }
      if (n == '<') {
        length++;
        if (offset + length < bufferLen && source.charAt(offset + length) == '=') {
          length++;
          return PythonTokens.SHIFT_LEFT_ASSIGN;
        }
        return PythonTokens.SHIFT_LEFT;
      }
    }
    return PythonTokens.LT;
  }

  private PythonTokens scanGT() {
    if (offset + 1 < bufferLen) {
      char n = source.charAt(offset + 1);
      if (n == '=') {
        length++;
        return PythonTokens.GT_EQ;
      }
      if (n == '>') {
        length++;
        if (offset + length < bufferLen && source.charAt(offset + length) == '=') {
          length++;
          return PythonTokens.SHIFT_RIGHT_ASSIGN;
        }
        return PythonTokens.SHIFT_RIGHT;
      }
    }
    return PythonTokens.GT;
  }

  private PythonTokens scanDot() {
    if (offset + 2 < bufferLen
        && source.charAt(offset + 1) == '.'
        && source.charAt(offset + 2) == '.') {
      length += 2;
      return PythonTokens.ELLIPSIS;
    }
    return PythonTokens.DOT;
  }

  private PythonTokens scanOperatorTwo(
      PythonTokens single, char nextChar, PythonTokens doubleToken) {
    if (offset + 1 < bufferLen && source.charAt(offset + 1) == nextChar) {
      length++;
      return doubleToken;
    }
    return single;
  }

  private static boolean isWhitespace(char c) {
    return c == ' ' || c == '\t' || c == '\f';
  }

  private static boolean isDigit(char c) {
    return c >= '0' && c <= '9';
  }

  private static boolean isDigitOrSeparator(char c) {
    return isDigit(c) || c == '_';
  }

  private static boolean isHexDigit(char c) {
    return isDigit(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
  }

  private static boolean isHexDigitOrSeparator(char c) {
    return isHexDigit(c) || c == '_';
  }

  private static boolean isIdentifierStart(char c) {
    return MyCharacter.isJavaIdentifierStart(c);
  }

  private static boolean isIdentifierPart(char c) {
    return MyCharacter.isJavaIdentifierPart(c);
  }

  private static void doStaticInit() {
    String[] words = {
      "False",
      "None",
      "True",
      "and",
      "as",
      "assert",
      "async",
      "await",
      "break",
      "class",
      "continue",
      "def",
      "del",
      "elif",
      "else",
      "except",
      "finally",
      "for",
      "from",
      "global",
      "if",
      "import",
      "in",
      "is",
      "lambda",
      "nonlocal",
      "not",
      "or",
      "pass",
      "raise",
      "return",
      "try",
      "while",
      "with",
      "yield"
    };
    PythonTokens[] tokens = {
      PythonTokens.FALSE_, PythonTokens.NONE_, PythonTokens.TRUE_, PythonTokens.AND,
      PythonTokens.AS, PythonTokens.ASSERT, PythonTokens.ASYNC, PythonTokens.AWAIT,
      PythonTokens.BREAK, PythonTokens.CLASS, PythonTokens.CONTINUE, PythonTokens.DEF,
      PythonTokens.DEL, PythonTokens.ELIF, PythonTokens.ELSE, PythonTokens.EXCEPT,
      PythonTokens.FINALLY, PythonTokens.FOR, PythonTokens.FROM, PythonTokens.GLOBAL,
      PythonTokens.IF, PythonTokens.IMPORT, PythonTokens.IN, PythonTokens.IS,
      PythonTokens.LAMBDA, PythonTokens.NONLOCAL, PythonTokens.NOT, PythonTokens.OR,
      PythonTokens.PASS, PythonTokens.RAISE, PythonTokens.RETURN, PythonTokens.TRY,
      PythonTokens.WHILE, PythonTokens.WITH, PythonTokens.YIELD
    };
    keywords = new TrieTree<>();
    for (int i = 0; i < words.length; i++) {
      keywords.put(words[i], tokens[i]);
    }
  }
}
