/**
 * Comment by ghost ide
 *
 * @author: Ninjacoder
 */
package ir.hanzodev1375.ghostide.codeeditors.langs.vue;

import io.github.rosemoe.sora.util.TrieTree;

public class VueTextTokenizer {
  public static final int MODE_MARKUP = 0;
  public static final int MODE_SCRIPT = 1;
  public static final int MODE_STYLE = 2;

  private static TrieTree<VueTokens> keywords;

  static {
    doStaticInit();
  }

  public static TrieTree<VueTokens> getTree() {
    return keywords;
  }

  private CharSequence source;
  private int bufferLen;
  private int index;
  public int offset;
  public int length;
  private VueTokens currToken;
  private int mode = MODE_MARKUP;
  public boolean inTag = false;
  public boolean tagNamePending = false;
  public boolean inMustache = false;

  public VueTextTokenizer(CharSequence src) {
    if (src == null) throw new IllegalArgumentException("src cannot be null");
    this.source = src;
    init();
  }

  private void init() {
    length = 0;
    index = 0;
    offset = 0;
    currToken = VueTokens.WHITESPACE;
    this.bufferLen = source.length();
  }

  public void reset(CharSequence src) {
    if (src == null) throw new IllegalArgumentException("src cannot be null");
    this.source = src;
    this.bufferLen = src.length();
    init();
  }

  public void setMode(int mode) {
    this.mode = mode;
  }

  public CharSequence getTokenText() {
    return source.subSequence(offset, offset + length);
  }

  public int getTokenLength() {
    return length;
  }

  public VueTokens nextToken() {
    return currToken = nextTokenInternal();
  }

  private VueTokens nextTokenInternal() {
    index += length;
    offset += length;
    if (offset >= bufferLen) return VueTokens.EOF;
    if (mode == MODE_SCRIPT) return nextScriptToken();
    if (mode == MODE_STYLE) return nextStyleToken();
    return nextMarkupToken();
  }

  private char charAt(int rel) {
    int p = offset + rel;
    return p < bufferLen ? source.charAt(p) : '\0';
  }

  private boolean startsWith(String s) {
    if (offset + s.length() > bufferLen) return false;
    for (int i = 0; i < s.length(); i++) {
      if (source.charAt(offset + i) != s.charAt(i)) return false;
    }
    return true;
  }

  private VueTokens nextMarkupToken() {
    char ch = source.charAt(offset);
    length = 1;
    if (ch == '\n') return VueTokens.NEWLINE;
    if (ch == '\r') {
      if (charAt(1) == '\n') length++;
      return VueTokens.NEWLINE;
    }
    if (isWhitespace(ch)) {
      while (offset + length < bufferLen && isWhitespace(source.charAt(offset + length))) length++;
      return VueTokens.WHITESPACE;
    }
    if (inMustache) {
      if (ch == '}' && charAt(1) == '}') {
        length = 2;
        inMustache = false;
        return VueTokens.MUSTACHE_END;
      }
      return scanScriptLikeToken();
    }
    if (inTag) {
      if (ch == '/' && charAt(1) == '>') {
        length = 2;
        inTag = false;
        tagNamePending = false;
        return VueTokens.TAG_SELF_CLOSE;
      }
      if (ch == '>') {
        inTag = false;
        tagNamePending = false;
        return VueTokens.TAG_END;
      }
      if (ch == '"' || ch == '\'') {
        scanQuoted(ch);
        return VueTokens.ATTR_VALUE;
      }
      if (ch == '=') return VueTokens.ATTR_EQUALS;
      if (ch == ':' || ch == '@' || ch == '#') {
        while (offset + length < bufferLen
            && isDirectiveShorthandPart(source.charAt(offset + length))) length++;
        return VueTokens.DIRECTIVE_NAME;
      }
      if (isIdentifierStart(ch)) {
        while (offset + length < bufferLen && isAttrNamePart(source.charAt(offset + length)))
          length++;
        if (tagNamePending) {
          tagNamePending = false;
          return VueTokens.TAG_NAME;
        }
        CharSequence text = getTokenText();
        if (text.length() >= 2
            && (text.charAt(0) == 'v' || text.charAt(0) == 'V')
            && text.charAt(1) == '-') {
          return VueTokens.DIRECTIVE_NAME;
        }
        return VueTokens.ATTR_NAME;
      }
      return VueTokens.UNKNOWN;
    }
    if (ch == '<') {
      if (startsWith("<!--")) {
        length = 4;
        return scanHtmlComment();
      }
      if (charAt(1) == '/') {
        length = 2;
        inTag = true;
        tagNamePending = true;
        return VueTokens.TAG_CLOSE_START;
      }
      length = 1;
      inTag = true;
      tagNamePending = true;
      return VueTokens.TAG_OPEN_START;
    }
    if (ch == '{' && charAt(1) == '{') {
      length = 2;
      inMustache = true;
      return VueTokens.MUSTACHE_START;
    }
    while (offset + length < bufferLen
        && source.charAt(offset + length) != '<'
        && !(source.charAt(offset + length) == '{' && charAt(length + 1) == '{')) {
      length++;
    }
    return VueTokens.TEXT_CONTENT;
  }

  private VueTokens scanHtmlComment() {
    char a = 0, b = 0, c;
    boolean finished = false;
    while (offset + length < bufferLen) {
      c = source.charAt(offset + length);
      length++;
      if (a == '-' && b == '-' && c == '>') {
        finished = true;
        break;
      }
      a = b;
      b = c;
    }
    return finished ? VueTokens.HTML_COMMENT_COMPLETE : VueTokens.HTML_COMMENT_INCOMPLETE;
  }

  private VueTokens nextScriptToken() {
    char ch = source.charAt(offset);
    length = 1;
    if (ch == '\n') return VueTokens.NEWLINE;
    if (ch == '\r') {
      if (charAt(1) == '\n') length++;
      return VueTokens.NEWLINE;
    }
    if (isWhitespace(ch)) {
      while (offset + length < bufferLen && isWhitespace(source.charAt(offset + length))) length++;
      return VueTokens.WHITESPACE;
    }
    if (ch == '/' && charAt(1) == '/') {
      while (offset + length < bufferLen && source.charAt(offset + length) != '\n') length++;
      return VueTokens.LINE_COMMENT;
    }
    if (ch == '/' && charAt(1) == '*') return scanBlockComment();
    return scanScriptLikeToken();
  }

  private VueTokens scanBlockComment() {
    length = 2;
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
    return finished ? VueTokens.BLOCK_COMMENT_COMPLETE : VueTokens.BLOCK_COMMENT_INCOMPLETE;
  }

  private VueTokens scanScriptLikeToken() {
    char ch = source.charAt(offset);
    if (ch == '"' || ch == '\'') {
      scanJsString(ch);
      return VueTokens.STRING_LITERAL;
    }
    if (ch == '`') {
      scanJsString('`');
      return VueTokens.TEMPLATE_LITERAL;
    }
    if (isDigit(ch)
        || (ch == '.' && offset + 1 < bufferLen && isDigit(source.charAt(offset + 1)))) {
      return scanNumber();
    }
    if (isIdentifierStart(ch)) return scanIdentifierOrKeyword();
    switch (ch) {
      case '{':
        return VueTokens.LBRACE;
      case '}':
        return VueTokens.RBRACE;
      case '(':
        return VueTokens.LPAREN;
      case ')':
        return VueTokens.RPAREN;
      case '[':
        return VueTokens.LBRACK;
      case ']':
        return VueTokens.RBRACK;
      case ';':
        return VueTokens.SEMICOLON;
      case ',':
        return VueTokens.COMMA;
      case ':':
        return VueTokens.COLON;
      case '.':
        if (charAt(1) == '.' && charAt(2) == '.') {
          length = 3;
          return VueTokens.ELLIPSIS;
        }
        return VueTokens.DOT;
      case '=':
        if (charAt(1) == '=' && charAt(2) == '=') {
          length = 3;
          return VueTokens.STRICT_EQ;
        }
        if (charAt(1) == '=') {
          length = 2;
          return VueTokens.EQ;
        }
        if (charAt(1) == '>') {
          length = 2;
          return VueTokens.ARROW;
        }
        return VueTokens.ASSIGN;
      case '!':
        if (charAt(1) == '=' && charAt(2) == '=') {
          length = 3;
          return VueTokens.STRICT_NEQ;
        }
        if (charAt(1) == '=') {
          length = 2;
          return VueTokens.NEQ;
        }
        return VueTokens.NOT;
      case '+':
        if (charAt(1) == '+') {
          length = 2;
          return VueTokens.INC;
        }
        if (charAt(1) == '=') {
          length = 2;
          return VueTokens.PLUS_ASSIGN;
        }
        return VueTokens.PLUS;
      case '-':
        if (charAt(1) == '-') {
          length = 2;
          return VueTokens.DEC;
        }
        if (charAt(1) == '=') {
          length = 2;
          return VueTokens.MINUS_ASSIGN;
        }
        return VueTokens.MINUS;
      case '*':
        if (charAt(1) == '=') {
          length = 2;
          return VueTokens.STAR_ASSIGN;
        }
        return VueTokens.STAR;
      case '/':
        if (charAt(1) == '=') {
          length = 2;
          return VueTokens.SLASH_ASSIGN;
        }
        return VueTokens.SLASH;
      case '%':
        if (charAt(1) == '=') {
          length = 2;
          return VueTokens.PERCENT_ASSIGN;
        }
        return VueTokens.PERCENT;
      case '<':
        if (charAt(1) == '=') {
          length = 2;
          return VueTokens.LTE;
        }
        return VueTokens.LT;
      case '>':
        if (charAt(1) == '=') {
          length = 2;
          return VueTokens.GTE;
        }
        return VueTokens.GT;
      case '&':
        if (charAt(1) == '&') {
          length = 2;
          return VueTokens.AND;
        }
        return VueTokens.UNKNOWN;
      case '|':
        if (charAt(1) == '|') {
          length = 2;
          return VueTokens.OR;
        }
        return VueTokens.UNKNOWN;
      case '?':
        if (charAt(1) == '?') {
          length = 2;
          return VueTokens.NULLISH;
        }
        if (charAt(1) == '.') {
          length = 2;
          return VueTokens.OPTIONAL_CHAIN;
        }
        return VueTokens.QUESTION;
      default:
        return VueTokens.UNKNOWN;
    }
  }

  private VueTokens scanIdentifierOrKeyword() {
    char first = source.charAt(offset);
    TrieTree.Node<VueTokens> node = keywords.root.map.get(first);
    while (offset + length < bufferLen && isIdentifierPart(source.charAt(offset + length))) {
      char c = source.charAt(offset + length);
      node = (node == null) ? null : node.map.get(c);
      length++;
    }
    return (node != null && node.token != null) ? node.token : VueTokens.IDENTIFIER;
  }

  private void scanJsString(char quote) {
    while (offset + length < bufferLen) {
      char c = source.charAt(offset + length);
      if (c == quote) {
        length++;
        break;
      }
      if (c == '\\') {
        length++;
        if (offset + length < bufferLen) length++;
      } else {
        length++;
      }
    }
  }

  private void scanQuoted(char quote) {
    while (offset + length < bufferLen) {
      char c = source.charAt(offset + length);
      length++;
      if (c == quote) break;
    }
  }

  private VueTokens scanNumber() {
    if (offset + 1 < bufferLen && source.charAt(offset) == '0') {
      char next = source.charAt(offset + 1);
      if (next == 'b' || next == 'B') {
        length++;
        while (offset + length < bufferLen
            && (source.charAt(offset + length) == '0' || source.charAt(offset + length) == '1'))
          length++;
        return VueTokens.NUMBER_LITERAL;
      }
      if (next == 'o' || next == 'O') {
        length++;
        while (offset + length < bufferLen
            && source.charAt(offset + length) >= '0'
            && source.charAt(offset + length) <= '7') length++;
        return VueTokens.NUMBER_LITERAL;
      }
      if (next == 'x' || next == 'X') {
        length++;
        while (offset + length < bufferLen && isHexDigit(source.charAt(offset + length))) length++;
        return VueTokens.NUMBER_LITERAL;
      }
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
          && (source.charAt(offset + length) == '+' || source.charAt(offset + length) == '-'))
        length++;
      while (offset + length < bufferLen && isDigit(source.charAt(offset + length))) length++;
    }
    if (offset + length < bufferLen && source.charAt(offset + length) == 'n') length++;
    return VueTokens.NUMBER_LITERAL;
  }

  private VueTokens nextStyleToken() {
    char ch = source.charAt(offset);
    length = 1;
    if (ch == '\n') return VueTokens.NEWLINE;
    if (ch == '\r') {
      if (charAt(1) == '\n') length++;
      return VueTokens.NEWLINE;
    }
    if (isWhitespace(ch)) {
      while (offset + length < bufferLen && isWhitespace(source.charAt(offset + length))) length++;
      return VueTokens.WHITESPACE;
    }
    if (ch == '/' && charAt(1) == '*') return scanBlockComment();
    if (ch == '"' || ch == '\'') {
      scanJsString(ch);
      return VueTokens.STRING_LITERAL;
    }
    if (ch == '@') {
      while (offset + length < bufferLen && isIdentifierPart(source.charAt(offset + length)))
        length++;
      return VueTokens.CSS_AT_RULE;
    }
    if (isDigit(ch)
        || (ch == '.' && offset + 1 < bufferLen && isDigit(source.charAt(offset + 1)))) {
      return scanCssNumber();
    }
    if (isCssWordStart(ch)) {
      while (offset + length < bufferLen && isCssWordPart(source.charAt(offset + length))) length++;
      return VueTokens.IDENTIFIER;
    }
    switch (ch) {
      case '{':
        return VueTokens.LBRACE;
      case '}':
        return VueTokens.RBRACE;
      case '(':
        return VueTokens.LPAREN;
      case ')':
        return VueTokens.RPAREN;
      case '[':
        return VueTokens.LBRACK;
      case ']':
        return VueTokens.RBRACK;
      case ';':
        return VueTokens.SEMICOLON;
      case ':':
        return VueTokens.COLON;
      case ',':
        return VueTokens.COMMA;
      case '.':
        return VueTokens.DOT;
      case '%':
        return VueTokens.PERCENT;
      default:
        return VueTokens.UNKNOWN;
    }
  }

  private VueTokens scanCssNumber() {
    while (offset + length < bufferLen && isDigit(source.charAt(offset + length))) length++;
    if (offset + length < bufferLen && source.charAt(offset + length) == '.') {
      length++;
      while (offset + length < bufferLen && isDigit(source.charAt(offset + length))) length++;
    }
    while (offset + length < bufferLen && Character.isLetter(source.charAt(offset + length)))
      length++;
    if (offset + length < bufferLen && source.charAt(offset + length) == '%') length++;
    return VueTokens.NUMBER_LITERAL;
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
    return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_' || c == '$';
  }

  private static boolean isIdentifierPart(char c) {
    return isIdentifierStart(c) || isDigit(c);
  }

  private static boolean isAttrNamePart(char c) {
    return (c >= 'a' && c <= 'z')
        || (c >= 'A' && c <= 'Z')
        || isDigit(c)
        || c == '-'
        || c == '_'
        || c == ':';
  }

  private static boolean isDirectiveShorthandPart(char c) {
    return isAttrNamePart(c) || c == '.' || c == '[' || c == ']';
  }

  private static boolean isCssWordStart(char c) {
    return (c >= 'a' && c <= 'z')
        || (c >= 'A' && c <= 'Z')
        || c == '_'
        || c == '-'
        || c == '.'
        || c == '#'
        || c == '&'
        || c == '*'
        || c == '$';
  }

  private static boolean isCssWordPart(char c) {
    return isCssWordStart(c) || isDigit(c);
  }

  private static void doStaticInit() {
    String[] words = {
      "function",
      "const",
      "let",
      "var",
      "class",
      "new",
      "return",
      "import",
      "export",
      "from",
      "default",
      "break",
      "case",
      "catch",
      "continue",
      "debugger",
      "delete",
      "do",
      "else",
      "extends",
      "finally",
      "for",
      "if",
      "in",
      "instanceof",
      "of",
      "super",
      "switch",
      "this",
      "throw",
      "try",
      "typeof",
      "void",
      "while",
      "with",
      "yield",
      "async",
      "await",
      "static",
      "get",
      "set",
      "as",
      "null",
      "true",
      "false",
      "undefined",
      "interface",
      "type",
      "enum",
      "implements",
      "private",
      "public",
      "protected",
      "readonly",
      "namespace",
      "declare",
      "abstract",
      "is",
      "keyof",
      "infer",
      "satisfies"
    };
    VueTokens[] tokens = {
      VueTokens.KW_FUNCTION,
      VueTokens.KW_CONST,
      VueTokens.KW_LET,
      VueTokens.KW_VAR,
      VueTokens.KW_CLASS,
      VueTokens.KW_NEW,
      VueTokens.KW_RETURN,
      VueTokens.KW_IMPORT,
      VueTokens.KW_EXPORT,
      VueTokens.KW_FROM,
      VueTokens.KW_DEFAULT,
      VueTokens.KEYWORD,
      VueTokens.KEYWORD,
      VueTokens.KEYWORD,
      VueTokens.KEYWORD,
      VueTokens.KEYWORD,
      VueTokens.KEYWORD,
      VueTokens.KEYWORD,
      VueTokens.KEYWORD,
      VueTokens.KEYWORD,
      VueTokens.KEYWORD,
      VueTokens.KEYWORD,
      VueTokens.KEYWORD,
      VueTokens.KEYWORD,
      VueTokens.KEYWORD,
      VueTokens.KEYWORD,
      VueTokens.KEYWORD,
      VueTokens.KEYWORD,
      VueTokens.KEYWORD,
      VueTokens.KEYWORD,
      VueTokens.KEYWORD,
      VueTokens.KEYWORD,
      VueTokens.KEYWORD,
      VueTokens.KEYWORD,
      VueTokens.KEYWORD,
      VueTokens.KEYWORD,
      VueTokens.KEYWORD,
      VueTokens.KEYWORD,
      VueTokens.KEYWORD,
      VueTokens.KEYWORD,
      VueTokens.KEYWORD,
      VueTokens.KEYWORD,
      VueTokens.KEYWORD,
      VueTokens.KEYWORD,
      VueTokens.KEYWORD,
      VueTokens.KEYWORD,
      VueTokens.KEYWORD,
      VueTokens.KEYWORD,
      VueTokens.KEYWORD,
      VueTokens.KEYWORD,
      VueTokens.KEYWORD,
      VueTokens.KEYWORD,
      VueTokens.KEYWORD,
      VueTokens.KEYWORD,
      VueTokens.KEYWORD,
      VueTokens.KEYWORD,
      VueTokens.KEYWORD,
      VueTokens.KEYWORD,
      VueTokens.KEYWORD,
      VueTokens.KEYWORD,
      VueTokens.KEYWORD
    };
    keywords = new TrieTree<>();
    for (int i = 0; i < words.length; i++) {
      keywords.put(words[i], tokens[i]);
    }
  }
}
