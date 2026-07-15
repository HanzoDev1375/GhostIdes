package ir.hanzodev1375.ghostide.refactor.renameclass;

import java.util.Collections;
import java.util.List;

public final class ClassScanResult {

  private final java.io.File targetFile;
  private final boolean kotlin;
  private final String packageName;
  private final String oldClassName;
  private final List<ClassFileTarget> targets;
  private final List<java.io.File> ambiguousFiles;

  public ClassScanResult(
      java.io.File targetFile,
      boolean kotlin,
      String packageName,
      String oldClassName,
      List<ClassFileTarget> targets,
      List<java.io.File> ambiguousFiles) {
    this.targetFile = targetFile;
    this.kotlin = kotlin;
    this.packageName = packageName;
    this.oldClassName = oldClassName;
    this.targets = Collections.unmodifiableList(targets);
    this.ambiguousFiles = Collections.unmodifiableList(ambiguousFiles);
  }

  public java.io.File getTargetFile() {
    return targetFile;
  }

  public boolean isKotlin() {
    return kotlin;
  }

  public String getPackageName() {
    return packageName;
  }

  public String getOldClassName() {
    return oldClassName;
  }

  public List<ClassFileTarget> getTargets() {
    return targets;
  }

  public List<java.io.File> getAmbiguousFiles() {
    return ambiguousFiles;
  }
}
