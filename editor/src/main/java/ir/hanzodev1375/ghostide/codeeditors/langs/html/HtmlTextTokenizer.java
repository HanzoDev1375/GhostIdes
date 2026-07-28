/**
 * Comment by ghost ide
 *
 * @author: Ninjacoder
 */
package ir.hanzodev1375.ghostide.codeeditors.langs.html;

import io.github.rosemoe.sora.util.MyCharacter;

public class HtmlTextTokenizer {

  public static final int STATE_TEXT = 0;
  public static final int STATE_COMMENT = 1;
  public static final int STATE_CDATA = 2;
  public static final int STATE_RAW_SCRIPT = 3;
  public static final int STATE_RAW_STYLE = 4;

  private CharSequence source;
  private int bufferLen;
  private int index;

  public int offset;
  public int length;

  private HtmlTokens currToken;

  private int mode;
  private boolean inTag;
  private boolean tagNameConsumed;
  private boolean closingTag;
  private String currentTagName = "";

  public HtmlTextTokenizer(CharSequence src) {
    if (src == null) throw new IllegalArgumentException("src cannot be null");
    this.source = src;
    init();
  }

  private void init() {
    length = 0;
    index = 0;
    offset = 0;
    currToken = HtmlTokens.WHITESPACE;
    this.bufferLen = source.length();
    mode = STATE_TEXT;
    inTag = false;
    tagNameConsumed = false;
    closingTag = false;
  }

  public void reset(CharSequence src) {
    if (src == null) throw new IllegalArgumentException("src cannot be null");
    this.source = src;
    this.bufferLen = src.length();
    init();
  }

  public void resume(CharSequence src, int state, String tagName) {
    reset(src);
    switch (state) {
      case STATE_COMMENT:
        mode = STATE_COMMENT;
        break;
      case STATE_CDATA:
        mode = STATE_CDATA;
        break;
      case STATE_RAW_SCRIPT:
        mode = STATE_RAW_SCRIPT;
        currentTagName = tagName;
        break;
      case STATE_RAW_STYLE:
        mode = STATE_RAW_STYLE;
        currentTagName = tagName;
        break;
      default:
        mode = STATE_TEXT;
    }
  }

  public int getMode() {
    return mode;
  }

  public String getCurrentTagName() {
    return currentTagName;
  }

  public CharSequence getTokenText() {
    return source.subSequence(offset, offset + length);
  }

  public int getTokenLength() {
    return length;
  }

  public HtmlTokens nextToken() {
    return currToken = nextTokenInternal();
  }

  private HtmlTokens nextTokenInternal() {
    index += length;
    offset += length;
    if (offset >= bufferLen) return HtmlTokens.EOF;

    if (mode == STATE_COMMENT) {
      length = 0;
      return continueComment();
    }
    if (mode == STATE_CDATA) {
      length = 0;
      return continueCdata();
    }
    if (mode == STATE_RAW_SCRIPT || mode == STATE_RAW_STYLE) {
      length = 0;
      return continueRawText();
    }

    char ch = source.charAt(offset);
    length = 1;

    if (ch == '\n') return HtmlTokens.NEWLINE;
    if (ch == '\r') {
      if (offset + 1 < bufferLen && source.charAt(offset + 1) == '\n') length++;
      return HtmlTokens.NEWLINE;
    }

    if (inTag) {
      return nextTagToken(ch);
    }

    if (isWhitespace(ch)) {
      while (offset + length < bufferLen && isWhitespace(source.charAt(offset + length))) {
        length++;
      }
      return HtmlTokens.WHITESPACE;
    }

    if (ch == '<') {
      return startMarkup();
    }

    if (ch == '&') {
      return scanEntity();
    }

    while (offset + length < bufferLen) {
      char c = source.charAt(offset + length);
      if (c == '<' || c == '&' || c == '\n') break;
      length++;
    }
    return HtmlTokens.TEXT;
  }

  private HtmlTokens startMarkup() {
    char next = offset + 1 < bufferLen ? source.charAt(offset + 1) : '\0';
    if (next == '!') {
      if (matchesAt(offset, "<!--")) {
        length = 4;
        mode = STATE_COMMENT;
        return continueComment();
      }
      if (matchesAt(offset, "<![CDATA[")) {
        length = 9;
        mode = STATE_CDATA;
        return continueCdata();
      }
      if (matchesIgnoreCaseAt(offset, "<!doctype")) {
        while (offset + length < bufferLen && source.charAt(offset + length) != '>') length++;
        if (offset + length < bufferLen) length++;
        return HtmlTokens.DOCTYPE;
      }
      length = 1;
      inTag = true;
      tagNameConsumed = true;
      closingTag = false;
      currentTagName = "";
      return HtmlTokens.LT;
    }
    if (next == '/') {
      length = 2;
      inTag = true;
      tagNameConsumed = false;
      closingTag = true;
      currentTagName = "";
      return HtmlTokens.LT_SLASH;
    }
    inTag = true;
    tagNameConsumed = false;
    closingTag = false;
    currentTagName = "";
    return HtmlTokens.LT;
  }

  private HtmlTokens nextTagToken(char ch) {
    if (isWhitespace(ch)) {
      while (offset + length < bufferLen && isWhitespace(source.charAt(offset + length))) {
        length++;
      }
      return HtmlTokens.WHITESPACE;
    }
    if (ch == '/' && offset + 1 < bufferLen && source.charAt(offset + 1) == '>') {
      length = 2;
      inTag = false;
      return HtmlTokens.SLASH_GT;
    }
    if (ch == '>') {
      inTag = false;
      String tag = currentTagName.toLowerCase();
      if (!closingTag && tag.equals("script")) {
        mode = STATE_RAW_SCRIPT;
      } else if (!closingTag && tag.equals("style")) {
        mode = STATE_RAW_STYLE;
      }
      return HtmlTokens.GT;
    }
    if (ch == '=') {
      return HtmlTokens.ASSIGN;
    }
    if (ch == '"' || ch == '\'') {
      char quote = ch;
      while (offset + length < bufferLen) {
        char c = source.charAt(offset + length);
        if (c == '\n') break;
        length++;
        if (c == quote) break;
      }
      return HtmlTokens.ATTR_VALUE;
    }
    if (!tagNameConsumed && isNameStart(ch)) {
      while (offset + length < bufferLen && isNameChar(source.charAt(offset + length))) length++;
      currentTagName = source.subSequence(offset, offset + length).toString();
      tagNameConsumed = true;
      return HtmlTokens.TAG_NAME;
    }
    if (isNameStart(ch)) {
      while (offset + length < bufferLen && isNameChar(source.charAt(offset + length))) length++;
      return HtmlTokens.ATTR_NAME;
    }
    while (offset + length < bufferLen) {
      char c = source.charAt(offset + length);
      if (isWhitespace(c) || c == '>' || c == '=' || c == '\n') break;
      length++;
    }
    return HtmlTokens.ATTR_VALUE;
  }

  private HtmlTokens continueComment() {
    while (offset + length < bufferLen) {
      if (matchesAt(offset + length, "-->")) {
        length += 3;
        mode = STATE_TEXT;
        return HtmlTokens.COMMENT_COMPLETE;
      }
      length++;
    }
    return HtmlTokens.COMMENT_INCOMPLETE;
  }

  private HtmlTokens continueCdata() {
    while (offset + length < bufferLen) {
      if (matchesAt(offset + length, "]]>")) {
        length += 3;
        mode = STATE_TEXT;
        return HtmlTokens.CDATA_COMPLETE;
      }
      length++;
    }
    return HtmlTokens.CDATA_INCOMPLETE;
  }

  private HtmlTokens continueRawText() {
    String closeTag = mode == STATE_RAW_SCRIPT ? "</script" : "</style";
    while (offset + length < bufferLen) {
      if (matchesIgnoreCaseAt(offset + length, closeTag)) {
        if (length == 0) {
          length = 2;
          inTag = true;
          tagNameConsumed = false;
          closingTag = true;
          currentTagName = "";
          mode = STATE_TEXT;
          return HtmlTokens.LT_SLASH;
        }
        return HtmlTokens.RAW_TEXT;
      }
      length++;
    }
    return HtmlTokens.RAW_TEXT_INCOMPLETE;
  }

  private HtmlTokens scanEntity() {
    int max = Math.min(bufferLen, offset + 32);
    int i = offset + 1;
    if (i < max && source.charAt(i) == '#') {
      i++;
      if (i < max && (source.charAt(i) == 'x' || source.charAt(i) == 'X')) i++;
      int digitsStart = i;
      while (i < max && Character.isLetterOrDigit(source.charAt(i))) i++;
      if (i > digitsStart && i < bufferLen && source.charAt(i) == ';') {
        length = i + 1 - offset;
        return HtmlTokens.ENTITY;
      }
    } else {
      int nameStart = i;
      while (i < max && Character.isLetterOrDigit(source.charAt(i))) i++;
      if (i > nameStart && i < bufferLen && source.charAt(i) == ';') {
        length = i + 1 - offset;
        return HtmlTokens.ENTITY;
      }
    }
    return HtmlTokens.TEXT;
  }

  private boolean matchesAt(int pos, String literal) {
    if (pos + literal.length() > bufferLen) return false;
    for (int i = 0; i < literal.length(); i++) {
      if (source.charAt(pos + i) != literal.charAt(i)) return false;
    }
    return true;
  }

  private boolean matchesIgnoreCaseAt(int pos, String literal) {
    if (pos + literal.length() > bufferLen) return false;
    for (int i = 0; i < literal.length(); i++) {
      if (Character.toLowerCase(source.charAt(pos + i)) != Character.toLowerCase(literal.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  private static boolean isWhitespace(char c) {
    return c == ' ' || c == '\t' || c == '\f';
  }

  private static boolean isNameStart(char c) {
    return MyCharacter.isJavaIdentifierStart(c);
  }

  private static boolean isNameChar(char c) {
    return MyCharacter.isJavaIdentifierPart(c) || c == '-' || c == ':' || c == '.';
  }
}
