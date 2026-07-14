package ir.hanzodev1375.ghostide.refactor.rename.model;

import java.util.Collections;
import java.util.List;

public final class PreviewResult {

  private final String oldPackage;
  private final String newPackage;
  private final List<PreviewEntry> entries;
  private final int javaFilesAffected;
  private final int kotlinFilesAffected;
  private final int directoriesAffected;
  private final boolean manifestAffected;
  private final boolean gradleAffected;
  private final int totalChanges;

  public PreviewResult(
      String oldPackage,
      String newPackage,
      List<PreviewEntry> entries,
      int javaFilesAffected,
      int kotlinFilesAffected,
      int directoriesAffected,
      boolean manifestAffected,
      boolean gradleAffected,
      int totalChanges) {
    this.oldPackage = oldPackage;
    this.newPackage = newPackage;
    this.entries = Collections.unmodifiableList(entries);
    this.javaFilesAffected = javaFilesAffected;
    this.kotlinFilesAffected = kotlinFilesAffected;
    this.directoriesAffected = directoriesAffected;
    this.manifestAffected = manifestAffected;
    this.gradleAffected = gradleAffected;
    this.totalChanges = totalChanges;
  }

  public String getOldPackage() {
    return oldPackage;
  }

  public String getNewPackage() {
    return newPackage;
  }

  public List<PreviewEntry> getEntries() {
    return entries;
  }

  public int getJavaFilesAffected() {
    return javaFilesAffected;
  }

  public int getKotlinFilesAffected() {
    return kotlinFilesAffected;
  }

  public int getDirectoriesAffected() {
    return directoriesAffected;
  }

  public boolean isManifestAffected() {
    return manifestAffected;
  }

  public boolean isGradleAffected() {
    return gradleAffected;
  }

  public int getTotalChanges() {
    return totalChanges;
  }
}
