package ir.hanzodev1375.ghostide.terminal.sheet;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.termux.terminal.TerminalSession;
import ir.hanzodev1375.components.sheet.BaseBlurBottomSheet;
import ir.hanzodev1375.ghostide.R;
import ir.hanzodev1375.ghostide.codeeditors.setting.PreferencesUtils;
import ir.hanzodev1375.ghostide.terminal.TerminalColorsUtil;
import ir.theme.ThemeUtils;
import ir.theme.ThemeManager;
import ir.hanzodev1375.ghostide.databinding.ActivityTerminalBinding;
import ir.hanzodev1375.ghostide.terminal.DebianBootstrap;
import ir.hanzodev1375.ghostide.terminal.DebianInstaller;
import ir.hanzodev1375.ghostide.terminal.GhostTerminalSessionClient;
import ir.hanzodev1375.ghostide.terminal.GhostTerminalViewClient;
import ir.hanzodev1375.ghostide.terminal.ProotSessionFactory;
import ir.hanzodev1375.ghostide.terminal.TerminalInputDock;
import ir.hanzodev1375.ghostide.terminal.TerminalSessionFactory;
import ir.hanzodev1375.ghostide.terminal.TerminalTab;
import ir.hanzodev1375.ghostide.terminal.adapters.TerminalTabAdapter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class TerminalBottomSheetFragment extends BaseBlurBottomSheet
    implements GhostTerminalViewClient.KeyModifierState {

  public static final String EXTRA_WORKING_DIR = "working_dir";
  public static final String EXTRA_COMMAND = "command";

  private ActivityTerminalBinding terminalBinding;
  private final List<TerminalTab> sessions = new ArrayList<>();
  private int currentTabIndex = -1;
  private int nextSessionId = 1;

  private boolean ctrlToggled = false;
  private boolean altToggled = false;
  private int defaultKeyBackgroundColor;
  private int defaultKeyTextColor;
  private TerminalInputDock inputDock;

  private AlertDialog installDialog;
  private TextView installStatusText;
  private ProgressBar installProgressBar;
  private DebianInstaller.InstallListener installListener;
  private TerminalTabAdapter tabAdapter;

  private final GhostTerminalSessionClient.Callback internalCallback =
      new GhostTerminalSessionClient.Callback() {
        @Override
        public void onTextChanged(TerminalSession session) {
          if (session == currentSession()) terminalBinding.terminalView.invalidate();
        }

        @Override
        public void onTitleChanged(TerminalSession session) {
          int index = indexOfSession(session);
          if (index >= 0 && tabAdapter != null) tabAdapter.notifyItemChanged(index);
        }

        @Override
        public void onSessionFinished(TerminalSession session) {
          removeSession(session);
        }
      };

  public static TerminalBottomSheetFragment newInstance(
      @Nullable String command, @Nullable String workingDir) {
    TerminalBottomSheetFragment fragment = new TerminalBottomSheetFragment();
    Bundle args = new Bundle();
    if (command != null) args.putString(EXTRA_COMMAND, command);
    if (workingDir != null) args.putString(EXTRA_WORKING_DIR, workingDir);
    fragment.setArguments(args);
    return fragment;
  }

  @Override
  protected void onContentReady(ViewGroup contentContainer) {
    terminalBinding = ActivityTerminalBinding.inflate(getLayoutInflater(), contentContainer, false);
    contentContainer.addView(terminalBinding.getRoot());
    terminalBinding.getRoot().setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
    setupTerminalView();
    setupExtraKeys();
    setupInputDock();
    maybeRequestNotificationPermission();
    initializeTerminal();
    setupBackgroundBlur();
    setHasPeekMod(false);
  }

  private void setupInputDock() {
    inputDock = new TerminalInputDock(terminalBinding, this::currentSession);
    inputDock.attach();
    inputDock.attachKeyboardWatcher(requireDialog().getWindow().getDecorView());
  }

  @Override
  public void onDestroyView() {
    for (TerminalTab tab : sessions) {
      tab.session.finishIfRunning();
    }
    sessions.clear();
    super.onDestroyView();
  }

  @Override
  public void onStop() {
    DebianInstaller.detach(installListener);
    super.onStop();
  }

  private void initializeTerminal() {
    if (DebianBootstrap.isInstalled(requireContext())) {
      createInitialSession();
      return;
    }
    if (DebianInstaller.isInstalling()) {
      terminalBinding.terminalView.setVisibility(View.INVISIBLE);
      attachToRunningInstall();
      return;
    }
    terminalBinding.terminalView.setVisibility(View.INVISIBLE);
    startDebianInstall();
  }

  private void maybeRequestNotificationPermission() {
    if (Build.VERSION.SDK_INT >= 33) {
      if (ContextCompat.checkSelfPermission(
              requireContext(), Manifest.permission.POST_NOTIFICATIONS)
          != PackageManager.PERMISSION_GRANTED) {
        requestPermissions(new String[] {Manifest.permission.POST_NOTIFICATIONS}, 4821);
      }
    }
  }

  private void createInitialSession() {
    String command = getArguments().getString(EXTRA_COMMAND);
    if (command != null && !command.isEmpty()) {
      addNewDebianSession();
      getArguments().remove(EXTRA_COMMAND);
      return;
    }
    if (sessions.isEmpty()) {
      addNewDebianSession();
    } else {
      switchToTab(sessions.size() - 1);
    }
  }

  private void setupBackgroundBlur() {
    var appsetting = new PreferencesUtils(getContext());
   var themeutil = new ThemeUtils(new ThemeManager(getContext()));

    if (!appsetting.isBlurMod()) {
      return;
    }

    var theme = themeutil.getTheme();
    if (theme == null || theme.getWidget() == null) return;
    var widget = theme.getWidget();
    if (widget.getImagepath() == null || widget.getImagepath().isEmpty()) return;

    terminalBinding.toolbar.setBackgroundColor(Color.TRANSPARENT);
    terminalBinding.sessionTabsRow.setBackgroundColor(Color.TRANSPARENT);
    terminalBinding.inputDock.setBackgroundColor(Color.TRANSPARENT);
    terminalBinding.inputDock.setElevation(0f);
    terminalBinding.terminalView.setBackgroundColor(Color.TRANSPARENT);
    themeutil.applyImageBackground(terminalBinding.backgroundIconTerminal);
  }

  private void setupTerminalView() {
    terminalBinding.terminalView.setTerminalViewClient(
        new GhostTerminalViewClient(terminalBinding.terminalView, this));
    TerminalColorsUtil.apply(getActivity(), requireContext());
    terminalBinding.terminalView.setBackgroundColor(Color.TRANSPARENT);
  }

  private void setupExtraKeys() {
    defaultKeyBackgroundColor =
        terminalBinding.keyCtrl.getBackgroundTintList() != null
            ? terminalBinding.keyCtrl.getBackgroundTintList().getDefaultColor()
            : MaterialColors.getColor(terminalBinding.keyCtrl, R.attr.colorSecondaryContainer);
    defaultKeyTextColor = terminalBinding.keyCtrl.getCurrentTextColor();

    terminalBinding.keyEsc.setOnClickListener(v -> sendKeyEvent(KeyEvent.KEYCODE_ESCAPE));
    terminalBinding.keyTab.setOnClickListener(v -> sendKeyEvent(KeyEvent.KEYCODE_TAB));
    terminalBinding.keyUp.setOnClickListener(v -> sendKeyEvent(KeyEvent.KEYCODE_DPAD_UP));
    terminalBinding.keyDown.setOnClickListener(v -> sendKeyEvent(KeyEvent.KEYCODE_DPAD_DOWN));
    terminalBinding.keyLeft.setOnClickListener(v -> sendKeyEvent(KeyEvent.KEYCODE_DPAD_LEFT));
    terminalBinding.keyRight.setOnClickListener(v -> sendKeyEvent(KeyEvent.KEYCODE_DPAD_RIGHT));
    terminalBinding.keySlash.setOnClickListener(v -> typeText("/"));
    terminalBinding.keyDash.setOnClickListener(v -> typeText("-"));
    terminalBinding.keyPipe.setOnClickListener(v -> typeText("|"));
    terminalBinding.keyCtrl.setOnClickListener(
        v -> {
          ctrlToggled = !ctrlToggled;
          updateModifierButtonStyle(terminalBinding.keyCtrl, ctrlToggled);
        });
    terminalBinding.keyAlt.setOnClickListener(
        v -> {
          altToggled = !altToggled;
          updateModifierButtonStyle(terminalBinding.keyAlt, altToggled);
        });
  }

  private void updateModifierButtonStyle(android.widget.Button button, boolean active) {
    if (active) {
      int bg = MaterialColors.getColor(button, R.attr.colorPrimary);
      int fg = MaterialColors.getColor(button, R.attr.colorOnPrimary);
      button.setBackgroundTintList(ColorStateList.valueOf(bg));
      button.setTextColor(fg);
    } else {
      button.setBackgroundTintList(ColorStateList.valueOf(defaultKeyBackgroundColor));
      button.setTextColor(defaultKeyTextColor);
    }
  }

  private void sendKeyEvent(int keyCode) {
    long now = SystemClock.uptimeMillis();
    terminalBinding.terminalView.dispatchKeyEvent(
        new KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0));
    terminalBinding.terminalView.dispatchKeyEvent(
        new KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0));
  }

  private void typeText(String text) {
    TerminalSession session = currentSession();
    if (session == null) return;
    byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
    session.write(bytes, 0, bytes.length);
  }

  private void addNewSession() {
    GhostTerminalSessionClient client =
        new GhostTerminalSessionClient(requireContext(), internalCallback);
    String workingDir = getArguments().getString(EXTRA_WORKING_DIR);
    TerminalSession session =
        TerminalSessionFactory.createSession(requireContext(), workingDir, client);
    TerminalTab tab = new TerminalTab(nextSessionId++, session);
    sessions.add(tab);
    if (tabAdapter == null) setupSessionTabs();
    else tabAdapter.notifyDataSetChanged();
    switchToTab(sessions.size() - 1);
  }

  private void addNewDebianSession() {
    GhostTerminalSessionClient client =
        new GhostTerminalSessionClient(requireContext(), internalCallback);
    TerminalSession session =
        ProotSessionFactory.createProotSession(
            requireContext(), DebianBootstrap.getRootfsDir(requireContext()), "/bin/bash", client);
    TerminalTab tab = new TerminalTab(nextSessionId++, session);
    sessions.add(tab);
    if (tabAdapter == null) setupSessionTabs();
    else tabAdapter.notifyDataSetChanged();
    switchToTab(sessions.size() - 1);

    String command = getArguments().getString(EXTRA_COMMAND);
    if (command != null && !command.isEmpty()) {
      writeCommandWhenReady(session, command);
    }
  }

  /** وقتی ویو هنوز layout نشده، emulator تا اولین onSizeChanged ساخته نمی‌شه و write() از دست می‌ره؛ پس صبر می‌کنیم. */
  private void writeCommandWhenReady(TerminalSession session, String command) {
    if (session == null || command == null || command.isEmpty()) return;
    terminalBinding.terminalView.post(
        new Runnable() {
          @Override
          public void run() {
            if (session.getEmulator() != null) {
              session.write(command + "\n");
            } else {
              terminalBinding.terminalView.postDelayed(this, 100);
            }
          }
        });
  }

  private void removeSession(TerminalSession session) {
    for (int i = 0; i < sessions.size(); i++) {
      if (sessions.get(i).session == session) {
        sessions.remove(i);
        if (sessions.isEmpty()) {
          dismiss();
          return;
        }
        int newIndex = Math.min(i, sessions.size() - 1);
        if (tabAdapter != null) tabAdapter.notifyDataSetChanged();
        switchToTab(newIndex);
        return;
      }
    }
  }

  private void switchToTab(int position) {
    if (position < 0 || position >= sessions.size()) return;
    currentTabIndex = position;
    TerminalSession session = sessions.get(position).session;
    TerminalColorsUtil.refreshSession(session);
    terminalBinding.terminalView.attachSession(session);
    terminalBinding.terminalView.invalidate();
    if (tabAdapter != null) {
      tabAdapter.setSelectedPosition(position);
      terminalBinding.sessionTabs.scrollToPosition(position);
    }
  }

  private int indexOfSession(TerminalSession session) {
    for (int i = 0; i < sessions.size(); i++) {
      if (sessions.get(i).session == session) return i;
    }
    return -1;
  }

  @Nullable
  private TerminalSession currentSession() {
    if (currentTabIndex < 0 || currentTabIndex >= sessions.size()) return null;
    return sessions.get(currentTabIndex).session;
  }

  private void setupSessionTabs() {
    tabAdapter =
        new TerminalTabAdapter(
            sessions,
            new TerminalTabAdapter.Listener() {
              @Override
              public void onTabSelected(int position) {
                switchToTab(position);
              }

              @Override
              public void onTabClosed(int position) {
                TerminalTab tab = sessions.get(position);
                tab.session.finishIfRunning();
                sessions.remove(position);
                if (sessions.isEmpty()) {
                  dismiss();
                  return;
                }
                int newIndex = Math.min(position, sessions.size() - 1);
                tabAdapter.notifyDataSetChanged();
                switchToTab(newIndex);
              }
            });
    terminalBinding.sessionTabs.setLayoutManager(
        new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
    terminalBinding.sessionTabs.setAdapter(tabAdapter);
    terminalBinding.btnNewSession.setOnClickListener(this::showNewSessionMenu);
  }

  private void showNewSessionMenu(View anchor) {
    if (!DebianBootstrap.isInstalled(requireContext())) {
      Toast.makeText(
              requireContext(),
              getString(
                  R.string.terminal_debian_rootfs_not_found_fallback,
                  DebianBootstrap.getRootfsDir(requireContext()).getAbsolutePath()),
              Toast.LENGTH_LONG)
          .show();
      addNewSession();
      return;
    }
    PopupMenu popup = new PopupMenu(requireContext(), anchor);
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
    updateModifierButtonStyle(terminalBinding.keyCtrl, false);
  }

  @Override
  public void consumeAltToggle() {
    altToggled = false;
    updateModifierButtonStyle(terminalBinding.keyAlt, false);
  }

  private void buildInstallDialogViews() {
    LinearLayout layout = new LinearLayout(requireContext());
    layout.setOrientation(LinearLayout.VERTICAL);
    int pad = (int) (16 * requireContext().getResources().getDisplayMetrics().density);
    layout.setPadding(pad, pad, pad, pad);

    installStatusText = new TextView(requireContext());
    installStatusText.setText(getString(R.string.terminal_status_starting_download));

    installProgressBar =
        new ProgressBar(requireContext(), null, android.R.attr.progressBarStyleHorizontal);
    installProgressBar.setMax(100);
    installProgressBar.setIndeterminate(false);

    layout.addView(installStatusText);
    layout.addView(installProgressBar);

    installDialog =
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.terminal_install_debian))
            .setView(layout)
            .setCancelable(false)
            .setNegativeButton(
                getString(R.string.terminal_action_cancel),
                (dialog, which) -> {
                  DebianInstaller.cancelInstall();
                  Toast.makeText(
                          requireContext(),
                          getString(R.string.terminal_install_cancelled),
                          Toast.LENGTH_SHORT)
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
            if (getView() == null) return;
            requireActivity()
                .runOnUiThread(
                    () -> {
                      installStatusText.setText(
                          getString(R.string.terminal_status_downloading, percent));
                      installProgressBar.setIndeterminate(false);
                      installProgressBar.setProgress(percent);
                    });
          }

          @Override
          public void onExtractProgress(int extractedEntries) {
            if (getView() == null) return;
            requireActivity()
                .runOnUiThread(
                    () -> {
                      installStatusText.setText(
                          getString(R.string.terminal_status_extracting, extractedEntries));
                      installProgressBar.setIndeterminate(true);
                    });
          }

          @Override
          public void onSuccess() {
            if (getView() == null) return;
            requireActivity()
                .runOnUiThread(
                    () -> {
                      if (installDialog != null) installDialog.dismiss();
                      Toast.makeText(
                              requireContext(),
                              getString(R.string.terminal_debian_installed_success),
                              Toast.LENGTH_LONG)
                          .show();
                      terminalBinding.terminalView.setVisibility(View.VISIBLE);
                      createInitialSession();
                    });
          }

          @Override
          public void onError(String message) {
            if (getView() == null) return;
            requireActivity()
                .runOnUiThread(
                    () -> {
                      if (installDialog != null) installDialog.dismiss();
                      Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                    });
          }
        };
    return installListener;
  }

  private void startDebianInstall() {
    buildInstallDialogViews();
    DebianInstaller.installDebian(requireContext(), getOrCreateInstallListener());
  }

  private void attachToRunningInstall() {
    buildInstallDialogViews();
    DebianInstaller.attach(getOrCreateInstallListener());
  }
}
