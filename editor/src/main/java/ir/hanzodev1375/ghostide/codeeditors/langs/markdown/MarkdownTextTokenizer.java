package ir.hanzodev1375.ghostide.codeeditors.langs.markdown;

public class MarkdownTextTokenizer {

  private char emphasisOpenChar = 0;
  private int emphasisOpenRun = 0;
  private CharSequence source;
  private int bufferLen;
  public int offset;
  public int length;
  private Tokens currToken;

  
  public MarkdownTextTokenizer(CharSequence src) {
    reset(src);
  }

  public void reset(CharSequence src) {
    if (src == null) throw new IllegalArgumentException("src cannot be null");
    source = src;
    bufferLen = src.length();
    offset = 0;
    length = 0;
    currToken = Tokens.WHITESPACE;
    emphasisOpenChar = 0;
    emphasisOpenRun = 0;
  }

  
  public Tokens getToken() {
    return currToken;
  }

  public int getOffset() {
    return offset;
  }

  public int getLength() {
    return length;
  }

  public CharSequence getTokenText() {
    return source.subSequence(offset, offset + length);
  }

  
  public Tokens nextToken() {
    offset += length;
    length = 0;
    return currToken = nextInternal();
  }

  
  private Tokens nextInternal() {
    if (offset >= bufferLen) return Tokens.EOF;
    char ch = c(offset);

    
    if (ch == ' ' || ch == '\t') {
      length = 1;
      while (offset + length < bufferLen) {
        char nx = c(offset + length);
        if (nx == ' ' || nx == '\t') length++;
        else break;
      }
      return Tokens.WHITESPACE;
    }

    
    if (ch == '\n') {
      length = 1;
      return Tokens.NEWLINE;
    }
    if (ch == '\r') {
      length = 1;
      if (offset + 1 < bufferLen && c(offset + 1) == '\n') length++;
      return Tokens.NEWLINE;
    }

    
    if (ch == '#') return scanHeading();

    
    if (ch == '>') {
      if (isAtLineStart()) {
        length = 1;
        return Tokens.BLOCKQUOTE_MARKER;
      }
      return scanText();
    }

    
    if (ch == '|') {
      length = 1;
      return Tokens.TABLE_PIPE;
    }

    
    if (ch == '\\' && offset + 1 < bufferLen) {
      length = 2;
      return Tokens.ESCAPE;
    }

    
    if (ch == '!' && offset + 1 < bufferLen && c(offset + 1) == '[') {
      length = 1;
      return Tokens.IMAGE_BANG;
    }

    
    if (ch == '[') return scanLinkText();

    
    if (ch == '(') {
      if (offset > 0 && c(offset - 1) == ']') return scanLinkUrl();
      return scanText();
    }

    
    if (ch == '`') return scanBacktick();

    
    if (ch == '~') return scanTilde();

    
    if (ch == '<') return scanAngle();

    
    if (ch == '*' || ch == '_' || ch == '-' || ch == '+') return scanEmphasisOrBlock(ch);

    
    if (ch >= '1' && ch <= '9') return scanOrderedList();

    
    return scanText();
  }

  
  
  

  /** True if everything before {@code offset} on this line is only spaces/tabs. */
  private boolean isAtLineStart() {
    for (int i = 0; i < offset; i++) {
      char x = c(i);
      if (x != ' ' && x != '\t') return false;
    }
    return true;
  }

  private Tokens scanHeading() {
    if (!isAtLineStart()) return scanText();

    int level = 0;
    while (offset + level < bufferLen && c(offset + level) == '#') level++;
    boolean valid =
        level <= 6
            && (offset + level >= bufferLen
                || c(offset + level) == ' '
                || c(offset + level) == '\t'
                || c(offset + level) == '\n'
                || c(offset + level) == '\r');
    if (!valid) return scanText();
    length = level;
    switch (level) {
      case 1:
        return Tokens.HEADING_H1;
      case 2:
        return Tokens.HEADING_H2;
      case 3:
        return Tokens.HEADING_H3;
      case 4:
        return Tokens.HEADING_H4;
      case 5:
        return Tokens.HEADING_H5;
      default:
        return Tokens.HEADING_H6;
    }
  }

  private Tokens scanOrderedList() {
    if (!isAtLineStart()) return scanText();

    int i = 1;
    while (offset + i < bufferLen && c(offset + i) >= '0' && c(offset + i) <= '9') i++;
    if (offset + i < bufferLen && (c(offset + i) == '.' || c(offset + i) == ')')) {
      i++;
      if (offset + i < bufferLen && (c(offset + i) == ' ' || c(offset + i) == '\t')) {
        length = i;
        return Tokens.ORDERED_LIST_MARKER;
      }
    }
    return scanText();
  }

  private Tokens scanEmphasisOrBlock(char ch) {
    
    int run = 0;
    while (offset + run < bufferLen && c(offset + run) == ch) run++;

    boolean lineStart = isAtLineStart();

    
    if (lineStart && (ch == '-' || ch == '*' || ch == '_') && isThematicBreak(ch)) {
      
      length = 0;
      while (offset + length < bufferLen
          && c(offset + length) != '\n'
          && c(offset + length) != '\r') length++;
      return Tokens.THEMATIC_BREAK;
    }

    
    if (lineStart
        && (ch == '-' || ch == '*' || ch == '+')
        && run == 1
        && offset + 1 < bufferLen
        && (c(offset + 1) == ' ' || c(offset + 1) == '\t')) {
      length = 1;
      return Tokens.UNORDERED_LIST_MARKER;
    }

    if (emphasisOpenChar == ch && emphasisOpenRun == run) {
      length = run;
      emphasisOpenChar = 0;
      emphasisOpenRun = 0;
      
      if (run >= 3) return Tokens.BOLD_ITALIC;
      if (run == 2) return Tokens.BOLD;
      return Tokens.ITALIC;
    }
  
    if (hasMatchingClose(ch, run)) {
      length = run;
      emphasisOpenChar = ch;
      emphasisOpenRun = run;
      if (run >= 3) return Tokens.BOLD_ITALIC;
      if (run == 2) return Tokens.BOLD;
      return Tokens.ITALIC;
    }

    
    length = run;
    return Tokens.TEXT;
  }

  private boolean hasMatchingClose(char ch, int run) {
    int i = offset + run;
    while (i < bufferLen) {
      char x = c(i);
      if (x == '\n' || x == '\r') return false;
      if (x == ch) {
        
        int closeRun = 0;
        int j = i;
        while (j < bufferLen && c(j) == ch) {
          closeRun++;
          j++;
        }
        if (closeRun == run) return true;
        i = j; 
      } else {
        i++;
      }
    }
    return false;
  }

  private boolean isThematicBreak(char ch) {
    int count = 0, i = offset;
    while (i < bufferLen) {
      char x = c(i);
      if (x == ch) {
        count++;
        i++;
      } else if (x == ' ' || x == '\t') {
        i++;
      } else if (x == '\n' || x == '\r') break;
      else return false;
    }
    return count >= 3;
  }

  private Tokens scanLinkText() {
    length = 1; 
    int depth = 1;
    while (offset + length < bufferLen) {
      char ch = c(offset + length);
      length++;
      if (ch == '[') depth++;
      else if (ch == ']') {
        depth--;
        if (depth == 0) return Tokens.LINK_TEXT;
      } else if (ch == '\n' || ch == '\r') break;
    }
    length = 1;
    return Tokens.TEXT;
  }

  private Tokens scanLinkUrl() {
    length = 1; 
    int depth = 1;
    while (offset + length < bufferLen) {
      char ch = c(offset + length);
      length++;
      if (ch == '(') depth++;
      else if (ch == ')') {
        depth--;
        if (depth == 0) return Tokens.LINK_URL;
      } else if (ch == '\n' || ch == '\r') break;
    }
    length = 1;
    return Tokens.TEXT;
  }

  /** ` or ``` */
  private Tokens scanBacktick() {
    int run = 0;
    while (offset + run < bufferLen && c(offset + run) == '`') run++;

    
    if (run >= 3 && offset == firstNonWhitespace()) {
      length = 0;
      while (offset + length < bufferLen
          && c(offset + length) != '\n'
          && c(offset + length) != '\r') length++;
      return Tokens.CODE_FENCE_OPEN;
    }

    
    length = run;
    while (offset + length < bufferLen) {
      if (c(offset + length) == '\n' || c(offset + length) == '\r') break;
      if (c(offset + length) == '`') {
        int closeRun = 0, pos = offset + length;
        while (pos + closeRun < bufferLen && c(pos + closeRun) == '`') closeRun++;
        if (closeRun == run) {
          length += closeRun;
          return Tokens.INLINE_CODE;
        }
      }
      length++;
    }
    length = run;
    return Tokens.TEXT;
  }

  /** ~~ strikethrough */
  private Tokens scanTilde() {
    if (offset + 1 < bufferLen && c(offset + 1) == '~') {
      length = 2;
      return Tokens.STRIKETHROUGH;
    }
    length = 1;
    return Tokens.TEXT;
  }

  private Tokens scanAngle() {
    length = 1;
    while (offset + length < bufferLen) {
      char ch = c(offset + length);
      length++;
      if (ch == '>') {
        String inner = source.subSequence(offset + 1, offset + length - 1).toString();
        if (inner.startsWith("http://")
            || inner.startsWith("https://")
            || inner.startsWith("content://")
            || inner.startsWith("mailto:")
            || inner.contains("@")) return Tokens.AUTO_LINK;
        return Tokens.HTML_INLINE;
      }
      if (ch == '\n' || ch == '\r') break;
    }
    length = 1;
    return Tokens.TEXT;
  }

  private Tokens scanText() {
    length = 0;
    while (offset + length < bufferLen) {
      char ch = c(offset + length);
      if (isSpecial(ch) || ch == '\n' || ch == '\r') break;
      length++;
    }
    if (length == 0) length = 1;
    return Tokens.TEXT;
  }
 
  private int firstNonWhitespace() {
    int i = 0;
    while (i < bufferLen && (c(i) == ' ' || c(i) == '\t')) i++;
    return i;
  }

  private static boolean isSpecial(char ch) {
    switch (ch) {
      case '#':
      case '>':
      case '-':
      case '*':
      case '+':
      case '_':
      case '`':
      case '~':
      case '!':
      case '[':
      case '(':
      case '<':
      case '\\':
      case '|':
        return true;
      default:
        return false;
    }
  }

  private char c(int pos) {
    return source.charAt(pos);
  }
}