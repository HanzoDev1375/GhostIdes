package ir.hanzodev1375.ghostide.refactor.rename.model;

public final class RenameProgress {

  public enum Phase {
    SCANNING,
    VALIDATING,
    BUILDING_PREVIEW,
    BACKING_UP,
    REWRITING_JAVA,
    REWRITING_KOTLIN,
    REWRITING_MANIFEST,
    REWRITING_GRADLE,
    MOVING_FILES,
    DELETING_EMPTY_DIRECTORIES,
    ROLLING_BACK,
    COMPLETED
  }

  private final Phase phase;
  private final int currentIndex;
  private final int totalCount;
  private final String currentItemName;

  public RenameProgress(Phase phase, int currentIndex, int totalCount, String currentItemName) {
    this.phase = phase;
    this.currentIndex = currentIndex;
    this.totalCount = totalCount;
    this.currentItemName = currentItemName;
  }

  public Phase getPhase() {
    return phase;
  }

  public int getCurrentIndex() {
    return currentIndex;
  }

  public int getTotalCount() {
    return totalCount;
  }

  public String getCurrentItemName() {
    return currentItemName;
  }

  public int getPercent() {
    if (totalCount <= 0) {
      return 0;
    }
    int percent = (int) ((currentIndex * 100L) / totalCount);
    if (percent < 0) {
      return 0;
    }
    if (percent > 100) {
      return 100;
    }
    return percent;
  }
}
