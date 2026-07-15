package ir.hanzodev1375.ghostide.refactor.rename.lang;

import java.util.List;

public final class SimpleIdentifierRewriter {

  public static final class Result {

    private final String content;
    private final int changeCount;

    public Result(String content, int changeCount) {
      this.content = content;
      this.changeCount = changeCount;
    }

    public String getContent() {
      return content;
    }

    public int getChangeCount() {
      return changeCount;
    }
  }

  public Result rewrite(String content, List<SourceRegion> regions, String oldName, String newName) {
    StringBuilder builder = new StringBuilder(content.length());
    int changeCount = 0;
    for (SourceRegion region : regions) {
      if (region.getType() == SourceRegion.Type.CODE) {
        int i = region.getStart();
        int end = region.getEnd();
        while (i < end) {
          if (matchesIdentifier(content, i, end, oldName)) {
            builder.append(newName);
            i += oldName.length();
            changeCount++;
          } else {
            builder.append(content.charAt(i));
            i++;
          }
        }
      } else {
        builder.append(content, region.getStart(), region.getEnd());
      }
    }
    return new Result(builder.toString(), changeCount);
  }

  private boolean matchesIdentifier(String content, int index, int regionEnd, String oldName) {
    int length = oldName.length();
    if (index + length > regionEnd) {
      return false;
    }
    if (!content.regionMatches(index, oldName, 0, length)) {
      return false;
    }
    if (index > 0) {
      char before = content.charAt(index - 1);
      if (isIdentifierChar(before) || before == '.') {
        return false;
      }
    }
    int after = index + length;
    if (after < content.length() && isIdentifierChar(content.charAt(after))) {
      return false;
    }
    return true;
  }

  private boolean isIdentifierChar(char c) {
    return Character.isLetterOrDigit(c) || c == '_' || c == '$';
  }
}
