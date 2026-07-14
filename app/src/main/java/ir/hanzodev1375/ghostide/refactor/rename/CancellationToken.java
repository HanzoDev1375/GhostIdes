package ir.hanzodev1375.ghostide.refactor.rename;

import java.util.concurrent.atomic.AtomicBoolean;

public final class CancellationToken {

  private final AtomicBoolean cancelled = new AtomicBoolean(false);

  public void cancel() {
    cancelled.set(true);
  }

  public boolean isCancelled() {
    return cancelled.get();
  }

  public void throwIfCancelled() throws RenameCancelledException {
    if (cancelled.get()) {
      throw new RenameCancelledException();
    }
  }
}
