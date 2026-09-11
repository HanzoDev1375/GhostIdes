package ir.hanzodev1375.ghostide.terminal;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.blankj.utilcode.util.FileIOUtils;
import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.ResourceUtils;
import com.termux.terminal.TerminalSession;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TerminalViewModel extends AndroidViewModel {

  private static final String LOG_TAG = "TerminalViewModel";
  private static final String ASSET_INIT_SH = "shell/init.sh";
  private static final String INIT_RUN_MARKER = "ghostide-init-run";
  private static final String[] HELPER_COMMANDS = {"weblsp"};

  private final MutableLiveData<List<TerminalTab>> sessions = new MutableLiveData<>(new ArrayList<>());
  private final MutableLiveData<Integer> currentTabIndex = new MutableLiveData<>(-1);

  /** فرگمنتی که یه سشن رو نشون میده، خودش رو با tabId ثبت می‌کنه تا وقتی خروجی جدید میاد صدا زده بشه. */
  public interface ScreenRefresher {
    void onScreenUpdated();
  }

  private final Map<Integer, ScreenRefresher> screenRefreshers = new HashMap<>();

  public void registerScreenRefresher(int tabId, ScreenRefresher refresher) {
    screenRefreshers.put(tabId, refresher);
  }

  public void unregisterScreenRefresher(int tabId) {
    screenRefreshers.remove(tabId);
  }

  private TerminalSessionService service;
  private boolean isBound;
  private boolean ctrlToggled;
  private boolean altToggled;

  private final Handler mainHandler = new Handler(Looper.getMainLooper());

  public interface SessionListener {
    void onServiceConnected();
    void onSessionAdded(int index);
    void onSessionRemoved(boolean empty);
    void onTitleChanged(int index);
    void onServiceLost();
  }

  /** وقتی init.sh تازه قراره اجرا بشه (نصب تازه) صدا زده میشه تا Activity اورلیِ لودینگ رو نشون بده. */
  private Runnable initOverlayShower;
  /** بعد از کامل شدن init.sh صدا زده میشه تا Activity اورلیِ لودینگ رو مخفی کنه. */
  private Runnable initOverlayHider;

  private SessionListener activityListener;

  private final ServiceConnection connection =
      new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
          service = ((TerminalSessionService.LocalBinder) binder).getService();
          isBound = true;
          sessions.setValue(new ArrayList<>(service.getSessions()));
          setServiceListener();
          mainHandler.post(() -> {
            if (activityListener != null) activityListener.onServiceConnected();
          });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
          isBound = false;
          service = null;
          mainHandler.post(() -> {
            if (activityListener != null) activityListener.onServiceLost();
          });
        }
      };

  public TerminalViewModel(@NonNull Application application) {
    super(application);
  }

  public void setActivityListener(SessionListener listener) {
    this.activityListener = listener;
  }

  public void setInitOverlayShower(Runnable shower) {
    this.initOverlayShower = shower;
  }

  public void setInitOverlayHider(Runnable hider) {
    this.initOverlayHider = hider;
  }

  public LiveData<List<TerminalTab>> getSessions() {
    return sessions;
  }

  public LiveData<Integer> getCurrentTabIndex() {
    return currentTabIndex;
  }

  public List<TerminalTab> getSessionList() {
    List<TerminalTab> list = sessions.getValue();
    return list != null ? list : new ArrayList<>();
  }

  public TerminalSession getCurrentSession() {
    int idx = currentTabIndex.getValue() != null ? currentTabIndex.getValue() : -1;
    List<TerminalTab> list = sessions.getValue();
    if (list == null || idx < 0 || idx >= list.size()) return null;
    return list.get(idx).session;
  }

  public TerminalSession getSessionById(int id) {
    List<TerminalTab> list = sessions.getValue();
    if (list == null) return null;
    for (TerminalTab tab : list) {
      if (tab.id == id) return tab.session;
    }
    return null;
  }

  public boolean containsSessionId(int id) {
    List<TerminalTab> list = sessions.getValue();
    if (list == null) return false;
    for (TerminalTab tab : list) {
      if (tab.id == id) return true;
    }
    return false;
  }

  public boolean isCtrlToggled() {
    return ctrlToggled;
  }

  public boolean isAltToggled() {
    return altToggled;
  }

  public void consumeCtrlToggle() {
    ctrlToggled = false;
  }

  public void consumeAltToggle() {
    altToggled = false;
  }

  public void toggleCtrl() {
    ctrlToggled = !ctrlToggled;
  }

  public void toggleAlt() {
    altToggled = !altToggled;
  }

  public void bindService() {
    if (isBound) return;
    Intent intent = new Intent(getApplication(), TerminalSessionService.class);
    ContextCompat.startForegroundService(getApplication(), intent);
    getApplication().bindService(intent, connection, Context.BIND_AUTO_CREATE);
  }

  public void unbindService() {
    if (!isBound) return;
    isBound = false;
    getApplication().unbindService(connection);
    service = null;
  }

  public void setServiceListener() {
    if (service == null) return;
    service.setUiListener(
        new TerminalSessionService.SessionListener() {
          @Override
          public void onTextChanged(TerminalSession session) {
            int index = indexOfSession(session);
            if (index < 0) return;
            List<TerminalTab> list = sessions.getValue();
            if (list == null) return;
            ScreenRefresher refresher = screenRefreshers.get(list.get(index).id);
            if (refresher != null) mainHandler.post(refresher::onScreenUpdated);
          }

          @Override
          public void onTitleChanged(TerminalSession session) {
            int index = indexOfSession(session);
            if (index >= 0) {
              int i = index;
              mainHandler.post(() -> {
                if (activityListener != null) activityListener.onTitleChanged(i);
              });
            }
          }

          @Override
          public void onSessionFinished(TerminalSession session) {
            removeSessionFromList(session);
          }
        });
  }

  public void addSession(@Nullable String workingDir) {
    if (service == null) return;
    service.createSession(workingDir);
    sessions.setValue(new ArrayList<>(service.getSessions()));
    int newIndex = sessions.getValue().size() - 1;
    currentTabIndex.setValue(newIndex);
    int added = newIndex;
    mainHandler.post(() -> {
      if (activityListener != null) activityListener.onSessionAdded(added);
    });
  }

  public void addDebianSession() {
    if (service == null) return;
    service.createDebianSession();
    sessions.setValue(new ArrayList<>(service.getSessions()));
    int newIndex = sessions.getValue().size() - 1;
    currentTabIndex.setValue(newIndex);
    syncShellScriptsToFilesDir();
    runInitScriptIfNeeded();
    int added = newIndex;
    mainHandler.post(() -> {
      if (activityListener != null) activityListener.onSessionAdded(added);
    });
  }

  public void removeSession(int position) {
    if (service == null) return;
    List<TerminalTab> list = sessions.getValue();
    if (list == null || position < 0 || position >= list.size()) return;
    TerminalSession session = list.get(position).session;
    service.removeSession(session);
    List<TerminalTab> updated = new ArrayList<>(service.getSessions());
    sessions.setValue(updated);

    if (updated.isEmpty()) {
      currentTabIndex.setValue(-1);
      mainHandler.post(() -> {
        if (activityListener != null) activityListener.onSessionRemoved(true);
      });
    } else {
      int newIdx = Math.min(position, updated.size() - 1);
      currentTabIndex.setValue(newIdx);
      mainHandler.post(() -> {
        if (activityListener != null) activityListener.onSessionRemoved(false);
      });
    }
  }

  public void switchToTab(int position) {
    List<TerminalTab> list = sessions.getValue();
    if (list == null || position < 0 || position >= list.size()) return;
    TerminalSession session = list.get(position).session;
    TerminalColorsUtil.refreshSession(session);
    currentTabIndex.setValue(position);
  }

  public void writeCommandWhenReady(TerminalSession session, String command) {
    if (session == null || command == null || command.isEmpty()) return;
    mainHandler.post(
        new Runnable() {
          @Override
          public void run() {
            if (session.getEmulator() != null) {
              session.write(command + "\n");
            } else {
              mainHandler.postDelayed(this, 100);
            }
          }
        });
  }

  private int indexOfSession(TerminalSession session) {
    List<TerminalTab> list = sessions.getValue();
    if (list == null) return -1;
    for (int i = 0; i < list.size(); i++) {
      if (list.get(i).session == session) return i;
    }
    return -1;
  }

  private void removeSessionFromList(TerminalSession session) {
    List<TerminalTab> list = sessions.getValue();
    if (list == null) return;
    int index = indexOfSession(session);
    if (index < 0) return;
    list.remove(index);
    sessions.postValue(new ArrayList<>(list));
    if (list.isEmpty()) {
      currentTabIndex.postValue(-1);
      mainHandler.post(() -> {
        if (activityListener != null) activityListener.onSessionRemoved(true);
      });
    } else {
      int newIdx = Math.min(index, list.size() - 1);
      currentTabIndex.postValue(newIdx);
      mainHandler.post(() -> {
        if (activityListener != null) activityListener.onSessionRemoved(false);
      });
    }
  }

  public void syncShellScriptsToFilesDir() {
    File shellDir = new File(getApplication().getFilesDir(), "shell");
    if (!shellDir.exists() && !shellDir.mkdirs()) return;
    for (String name : HELPER_COMMANDS) {
      copyAssetToFile("shell/" + name + ".sh", new File(shellDir, name + ".sh"));
    }
  }

  public void runInitScriptIfNeeded() {
    File rootfs = DebianBootstrap.getRootfsDir(getApplication());
    File marker = new File(rootfs, INIT_RUN_MARKER);
    if (marker.exists()) return;

    if (initOverlayShower != null) {
      mainHandler.post(() -> initOverlayShower.run());
    }

    mainHandler.postDelayed(
        () -> {
          List<TerminalTab> list = sessions.getValue();
          if (list == null || list.isEmpty()) return;
          TerminalSession session = list.get(list.size() - 1).session;
          if (session.getEmulator() == null) {
            mainHandler.postDelayed(this::runInitScriptIfNeeded, 200);
            return;
          }
          installHelperCommands(rootfs);
          String initScript = ResourceUtils.readAssets2String(ASSET_INIT_SH);
          String weblspScript = ResourceUtils.readAssets2String("shell/weblsp.sh");
          if (initScript != null && !initScript.isEmpty()) {
            StringBuilder combined = new StringBuilder(initScript);
            if (weblspScript != null && !weblspScript.isEmpty()) {
              combined.append("\n");
              combined.append(weblspScript);
            }
            combined.append("\necho '[GHOSTIDE] setup complete ✓'\n");
            session.write(combined.toString());
            FileUtils.createFileByDeleteOldFile(marker);
            FileIOUtils.writeFileFromString(marker, "done");
            if (initOverlayHider != null) {
              mainHandler.postDelayed(() -> initOverlayHider.run(), 3000);
            }
          }
        },
        2000);
  }

  private void installHelperCommands(File rootfs) {
    for (String name : HELPER_COMMANDS) {
      installHelperCommand(rootfs, name);
    }
  }

  private void installHelperCommand(File rootfs, String name) {
    try {
      File binDir = new File(rootfs, "usr/local/bin");
      if (!binDir.exists() && !binDir.mkdirs()) return;
      File command = new File(binDir, name);
      String wrapper = "#!/bin/bash\nexec bash /ghostide/files/shell/" + name + ".sh \"$@\"\n";
      FileUtils.createFileByDeleteOldFile(command);
      FileIOUtils.writeFileFromString(command, wrapper);
      command.setExecutable(true, false);
    } catch (Exception e) {
      Log.w(LOG_TAG, "install helper command failed: " + name, e);
    }
  }

  private void copyAssetToFile(String assetPath, File target) {
    try {
      String content = ResourceUtils.readAssets2String(assetPath);
      if (content == null || content.isEmpty()) return;
      FileUtils.createFileByDeleteOldFile(target);
      FileIOUtils.writeFileFromString(target, content);
    } catch (Exception e) {
      Log.w(LOG_TAG, "copy asset failed: " + assetPath, e);
    }
  }

  @Override
  protected void onCleared() {
    super.onCleared();
    mainHandler.removeCallbacksAndMessages(null);
    unbindService();
  }
}