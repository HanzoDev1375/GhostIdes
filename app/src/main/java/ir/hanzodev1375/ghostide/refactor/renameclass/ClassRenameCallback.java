package ir.hanzodev1375.ghostide.refactor.renameclass;

import ir.hanzodev1375.ghostide.refactor.rename.model.RenameProgress;
import java.io.File;

public interface ClassRenameCallback {

  void onScanCompleted(ClassScanResult scanResult);

  void onProgress(RenameProgress progress);

  void onSuccess(File newFile);

  void onError(String message, Exception cause);

  void onCancelled();
}
