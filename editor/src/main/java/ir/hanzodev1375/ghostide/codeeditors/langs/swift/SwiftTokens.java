/**
 * Comment by ghost ide
 *
 * @author: Ninjacoder
 */
package ir.hanzodev1375.ghostide.codeeditors.langs.swift;

public enum SwiftTokens {
  WHITESPACE,
  NEWLINE,
  EOF,
  UNKNOWN,
  // //
  LINE_COMMENT,
  /* nested-aware block comment chunks */
  BLOCK_COMMENT_COMPLETE,
  BLOCK_COMMENT_INCOMPLETE,
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
  // ..
  DOUBLE_DOT,
  // ...
  CLOSED_RANGE,
  // ..<
  HALF_OPEN_RANGE,
  AT,
  HASH,
  BACKTICK,
  PLUS,
  MINUS,
  STAR,
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
  ASSIGN,
  EQ,
  NOT_EQ,
  NOT,
  INC,
  DEC,
  LOGICAL_AND,
  LOGICAL_OR,
  SHIFT_LEFT,
  SHIFT_RIGHT,
  SHIFT_LEFT_ASSIGN,
  SHIFT_RIGHT_ASSIGN,
  PLUS_ASSIGN,
  MINUS_ASSIGN,
  STAR_ASSIGN,
  SLASH_ASSIGN,
  PERCENT_ASSIGN,
  AND_ASSIGN,
  OR_ASSIGN,
  XOR_ASSIGN,
  // ->
  ARROW,
  // ??
  NULL_COALESCE,
  QUESTION,
  INTEGER_LITERAL,
  FLOATING_LITERAL,
  CHARACTER_LITERAL,
  /** A plain piece of a single-line string, may be the opening or closing part. */
  STRING_CHUNK,
  /** \\(expr) interpolation expression including delimiters. */
  INTERPOLATION,
  TRIPLE_STRING_COMPLETE,
  TRIPLE_STRING_INCOMPLETE,
  IDENTIFIER,
  ESCAPED_IDENTIFIER,
  KEYWORD,
  TYPE_KEYWORD,
  BUILTIN,
  ANNOTATION,
  BOOLEAN_LITERAL,
  NIL_LITERAL,
  /* internal trie markers, never emitted as spans directly */
  BOOLEAN_KEYWORD_TRUE,
  BOOLEAN_KEYWORD_FALSE,
  NIL_KEYWORD
}
