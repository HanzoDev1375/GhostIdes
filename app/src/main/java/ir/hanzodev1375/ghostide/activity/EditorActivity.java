package ir.hanzodev1375.ghostide.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Rect;
import ir.hanzodev1375.ghostide.plugin.gpl.GplManifest;
import ir.hanzodev1375.ghostide.utils.ObjectUtil;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.provider.OpenableColumns;
import android.util.TypedValue;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.Toast;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.bumptech.glide.Glide;
import com.google.android.material.sidesheet.SideSheetDialog;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.skydoves.powermenu.PowerMenuItem;
import com.blankj.utilcode.util.FileIOUtils;
import io.github.rosemoe.sora.event.ContentChangeEvent;
import io.github.rosemoe.sora.lsp.editor.LspEditorStatus;
import ir.hanzodev1375.components.colors.ColorPickerBottomSheet;
import ir.hanzodev1375.filetreelib.widget.FileTreeView;
import ir.hanzodev1375.ghostide.codeeditors.langs.lsp.model.BreadcrumbItem;
import ir.hanzodev1375.ghostide.codeeditors.setting.PreferencesUtils;
import ir.hanzodev1375.ghostide.codeeditors.ui.model.OpenFileLocationEvent;
import ir.hanzodev1375.ghostide.customui.EditorStatusBar;
import ir.hanzodev1375.ghostide.customui.TabCustomView;
import ir.hanzodev1375.ghostide.fragments.MarkDownPreview;
import ir.hanzodev1375.ghostide.jgit.GitHubClient;
import ir.hanzodev1375.ghostide.jgit.GitHubProfileSheet;
import ir.hanzodev1375.ghostide.jgit.fragments.GitBottomSheetFragment;
import ir.hanzodev1375.ghostide.jgit.jgitandroid.datamanager.GitManager;
import ir.hanzodev1375.ghostide.jgit.jgitandroid.model.FileChange;
import ir.hanzodev1375.ghostide.runer.CodeRuner;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import ir.hanzodev1375.ghostide.R;
import ir.hanzodev1375.ghostide.adapters.EditorPagerAdapter;
import ir.hanzodev1375.ghostide.adapters.ToolbarListAdapter;
import ir.hanzodev1375.ghostide.adapters.PluginPopupAdapter;
import ir.hanzodev1375.ghostide.adapters.BreadcrumbAdapter;
import ir.hanzodev1375.ghostide.codeeditors.IdeEditor;
import ir.hanzodev1375.ghostide.customui.GhostIdeEditorSearch;
import ir.hanzodev1375.ghostide.databinding.ActivityEditorBinding;
import ir.hanzodev1375.ghostide.fragments.EditorFragment;
import ir.hanzodev1375.ghostide.models.TabModel;
import ir.hanzodev1375.ghostide.models.ToolbarModel;
import ir.hanzodev1375.ghostide.adapters.EditorHostAdapter;
import ir.hanzodev1375.ghostide.adapters.CodeRunnerHostAdapter;
import ir.hanzodev1375.ghostide.ide.ui.api.EditorPanel;
import ir.hanzodev1375.ghostide.ide.ui.api.CodeRunnerHost;
import ir.hanzodev1375.ghostide.ide.ui.api.FileEvent;
import ir.hanzodev1375.ghostide.ide.ui.api.IdeEvents;
import ir.hanzodev1375.ghostide.ide.ui.api.IdeHostServices;
import ir.hanzodev1375.ghostide.ide.ui.api.PluginUiExtensionPoints;
import ir.hanzodev1375.ghostide.plugin.PluginManager;
import ir.hanzodev1375.ghostide.plugin.PluginPanelHost;
import ir.hanzodev1375.ghostide.plugin.api.Disposable;
import ir.hanzodev1375.ghostide.plugin.api.GlobalRegistry;
import ir.hanzodev1375.ghostide.plugin.gpl.GplInstalledPlugins;
import ir.hanzodev1375.ghostide.plugin.gpl.GplManifestReader;
import ir.hanzodev1375.ghostide.plugin.gpl.GplPluginLoader;
import ir.theme.ThemeManager;
import ir.theme.ThemeUtils;
import android.view.ViewTreeObserver;
import ir.hanzodev1375.ghostide.splitlayout.EditorPaneFragment;
import ir.hanzodev1375.ghostide.splitlayout.SplitLayoutPopup;
import ir.hanzodev1375.ghostide.refactor.rename.FileRenameNotifier;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import ir.hanzodev1375.components.sheet.customitemsheet.ui.DialogCompat;
public class EditorActivity extends BaseCompat implements FileRenameNotifier.Listener {

  private ActivityEditorBinding binding;
  private EditorPagerAdapter adapter;
  private ThemeUtils theme;
  private List<TabModel> tabsList = new ArrayList<>();
  private SharedPreferences prefs;
  private Gson gson = new Gson();
  private static final String KEY_TABS = "path";
  private static final String KEY_POSITION = "positionTabs";
  private static final String KEY_SPLIT_ACTIVE = "splitActive";
  private static final String KEY_SPLIT_ROWS = "splitRows";
  private static final String KEY_SPLIT_COLS = "splitCols";
  private TabLayoutMediator tabMediator;
  private ToolbarListAdapter listAdapter;
  private boolean isShowSys = false;
  private List<ToolbarModel> toolbarModel = new ArrayList<>();
  private final ExecutorService gitStatusExecutor = Executors.newSingleThreadExecutor();
  private String gitStatusRepoPath;
  private Set<String> gitChangedPaths = new HashSet<>();
  private ViewTreeObserver.OnGlobalLayoutListener keyboardLayoutListener;
  private ViewTreeObserver.OnGlobalLayoutListener symbolBarVisibilityListener;
  private long lastGitRefreshTime = 0;
  private static final long GIT_REFRESH_DEBOUNCE_MS = 1500;
  private SplitLayoutPopup splitLayoutPopup;
  private boolean isSplitViewActive = false;
  private PreferencesUtils settings;
  private int lastSplitRows = 1, lastSplitCols = 2;
  private EditorPaneFragment activePane = null;

  private static final long LSP_STATUS_POLL_INTERVAL_MS = 1500;

  /** تعداد خط و حجم فایل رو می‌خونه. برای فایل‌های خیلی بزرگ از شمردن خط صرف‌نظر می‌کنیم. */
  private static final long STATS_MAX_SCAN_BYTES = 8L * 1024 * 1024;

  private final Handler lspStatusHandler = new Handler(Looper.getMainLooper());
  private BreadcrumbAdapter breadcrumbAdapter;
  private Disposable editorHostRegistration;
  private Disposable codeRunnerHostRegistration;
  private PluginPanelHost pluginPanelHost;
  private final Runnable lspStatusPollRunnable =
      new Runnable() {
        @Override
        public void run() {
          refreshLspStatusIndicator();
          lspStatusHandler.postDelayed(this, LSP_STATUS_POLL_INTERVAL_MS);
        }
      };

  private final EditorPaneFragment.PaneActionListener paneActionListener =
      new EditorPaneFragment.PaneActionListener() {
        @Override
        public void onCloseTab(String filePath) {
          closeTabByPath(filePath);
        }

        @Override
        public void onCloseOthers(String filePath) {
          closeOtherTabsByPath(filePath);
        }

        @Override
        public void onCloseAll() {
          closeAllTabs();
        }

        @Override
        public void onTogglePin(String filePath) {
          togglePinByPath(filePath);
        }
      };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    binding = ActivityEditorBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());
    prefs = getSharedPreferences("editor", MODE_PRIVATE);
    settings = new PreferencesUtils(this);
    ThemeManager manager = new ThemeManager(this);
    theme = new ThemeUtils(manager);
    EventBus.getDefault().register(this);
    setupViewPager();
    setupTabLayout();
    breadcrumbAdapter = new BreadcrumbAdapter();
    binding.rvBreadcrumb.setLayoutManager(
        new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
    binding.rvBreadcrumb.setAdapter(breadcrumbAdapter);
    breadcrumbAdapter.setOnItemClickListener(
        item -> {
          IdeEditor current = getEditor();
          if (current != null) current.setSelection(item.getLine(), item.getColumn());
        });
    setupFAB();
    loadSavedTabs();
    updateLanguageStatus(binding.viewPager.getCurrentItem());
    PluginManager.init(this);
    String configPath = new File(getFilesDir(), "GhostIDE/plugins/config.json").getAbsolutePath();
    PluginManager.getInstance().loadPluginsFromConfig(configPath);
    editorHostRegistration =
        GlobalRegistry.services()
            .register(IdeHostServices.EDITOR_HOST, new EditorHostAdapter(this));
    codeRunnerHostRegistration =
        GlobalRegistry.services()
            .register(IdeHostServices.CODE_RUNNER_HOST, new CodeRunnerHostAdapter(this));
    theme.applyActivity(this);
    theme.applyFab(binding.fabineditor);
    theme.applyView(binding.mainContent);
    theme.applyImageBackground(binding.backgroundicon);
    theme.applyGhostIdeEditorSearch(binding.editorSearch);

    handleIncomingIntent(getIntent());

    String path = getIntent().getStringExtra("file_path");
    String name = getIntent().getStringExtra("file_name");
    if (path != null && name != null) {
      openFile(path, name);
    }
    pluginPanelHost = new PluginPanelHost(this, this::getCurrentFilePath);
    stepToolbar();
    theme.applyTabLayout(binding.tab,getCurrentFilePath());
    setupKeyboardListener();
    setupSymbolBarVisibilityWatcher();
    GitHubClient gitHub = new GitHubClient(this);
    if (gitHub.isLoggedIn()) {
      binding.userName.setText(gitHub.getName());
      Glide.with(this)
          .load(gitHub.getAvatarUrl())
          .circleCrop()
          .placeholder(R.drawable.user)
          .into(binding.userIcon);
    }

    binding.userIcon.setOnClickListener(
        v -> {
          if (gitHub.isLoggedIn()) {
            GitHubProfileSheet.newInstance().show(getSupportFragmentManager(), "github_profile");
          } else {
            new DialogCompat(v.getContext())
                .setTitle(getString(R.string.github_tokenerrortitle))
                .setMessage(getString(R.string.github_tokenerrormsg))
                .setPositiveButton(
                    getString(R.string.ok),
                    (c, e) -> {
                      Intent i = new Intent(getApplicationContext(), SettingActivity.class);
                      i.putExtra("open_section", "githublogin");
                      startActivity(i);
                    })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
          }
        });
    binding.symbolBarContainer.hide();
    binding.symbolBarContainer.bindEditor(this::getEditor);
    theme.applyEditorStatusBar(binding.editorStatusBar);

    ViewCompat.setOnApplyWindowInsetsListener(
        binding.getRoot(),
        (v, insets) -> {
          int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
          int navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
          int imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
          if (navBarHeight == 0) {
            navBarHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
          }

          if (binding.mainContent != null) {
            binding.mainContent.setPadding(0, statusBarHeight, 0, navBarHeight);
          }

          int gapFromKeyboardDp = 8;
          int gapPx =
              (int)
                  TypedValue.applyDimension(
                      TypedValue.COMPLEX_UNIT_DIP,
                      gapFromKeyboardDp,
                      getResources().getDisplayMetrics());

          int editorStatusBarHeightPx =
              (int)
                  TypedValue.applyDimension(
                      TypedValue.COMPLEX_UNIT_DIP, 36, getResources().getDisplayMetrics());
          boolean willShowEditorStatusBar = imeHeight <= 0 && !binding.editorSearch.isShowing;

          CoordinatorLayout.LayoutParams fabParams =
              (CoordinatorLayout.LayoutParams) binding.fabineditor.getLayoutParams();
          int originalFabBottomMarginDp = 20;
          int originalFabBottomMarginPx =
              (int)
                  TypedValue.applyDimension(
                      TypedValue.COMPLEX_UNIT_DIP,
                      originalFabBottomMarginDp,
                      getResources().getDisplayMetrics());
          int newFabMargin = navBarHeight + originalFabBottomMarginPx;
          if (imeHeight > 0) newFabMargin += imeHeight;
          int extraForStatusBar = willShowEditorStatusBar ? (editorStatusBarHeightPx + gapPx) : 0;
          fabParams.bottomMargin = newFabMargin + 9 + extraForStatusBar;
          binding.fabineditor.setLayoutParams(fabParams);

          CoordinatorLayout.LayoutParams searchParams =
              (CoordinatorLayout.LayoutParams) binding.editorSearch.getLayoutParams();
          if (imeHeight > 0) {
            searchParams.bottomMargin = imeHeight + gapPx;
          } else {
            int defaultBottomDp = 16;
            int defaultPx =
                (int)
                    TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        defaultBottomDp,
                        getResources().getDisplayMetrics());
            searchParams.bottomMargin = defaultPx;
          }
          binding.editorSearch.setLayoutParams(searchParams);

          CoordinatorLayout.LayoutParams symbolParams =
              (CoordinatorLayout.LayoutParams) binding.symbolBarContainer.getLayoutParams();
          if (imeHeight > 0) {
            symbolParams.bottomMargin = imeHeight + gapPx;
          } else {
            int defaultBottomDp = 16;
            int defaultPx =
                (int)
                    TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        defaultBottomDp,
                        getResources().getDisplayMetrics());
            symbolParams.bottomMargin = defaultPx;
          }
          binding.symbolBarContainer.setLayoutParams(symbolParams);

          CoordinatorLayout.LayoutParams statusBarParams =
              (CoordinatorLayout.LayoutParams) binding.editorStatusBar.getLayoutParams();
          if (imeHeight > 0) {
            statusBarParams.bottomMargin = navBarHeight + imeHeight + gapPx;
          } else {
            int statusBarDefaultDp = 16;
            int statusBarDefaultPx =
                (int)
                    TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        statusBarDefaultDp,
                        getResources().getDisplayMetrics());
            statusBarParams.bottomMargin = navBarHeight + statusBarDefaultPx;
          }
          binding.editorStatusBar.setLayoutParams(statusBarParams);

          return insets;
        });
    refreshGitStatus();
    refreshGitStatus();
  }

  @Override
  protected void onResume() {
    super.onResume();

    refreshGitStatus();
    FileRenameNotifier.getInstance().addListener(this);
    lspStatusHandler.removeCallbacks(lspStatusPollRunnable);
    lspStatusHandler.post(lspStatusPollRunnable);
  }

  @Override
  protected void onPause() {
    super.onPause();
    FileRenameNotifier.getInstance().removeListener(this);
    lspStatusHandler.removeCallbacks(lspStatusPollRunnable);
  }

  @Override
  public void onFileRenamed(String oldPath, String newPath) {
    int index = indexOfTab(oldPath);
    if (index < 0) {
      return;
    }
    TabModel tab = tabsList.get(index);
    tab.updatePath(newPath, new File(newPath).getName());
    adapter.setTabs(new ArrayList<>(tabsList));
    TabLayout.Tab layoutTab = binding.tab.getTabAt(index);
    if (layoutTab != null && layoutTab.getCustomView() instanceof TabCustomView) {
      ((TabCustomView) layoutTab.getCustomView()).bind(tab);
    }
  }

  private int indexOfTab(String filePath) {
    for (int i = 0; i < tabsList.size(); i++) {
      if (tabsList.get(i).getFilePath().equals(filePath)) return i;
    }
    return -1;
  }

  private void closeTabByPath(String filePath) {
    int i = indexOfTab(filePath);
    if (i >= 0) closeTab(i);
  }

  private void closeOtherTabsByPath(String filePath) {
    int i = indexOfTab(filePath);
    if (i >= 0) closeOtherTabs(i);
  }

  private void togglePinByPath(String filePath) {
    int i = indexOfTab(filePath);
    if (i >= 0) togglePin(i);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    EventBus.getDefault().unregister(this);
    lspStatusHandler.removeCallbacks(lspStatusPollRunnable);
    if (keyboardLayoutListener != null) {
      getWindow()
          .getDecorView()
          .getViewTreeObserver()
          .removeOnGlobalLayoutListener(keyboardLayoutListener);
      keyboardLayoutListener = null;
    }
    if (symbolBarVisibilityListener != null) {
      getWindow()
          .getDecorView()
          .getViewTreeObserver()
          .removeOnGlobalLayoutListener(symbolBarVisibilityListener);
      symbolBarVisibilityListener = null;
    }
    gitStatusExecutor.shutdownNow();
    if (editorHostRegistration != null) {
      editorHostRegistration.dispose();
      editorHostRegistration = null;
    }
    if (codeRunnerHostRegistration != null) {
      codeRunnerHostRegistration.dispose();
      codeRunnerHostRegistration = null;
    }
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);
    handleIncomingIntent(intent);
  }

  @Subscribe(threadMode = ThreadMode.MAIN)
  public void onOpenFileLocationEvent(OpenFileLocationEvent event) {
    openFileAtLocation(event.filePath, event.line, event.column);
  }

  private void openFileAtLocation(String filePath, int line, int column) {
    for (int i = 0; i < tabsList.size(); i++) {
      if (tabsList.get(i).getFilePath().equals(filePath)) {
        final int pos = i;
        binding.viewPager.setCurrentItem(pos, false);
        return;
      }
    }
    openFile(filePath, new File(filePath).getName());
  }

  private void handleIncomingIntent(Intent intent) {
    if (intent == null) return;
    String directPath = intent.getStringExtra("open_file_direct");
    if (directPath != null && !directPath.isEmpty()) {
      openFileDirect(directPath);
      return;
    }
    String action = intent.getAction();
    if (Intent.ACTION_VIEW.equals(action) || Intent.ACTION_EDIT.equals(action)) {
      Uri uri = intent.getData();
      if (uri != null) {
        String path = getRealPathFromUri(uri);
        File file = new File(path);
        if (path != null && file.exists()) {
          openFile(path);
        } else {
          Toast.makeText(this, "خطا: فایل معتبر نیست", Toast.LENGTH_SHORT).show();
        }
      }
    }
    if (Intent.ACTION_SEND.equals(action)
        && intent.getType() != null
        && "text/plain".equals(intent.getType())) {
      @SuppressWarnings("deprecation")
      Uri sharedUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
      if (sharedUri != null) {
        String path = getRealPathFromUri(sharedUri);
        if (path != null) openFile(path);
      } else {
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null) saveAndOpenSharedText(sharedText);
      }
    }
  }

  private void saveAndOpenSharedText(String text) {
    File dir = new File(getCacheDir(), "GhostIDE/temp");
    if (!dir.exists()) dir.mkdirs();
    String fileName = "shared_text_" + System.currentTimeMillis() + ".txt";
    File file = new File(dir, fileName);
    FileIOUtils.writeFileFromString(file.getAbsolutePath(), text);
    openFile(file.getAbsolutePath());
  }

  private String getRealPathFromUri(Uri uri) {
    if (uri == null) return null;
    if ("file".equals(uri.getScheme())) {
      return uri.getPath();
    }
    if ("content".equals(uri.getScheme())) {
      return copyFileFromContentUri(uri);
    }
    return null;
  }

  private String copyFileFromContentUri(Uri uri) {
    String fileName = "temp_file_" + System.currentTimeMillis();
    try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
      if (cursor != null && cursor.moveToFirst()) {
        int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        if (nameIndex != -1) fileName = cursor.getString(nameIndex);
      }
    } catch (Exception ignored) {
    }
    File tempDir = new File(getCacheDir(), "GhostIDE/temp");
    if (!tempDir.exists()) tempDir.mkdirs();
    File destFile = new File(tempDir, fileName);
    try (InputStream is = getContentResolver().openInputStream(uri);
        FileOutputStream os = new FileOutputStream(destFile)) {
      byte[] buffer = new byte[8192];
      int len;
      while ((len = is.read(buffer)) != -1) os.write(buffer, 0, len);
      return destFile.getAbsolutePath();
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  private void openFileDirect(String filePath) {
    File file = new File(filePath);
    if (!file.exists()) return;
    for (int i = 0; i < tabsList.size(); i++) {
      if (tabsList.get(i).getFilePath().equals(filePath)) {
        binding.viewPager.setCurrentItem(i);
        return;
      }
    }
    openFile(filePath, file.getName());
  }

  public void openFile(String filePath) {
    File file = new File(filePath);
    if (!file.exists()) return;
    openFile(filePath, file.getName());
  }

  private void stepFileTree() {
    String currentPath = getCurrentFilePath();
    if (currentPath == null) {
      Toast.makeText(this, "هیچ فایلی باز نیست", Toast.LENGTH_SHORT).show();
      return;
    }

    File currentFile = new File(currentPath);
    String rootPath = currentFile.isDirectory() ? currentPath : currentFile.getParent();

    if (rootPath == null) {
      Toast.makeText(this, "مسیر نامعتبر است", Toast.LENGTH_SHORT).show();
      return;
    }
    FileTreeView tree = new FileTreeView(this);
    tree.setNodePath(rootPath);
    tree.loadTree();

    tree.setClickNode(
        (v, c) -> {
          if (v != null && !v.isFolder()) {
            String filePath = v.getAbsolutePath();
            if (filePath != null) {
              openFile(filePath);
            }
          }
        });

    SideSheetDialog sideSheet = new SideSheetDialog(this);
    sideSheet.setContentView(tree);
    sideSheet.getWindow().setNavigationBarColor(0);
    theme.applySideSheetAndFileTree(sideSheet, tree);
    sideSheet.show();
  }

  private void setupKeyboardListener() {
    View rootView = getWindow().getDecorView();
    if (settings.isBackgroundZoomMod()) {
      if (keyboardLayoutListener != null) {
        getWindow()
            .getDecorView()
            .getViewTreeObserver()
            .removeOnGlobalLayoutListener(keyboardLayoutListener);
        keyboardLayoutListener = null;
      }
    } else {
      keyboardLayoutListener =
          () -> {
            Rect r = new Rect();
            rootView.getWindowVisibleDisplayFrame(r);
            int screenHeight = rootView.getRootView().getHeight();
            int keypadHeight = screenHeight - r.bottom;
            if (binding.editorSearch.isShowing) {
              binding.symbolBarContainer.hide();
              return;
            }
            if (keypadHeight > screenHeight * 0.15) {
              binding.backgroundicon.animate().scaleX(1.5f).scaleY(1.5f).setDuration(1000).start();
              binding.symbolBarContainer.show();
            } else {
              binding.backgroundicon.animate().scaleX(1.0f).scaleY(1.0f).setDuration(1000).start();
              isShowSys = false;
              binding.symbolBarContainer.hide();
            }
          };
      rootView.getViewTreeObserver().addOnGlobalLayoutListener(keyboardLayoutListener);
    }
  }

  /**
   * Keeps editorStatusBar and the symbol bar mutually exclusive: whenever the symbol bar is hidden,
   * our status bar shows, and vice versa. This watches the symbol bar's actual visibility state
   * (not its show()/hide() call sites), so it stays correct no matter what triggers the change.
   */
  private void setupSymbolBarVisibilityWatcher() {
    View rootView = getWindow().getDecorView();
    symbolBarVisibilityListener =
        () -> {
          boolean symbolBarHidden = binding.symbolBarContainer.getVisibility() != View.VISIBLE;
          boolean searchShowing = binding.editorSearch.isShowing;
          int desiredVisibility = (symbolBarHidden && !searchShowing) ? View.VISIBLE : View.GONE;
          if (binding.editorStatusBar.getVisibility() != desiredVisibility) {
            binding.editorStatusBar.setVisibility(desiredVisibility);
          }
        };
    rootView.getViewTreeObserver().addOnGlobalLayoutListener(symbolBarVisibilityListener);
  }

  void stepToolbar() {
    toolbarModel.clear();
    toolbarModel.add(new ToolbarModel(R.drawable.ic_git, "git", isGit()));
    toolbarModel.add(new ToolbarModel(R.drawable.ic_split_column, "Item Spilt!"));
    toolbarModel.add(new ToolbarModel(R.drawable.round_account_tree, "file tree"));
    toolbarModel.add(new ToolbarModel(R.drawable.outline_search, "search"));
    toolbarModel.add(new ToolbarModel(R.drawable.outline_undo, "undo"));
    toolbarModel.add(new ToolbarModel(R.drawable.outline_redo, "redo"));
    toolbarModel.add(new ToolbarModel(R.drawable.more_vert, "more"));
    toolbarModel.add(new ToolbarModel(R.drawable.ic_panel, "plugins"));
    if (listAdapter != null) {
      listAdapter.notifyDataSetChanged();
    } else {
      listAdapter =
          new ToolbarListAdapter(
              toolbarModel,
              (view, m, pos) -> {
                switch (pos) {
                  case 0 -> showGitBottomSheet();
                  case 1 -> stepFileTree();
                  case 2 -> stepSearch();
                  case 3 -> toggleOrShowSplitPopup(view);
                  case 4 -> {
                    if (getEditor().canUndo()) getEditor().undo();
                  }
                  case 5 -> {
                    if (getEditor().canRedo()) getEditor().redo();
                  }
                  case 6 -> setupMenuCalltoAction(view);
                  case 7 -> showPluginPopup(view);
                }
              },
              EditorActivity.this);
    }
    binding.rvtoolbar.setLayoutManager(
        new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
    binding.rvtoolbar.setAdapter(listAdapter);
  }

  private void openPluginPanelAt(int pos) {
    if (pluginPanelHost == null) {
      return;
    }
    int panelIndex = pos - 7;
    List<EditorPanel> panels = pluginPanelHost.getPanels();
    if (panelIndex >= 0 && panelIndex < panels.size()) {
      pluginPanelHost.showPanel(panels.get(panelIndex));
    }
  }

  private void showPluginPopup(View anchor) {
    var installedFiles = GplInstalledPlugins.listInstalled(this);
    Log.d("EditorActivity", "showPluginPopup: installed files = " + installedFiles.size());

    if (installedFiles.isEmpty()) {
      Log.w("EditorActivity", "showPluginPopup: no .gpl files found on disk");
      Toast.makeText(this, R.string.no_plugins, Toast.LENGTH_SHORT).show();
      return;
    }

    var loader = GplPluginLoader.getInstance(this);
    for (var f : installedFiles) {
      try {
        var manifest = GplManifestReader.read(f);
        if (manifest == null) continue;
        if (!loader.isLoaded(manifest.id())) {
          loader.load(f);
          Log.d("EditorActivity", "showPluginPopup: loaded .gpl plugin: " + manifest.id());
        }
      } catch (Exception e) {
        Log.e("EditorActivity", "showPluginPopup: failed to load " + f.getName(), e);
      }
    }

    var registeredPanels =
        GlobalRegistry.extensions().extensions(PluginUiExtensionPoints.EDITOR_PANEL);
    var registeredScreens =
        GlobalRegistry.extensions().extensions(PluginUiExtensionPoints.PLUGIN_SCREEN);
    Log.d(
        "EditorActivity", "showPluginPopup: registered EditorPanels = " + registeredPanels.size());
    Log.d(
        "EditorActivity",
        "showPluginPopup: registered PluginScreens = " + registeredScreens.size());

    var pluginItems =
        installedFiles.stream()
            .map(
                f -> {
                  try {
                    GplManifest manifest = GplManifestReader.read(f);
                    if (manifest == null) {
                      Log.w("EditorActivity", "  manifest null for: " + f.getName());
                      return Optional.<PluginPopupAdapter.PluginItem>empty();
                    }
                    Log.d(
                        "EditorActivity", "  file=" + f.getName() + " manifestId=" + manifest.id());

                    var matchingPanel =
                        registeredPanels.stream()
                            .filter(p -> manifest.id().equals(p.getId()))
                            .findFirst();
                    var matchingScreen =
                        registeredScreens.stream()
                            .filter(s -> manifest.id().equals(s.getId()))
                            .findFirst();

                    if (matchingPanel.isPresent()) {
                      Log.d(
                          "EditorActivity",
                          "    -> matched EditorPanel: " + matchingPanel.get().getId());
                      return Optional.of(
                          new PluginPopupAdapter.PluginItem(
                              matchingPanel.get().getId(),
                              matchingPanel.get().getTitle(),
                              f,
                              manifest));
                    } else if (matchingScreen.isPresent()) {
                      Log.d(
                          "EditorActivity",
                          "    -> matched PluginScreen: " + matchingScreen.get().getId());
                      return Optional.of(
                          new PluginPopupAdapter.PluginItem(
                              matchingScreen.get().getId(),
                              matchingScreen.get().getTitle(),
                              f,
                              manifest));
                    } else {
                      Log.d(
                          "EditorActivity",
                          "    -> no extension for manifestId="
                              + manifest.id()
                              + ", showing by manifest name");
                      return Optional.of(
                          new PluginPopupAdapter.PluginItem(
                              manifest.id(), manifest.name(), f, manifest));
                    }
                  } catch (Exception e) {
                    Log.e("EditorActivity", "  error reading: " + f.getName(), e);
                    return Optional.<PluginPopupAdapter.PluginItem>empty();
                  }
                })
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toList();

    Log.d("EditorActivity", "showPluginPopup: pluginItems size = " + pluginItems.size());

    if (pluginItems.isEmpty()) {
      Toast.makeText(this, R.string.no_plugins, Toast.LENGTH_SHORT).show();
      return;
    }

    var rv = new RecyclerView(this);
    rv.setLayoutManager(new LinearLayoutManager(this));
    var popupRef = new PopupWindow[1];
    rv.setAdapter(
        new PluginPopupAdapter(
            (view, item, pos) -> {
              if (popupRef[0] != null) popupRef[0].dismiss();
              var allPanels =
                  GlobalRegistry.extensions().extensions(PluginUiExtensionPoints.EDITOR_PANEL);
              var allScreens =
                  GlobalRegistry.extensions().extensions(PluginUiExtensionPoints.PLUGIN_SCREEN);

              var matchingPanel =
                  allPanels.stream()
                      .filter(
                          p ->
                              item.id().equals(p.getId())
                                  || (item.manifest() != null
                                      && item.manifest().id().equals(p.getId())))
                      .findFirst();
              if (matchingPanel.isPresent()) {
                pluginPanelHost.showPanel(matchingPanel.get());
                return;
              }

              var matchingScreen =
                  allScreens.stream()
                      .filter(
                          s ->
                              item.id().equals(s.getId())
                                  || (item.manifest() != null
                                      && item.manifest().id().equals(s.getId())))
                      .findFirst();
              if (matchingScreen.isPresent()) {
                startActivity(
                    PluginScreenActivity.createIntent(this, matchingScreen.get().getId()));
                return;
              }

              Toast.makeText(
                      this,
                      getString(R.string.plugin_manager_installed_toast, item.name()),
                      Toast.LENGTH_SHORT)
                  .show();
            }));

    ((PluginPopupAdapter) rv.getAdapter()).submit(pluginItems);

    popupRef[0] = ObjectUtil.showGlassPopup(this, anchor, rv);
  }

  void stepSearch() {
    binding.editorSearch.bindEditor(this::getEditor);
    binding.editorSearch.setCallBack(
        new GhostIdeEditorSearch.onViewChange() {
          @Override
          public void onViewShow() {
            binding.fabineditor.hide();
            binding.symbolBarContainer.hide();
          }

          @Override
          public void onViewHide() {
            binding.fabineditor.show();
          }
        });
    binding.editorSearch.showAndHide();
  }

  private void showGitBottomSheet() {
    String repoPath = findGitRepositoryPath();
    if (repoPath == null) {
      Toast.makeText(this, "هیچ مخزن گیتی در مسیر فایل جاری یافت نشد", Toast.LENGTH_LONG).show();
      return;
    }
    GitBottomSheetFragment bottomSheet = GitBottomSheetFragment.newInstance(repoPath);
    bottomSheet.show(getSupportFragmentManager(), "git_bottom_sheet");
  }

  private String findGitRepositoryPath() {
    String currentFilePath = getCurrentFilePath();
    if (currentFilePath == null) return null;
    File currentFile = new File(currentFilePath);
    File dir = currentFile.isDirectory() ? currentFile : currentFile.getParentFile();
    while (dir != null) {
      File gitDir = new File(dir, ".git");
      if (gitDir.exists() && gitDir.isDirectory()) {
        return dir.getAbsolutePath();
      }
      dir = dir.getParentFile();
    }
    return null;
  }

  /**
   * Refreshes the git status of the project that the currently opened tab belongs to and updates
   * the colored "modified" indicator on every open tab accordingly. Call this after any action that
   * may change the working tree state: opening/saving files, switching tabs, resuming the activity
   * (e.g. returning from the Git bottom sheet after commit/push).
   */
  private void refreshGitStatus() {
    long now = System.currentTimeMillis();
    if (now - lastGitRefreshTime < GIT_REFRESH_DEBOUNCE_MS) return;
    lastGitRefreshTime = now;
    String repoPath = findGitRepositoryPath();
    if (repoPath == null) {
      gitStatusRepoPath = null;
      gitChangedPaths.clear();
      updateAllTabsGitStatus();
      return;
    }
    gitStatusRepoPath = repoPath;
    gitStatusExecutor.execute(
        () -> {
          GitManager manager = new GitManager(repoPath);
          if (!manager.openRepository()) return;
          List<FileChange> changes = manager.getChangedFiles();
          runOnUiThread(
              () -> {
                updateGitChangedPaths(changes);
                updateAllTabsGitStatus();
              });
        });
  }

  private void updateGitChangedPaths(List<FileChange> changes) {
    gitChangedPaths.clear();
    if (changes == null) return;
    for (FileChange change : changes) {
      if (change.getPath() != null) {
        gitChangedPaths.add(change.getPath().replace(File.separatorChar, '/'));
      }
    }
  }

  private boolean isGit() {
    for (int i = 0; i < tabsList.size(); i++) {
      TabLayout.Tab layoutTab = binding.tab.getTabAt(i);
      return isFileGitChanged(tabsList.get(i).getFilePath());
    }
    return false;
  }

  private void updateAllTabsGitStatus() {
    for (int i = 0; i < tabsList.size(); i++) {
      TabLayout.Tab layoutTab = binding.tab.getTabAt(i);
      if (layoutTab != null && layoutTab.getCustomView() instanceof TabCustomView) {
        boolean changed = isFileGitChanged(tabsList.get(i).getFilePath());
        ((TabCustomView) layoutTab.getCustomView()).setGitChanged(changed);
      }
    }
    if (binding.splitPaneRoot != null) {
      binding.splitPaneRoot.notifyGitStatus(this::isFileGitChanged);
    }
  }

  private boolean isFileGitChanged(String filePath) {
    if (gitStatusRepoPath == null || filePath == null || gitChangedPaths.isEmpty()) return false;
    try {
      File repoDir = new File(gitStatusRepoPath);
      File file = new File(filePath);
      String relative =
          repoDir.toPath().relativize(file.toPath()).toString().replace(File.separatorChar, '/');
      return gitChangedPaths.contains(relative);
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * توسط EditorFragment صدا زده میشه: هم موقع ContentChangeEvent (dirty=true) هم بعد از سیو موفق —
   * چه دستی چه auto-save (dirty=false). مدل تب رو آپدیت می‌کنه و اگه همون تب الان روی TabLayout
   * دیده میشه، ستاره رو هم فوری رفرش می‌کنه.
   */
  public void setTabDirty(String filePath, boolean dirty) {
    if (filePath == null) return;
    for (int i = 0; i < tabsList.size(); i++) {
      TabModel tab = tabsList.get(i);
      if (filePath.equals(tab.getFilePath())) {
        tab.setHasStar(dirty);
        TabLayout.Tab layoutTab = binding.tab.getTabAt(i);
        if (layoutTab != null && layoutTab.getCustomView() instanceof TabCustomView) {
          ((TabCustomView) layoutTab.getCustomView()).setHasStar(dirty);
        }
        if (i == binding.viewPager.getCurrentItem()) {
          binding.editorStatusBar.setDirty(dirty);
        }
        return;
      }
    }
  }

  public void setTabError(String filePath, boolean hasError) {
    if (filePath == null) return;
    for (int i = 0; i < tabsList.size(); i++) {
      TabModel tab = tabsList.get(i);
      if (filePath.equals(tab.getFilePath())) {
        tab.setHasError(hasError);
        TabLayout.Tab layoutTab = binding.tab.getTabAt(i);
        if (layoutTab != null && layoutTab.getCustomView() instanceof TabCustomView) {
          ((TabCustomView) layoutTab.getCustomView()).setHasErrors(hasError);
        }
        return;
      }
    }
  }

  void setupMenuCalltoAction(View v) {
    var menu = theme.apply(this);
    menu.addItem(new PowerMenuItem(getString(R.string.saveitemthis), false, R.drawable.save));
    menu.addItem(new PowerMenuItem(getString(R.string.saveitemall), false, R.drawable.save));
    menu.addItem(
        new PowerMenuItem(getString(R.string.webcolor), false, R.drawable.outline_color_lens));
    menu.setOnMenuItemClickListener(
        (pos, c) -> {
          switch (pos) {
            case 0 -> saveCurrentTab();
            case 1 -> saveAllTabs();
            case 2 -> {
              var colors = new ColorPickerBottomSheet();
              colors.show(getSupportFragmentManager(), "");
            }
          }
        });
    menu.setIconSize(25);
    menu.showAsDropDown(v);
  }

  private void toggleOrShowSplitPopup(View anchor) {
    if (isSplitViewActive) {
      exitSplitView();
      return;
    }
    if (splitLayoutPopup == null) {
      splitLayoutPopup = new SplitLayoutPopup(this);
      splitLayoutPopup.setOnSplitChangeListener(
          new SplitLayoutPopup.OnSplitChangeListener() {
            @Override
            public void onApplySplit(int rows, int cols) {
              applySplitView(rows, cols);
            }

            @Override
            public void onExitSplit() {
              exitSplitView();
            }
          });
    }
    splitLayoutPopup.setCurrentState(isSplitViewActive, lastSplitRows, lastSplitCols);
    splitLayoutPopup.show(anchor);
  }

  private void applySplitView(int rows, int cols) {
    binding.splitPaneRoot.applySplit(rows, cols, tabsList);
    isSplitViewActive = binding.splitPaneRoot.isSplit();
    if (isSplitViewActive) {
      lastSplitRows = rows;
      lastSplitCols = cols;
    }
    saveSplitState();
  }

  private void exitSplitView() {
    binding.splitPaneRoot.exitSplit();
    isSplitViewActive = false;
    saveSplitState();
  }

  private void saveSplitState() {
    prefs
        .edit()
        .putBoolean(KEY_SPLIT_ACTIVE, isSplitViewActive)
        .putInt(KEY_SPLIT_ROWS, lastSplitRows)
        .putInt(KEY_SPLIT_COLS, lastSplitCols)
        .apply();
  }

  /**
   * اسپیلت رو از SharedPreferences برمیگردونه؛ فقط با خروج دستی کاربر (exitSplitView) بسته میشه، نه
   * با خروج/رفتن به activity دیگه یا کشته‌شدن پروسه.
   */
  private void restoreSplitState() {
    boolean wasSplitActive = prefs.getBoolean(KEY_SPLIT_ACTIVE, false);
    if (!wasSplitActive) return;
    int savedRows = prefs.getInt(KEY_SPLIT_ROWS, lastSplitRows);
    int savedCols = prefs.getInt(KEY_SPLIT_COLS, lastSplitCols);
    binding.splitPaneRoot.post(() -> applySplitView(savedRows, savedCols));
  }

  private void setupViewPager() {
    adapter = new EditorPagerAdapter(this, new ArrayList<>());
    binding.viewPager.setAdapter(adapter);
    binding.viewPager.setUserInputEnabled(false);
  }

  private void setupTabLayout() {
    if (tabMediator != null) tabMediator.detach();
    tabMediator =
        new TabLayoutMediator(
            binding.tab,
            binding.viewPager,
            (tab, position) -> {
              if (position < tabsList.size()) {
                TabCustomView customView = new TabCustomView(this);
                customView.bind(tabsList.get(position));
                customView.setGitChanged(isFileGitChanged(tabsList.get(position).getFilePath()));
                tab.setCustomView(customView);
              }
            });
    tabMediator.attach();
    binding.tab.addOnTabSelectedListener(
        new TabLayout.OnTabSelectedListener() {
          @Override
          public void onTabSelected(TabLayout.Tab tab) {
            int position = tab.getPosition();
            if (binding.viewPager.getCurrentItem() != position)
              binding.viewPager.setCurrentItem(position, false);
            saveCurrentPosition(position);
            updateLanguageStatus(position);
            theme.applyTabLayout(binding.tab, getCurrentFilePath());
          }

          @Override
          public void onTabUnselected(TabLayout.Tab tab) {}

          @Override
          public void onTabReselected(TabLayout.Tab tab) {
            showPopupMenu(tab.view, tab.getPosition());
          }
        });
    binding.viewPager.registerOnPageChangeCallback(
        new ViewPager2.OnPageChangeCallback() {
          @Override
          public void onPageSelected(int position) {
            super.onPageSelected(position);
            TabLayout.Tab tab = binding.tab.getTabAt(position);
            if (tab != null && !tab.isSelected()) tab.select();
            saveCurrentPosition(position);
            updateLanguageStatus(position);
            theme.applyTabLayout(binding.tab, getCurrentFilePath());
          }
        });
  }

  private void loadSavedTabs() {
    String json = prefs.getString(KEY_TABS, "");
    if (!json.isEmpty()) {
      try {
        Type type = new TypeToken<List<TabModel>>() {}.getType();
        List<TabModel> saved = gson.fromJson(json, type);
        if (saved != null) tabsList = saved;
      } catch (Exception e) {
        tabsList = new ArrayList<>();
      }
    } else {
      tabsList = new ArrayList<>();
    }
    adapter.setTabs(new ArrayList<>(tabsList));
    if (binding.splitPaneRoot != null) binding.splitPaneRoot.notifyTabsChanged(tabsList);
    int savedPosition = 0;
    String posStr = prefs.getString(KEY_POSITION, "0");
    try {
      savedPosition = Integer.parseInt(posStr);
    } catch (NumberFormatException e) {
      savedPosition = 0;
    }
    if (!tabsList.isEmpty() && savedPosition >= 0 && savedPosition < tabsList.size()) {
      binding.viewPager.setCurrentItem(savedPosition, false);
      binding.tab.setScrollPosition(savedPosition, 0f, true);
    }
    binding.splitPaneRoot.initialize(this, paneActionListener);
    binding.splitPaneRoot.setOnActivePaneChangedListener(pane -> activePane = pane);
    restoreSplitState();
  }

  private void saveCurrentPosition(int position) {
    prefs.edit().putString(KEY_POSITION, String.valueOf(position)).apply();
  }

  private void saveTabs() {
    String json = gson.toJson(tabsList);
    prefs.edit().putString(KEY_TABS, json).apply();
  }

  private void openFile(String path, String name) {
    for (int i = 0; i < tabsList.size(); i++) {
      if (tabsList.get(i).getFilePath().equals(path)) {
        binding.viewPager.setCurrentItem(i);
        return;
      }
    }
    tabsList.add(new TabModel(path, name));
    adapter.setTabs(new ArrayList<>(tabsList));
    if (binding.splitPaneRoot != null) binding.splitPaneRoot.notifyTabsChanged(tabsList);
    saveTabs();
    int newPos = tabsList.size() - 1;
    binding.viewPager.setCurrentItem(newPos);
    saveCurrentPosition(newPos);
    updateLanguageStatus(newPos);
    String ext = "";
    int dot = path.lastIndexOf('.');
    if (dot != -1) ext = path.substring(dot + 1);
    PluginManager.getInstance().setCurrentEditorActivity(this, getEditor(), path, ext);
    IdeEvents.post(FileEvent.opened(path));
    refreshGitStatus();
  }

  private String getLanguageFromPath(String path) {
    if (path == null) return "";
    int dot = path.lastIndexOf('.');
    if (dot == -1 || dot == path.length() - 1) return "";
    String ext = path.substring(dot + 1);
    return ext.substring(0, 1).toUpperCase(Locale.ROOT) + ext.substring(1);
  }

  private void updateLanguageStatus(int position) {
    if (tabsList == null || position < 0 || position >= tabsList.size()) return;
    String filePath = tabsList.get(position).getFilePath();
    String lang = getLanguageFromPath(filePath);
    binding.editorStatusBar.setLanguageText(lang.isEmpty() ? "Text" : lang);
    binding.editorStatusBar.setLinesText(formatFileStats(filePath));
    binding.editorStatusBar.setDirty(tabsList.get(position).getHasStar());
    refreshLspStatusIndicator();
  }

  /** تعداد خط و حجم فایل رو می‌خونه. برای فایل‌های خیلی بزرگ از شمردن خط صرف‌نظر می‌کنیم. */
  private String formatFileStats(String filePath) {
    File file = new File(filePath);
    if (!file.isFile()) return "";
    String size = formatFileSize(file.length());
    if (file.length() > STATS_MAX_SCAN_BYTES) return size;
    int lines = 0;
    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
      while (reader.readLine() != null) lines++;
    } catch (IOException e) {
      return size;
    }
    return getString(R.string.editor_status_lines, lines, size);
  }

  private static String formatFileSize(long bytes) {
    if (bytes < 1024) return bytes + " B";
    if (bytes < 1024 * 1024) return String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0);
    return String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024.0));
  }

  /**
   * وضعیتِ اتصالِ LSP مربوط به تبِ الان دیده شده رو می‌خونه (از IdeEditor.getLspStatus()) و نقطه‌ی
   * گِردِ داخل editorStatusBar رو بر همون اساس رنگ/متنش رو آپدیت می‌کنه.
   *
   * <p>هم از updateLanguageStatus() (موقع تعویض تب) صدا زده می‌شه، هم از lspStatusPollRunnable (هر
   * ۱.۵ ثانیه، تا وضعیت‌هایی مثل CONNECTING → CONNECTED که وسط کار عوض می‌شن هم دیده بشن).
   */
  private void refreshLspStatusIndicator() {
    if (binding == null) return;
    IdeEditor editor = getEditor();
    if (editor == null) {
      binding.editorStatusBar.setStatusIndicator(EditorStatusBar.StatusIndicator.IDLE, "—");
      return;
    }
    LspEditorStatus status = editor.getLspStatus();
    if (status == null) {

      binding.editorStatusBar.setStatusIndicator(EditorStatusBar.StatusIndicator.IDLE, "—");
      return;
    }
    switch (status) {
      case CONNECTED:
        binding.editorStatusBar.setStatusIndicator(
            EditorStatusBar.StatusIndicator.CONNECTED, "Connected");
        break;
      case CONNECTING:
        binding.editorStatusBar.setStatusIndicator(
            EditorStatusBar.StatusIndicator.CONNECTING, "Connecting…");
        break;
      case DISCONNECTED:
        binding.editorStatusBar.setStatusIndicator(
            EditorStatusBar.StatusIndicator.ERROR, "Disconnected");
        break;
      case IDLE:
      default:
        binding.editorStatusBar.setStatusIndicator(EditorStatusBar.StatusIndicator.IDLE, "Idle");
        break;
    }
    refreshBreadcrumbs();
  }

  private void refreshBreadcrumbs() {
    if (binding == null || adapter == null) return;
    if (activePane != null) {
      breadcrumbAdapter.setItems(new ArrayList<>());
      return;
    }
    if (binding.viewPager == null || adapter.getItemCount() == 0) {
      breadcrumbAdapter.setItems(new ArrayList<>());
      return;
    }
    int currentPos = binding.viewPager.getCurrentItem();
    Fragment currentFragment = adapter.getFragmentAtPosition(currentPos, this);
    if (currentFragment instanceof EditorFragment) {
      ((EditorFragment) currentFragment).scheduleBreadcrumbRefresh();
    } else {
      breadcrumbAdapter.setItems(new ArrayList<>());
    }
  }

  public void showBreadcrumbs(EditorFragment source, List<BreadcrumbItem> items) {
    if (binding == null || activePane != null) return;
    if (getEditor() != source.getEditor()) return;
    breadcrumbAdapter.setItems(items);
  }

  private void closeTab(int position) {
    if (position >= 0 && position < tabsList.size()) {
      if (tabsList.get(position).isPinned()) return;
      IdeEvents.post(FileEvent.closed(tabsList.get(position).getFilePath()));
      tabsList.remove(position);
      adapter.setTabs(new ArrayList<>(tabsList));
      if (binding.splitPaneRoot != null) binding.splitPaneRoot.notifyTabsChanged(tabsList);
      saveTabs();
      if (tabsList.isEmpty()) {
        finish();
        return;
      }
      int newPos = Math.min(position, tabsList.size() - 1);
      binding.viewPager.setCurrentItem(newPos);
      saveCurrentPosition(newPos);
    }
  }

  private void closeOtherTabs(int position) {
    if (position < 0 || position >= tabsList.size()) return;
    TabModel current = tabsList.get(position);
    List<TabModel> newList = new ArrayList<>();
    newList.add(current);
    for (int i = 0; i < tabsList.size(); i++) {
      if (i != position && tabsList.get(i).isPinned()) newList.add(tabsList.get(i));
      else if (i != position) IdeEvents.post(FileEvent.closed(tabsList.get(i).getFilePath()));
    }
    tabsList = newList;
    adapter.setTabs(new ArrayList<>(tabsList));

    if (binding.splitPaneRoot != null) binding.splitPaneRoot.notifyTabsChanged(tabsList);
    saveTabs();
    binding.viewPager.setCurrentItem(0);
    saveCurrentPosition(0);
  }

  private void closeAllTabs() {
    List<TabModel> pinned = new ArrayList<>();
    for (TabModel tab : tabsList) {
      if (tab.isPinned()) pinned.add(tab);
      else IdeEvents.post(FileEvent.closed(tab.getFilePath()));
    }
    tabsList = pinned;
    adapter.setTabs(new ArrayList<>(tabsList));
    if (binding.splitPaneRoot != null) binding.splitPaneRoot.notifyTabsChanged(tabsList);
    saveTabs();
    if (tabsList.isEmpty()) finish();
    else {
      binding.viewPager.setCurrentItem(0);
      saveCurrentPosition(0);
    }
  }

  private void togglePin(int position) {
    if (position >= 0 && position < tabsList.size()) {
      TabModel tab = tabsList.get(position);
      tab.setPinned(!tab.isPinned());
      adapter.setTabs(new ArrayList<>(tabsList));
      if (binding.splitPaneRoot != null) binding.splitPaneRoot.notifyTabsChanged(tabsList);
      saveTabs();
      TabLayout.Tab layoutTab = binding.tab.getTabAt(position);
      if (layoutTab != null && layoutTab.getCustomView() instanceof TabCustomView) {
        ((TabCustomView) layoutTab.getCustomView()).bind(tab);
      }
    }
  }

  public String getCurrentFilePath() {
    if (activePane != null) {
      return activePane.getCurrentFilePath();
    }
    int currentPos = binding.viewPager.getCurrentItem();
    if (currentPos >= 0 && currentPos < tabsList.size())
      return tabsList.get(currentPos).getFilePath();
    return null;
  }

  private void setupFAB() {
    binding.fabineditor.setOnClickListener(
        v -> {
          String currentFilePath = getCurrentFilePath();
          if (currentFilePath == null) return;
          if (currentFilePath.endsWith(".html")) {
            Intent intent = new Intent(EditorActivity.this, WebViewActivity.class);
            intent.putExtra("keyweb", currentFilePath);
            startActivity(intent);
          } else if (currentFilePath.endsWith(".md")) {
            var bl = new Bundle();
            bl.putString("md", currentFilePath);
            var mdview = new MarkDownPreview();
            mdview.setArguments(bl);
            mdview.show(getSupportFragmentManager(), "MarkDownPreview");
          } else {
            CodeRunnerHost runner = GlobalRegistry.services().get(IdeHostServices.CODE_RUNNER_HOST);
            if (runner != null) {
              runner.runFile(currentFilePath, settings.isTerminalFragment());
            } else {
              new CodeRuner(EditorActivity.this)
                  .bindof(currentFilePath, settings.isTerminalFragment());
            }
          }
        });
  }

  private void showPopupMenu(View anchor, int position) {
    var menu = theme.apply(this);
    menu.addItem(new PowerMenuItem(getString(R.string.close)));
    menu.addItem(new PowerMenuItem(getString(R.string.closeother)));
    menu.addItem(new PowerMenuItem(getString(R.string.closeall)));
    menu.addItem(new PowerMenuItem(getString(R.string.pin)));
    menu.setOnMenuItemClickListener(
        (c, pos) -> {
          switch (c) {
            case 0 -> closeTab(position);
            case 1 -> closeOtherTabs(position);
            case 2 -> closeAllTabs();
            case 3 -> togglePin(position);
          }
        });
    menu.showAsDropDown(anchor);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
      String path = data.getStringExtra("selected_file_path");
      String name = data.getStringExtra("selected_file_name");
      if (path != null) openFile(path, name);
    }
  }

  private void saveAllTabs() {
    if (adapter == null || adapter.getItemCount() == 0) {
      Toast.makeText(this, "هیچ فایلی باز نیست", Toast.LENGTH_SHORT).show();
      return;
    }
    int savedCount = 0;
    for (Fragment fragment : getSupportFragmentManager().getFragments()) {
      if (fragment instanceof EditorFragment) {
        ((EditorFragment) fragment).saveCurrentFile();
        savedCount++;
      }
    }
    List<String> activeFragPaths = new ArrayList<>();
    for (Fragment fragment : getSupportFragmentManager().getFragments()) {
      if (fragment instanceof EditorFragment && fragment.getArguments() != null) {
        String p = fragment.getArguments().getString("file_path");
        if (p != null) activeFragPaths.add(p);
      }
    }
    for (TabModel tab : tabsList) {
      if (!activeFragPaths.contains(tab.getFilePath())) {

        savedCount++;
      }
    }
    Toast.makeText(this, savedCount + getString(R.string.editorac_savefile), Toast.LENGTH_SHORT)
        .show();
    refreshGitStatus();
  }

  private void saveCurrentTab() {
    if (binding.viewPager == null || adapter == null || adapter.getItemCount() == 0) {
      Toast.makeText(this, getString(R.string.editorac_notopenfile), Toast.LENGTH_SHORT).show();
      return;
    }
    int currentPos = binding.viewPager.getCurrentItem();
    Fragment currentFragment = adapter.getFragmentAtPosition(currentPos, this);
    if (currentFragment instanceof EditorFragment) {
      ((EditorFragment) currentFragment).saveCurrentFile();
      Toast.makeText(this, getString(R.string.editorac_wassaved), Toast.LENGTH_SHORT).show();
      refreshGitStatus();
    } else {
      Toast.makeText(this, getString(R.string.editorac_errorfargment), Toast.LENGTH_SHORT).show();
    }
  }

  public IdeEditor getEditor() {
    if (activePane != null) {
      return activePane.getEditor();
    }
    if (binding.viewPager == null || adapter == null || adapter.getItemCount() == 0) {
      return null;
    }
    int currentPos = binding.viewPager.getCurrentItem();
    Fragment currentFragment = adapter.getFragmentAtPosition(currentPos, this);
    if (currentFragment instanceof EditorFragment)
      return ((EditorFragment) currentFragment).getEditor();
    return null;
  }
}
