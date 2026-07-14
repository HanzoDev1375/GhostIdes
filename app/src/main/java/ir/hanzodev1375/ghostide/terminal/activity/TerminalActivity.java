package ir.hanzodev1375.ghostide.terminal.activity;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
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

/**
 * ترمینال مستقل GhostIDE. چند سشن به‌صورت تب (مثل مرورگر) پشتیبانی میشه. خودِ سشن‌ها (پروسه‌ی شل)
 * توی {@link TerminalSessionService} زندگی میکنن، نه این اکتیویتی — یعنی چرخوندن صفحه، رفتن به یه
 * اکتیویتی دیگه، یا حتی بستن این صفحه، سشن‌ها رو نمی‌کشه؛ فقط با × زدن رو تب یا kill کردنِ خودِ
 * notification سرویس از بین میرن.
 *
 * <p>برای اجرا نیاز به این dependency در build.gradle ماژول app هست:
 *
 * <p>repositories { maven { url "https://jitpack.io" } } dependencies { implementation
 * 'com.termux.termux-app:terminal-view:0.118.3' }
 *
 * <p>و حتماً permission/service مربوط به {@link TerminalSessionService} رو توی AndroidManifest.xml
 * اضافه کن (توضیحش بالای اون کلاسه).
 */
public class TerminalActivity extends BaseCompat
    implements GhostTerminalViewClient.KeyModifierState, TerminalSessionService.SessionListener {

  /** اگه بخوای ترمینال رو مستقیم توی مسیر یه پروژه باز کنی: putExtra(EXTRA_WORKING_DIR, path) */
  public static final String EXTRA_WORKING_DIR = "working_dir";

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
          service.setUiListener(TerminalActivity.this);
          onServiceReady();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
          isBound = false;
          service = null;
        }
      };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    b = ActivityTerminalBinding.inflate(getLayoutInflater());
    setContentView(b.getRoot());

    setupToolbar();
    setupEdgeToEdgeInsets();
    setupTerminalView();
    setupExtraKeys();
    setupBackHandler();
    setupBackgroundBlur();
    maybeRequestNotificationPermission();
    getWindow()
        .getDecorView()
        .setBackgroundColor(MaterialColors.getColor(this, R.attr.colorSurfaceContainerHigh, 0));
  }

  @Override
  protected void onStart() {
    super.onStart();
    Intent serviceIntent = new Intent(this, TerminalSessionService.class);
    ContextCompat.startForegroundService(this, serviceIntent);
    bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE);
  }

  @Override
  protected void onStop() {
    if (isBound) {
      service.setUiListener(null);
      unbindService(connection);
      isBound = false;
    }
    super.onStop();
  }

  private void maybeRequestNotificationPermission() {
    if (Build.VERSION.SDK_INT >= 33) {
      if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
          != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(
            this, new String[] {android.Manifest.permission.POST_NOTIFICATIONS}, 4821);
      }
    }
  }

  /** بعد از اولین bind موفق به سرویس صدا زده میشه: آدابتور تب‌ها رو با لیست واقعیِ سرویس میسازه. */
  private void onServiceReady() {
    if (tabAdapter == null) setupSessionTabs();
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

  /**
   * چون BaseCompat اینجا edge-to-edge رو خودش برای این صفحه هندل نمیکنه (مثل EditorActivity)، صریح
   * پدینگ میدیم — ولی به‌جای ریشه (که الان CoordinatorLayout با ImageView بک‌گراند تمام‌صفحه‌ست)،
   * فقط به تولبار (بالا) و ردیف extra-keys (پایین) پدینگ میدیم؛ این‌جوری بک‌گراند بلور‌شده
   * edge-to-edge باقی می‌مونه، دقیقاً مثل FileManagerActivity.setupInsets().
   */
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
          b.extraKeysScroll.setPadding(
              b.extraKeysScroll.getPaddingLeft(),
              b.extraKeysScroll.getPaddingTop(),
              b.extraKeysScroll.getPaddingRight(),
              Math.max(systemBars.bottom, ime.bottom));
          return insets;
        });
  }

  /**
   * دقیقاً همون مکانیزمِ FileManagerActivity: اگه کاربر از تنظیمات "نمایش بک‌گراند" رو روشن کرده
   * باشه (و توی ادیتور تم یه عکس بک‌گراند ست کرده باشه)، همون عکسِ بلورشده رو پشتِ کروم (تولبار،
   * نوار تب‌ها، ردیف extra-keys) نشون میدیم؛ خودِ صفحه‌ی ترمینال (متن) همیشه مات/opaque می‌مونه تا
   * خوانایی خراب نشه. اگه کاربر خاموشش کرده باشه، هیچی تغییر نمیکنه (همون پس‌زمینه‌ی توپُر فعلی).
   */
  private void setupBackgroundBlur() {
    appsetting = new PreferencesUtils(this);
    themeutil = new ThemeUtils(new ThemeManager(this));

    if (!appsetting.isShowBackground()) {
      return; // کاربر نخواسته؛ دست به هیچی نمیزنیم
    }

    var theme = themeutil.getTheme();
    if (theme == null || theme.getWidget() == null) return;
    var widget = theme.getWidget();
    if (widget.getImagepath() == null || widget.getImagepath().isEmpty()) return;

    b.toolbar.setBackgroundColor(android.graphics.Color.TRANSPARENT);
    b.sessionTabsRow.setBackgroundColor(android.graphics.Color.TRANSPARENT);
    b.extraKeysScroll.setBackgroundColor(android.graphics.Color.TRANSPARENT);

    b.backgroundIconTerminal.setVisibility(View.VISIBLE);
    Glide.with(this)
        .load(widget.getImagepath())
        .transform(new BlurTransformation((int) widget.getBlursize()))
        .into(b.backgroundIconTerminal);
  }

  private void setupToolbar() {
    setSupportActionBar(b.toolbar);
    if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    b.toolbar.setNavigationOnClickListener(v -> finish());
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
        new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
    b.sessionTabs.setAdapter(tabAdapter);
    b.btnNewSession.setOnClickListener(this::showNewSessionMenu);
  }

  private void stepDB() {
    if (!DebianInstaller.isInstalled(this)) {
      startDebianInstall();
      b.terminalView.setVisibility(View.INVISIBLE);
    } else {
      b.terminalView.setVisibility(View.VISIBLE);
      addNewDebianSession();
    }
  }

  /** اگه Debian نصب شده باشه، بین Shell/Debian می‌پرسه؛ وگرنه با توضیح، شل ساده میسازه. */
  private void showNewSessionMenu(View anchor) {
    if (!DebianBootstrap.isInstalled(this)) {
      Toast.makeText(
              this,
              "Debian rootfs پیدا نشد رو: "
                  + DebianBootstrap.getRootfsDir(this).getAbsolutePath()
                  + " — یه شل معمولی باز میشه",
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

  /**
   * چون دیگه ToggleButton نیستن (اون ایندیکیتور توکار زشت بود)، حالت on/off رو خودمون رنگ میکنیم.
   */
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

  /**
   * یه KeyEvent واقعی رو از طریق مسیر ورودیِ خودِ TerminalView دیسپچ میکنه (ESC/TAB/جهت‌نما‌ها).
   */
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
    try {
      service.createDebianSession();
      tabAdapter.notifyDataSetChanged();
      switchToTab(service.getSessions().size() - 1);
    } catch (Exception e) {
      android.util.Log.e("GHOST_DEBIAN", "addNewDebianSession failed", e);
      android.widget.Toast.makeText(
              this,
              e.getMessage() == null ? e.toString() : e.getMessage(),
              android.widget.Toast.LENGTH_LONG)
          .show();
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

  // ───────────────────────── TerminalSessionService.SessionListener ─────────────────────────

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
    // سرویس خودش این سشن رو از لیستش حذف کرده؛ فقط UI رو با وضعیت فعلی sync میکنیم
    tabAdapter.notifyDataSetChanged();
    List<TerminalTab> sessions = service.getSessions();
    if (sessions.isEmpty()) {
      finish();
      return;
    }
    int newIndex = Math.min(Math.max(currentTabIndex, 0), sessions.size() - 1);
    switchToTab(newIndex);
  }

  // ───────────────────────── GhostTerminalViewClient.KeyModifierState ─────────────────────────

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

  // ───────────────────────── نصب Debian (منوی تولبار) ─────────────────────────

  private AlertDialog installDialog;
  private TextView installStatusText;
  private ProgressBar installProgressBar;

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    menu.add(0, 1001, 0, DebianBootstrap.isInstalled(this) ? "حذف Debian" : "نصب Debian");
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

  /**
   * قبل از پاک کردنِ rootfs (مثلاً برای رفعِ یه نصبِ ناقص/معماریِ اشتباه) از کاربر تأیید میگیره.
   */
  private void confirmAndRemoveDebian() {
    new AlertDialog.Builder(this)
        .setTitle("حذف Debian")
        .setMessage("کل rootfs دبیان پاک میشه (فایل های خودِ توی دبیان هم از بین میره). مطمئنی؟")
        .setPositiveButton(
            "حذف کن",
            (dialog, which) ->
                DebianBootstrap.uninstall(
                    this,
                    () -> {
                      Toast.makeText(this, "Debian حذف شد", Toast.LENGTH_SHORT).show();
                      invalidateOptionsMenu();
                    }))
        .setNegativeButton("لغو", null)
        .show();
  }

  private void startDebianInstall() {
    LinearLayout layout = new LinearLayout(this);
    layout.setOrientation(LinearLayout.VERTICAL);
    int pad = (int) (16 * getResources().getDisplayMetrics().density);
    layout.setPadding(pad, pad, pad, pad);

    installStatusText = new TextView(this);
    installStatusText.setText("در حال شروع دانلود...");

    installProgressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
    installProgressBar.setMax(100);
    installProgressBar.setIndeterminate(false);

    layout.addView(installStatusText);
    layout.addView(installProgressBar);

    installDialog =
        new AlertDialog.Builder(this)
            .setTitle("نصب Debian")
            .setView(layout)
            .setCancelable(false)
            .setNegativeButton(
                "لغو",
                (dialog, which) -> {
                  DebianInstaller.cancelInstall();
                  Toast.makeText(this, "نصب لغو شد", Toast.LENGTH_SHORT).show();
                })
            .create();
    installDialog.show();

    DebianInstaller.installDebian(
        this,
        new DebianInstaller.InstallListener() {
          @Override
          public void onDownloadProgress(int percent) {
            runOnUiThread(
                () -> {
                  installStatusText.setText("دانلود: " + percent + "%");
                  installProgressBar.setProgress(percent);
                });
          }

          @Override
          public void onExtractProgress(int extractedEntries) {
            runOnUiThread(
                () -> {
                  installStatusText.setText("در حال استخراج... (" + extractedEntries + " فایل)");
                  installProgressBar.setIndeterminate(true);
                });
          }

          @Override
          public void onSuccess() {
            runOnUiThread(
                () -> {
                  if (installDialog != null) installDialog.dismiss();
                  Toast.makeText(TerminalActivity.this, "Debian نصب شد ✓", Toast.LENGTH_LONG)
                      .show();
                  invalidateOptionsMenu();
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
        });
  }
}
