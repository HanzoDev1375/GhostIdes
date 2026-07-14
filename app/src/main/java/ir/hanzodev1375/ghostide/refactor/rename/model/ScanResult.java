package ir.hanzodev1375.ghostide.refactor.rename.model;

import java.io.File;
import java.util.Collections;
import java.util.List;

public final class ScanResult {

  private final File projectRoot;
  private final String oldPackage;
  private final List<File> sourceRoots;
  private final List<File> javaFiles;
  private final List<File> kotlinFiles;
  private final List<File> manifestFiles;
  private final List<File> gradleFiles;
  private final List<File> oldPackageDirectories;

  public ScanResult(
      File projectRoot,
      String oldPackage,
      List<File> sourceRoots,
      List<File> javaFiles,
      List<File> kotlinFiles,
      List<File> manifestFiles,
      List<File> gradleFiles,
      List<File> oldPackageDirectories) {
    this.projectRoot = projectRoot;
    this.oldPackage = oldPackage;
    this.sourceRoots = Collections.unmodifiableList(sourceRoots);
    this.javaFiles = Collections.unmodifiableList(javaFiles);
    this.kotlinFiles = Collections.unmodifiableList(kotlinFiles);
    this.manifestFiles = Collections.unmodifiableList(manifestFiles);
    this.gradleFiles = Collections.unmodifiableList(gradleFiles);
    this.oldPackageDirectories = Collections.unmodifiableList(oldPackageDirectories);
  }

  public File getProjectRoot() {
    return projectRoot;
  }

  public String getOldPackage() {
    return oldPackage;
  }

  public List<File> getSourceRoots() {
    return sourceRoots;
  }

  public List<File> getJavaFiles() {
    return javaFiles;
  }

  public List<File> getKotlinFiles() {
    return kotlinFiles;
  }

  public List<File> getManifestFiles() {
    return manifestFiles;
  }

  public List<File> getGradleFiles() {
    return gradleFiles;
  }

  public List<File> getOldPackageDirectories() {
    return oldPackageDirectories;
  }

  public boolean oldPackageExistsOnDisk() {
    return !oldPackageDirectories.isEmpty();
  }

  public int getTotalCandidateFileCount() {
    return javaFiles.size() + kotlinFiles.size() + manifestFiles.size() + gradleFiles.size();
  }
}
