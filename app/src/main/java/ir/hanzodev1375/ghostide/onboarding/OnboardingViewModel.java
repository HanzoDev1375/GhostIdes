package ir.hanzodev1375.ghostide.onboarding;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.termux.terminal.TerminalSession;
import ir.hanzodev1375.ghostide.R;
import ir.hanzodev1375.ghostide.terminal.DebianBootstrap;
import ir.hanzodev1375.ghostide.terminal.DebianInstaller;
import ir.hanzodev1375.ghostide.terminal.GhostTerminalSessionClient;
import ir.hanzodev1375.ghostide.terminal.ProotSessionFactory;
import ir.hanzodev1375.ghostide.terminal.TerminalSessionFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class OnboardingViewModel extends AndroidViewModel {

  public static final int PAGE_COUNT = 3;
  public static final int PAGE_WELCOME = 0;
  public static final int PAGE_TERMINAL = 1;
  public static final int PAGE_FEATURES = 2;

  private static final String INIT_SCRIPT = "shell/init.sh";

  private final Handler mainHandler = new Handler(Looper.getMainLooper());

  private final MutableLiveData<Integer> page = new MutableLiveData<>(PAGE_WELCOME);
  private final MutableLiveData<Boolean> terminalInstalling = new MutableLiveData<>(false);
  private final MutableLiveData<Boolean> terminalExtracting = new MutableLiveData<>(false);
  private final MutableLiveData<Integer> terminalProgress = new MutableLiveData<>(0);
  private final MutableLiveData<Boolean> terminalDone = new MutableLiveData<>(false);
  private final MutableLiveData<String> terminalStatus = new MutableLiveData<>("");
  private final MutableLiveData<TerminalSession> activeSession = new MutableLiveData<>();

  private volatile Runnable invalidator;
  private boolean debianSessionStarted = false;
  private TerminalSession oldInstallSession;

  private final GhostTerminalSessionClient.Callback internalCallback =
      new GhostTerminalSessionClient.Callback() {
        @Override
        public void onTextChanged(TerminalSession session) {
          Runnable r = invalidator;
          if (r != null) r.run();
        }

        @Override
        public void onTitleChanged(TerminalSession session) {}

        @Override
        public void onSessionFinished(TerminalSession session) {}
      };

  private final DebianInstaller.InstallListener installListener =
      new DebianInstaller.InstallListener() {
        @Override
        public void onDownloadProgress(int percent) {
          mainHandler.post(
              () -> {
                terminalExtracting.setValue(false);
                terminalInstalling.setValue(true);
                terminalProgress.setValue(percent);
                terminalStatus.setValue(
                    getApplication().getString(R.string.onboarding_terminal_downloading, percent));
              });
        }

        @Override
        public void onExtractProgress(int extractedEntries) {
          mainHandler.post(
              () -> {
                terminalExtracting.setValue(true);
                terminalInstalling.setValue(true);
                terminalProgress.setValue(0);
                terminalStatus.setValue(
                    getApplication()
                        .getString(R.string.onboarding_terminal_extracting_line, extractedEntries));
                appendOutput(
                    getApplication()
                        .getString(R.string.onboarding_terminal_extracting_line, extractedEntries));
              });
        }

        @Override
        public void onSuccess() {
          mainHandler.post(
              () -> {
                terminalExtracting.setValue(false);
                terminalInstalling.setValue(false);
                terminalStatus.setValue("");
                terminalDone.setValue(true);
                appendOutput(getApplication().getString(R.string.onboarding_terminal_ready));
                startDebianSession();
              });
        }

        @Override
        public void onError(String message) {
          mainHandler.post(
              () -> {
                terminalExtracting.setValue(false);
                terminalInstalling.setValue(false);
                terminalStatus.setValue("");
                appendOutput("✗ " + message);
              });
        }
      };

  public OnboardingViewModel(@NonNull Application application) {
    super(application);
    DebianInstaller.attach(installListener);
  }

  public void setInvalidator(Runnable runnable) {
    this.invalidator = runnable;
  }

  public LiveData<Integer> getPage() {
    return page;
  }

  public LiveData<Boolean> isTerminalInstalling() {
    return terminalInstalling;
  }

  public LiveData<Boolean> isTerminalExtracting() {
    return terminalExtracting;
  }

  public LiveData<Integer> getTerminalProgress() {
    return terminalProgress;
  }

  public LiveData<Boolean> isTerminalDone() {
    return terminalDone;
  }

  public LiveData<String> getTerminalStatus() {
    return terminalStatus;
  }

  public LiveData<TerminalSession> getActiveSession() {
    return activeSession;
  }

  public void setPage(int value) {
    if (value >= 0 && value < PAGE_COUNT) page.setValue(value);
  }

  public void next() {
    Integer value = page.getValue();
    setPage((value == null ? PAGE_WELCOME : value) + 1);
  }

  public void back() {
    Integer value = page.getValue();
    setPage((value == null ? PAGE_WELCOME : value) - 1);
  }

  public void setTerminalDone(boolean value) {
    terminalDone.setValue(value);
  }

  public void ensureInstallSession() {
    if (activeSession.getValue() != null) return;
    GhostTerminalSessionClient client =
        new GhostTerminalSessionClient(getApplication(), internalCallback);
    TerminalSession session = TerminalSessionFactory.createSession(getApplication(), null, client);
    oldInstallSession = session;
    activeSession.setValue(session);
    runWhenEmulatorReady(
        () -> appendOutput("--- GhostIDE Debian Installer ---"));
  }

  public void startDebianSession() {
    if (debianSessionStarted) return;
    debianSessionStarted = true;
    try {
      GhostTerminalSessionClient client =
          new GhostTerminalSessionClient(getApplication(), internalCallback);
      TerminalSession deb =
          ProotSessionFactory.createProotSession(
              getApplication(),
              DebianBootstrap.getRootfsDir(getApplication()),
              "/bin/bash",
              client);
      activeSession.setValue(deb);
      if (oldInstallSession != null) {
        oldInstallSession.finishIfRunning();
        oldInstallSession = null;
      }
      runWhenEmulatorReady(
          () -> {
            appendOutput("✓ Debian rootfs ready");
            runInitScript();
          });
    } catch (Exception e) {
      appendOutput("✗ Cannot start Debian: " + e.getMessage());
    }
  }

  private void runInitScript() {
    String script = readAsset(INIT_SCRIPT);
    if (script == null || script.isEmpty()) return;
    script = script.replace("clear", "");
    TerminalSession session = activeSession.getValue();
    if (session != null) session.write(script.trim() + "\n", true);
  }

  private String readAsset(String path) {
    try (InputStream is = getApplication().getAssets().open(path)) {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      byte[] buf = new byte[8192];
      int n;
      while ((n = is.read(buf)) != -1) out.write(buf, 0, n);
      return out.toString("UTF-8");
    } catch (IOException e) {
      return null;
    }
  }

  private void runWhenEmulatorReady(Runnable action) {
    mainHandler.post(
        new Runnable() {
          @Override
          public void run() {
            TerminalSession session = activeSession.getValue();
            if (session != null && session.getEmulator() != null) {
              action.run();
            } else {
              mainHandler.postDelayed(this, 100);
            }
          }
        });
  }

  private void appendOutput(String line) {
    TerminalSession session = activeSession.getValue();
    if (session == null || session.getEmulator() == null || line == null) return;
    byte[] bytes = (line + "\r\n").getBytes(StandardCharsets.UTF_8);
    session.getEmulator().append(bytes, bytes.length);
    Runnable r = invalidator;
    if (r != null) r.run();
  }

  public void startInstall() {
    terminalInstalling.setValue(true);
    terminalExtracting.setValue(false);
    terminalProgress.setValue(0);
    terminalStatus.setValue(getApplication().getString(R.string.onboarding_terminal_downloading, 0));
    DebianInstaller.installDebian(getApplication(), installListener);
  }

  public boolean isDebianInstalled() {
    return DebianBootstrap.isInstalled(getApplication());
  }

  @Override
  protected void onCleared() {
    super.onCleared();
    mainHandler.removeCallbacksAndMessages(null);
  }
}