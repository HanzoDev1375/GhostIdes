package ir.hanzodev1375.ghostide.terminal.activity;

import android.os.Bundle;
import android.os.SystemClock;
import android.view.KeyEvent;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.termux.terminal.TerminalSession;
import ir.hanzodev1375.ghostide.activity.BaseCompat;
import ir.hanzodev1375.ghostide.databinding.ActivityTerminalBinding;
import ir.hanzodev1375.ghostide.terminal.GhostTerminalSessionClient;
import ir.hanzodev1375.ghostide.terminal.GhostTerminalViewClient;
import ir.hanzodev1375.ghostide.terminal.TerminalSessionFactory;
import ir.hanzodev1375.ghostide.terminal.TerminalTab;
import ir.hanzodev1375.ghostide.terminal.adapters.TerminalTabAdapter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * ترمینال مستقل GhostIDE. چند سشن به‌صورت تب (مثل مرورگر) پشتیبانی میشه؛ همه‌ی سشن‌ها فقط تا وقتی
 * این اکتیویتی زنده‌ست وجود دارن (بدون Service پس‌زمینه) — با بستن صفحه، همه‌ی پروسه‌های شل با
 * finishIfRunning() کشته میشن.
 *
 * <p>برای اجرا نیاز به این dependency در build.gradle ماژول app هست (که خودم نمی‌تونم اضافه کنم چون
 * build.gradle توی پروژه‌ای که برام فرستادی نبود):
 *
 * <p>repositories { maven { url "https://jitpack.io" } } dependencies { implementation
 * 'com.termux.termux-app:terminal-view:0.118.3' }
 */
public class TerminalActivity extends BaseCompat
    implements GhostTerminalSessionClient.Callback, GhostTerminalViewClient.KeyModifierState {

  /** اگه بخوای ترمینال رو مستقیم توی مسیر یه پروژه باز کنی: putExtra(EXTRA_WORKING_DIR, path) */
  public static final String EXTRA_WORKING_DIR = "working_dir";

  private ActivityTerminalBinding b;
  private final List<TerminalTab> tabs = new ArrayList<>();
  private TerminalTabAdapter tabAdapter;
  private GhostTerminalSessionClient sessionClient;
  private int nextSessionId = 1;
  private int currentTabIndex = -1;

  private boolean ctrlToggled = false;
  private boolean altToggled = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    b = ActivityTerminalBinding.inflate(getLayoutInflater());
    setContentView(b.getRoot());

    setupToolbar();
    setupTerminalView();
    setupSessionTabs();
    setupExtraKeys();
    setupBackHandler();

    addNewSession();
  }

  private void setupToolbar() {
    setSupportActionBar(b.toolbar);
    if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    b.toolbar.setNavigationOnClickListener(v -> finish());
  }

  private void setupTerminalView() {
    sessionClient = new GhostTerminalSessionClient(this, this);
    b.terminalView.setTerminalViewClient(new GhostTerminalViewClient(b.terminalView, this));
  }

  private void setupSessionTabs() {
    tabAdapter =
        new TerminalTabAdapter(
            tabs,
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
    b.btnNewSession.setOnClickListener(v -> addNewSession());
  }

  private void setupExtraKeys() {
    b.keyEsc.setOnClickListener(v -> sendKeyEvent(KeyEvent.KEYCODE_ESCAPE));
    b.keyTab.setOnClickListener(v -> sendKeyEvent(KeyEvent.KEYCODE_TAB));
    b.keyUp.setOnClickListener(v -> sendKeyEvent(KeyEvent.KEYCODE_DPAD_UP));
    b.keyDown.setOnClickListener(v -> sendKeyEvent(KeyEvent.KEYCODE_DPAD_DOWN));
    b.keyLeft.setOnClickListener(v -> sendKeyEvent(KeyEvent.KEYCODE_DPAD_LEFT));
    b.keyRight.setOnClickListener(v -> sendKeyEvent(KeyEvent.KEYCODE_DPAD_RIGHT));
    b.keySlash.setOnClickListener(v -> typeText("/"));
    b.keyDash.setOnClickListener(v -> typeText("-"));
    b.keyPipe.setOnClickListener(v -> typeText("|"));
    b.keyCtrl.setOnCheckedChangeListener((btn, checked) -> ctrlToggled = checked);
    b.keyAlt.setOnCheckedChangeListener((btn, checked) -> altToggled = checked);
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
    String workingDir = getIntent().getStringExtra(EXTRA_WORKING_DIR);
    TerminalSession session = TerminalSessionFactory.createSession(this, workingDir, sessionClient);
    TerminalTab tab = new TerminalTab(nextSessionId++, session);
    tabs.add(tab);
    tabAdapter.notifyItemInserted(tabs.size() - 1);
    switchToTab(tabs.size() - 1);
  }

  private void switchToTab(int position) {
    if (position < 0 || position >= tabs.size()) return;
    currentTabIndex = position;
    b.terminalView.attachSession(tabs.get(position).session);
    tabAdapter.setSelectedPosition(position);
    b.sessionTabs.scrollToPosition(position);
  }

  private void closeTab(int position) {
    if (position < 0 || position >= tabs.size()) return;
    TerminalTab tab = tabs.remove(position);
    tab.session.finishIfRunning();
    tabAdapter.notifyItemRemoved(position);

    if (tabs.isEmpty()) {
      // بستن آخرین تب یعنی دیگه چیزی برای نمایش نیست؛ کل صفحه رو ببند
      finish();
      return;
    }
    int newIndex = Math.min(position, tabs.size() - 1);
    switchToTab(newIndex);
  }

  @Nullable
  private TerminalSession currentSession() {
    if (currentTabIndex < 0 || currentTabIndex >= tabs.size()) return null;
    return tabs.get(currentTabIndex).session;
  }

  private int indexOfSession(TerminalSession session) {
    for (int i = 0; i < tabs.size(); i++) {
      if (tabs.get(i).session == session) return i;
    }
    return -1;
  }

  // ───────────────────────── GhostTerminalSessionClient.Callback ─────────────────────────

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
    int index = indexOfSession(session);
    if (index >= 0) closeTab(index);
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
    b.keyCtrl.setChecked(false);
  }

  @Override
  public void consumeAltToggle() {
    altToggled = false;
    b.keyAlt.setChecked(false);
  }

  @Override
  protected void onDestroy() {
    for (TerminalTab tab : tabs) {
      tab.session.finishIfRunning();
    }
    super.onDestroy();
  }
}
