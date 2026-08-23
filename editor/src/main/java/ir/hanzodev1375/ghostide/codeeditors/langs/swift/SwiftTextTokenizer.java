/**
 * Comment by ghost ide
 *
 * @author: Ninjacoder
 */
package ir.hanzodev1375.ghostide.codeeditors.langs.swift;

import io.github.rosemoe.sora.util.TrieTree;

/**
 * Line tokenizer for Swift with full support for \\(expr) string interpolation, nested block
 * comments, triple-quoted strings, annotations and numeric literal prefixes.
 */
public class SwiftTextTokenizer {

  private static TrieTree<SwiftTokens> keywords;

  private static TrieTree<SwiftTokens> types;

  private static TrieTree<SwiftTokens> builtins;

  static {
    doStaticInit();
  }

  public static TrieTree<SwiftTokens> getKeywordsTree() {
    return keywords;
  }

  private CharSequence source;
  private int bufferLen;
  public int offset;
  public int length;
  /** Remaining nesting depth after an incomplete block comment on this line. */
  public int blockCommentDepth;
  /** True while the scan is paused inside a single-line string body. */
  private boolean stringResume;

  private SwiftTokens currToken;

  public SwiftTextTokenizer(CharSequence src) {
    if (src == null) throw new IllegalArgumentException("src cannot be null");
    this.source = src;
    init();
  }

  private void init() {
    length = 0;
    offset = 0;
    currToken = SwiftTokens.WHITESPACE;
    blockCommentDepth = 0;
    stringResume = false;
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

  public SwiftTokens nextToken() {
    return currToken = nextTokenInternal();
  }

  private SwiftTokens nextTokenInternal() {
    offset += length;
    if (offset >= bufferLen) return SwiftTokens.EOF;
    if (stringResume) {
      length = 0;
      return continueStringChunk();
    }
    char ch = source.charAt(offset);
    length = 1;
    if (ch == '\n') return SwiftTokens.NEWLINE;
    if (ch == '\r') {
      if (offset + 1 < bufferLen && source.charAt(offset + 1) == '\n') length++;
      return SwiftTokens.NEWLINE;
    }
    if (isWhitespace(ch)) {
      while (offset + length < bufferLen && isWhitespace(source.charAt(offset + length))) {
        length++;
      }
      return SwiftTokens.WHITESPACE;
    }
    // Line comment //
    if (ch == '/' && offset + 1 < bufferLen && source.charAt(offset + 1) == '/') {
      while (offset + length < bufferLen && source.charAt(offset + length) != '\n') {
        length++;
      }
      return SwiftTokens.LINE_COMMENT;
    }
    // Nested-aware block comment
    if (ch == '/' && offset + 1 < bufferLen && source.charAt(offset + 1) == '*') {
      length += 2;
      blockCommentDepth = 1;
      return scanBlockCommentBody();
    }
    if (ch == '"') {
      if (offset + 2 < bufferLen
          && source.charAt(offset + 1) == '"'
          && source.charAt(offset + 2) == '"') {
        length = 3;
        return scanTripleStringBody();
      }
      return continueStringChunk();
    }
    if (ch == '\'') {
      while (offset + length < bufferLen && source.charAt(offset + length) != '\'') {
        if (source.charAt(offset + length) == '\\') {
          length++;
          if (offset + length < bufferLen) length++;
        } else {
          length++;
        }
      }
      if (offset + length < bufferLen) length++;
      return SwiftTokens.CHARACTER_LITERAL;
    }
    // @annotation / @attribute
    if (ch == '@' && offset + 1 < bufferLen && isIdentifierStart(source.charAt(offset + 1))) {
      length++;
      while (offset + length < bufferLen && isIdentifierPart(source.charAt(offset + length))) {
        length++;
      }
      return SwiftTokens.ANNOTATION;
    }
    // #directive like #available, #selector, #if
    if (ch == '#' && offset + 1 < bufferLen && isIdentifierStart(source.charAt(offset + 1))) {
      length++;
      while (offset + length < bufferLen && isIdentifierPart(source.charAt(offset + length))) {
        length++;
      }
      return SwiftTokens.BUILTIN;
    }
    // `escaped identifier`
    if (ch == '`') {
      while (offset + length < bufferLen && source.charAt(offset + length) != '`') {
        length++;
      }
      if (offset + length < bufferLen) length++;
      return SwiftTokens.ESCAPED_IDENTIFIER;
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
        return SwiftTokens.LPAREN;
      case ')':
        return SwiftTokens.RPAREN;
      case '{':
        return SwiftTokens.LBRACE;
      case '}':
        return SwiftTokens.RBRACE;
      case '[':
        return SwiftTokens.LBRACK;
      case ']':
        return SwiftTokens.RBRACK;
      case ',':
        return SwiftTokens.COMMA;
      case ';':
        return SwiftTokens.SEMICOLON;
      case ':':
        return SwiftTokens.COLON;
      case '.':
        return scanDot();
      case '@':
        return SwiftTokens.AT;
      case '#':
        return SwiftTokens.HASH;
      case '+':
        return scanPlus();
      case '-':
        return scanMinus();
      case '*':
        return scanAssign(SwiftTokens.STAR, SwiftTokens.STAR_ASSIGN);
      case '/':
        return scanAssign(SwiftTokens.SLASH, SwiftTokens.SLASH_ASSIGN);
      case '%':
        return scanAssign(SwiftTokens.PERCENT, SwiftTokens.PERCENT_ASSIGN);
      case '&':
        return scanAnd();
      case '|':
        return scanOr();
      case '^':
        return scanAssign(SwiftTokens.CARET, SwiftTokens.XOR_ASSIGN);
      case '~':
        return SwiftTokens.TILDE;
      case '=':
        return scanAssign(SwiftTokens.ASSIGN, SwiftTokens.EQ);
      case '!':
        return scanNot();
      case '<':
        return scanLT();
      case '>':
        return scanGT();
      case '?':
        return scanQuestion();
      default:
        return SwiftTokens.UNKNOWN;
    }
  }

  /**
   * Scans a block comment body honoring nesting; leaves {@link #blockCommentDepth} set to the
   * pending depth when the line ends before the comment closes.
   */
  private SwiftTokens scanBlockCommentBody() {
    while (offset + length < bufferLen) {
      char c = source.charAt(offset + length);
      if (c == '/' && offset + length + 1 < bufferLen && source.charAt(offset + length + 1) == '*') {
        blockCommentDepth++;
        length += 2;
        continue;
      }
      if (c == '*' && offset + length + 1 < bufferLen && source.charAt(offset + length + 1) == '/') {
        blockCommentDepth--;
        length += 2;
        if (blockCommentDepth == 0) return SwiftTokens.BLOCK_COMMENT_COMPLETE;
        continue;
      }
      length++;
    }
    return SwiftTokens.BLOCK_COMMENT_INCOMPLETE;
  }

  /**
   * Scans a piece of a single-line double-quoted string. Splits at interpolation starts so the
   * expression can be emitted as a dedicated INTERPOLATION token.
   */
  private SwiftTokens continueStringChunk() {
    while (offset + length < bufferLen) {
      char c = source.charAt(offset + length);
      if (c == '\\') {
        if (offset + length + 1 < bufferLen) {
          char n = source.charAt(offset + length + 1);
          if (n == '(') {
            if (length > 0) {
              stringResume = true;
              return SwiftTokens.STRING_CHUNK;
            }
            length += 2;
            return scanInterpolation();
          }
          length += 2;
        } else {
          length++;
        }
        continue;
      }
      if (c == '"') {
        length++;
        stringResume = false;
        return SwiftTokens.STRING_CHUNK;
      }
      length++;
    }
    stringResume = false;
    return SwiftTokens.STRING_CHUNK;
  }

  /** Consumes a balanced \\(...) expression including delimiters. */
  private SwiftTokens scanInterpolation() {
    int depth = 1;
    while (offset + length < bufferLen) {
      char c = source.charAt(offset + length);
      if (c == '\\') {
        length += (offset + length + 1 < bufferLen) ? 2 : 1;
        continue;
      }
      if (c == '(') depth++;
      if (c == ')') {
        depth--;
        length++;
        if (depth == 0) break;
        continue;
      }
      length++;
    }
    return SwiftTokens.INTERPOLATION;
  }

  /** Scans a triple-quoted string starting right after its opening quotes. */
  private SwiftTokens scanTripleStringBody() {
    while (offset + length < bufferLen) {
      char c = source.charAt(offset + length);
      if (c == '\\') {
        length += (offset + length + 1 < bufferLen) ? 2 : 1;
        continue;
      }
      if (c == '"'
          && offset + length + 2 < bufferLen
          && source.charAt(offset + length + 1) == '"'
          && source.charAt(offset + length + 2) == '"') {
        length += 3;
        return SwiftTokens.TRIPLE_STRING_COMPLETE;
      }
      length++;
    }
    return SwiftTokens.TRIPLE_STRING_INCOMPLETE;
  }

  private SwiftTokens scanDot() {
    if (offset + 1 < bufferLen && source.charAt(offset + 1) == '.') {
      length++;
      if (offset + 1 < bufferLen && source.charAt(offset + 1) == '.') {
        length++;
        return SwiftTokens.CLOSED_RANGE;
      }
      if (offset + 1 < bufferLen && source.charAt(offset + 1) == '<') {
        length++;
        return SwiftTokens.HALF_OPEN_RANGE;
      }
      return SwiftTokens.DOUBLE_DOT;
    }
    return SwiftTokens.DOT;
  }

  private SwiftTokens scanPlus() {
    if (offset + 1 < bufferLen) {
      char n = source.charAt(offset + 1);
      if (n == '+') {
        length++;
        return SwiftTokens.INC;
      }
      if (n == '=') {
        length++;
        return SwiftTokens.PLUS_ASSIGN;
      }
    }
    return SwiftTokens.PLUS;
  }

  private SwiftTokens scanMinus() {
    if (offset + 1 < bufferLen) {
      char n = source.charAt(offset + 1);
      if (n == '-') {
        length++;
        return SwiftTokens.DEC;
      }
      if (n == '=') {
        length++;
        return SwiftTokens.MINUS_ASSIGN;
      }
      if (n == '>') {
        length++;
        return SwiftTokens.ARROW;
      }
    }
    return SwiftTokens.MINUS;
  }

  private SwiftTokens scanAnd() {
    if (offset + 1 < bufferLen) {
      char n = source.charAt(offset + 1);
      if (n == '&') {
        length++;
        return SwiftTokens.LOGICAL_AND;
      }
      if (n == '=') {
        length++;
        return SwiftTokens.AND_ASSIGN;
      }
    }
    return SwiftTokens.AMPERSAND;
  }

  private SwiftTokens scanOr() {
    if (offset + 1 < bufferLen) {
      char n = source.charAt(offset + 1);
      if (n == '|') {
        length++;
        return SwiftTokens.LOGICAL_OR;
      }
      if (n == '=') {
        length++;
        return SwiftTokens.OR_ASSIGN;
      }
    }
    return SwiftTokens.PIPE;
  }

  private SwiftTokens scanNot() {
    if (offset + 1 < bufferLen && source.charAt(offset + 1) == '=') {
      length++;
      return SwiftTokens.NOT_EQ;
    }
    return SwiftTokens.NOT;
  }

  private SwiftTokens scanLT() {
    if (offset + 1 < bufferLen) {
      char n = source.charAt(offset + 1);
      if (n == '=') {
        length++;
        return SwiftTokens.LT_EQ;
      }
      if (n == '<') {
        length++;
        if (offset + 1 < bufferLen && source.charAt(offset + 1) == '=') {
          length++;
          return SwiftTokens.SHIFT_LEFT_ASSIGN;
        }
        return SwiftTokens.SHIFT_LEFT;
      }
    }
    return SwiftTokens.LT;
  }

  private SwiftTokens scanGT() {
    if (offset + 1 < bufferLen) {
      char n = source.charAt(offset + 1);
      if (n == '=') {
        length++;
        return SwiftTokens.GT_EQ;
      }
      if (n == '>') {
        length++;
        if (offset + 1 < bufferLen && source.charAt(offset + 1) == '=') {
          length++;
          return SwiftTokens.SHIFT_RIGHT_ASSIGN;
        }
        return SwiftTokens.SHIFT_RIGHT;
      }
    }
    return SwiftTokens.GT;
  }

  private SwiftTokens scanQuestion() {
    if (offset + 1 < bufferLen && source.charAt(offset + 1) == '?') {
      length++;
      return SwiftTokens.NULL_COALESCE;
    }
    return SwiftTokens.QUESTION;
  }

  private SwiftTokens scanAssign(SwiftTokens single, SwiftTokens assignToken) {
    if (offset + 1 < bufferLen && source.charAt(offset + 1) == '=') {
      length++;
      return assignToken;
    }
    return single;
  }

  private SwiftTokens scanNumber() {
    boolean isFloat = false;
    if (offset + 1 < bufferLen && source.charAt(offset) == '0') {
      char next = source.charAt(offset + 1);
      if (next == 'x' || next == 'X') {
        length++;
        while (offset + length < bufferLen && isHexDigit(source.charAt(offset + length))) length++;
        return SwiftTokens.INTEGER_LITERAL;
      }
      if (next == 'o' || next == 'O') {
        length++;
        while (offset + length < bufferLen
            && source.charAt(offset + length) >= '0'
            && source.charAt(offset + length) <= '7') length++;
        return SwiftTokens.INTEGER_LITERAL;
      }
      if (next == 'b' || next == 'B') {
        length++;
        while (offset + length < bufferLen
            && (source.charAt(offset + length) == '0' || source.charAt(offset + length) == '1'))
          length++;
        return SwiftTokens.INTEGER_LITERAL;
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
    return isFloat ? SwiftTokens.FLOATING_LITERAL : SwiftTokens.INTEGER_LITERAL;
  }

  private SwiftTokens scanIdentifier(char first) {
    TrieTree.Node<SwiftTokens> node = keywords.root.map.get(first);
    while (offset + length < bufferLen && isIdentifierPart(source.charAt(offset + length))) {
      char c = source.charAt(offset + length);
      node = (node == null) ? null : node.map.get(c);
      length++;
    }
    if (node != null && node.token != null) {
      SwiftTokens tok = node.token;
      if (tok == SwiftTokens.BOOLEAN_KEYWORD_TRUE || tok == SwiftTokens.BOOLEAN_KEYWORD_FALSE)
        return SwiftTokens.BOOLEAN_LITERAL;
      if (tok == SwiftTokens.NIL_KEYWORD) return SwiftTokens.NIL_LITERAL;
      return tok;
    }
    String text = source.subSequence(offset, offset + length).toString();
    if (types.get(text, 0, text.length()) != null) return SwiftTokens.TYPE_KEYWORD;
    if (builtins.get(text, 0, text.length()) != null) return SwiftTokens.BUILTIN;
    return SwiftTokens.IDENTIFIER;
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

  private static void putAll(TrieTree<SwiftTokens> tree, String[] words, SwiftTokens token) {
    for (String word : words) tree.put(word, token);
  }

  private static void doStaticInit() {
    keywords = new TrieTree<>();
    putAll(
        keywords,
        new String[] {
          "associatedtype", "class", "deinit", "enum", "extension", "fileprivate", "func",
          "import", "init", "inout", "internal", "let", "open", "operator", "private",
          "precedencegroup", "protocol", "public", "rethrows", "static", "struct", "subscript",
          "typealias", "var", "break", "case", "catch", "continue", "default", "defer", "do",
          "else", "fallthrough", "for", "guard", "if", "in", "repeat", "return", "throw",
          "switch", "where", "while", "as", "is", "super", "self", "Self", "throws", "try",
          "await", "async", "actor", "some", "any", "weak", "unowned", "lazy", "willSet",
          "didSet", "get", "set", "mutating", "nonmutating", "required", "convenience",
          "override", "final", "indirect", "infix", "postfix", "prefix"
        },
        SwiftTokens.KEYWORD);
    putAll(keywords, new String[] {"true"}, SwiftTokens.BOOLEAN_KEYWORD_TRUE);
    putAll(keywords, new String[] {"false"}, SwiftTokens.BOOLEAN_KEYWORD_FALSE);
    putAll(keywords, new String[] {"nil"}, SwiftTokens.NIL_KEYWORD);
    types = new TrieTree<>();
    putAll(
        types,
        new String[] {
          "Int", "Int8", "Int16", "Int32", "Int64", "UInt", "UInt8", "UInt16", "UInt32",
          "UInt64", "Float", "Double", "Bool", "Character", "String", "Array", "Dictionary",
          "Set", "Optional", "Result", "Error", "AnyObject", "Void", "Never", "Range",
          "ClosedRange", "Slice", "KeyPath", "Codable", "Decodable", "Encodable", "Equatable",
          "Comparable", "Hashable", "CustomStringConvertible", "Sequence", "Collection"
        },
        SwiftTokens.TYPE_KEYWORD);
    builtins = new TrieTree<>();
    putAll(
        builtins,
        new String[] {
          "print", "debugPrint", "readLine", "fatalError", "assert", "precondition", "zip",
          "stride", "min", "max", "abs", "swap", "dump", "type", "of"
        },
        SwiftTokens.BUILTIN);
  }
}
