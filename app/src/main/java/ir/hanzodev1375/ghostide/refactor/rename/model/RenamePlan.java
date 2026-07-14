package ir.hanzodev1375.ghostide.refactor.rename.model;

import java.util.Collections;
import java.util.List;

public final class RenamePlan {

  private final ScanResult scanResult;
  private final PreviewResult previewResult;
  private final List<SourceEdit> fileChanges;
  private final List<DirectoryOperation> directoryOperations;

  public RenamePlan(
      ScanResult scanResult,
      PreviewResult previewResult,
      List<SourceEdit> fileChanges,
      List<DirectoryOperation> directoryOperations) {
    this.scanResult = scanResult;
    this.previewResult = previewResult;
    this.fileChanges = Collections.unmodifiableList(fileChanges);
    this.directoryOperations = Collections.unmodifiableList(directoryOperations);
  }

  public ScanResult getScanResult() {
    return scanResult;
  }

  public PreviewResult getPreviewResult() {
    return previewResult;
  }

  public List<SourceEdit> getFileChanges() {
    return fileChanges;
  }

  public List<DirectoryOperation> getDirectoryOperations() {
    return directoryOperations;
  }
}
