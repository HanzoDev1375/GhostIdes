package ir.hanzodev1375.ghostide.codeeditors.langs.php;

import io.github.rosemoe.sora.util.MyCharacter;
import io.github.rosemoe.sora.util.TrieTree;

public class PhpTextTokenizer {

  private static TrieTree<PhpTokens> keywords;

  static {
    doStaticInit();
  }

  public static TrieTree<PhpTokens> getTree() {
    return keywords;
  }

  private CharSequence source;
  private int bufferLen;
  private int index;
  public int offset;
  public int length;
  private PhpTokens currToken;
  private int mode = 0;
  private int exprDepth;

  public PhpTextTokenizer(CharSequence src) {
    if (src == null) throw new IllegalArgumentException("src cannot be null");
    this.source = src;
    init();
  }

  private void init() {
    length = 0;
    index = 0;
    offset = 0;
    currToken = PhpTokens.WHITESPACE;
    mode = 0;
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

  public PhpTokens nextToken() {
    return currToken = nextTokenInternal();
  }

  private PhpTokens nextTokenInternal() {
    index += length;
    offset += length;
    if (offset >= bufferLen) return PhpTokens.EOF;

    if (mode == 1) {
      length = 0;
      return continueStringText();
    }
    if (mode == 4) {
      return scanInterpIdentifier();
    }

    char ch = source.charAt(offset);
    length = 1;

    if (mode == 2 && ch == '}') {
      if (exprDepth == 0) {
        mode = 1;
        return PhpTokens.STRING_TEMPLATE_EXPR_END;
      }
      exprDepth--;
      return PhpTokens.RBRACE;
    }
    if (mode == 2 && ch == '{') {
      exprDepth++;
      return PhpTokens.LBRACE;
    }

    if (ch == '\n') return PhpTokens.NEWLINE;
    if (ch == '\r') {
      if (offset + 1 < bufferLen && source.charAt(offset + 1) == '\n') length++;
      return PhpTokens.NEWLINE;
    }

    if (isWhitespace(ch)) {
      while (offset + length < bufferLen && isWhitespace(source.charAt(offset + length))) {
        length++;
      }
      return PhpTokens.WHITESPACE;
    }

    // PHP Open Tag: <?php, <?, <?=
    if (ch == '<' && offset + 1 < bufferLen && source.charAt(offset + 1) == '?') {
      length++;
      if (offset + 2 < bufferLen && source.charAt(offset + 2) == '=') {
        length++;
        return PhpTokens.SHORT_ECHO;
      }
      if (offset + 2 < bufferLen) {
        char c1 = source.charAt(offset + 2);
        if ((c1 == 'p' || c1 == 'P') && offset + 3 < bufferLen) {
          char c2 = source.charAt(offset + 3);
          if ((c2 == 'h' || c2 == 'H') && offset + 4 < bufferLen) {
            char c3 = source.charAt(offset + 4);
            if ((c3 == 'p' || c3 == 'P')) {
              length += 3;
              return PhpTokens.PHP_OPEN_TAG;
            }
          }
        }
      }
      return PhpTokens.PHP_OPEN_TAG;
    }

    // PHP Close Tag: ?>
    if (ch == '?' && offset + 1 < bufferLen && source.charAt(offset + 1) == '>') {
      length++;
      return PhpTokens.PHP_CLOSE_TAG;
    }

    // Line comment: //
    if (ch == '/' && offset + 1 < bufferLen && source.charAt(offset + 1) == '/') {
      while (offset + length < bufferLen && source.charAt(offset + length) != '\n') {
        length++;
      }
      return PhpTokens.LINE_COMMENT;
    }

    // Line comment: #
    if (ch == '#') {
      while (offset + length < bufferLen && source.charAt(offset + length) != '\n') {
        length++;
      }
      return PhpTokens.LINE_COMMENT_HASH;
    }

    // Block comment: /* */
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
      return finished ? PhpTokens.BLOCK_COMMENT_COMPLETE : PhpTokens.BLOCK_COMMENT_INCOMPLETE;
    }

    // String literal: "..." (interpolation-aware) or '...' (literal, no interpolation)
    if (ch == '"') {
      mode = 1;
      return continueStringText();
    }
    if (ch == '\'') {
      scanStringLiteral(ch);
      return PhpTokens.STRING_LITERAL;
    }

    // Heredoc / Nowdoc (simplified)
    if (ch == '<' && offset + 1 < bufferLen && source.charAt(offset + 1) == '<') {
      length++;
      return scanHereDoc();
    }

    // Number
    if (isDigit(ch)
        || (ch == '.' && offset + 1 < bufferLen && isDigit(source.charAt(offset + 1)))) {
      return scanNumber();
    }

    // Variable: $var - برگرداندن $ به‌عنوان توکن جداگانه
    if (ch == '$') {
      return PhpTokens.DOLLAR;
    }

    // Identifier or keyword (باید بعد از $ بیاد)
    if (isIdentifierStart(ch)) {
      return scanIdentifier(ch);
    }

    switch (ch) {
      case '=':
        return scanOperatorTwo(PhpTokens.ASSIGN, '=', PhpTokens.EQ);
      case '+':
        return scanPlus();
      case '-':
        return scanMinus();
      case '*':
        return scanOperatorTwo(PhpTokens.STAR, '=', PhpTokens.STAR_ASSIGN);
      case '/':
        return scanOperatorTwo(PhpTokens.SLASH, '=', PhpTokens.SLASH_ASSIGN);
      case '%':
        return scanOperatorTwo(PhpTokens.PERCENT, '=', PhpTokens.PERCENT_ASSIGN);
      case '^':
        return scanOperatorTwo(PhpTokens.XOR, '=', PhpTokens.XOR_ASSIGN);
      case '&':
        return scanAnd();
      case '|':
        return scanOr();
      case '<':
        return scanLT();
      case '>':
        return scanGT();
      case '!':
        return scanOperatorTwo(PhpTokens.NOT, '=', PhpTokens.NOT);
      case '.':
        return scanDot();
      case ':':
        return PhpTokens.COLON;
      case ',':
        return PhpTokens.COMMA;
      case ';':
        return PhpTokens.SEMICOLON;
      case '{':
        return PhpTokens.LBRACE;
      case '}':
        return PhpTokens.RBRACE;
      case '(':
        return PhpTokens.LPAREN;
      case ')':
        return PhpTokens.RPAREN;
      case '[':
        return PhpTokens.LBRACK;
      case ']':
        return PhpTokens.RBRACK;
      case '\\':
        return PhpTokens.BACKSLASH;
      case '~':
        return PhpTokens.UNKNOWN;
      case '?':
        return PhpTokens.UNKNOWN;
      default:
        return PhpTokens.UNKNOWN;
    }
  }

  private PhpTokens scanHereDoc() {
    int start = offset + length;
    while (start < bufferLen && isWhitespace(source.charAt(start))) {
      start++;
    }
    if (start >= bufferLen) return PhpTokens.UNKNOWN;

    boolean isNowdoc = false;
    if (start < bufferLen && source.charAt(start) == '\'') {
      isNowdoc = true;
      start++;
    }

    int labelStart = start;
    while (start < bufferLen && isIdentifierPart(source.charAt(start))) {
      start++;
    }
    if (labelStart == start) return PhpTokens.UNKNOWN;

    String label = source.subSequence(labelStart, start).toString();
    if (isNowdoc && start < bufferLen && source.charAt(start) == '\'') {
      start++;
    }

    while (start < bufferLen && isWhitespace(source.charAt(start))) {
      start++;
    }
    if (start < bufferLen && source.charAt(start) == ';') {
      start++;
    }

    boolean found = false;
    int pos = start;
    while (pos + label.length() < bufferLen) {
      if (pos > 0 && (source.charAt(pos - 1) == '\n' || source.charAt(pos - 1) == '\r')) {
        if (source.subSequence(pos, pos + label.length()).toString().equals(label)) {
          int endPos = pos + label.length();
          if (endPos < bufferLen && source.charAt(endPos) == ';') {
            endPos++;
          }
          length = endPos - offset;
          found = true;
          break;
        }
      }
      pos++;
    }

    if (!found) {
      length = bufferLen - offset;
    }

    return PhpTokens.STRING_LITERAL;
  }

  private PhpTokens scanIdentifier(char first) {
    TrieTree.Node<PhpTokens> node = keywords.root.map.get(first);
    while (offset + length < bufferLen && isIdentifierPart(source.charAt(offset + length))) {
      char c = source.charAt(offset + length);
      node = (node == null) ? null : node.map.get(c);
      length++;
    }
    return (node != null && node.token != null) ? node.token : PhpTokens.IDENTIFIER;
  }

  private PhpTokens scanNumber() {
    boolean isFloat = false;

    if (offset + 1 < bufferLen && source.charAt(offset) == '0') {
      char next = source.charAt(offset + 1);
      if (next == 'b' || next == 'B') {
        length++;
        while (offset + length < bufferLen
            && (source.charAt(offset + length) == '0' || source.charAt(offset + length) == '1')) {
          length++;
        }
        return PhpTokens.INTEGER_LITERAL;
      }
      if (next == 'o' || next == 'O') {
        length++;
        while (offset + length < bufferLen
            && source.charAt(offset + length) >= '0'
            && source.charAt(offset + length) <= '7') {
          length++;
        }
        return PhpTokens.INTEGER_LITERAL;
      }
      if (next == 'x' || next == 'X') {
        length++;
        while (offset + length < bufferLen && isHexDigit(source.charAt(offset + length))) {
          length++;
        }
        return PhpTokens.INTEGER_LITERAL;
      }
    }

    while (offset + length < bufferLen && isDigit(source.charAt(offset + length))) length++;

    if (offset + length < bufferLen && source.charAt(offset + length) == '.') {
      isFloat = true;
      length++;
      while (offset + length < bufferLen && isDigit(source.charAt(offset + length))) length++;
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

    return isFloat ? PhpTokens.FLOATING_LITERAL : PhpTokens.INTEGER_LITERAL;
  }

  private void scanStringLiteral(char quote) {
    while (offset + length < bufferLen) {
      char c = source.charAt(offset + length);
      if (c == quote) {
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

  private PhpTokens continueStringText() {
    while (offset + length < bufferLen) {
      char c = source.charAt(offset + length);
      if (c == '"') {
        length++;
        mode = 0;
        return PhpTokens.STRING_LITERAL;
      }
      if (c == '\\') {
        length++;
        if (offset + length < bufferLen) scanEscape();
        continue;
      }
      if (c == '{') {
        char next = offset + length + 1 < bufferLen ? source.charAt(offset + length + 1) : '\0';
        if (next == '$') {
          if (length == 0) {
            length = 1;
            mode = 2;
            exprDepth = 0;
            return PhpTokens.STRING_TEMPLATE_EXPR_START;
          }
          return PhpTokens.STRING_LITERAL;
        }
        length++;
        continue;
      }
      if (c == '$') {
        char next = offset + length + 1 < bufferLen ? source.charAt(offset + length + 1) : '\0';
        if (isIdentifierStart(next)) {
          if (length == 0) {
            length = 1;
            mode = 4;
            return PhpTokens.DOLLAR;
          }
          return PhpTokens.STRING_LITERAL;
        }
        length++;
        continue;
      }
      length++;
    }
    mode = 0;
    return PhpTokens.STRING_LITERAL;
  }

  private PhpTokens scanInterpIdentifier() {
    length = 1;
    while (offset + length < bufferLen && isIdentifierPart(source.charAt(offset + length))) {
      length++;
    }
    mode = 1;
    return PhpTokens.IDENTIFIER;
  }

  private void scanEscape() {
    if (offset + length < bufferLen) {
      char c = source.charAt(offset + length);
      if (c == 'n' || c == 'r' || c == 't' || c == 'v' || c == 'e' || c == 'f' || c == '\\'
          || c == '"' || c == '\'' || c == '$') {
        length++;
      } else if (c == 'x') {
        length++;
        while (offset + length < bufferLen && isHexDigit(source.charAt(offset + length))) length++;
      } else if (isDigit(c)) {
        while (offset + length < bufferLen && isDigit(source.charAt(offset + length))) length++;
      }
    }
  }

  private PhpTokens scanPlus() {
    if (offset + 1 < bufferLen) {
      char n = source.charAt(offset + 1);
      if (n == '+') {
        length++;
        return PhpTokens.INC;
      }
      if (n == '=') {
        length++;
        return PhpTokens.PLUS_ASSIGN;
      }
    }
    return PhpTokens.PLUS;
  }

  private PhpTokens scanMinus() {
    if (offset + 1 < bufferLen) {
      char n = source.charAt(offset + 1);
      if (n == '-') {
        length++;
        return PhpTokens.DEC;
      }
      if (n == '=') {
        length++;
        return PhpTokens.MINUS_ASSIGN;
      }
      if (n == '>') {
        length++;
        return PhpTokens.OBJECT_OPERATOR;
      } 
    }
    return PhpTokens.MINUS;
  }

  private PhpTokens scanLT() {
    if (offset + 1 < bufferLen) {
      char n = source.charAt(offset + 1);
      if (n == '=') {
        length++;
        return PhpTokens.LT;
      }
      if (n == '<') {
        length++;
        if (offset + 1 < bufferLen && source.charAt(offset + 1) == '=') {
          length++;
        }
        return PhpTokens.LT;
      }
    }
    return PhpTokens.LT;
  }

  private PhpTokens scanGT() {
    if (offset + 1 < bufferLen) {
      char n = source.charAt(offset + 1);
      if (n == '=') {
        length++;
        return PhpTokens.GT;
      }
      if (n == '>') {
        length++;
        if (offset + 1 < bufferLen && source.charAt(offset + 1) == '=') {
          length++;
        }
        return PhpTokens.GT;
      }
    }
    return PhpTokens.GT;
  }

  private PhpTokens scanAnd() {
    if (offset + 1 < bufferLen) {
      char n = source.charAt(offset + 1);
      if (n == '&') {
        length++;
        return PhpTokens.AND;
      }
      if (n == '=') {
        length++;
        return PhpTokens.AND_ASSIGN;
      }
    }
    return PhpTokens.AND;
  }

  private PhpTokens scanOr() {
    if (offset + 1 < bufferLen) {
      char n = source.charAt(offset + 1);
      if (n == '|') {
        length++;
        return PhpTokens.OR;
      }
      if (n == '=') {
        length++;
        return PhpTokens.OR_ASSIGN;
      }
    }
    return PhpTokens.OR;
  }

  private PhpTokens scanDot() {
    if (offset + 1 < bufferLen && source.charAt(offset + 1) == '.') {
      length++;
      if (offset + 1 < bufferLen && source.charAt(offset + 1) == '.') {
        length++;
        return PhpTokens.ELLIPSIS;
      }
      return PhpTokens.CONCAT;
    }
    if (offset + 1 < bufferLen && source.charAt(offset + 1) == '=') {
      length++;
      return PhpTokens.CONCAT_ASSIGN;
    }
    return PhpTokens.DOT;
  }

  private PhpTokens scanOperatorTwo(PhpTokens single, char nextChar, PhpTokens doubleToken) {
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

  private static boolean isHexDigit(char c) {
    return isDigit(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
  }

  private static boolean isIdentifierStart(char c) {
    return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
  }

  private static boolean isIdentifierPart(char c) {
    return isIdentifierStart(c) || isDigit(c);
  }

  private static void doStaticInit() {
    String[] words = {
      "abstract",
      "and",
      "array",
      "as",
      "break",
      "callable",
      "case",
      "catch",
      "class",
      "clone",
      "const",
      "continue",
      "declare",
      "default",
      "die",
      "do",
      "echo",
      "else",
      "elseif",
      "empty",
      "enddeclare",
      "endfor",
      "endforeach",
      "endif",
      "endswitch",
      "endwhile",
      "eval",
      "exit",
      "extends",
      "final",
      "finally",
      "fn",
      "for",
      "foreach",
      "function",
      "global",
      "goto",
      "if",
      "implements",
      "include",
      "include_once",
      "instanceof",
      "insteadof",
      "interface",
      "isset",
      "list",
      "match",
      "namespace",
      "new",
      "or",
      "print",
      "private",
      "protected",
      "public",
      "readonly",
      "require",
      "require_once",
      "return",
      "static",
      "switch",
      "throw",
      "trait",
      "try",
      "unset",
      "use",
      "var",
      "while",
      "xor",
      "yield",
      "true",
      "false",
      "null",
      "parent",
      "self"
    };
    PhpTokens[] tokens = {
      PhpTokens.ABSTRACT,
      PhpTokens.AND_KW,
      PhpTokens.ARRAY,
      PhpTokens.AS,
      PhpTokens.BREAK,
      PhpTokens.CALLABLE,
      PhpTokens.CASE,
      PhpTokens.CATCH,
      PhpTokens.CLASS,
      PhpTokens.CLONE,
      PhpTokens.CONST,
      PhpTokens.CONTINUE,
      PhpTokens.DECLARE,
      PhpTokens.DEFAULT,
      PhpTokens.DIE,
      PhpTokens.DO,
      PhpTokens.ECHO,
      PhpTokens.ELSE,
      PhpTokens.ELSEIF,
      PhpTokens.EMPTY,
      PhpTokens.ENDDECLARE,
      PhpTokens.ENDFOR,
      PhpTokens.ENDFOREACH,
      PhpTokens.ENDIF,
      PhpTokens.ENDSWITCH,
      PhpTokens.ENDWHILE,
      PhpTokens.EVAL,
      PhpTokens.EXIT,
      PhpTokens.EXTENDS,
      PhpTokens.FINAL,
      PhpTokens.FINALLY,
      PhpTokens.FN,
      PhpTokens.FOR,
      PhpTokens.FOREACH,
      PhpTokens.FUNCTION,
      PhpTokens.GLOBAL,
      PhpTokens.GOTO,
      PhpTokens.IF,
      PhpTokens.IMPLEMENTS,
      PhpTokens.INCLUDE,
      PhpTokens.INCLUDE_ONCE,
      PhpTokens.INSTANCEOF,
      PhpTokens.INSTEADOF,
      PhpTokens.INTERFACE,
      PhpTokens.ISSET,
      PhpTokens.LIST,
      PhpTokens.MATCH,
      PhpTokens.NAMESPACE,
      PhpTokens.NEW,
      PhpTokens.OR_KW,
      PhpTokens.PRINT,
      PhpTokens.PRIVATE,
      PhpTokens.PROTECTED,
      PhpTokens.PUBLIC,
      PhpTokens.READONLY,
      PhpTokens.REQUIRE,
      PhpTokens.REQUIRE_ONCE,
      PhpTokens.RETURN,
      PhpTokens.STATIC,
      PhpTokens.SWITCH,
      PhpTokens.THROW,
      PhpTokens.TRAIT,
      PhpTokens.TRY,
      PhpTokens.UNSET,
      PhpTokens.USE,
      PhpTokens.VAR,
      PhpTokens.WHILE,
      PhpTokens.XOR_KW,
      PhpTokens.YIELD,
      PhpTokens.TRUE,
      PhpTokens.FALSE,
      PhpTokens.NULL,
      PhpTokens.PARENT,
      PhpTokens.SELF
    };
    keywords = new TrieTree<>();
    for (int i = 0; i < words.length; i++) {
      keywords.put(words[i], tokens[i]);
    }
  }
}
