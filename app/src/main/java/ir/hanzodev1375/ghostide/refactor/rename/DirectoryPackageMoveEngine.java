package ir.hanzodev1375.ghostide.refactor.rename;

import ir.hanzodev1375.ghostide.refactor.rename.model.RenameProgress;
import ir.hanzodev1375.ghostide.refactor.rename.model.ScanResult;
import java.io.File;
import java.util.List;

public final class DirectoryPackageMoveEngine implements PackageMoveEngine {

  @Override
  public void move(
      ScanResult scanResult,
      String oldPackage,
      String newPackage,
      RollbackManager rollbackManager,
      ProgressReporter reporter,
      CancellationToken token)
      throws Exception {
    List<File> oldDirectories = scanResult.getOldPackageDirectories();
    int total = oldDirectories.size();
    String newRelativePath = newPackage.replace('.', File.separatorChar);

    for (int index = 0; index < total; index++) {
      token.throwIfCancelled();
      File oldDirectory = oldDirectories.get(index);
      reporter.report(
          new RenameProgress(
              RenameProgress.Phase.MOVING_FILES, index + 1, total, oldDirectory.getName()));
      File sourceRoot = findSourceRootAncestor(oldDirectory, scanResult);
      if (sourceRoot == null) {
        continue;
      }
      File newDirectory = new File(sourceRoot, newRelativePath);
      rollbackManager.moveDirectory(oldDirectory, newDirectory);
    }

    for (int index = 0; index < total; index++) {
      token.throwIfCancelled();
      File oldDirectory = oldDirectories.get(index);
      File sourceRoot = findSourceRootAncestor(oldDirectory, scanResult);
      if (sourceRoot == null) {
        continue;
      }
      reporter.report(
          new RenameProgress(RenameProgress.Phase.DELETING_EMPTY_DIRECTORIES, index + 1, total, null));
      deleteEmptyAncestors(oldDirectory.getParentFile(), sourceRoot, rollbackManager);
    }
  }

  private File findSourceRootAncestor(File directory, ScanResult scanResult) {
    for (File sourceRoot : scanResult.getSourceRoots()) {
      if (isAncestor(sourceRoot, directory)) {
        return sourceRoot;
      }
    }
    return null;
  }

  private boolean isAncestor(File ancestor, File descendant) {
    File current = descendant;
    while (current != null) {
      if (current.equals(ancestor)) {
        return true;
      }
      current = current.getParentFile();
    }
    return false;
  }

  private void deleteEmptyAncestors(File directory, File sourceRoot, RollbackManager rollbackManager)
      throws Exception {
    File current = directory;
    while (current != null && !current.equals(sourceRoot) && isAncestor(sourceRoot, current)) {
      File[] children = current.listFiles();
      if (children != null && children.length == 0) {
        File parent = current.getParentFile();
        rollbackManager.deleteEmptyDirectory(current);
        current = parent;
      } else {
        break;
      }
    }
  }
}
