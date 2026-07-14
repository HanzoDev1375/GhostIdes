package ir.hanzodev1375.ghostide.refactor.rename;

public final class RenameCancelledException extends Exception {

  public RenameCancelledException() {
    super("Package rename operation was cancelled.");
  }
}
