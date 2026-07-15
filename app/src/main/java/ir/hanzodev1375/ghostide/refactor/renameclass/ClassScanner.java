package ir.hanzodev1375.ghostide.refactor.renameclass;

import ir.hanzodev1375.ghostide.refactor.rename.lang.ImportInfo;
import ir.hanzodev1375.ghostide.refactor.rename.lang.ImportScanner;
import ir.hanzodev1375.ghostide.refactor.rename.lang.JavaLexer;
import ir.hanzodev1375.ghostide.refactor.rename.lang.KotlinLexer;
import ir.hanzodev1375.ghostide.refactor.rename.lang.SourceRegion;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ClassScanner {

  private static final Set<String> EXCLUDED_DIRECTORY_NAMES =
      new HashSet<>(
          Arrays.asList("build", ".gradle", ".git", ".idea", "node_modules", ".cxx", "captures"));

  public ClassScanResult scan(File projectRoot, File targetFile, String oldClassName)
      throws IOException {
    boolean kotlin = targetFile.getName().endsWith(".kt");
    String content = new String(Files.readAllBytes(targetFile.toPath()), StandardCharsets.UTF_8);
    List<SourceRegion> regions = tokenize(content, kotlin);
    ImportInfo targetInfo = new ImportScanner().scan(content, regions);
    String packageName = targetInfo.getPackageName();
    String qualifiedName = packageName.isEmpty() ? oldClassName : packageName + "." + oldClassName;

    List<File> candidateFiles = new ArrayList<>();
    collectSourceFiles(projectRoot, candidateFiles);

    List<ClassFileTarget> targets = new ArrayList<>();
    List<File> ambiguousFiles = new ArrayList<>();
    targets.add(new ClassFileTarget(targetFile, true));

    for (File file : candidateFiles) {
      if (file.equals(targetFile)) {
        continue;
      }
      String fileContent;
      try {
        fileContent = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
      } catch (IOException exception) {
        continue;
      }
      if (!fileContent.contains(oldClassName)) {
        continue;
      }
      boolean isKotlinFile = file.getName().endsWith(".kt");
      List<SourceRegion> fileRegions = tokenize(fileContent, isKotlinFile);
      ImportInfo fileImportInfo = new ImportScanner().scan(fileContent, fileRegions);

      boolean hasExplicitImport =
          qualifiedName.equals(fileImportInfo.resolveExplicitImport(oldClassName));
      boolean hasConflictingImport =
          fileImportInfo.getSimpleNameToFullyQualified().containsKey(oldClassName)
              && !hasExplicitImport;
      boolean samePackage = fileImportInfo.getPackageName().equals(packageName);
      boolean containsQualifiedReference = fileContent.contains(qualifiedName);

      if (hasExplicitImport) {
        targets.add(new ClassFileTarget(file, true));
      } else if (hasConflictingImport) {
        if (containsQualifiedReference) {
          targets.add(new ClassFileTarget(file, false));
        }
      } else if (samePackage) {
        if (fileImportInfo.hasWildcardImport()) {
          ambiguousFiles.add(file);
          if (containsQualifiedReference) {
            targets.add(new ClassFileTarget(file, false));
          }
        } else {
          targets.add(new ClassFileTarget(file, true));
        }
      } else if (containsQualifiedReference) {
        targets.add(new ClassFileTarget(file, false));
      }
    }

    return new ClassScanResult(targetFile, kotlin, packageName, oldClassName, targets, ambiguousFiles);
  }

  private List<SourceRegion> tokenize(String content, boolean kotlin) {
    return kotlin ? new KotlinLexer().tokenize(content) : new JavaLexer().tokenize(content);
  }

  private void collectSourceFiles(File directory, List<File> result) {
    File[] children = directory.listFiles();
    if (children == null) {
      return;
    }
    for (File child : children) {
      if (child.isDirectory()) {
        if (EXCLUDED_DIRECTORY_NAMES.contains(child.getName())) {
          continue;
        }
        collectSourceFiles(child, result);
      } else {
        String name = child.getName();
        if (name.endsWith(".java") || name.endsWith(".kt")) {
          result.add(child);
        }
      }
    }
  }
}
