package ir.hanzodev1375.ghostide.terminal.sheet;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.MainThread;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.app.ActivityCompat;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import android.widget.FrameLayout;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import ir.hanzodev1375.ghostide.R;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.bumptech.glide.Glide;
import com.google.android.material.color.MaterialColors;
import com.termux.terminal.TerminalSession;
import ir.hanzodev1375.ghostide.activity.BaseCompat;
import ir.hanzodev1375.ghostide.codeeditors.setting.PreferencesUtils;
import ir.hanzodev1375.ghostide.databinding.ActivityTerminalBinding;
import ir.hanzodev1375.ghostide.terminal.DebianBootstrap;
import ir.hanzodev1375.ghostide.terminal.DebianInstaller;
import ir.hanzodev1375.ghostide.terminal.GhostTerminalViewClient;
import ir.hanzodev1375.ghostide.terminal.TerminalSessionService;
import ir.hanzodev1375.ghostide.terminal.TerminalTab;
import ir.hanzodev1375.ghostide.terminal.adapters.TerminalTabAdapter;
import ir.hanzodev1375.ghostide.utils.BlurTransformation;
import ir.theme.ThemeManager;
import ir.theme.ThemeUtils;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class TerminalBottomSheetFragment extends BottomSheetDialogFragment
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

  private final ServiceConnection connection =
      new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binderObj) {
          service = ((TerminalSessionService.LocalBinder) binderObj).getService();
          isBound = true;
          service.setUiListener(TerminalBottomSheetFragment.this);
          onServiceReady();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
          isBound = false;
          service = null;
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
  @MainThread
  @Nullable
  public View onCreateView(LayoutInflater arg0, ViewGroup arg1, Bundle arg2) {
    b = ActivityTerminalBinding.inflate(getLayoutInflater());
    return b.getRoot();
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    setupToolbar();
    setupTerminalView();
    setupExtraKeys();
    setupBackHandler();
    setupBackgroundBlur();
    maybeRequestNotificationPermission();
  }

  @Override
  public void onStart() {
    super.onStart();
    Intent serviceIntent = new Intent(requireContext(), TerminalSessionService.class);
    ContextCompat.startForegroundService(requireContext(), serviceIntent);
    requireContext().bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE);
  }

  @Override
  public void onStop() {
    if (isBound) {
      service.setUiListener(null);
      requireContext().unbindService(connection);
      isBound = false;
    }
    super.onStop();
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

  private void onServiceReady() {
    if (tabAdapter == null) setupSessionTabs();

    String command = getArguments().getString(EXTRA_COMMAND);
    if (command != null && !command.isEmpty()) {
      if (!DebianBootstrap.isInstalled(requireContext())) {
        Toast.makeText(requireContext(), "Debian نصب نیست", Toast.LENGTH_LONG).show();
        return;
      }
      addNewDebianSession();
      getArguments().remove(EXTRA_COMMAND);
      return;
    }

    List<TerminalTab> sessions = service.getSessions();
    if (sessions.isEmpty()) {
      stepDB();
    } else {
      int index =
          (currentTabIndex >= 0 && currentTabIndex < sessions.size())
              ? currentTabIndex
              : sessions.size() - 1;
      switchToTab(index);
    }
  }

  private void setupBackgroundBlur() {
    appsetting = new PreferencesUtils(requireContext());
    themeutil = new ThemeUtils(new ThemeManager(requireContext()));

    if (!appsetting.isShowBackground()) {
      return;
    }

    var theme = themeutil.getTheme();
    if (theme == null || theme.getWidget() == null) return;
    var widget = theme.getWidget();
    if (widget.getImagepath() == null || widget.getImagepath().isEmpty()) return;
    b.sessionTabsRow.setBackgroundColor(Color.parseColor(widget.getAccent()));
    b.extraKeysScroll.setBackgroundColor(Color.parseColor(widget.getAccent()));
    b.terminalView.setBackgroundColor(Color.TRANSPARENT);
    b.keyAlt.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(widget.getHint())));
    b.keyCtrl.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(widget.getHint())));
    b.keyDash.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(widget.getHint())));
    b.keyDown.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(widget.getHint())));
    b.keyEsc.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(widget.getHint())));
    b.keyLeft.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(widget.getHint())));
    b.keyRight.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(widget.getHint())));
    b.keyTab.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(widget.getHint())));
    b.keyUp.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(widget.getHint())));
    b.keyPipe.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(widget.getHint())));
    b.keySlash.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(widget.getHint())));
    b.backgroundIconTerminal.setVisibility(View.VISIBLE);
    Glide.with(this)
        .load(widget.getImagepath())
        .transform(new BlurTransformation((int) widget.getBlursize()))
        .into(b.backgroundIconTerminal);
    var dialog = getDialog();
    if (!(dialog instanceof BottomSheetDialog)) return;
    BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialog;
    FrameLayout bottomSheet =
        bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
    if (bottomSheet == null) return;
    BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
    bottomSheet.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
  }

  private void setupToolbar() {
    b.toolbar.setVisibility(View.GONE);
  }

  private void setupTerminalView() {
    b.terminalView.setTerminalViewClient(new GhostTerminalViewClient(b.terminalView, this));
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
        new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
    b.sessionTabs.setAdapter(tabAdapter);
    b.btnNewSession.setOnClickListener(this::showNewSessionMenu);
  }

  private void stepDB() {
    if (!DebianInstaller.isInstalled(requireContext())) {
      startDebianInstall();
      b.terminalView.setVisibility(View.INVISIBLE);
    } else {
      b.terminalView.setVisibility(View.VISIBLE);
      addNewDebianSession();
    }
  }

  private void showNewSessionMenu(View anchor) {
    if (!DebianBootstrap.isInstalled(requireContext())) {
      Toast.makeText(
              requireContext(),
              "Debian rootfs پیدا نشد رو: "
                  + DebianBootstrap.getRootfsDir(requireContext()).getAbsolutePath()
                  + " — یه شل معمولی باز میشه",
              Toast.LENGTH_LONG)
          .show();
      addNewSession();
      return;
    }
    var popup = new PopupMenu(requireContext(), anchor);
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
            : MaterialColors.getColor(
                b.keyCtrl, com.google.android.material.R.attr.colorSecondaryContainer);
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
      button.setBackgroundTintList(ColorStateList.valueOf(defaultKeyBackgroundColor));
      button.setTextColor(defaultKeyTextColor);
    }
  }

  private void setupBackHandler() {}

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
    String workingDir = getArguments().getString(EXTRA_WORKING_DIR);
    service.createSession(workingDir);
    tabAdapter.notifyDataSetChanged();
    switchToTab(service.getSessions().size() - 1);
  }

  private void addNewDebianSession() {
    if (service == null) return;
    service.createDebianSession();
    tabAdapter.notifyDataSetChanged();
    switchToTab(service.getSessions().size() - 1);

    String command = getArguments().getString(EXTRA_COMMAND);
    if (command != null && !command.isEmpty()) {
      TerminalSession session = currentSession();
      session.write(command + "\n");
    }
  }

  private void switchToTab(int position) {
    if (service == null) return;
    List<TerminalTab> sessions = service.getSessions();
    if (position < 0 || position >= sessions.size()) return;
    currentTabIndex = position;
    b.terminalView.attachSession(sessions.get(position).session);
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
      dismiss();
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
      dismiss();
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

  private AlertDialog installDialog;
  private TextView installStatusText;
  private ProgressBar installProgressBar;

  @Override
  public void onCreateOptionsMenu(Menu menu, android.view.MenuInflater inflater) {
    menu.add(
        0, 1001, 0, DebianBootstrap.isInstalled(requireContext()) ? "حذف Debian" : "نصب Debian");
    super.onCreateOptionsMenu(menu, inflater);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == 1001) {
      if (DebianBootstrap.isInstalled(requireContext())) {
        confirmAndRemoveDebian();
      } else {
        startDebianInstall();
      }
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  private void confirmAndRemoveDebian() {
    new AlertDialog.Builder(requireContext())
        .setTitle("حذف Debian")
        .setMessage("کل rootfs دبیان پاک میشه (فایل های خودِ توی دبیان هم از بین میره). مطمئنی؟")
        .setPositiveButton(
            "حذف کن",
            (dialog, which) ->
                DebianBootstrap.uninstall(
                    requireContext(),
                    () -> {
                      Toast.makeText(requireContext(), "Debian حذف شد", Toast.LENGTH_SHORT).show();
                      requireActivity().invalidateOptionsMenu();
                    }))
        .setNegativeButton("لغو", null)
        .show();
  }

  private void startDebianInstall() {
    LinearLayout layout = new LinearLayout(requireContext());
    layout.setOrientation(LinearLayout.VERTICAL);
    int pad = (int) (16 * requireContext().getResources().getDisplayMetrics().density);
    layout.setPadding(pad, pad, pad, pad);

    installStatusText = new TextView(requireContext());
    installStatusText.setText("در حال شروع دانلود...");

    installProgressBar =
        new ProgressBar(requireContext(), null, android.R.attr.progressBarStyleHorizontal);
    installProgressBar.setMax(100);
    installProgressBar.setIndeterminate(false);

    layout.addView(installStatusText);
    layout.addView(installProgressBar);

    installDialog =
        new AlertDialog.Builder(requireContext())
            .setTitle("نصب Debian")
            .setView(layout)
            .setCancelable(false)
            .setNegativeButton(
                "لغو",
                (dialog, which) -> {
                  DebianInstaller.cancelInstall();
                  Toast.makeText(requireContext(), "نصب لغو شد", Toast.LENGTH_SHORT).show();
                })
            .create();
    installDialog.show();

    DebianInstaller.installDebian(
        requireContext(),
        new DebianInstaller.InstallListener() {
          @Override
          public void onDownloadProgress(int percent) {
            if (getView() == null) return;
            requireActivity()
                .runOnUiThread(
                    () -> {
                      installStatusText.setText("دانلود: " + percent + "%");
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
                          "در حال استخراج... (" + extractedEntries + " فایل)");
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
                      Toast.makeText(requireContext(), "Debian نصب شد ✓", Toast.LENGTH_LONG).show();
                      requireActivity().invalidateOptionsMenu();
                      b.terminalView.setVisibility(View.VISIBLE);
                      if (service != null) {
                        addNewDebianSession();
                      }
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
        });
  }
}
