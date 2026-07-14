package ir.hanzodev1375.ghostide.refactor.rename.model;

public interface RenameCallback {

  void onScanCompleted(ScanResult scanResult);

  void onPreviewReady(PreviewResult previewResult);

  void onProgress(RenameProgress progress);

  void onSuccess(PreviewResult previewResult);

  void onError(String message, Exception cause);

  void onCancelled();
}
