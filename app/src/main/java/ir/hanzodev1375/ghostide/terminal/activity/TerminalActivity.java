package ir.hanzodev1375.ghostide.terminal.activity;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.blankj.utilcode.util.FileUtils;
import ir.hanzodev1375.components.views.GhostToast;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.blankj.utilcode.util.FileIOUtils;
import com.blankj.utilcode.util.ResourceUtils;
import ir.theme.GhostTheme;
import ir.theme.M3Theme;
import com.termux.terminal.TerminalSession;
import ir.hanzodev1375.ghostide.R;
import ir.hanzodev1375.ghostide.activity.BaseCompat;
import ir.hanzodev1375.ghostide.codeeditors.setting.PreferencesUtils;
import ir.hanzodev1375.ghostide.databinding.ActivityTerminalBinding;
import ir.hanzodev1375.ghostide.terminal.DebianBootstrap;
import ir.hanzodev1375.ghostide.terminal.DebianInstaller;
import ir.hanzodev1375.ghostide.terminal.GhostTerminalViewClient;
import ir.hanzodev1375.ghostide.terminal.TerminalColorsUtil;
import ir.hanzodev1375.ghostide.terminal.TerminalInputDock;
import ir.hanzodev1375.ghostide.terminal.TerminalSessionService;
import ir.hanzodev1375.ghostide.terminal.TerminalTab;
import ir.hanzodev1375.ghostide.terminal.adapters.TerminalTabAdapter;
import ir.hanzodev1375.ghostide.utils.ObjectUtil;
import ir.theme.ThemeManager;
import ir.theme.ThemeUtils;
import java.nio.charset.StandardCharsets;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import ir.hanzodev1375.components.sheet.customitemsheet.ui.DialogCompat;

public class TerminalActivity extends BaseCompat
    implements GhostTerminalViewClient.KeyModifierState, TerminalSessionService.SessionListener {

  public static final String EXTRA_WORKING_DIR = "working_dir";
  public static final String EXTRA_COMMAND = "command";
  private static final String ASSET_INIT_SH = "shell/init.sh";
  private static final String INIT_RUN_MARKER = "ghostide-init-run";
  private static final String[] HELPER_COMMANDS = {
    "weblsp",
    "pylsp",
    "phplsp",
    "cpplsp",
    "golsp",
    "sasslsp",
    "rubylsp",
    "csharplsp",
    "vuelsp",
    "javalsp"
  };
  private static final String LOG_TAG = "TerminalActivity";
  private ActivityTerminalBinding b;
  private TerminalSessionService service;
  private boolean isBound = false;
  private TerminalTabAdapter tabAdapter;
  private int currentTabIndex = -1;

  private boolean ctrlToggled = false;
  private boolean altToggled = false;
  private int defaultKeyBackgroundColor;
  private int defaultKeyTextColor;
  private PreferencesUtils appsetting;
  private ThemeUtils themeutil;
  private TerminalInputDock inputDock;

  private final ServiceConnection connection =
      new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binderObj) {
          service = ((TerminalSessionService.LocalBinder) binderObj).getService();
          isBound = true;
          service.setUiListener(TerminalActivity.this);
          onServiceReady();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
          isBound = false;
          service = null;
        }
      };

  private AlertDialog installDialog;
  private TextView installStatusText;
  private ProgressBar installProgressBar;
  private DebianInstaller.InstallListener installListener;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    b = ActivityTerminalBinding.inflate(getLayoutInflater());
    setContentView(b.getRoot());
    M3Theme.apply(b.getRoot());

    setupToolbar();
    setupEdgeToEdgeInsets();
    setupTerminalView();
    setupExtraKeys();
    applyJsonTheme();
    setupInputDock();
    setupBackHandler();
    setupBackgroundBlur();
    maybeRequestNotificationPermission();
  }

  @Override
  protected void onStart() {
    super.onStart();
    initializeTerminal();
  }

  @Override
  protected void onStop() {
    if (isBound) {
      service.setUiListener(null);
      unbindService(connection);
      isBound = false;
    }
    DebianInstaller.detach(installListener);
    super.onStop();
  }

  private void initializeTerminal() {
    if (DebianBootstrap.isInstalled(this)) {
      bindServiceAndStart();
      return;
    }
    if (DebianInstaller.isInstalling()) {
      b.terminalView.setVisibility(View.INVISIBLE);
      attachToRunningInstall();
      return;
    }
    b.terminalView.setVisibility(View.INVISIBLE);
    startDebianInstall();
  }

  private void bindServiceAndStart() {
    Intent serviceIntent = new Intent(this, TerminalSessionService.class);
    ContextCompat.startForegroundService(this, serviceIntent);
    bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE);
  }

  private void maybeRequestNotificationPermission() {
    if (Build.VERSION.SDK_INT >= 33) {
      if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
          != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(
            this, new String[] {Manifest.permission.POST_NOTIFICATIONS}, 4821);
      }
    }
  }

  private void onServiceReady() {
    if (tabAdapter == null) setupSessionTabs();

    String command = getIntent().getStringExtra(EXTRA_COMMAND);
    if (command != null && !command.isEmpty()) {
      addNewDebianSession();
      getIntent().removeExtra(EXTRA_COMMAND);
      return;
    }

    List<TerminalTab> sessions = service.getSessions();
    if (sessions.isEmpty()) {
      addNewDebianSession();
    } else {
      int index =
          (currentTabIndex >= 0 && currentTabIndex < sessions.size())
              ? currentTabIndex
              : sessions.size() - 1;
      switchToTab(index);
    }
  }

  private void setupInputDock() {
    inputDock = new TerminalInputDock(b, this::currentSession);
    inputDock.attach();
    inputDock.attachKeyboardWatcher(getWindow().getDecorView());
  }

  private void setupEdgeToEdgeInsets() {
    ViewCompat.setOnApplyWindowInsetsListener(
        b.coordinator,
        (v, insets) -> {
          Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
          Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
          b.toolbar.setPadding(
              b.toolbar.getPaddingLeft(),
              systemBars.top,
              b.toolbar.getPaddingRight(),
              b.toolbar.getPaddingBottom());
          b.inputDock.setPadding(
              b.inputDock.getPaddingLeft(),
              b.inputDock.getPaddingTop(),
              b.inputDock.getPaddingRight(),
              Math.max(systemBars.bottom, ime.bottom));
          return insets;
        });
  }

  private void setupBackgroundBlur() {
    b.inputDock.setElevation(0f);
    setupBackgroundBlur(
        b.backgroundIconTerminal, b.toolbar, b.sessionTabsRow, b.inputDock, b.terminalView);
  }

  /**
   * تم JSON را به‌صورت صریح روی تک‌تک المان‌های layout اعمال می‌کند. خیلی از ویوها (نوار تب‌ها،
   * دیوایدرها، پنل ورودی و...) background را از ?attr/... می‌گیرند که همیشه به رنگ‌های تم JSON ست
   * نمی‌شود؛ اینجا همه را مستقیماً از {@link M3Theme} رنگ می‌زنیم تا تم کامل باشد.
   */
  private void applyJsonTheme() {
    boolean hasBackgroundImage = isBackgroundImageEnabled();

    if (hasBackgroundImage) {
      // با وجود عکس پس‌زمینه، رنگ‌های تُپُر اعمال نمی‌شوند تا عکس نمایان باشد.
      b.coordinator.setBackgroundColor(Color.TRANSPARENT);
    } else {
      Integer surfaceContainer = M3Theme.surfaceContainer();
      Integer surface = M3Theme.surface();
      Integer surfaceHigh = M3Theme.surfaceContainerHigh();

      if (surfaceContainer == null) surfaceContainer = surface;
      if (surfaceContainer != null) b.coordinator.setBackgroundColor(surfaceContainer);

      if (surfaceHigh != null) {
        applyViewColor(b.toolbar, surfaceHigh);
        applyViewColor(b.sessionTabsRow, surfaceHigh);
        applyViewColor(b.inputDock, surfaceHigh);
      }
    }

    Integer onSurface = M3Theme.onSurface();
    Integer onSurfaceVariant = M3Theme.onSurfaceVariant();
    Integer outlineVariant = M3Theme.outlineVariant();
    Integer primary = M3Theme.primary();

    if (onSurface != null) {
      b.handleChevron.setColorFilter(onSurface);
    }

    if (primary != null) {
      b.commandInputLayout.setEndIconTintList(ColorStateList.valueOf(primary));
    }

    if (onSurfaceVariant != null) {
      b.commandInput.setHintTextColor(onSurfaceVariant);
    }

    if (outlineVariant != null) {
      applyViewColor(b.dividerTabs, outlineVariant);
      applyViewColor(b.dividerTop, outlineVariant);
      applyViewColor(b.divExtra1, outlineVariant);
      applyViewColor(b.divExtra2, outlineVariant);
      applyViewColor(b.divExtra3, outlineVariant);
      applyViewColor(b.dragHandlePill, outlineVariant);
    }
  }

  /** آیا کاربر عکس پس‌زمینه فعال کرده تا رنگ‌های تُپُر حذف شوند؟ */
  private boolean isBackgroundImageEnabled() {
    boolean showBg = new PreferencesUtils(this).isShowBackground();
    try {
      GhostTheme theme = new ThemeUtils(new ThemeManager(this)).getTheme();
      return showBg
          && theme != null
          && theme.getWidget() != null
          && theme.getWidget().getImagepath() != null
          && !theme.getWidget().getImagepath().isEmpty();
    } catch (Throwable ignored) {
      return false;
    }
  }

  /** رنگ را روی background موجود View اعمال می‌کند (فرم/گوشه‌های گرد حفظ می‌شود). */
  private void applyViewColor(View view, int color) {
    if (view == null) return;
    Drawable background = view.getBackground();
    if (background != null) {
      background.mutate().setTint(color);
    } else {
      view.setBackgroundColor(color);
    }
  }

  private void setupToolbar() {
    setSupportActionBar(b.toolbar);
    if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    b.toolbar.setNavigationOnClickListener(v -> finish());
    b.btnMoreMenu.setOnClickListener(this::showMoreMenu);
    styleToolbarChrome();
  }

  private void styleToolbarChrome() {
    Integer onSurface = M3Theme.onSurface();
    if (onSurface == null) return;
    b.toolbar.setTitleTextColor(onSurface);
    b.toolbar.setSubtitleTextColor(onSurface);
    b.toolbar.setNavigationIconTint(onSurface);
    tintIcon(b.btnMoreMenu, onSurface);
    tintIcon(b.btnNewSession, onSurface);
  }

  private void tintIcon(ImageButton button, int color) {
    Drawable drawable = button.getDrawable();
    if (drawable == null) return;
    DrawableCompat.setTint(drawable.mutate(), color);
  }

  private void showMoreMenu(View anchor) {
    List<String> items =
        Collections.singletonList(
            DebianBootstrap.isInstalled(this)
                ? getString(R.string.terminal_remove_debian)
                : getString(R.string.terminal_install_debian));
    ObjectUtil.showGlassMenu(
        this,
        anchor,
        items,
        (index, title) -> {
          if (DebianBootstrap.isInstalled(this)) {
            confirmAndRemoveDebian();
          } else {
            startDebianInstall();
          }
        });
  }

  private void setupTerminalView() {
    b.terminalView.setTerminalViewClient(new GhostTerminalViewClient(b.terminalView, this));
    TerminalColorsUtil.apply(this, this);
    b.terminalView.setBackgroundColor(Color.TRANSPARENT);
  }

  private void setupSessionTabs() {
    tabAdapter =
        new TerminalTabAdapter(
            service.getSessions(),
            new TerminalTabAdapter.Listener() {
              @Override
              public void onTabSelected(int position) {
                switchToTab(position);
              }

              @Override
              public void onTabClosed(int position) {
                closeTab(position);
              }
            });
    b.sessionTabs.setLayoutManager(
        new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
    b.sessionTabs.setAdapter(tabAdapter);
    b.btnNewSession.setOnClickListener(this::showNewSessionMenu);
  }

  private void showNewSessionMenu(View anchor) {
    if (!DebianBootstrap.isInstalled(this)) {
      GhostToast.makeText(
              this,
              getString(
                  R.string.terminal_debian_rootfs_not_found_fallback,
                  DebianBootstrap.getRootfsDir(this).getAbsolutePath()),
              GhostToast.LENGTH_LONG)
          .show();
      addNewSession();
      return;
    }
    List<String> items = Arrays.asList("Shell", "Debian");
    ObjectUtil.showGlassMenu(
        this,
        anchor,
        items,
        (index, title) -> {
          if (index == 1) {
            addNewDebianSession();
          } else {
            addNewSession();
          }
        });
  }

  private void setupExtraKeys() {
    int tonalBg = fallback(M3Theme.secondaryContainer(), fallback(M3Theme.secondary(), 0));
    int tonalFg = fallback(M3Theme.onSecondaryContainer(), fallback(M3Theme.onSecondary(), 0));
    defaultKeyBackgroundColor = tonalBg;
    defaultKeyTextColor = tonalFg;

    Button[] keys = {
      b.keyEsc,
      b.keyTab,
      b.keyCtrl,
      b.keyAlt,
      b.keyUp,
      b.keyDown,
      b.keyLeft,
      b.keyRight,
      b.keySlash,
      b.keyDash,
      b.keyPipe
    };
    for (Button key : keys) {
      key.setBackgroundTintList(ColorStateList.valueOf(tonalBg));
      key.setTextColor(tonalFg);
    }

    b.keyEsc.setOnClickListener(v -> sendKeyEvent(KeyEvent.KEYCODE_ESCAPE));
    b.keyTab.setOnClickListener(v -> sendKeyEvent(KeyEvent.KEYCODE_TAB));
    b.keyUp.setOnClickListener(v -> sendKeyEvent(KeyEvent.KEYCODE_DPAD_UP));
    b.keyDown.setOnClickListener(v -> sendKeyEvent(KeyEvent.KEYCODE_DPAD_DOWN));
    b.keyLeft.setOnClickListener(v -> sendKeyEvent(KeyEvent.KEYCODE_DPAD_LEFT));
    b.keyRight.setOnClickListener(v -> sendKeyEvent(KeyEvent.KEYCODE_DPAD_RIGHT));
    b.keySlash.setOnClickListener(v -> typeText("/"));
    b.keyDash.setOnClickListener(v -> typeText("-"));
    b.keyPipe.setOnClickListener(v -> typeText("|"));
    b.keyCtrl.setOnClickListener(
        v -> {
          ctrlToggled = !ctrlToggled;
          updateModifierButtonStyle(b.keyCtrl, ctrlToggled);
        });
    b.keyAlt.setOnClickListener(
        v -> {
          altToggled = !altToggled;
          updateModifierButtonStyle(b.keyAlt, altToggled);
        });
  }

  private void updateModifierButtonStyle(Button button, boolean active) {
    if (active) {
      int bg = fallback(M3Theme.primary(), 0);
      int fg = fallback(M3Theme.onPrimary(), 0);
      button.setBackgroundTintList(ColorStateList.valueOf(bg));
      button.setTextColor(fg);
    } else {
      button.setBackgroundTintList(ColorStateList.valueOf(defaultKeyBackgroundColor));
      button.setTextColor(defaultKeyTextColor);
    }
  }

  private void setupBackHandler() {
    getOnBackPressedDispatcher()
        .addCallback(
            this,
            new OnBackPressedCallback(true) {
              @Override
              public void handleOnBackPressed() {
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
              }
            });
  }

  private void sendKeyEvent(int keyCode) {
    long now = SystemClock.uptimeMillis();
    b.terminalView.dispatchKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0));
    b.terminalView.dispatchKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0));
  }

  private void typeText(String text) {
    TerminalSession session = currentSession();
    if (session == null) return;
    byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
    session.write(bytes, 0, bytes.length);
  }

  private void addNewSession() {
    if (service == null) return;
    String workingDir = getIntent().getStringExtra(EXTRA_WORKING_DIR);
    service.createSession(workingDir);
    tabAdapter.notifyDataSetChanged();
    switchToTab(service.getSessions().size() - 1);
  }

  private void addNewDebianSession() {
    if (service == null) return;
    service.createDebianSession();
    tabAdapter.notifyDataSetChanged();
    switchToTab(service.getSessions().size() - 1);

    String command = getIntent().getStringExtra(EXTRA_COMMAND);
    if (command != null && !command.isEmpty()) {
      TerminalSession session = currentSession();
      writeCommandWhenReady(session, command);
    }

    syncShellScriptsToFilesDir();
    runInitScriptIfNeeded();
  }

  /**
   * بعد از نصبِ تازه‌ی Debian، فایل init.sh را فقط یک‌بار (بعد از ۲ ثانیه) در اولین سشنِ Debian
   * اجرا می‌کند تا دیتاهای نصب (مثل nodejs) تنظیم شوند. بعد از اجرا یک marker روی rootfs ساخته
   * می‌شود؛ پس وقتی کاربر Debian را حذف و دوباره نصب کند، marker حذف شده و init.sh دوباره اجرا
   * می‌شود.
   */
  private void runInitScriptIfNeeded() {
    File rootfs = DebianBootstrap.getRootfsDir(this);
    File marker = new File(rootfs, INIT_RUN_MARKER);
    if (marker.exists()) return;

    b.terminalView.postDelayed(
        new Runnable() {
          @Override
          public void run() {
            TerminalSession session = currentSession();
            if (session == null) return;
            if (session.getEmulator() == null) {
              b.terminalView.postDelayed(this, 100);
              return;
            }
            installHelperCommands(rootfs);
            String script = ResourceUtils.readAssets2String(ASSET_INIT_SH);
            if (script != null && !script.isEmpty()) {
              session.write(
                  script
                      + "\n"
                      + "echo 'GhostIDE: type one of: "
                      + String.join(", ", HELPER_COMMANDS)
                      + "'\n");
              FileUtils.createFileByDeleteOldFile(marker);
              FileIOUtils.writeFileFromString(marker, "done");
            }
          }
        },
        2000);
  }

  /** اسکریپت‌های کمکی را از assets به files/shell اپ کپی می‌کند تا همیشه به‌روز باشند. */
  private void syncShellScriptsToFilesDir() {
    File shellDir = new File(getFilesDir(), "shell");
    if (!shellDir.exists() && !shellDir.mkdirs()) return;
    for (String name : HELPER_COMMANDS) {
      copyAssetToFile("shell/" + name + ".sh", new File(shellDir, name + ".sh"));
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

  /**
   * برای دستورهای weblsp و pylsp داخل rootfs یک wrapper می‌سازد که نسخه‌ی اسکریپت را از فایل‌های اپ
   * (مسیر /ghostide/files/shell/ داخل proot) اجرا می‌کند؛ یعنی با هر آپدیت اپ، کاربر همیشه آخرین
   * نسخه‌ی اسکریپت‌ها را می‌گیرد.
   */
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

  private void writeCommandWhenReady(TerminalSession session, String command) {
    if (session == null || command == null || command.isEmpty()) return;
    b.terminalView.post(
        new Runnable() {
          @Override
          public void run() {
            if (session.getEmulator() != null) {
              session.write(command + "\n");
            } else {
              b.terminalView.postDelayed(this, 100);
            }
          }
        });
  }

  private void switchToTab(int position) {
    if (service == null) return;
    List<TerminalTab> sessions = service.getSessions();
    if (position < 0 || position >= sessions.size()) return;
    currentTabIndex = position;
    TerminalSession session = sessions.get(position).session;
    TerminalColorsUtil.refreshSession(session);
    b.terminalView.attachSession(session);
    b.terminalView.invalidate();
    tabAdapter.setSelectedPosition(position);
    b.sessionTabs.scrollToPosition(position);
  }

  private void closeTab(int position) {
    if (service == null) return;
    List<TerminalTab> sessions = service.getSessions();
    if (position < 0 || position >= sessions.size()) return;
    TerminalTab tab = sessions.get(position);
    service.removeSession(tab.session);
    tabAdapter.notifyDataSetChanged();

    sessions = service.getSessions();
    if (sessions.isEmpty()) {
      finish();
      return;
    }
    int newIndex = Math.min(position, sessions.size() - 1);
    switchToTab(newIndex);
  }

  @Nullable
  private TerminalSession currentSession() {
    if (service == null) return null;
    List<TerminalTab> sessions = service.getSessions();
    if (currentTabIndex < 0 || currentTabIndex >= sessions.size()) return null;
    return sessions.get(currentTabIndex).session;
  }

  private int indexOfSession(TerminalSession session) {
    if (service == null) return -1;
    List<TerminalTab> sessions = service.getSessions();
    for (int i = 0; i < sessions.size(); i++) {
      if (sessions.get(i).session == session) return i;
    }
    return -1;
  }

  @Override
  public void onTextChanged(TerminalSession session) {
    if (session == currentSession()) b.terminalView.invalidate();
  }

  @Override
  public void onTitleChanged(TerminalSession session) {
    int index = indexOfSession(session);
    if (index >= 0) tabAdapter.notifyItemChanged(index);
  }

  @Override
  public void onSessionFinished(TerminalSession session) {
    tabAdapter.notifyDataSetChanged();
    List<TerminalTab> sessions = service.getSessions();
    if (sessions.isEmpty()) {
      finish();
      return;
    }
    int newIndex = Math.min(Math.max(currentTabIndex, 0), sessions.size() - 1);
    switchToTab(newIndex);
  }

  @Override
  public boolean isCtrlToggled() {
    return ctrlToggled;
  }

  @Override
  public boolean isAltToggled() {
    return altToggled;
  }

  @Override
  public void consumeCtrlToggle() {
    ctrlToggled = false;
    updateModifierButtonStyle(b.keyCtrl, false);
  }

  @Override
  public void consumeAltToggle() {
    altToggled = false;
    updateModifierButtonStyle(b.keyAlt, false);
  }

  private void confirmAndRemoveDebian() {
    new DialogCompat(this)
        .setTitle(getString(R.string.terminal_remove_debian))
        .setMessage(getString(R.string.terminal_confirm_remove_debian_message))
        .setPositiveButton(
            getString(R.string.terminal_action_remove),
            (dialog, which) ->
                DebianBootstrap.uninstall(
                    this,
                    () -> {
                      GhostToast.makeText(
                              this,
                              getString(R.string.terminal_debian_removed),
                              GhostToast.LENGTH_SHORT)
                          .show();
                    }))
        .setNegativeButton(getString(R.string.terminal_action_cancel), null)
        .show();
  }

  private void buildInstallDialogViews() {
    LinearLayout layout = new LinearLayout(this);
    layout.setOrientation(LinearLayout.VERTICAL);
    int pad = (int) (16 * getResources().getDisplayMetrics().density);
    layout.setPadding(pad, pad, pad, pad);

    installStatusText = new TextView(this);
    installStatusText.setText(getString(R.string.terminal_status_starting_download));

    installProgressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
    installProgressBar.setMax(100);
    installProgressBar.setIndeterminate(false);

    layout.addView(installStatusText);
    layout.addView(installProgressBar);

    installDialog =
        new DialogCompat(this)
            .setTitle(getString(R.string.terminal_install_debian))
            .setView(layout)
            .setCancelable(false)
            .setNegativeButton(
                getString(R.string.terminal_action_cancel),
                (dialog, which) -> {
                  DebianInstaller.cancelInstall();
                  GhostToast.makeText(
                          this,
                          getString(R.string.terminal_install_cancelled),
                          GhostToast.LENGTH_SHORT)
                      .show();
                })
            .create();
    installDialog.show();
  }

  private DebianInstaller.InstallListener getOrCreateInstallListener() {
    if (installListener != null) return installListener;
    installListener =
        new DebianInstaller.InstallListener() {
          @Override
          public void onDownloadProgress(int percent) {
            runOnUiThread(
                () -> {
                  installStatusText.setText(
                      getString(R.string.terminal_status_downloading, percent));
                  installProgressBar.setIndeterminate(false);
                  installProgressBar.setProgress(percent);
                });
          }

          @Override
          public void onExtractProgress(int extractedEntries) {
            runOnUiThread(
                () -> {
                  installStatusText.setText(
                      getString(R.string.terminal_status_extracting, extractedEntries));
                  installProgressBar.setIndeterminate(true);
                });
          }

          @Override
          public void onSuccess() {
            runOnUiThread(
                () -> {
                  if (installDialog != null) installDialog.dismiss();
                  GhostToast.makeText(
                          TerminalActivity.this,
                          getString(R.string.terminal_debian_installed_success),
                          GhostToast.LENGTH_LONG)
                      .show();
                  b.terminalView.setVisibility(View.VISIBLE);
                  bindServiceAndStart();
                });
          }

          @Override
          public void onError(String message) {
            runOnUiThread(
                () -> {
                  if (installDialog != null) installDialog.dismiss();
                  GhostToast.makeText(TerminalActivity.this, message, GhostToast.LENGTH_LONG)
                      .show();
                });
          }
        };
    return installListener;
  }

  private void startDebianInstall() {
    buildInstallDialogViews();
    DebianInstaller.installDebian(this, getOrCreateInstallListener());
  }

  private void attachToRunningInstall() {
    buildInstallDialogViews();
    DebianInstaller.attach(getOrCreateInstallListener());
  }

  private static int fallback(Integer value, int def) {
    return value != null ? value : def;
  }
}
