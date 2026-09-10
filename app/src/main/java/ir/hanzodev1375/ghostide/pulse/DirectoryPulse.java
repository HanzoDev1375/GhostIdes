package ir.hanzodev1375.ghostide.pulse;

import android.os.FileObserver;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import androidx.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Recursive, race-safe dir watcher.
 *
 * <p>Registers one {@link FileObserver} per directory under a root so structural changes deep
 * inside the tree are seen, not just in the root folder itself. Events are only used as a "dirty"
 * signal: instead of forwarding raw event codes (which can interleave and get lost between the
 * observer thread and the UI thread), bursts are coalesced into a single debounced callback that
 * carries the path of the most recent mutation.
 *
 * <p>Guards: heavyweight/build folders are skipped, the number of live observers is capped, and a
 * throttled re-scan heals anything missed during an event flood or an inotify overflow.
 */
public final class DirectoryPulse {

  private static final long QUIET_MS = 300L;
  private static final long MAX_HOLD_MS = 1200L;
  private static final long REHEAL_GAP_MS = 8000L;
  private static final int ROOST_LIMIT = 384;

  private static final int WATCH_BITS =
      FileObserver.CREATE
          | FileObserver.DELETE
          | FileObserver.MOVED_FROM
          | FileObserver.MOVED_TO
          | FileObserver.DELETE_SELF
          | FileObserver.MOVE_SELF;

  private static final int GROW_BITS = FileObserver.CREATE | FileObserver.MOVED_TO;
  private static final int LOST_BITS = FileObserver.DELETE_SELF | FileObserver.MOVE_SELF;

  private static final Set<String> VETOED =
      new HashSet<>(Arrays.asList(".git", ".gradle", ".idea", "node_modules", "build", ".cxx"));

  private final Handler desk = new Handler(Looper.getMainLooper());
  private final ConcurrentHashMap<String, FileObserver> roosts = new ConcurrentHashMap<>();
  private final CopyOnWriteArrayList<PulseListener> audience = new CopyOnWriteArrayList<>();
  private final ExecutorService handyman =
      Executors.newSingleThreadExecutor(
          runnable -> {
            Thread thread = new Thread(runnable, "dir-pulse");
            thread.setDaemon(true);
            return thread;
          });

  private volatile boolean armed;
  private volatile long armedAt;
  private volatile long lastReheal;
  private volatile long season;
  private volatile String lastMutated;
  private volatile File currentRoot;

  private final Runnable relay = this::relayNow;

  public void attach(PulseListener listener) {
    if (listener != null) audience.addIfAbsent(listener);
  }

  public void detach(PulseListener listener) {
    audience.remove(listener);
  }

  public void survey(String root) {
    final long round = ++season;
    currentRoot = root == null ? null : new File(root);
    final File target = currentRoot;
    handyman.execute(
        () -> {
          if (round != season) return;
          disband();
          if (target != null) sow(target, round, true);
        });
  }

  public void land() {
    final long round = ++season;
    currentRoot = null;
    handyman.execute(
        () -> {
          if (round != season) return;
          disband();
        });
  }

  public void shutdown() {
    ++season;
    currentRoot = null;
    disband();
    handyman.shutdownNow();
    desk.removeCallbacksAndMessages(null);
  }

  private void sow(File dir, long round, boolean prime) {
    if (round != season) return;
    if (dir == null || !dir.isDirectory()) return;
    if (!prime && VETOED.contains(dir.getName())) return;
    if (roosts.size() >= ROOST_LIMIT) return;

    String marker = key(dir);
    if (roosts.containsKey(marker)) return;

    FileObserver roost = hatch(dir, round);
    if (roosts.putIfAbsent(marker, roost) != null) return;
    roost.startWatching();

    File[] offspring = dir.listFiles();
    if (offspring == null) return;
    for (File kid : offspring) {
      if (kid.isDirectory()) sow(kid, round, false);
    }
  }

  private FileObserver hatch(File dir, long round) {
    final String marker = key(dir);
    return new FileObserver(dir) {
      @Override
      public void onEvent(int event, @Nullable String child) {
        int seen = event & WATCH_BITS;
        if (seen == 0) return;

        String mutation =
            child == null ? dir.getAbsolutePath() : new File(dir, child).getAbsolutePath();

        if ((seen & GROW_BITS) != 0 && child != null) {
          File candidate = new File(dir, child);
          if (candidate.isDirectory()) {
            handyman.execute(() -> sow(candidate, round, false));
          }
        }

        if ((seen & LOST_BITS) != 0) {
          handyman.execute(() -> roosts.remove(marker));
        }

        spark(mutation);
      }
    };
  }

  private void spark(String mutation) {
    lastMutated = mutation;
    long now = SystemClock.uptimeMillis();
    if (!armed) {
      armed = true;
      armedAt = now;
    }
    long held = now - armedAt;
    long delay = held >= MAX_HOLD_MS ? 0 : QUIET_MS;
    desk.removeCallbacks(relay);
    desk.postDelayed(relay, delay);
  }

  private void relayNow() {
    armed = false;
    String mutation = lastMutated;
    if (mutation == null) return;

    rehealIfDue();

    for (PulseListener listener : audience) {
      try {
        listener.onPulse(mutation);
      } catch (RuntimeException ignored) {
      }
    }
  }

  private void rehealIfDue() {
    long now = SystemClock.uptimeMillis();
    if (now - lastReheal < REHEAL_GAP_MS) return;
    lastReheal = now;
    final long round = season;
    handyman.execute(
        () -> {
          if (round != season) return;
          File root = currentRoot;
          if (root != null) sow(root, round, true);
        });
  }

  private void disband() {
    for (FileObserver roost : roosts.values()) {
      try {
        roost.stopWatching();
      } catch (RuntimeException ignored) {
      }
    }
    roosts.clear();
  }

  private String key(File file) {
    try {
      return file.getCanonicalPath();
    } catch (IOException e) {
      return file.getAbsolutePath();
    }
  }
}