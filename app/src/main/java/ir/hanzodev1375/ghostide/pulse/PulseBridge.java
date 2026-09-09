package ir.hanzodev1375.ghostide.pulse;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import java.io.File;

/**
 * Manages the client side of the {@link PulseService} binding: registers a listener, picks the
 * folder to scan and tears everything down again when the caller goes away.
 */
public final class PulseBridge implements ServiceConnection {

  private final PulseListener hand;
  private PulseService.PulseCore core;
  private File pendingRoot;
  private boolean linked;

  public PulseBridge(PulseListener hand) {
    this.hand = hand;
  }

  @Override
  public void onServiceConnected(ComponentName name, IBinder raw) {
    core = (PulseService.PulseCore) raw;
    if (hand != null) core.shake(hand);
    linked = true;
    if (pendingRoot != null) core.survey(pendingRoot.getAbsolutePath());
  }

  @Override
  public void onServiceDisconnected(ComponentName name) {
    linked = false;
    core = null;
  }

  public void point(File root) {
    pendingRoot = root;
    if (linked && core != null && root != null) {
      core.survey(root.getAbsolutePath());
    }
  }

  public void quell() {
    linked = false;
    if (core != null) {
      if (hand != null) core.release(hand);
      core.standDown();
    }
    core = null;
  }

  public boolean isLinked() {
    return linked;
  }
}