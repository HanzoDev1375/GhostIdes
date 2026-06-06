package ir.hanzodev1375.ghostide.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Environment;
import android.util.TypedValue;
import android.view.View;
import android.widget.PopupMenu;

import android.widget.Toast;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.ViewCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.skydoves.powermenu.PowerMenuItem;
import ir.hanzodev1375.ghostide.adapters.ToolbarListAdapter;
import ir.hanzodev1375.ghostide.codeeditors.IdeEditor;
import androidx.fragment.app.Fragment;
import ir.hanzodev1375.ghostide.customui.GhostIdeEditorSearch;
import ir.hanzodev1375.ghostide.fragments.EditorFragment;
import ir.hanzodev1375.ghostide.models.ToolbarModel;
import ir.hanzodev1375.ghostide.plugin.PluginManager;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import ir.hanzodev1375.ghostide.R;
import ir.hanzodev1375.ghostide.adapters.EditorPagerAdapter;
import ir.hanzodev1375.ghostide.databinding.ActivityEditorBinding;
import ir.hanzodev1375.ghostide.models.TabModel;
import ir.theme.ThemeManager;
import ir.theme.ThemeUtils;

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

    String path = getIntent().getStringExtra("file_path");
    String name = getIntent().getStringExtra("file_name");
    if (path != null && name != null) {
      openFile(path, name);
    }
    stepToolbar();
    setupKeyboardListener();
    binding.symbolBarContainer.hide();
    binding.symbolBarContainer.bindEditor(getEditor());

    ViewCompat.setOnApplyWindowInsetsListener(
        binding.getRoot(),
        (v, insets) -> {
          int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
          int navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
          int imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
          if (navBarHeight == 0) {
            navBarHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
          }

          var mainContent = binding.mainContent;
          if (mainContent != null) {
            mainContent.setPadding(0, statusBarHeight, 0, navBarHeight);
          }
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
          if (imeHeight > 0) {
            newFabMargin += imeHeight;
          }
          fabParams.bottomMargin = newFabMargin;
          binding.fabineditor.setLayoutParams(fabParams);
          CoordinatorLayout.LayoutParams searchParams =
              (CoordinatorLayout.LayoutParams) binding.editorSearch.getLayoutParams();
          int gapFromKeyboardDp = 8;
          int gapPx =
              (int)
                  TypedValue.applyDimension(
                      TypedValue.COMPLEX_UNIT_DIP,
                      gapFromKeyboardDp,
                      getResources().getDisplayMetrics());
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

          return insets;
        });
  }

  private void setupKeyboardListener() {
    View rootView = getWindow().getDecorView();
    rootView
        .getViewTreeObserver()
        .addOnGlobalLayoutListener(
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
                
                binding
                    .backgroundicon
                    .animate()
                    .scaleX(1.5f)
                    .scaleY(1.5f)
                    .setDuration(1000)
                    .start();
                binding.symbolBarContainer.show();
              } else {
                
                binding
                    .backgroundicon
                    .animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(1000)
                    .start();
                isShowSys = false;
                binding.symbolBarContainer.hide();
              }
            });
  }
  void stepToolbar() {
    toolbarModel.add(new ToolbarModel(R.drawable.outline_search, "search"));
    toolbarModel.add(new ToolbarModel(R.drawable.outline_undo, "undo"));
    toolbarModel.add(new ToolbarModel(R.drawable.outline_redo, "redo"));
    toolbarModel.add(new ToolbarModel(R.drawable.more_vert, "more"));

    listAdapter =
        new ToolbarListAdapter(
            toolbarModel,
            (view, m, pos) -> {
              switch (pos) {
                case 0 -> stepSearch();
                case 1 -> {
                  if (getEditor().canUndo()) getEditor().undo();
                }
                case 2 -> {
                  if (getEditor().canRedo()) getEditor().redo();
                }
                case 3 -> setupMenuCalltoAction(view);
              }
            },
            EditorActivity.this);
    binding.rvtoolbar.setLayoutManager(
        new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
    binding.rvtoolbar.setAdapter(listAdapter);
  }

  void stepSearch() {
    binding.editorSearch.bindEditor(getEditor());
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
            // binding.symbolBarContainer.show();
          }
        });
    binding.editorSearch.showAndHide();
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
    if (tabMediator != null) {
      tabMediator.detach();
    }
    tabMediator =
        new TabLayoutMediator(
            binding.tab,
            binding.viewPager,
            (tab, position) -> {
              if (position < tabsList.size()) {
                tab.setText(tabsList.get(position).getFileName());
              }
            });
    tabMediator.attach();

    binding.tab.addOnTabSelectedListener(
        new TabLayout.OnTabSelectedListener() {
          @Override
          public void onTabSelected(TabLayout.Tab tab) {
            int position = tab.getPosition();
            if (binding.viewPager.getCurrentItem() != position) {
              binding.viewPager.setCurrentItem(position, false);
            }
            saveCurrentPosition(position);
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
            if (tab != null && !tab.isSelected()) {
              tab.select();
            }
            binding.symbolBarContainer.bindEditor(getEditor());
            saveCurrentPosition(position);
          }
        });
  }

  private void loadSavedTabs() {
    String json = prefs.getString(KEY_TABS, "");
    if (!json.isEmpty()) {
      try {
        Type type = new TypeToken<List<TabModel>>() {}.getType();
        List<TabModel> saved = gson.fromJson(json, type);
        if (saved != null) {
          tabsList = saved;
        }
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
    String ext = "";
    int dot = path.lastIndexOf('.');
    if (dot != -1) ext = path.substring(dot + 1);
    PluginManager.getInstance().setCurrentEditorActivity(this, getEditor(), path, ext);
  }

  private void closeTab(int position) {
    if (position >= 0 && position < tabsList.size()) {
      if (tabsList.get(position).isPinned()) {
        return;
      }
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
      if (i != position && tabsList.get(i).isPinned()) {
        newList.add(tabsList.get(i));
      }
    }
    tabsList = newList;
    adapter.setTabs(new ArrayList<>(tabsList));
    saveTabs();
    int newPos = 0;
    binding.viewPager.setCurrentItem(newPos);
    saveCurrentPosition(newPos);
  }

  private void closeAllTabs() {
    List<TabModel> pinned = new ArrayList<>();
    for (TabModel tab : tabsList) {
      if (tab.isPinned()) pinned.add(tab);
    }
    tabsList = pinned;
    adapter.setTabs(new ArrayList<>(tabsList));
    saveTabs();
    if (tabsList.isEmpty()) {
      finish();
    } else {
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
      binding.tab.getTabAt(position).setText(tab.getFileName());
    }
  }

  private String getCurrentFilePath() {
    int currentPos = binding.viewPager.getCurrentItem();
    if (currentPos >= 0 && currentPos < tabsList.size()) {
      return tabsList.get(currentPos).getFilePath();
    }
    return null;
  }

  private void setupFAB() {
    binding.fabineditor.setOnClickListener(
        v -> {
          String currentFilePath = getCurrentFilePath();
          if (currentFilePath.endsWith(".html")) {
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
      if (path != null) {
        openFile(path, name);
      }
    }
  }

  private void saveAllTabs() {
    if (adapter == null || adapter.getItemCount() == 0) {
      Toast.makeText(this, "هیچ فایلی باز نیست", Toast.LENGTH_SHORT).show();
      return;
    }

    int savedCount = 0;
    List<Fragment> fragments = getSupportFragmentManager().getFragments();
    for (Fragment fragment : fragments) {
      if (fragment instanceof EditorFragment) {
        ((EditorFragment) fragment).saveCurrentFile();
        savedCount++;
      }
    }

    Toast.makeText(this, savedCount + " فایل ذخیره شد", Toast.LENGTH_SHORT).show();
  }

  private void saveCurrentTab() {
    if (binding.viewPager == null || adapter == null || adapter.getItemCount() == 0) {
      Toast.makeText(this, "هیچ فایلی باز نیست", Toast.LENGTH_SHORT).show();
      return;
    }
    int currentPos = binding.viewPager.getCurrentItem();
    Fragment currentFragment = adapter.getFragmentAtPosition(currentPos, this);
    if (currentFragment instanceof EditorFragment) {
      ((EditorFragment) currentFragment).saveCurrentFile();
      Toast.makeText(this, "فایل جاری ذخیره شد", Toast.LENGTH_SHORT).show();
    } else {
      Toast.makeText(this, "خطا در یافت فرگمنت", Toast.LENGTH_SHORT).show();
    }
  }

  private IdeEditor getEditor() {
    if (adapter == null || adapter.getItemCount() == 0) return null;
    int currentPos = binding.viewPager.getCurrentItem();
    if (currentPos < 0 || currentPos >= adapter.getItemCount()) return null;

    Fragment fragment = adapter.getFragmentAtPosition(currentPos, this);
    if (fragment instanceof EditorFragment) {
      return ((EditorFragment) fragment).getEditor();
    }
    return null;
  }
}
