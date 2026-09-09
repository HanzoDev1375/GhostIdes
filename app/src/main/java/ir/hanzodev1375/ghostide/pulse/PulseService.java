package ir.hanzodev1375.ghostide.pulse;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import androidx.annotation.Nullable;

/**
 * Hosts the shared folder radar while the file manager is on screen. It is a plain bound service
 * (no foreground notification): the engine lives only as long as a client stays bound to it.
 */
public class PulseService extends Service {

  private volatile DirectoryPulse station;
  private PulseCore gateway;

  @Override
  public void onCreate() {
    super.onCreate();
    station = new DirectoryPulse();
    gateway = new PulseCore();
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return gateway;
  }

  @Override
  public void onDestroy() {
    if (station != null) station.shutdown();
    station = null;
    gateway = null;
    super.onDestroy();
  }

  public final class PulseCore extends Binder {

    public void survey(String root) {
      if (station != null) station.survey(root);
    }

    public void standDown() {
      if (station != null) station.land();
    }

    public void shake(PulseListener hand) {
      if (station != null) station.attach(hand);
    }

    public void release(PulseListener hand) {
      if (station != null) station.detach(hand);
    }
  }
}