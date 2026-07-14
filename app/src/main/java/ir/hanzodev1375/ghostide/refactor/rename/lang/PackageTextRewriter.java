package ir.hanzodev1375.ghostide.refactor.rename.lang;

import java.util.List;

public final class PackageTextRewriter {

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

  public Result rewrite(
      String content, List<SourceRegion> regions, String oldPackage, String newPackage) {
    StringBuilder builder = new StringBuilder(content.length());
    int changeCount = 0;
    for (SourceRegion region : regions) {
      if (region.getType() == SourceRegion.Type.CODE) {
        int i = region.getStart();
        int end = region.getEnd();
        while (i < end) {
          if (matchesQualifiedName(content, i, end, oldPackage)) {
            builder.append(newPackage);
            i += oldPackage.length();
            changeCount++;
          } else {
            builder.append(content.charAt(i));
            i++;
          }
        }
      } else if (region.getType() == SourceRegion.Type.STRING) {
        int start = region.getStart();
        int end = region.getEnd();
        if (end - start >= 2 && content.charAt(end - 1) == '"') {
          String literal = content.substring(start + 1, end - 1);
          if (isQualifiedPrefixMatch(literal, oldPackage)) {
            builder.append('"');
            builder.append(newPackage);
            builder.append(literal, oldPackage.length(), literal.length());
            builder.append('"');
            changeCount++;
          } else {
            builder.append(content, start, end);
          }
        } else {
          builder.append(content, start, end);
        }
      } else {
        builder.append(content, region.getStart(), region.getEnd());
      }
    }
    return new Result(builder.toString(), changeCount);
  }

  private boolean matchesQualifiedName(String content, int index, int regionEnd, String oldPackage) {
    int packageLength = oldPackage.length();
    if (index + packageLength > regionEnd) {
      return false;
    }
    if (!content.regionMatches(index, oldPackage, 0, packageLength)) {
      return false;
    }
    if (index > 0) {
      char before = content.charAt(index - 1);
      if (isIdentifierChar(before) || before == '.') {
        return false;
      }
    }
    int after = index + packageLength;
    if (after < content.length() && isIdentifierChar(content.charAt(after))) {
      return false;
    }
    return true;
  }

  private boolean isQualifiedPrefixMatch(String literal, String oldPackage) {
    if (!literal.startsWith(oldPackage)) {
      return false;
    }
    if (literal.length() == oldPackage.length()) {
      return true;
    }
    return literal.charAt(oldPackage.length()) == '.';
  }

  private boolean isIdentifierChar(char c) {
    return Character.isLetterOrDigit(c) || c == '_' || c == '$';
  }
}
