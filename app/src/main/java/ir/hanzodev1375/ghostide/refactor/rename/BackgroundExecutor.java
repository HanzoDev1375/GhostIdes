package ir.hanzodev1375.ghostide.refactor.rename;

import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

public final class BackgroundExecutor {

  public interface TaskResultCallback {
    void onCompleted();

    void onFailed(Exception exception);

    void onCancelled();
  }

  private final ExecutorService executorService;
  private final Handler mainThreadHandler;
  private final AtomicReference<CancellationToken> activeToken;
  private volatile Future<?> activeFuture;

  public BackgroundExecutor() {
    this.executorService = Executors.newSingleThreadExecutor();
    this.mainThreadHandler = new Handler(Looper.getMainLooper());
    this.activeToken = new AtomicReference<>();
  }

  public CancellationToken execute(CancellableTask task, TaskResultCallback callback) {
    CancellationToken token = new CancellationToken();
    activeToken.set(token);
    activeFuture =
        executorService.submit(
            () -> {
              try {
                task.run(token);
                if (token.isCancelled()) {
                  mainThreadHandler.post(callback::onCancelled);
                } else {
                  mainThreadHandler.post(callback::onCompleted);
                }
              } catch (RenameCancelledException cancelledException) {
                mainThreadHandler.post(callback::onCancelled);
              } catch (Exception exception) {
                if (token.isCancelled()) {
                  mainThreadHandler.post(callback::onCancelled);
                } else {
                  mainThreadHandler.post(() -> callback.onFailed(exception));
                }
              }
            });
    return token;
  }

  public void cancelActive() {
    CancellationToken token = activeToken.get();
    if (token != null) {
      token.cancel();
    }
  }

  public void postToMainThread(Runnable runnable) {
    mainThreadHandler.post(runnable);
  }

  public void shutdown() {
    executorService.shutdownNow();
  }
}
