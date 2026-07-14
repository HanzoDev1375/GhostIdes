package ir.hanzodev1375.ghostide.refactor.rename;

import ir.hanzodev1375.ghostide.refactor.rename.model.ScanResult;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PackageScanner {

  private static final Set<String> EXCLUDED_DIRECTORY_NAMES =
      new HashSet<>(
          Arrays.asList(
              "build", ".gradle", ".git", ".idea", "node_modules", ".cxx", "captures"));

  private static final Set<String> SOURCE_ROOT_DIRECTORY_NAMES =
      new HashSet<>(Arrays.asList("java", "kotlin"));

  public ScanResult scan(File projectRoot, String oldPackage) {
    List<File> sourceRoots = new ArrayList<>();
    List<File> javaFiles = new ArrayList<>();
    List<File> kotlinFiles = new ArrayList<>();
    List<File> manifestFiles = new ArrayList<>();
    List<File> gradleFiles = new ArrayList<>();

    if (projectRoot != null && projectRoot.isDirectory()) {
      collectFiles(projectRoot, sourceRoots, javaFiles, kotlinFiles, manifestFiles, gradleFiles);
    }

    List<File> oldPackageDirectories = new ArrayList<>();
    String relativePath = oldPackage.replace('.', File.separatorChar);
    for (File sourceRoot : sourceRoots) {
      File candidate = new File(sourceRoot, relativePath);
      if (candidate.isDirectory()) {
        oldPackageDirectories.add(candidate);
      }
    }

    List<File> filteredJavaFiles = filterByPackageReference(javaFiles, oldPackage);
    List<File> filteredKotlinFiles = filterByPackageReference(kotlinFiles, oldPackage);

    return new ScanResult(
        projectRoot,
        oldPackage,
        sourceRoots,
        filteredJavaFiles,
        filteredKotlinFiles,
        manifestFiles,
        gradleFiles,
        oldPackageDirectories);
  }

  private void collectFiles(
      File directory,
      List<File> sourceRoots,
      List<File> javaFiles,
      List<File> kotlinFiles,
      List<File> manifestFiles,
      List<File> gradleFiles) {
    File[] children = directory.listFiles();
    if (children == null) {
      return;
    }
    for (File child : children) {
      String name = child.getName();
      if (child.isDirectory()) {
        if (EXCLUDED_DIRECTORY_NAMES.contains(name)) {
          continue;
        }
        if (SOURCE_ROOT_DIRECTORY_NAMES.contains(name) && isUnderSourceSet(child)) {
          sourceRoots.add(child);
        }
        collectFiles(child, sourceRoots, javaFiles, kotlinFiles, manifestFiles, gradleFiles);
      } else {
        if (name.endsWith(".java")) {
          javaFiles.add(child);
        } else if (name.endsWith(".kt")) {
          kotlinFiles.add(child);
        } else if (name.equals("AndroidManifest.xml")) {
          manifestFiles.add(child);
        } else if (name.equals("build.gradle") || name.equals("build.gradle.kts")) {
          gradleFiles.add(child);
        }
      }
    }
  }

  private boolean isUnderSourceSet(File directory) {
    File parent = directory.getParentFile();
    if (parent == null) {
      return false;
    }
    File grandParent = parent.getParentFile();
    return grandParent != null && grandParent.getName().equals("src");
  }

  private List<File> filterByPackageReference(List<File> files, String packageName) {
    List<File> result = new ArrayList<>();
    for (File file : files) {
      if (mightReferencePackage(file, packageName)) {
        result.add(file);
      }
    }
    return result;
  }

  private boolean mightReferencePackage(File file, String packageName) {
    try {
      String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
      return content.contains(packageName);
    } catch (IOException exception) {
      return true;
    }
  }
}
