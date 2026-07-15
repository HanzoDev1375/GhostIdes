package ir.hanzodev1375.ghostide.refactor.rename;

import ir.hanzodev1375.ghostide.refactor.rename.model.PreviewEntry;
import ir.hanzodev1375.ghostide.refactor.rename.model.PreviewResult;
import ir.hanzodev1375.ghostide.refactor.rename.model.ScanResult;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public final class PackagePreview {

  public PreviewResult build(ScanResult scanResult, String newPackage) {
    List<PreviewEntry> entries = new ArrayList<>();
    String oldPackage = scanResult.getOldPackage();
    int totalChanges = 0;

    int javaFilesAffected = 0;
    for (File file : scanResult.getJavaFiles()) {
      int count = countOccurrences(file, oldPackage);
      if (count > 0) {
        javaFilesAffected++;
        totalChanges += count;
        entries.add(new PreviewEntry("Java", describeChangeCount(count), file.getPath(), count));
      }
    }

    int kotlinFilesAffected = 0;
    for (File file : scanResult.getKotlinFiles()) {
      int count = countOccurrences(file, oldPackage);
      if (count > 0) {
        kotlinFilesAffected++;
        totalChanges += count;
        entries.add(new PreviewEntry("Kotlin", describeChangeCount(count), file.getPath(), count));
      }
    }

    boolean manifestAffected = false;
    for (File file : scanResult.getManifestFiles()) {
      int count = countOccurrences(file, oldPackage);
      if (count > 0) {
        manifestAffected = true;
        totalChanges += count;
        entries.add(
            new PreviewEntry("Manifest", describeChangeCount(count), file.getPath(), count));
      }
    }

    boolean gradleAffected = false;
    for (File file : scanResult.getGradleFiles()) {
      int count = countOccurrences(file, oldPackage);
      if (count > 0) {
        gradleAffected = true;
        totalChanges += count;
        entries.add(new PreviewEntry("Gradle", describeChangeCount(count), file.getPath(), count));
      }
    }

    int directoriesAffected = scanResult.getOldPackageDirectories().size();
    if (directoriesAffected > 0) {
      totalChanges += directoriesAffected;
      for (File directory : scanResult.getOldPackageDirectories()) {
        entries.add(new PreviewEntry("Directory", "Will be moved", directory.getPath(), 1));
      }
    }

    return new PreviewResult(
        oldPackage,
        newPackage,
        entries,
        javaFilesAffected,
        kotlinFilesAffected,
        directoriesAffected,
        manifestAffected,
        gradleAffected,
        totalChanges);
  }

  private String describeChangeCount(int count) {
    return count + (count == 1 ? " reference will be updated" : " references will be updated");
  }

  private int countOccurrences(File file, String needle) {
    try {
      String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
      int count = 0;
      int index = content.indexOf(needle);
      while (index >= 0) {
        count++;
        index = content.indexOf(needle, index + needle.length());
      }
      return count;
    } catch (IOException exception) {
      return 0;
    }
  }
}
