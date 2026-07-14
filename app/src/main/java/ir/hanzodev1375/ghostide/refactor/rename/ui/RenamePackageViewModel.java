package ir.hanzodev1375.ghostide.refactor.rename.ui;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import ir.hanzodev1375.ghostide.refactor.rename.PackageRenameManager;
import ir.hanzodev1375.ghostide.refactor.rename.model.PreviewResult;
import ir.hanzodev1375.ghostide.refactor.rename.model.RenameCallback;
import ir.hanzodev1375.ghostide.refactor.rename.model.RenameProgress;
import ir.hanzodev1375.ghostide.refactor.rename.model.ScanResult;
import ir.hanzodev1375.ghostide.refactor.rename.model.ValidationResult;
import java.io.File;

public final class RenamePackageViewModel extends AndroidViewModel {

  public enum UiState {
    IDLE,
    SCANNING,
    INVALID_STRUCTURE,
    READY,
    VALIDATING_INPUT,
    BUILDING_PREVIEW,
    PREVIEW_READY,
    CONFIRMING,
    EXECUTING,
    SUCCESS,
    ERROR,
    CANCELLED
  }

  private static final long PREVIEW_DEBOUNCE_MS = 350L;

  private final PackageRenameManager manager;
  private final Handler debounceHandler;
  private final Runnable previewRunnable = this::runPreviewNow;

  private final MutableLiveData<UiState> uiState = new MutableLiveData<>(UiState.IDLE);
  private final MutableLiveData<ValidationResult> inputValidation = new MutableLiveData<>();
  private final MutableLiveData<PreviewResult> previewResult = new MutableLiveData<>();
  private final MutableLiveData<RenameProgress> progress = new MutableLiveData<>();
  private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
  private final MutableLiveData<String> newPackageText = new MutableLiveData<>("");

  private final RenameCallback scanCallback =
      new RenameCallback() {
        @Override
        public void onScanCompleted(ScanResult result) {
          scanResult = result;
          uiState.setValue(UiState.READY);
        }

        @Override
        public void onPreviewReady(PreviewResult result) {}

        @Override
        public void onProgress(RenameProgress renameProgress) {
          progress.setValue(renameProgress);
        }

        @Override
        public void onSuccess(PreviewResult result) {}

        @Override
        public void onError(String message, Exception cause) {
          errorMessage.setValue(message);
          uiState.setValue(UiState.INVALID_STRUCTURE);
        }

        @Override
        public void onCancelled() {
          uiState.setValue(UiState.CANCELLED);
        }
      };

  private final RenameCallback previewCallback =
      new RenameCallback() {
        @Override
        public void onScanCompleted(ScanResult result) {}

        @Override
        public void onPreviewReady(PreviewResult result) {
          previewResult.setValue(result);
          uiState.setValue(UiState.PREVIEW_READY);
        }

        @Override
        public void onProgress(RenameProgress renameProgress) {
          progress.setValue(renameProgress);
        }

        @Override
        public void onSuccess(PreviewResult result) {}

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

  private final RenameCallback executeCallback =
      new RenameCallback() {
        @Override
        public void onScanCompleted(ScanResult result) {}

        @Override
        public void onPreviewReady(PreviewResult result) {}

        @Override
        public void onProgress(RenameProgress renameProgress) {
          progress.setValue(renameProgress);
        }

        @Override
        public void onSuccess(PreviewResult result) {
          previewResult.setValue(result);
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

  private ScanResult scanResult;
  private String oldPackage = "";
  private String pendingPreviewPackage;

  public RenamePackageViewModel(@NonNull Application application) {
    super(application);
    this.manager = new PackageRenameManager();
    this.debounceHandler = new Handler(Looper.getMainLooper());
  }

  public LiveData<UiState> getUiState() {
    return uiState;
  }

  public LiveData<ValidationResult> getInputValidation() {
    return inputValidation;
  }

  public LiveData<PreviewResult> getPreviewResult() {
    return previewResult;
  }

  public LiveData<RenameProgress> getProgress() {
    return progress;
  }

  public LiveData<String> getErrorMessage() {
    return errorMessage;
  }

  public LiveData<String> getNewPackageText() {
    return newPackageText;
  }

  public String getOldPackage() {
    return oldPackage;
  }

  public void start(String moduleRootPath, String packageName) {
    if (scanResult != null && oldPackage.equals(packageName)) {
      return;
    }
    this.oldPackage = packageName;
    uiState.setValue(UiState.SCANNING);
    manager.startScan(new File(moduleRootPath), packageName, scanCallback);
  }

  public void onNewPackageChanged(String text) {
    newPackageText.setValue(text);
    debounceHandler.removeCallbacks(previewRunnable);
    if (scanResult == null) {
      return;
    }
    ValidationResult result = manager.validateNewPackageName(oldPackage, text, scanResult);
    inputValidation.setValue(result);
    if (result.isValid()) {
      uiState.setValue(UiState.VALIDATING_INPUT);
      pendingPreviewPackage = text;
      debounceHandler.postDelayed(previewRunnable, PREVIEW_DEBOUNCE_MS);
    } else {
      previewResult.setValue(null);
      uiState.setValue(UiState.READY);
    }
  }

  private void runPreviewNow() {
    if (scanResult == null || pendingPreviewPackage == null) {
      return;
    }
    uiState.setValue(UiState.BUILDING_PREVIEW);
    manager.buildPreview(scanResult, pendingPreviewPackage, previewCallback);
  }

  public boolean canConfirmRename() {
    ValidationResult validation = inputValidation.getValue();
    UiState state = uiState.getValue();
    return validation != null
        && validation.isValid()
        && state == UiState.PREVIEW_READY
        && previewResult.getValue() != null;
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
    String target = newPackageText.getValue();
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
    debounceHandler.removeCallbacksAndMessages(null);
    manager.shutdown();
  }
}
