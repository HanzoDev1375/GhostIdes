/**
 * Comment by ghost ide
 *
 * @author: Ninjacoder
 */
package ir.hanzodev1375.ghostide.codeeditors.langs.cpp;

import io.github.rosemoe.sora.util.MyCharacter;
import io.github.rosemoe.sora.util.TrieTree;

public class CppTextTokenizer {

  private static TrieTree<CppTokens> keywords;

  static {
    doStaticInit();
  }

  public static TrieTree<CppTokens> getTree() {
    return keywords;
  }

  private CharSequence source;
  private int bufferLen;
  private int index;
  public int offset;
  public int length;

  private CppTokens currToken;

  public CppTextTokenizer(CharSequence src) {
    if (src == null) throw new IllegalArgumentException("src cannot be null");
    this.source = src;
    init();
  }

  private void init() {
    length = 0;
    index = 0;
    offset = 0;
    currToken = CppTokens.WHITESPACE;
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

  public CppTokens nextToken() {
    return currToken = nextTokenInternal();
  }

  private CppTokens nextTokenInternal() {
    index += length;
    offset += length;
    if (offset >= bufferLen) return CppTokens.EOF;
    char ch = source.charAt(offset);
    length = 1;
    if (ch == '\n') return CppTokens.NEWLINE;
    if (ch == '\r') {
      if (offset + 1 < bufferLen && source.charAt(offset + 1) == '\n') length++;
      return CppTokens.NEWLINE;
    }
    if (isWhitespace(ch)) {
      while (offset + length < bufferLen && isWhitespace(source.charAt(offset + length))) {
        length++;
      }
      return CppTokens.WHITESPACE;
    }

    if (ch == '/' && offset + 1 < bufferLen && source.charAt(offset + 1) == '/') {
      while (offset + length < bufferLen && source.charAt(offset + length) != '\n') length++;
      return CppTokens.LINE_COMMENT;
    }

    if (ch == '/' && offset + 1 < bufferLen && source.charAt(offset + 1) == '*') {
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
      return finished ? CppTokens.BLOCK_COMMENT_COMPLETE : CppTokens.BLOCK_COMMENT_INCOMPLETE;
    }

    if (ch == '#') {
      while (offset + length < bufferLen && source.charAt(offset + length) != '\n') length++;
      return CppTokens.PREPROCESSOR;
    }

    if (ch == '\'') {
      scanCharLiteral();
      return CppTokens.CHAR_LITERAL;
    }

    if (ch == '"') {
      scanStringLiteral();
      return CppTokens.STRING_LITERAL;
    }

    if (isDigit(ch)
        || (ch == '.' && offset + 1 < bufferLen && isDigit(source.charAt(offset + 1)))) {
      return scanNumber();
    }

    if (isIdentifierStart(ch)) {
      return scanIdentifierOrPrefixedLiteral(ch);
    }

    switch (ch) {
      case '=':
        return scanOperatorTwo(CppTokens.ASSIGN, '=', CppTokens.EQ);
      case '+':
        return scanPlus();
      case '-':
        return scanMinus();
      case '*':
        return scanOperatorTwo(CppTokens.STAR, '=', CppTokens.STAR_ASSIGN);
      case '/':
        return scanOperatorTwo(CppTokens.SLASH, '=', CppTokens.SLASH_ASSIGN);
      case '%':
        return scanOperatorTwo(CppTokens.PERCENT, '=', CppTokens.PERCENT_ASSIGN);
      case '&':
        return scanAnd();
      case '|':
        return scanOr();
      case '^':
        return scanOperatorTwo(CppTokens.XOR, '=', CppTokens.XOR_ASSIGN);
      case '!':
        return scanOperatorTwo(CppTokens.NOT, '=', CppTokens.EQ);
      case '<':
        return scanLT();
      case '>':
        return scanGT();
      case '.':
        return scanDot();
      case '~':
        return CppTokens.COMP;
      case '?':
        return CppTokens.QUESTION;
      case '{':
        return CppTokens.LBRACE;
      case '}':
        return CppTokens.RBRACE;
      case '(':
        return CppTokens.LPAREN;
      case ')':
        return CppTokens.RPAREN;
      case '[':
        return CppTokens.LBRACK;
      case ']':
        return CppTokens.RBRACK;
      case ';':
        return CppTokens.SEMICOLON;
      case ':':
        return scanOperatorTwo(CppTokens.COLON, ':', CppTokens.DOUBLE_COLON);
      case ',':
        return CppTokens.COMMA;
      default:
        return CppTokens.UNKNOWN;
    }
  }

  private CppTokens scanIdentifierOrPrefixedLiteral(char first) {
    int startOffset = offset;
    TrieTree.Node<CppTokens> node = keywords.root.map.get(first);
    while (offset + length < bufferLen && isIdentifierPart(source.charAt(offset + length))) {
      char c = source.charAt(offset + length);
      node = (node == null) ? null : node.map.get(c);
      length++;
    }
    CppTokens result = (node != null && node.token != null) ? node.token : CppTokens.IDENTIFIER;
    if (result == CppTokens.IDENTIFIER && offset + length < bufferLen) {
      CharSequence text = source.subSequence(startOffset, startOffset + length);
      char next = source.charAt(offset + length);
      boolean isStringPrefix =
          text.equals("L") || text.equals("u") || text.equals("U") || text.equals("u8");
      if (isStringPrefix && next == '"') {
        length++;
        scanStringLiteral();
        return CppTokens.STRING_LITERAL;
      }
      if (isStringPrefix && next == '\'') {
        length++;
        scanCharLiteral();
        return CppTokens.CHAR_LITERAL;
      }
    }
    return result;
  }

  private CppTokens scanNumber() {
    boolean isFloat = false;

    if (source.charAt(offset) == '0'
        && offset + 1 < bufferLen
        && (source.charAt(offset + 1) == 'x' || source.charAt(offset + 1) == 'X')) {
      length++;
      while (offset + length < bufferLen && isHexDigitOrSeparator(source.charAt(offset + length)))
        length++;
      consumeIntegerSuffix();
      return CppTokens.INTEGER_LITERAL;
    }
    if (source.charAt(offset) == '0'
        && offset + 1 < bufferLen
        && (source.charAt(offset + 1) == 'b' || source.charAt(offset + 1) == 'B')) {
      length++;
      while (offset + length < bufferLen
          && (source.charAt(offset + length) == '0'
              || source.charAt(offset + length) == '1'
              || source.charAt(offset + length) == '\'')) length++;
      consumeIntegerSuffix();
      return CppTokens.INTEGER_LITERAL;
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
    if (isFloat) {
      if (offset + length < bufferLen
          && (source.charAt(offset + length) == 'f' || source.charAt(offset + length) == 'F')) {
        length++;
      } else if (offset + length < bufferLen
          && (source.charAt(offset + length) == 'l' || source.charAt(offset + length) == 'L')) {
        length++;
      }
      return CppTokens.FLOATING_LITERAL;
    }
    consumeIntegerSuffix();
    return CppTokens.INTEGER_LITERAL;
  }

  private void consumeIntegerSuffix() {
    while (offset + length < bufferLen) {
      char c = source.charAt(offset + length);
      if (c == 'u' || c == 'U' || c == 'l' || c == 'L') {
        length++;
      } else {
        break;
      }
    }
  }

  private void scanStringLiteral() {
    while (offset + length < bufferLen) {
      char c = source.charAt(offset + length);
      if (c == '\n') {
        break;
      }
      if (c == '"') {
        length++;
        break;
      }
      if (c == '\\') {
        length++;
        scanEscape();
      } else {
        length++;
      }
    }
  }

  private void scanCharLiteral() {
    while (offset + length < bufferLen) {
      char c = source.charAt(offset + length);
      if (c == '\n') {
        break;
      }
      if (c == '\'') {
        length++;
        break;
      }
      if (c == '\\') {
        length++;
        scanEscape();
      } else {
        length++;
      }
    }
  }

  private void scanEscape() {
    if (offset + length < bufferLen) {
      char c = source.charAt(offset + length);
      if (c == 'n' || c == 't' || c == 'r' || c == '0' || c == '\\' || c == '\'' || c == '"'
          || c == 'a' || c == 'b' || c == 'f' || c == 'v' || c == '?') {
        length++;
      } else if (c >= '0' && c <= '7') {
        while (offset + length < bufferLen
            && source.charAt(offset + length) >= '0'
            && source.charAt(offset + length) <= '7') length++;
      } else if (c == 'x') {
        length++;
        while (offset + length < bufferLen && isHexDigit(source.charAt(offset + length))) length++;
      } else if (c == 'u' || c == 'U') {
        length++;
        int max = c == 'u' ? 4 : 8;
        int count = 0;
        while (offset + length < bufferLen
            && count < max
            && isHexDigit(source.charAt(offset + length))) {
          length++;
          count++;
        }
      }
    }
  }

  private CppTokens scanPlus() {
    if (offset + 1 < bufferLen) {
      char n = source.charAt(offset + 1);
      if (n == '+') {
        length++;
        return CppTokens.INC;
      }
      if (n == '=') {
        length++;
        return CppTokens.PLUS_ASSIGN;
      }
    }
    return CppTokens.PLUS;
  }

  private CppTokens scanMinus() {
    if (offset + 1 < bufferLen) {
      char n = source.charAt(offset + 1);
      if (n == '-') {
        length++;
        return CppTokens.DEC;
      }
      if (n == '=') {
        length++;
        return CppTokens.MINUS_ASSIGN;
      }
      if (n == '>') {
        length++;
        if (offset + length < bufferLen && source.charAt(offset + length) == '*') {
          length++;
          return CppTokens.ARROW_STAR;
        }
        return CppTokens.ARROW;
      }
    }
    return CppTokens.MINUS;
  }

  private CppTokens scanAnd() {
    if (offset + 1 < bufferLen) {
      char n = source.charAt(offset + 1);
      if (n == '&') {
        length++;
        return CppTokens.AND_AND;
      }
      if (n == '=') {
        length++;
        return CppTokens.AND_ASSIGN;
      }
    }
    return CppTokens.AND;
  }

  private CppTokens scanOr() {
    if (offset + 1 < bufferLen) {
      char n = source.charAt(offset + 1);
      if (n == '|') {
        length++;
        return CppTokens.OR_OR;
      }
      if (n == '=') {
        length++;
        return CppTokens.OR_ASSIGN;
      }
    }
    return CppTokens.OR;
  }

  private CppTokens scanLT() {
    if (offset + 1 < bufferLen) {
      char n = source.charAt(offset + 1);
      if (n == '=') {
        length++;
        return CppTokens.LT_EQ;
      }
      if (n == '<') {
        length++;
        if (offset + length < bufferLen && source.charAt(offset + length) == '=') {
          length++;
          return CppTokens.SHIFT_LEFT_ASSIGN;
        }
        return CppTokens.SHIFT_LEFT;
      }
    }
    return CppTokens.LT;
  }

  private CppTokens scanGT() {
    if (offset + 1 < bufferLen) {
      char n = source.charAt(offset + 1);
      if (n == '=') {
        length++;
        return CppTokens.GT_EQ;
      }
      if (n == '>') {
        length++;
        if (offset + length < bufferLen && source.charAt(offset + length) == '=') {
          length++;
          return CppTokens.SHIFT_RIGHT_ASSIGN;
        }
        return CppTokens.SHIFT_RIGHT;
      }
    }
    return CppTokens.GT;
  }

  private CppTokens scanDot() {
    if (offset + 1 < bufferLen && source.charAt(offset + 1) == '.') {
      length++;
      if (offset + 1 < bufferLen && source.charAt(offset + 1) == '.') {
        length++;
      }
      return CppTokens.ELLIPSIS;
    }
    if (offset + 1 < bufferLen && source.charAt(offset + 1) == '*') {
      length++;
      return CppTokens.DOT_STAR;
    }
    return CppTokens.DOT;
  }

  private CppTokens scanOperatorTwo(CppTokens single, char nextChar, CppTokens doubleToken) {
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
    return isDigit(c) || c == '\'';
  }

  private static boolean isHexDigit(char c) {
    return isDigit(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
  }

  private static boolean isHexDigitOrSeparator(char c) {
    return isHexDigit(c) || c == '\'';
  }

  private static boolean isIdentifierStart(char c) {
    return MyCharacter.isJavaIdentifierStart(c) || c == '_';
  }

  private static boolean isIdentifierPart(char c) {
    return MyCharacter.isJavaIdentifierPart(c) || c == '_';
  }

  private static void doStaticInit() {
    String[] words = {
      "alignas",
      "alignof",
      "asm",
      "auto",
      "bool",
      "break",
      "case",
      "catch",
      "char",
      "char16_t",
      "char32_t",
      "class",
      "const",
      "constexpr",
      "const_cast",
      "continue",
      "decltype",
      "default",
      "delete",
      "do",
      "double",
      "dynamic_cast",
      "else",
      "enum",
      "explicit",
      "export",
      "extern",
      "false",
      "final",
      "float",
      "for",
      "friend",
      "goto",
      "if",
      "inline",
      "int",
      "long",
      "mutable",
      "namespace",
      "new",
      "noexcept",
      "nullptr",
      "operator",
      "override",
      "private",
      "protected",
      "public",
      "register",
      "reinterpret_cast",
      "return",
      "short",
      "signed",
      "sizeof",
      "static",
      "static_assert",
      "static_cast",
      "struct",
      "switch",
      "template",
      "this",
      "thread_local",
      "throw",
      "true",
      "try",
      "typedef",
      "typeid",
      "typename",
      "union",
      "unsigned",
      "using",
      "virtual",
      "void",
      "volatile",
      "wchar_t",
      "while"
    };
    CppTokens[] tokens = {
      CppTokens.ALIGNAS,
      CppTokens.ALIGNOF,
      CppTokens.ASM,
      CppTokens.AUTO,
      CppTokens.BOOL,
      CppTokens.BREAK,
      CppTokens.CASE,
      CppTokens.CATCH,
      CppTokens.CHAR,
      CppTokens.CHAR16,
      CppTokens.CHAR32,
      CppTokens.CLASS,
      CppTokens.CONST,
      CppTokens.CONSTEXPR,
      CppTokens.CONST_CAST,
      CppTokens.CONTINUE,
      CppTokens.DECLTYPE,
      CppTokens.DEFAULT,
      CppTokens.DELETE,
      CppTokens.DO,
      CppTokens.DOUBLE,
      CppTokens.DYNAMIC_CAST,
      CppTokens.ELSE,
      CppTokens.ENUM,
      CppTokens.EXPLICIT,
      CppTokens.EXPORT,
      CppTokens.EXTERN,
      CppTokens.FALSE_,
      CppTokens.FINAL,
      CppTokens.FLOAT,
      CppTokens.FOR,
      CppTokens.FRIEND,
      CppTokens.GOTO,
      CppTokens.IF,
      CppTokens.INLINE,
      CppTokens.INT,
      CppTokens.LONG,
      CppTokens.MUTABLE,
      CppTokens.NAMESPACE,
      CppTokens.NEW,
      CppTokens.NOEXCEPT,
      CppTokens.NULLPTR,
      CppTokens.OPERATOR,
      CppTokens.OVERRIDE,
      CppTokens.PRIVATE,
      CppTokens.PROTECTED,
      CppTokens.PUBLIC,
      CppTokens.REGISTER,
      CppTokens.REINTERPRET_CAST,
      CppTokens.RETURN,
      CppTokens.SHORT,
      CppTokens.SIGNED,
      CppTokens.SIZEOF,
      CppTokens.STATIC,
      CppTokens.STATIC_ASSERT,
      CppTokens.STATIC_CAST,
      CppTokens.STRUCT,
      CppTokens.SWITCH,
      CppTokens.TEMPLATE,
      CppTokens.THIS,
      CppTokens.THREAD_LOCAL,
      CppTokens.THROW,
      CppTokens.TRUE_,
      CppTokens.TRY,
      CppTokens.TYPEDEF,
      CppTokens.TYPEID,
      CppTokens.TYPENAME,
      CppTokens.UNION,
      CppTokens.UNSIGNED,
      CppTokens.USING,
      CppTokens.VIRTUAL,
      CppTokens.VOID,
      CppTokens.VOLATILE,
      CppTokens.WCHAR,
      CppTokens.WHILE
    };
    keywords = new TrieTree<>();
    for (int i = 0; i < words.length; i++) {
      keywords.put(words[i], tokens[i]);
    }
  }
}
