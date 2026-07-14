package ir.hanzodev1375.ghostide.refactor.rename;

public interface CancellableTask {
  void run(CancellationToken token) throws Exception;
}
