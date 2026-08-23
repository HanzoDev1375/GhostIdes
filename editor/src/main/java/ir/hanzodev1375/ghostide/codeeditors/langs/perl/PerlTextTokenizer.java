/**
 * Comment by ghost ide
 *
 * @author: Ninjacoder
 */
package ir.hanzodev1375.ghostide.codeeditors.langs.perl;

import io.github.rosemoe.sora.util.TrieTree;

/**
 * Line tokenizer for Perl with full support for sigil variables ($var, @arr, %hash, $#arr),
 * interpolation inside double-quoted strings, heredocs and POD blocks.
 */
public class PerlTextTokenizer {

  private static TrieTree<PerlTokens> keywords;

  private static TrieTree<PerlTokens> builtins;

  static {
    doStaticInit();
  }

  public static TrieTree<PerlTokens> getKeywordsTree() {
    return keywords;
  }

  private CharSequence source;
  private int bufferLen;
  public int offset;
  public int length;
  /** Label of the heredoc opened by the last HEREDOC_START token. */
  public String heredocDelimiter;
  /** True while the scan is paused inside a double-quoted string body. */
  private boolean stringResume;

  private PerlTokens currToken;

  public PerlTextTokenizer(CharSequence src) {
    if (src == null) throw new IllegalArgumentException("src cannot be null");
    this.source = src;
    init();
  }

  private void init() {
    length = 0;
    offset = 0;
    currToken = PerlTokens.WHITESPACE;
    heredocDelimiter = null;
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

  public PerlTokens nextToken() {
    return currToken = nextTokenInternal();
  }

  /** Returns the char at {@code idx} positions from the token start, or '\0' past the end. */
  private char at(int idx) {
    return (offset + idx) < bufferLen ? source.charAt(offset + idx) : '\0';
  }

  private PerlTokens nextTokenInternal() {
    offset += length;
    if (offset >= bufferLen) return PerlTokens.EOF;
    if (stringResume) {
      length = 0;
      return continueDoubleChunk();
    }
    char ch = at(0);
    length = 1;
    if (ch == '\n') return PerlTokens.NEWLINE;
    if (ch == '\r') {
      if (at(1) == '\n') length++;
      return PerlTokens.NEWLINE;
    }
    if (isWhitespace(ch)) {
      while (offset + length < bufferLen && isWhitespace(at(length))) {
        length++;
      }
      return PerlTokens.WHITESPACE;
    }
    // POD block start: '=' followed by a letter at column zero
    if (ch == '=' && offset == 0 && isIdentifierStart(at(1)) && !isPodCutLine()) {
      while (offset + length < bufferLen && at(length) != '\n') length++;
      return PerlTokens.POD_LINE;
    }
    // Line comment #
    if (ch == '#') {
      while (offset + length < bufferLen && at(length) != '\n') {
        length++;
      }
      return PerlTokens.LINE_COMMENT;
    }
    if (ch == '"') {
      return continueDoubleChunk();
    }
    if (ch == '\'' || ch == '`') {
      while (offset + length < bufferLen && at(length) != ch) {
        if (at(length) == '\\') {
          length++;
          if (offset + length < bufferLen) length++;
        } else {
          length++;
        }
      }
      if (offset + length < bufferLen) length++;
      return PerlTokens.STRING_LITERAL;
    }
    // Sigil variables in code
    if (ch == '$' || ch == '@' || ch == '%') {
      return scanSigil(ch);
    }
    if (isDigit(ch) || (ch == '.' && isDigit(at(1)))) {
      return scanNumber();
    }
    if (isIdentifierStart(ch)) {
      return scanIdentifier(ch);
    }
    switch (ch) {
      case '(':
        return PerlTokens.LPAREN;
      case ')':
        return PerlTokens.RPAREN;
      case '{':
        return PerlTokens.LBRACE;
      case '}':
        return PerlTokens.RBRACE;
      case '[':
        return PerlTokens.LBRACK;
      case ']':
        return PerlTokens.RBRACK;
      case ',':
        return PerlTokens.COMMA;
      case ';':
        return PerlTokens.SEMICOLON;
      case ':':
        return scanColon();
      case '.':
        return scanDot();
      case '+':
        return scanPlus();
      case '-':
        return scanMinus();
      case '*':
        return scanStar();
      case '/':
        return scanSlash();
      case '&':
        return scanAnd();
      case '|':
        return scanOr();
      case '^':
        return scanTwo(PerlTokens.CARET, '=', PerlTokens.XOR_ASSIGN);
      case '~':
        return PerlTokens.TILDE;
      case '=':
        return scanEq();
      case '!':
        return scanNot();
      case '<':
        return scanLT();
      case '>':
        return scanGT();
      case '?':
        return PerlTokens.QUESTION;
      default:
        return PerlTokens.UNKNOWN;
    }
  }

  /** Detects an "=cut" line so POD termination is not mistaken for a new POD block. */
  private boolean isPodCutLine() {
    return at(1) == 'c' && at(2) == 'u' && at(3) == 't';
  }

  /**
   * Scans a piece of an interpolating double-quoted string, splitting at $var, ${expr}, @arr and
   * @{expr} interpolation starts.
   */
  private PerlTokens continueDoubleChunk() {
    while (offset + length < bufferLen) {
      char c = at(length);
      if (c == '\\') {
        length += (offset + length + 1 < bufferLen) ? 2 : 1;
        continue;
      }
      if (c == '"') {
        length++;
        stringResume = false;
        return PerlTokens.STRING_CHUNK;
      }
      if (c == '$' || c == '@') {
        char n = at(length + 1);
        if (n == '{') {
          if (length > 0) {
            stringResume = true;
            return PerlTokens.STRING_CHUNK;
          }
          length++;
          return scanBalanced('}');
        }
        boolean lastIndex = c == '$' && n == '#' && isIdentifierStart(at(length + 2));
        if (lastIndex || isIdentifierStart(n)) {
          if (length > 0) {
            stringResume = true;
            return PerlTokens.STRING_CHUNK;
          }
          length += lastIndex ? 2 : 1;
          scanIdentPath();
          return PerlTokens.INTERPOLATION;
        }
      }
      length++;
    }
    stringResume = false;
    return PerlTokens.STRING_CHUNK;
  }

  /** Consumes a balanced {...} expression; opening brace must already be consumed. */
  private PerlTokens scanBalanced(char close) {
    int depth = 1;
    while (offset + length < bufferLen) {
      char c = at(length);
      if (c == '\\') {
        length += (offset + length + 1 < bufferLen) ? 2 : 1;
        continue;
      }
      if (close == '}' && c == '{') {
        depth++;
        length++;
        continue;
      }
      if (c == close) {
        depth--;
        length++;
        if (depth == 0) break;
        continue;
      }
      length++;
    }
    return PerlTokens.INTERPOLATION;
  }

  /** Consumes an identifier path with optional Package::Name segments. */
  private void scanIdentPath() {
    while (offset + length < bufferLen) {
      if (isIdentifierPart(at(length))) {
        length++;
        continue;
      }
      if (at(length) == ':' && at(length + 1) == ':' && isIdentifierStart(at(length + 2))) {
        length += 3;
        continue;
      }
      break;
    }
  }

  /** Scans a code-level sigil variable like $var, @arr, %hash, $#arr, $_ or ${expr}. */
  private PerlTokens scanSigil(char sigil) {
    char n = at(1);
    if (sigil == '$') {
      if (n == '#' && isIdentifierStart(at(2))) {
        length += 2;
        scanIdentPath();
        return PerlTokens.SIGIL_VAR;
      }
      if (isIdentifierStart(n)) {
        length++;
        scanIdentPath();
        return PerlTokens.SIGIL_VAR;
      }
      if (n == '{') {
        length++;
        return scanBalanced('}');
      }
      if (isDigit(n)) {
        length++;
        while (offset + length < bufferLen && isDigit(at(length))) length++;
        return PerlTokens.SIGIL_VAR;
      }
      if (isSpecialVarChar(n)) {
        length++;
        return PerlTokens.SIGIL_VAR;
      }
      return PerlTokens.DOLLAR;
    }
    if (isIdentifierStart(n)) {
      length++;
      scanIdentPath();
      return PerlTokens.SIGIL_VAR;
    }
    if (n == '{') {
      length++;
      return scanBalanced('}');
    }
    return sigil == '@' ? PerlTokens.AT : PerlTokens.PERCENT;
  }

  private static boolean isSpecialVarChar(char c) {
    switch (c) {
      case '_':
      case '$':
      case '&':
      case '!':
      case ',':
      case '/':
      case ';':
      case '.':
      case '%':
      case '^':
      case '|':
      case '<':
      case '>':
      case '~':
      case '-':
      case '+':
      case ':':
      case '(':
      case ')':
      case '[':
      case ']':
      case '{':
      case '}':
        return true;
      default:
        return false;
    }
  }

  /** Scans "&lt;&lt;" as heredoc opener or shift operator. */
  private PerlTokens scanLT() {
    char n1 = at(1);
    if (n1 == '=') {
      if (at(2) == '>') {
        length += 2;
        return PerlTokens.SPACESHIP;
      }
      length++;
      return PerlTokens.LT_EQ;
    }
    if (n1 == '<') {
      if (tryScanHeredoc()) return PerlTokens.HEREDOC_START;
      length++;
      if (at(2) == '=') {
        length++;
        return PerlTokens.SHIFT_LEFT_ASSIGN;
      }
      return PerlTokens.SHIFT_LEFT;
    }
    return PerlTokens.LT;
  }

  /**
   * Tries to read a heredoc label after "<<" with optional ~ and optional quoting. On success
   * {@link #heredocDelimiter} holds the label and the rest of the line is consumed.
   */
  private boolean tryScanHeredoc() {
    int pos = 2;
    if (at(pos) == '~') pos++;
    char quote = '\0';
    if (at(pos) == '\'' || at(pos) == '"') {
      quote = at(pos);
      pos++;
    }
    int labelStart = pos;
    while (offset + pos < bufferLen && isIdentifierPart(at(pos))) pos++;
    if (pos == labelStart) return false;
    heredocDelimiter = source.subSequence(offset + labelStart, offset + pos).toString();
    if (quote != '\0' && at(pos) == quote) pos++;
    while (offset + length < bufferLen && at(length) != '\n') length++;
    return true;
  }

  private PerlTokens scanGT() {
    if (at(1) == '=') {
      length++;
      return PerlTokens.GT_EQ;
    }
    if (at(1) == '>') {
      length++;
      if (at(2) == '=') {
        length++;
        return PerlTokens.SHIFT_RIGHT_ASSIGN;
      }
      return PerlTokens.SHIFT_RIGHT;
    }
    return PerlTokens.GT;
  }

  private PerlTokens scanColon() {
    if (at(1) == ':') {
      length++;
      return PerlTokens.PACKAGE_SEP;
    }
    return PerlTokens.COLON;
  }

  private PerlTokens scanDot() {
    if (at(1) == '.') {
      length++;
      if (at(2) == '.') {
        length++;
        return PerlTokens.ELLIPSIS;
      }
      return PerlTokens.RANGE;
    }
    if (at(1) == '=') {
      length++;
      return PerlTokens.CONCAT_ASSIGN;
    }
    return PerlTokens.CONCAT;
  }

  private PerlTokens scanPlus() {
    if (at(1) == '+') {
      length++;
      return PerlTokens.INC;
    }
    return scanTwo(PerlTokens.PLUS, '=', PerlTokens.PLUS_ASSIGN);
  }

  private PerlTokens scanMinus() {
    if (at(1) == '-') {
      length++;
      return PerlTokens.DEC;
    }
    if (at(1) == '=') {
      length++;
      return PerlTokens.MINUS_ASSIGN;
    }
    if (at(1) == '>') {
      length++;
      return PerlTokens.ARROW;
    }
    return PerlTokens.MINUS;
  }

  private PerlTokens scanStar() {
    if (at(1) == '*') {
      length++;
      if (at(2) == '=') {
        length++;
        return PerlTokens.POWER_ASSIGN;
      }
      return PerlTokens.POWER;
    }
    return scanTwo(PerlTokens.STAR, '=', PerlTokens.STAR_ASSIGN);
  }

  private PerlTokens scanSlash() {
    if (at(1) == '/') {
      length++;
      if (at(2) == '=') {
        length++;
        return PerlTokens.DEFINED_OR_ASSIGN;
      }
      return PerlTokens.DEFINED_OR;
    }
    return scanTwo(PerlTokens.SLASH, '=', PerlTokens.SLASH_ASSIGN);
  }

  private PerlTokens scanAnd() {
    if (at(1) == '&') {
      length++;
      return PerlTokens.LOGICAL_AND;
    }
    return scanTwo(PerlTokens.AMPERSAND, '=', PerlTokens.AND_ASSIGN);
  }

  private PerlTokens scanOr() {
    if (at(1) == '|') {
      length++;
      return PerlTokens.LOGICAL_OR;
    }
    return scanTwo(PerlTokens.PIPE, '=', PerlTokens.OR_ASSIGN);
  }

  private PerlTokens scanEq() {
    if (at(1) == '=') {
      length++;
      return PerlTokens.EQ;
    }
    if (at(1) == '~') {
      length++;
      return PerlTokens.MATCH;
    }
    if (at(1) == '>') {
      length++;
      return PerlTokens.FAT_COMMA;
    }
    return PerlTokens.ASSIGN;
  }

  private PerlTokens scanNot() {
    if (at(1) == '=') {
      length++;
      return PerlTokens.NOT_EQ;
    }
    if (at(1) == '~') {
      length++;
      return PerlTokens.NOT_MATCH;
    }
    return PerlTokens.NOT;
  }

  private PerlTokens scanTwo(PerlTokens single, char nextChar, PerlTokens doubleToken) {
    if (at(1) == nextChar) {
      length++;
      return doubleToken;
    }
    return single;
  }

  private PerlTokens scanNumber() {
    boolean isFloat = false;
    if (at(0) == '0') {
      char next = at(1);
      if (next == 'x' || next == 'X') {
        length++;
        while (offset + length < bufferLen && isHexDigit(at(length))) length++;
        return PerlTokens.INTEGER_LITERAL;
      }
      if (next == 'b' || next == 'B') {
        length++;
        while (offset + length < bufferLen && (at(length) == '0' || at(length) == '1')) length++;
        return PerlTokens.INTEGER_LITERAL;
      }
    }
    while (offset + length < bufferLen && (isDigit(at(length)) || at(length) == '_')) {
      length++;
    }
    if (at(length) == '.' && isDigit(at(length + 1))) {
      isFloat = true;
      length++;
      while (offset + length < bufferLen && isDigit(at(length))) length++;
    }
    if (at(length) == 'e' || at(length) == 'E') {
      int save = length;
      length++;
      if (at(length) == '+' || at(length) == '-') length++;
      if (isDigit(at(length))) {
        isFloat = true;
        while (offset + length < bufferLen && isDigit(at(length))) length++;
      } else {
        length = save;
      }
    }
    return isFloat ? PerlTokens.FLOATING_LITERAL : PerlTokens.INTEGER_LITERAL;
  }

  private PerlTokens scanIdentifier(char first) {
    TrieTree.Node<PerlTokens> node = keywords.root.map.get(first);
    while (offset + length < bufferLen && isIdentifierPart(at(length))) {
      char c = at(length);
      node = (node == null) ? null : node.map.get(c);
      length++;
    }
    if (node != null && node.token != null) return node.token;
    String text = source.subSequence(offset, offset + length).toString();
    if (builtins.get(text, 0, text.length()) != null) return PerlTokens.BUILTIN;
    return PerlTokens.IDENTIFIER;
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

  private static void putAll(TrieTree<PerlTokens> tree, String[] words, PerlTokens token) {
    for (String word : words) tree.put(word, token);
  }

  private static void doStaticInit() {
    keywords = new TrieTree<>();
    putAll(
        keywords,
        new String[] {
          "my", "our", "local", "sub", "if", "elsif", "else", "unless", "while", "until",
          "for", "foreach", "do", "last", "next", "redo", "goto", "return", "wantarray",
          "use", "no", "require", "package", "bless", "ref", "defined", "undef", "exists",
          "delete", "each", "keys", "values", "and", "or", "not", "xor", "eq", "ne", "lt",
          "gt", "le", "ge", "cmp", "x", "__PACKAGE__", "__FILE__", "__LINE__", "__SUB__",
          "__DATA__", "__END__"
        },
        PerlTokens.KEYWORD);
    builtins = new TrieTree<>();
    putAll(
        builtins,
        new String[] {
          "print", "say", "printf", "sprintf", "die", "warn", "open", "close", "binmode",
          "chomp", "chop", "lc", "uc", "lcfirst", "ucfirst", "length", "substr", "index",
          "rindex", "split", "join", "sort", "reverse", "grep", "map", "push", "pop", "shift",
          "unshift", "splice", "scalar", "eval", "exec", "system", "fork", "wait", "waitpid",
          "kill", "sleep", "rand", "srand", "time", "localtime", "gmtime", "chdir", "mkdir",
          "rmdir", "rename", "unlink", "chmod", "chown", "umask", "read", "write", "seek",
          "tell", "eof", "abs", "int", "sqrt", "exp", "log", "hex", "oct", "ord", "chr",
          "crypt", "qw", "qq", "q", "qr", "m", "s", "tr", "y"
        },
        PerlTokens.BUILTIN);
  }
}
