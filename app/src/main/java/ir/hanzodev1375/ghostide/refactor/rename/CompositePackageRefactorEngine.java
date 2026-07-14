package ir.hanzodev1375.ghostide.refactor.rename;

import ir.hanzodev1375.ghostide.refactor.rename.lang.JavaRefactor;
import ir.hanzodev1375.ghostide.refactor.rename.lang.KotlinRefactor;
import ir.hanzodev1375.ghostide.refactor.rename.model.ModuleType;
import ir.hanzodev1375.ghostide.refactor.rename.model.ScanResult;

public final class CompositePackageRefactorEngine implements PackageRefactorEngine {

  private final JavaRefactor javaRefactor;
  private final KotlinRefactor kotlinRefactor;
  private final ManifestRefactor manifestRefactor;
  private final GradleRefactor gradleRefactor;
  private final AndroidModuleValidator moduleValidator;

  public CompositePackageRefactorEngine() {
    this(
        new JavaRefactor(),
        new KotlinRefactor(),
        new ManifestRefactor(),
        new GradleRefactor(),
        new AndroidModuleValidator());
  }

  public CompositePackageRefactorEngine(
      JavaRefactor javaRefactor,
      KotlinRefactor kotlinRefactor,
      ManifestRefactor manifestRefactor,
      GradleRefactor gradleRefactor,
      AndroidModuleValidator moduleValidator) {
    this.javaRefactor = javaRefactor;
    this.kotlinRefactor = kotlinRefactor;
    this.manifestRefactor = manifestRefactor;
    this.gradleRefactor = gradleRefactor;
    this.moduleValidator = moduleValidator;
  }

  @Override
  public void apply(
      ScanResult scanResult,
      String oldPackage,
      String newPackage,
      RollbackManager rollbackManager,
      ProgressReporter reporter,
      CancellationToken token)
      throws Exception {
    javaRefactor.rewrite(
        scanResult.getJavaFiles(), oldPackage, newPackage, rollbackManager, reporter, token);
    token.throwIfCancelled();
    kotlinRefactor.rewrite(
        scanResult.getKotlinFiles(), oldPackage, newPackage, rollbackManager, reporter, token);
    token.throwIfCancelled();
    manifestRefactor.rewrite(
        scanResult.getManifestFiles(), oldPackage, newPackage, rollbackManager, reporter, token);
    token.throwIfCancelled();
    ModuleType moduleType = moduleValidator.detectModuleType(scanResult);
    gradleRefactor.rewrite(
        scanResult.getGradleFiles(),
        oldPackage,
        newPackage,
        moduleType,
        rollbackManager,
        reporter,
        token);
  }
}
