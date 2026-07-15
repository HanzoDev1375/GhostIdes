package ir.hanzodev1375.ghostide.refactor.renameclass;

import android.os.Handler;
import android.os.Looper;
import ir.hanzodev1375.ghostide.refactor.rename.BackgroundExecutor;
import ir.hanzodev1375.ghostide.refactor.rename.FileRenameNotifier;
import ir.hanzodev1375.ghostide.refactor.rename.ProgressReporter;
import ir.hanzodev1375.ghostide.refactor.rename.RollbackManager;
import ir.hanzodev1375.ghostide.refactor.rename.model.RenameProgress;
import ir.hanzodev1375.ghostide.refactor.rename.model.ValidationResult;
import java.io.File;

public final class ClassRenameManager {

  private final ClassScanner scanner;
  private final ClassNameValidator validator;
  private final ClassRenameEngine engine;
  private final BackgroundExecutor executor;
  private final Handler mainHandler;

  public ClassRenameManager() {
    this(new ClassScanner(), new ClassNameValidator(), new ClassRenameEngine(), new BackgroundExecutor());
  }

  public ClassRenameManager(
      ClassScanner scanner,
      ClassNameValidator validator,
      ClassRenameEngine engine,
      BackgroundExecutor executor) {
    this.scanner = scanner;
    this.validator = validator;
    this.engine = engine;
    this.executor = executor;
    this.mainHandler = new Handler(Looper.getMainLooper());
  }

  public void scan(
      File projectRoot, File targetFile, String oldClassName, ClassRenameCallback callback) {
    executor.execute(
        token -> {
          mainHandler.post(
              () ->
                  callback.onProgress(
                      new RenameProgress(
                          RenameProgress.Phase.SCANNING, 0, 1, targetFile.getName())));
          ClassScanResult result = scanner.scan(projectRoot, targetFile, oldClassName);
          token.throwIfCancelled();
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

  public ValidationResult validateNewClassName(
      String oldClassName, String newClassName, File targetFile) {
    return validator.validate(oldClassName, newClassName, targetFile);
  }

  public void execute(ClassScanResult scanResult, String newClassName, ClassRenameCallback callback) {
    executor.execute(
        token -> {
          ProgressReporter reporter = new ProgressReporter(callback::onProgress);
          RollbackManager rollbackManager = new RollbackManager();
          String oldPath = scanResult.getTargetFile().getAbsolutePath();
          try {
            File newFile = engine.apply(scanResult, newClassName, rollbackManager, reporter, token);
            rollbackManager.commit();
            FileRenameNotifier.getInstance().notifyRenamed(oldPath, newFile.getAbsolutePath());
            mainHandler.post(() -> callback.onSuccess(newFile));
          } catch (Exception exception) {
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
