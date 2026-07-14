package ir.hanzodev1375.ghostide.refactor.rename;

import ir.hanzodev1375.ghostide.refactor.rename.model.ModuleType;
import ir.hanzodev1375.ghostide.refactor.rename.model.ScanResult;
import ir.hanzodev1375.ghostide.refactor.rename.model.ValidationResult;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public final class AndroidModuleValidator {

  private static final String[] APPLICATION_MARKERS = {
    "com.android.application", "android.application"
  };

  private static final String[] LIBRARY_MARKERS = {
    "com.android.library", "android.library"
  };

  private static final String[] DYNAMIC_FEATURE_MARKERS = {
    "com.android.dynamic-feature", "android.dynamic-feature"
  };

  public ValidationResult validateStructure(ScanResult scanResult) {
    List<String> errors = new ArrayList<>();

    if (scanResult == null) {
      errors.add("Nothing was scanned.");
      return ValidationResult.invalid(errors);
    }

    File projectRoot = scanResult.getProjectRoot();
    if (projectRoot == null || !projectRoot.isDirectory()) {
      errors.add("The selected path does not exist or is not a directory.");
      return ValidationResult.invalid(errors);
    }

    if (scanResult.getManifestFiles().isEmpty()) {
      errors.add(
          "No AndroidManifest.xml was found under \""
              + projectRoot.getPath()
              + "\". This does not look like an Android project or module.");
    }

    File androidGradleFile = findAndroidGradleFile(scanResult);
    if (androidGradleFile == null) {
      errors.add(
          "No Gradle build file with the Android application, library or dynamic-feature"
              + " plugin applied was found in this module.");
    }

    if (!scanResult.oldPackageExistsOnDisk()) {
      errors.add(
          "Package \""
              + scanResult.getOldPackage()
              + "\" was not found in any source root of this project.");
    }

    if (errors.isEmpty()) {
      return ValidationResult.valid();
    }
    return ValidationResult.invalid(errors);
  }

  public ModuleType detectModuleType(ScanResult scanResult) {
    File androidGradleFile = findAndroidGradleFile(scanResult);
    if (androidGradleFile == null) {
      return ModuleType.UNKNOWN;
    }
    String content = readQuietly(androidGradleFile);
    if (content == null) {
      return ModuleType.UNKNOWN;
    }
    if (containsAny(content, APPLICATION_MARKERS)) {
      return ModuleType.APPLICATION;
    }
    if (containsAny(content, DYNAMIC_FEATURE_MARKERS)) {
      return ModuleType.DYNAMIC_FEATURE;
    }
    if (containsAny(content, LIBRARY_MARKERS)) {
      return ModuleType.LIBRARY;
    }
    return ModuleType.UNKNOWN;
  }

  private File findAndroidGradleFile(ScanResult scanResult) {
    if (scanResult == null) {
      return null;
    }
    for (File gradleFile : scanResult.getGradleFiles()) {
      String content = readQuietly(gradleFile);
      if (content == null) {
        continue;
      }
      if (containsAny(content, APPLICATION_MARKERS)
          || containsAny(content, LIBRARY_MARKERS)
          || containsAny(content, DYNAMIC_FEATURE_MARKERS)) {
        return gradleFile;
      }
    }
    return null;
  }

  private boolean containsAny(String content, String[] markers) {
    for (String marker : markers) {
      if (content.contains(marker)) {
        return true;
      }
    }
    return false;
  }

  private String readQuietly(File file) {
    try {
      return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    } catch (IOException exception) {
      return null;
    }
  }
}
