/**
 * Comment by ghost ide
 *
 * @author: Ninjacoder
 */
package ir.hanzodev1375.ghostide.codeeditors.langs.asm;

public enum AsmTokens {
  WHITESPACE,
  NEWLINE,
  EOF,
  UNKNOWN,
  // ; // /* */
  LINE_COMMENT,
  BLOCK_COMMENT_COMPLETE,
  BLOCK_COMMENT_INCOMPLETE,
  LPAREN,
  RPAREN,
  LBRACK,
  RBRACK,
  COMMA,
  COLON,
  PLUS,
  MINUS,
  STAR,
  SLASH,
  PERCENT,
  CARET,
  TILDE,
  LT,
  GT,
  ASSIGN,
  EQ,
  NOT_EQ,
  NOT,
  SHIFT_LEFT,
  SHIFT_RIGHT,
  // $
  DOLLAR,
  // #
  HASH,
  // @
  AT,
  // .
  DOT,
  INTEGER_LITERAL,
  FLOATING_LITERAL,
  CHARACTER_LITERAL,
  STRING_LITERAL,
  IDENTIFIER,
  // mov, add, ldr, str, b, bl ...
  INSTRUCTION,
  // rax, x0, w1, sp, lr, pc ...
  REGISTER,
  // .section, .global, %define, db, resb ...
  DIRECTIVE
}
