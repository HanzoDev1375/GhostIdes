package ir.hanzodev1375.ghostide.refactor.rename;

import android.os.Handler;
import android.os.Looper;
import ir.hanzodev1375.ghostide.refactor.rename.model.RenameProgress;

public final class ProgressReporter {

  public interface Listener {
    void onProgress(RenameProgress progress);
  }

  private final Handler mainThreadHandler;
  private final Listener listener;

  public ProgressReporter(Listener listener) {
    this.mainThreadHandler = new Handler(Looper.getMainLooper());
    this.listener = listener;
  }

  public void report(RenameProgress progress) {
    if (listener == null) {
      return;
    }
    mainThreadHandler.post(() -> listener.onProgress(progress));
  }
}
