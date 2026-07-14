package ir.hanzodev1375.ghostide.refactor.rename;

import ir.hanzodev1375.ghostide.refactor.rename.model.ScanResult;

public interface PackageMoveEngine {
  void move(
      ScanResult scanResult,
      String oldPackage,
      String newPackage,
      RollbackManager rollbackManager,
      ProgressReporter reporter,
      CancellationToken token)
      throws Exception;
}
