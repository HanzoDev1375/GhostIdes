/**
 * Comment by ghost ide
 *
 * @author: Ninjacoder
 */
package ir.hanzodev1375.ghostide.codeeditors.langs.html;

public enum HtmlTokens {
  WHITESPACE,
  NEWLINE,
  EOF,
  UNKNOWN,
  TEXT,
  ENTITY,
  COMMENT_COMPLETE,
  COMMENT_INCOMPLETE,
  DOCTYPE,
  CDATA_COMPLETE,
  CDATA_INCOMPLETE,
  LT,
  LT_SLASH,
  GT,
  SLASH_GT,
  TAG_NAME,
  ATTR_NAME,
  ASSIGN,
  ATTR_VALUE,
  RAW_TEXT,
  RAW_TEXT_INCOMPLETE,
  RAW_TAG_CLOSE_START
}
