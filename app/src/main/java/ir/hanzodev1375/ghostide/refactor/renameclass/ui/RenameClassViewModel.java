package ir.hanzodev1375.ghostide.refactor.renameclass.ui;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import ir.hanzodev1375.ghostide.refactor.rename.model.RenameProgress;
import ir.hanzodev1375.ghostide.refactor.rename.model.ValidationResult;
import ir.hanzodev1375.ghostide.refactor.renameclass.ClassRenameCallback;
import ir.hanzodev1375.ghostide.refactor.renameclass.ClassRenameManager;
import ir.hanzodev1375.ghostide.refactor.renameclass.ClassScanResult;
import java.io.File;

public final class RenameClassViewModel extends AndroidViewModel {

  public enum UiState {
    IDLE,
    SCANNING,
    PREVIEW_READY,
    CONFIRMING,
    EXECUTING,
    SUCCESS,
    ERROR,
    CANCELLED
  }

  private final ClassRenameManager manager;

  private final MutableLiveData<UiState> uiState = new MutableLiveData<>(UiState.IDLE);
  private final MutableLiveData<ValidationResult> inputValidation = new MutableLiveData<>();
  private final MutableLiveData<ClassScanResult> scanResultLive = new MutableLiveData<>();
  private final MutableLiveData<RenameProgress> progress = new MutableLiveData<>();
  private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
  private final MutableLiveData<String> newClassNameText = new MutableLiveData<>("");
  private final MutableLiveData<File> renamedFile = new MutableLiveData<>();

  private ClassScanResult scanResult;
  private File targetFile;
  private String oldClassName = "";

  private final ClassRenameCallback scanCallback =
      new ClassRenameCallback() {
        @Override
        public void onScanCompleted(ClassScanResult result) {
          scanResult = result;
          scanResultLive.setValue(result);
          uiState.setValue(UiState.PREVIEW_READY);
        }

        @Override
        public void onProgress(RenameProgress renameProgress) {
          progress.setValue(renameProgress);
        }

        @Override
        public void onSuccess(File newFile) {}

        @Override
        public void onError(String message, Exception cause) {
          errorMessage.setValue(message);
          uiState.setValue(UiState.ERROR);
        }

        @Override
        public void onCancelled() {
          uiState.setValue(UiState.CANCELLED);
        }
      };

  private final ClassRenameCallback executeCallback =
      new ClassRenameCallback() {
        @Override
        public void onScanCompleted(ClassScanResult result) {}

        @Override
        public void onProgress(RenameProgress renameProgress) {
          progress.setValue(renameProgress);
        }

        @Override
        public void onSuccess(File newFile) {
          renamedFile.setValue(newFile);
          uiState.setValue(UiState.SUCCESS);
        }

        @Override
        public void onError(String message, Exception cause) {
          errorMessage.setValue(message);
          uiState.setValue(UiState.ERROR);
        }

        @Override
        public void onCancelled() {
          uiState.setValue(UiState.CANCELLED);
        }
      };

  public RenameClassViewModel(@NonNull Application application) {
    super(application);
    this.manager = new ClassRenameManager();
  }

  public LiveData<UiState> getUiState() {
    return uiState;
  }

  public LiveData<ValidationResult> getInputValidation() {
    return inputValidation;
  }

  public LiveData<ClassScanResult> getScanResult() {
    return scanResultLive;
  }

  public LiveData<RenameProgress> getProgress() {
    return progress;
  }

  public LiveData<String> getErrorMessage() {
    return errorMessage;
  }

  public LiveData<File> getRenamedFile() {
    return renamedFile;
  }

  public String getOldClassName() {
    return oldClassName;
  }

  public void start(String projectRootPath, String targetFilePath, String className) {
    if (scanResult != null && oldClassName.equals(className)) {
      return;
    }
    File projectRoot = new File(projectRootPath);
    this.targetFile = new File(targetFilePath);
    this.oldClassName = className;
    uiState.setValue(UiState.SCANNING);
    manager.scan(projectRoot, targetFile, className, scanCallback);
  }

  public void onNewClassNameChanged(String text) {
    newClassNameText.setValue(text);
    if (targetFile == null) {
      return;
    }
    ValidationResult result = manager.validateNewClassName(oldClassName, text, targetFile);
    inputValidation.setValue(result);
  }

  public boolean canConfirmRename() {
    ValidationResult validation = inputValidation.getValue();
    return validation != null && validation.isValid() && scanResult != null;
  }

  public void requestRename() {
    if (canConfirmRename()) {
      uiState.setValue(UiState.CONFIRMING);
    }
  }

  public void cancelConfirmation() {
    if (uiState.getValue() == UiState.CONFIRMING) {
      uiState.setValue(UiState.PREVIEW_READY);
    }
  }

  public void confirmRename() {
    if (scanResult == null) {
      return;
    }
    String target = newClassNameText.getValue();
    if (target == null || target.isEmpty()) {
      return;
    }
    uiState.setValue(UiState.EXECUTING);
    manager.execute(scanResult, target, executeCallback);
  }

  public void cancelActiveOperation() {
    manager.cancelActive();
  }

  @Override
  protected void onCleared() {
    super.onCleared();
    manager.shutdown();
  }
}
