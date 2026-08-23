/**
 * Comment by ghost ide
 *
 * @author: Ninjacoder
 */
package ir.hanzodev1375.ghostide.codeeditors.langs.perl;

public enum PerlTokens {
  WHITESPACE,
  NEWLINE,
  EOF,
  UNKNOWN,
  // #
  LINE_COMMENT,
  /* POD documentation blocks */
  POD_LINE,
  POD_BODY,
  POD_END,
  /* heredoc chunks */
  HEREDOC_START,
  HEREDOC_BODY,
  HEREDOC_END,
  LPAREN,
  RPAREN,
  LBRACE,
  RBRACE,
  LBRACK,
  RBRACK,
  SEMICOLON,
  COLON,
  COMMA,
  DOT,
  // .
  CONCAT,
  // ..
  RANGE,
  // ...
  ELLIPSIS,
  PLUS,
  MINUS,
  STAR,
  /** ** exponent operator */
  POWER,
  SLASH,
  PERCENT,
  AMPERSAND,
  PIPE,
  CARET,
  TILDE,
  LT,
  GT,
  LT_EQ,
  GT_EQ,
  // ==
  EQ,
  // !=
  NOT_EQ,
  // <=>
  SPACESHIP,
  ASSIGN,
  PLUS_ASSIGN,
  MINUS_ASSIGN,
  STAR_ASSIGN,
  SLASH_ASSIGN,
  PERCENT_ASSIGN,
  POWER_ASSIGN,
  CONCAT_ASSIGN,
  AND_ASSIGN,
  OR_ASSIGN,
  XOR_ASSIGN,
  // =~
  MATCH,
  // !~
  NOT_MATCH,
  // ->
  ARROW,
  // =>
  FAT_COMMA,
  // //
  DEFINED_OR,
  // //=
  DEFINED_OR_ASSIGN,
  INC,
  DEC,
  LOGICAL_AND,
  LOGICAL_OR,
  NOT,
  SHIFT_LEFT,
  SHIFT_RIGHT,
  SHIFT_LEFT_ASSIGN,
  SHIFT_RIGHT_ASSIGN,
  QUESTION,
  DOLLAR,
  AT,
  HASH,
  BACKTICK,
  // ::
  PACKAGE_SEP,
  INTEGER_LITERAL,
  FLOATING_LITERAL,
  /** Piece of an interpolating double-quoted string. */
  STRING_CHUNK,
  /** Non-interpolating single-quoted or backtick string. */
  STRING_LITERAL,
  /** Interpolated expression inside a string: $var, ${expr}, @arr, @{expr}. */
  INTERPOLATION,
  /** Sigil variable in code: $var, @arr, %hash, $#arr, $_ ... */
  SIGIL_VAR,
  IDENTIFIER,
  KEYWORD,
  BUILTIN
}
