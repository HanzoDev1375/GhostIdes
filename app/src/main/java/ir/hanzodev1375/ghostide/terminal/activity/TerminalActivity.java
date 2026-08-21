package ir.hanzodev1375.ghostide.terminal.activity;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.bumptech.glide.Glide;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
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
import ir.hanzodev1375.ghostide.utils.BlurTransformation;
import ir.theme.ThemeManager;
import ir.theme.ThemeUtils;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class TerminalActivity extends BaseCompat
    implements GhostTerminalViewClient.KeyModifierState, TerminalSessionService.SessionListener {

  public static final String EXTRA_WORKING_DIR = "working_dir";
  public static final String EXTRA_COMMAND = "command";

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

    setupToolbar();
    setupEdgeToEdgeInsets();
    setupTerminalView();
    setupExtraKeys();
    setupInputDock();
    setupBackHandler();
    setupBackgroundBlur();
    maybeRequestNotificationPermission();
    getWindow()
        .getDecorView()
        .setBackgroundColor(MaterialColors.getColor(this, R.attr.colorSurface, 0));
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
        b.backgroundIconTerminal,
        b.toolbar, b.sessionTabsRow, b.inputDock, b.terminalView);
  }

  private void setupToolbar() {
    setSupportActionBar(b.toolbar);
    if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    b.toolbar.setNavigationOnClickListener(v -> finish());
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
      Toast.makeText(
              this,
              getString(
                  R.string.terminal_debian_rootfs_not_found_fallback,
                  DebianBootstrap.getRootfsDir(this).getAbsolutePath()),
              Toast.LENGTH_LONG)
          .show();
      addNewSession();
      return;
    }
    var popup = new PopupMenu(this, anchor);
    popup.getMenu().add(0, 1, 0, "Shell");
    popup.getMenu().add(0, 2, 1, "Debian");
    popup.setOnMenuItemClickListener(
        item -> {
          if (item.getItemId() == 2) {
            addNewDebianSession();
          } else {
            addNewSession();
          }
          return true;
        });
    popup.show();
  }

  private void setupExtraKeys() {
    defaultKeyBackgroundColor =
        b.keyCtrl.getBackgroundTintList() != null
            ? b.keyCtrl.getBackgroundTintList().getDefaultColor()
            : MaterialColors.getColor(b.keyCtrl, R.attr.colorSecondaryContainer);
    defaultKeyTextColor = b.keyCtrl.getCurrentTextColor();

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

  private void updateModifierButtonStyle(android.widget.Button button, boolean active) {
    if (active) {
      int bg = MaterialColors.getColor(button, R.attr.colorPrimary);
      int fg = MaterialColors.getColor(button, R.attr.colorOnPrimary);
      button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(bg));
      button.setTextColor(fg);
    } else {
      button.setBackgroundTintList(
          android.content.res.ColorStateList.valueOf(defaultKeyBackgroundColor));
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

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    menu.add(
        0,
        1001,
        0,
        DebianBootstrap.isInstalled(this)
            ? getString(R.string.terminal_remove_debian)
            : getString(R.string.terminal_install_debian));
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == 1001) {
      if (DebianBootstrap.isInstalled(this)) {
        confirmAndRemoveDebian();
      } else {
        startDebianInstall();
      }
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  private void confirmAndRemoveDebian() {
    new MaterialAlertDialogBuilder(this)
        .setTitle(getString(R.string.terminal_remove_debian))
        .setMessage(getString(R.string.terminal_confirm_remove_debian_message))
        .setPositiveButton(
            getString(R.string.terminal_action_remove),
            (dialog, which) ->
                DebianBootstrap.uninstall(
                    this,
                    () -> {
                      Toast.makeText(
                              this, getString(R.string.terminal_debian_removed), Toast.LENGTH_SHORT)
                          .show();
                      invalidateOptionsMenu();
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
        new MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.terminal_install_debian))
            .setView(layout)
            .setCancelable(false)
            .setNegativeButton(
                getString(R.string.terminal_action_cancel),
                (dialog, which) -> {
                  DebianInstaller.cancelInstall();
                  Toast.makeText(
                          this, getString(R.string.terminal_install_cancelled), Toast.LENGTH_SHORT)
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
                  Toast.makeText(
                          TerminalActivity.this,
                          getString(R.string.terminal_debian_installed_success),
                          Toast.LENGTH_LONG)
                      .show();
                  invalidateOptionsMenu();
                  b.terminalView.setVisibility(View.VISIBLE);
                  bindServiceAndStart();
                });
          }

          @Override
          public void onError(String message) {
            runOnUiThread(
                () -> {
                  if (installDialog != null) installDialog.dismiss();
                  Toast.makeText(TerminalActivity.this, message, Toast.LENGTH_LONG).show();
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
}
