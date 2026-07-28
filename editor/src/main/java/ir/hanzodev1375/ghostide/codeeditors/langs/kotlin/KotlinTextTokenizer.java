package ir.hanzodev1375.ghostide.codeeditors.langs.kotlin;

import io.github.rosemoe.sora.util.MyCharacter;
import io.github.rosemoe.sora.util.TrieTree;

public class KotlinTextTokenizer {

  private static TrieTree<Tokens> keywords;

  static {
    doStaticInit();
  }

  public static TrieTree<Tokens> getTree() {
    return keywords;
  }

  private CharSequence source;
  private int bufferLen;
  private int line;
  private int column;
  private int index;
  public int offset;
  public int length;
  private Tokens currToken;
  private boolean lcCal;
  private int mode = 0;
  private boolean tripleQuote;
  private int exprDepth;

  public KotlinTextTokenizer(CharSequence src) {
    if (src == null) {
      throw new IllegalArgumentException("src can not be null");
    }
    this.source = src;
    init();
  }

  private void init() {
    line = 0;
    column = 0;
    length = 0;
    index = 0;
    offset = 0;
    currToken = Tokens.WHITESPACE;
    lcCal = false;
    mode = 0;
    this.bufferLen = source.length();
  }

  public void resumeStringContinuation(CharSequence src, boolean triple) {
    reset(src);
    mode = 1;
    tripleQuote = triple;
  }

  public int getMode() {
    return mode;
  }

  public boolean isPendingTriple() {
    return tripleQuote;
  }

  // متد reset برای ریست کردن توکنایزر با سورس جدید
  public void reset(CharSequence src) {
    if (src == null) {
      throw new IllegalArgumentException("src can not be null");
    }
    this.source = src;
    this.bufferLen = src.length();
    init();
  }

  public void setCalculateLineColumn(boolean cal) {
    this.lcCal = cal;
  }

  public void pushBack(int length) {
    if (length > getTokenLength()) {
      throw new IllegalArgumentException("pushBack length too large");
    }
    this.length -= length;
  }

  private boolean isIdentifierPart(char ch) {
    return MyCharacter.isJavaIdentifierPart(ch) || ch == '_' || ch == '$';
  }

  private boolean isIdentifierStart(char ch) {
    return MyCharacter.isJavaIdentifierStart(ch) || ch == '_' || ch == '$';
  }

  public CharSequence getTokenText() {
    return source.subSequence(offset, offset + length);
  }

  public int getTokenLength() {
    return length;
  }

  public int getLine() {
    return line;
  }

  public int getColumn() {
    return column;
  }

  public int getIndex() {
    return index;
  }

  public Tokens getToken() {
    return currToken;
  }

  private char charAt(int i) {
    return source.charAt(i);
  }

  private char charAt() {
    return source.charAt(offset + length);
  }

  public Tokens nextToken() {
    return currToken = nextTokenInternal();
  }

  private Tokens nextTokenInternal() {
    if (lcCal) {
      boolean r = false;
      for (int i = offset; i < offset + length; i++) {
        char ch = charAt(i);
        if (ch == '\r') {
          r = true;
          line++;
          column = 0;
        } else if (ch == '\n') {
          if (r) {
            r = false;
            continue;
          }
          line++;
          column = 0;
        } else {
          r = false;
          column++;
        }
      }
    }
    index = index + length;
    offset = offset + length;
    if (offset >= bufferLen) {
      return Tokens.EOF;
    }
    if (mode == 1) {
      length = 0;
      return continueStringText();
    }
    if (mode == 3) {
      return scanDollarIdentifier();
    }
    char ch = source.charAt(offset);
    length = 1;
    if (mode == 2 && ch == '}') {
      if (exprDepth == 0) {
        mode = 1;
        return Tokens.STRING_TEMPLATE_EXPR_END;
      }
      exprDepth--;
      return Tokens.RBRACE;
    }
    if (mode == 2 && ch == '{') {
      exprDepth++;
      return Tokens.LBRACE;
    }
    if (ch == '\n') {
      return Tokens.NEWLINE;
    } else if (ch == '\r') {
      scanNewline();
      return Tokens.NEWLINE;
    } else if (isWhitespace(ch)) {
      char chLocal;
      while (offset + length < bufferLen && isWhitespace(chLocal = charAt(offset + length))) {
        if (chLocal == '\r' || chLocal == '\n') {
          break;
        }
        length++;
      }
      return Tokens.WHITESPACE;
    } else {
      if (isIdentifierStart(ch)) {
        return scanIdentifier(ch);
      }
      if (isPrimeDigit(ch)) {
        return scanNumber();
      }
      if (ch == ';') {
        return Tokens.SEMICOLON;
      } else if (ch == '(') {
        return Tokens.LPAREN;
      } else if (ch == ')') {
        return Tokens.RPAREN;
      } else if (ch == ':') {
        return Tokens.COLON;
      } else if (ch == '<') {
        return scanLT();
      } else if (ch == '>') {
        return scanGT();
      }
      switch (ch) {
        case '=':
          return scanOperatorTwo(Tokens.EQ, '=', Tokens.ASSIGN);
        case '.':
          return scanDot();
        case '@':
          return Tokens.AT;
        case '{':
          return Tokens.LBRACE;
        case '}':
          return Tokens.RBRACE;
        case '/':
          return scanDIV();
        case '*':
          return scanOperatorTwo(Tokens.MULT, '=', Tokens.MULT_ASSIGN);
        case '-':
          return scanMinus();
        case '+':
          return scanPlus();
        case '[':
          return Tokens.LBRACK;
        case ']':
          return Tokens.RBRACK;
        case ',':
          return Tokens.COMMA;
        case '!':
          return scanOperatorTwo(Tokens.NOT, '=', Tokens.NOT);
        case '~':
          return Tokens.COMP;
        case '?':
          return scanQuestion();
        case '&':
          return scanOperatorTwo(Tokens.AND, '=', Tokens.AND);
        case '|':
          return scanOperatorTwo(Tokens.OR, '=', Tokens.OR);
        case '^':
          return scanOperatorTwo(Tokens.XOR, '=', Tokens.XOR);
        case '%':
          return scanOperatorTwo(Tokens.MOD, '=', Tokens.MOD_ASSIGN);
        case '\'':
          scanCharLiteral();
          return Tokens.CHARACTER_LITERAL;
        case '"':
          return scanStringStart();
        case '_':
          return Tokens.UNDERSCORE;
        default:
          return Tokens.UNKNOWN;
      }
    }
  }

  protected void scanNewline() {
    if (offset + length < bufferLen && charAt(offset + length) == '\n') {
      length++;
    }
  }

  protected Tokens scanIdentifier(char ch) {
    TrieTree.Node<Tokens> n = keywords.root.map.get(ch);
    while (offset + length < bufferLen && isIdentifierPart(ch = charAt(offset + length))) {
      length++;
      n = n == null ? null : n.map.get(ch);
    }
    return n == null ? Tokens.IDENTIFIER : (n.token == null ? Tokens.IDENTIFIER : n.token);
  }

  protected void scanTrans() {
    if (offset + length == bufferLen) {
      return;
    }
    char ch = charAt();
    if (ch == '\\'
        || ch == 't'
        || ch == 'f'
        || ch == 'n'
        || ch == 'r'
        || ch == '0'
        || ch == '\"'
        || ch == '\''
        || ch == 'b'
        || ch == '$') {
      length++;
    } else if (ch == 'u') {
      length++;
      for (int i = 0; i < 4; i++) {
        if (offset + length >= bufferLen || !isDigit(charAt(offset + length))) {
          return;
        }
        length++;
      }
    }
  }

  private Tokens scanStringStart() {
    boolean triple =
        offset + 2 < bufferLen && charAt(offset + 1) == '"' && charAt(offset + 2) == '"';
    length = triple ? 3 : 1;
    mode = 1;
    tripleQuote = triple;
    return continueStringText();
  }

  private Tokens continueStringText() {
    while (offset + length < bufferLen) {
      char c = charAt(offset + length);
      if (!tripleQuote && c == '\n') {
        mode = 0;
        return Tokens.STRING_LITERAL;
      }
      if (!tripleQuote && c == '\\') {
        length++;
        if (offset + length < bufferLen) {
          scanTrans();
        }
        continue;
      }
      if (c == '"') {
        if (tripleQuote) {
          if (offset + length + 2 < bufferLen
              && charAt(offset + length + 1) == '"'
              && charAt(offset + length + 2) == '"') {
            length += 3;
            mode = 0;
            return Tokens.STRING_LITERAL;
          }
          length++;
          continue;
        }
        length++;
        mode = 0;
        return Tokens.STRING_LITERAL;
      }
      if (c == '$') {
        char next = offset + length + 1 < bufferLen ? charAt(offset + length + 1) : '\0';
        if (next == '{') {
          if (length == 0) {
            length = 2;
            mode = 2;
            exprDepth = 0;
            return Tokens.STRING_TEMPLATE_EXPR_START;
          }
          return Tokens.STRING_LITERAL;
        }
        if (isIdentifierStartPlain(next)) {
          if (length == 0) {
            length = 1;
            mode = 3;
            return Tokens.STRING_TEMPLATE_DOLLAR;
          }
          return Tokens.STRING_LITERAL;
        }
        length++;
        continue;
      }
      length++;
    }
    return tripleQuote ? Tokens.STRING_INCOMPLETE : Tokens.STRING_LITERAL;
  }

  private boolean isIdentifierStartPlain(char c) {
    return MyCharacter.isJavaIdentifierStart(c) || c == '_';
  }

  private Tokens scanDollarIdentifier() {
    length = 1;
    while (offset + length < bufferLen && isIdentifierPart(charAt(offset + length))) {
      length++;
    }
    mode = 1;
    return Tokens.IDENTIFIER;
  }

  protected void scanCharLiteral() {
    if (offset + length == bufferLen) {
      return;
    }
    char ch;
    while (offset + length < bufferLen && (ch = charAt(offset + length)) != '\'') {
      if (ch == '\\') {
        length++;
        scanTrans();
      } else {
        if (ch == '\n') {
          return;
        }
        length++;
      }
    }
    if (offset + length != bufferLen) {
      length++;
    }
  }

  protected Tokens scanNumber() {
    if (offset + length == bufferLen) {
      return Tokens.INTEGER_LITERAL;
    }
    boolean flag = false;
    char ch = charAt(offset);
    if (ch == '0') {
      if (charAt() == 'x' || charAt() == 'X') {
        length++;
      }
      flag = true;
    }
    while (offset + length < bufferLen && isDigit(charAt())) {
      length++;
    }
    if (offset + length == bufferLen) {
      return Tokens.INTEGER_LITERAL;
    }
    ch = charAt();
    if (ch == '.') {
      if (flag) {
        return Tokens.INTEGER_LITERAL;
      }
      length++;
      if (offset + length == bufferLen) {
        return Tokens.FLOATING_POINT_LITERAL;
      }
      while (offset + length < bufferLen && isPrimeDigit(charAt())) {
        length++;
      }
      if (offset + length == bufferLen) {
        return Tokens.FLOATING_POINT_LITERAL;
      }
      ch = charAt();
      if (ch == 'e' || ch == 'E') {
        length++;
        if (offset + length == bufferLen) {
          return Tokens.FLOATING_POINT_LITERAL;
        }
        if (charAt() == '-' || charAt() == '+') {
          length++;
        }
        while (offset + length < bufferLen && isPrimeDigit(charAt())) {
          length++;
        }
        if (offset + length == bufferLen) {
          return Tokens.FLOATING_POINT_LITERAL;
        }
        ch = charAt();
      }
      if (ch == 'f' || ch == 'F' || ch == 'D' || ch == 'd') {
        length++;
      }
      return Tokens.FLOATING_POINT_LITERAL;
    } else if (ch == 'l' || ch == 'L') {
      length++;
      return Tokens.INTEGER_LITERAL;
    } else if (ch == 'F' || ch == 'f' || ch == 'D' || ch == 'd') {
      length++;
      return Tokens.FLOATING_POINT_LITERAL;
    } else {
      return Tokens.INTEGER_LITERAL;
    }
  }

  protected Tokens scanDIV() {
    if (offset + 1 == bufferLen) {
      return Tokens.DIV;
    }
    char ch = charAt();
    if (ch == '/') {
      length++;
      while (offset + length < bufferLen && charAt() != '\n') {
        length++;
      }
      return Tokens.LINE_COMMENT;
    } else if (ch == '*') {
      length++;
      char pre = '\0', curr = '\0';
      boolean finished = false;
      while (offset + length < bufferLen) {
        pre = curr;
        curr = charAt();
        if (curr == '/' && pre == '*') {
          length++;
          finished = true;
          break;
        }
        length++;
      }
      return finished ? Tokens.LONG_COMMENT_COMPLETE : Tokens.LONG_COMMENT_INCOMPLETE;
    } else {
      return Tokens.DIV;
    }
  }

  protected Tokens scanLT() {
    if (offset + 1 < bufferLen) {
      char next = charAt(offset + 1);
      if (next == '=') {
        length++;
      }
    }
    return Tokens.LT;
  }

  protected Tokens scanGT() {
    if (offset + 1 < bufferLen) {
      char next = charAt(offset + 1);
      if (next == '=') {
        length++;
      }
    }
    return Tokens.GT;
  }

  protected Tokens scanOperatorTwo(Tokens single, char nextChar, Tokens doubles) {
    if (offset + 1 < bufferLen && charAt(offset + 1) == nextChar) {
      length++;
      return doubles;
    }
    return single;
  }

  protected Tokens scanQuestion() {
    if (offset + 1 < bufferLen) {
      char next = charAt(offset + 1);
      if (next == ':') {
        length++;
        return Tokens.ELVIS;
      } else if (next == '.') {
        length++;
        return Tokens.SAFE_ACCESS;
      }
    }
    return Tokens.QUESTION;
  }

  protected Tokens scanDot() {
    if (offset + 1 < bufferLen && charAt(offset + 1) == '.') {
      length++;
      return Tokens.RANGE;
    }
    return Tokens.DOT;
  }

  protected Tokens scanMinus() {
    if (offset + 1 < bufferLen) {
      char next = charAt(offset + 1);
      if (next == '-') {
        length++;
        return Tokens.DEC;
      } else if (next == '=') {
        length++;
        return Tokens.MINUS_ASSIGN;
      } else if (next == '>') {
        length++;
        return Tokens.ARROW;
      }
    }
    return Tokens.MINUS;
  }

  protected Tokens scanPlus() {
    if (offset + 1 < bufferLen && charAt(offset + 1) == '+') {
      length++;
      return Tokens.INC;
    } else if (offset + 1 < bufferLen && charAt(offset + 1) == '=') {
      length++;
      return Tokens.PLUS_ASSIGN;
    }
    return Tokens.PLUS;
  }

  protected static boolean isDigit(char c) {
    return ((c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f'));
  }

  protected static boolean isPrimeDigit(char c) {
    return (c >= '0' && c <= '9');
  }

  protected static boolean isWhitespace(char c) {
    return (c == '\t' || c == ' ' || c == '\f' || c == '\n' || c == '\r');
  }

  protected static void doStaticInit() {
    String[] sKeywords = {
      "as",
      "break",
      "class",
      "continue",
      "do",
      "else",
      "enum",
      "false",
      "final",
      "finally",
      "for",
      "fun",
      "if",
      "import",
      "in",
      "interface",
      "is",
      "null",
      "object",
      "package",
      "private",
      "protected",
      "public",
      "return",
      "sealed",
      "super",
      "this",
      "throw",
      "throws",
      "true",
      "try",
      "typealias",
      "val",
      "var",
      "when",
      "while",
      "data",
      "inline",
      "open"
    };
    Tokens[] sTokens = {
      Tokens.AS,
      Tokens.BREAK,
      Tokens.CLASS,
      Tokens.CONTINUE,
      Tokens.DO,
      Tokens.ELSE,
      Tokens.ENUM,
      Tokens.FALSE,
      Tokens.FINAL,
      Tokens.FINALLY,
      Tokens.FOR,
      Tokens.FUN,
      Tokens.IF,
      Tokens.IMPORT,
      Tokens.IN,
      Tokens.INTERFACE,
      Tokens.IS,
      Tokens.NULL_LITERAL,
      Tokens.OBJECT,
      Tokens.PACKAGE,
      Tokens.PRIVATE,
      Tokens.PROTECTED,
      Tokens.PUBLIC,
      Tokens.RETURN,
      Tokens.SEALED,
      Tokens.SUPER,
      Tokens.THIS,
      Tokens.THROW,
      Tokens.THROWS,
      Tokens.TRUE,
      Tokens.TRY,
      Tokens.TYPEALIAS,
      Tokens.VAL,
      Tokens.VAR,
      Tokens.WHEN,
      Tokens.WHILE,
      Tokens.DATA,
      Tokens.INLINE,
      Tokens.OPEN
    };
    keywords = new TrieTree<>();
    for (int i = 0; i < sKeywords.length; i++) {
      keywords.put(sKeywords[i], sTokens[i]);
    }
  }
}
