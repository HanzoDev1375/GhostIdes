package ir.hanzodev1375.ghostide.refactor.rename.lang;

import java.util.ArrayList;
import java.util.List;

public final class KotlinLexer {

  public List<SourceRegion> tokenize(String content) {
    List<SourceRegion> regions = new ArrayList<>();
    int length = content.length();
    int i = 0;
    int codeStart = 0;
    while (i < length) {
      char c = content.charAt(i);
      if (c == '/' && i + 1 < length && content.charAt(i + 1) == '/') {
        flushCode(regions, codeStart, i);
        int start = i;
        i += 2;
        while (i < length && content.charAt(i) != '\n') {
          i++;
        }
        regions.add(new SourceRegion(SourceRegion.Type.LINE_COMMENT, start, i));
        codeStart = i;
      } else if (c == '/' && i + 1 < length && content.charAt(i + 1) == '*') {
        flushCode(regions, codeStart, i);
        int start = i;
        i = skipNestedBlockComment(content, i);
        regions.add(new SourceRegion(SourceRegion.Type.BLOCK_COMMENT, start, i));
        codeStart = i;
      } else if (isTripleQuote(content, i)) {
        flushCode(regions, codeStart, i);
        int start = i;
        i += 3;
        while (i < length && !isTripleQuote(content, i)) {
          i++;
        }
        i = Math.min(i + 3, length);
        regions.add(new SourceRegion(SourceRegion.Type.TEXT_BLOCK, start, i));
        codeStart = i;
      } else if (c == '"') {
        flushCode(regions, codeStart, i);
        int start = i;
        i++;
        while (i < length && content.charAt(i) != '"' && content.charAt(i) != '\n') {
          if (content.charAt(i) == '\\' && i + 1 < length) {
            i += 2;
          } else {
            i++;
          }
        }
        i = Math.min(i + 1, length);
        regions.add(new SourceRegion(SourceRegion.Type.STRING, start, i));
        codeStart = i;
      } else if (c == '\'') {
        flushCode(regions, codeStart, i);
        int start = i;
        i++;
        while (i < length && content.charAt(i) != '\'' && content.charAt(i) != '\n') {
          if (content.charAt(i) == '\\' && i + 1 < length) {
            i += 2;
          } else {
            i++;
          }
        }
        i = Math.min(i + 1, length);
        regions.add(new SourceRegion(SourceRegion.Type.CHAR, start, i));
        codeStart = i;
      } else {
        i++;
      }
    }
    flushCode(regions, codeStart, length);
    return regions;
  }

  private int skipNestedBlockComment(String content, int start) {
    int length = content.length();
    int i = start + 2;
    int depth = 1;
    while (i < length && depth > 0) {
      if (content.charAt(i) == '/' && i + 1 < length && content.charAt(i + 1) == '*') {
        depth++;
        i += 2;
      } else if (content.charAt(i) == '*' && i + 1 < length && content.charAt(i + 1) == '/') {
        depth--;
        i += 2;
      } else {
        i++;
      }
    }
    return i;
  }

  private void flushCode(List<SourceRegion> regions, int start, int end) {
    if (end > start) {
      regions.add(new SourceRegion(SourceRegion.Type.CODE, start, end));
    }
  }

  private boolean isTripleQuote(String content, int index) {
    return index + 2 < content.length()
        && content.charAt(index) == '"'
        && content.charAt(index + 1) == '"'
        && content.charAt(index + 2) == '"';
  }
}
