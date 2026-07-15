package ir.hanzodev1375.ghostide.refactor.rename.lang;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ImportScanner {

  private static final Pattern PACKAGE_PATTERN =
      Pattern.compile("\\bpackage\\s+([a-zA-Z_][a-zA-Z0-9_.]*)");
  private static final Pattern IMPORT_PATTERN =
      Pattern.compile(
          "\\bimport\\s+(static\\s+)?([a-zA-Z_][a-zA-Z0-9_.]*)(\\.\\*)?(\\s+as\\s+([a-zA-Z_][a-zA-Z0-9_]*))?");

  public ImportInfo scan(String content, List<SourceRegion> regions) {
    String codeOnly = extractCodeOnly(content, regions);

    String packageName = "";
    Matcher packageMatcher = PACKAGE_PATTERN.matcher(codeOnly);
    if (packageMatcher.find()) {
      packageName = packageMatcher.group(1);
    }

    Map<String, String> simpleNameMap = new HashMap<>();
    boolean hasWildcard = false;
    Matcher importMatcher = IMPORT_PATTERN.matcher(codeOnly);
    while (importMatcher.find()) {
      String qualifiedName = importMatcher.group(2);
      boolean isWildcard = importMatcher.group(3) != null;
      String alias = importMatcher.group(5);
      if (isWildcard) {
        hasWildcard = true;
        continue;
      }
      int lastDot = qualifiedName.lastIndexOf('.');
      String simpleName = lastDot >= 0 ? qualifiedName.substring(lastDot + 1) : qualifiedName;
      String key = alias != null ? alias : simpleName;
      simpleNameMap.put(key, qualifiedName);
    }
    return new ImportInfo(packageName, simpleNameMap, hasWildcard);
  }

  private String extractCodeOnly(String content, List<SourceRegion> regions) {
    StringBuilder builder = new StringBuilder(content.length());
    for (SourceRegion region : regions) {
      if (region.getType() == SourceRegion.Type.CODE) {
        builder.append(content, region.getStart(), region.getEnd());
      } else {
        for (int i = region.getStart(); i < region.getEnd(); i++) {
          builder.append(' ');
        }
      }
    }
    return builder.toString();
  }
}
