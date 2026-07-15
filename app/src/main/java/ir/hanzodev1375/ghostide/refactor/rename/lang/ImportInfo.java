package ir.hanzodev1375.ghostide.refactor.rename.lang;

import java.util.Collections;
import java.util.Map;

public final class ImportInfo {

  private final String packageName;
  private final Map<String, String> simpleNameToFullyQualified;
  private final boolean hasWildcardImport;

  public ImportInfo(
      String packageName, Map<String, String> simpleNameToFullyQualified, boolean hasWildcardImport) {
    this.packageName = packageName;
    this.simpleNameToFullyQualified = Collections.unmodifiableMap(simpleNameToFullyQualified);
    this.hasWildcardImport = hasWildcardImport;
  }

  public String getPackageName() {
    return packageName;
  }

  public Map<String, String> getSimpleNameToFullyQualified() {
    return simpleNameToFullyQualified;
  }

  public boolean hasWildcardImport() {
    return hasWildcardImport;
  }

  public String resolveExplicitImport(String simpleName) {
    return simpleNameToFullyQualified.get(simpleName);
  }
}
