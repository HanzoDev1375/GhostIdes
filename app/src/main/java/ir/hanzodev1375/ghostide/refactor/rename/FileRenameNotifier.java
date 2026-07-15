package ir.hanzodev1375.ghostide.refactor.rename;

import android.os.Handler;
import android.os.Looper;
import java.util.ArrayList;
import java.util.List;

public final class FileRenameNotifier {

  public interface Listener {
    void onFileRenamed(String oldPath, String newPath);
  }

  private static final FileRenameNotifier INSTANCE = new FileRenameNotifier();

  private final List<Listener> listeners = new ArrayList<>();
  private final Handler mainHandler = new Handler(Looper.getMainLooper());

  private FileRenameNotifier() {}

  public static FileRenameNotifier getInstance() {
    return INSTANCE;
  }

  public synchronized void addListener(Listener listener) {
    if (!listeners.contains(listener)) {
      listeners.add(listener);
    }
  }

  public synchronized void removeListener(Listener listener) {
    listeners.remove(listener);
  }

  public void notifyRenamed(String oldPath, String newPath) {
    List<Listener> snapshot;
    synchronized (this) {
      snapshot = new ArrayList<>(listeners);
    }
    mainHandler.post(
        () -> {
          for (Listener listener : snapshot) {
            listener.onFileRenamed(oldPath, newPath);
          }
        });
  }
}
