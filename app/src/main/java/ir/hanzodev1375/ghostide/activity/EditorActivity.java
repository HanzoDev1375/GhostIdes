package ir.hanzodev1375.ghostide.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.TypedValue;
import android.view.View;
import android.widget.Toast;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.sidesheet.SideSheetDialog;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.skydoves.powermenu.PowerMenuItem;
import com.blankj.utilcode.util.FileIOUtils;
import io.github.rosemoe.sora.event.ContentChangeEvent;
import ir.hanzodev1375.filetreelib.widget.FileTreeView;
import ir.hanzodev1375.ghostide.customui.TabCustomView;
import ir.hanzodev1375.ghostide.jgit.GitHubClient;
import ir.hanzodev1375.ghostide.jgit.GitHubProfileSheet;
import ir.hanzodev1375.ghostide.jgit.fragments.GitBottomSheetFragment;
import ir.hanzodev1375.ghostide.jgit.jgitandroid.datamanager.GitManager;
import ir.hanzodev1375.ghostide.jgit.jgitandroid.model.FileChange;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import ir.hanzodev1375.ghostide.R;
import ir.hanzodev1375.ghostide.adapters.EditorPagerAdapter;
import ir.hanzodev1375.ghostide.adapters.ToolbarListAdapter;
import ir.hanzodev1375.ghostide.codeeditors.IdeEditor;
import ir.hanzodev1375.ghostide.customui.GhostIdeEditorSearch;
import ir.hanzodev1375.ghostide.databinding.ActivityEditorBinding;
import ir.hanzodev1375.ghostide.fragments.EditorFragment;
import ir.hanzodev1375.ghostide.models.TabModel;
import ir.hanzodev1375.ghostide.models.ToolbarModel;
import ir.hanzodev1375.ghostide.plugin.PluginManager;
import ir.theme.ThemeManager;
import ir.theme.ThemeUtils;
import android.view.ViewTreeObserver;

public class EditorActivity extends BaseCompat {

  private ActivityEditorBinding binding;
  private EditorPagerAdapter adapter;
  private ThemeUtils theme;
  private List<TabModel> tabsList = new ArrayList<>();
  private SharedPreferences prefs;
  private Gson gson = new Gson();
  private static final String KEY_TABS = "path";
  private static final String KEY_POSITION = "positionTabs";
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

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    binding = ActivityEditorBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());
    prefs = getSharedPreferences("editor", MODE_PRIVATE);

    setupViewPager();
    setupTabLayout();
    setupFAB();
    loadSavedTabs();
    updateLanguageStatus(binding.viewPager.getCurrentItem());
    PluginManager.init(this);
    String configPath = Environment.getExternalStorageDirectory() + "/GhostIDE/plugins/config.json";
    PluginManager.getInstance().loadPluginsFromConfig(configPath);
    ThemeManager manager = new ThemeManager(this);
    theme = new ThemeUtils(manager);
    theme.applyActivity(this);
    theme.applyFab(binding.fabineditor);
    theme.applyTabLayout(binding.tab);
    theme.applyView(binding.mainContent);
    theme.applyImageBackground(binding.backgroundicon);
    theme.applyGhostIdeEditorSearch(binding.editorSearch);

    handleIncomingIntent(getIntent());

    String path = getIntent().getStringExtra("file_path");
    String name = getIntent().getStringExtra("file_name");
    if (path != null && name != null) {
      openFile(path, name);
    }
    stepToolbar();
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
            new MaterialAlertDialogBuilder(v.getContext())
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
    // Re-check git status whenever the activity resumes (e.g. after returning from the
    // Git bottom sheet where the user may have committed/pushed changes).
    refreshGitStatus();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
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
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);
    handleIncomingIntent(intent);
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
    String tempDir = Environment.getExternalStorageDirectory() + "/GhostIDE/temp/";
    File dir = new File(tempDir);
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
      String[] projection = {MediaStore.MediaColumns.DATA};
      try (Cursor cursor = getContentResolver().query(uri, projection, null, null, null)) {
        if (cursor != null && cursor.moveToFirst()) {
          int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
          String path = cursor.getString(columnIndex);
          if (path != null) return path;
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
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
    File tempDir = new File(Environment.getExternalStorageDirectory(), "GhostIDE/temp");
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

  private void openFile(String filePath) {
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
    toolbarModel.add(new ToolbarModel(R.drawable.round_account_tree, "file tree"));
    toolbarModel.add(new ToolbarModel(R.drawable.outline_search, "search"));
    toolbarModel.add(
        new ToolbarModel(com.bluewhaleyt.materialfileicon.R.drawable.ic_material_git, "git"));
    toolbarModel.add(new ToolbarModel(R.drawable.outline_undo, "undo"));
    toolbarModel.add(new ToolbarModel(R.drawable.outline_redo, "redo"));
    toolbarModel.add(new ToolbarModel(R.drawable.more_vert, "more"));
    listAdapter =
        new ToolbarListAdapter(
            toolbarModel,
            (view, m, pos) -> {
              switch (pos) {
                case 0 -> stepFileTree();
                case 1 -> stepSearch();
                case 2 -> showGitBottomSheet();
                case 3 -> {
                  if (getEditor().canUndo()) getEditor().undo();
                }
                case 4 -> {
                  if (getEditor().canRedo()) getEditor().redo();
                }
                case 5 -> setupMenuCalltoAction(view);
              }
            },
            EditorActivity.this);
    binding.rvtoolbar.setLayoutManager(
        new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
    binding.rvtoolbar.setAdapter(listAdapter);
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

  private void updateAllTabsGitStatus() {
    for (int i = 0; i < tabsList.size(); i++) {
      TabLayout.Tab layoutTab = binding.tab.getTabAt(i);
      if (layoutTab != null && layoutTab.getCustomView() instanceof TabCustomView) {
        boolean changed = isFileGitChanged(tabsList.get(i).getFilePath());
        ((TabCustomView) layoutTab.getCustomView()).setGitChanged(changed);
      }
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
        return;
      }
    }
  }

  void setupMenuCalltoAction(View v) {
    var menu = theme.apply(this);
    menu.addItem(new PowerMenuItem(getString(R.string.saveitemthis), false, R.drawable.save));
    menu.addItem(new PowerMenuItem(getString(R.string.saveitemall), false, R.drawable.save));
    menu.setOnMenuItemClickListener(
        (pos, c) -> {
          switch (pos) {
            case 0 -> saveCurrentTab();
            case 1 -> saveAllTabs();
          }
        });
    menu.setIconSize(25);
    menu.showAsDropDown(v);
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
    saveTabs();
    int newPos = tabsList.size() - 1;
    binding.viewPager.setCurrentItem(newPos);
    saveCurrentPosition(newPos);
    updateLanguageStatus(newPos);
    String ext = "";
    int dot = path.lastIndexOf('.');
    if (dot != -1) ext = path.substring(dot + 1);
    PluginManager.getInstance().setCurrentEditorActivity(this, getEditor(), path, ext);
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
    String lang = getLanguageFromPath(tabsList.get(position).getFilePath());
    binding.editorStatusBar.setLanguageText(lang.isEmpty() ? "Text" : lang);
  }

  private void closeTab(int position) {
    if (position >= 0 && position < tabsList.size()) {
      if (tabsList.get(position).isPinned()) return;
      tabsList.remove(position);
      adapter.setTabs(new ArrayList<>(tabsList));
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
    }
    tabsList = newList;
    adapter.setTabs(new ArrayList<>(tabsList));
    saveTabs();
    binding.viewPager.setCurrentItem(0);
    saveCurrentPosition(0);
  }

  private void closeAllTabs() {
    List<TabModel> pinned = new ArrayList<>();
    for (TabModel tab : tabsList) if (tab.isPinned()) pinned.add(tab);
    tabsList = pinned;
    adapter.setTabs(new ArrayList<>(tabsList));
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
      saveTabs();
      TabLayout.Tab layoutTab = binding.tab.getTabAt(position);
      if (layoutTab != null && layoutTab.getCustomView() instanceof TabCustomView) {
        ((TabCustomView) layoutTab.getCustomView()).bind(tab);
      }
    }
  }

  private String getCurrentFilePath() {
    int currentPos = binding.viewPager.getCurrentItem();
    if (currentPos >= 0 && currentPos < tabsList.size())
      return tabsList.get(currentPos).getFilePath();
    return null;
  }

  private void setupFAB() {
    binding.fabineditor.setOnClickListener(
        v -> {
          String currentFilePath = getCurrentFilePath();
          if (currentFilePath != null && currentFilePath.endsWith(".html")) {
            Intent intent = new Intent(EditorActivity.this, WebViewActivity.class);
            intent.putExtra("keyweb", currentFilePath);
            startActivity(intent);
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
        // فایل offscreen — محتوایش در SharedPreferences یا disk هست، نیاز به save ندارد
        // فقط count رو بالا ببر تا user گیج نشه
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

  private IdeEditor getEditor() {
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
