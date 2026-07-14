package ir.hanzodev1375.ghostide.refactor.rename;

import android.os.Handler;
import android.os.Looper;
import ir.hanzodev1375.ghostide.refactor.rename.model.PreviewResult;
import ir.hanzodev1375.ghostide.refactor.rename.model.RenameCallback;
import ir.hanzodev1375.ghostide.refactor.rename.model.RenameProgress;
import ir.hanzodev1375.ghostide.refactor.rename.model.ScanResult;
import ir.hanzodev1375.ghostide.refactor.rename.model.ValidationResult;
import java.io.File;

public final class PackageRenameManager {

  private final PackageScanner scanner;
  private final PackageValidator validator;
  private final AndroidModuleValidator moduleValidator;
  private final PackagePreview preview;
  private final BackgroundExecutor executor;
  private final PackageRefactorEngine refactorEngine;
  private final PackageMoveEngine moveEngine;
  private final Handler mainHandler;

  public PackageRenameManager() {
    this(
        new PackageScanner(),
        new PackageValidator(),
        new AndroidModuleValidator(),
        new PackagePreview(),
        new BackgroundExecutor(),
        new CompositePackageRefactorEngine(),
        new DirectoryPackageMoveEngine());
  }

  public PackageRenameManager(
      PackageScanner scanner,
      PackageValidator validator,
      AndroidModuleValidator moduleValidator,
      PackagePreview preview,
      BackgroundExecutor executor,
      PackageRefactorEngine refactorEngine,
      PackageMoveEngine moveEngine) {
    this.scanner = scanner;
    this.validator = validator;
    this.moduleValidator = moduleValidator;
    this.preview = preview;
    this.executor = executor;
    this.refactorEngine = refactorEngine;
    this.moveEngine = moveEngine;
    this.mainHandler = new Handler(Looper.getMainLooper());
  }

  public void startScan(File moduleRoot, String oldPackage, RenameCallback callback) {
    executor.execute(
        token -> {
          mainHandler.post(
              () ->
                  callback.onProgress(
                      new RenameProgress(
                          RenameProgress.Phase.SCANNING, 0, 1, moduleRoot.getName())));
          ScanResult result = scanner.scan(moduleRoot, oldPackage);
          token.throwIfCancelled();
          mainHandler.post(
              () ->
                  callback.onProgress(
                      new RenameProgress(RenameProgress.Phase.VALIDATING, 1, 1, null)));
          ValidationResult structure = moduleValidator.validateStructure(result);
          token.throwIfCancelled();
          if (!structure.isValid()) {
            mainHandler.post(() -> callback.onError(structure.getFirstError(), null));
            return;
          }
          mainHandler.post(() -> callback.onScanCompleted(result));
        },
        new BackgroundExecutor.TaskResultCallback() {
          @Override
          public void onCompleted() {}

          @Override
          public void onFailed(Exception exception) {
            mainHandler.post(() -> callback.onError(describeError(exception), exception));
          }

          @Override
          public void onCancelled() {
            mainHandler.post(callback::onCancelled);
          }
        });
  }

  public ValidationResult validateNewPackageName(
      String oldPackage, String newPackage, ScanResult scanResult) {
    return validator.validateAgainstProject(oldPackage, newPackage, scanResult);
  }

  public void buildPreview(ScanResult scanResult, String newPackage, RenameCallback callback) {
    executor.execute(
        token -> {
          mainHandler.post(
              () ->
                  callback.onProgress(
                      new RenameProgress(RenameProgress.Phase.BUILDING_PREVIEW, 0, 1, null)));
          PreviewResult result = preview.build(scanResult, newPackage);
          token.throwIfCancelled();
          mainHandler.post(() -> callback.onPreviewReady(result));
        },
        new BackgroundExecutor.TaskResultCallback() {
          @Override
          public void onCompleted() {}

          @Override
          public void onFailed(Exception exception) {
            mainHandler.post(() -> callback.onError(describeError(exception), exception));
          }

          @Override
          public void onCancelled() {
            mainHandler.post(callback::onCancelled);
          }
        });
  }

  public void execute(ScanResult scanResult, String newPackage, RenameCallback callback) {
    if (refactorEngine == null || moveEngine == null) {
      mainHandler.post(
          () ->
              callback.onError(
                  "The safe rewrite engine for this rename is not installed in this build yet."
                      + " Nothing on disk has been changed.",
                  null));
      return;
    }
    executor.execute(
        token -> {
          ProgressReporter reporter = new ProgressReporter(callback::onProgress);
          RollbackManager rollbackManager = new RollbackManager();
          reporter.report(new RenameProgress(RenameProgress.Phase.BACKING_UP, 0, 1, null));
          try {
            refactorEngine.apply(
                scanResult, scanResult.getOldPackage(), newPackage, rollbackManager, reporter, token);
            token.throwIfCancelled();
            moveEngine.move(
                scanResult, scanResult.getOldPackage(), newPackage, rollbackManager, reporter, token);
            token.throwIfCancelled();
            rollbackManager.commit();
            reporter.report(new RenameProgress(RenameProgress.Phase.COMPLETED, 1, 1, null));
            PreviewResult finalResult = preview.build(scanResult, newPackage);
            mainHandler.post(() -> callback.onSuccess(finalResult));
          } catch (Exception exception) {
            reporter.report(new RenameProgress(RenameProgress.Phase.ROLLING_BACK, 0, 1, null));
            rollbackManager.rollbackAll();
            throw exception;
          }
        },
        new BackgroundExecutor.TaskResultCallback() {
          @Override
          public void onCompleted() {}

          @Override
          public void onFailed(Exception exception) {
            mainHandler.post(() -> callback.onError(describeError(exception), exception));
          }

          @Override
          public void onCancelled() {
            mainHandler.post(callback::onCancelled);
          }
        });
  }

  public void cancelActive() {
    executor.cancelActive();
  }

  public void shutdown() {
    executor.shutdown();
  }

  private String describeError(Exception exception) {
    String message = exception.getMessage();
    return message != null ? message : exception.getClass().getSimpleName();
  }
}
