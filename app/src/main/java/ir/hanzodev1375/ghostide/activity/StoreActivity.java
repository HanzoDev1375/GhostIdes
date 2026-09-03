package ir.hanzodev1375.ghostide.activity;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import ir.hanzodev1375.components.store.adapter.ViewPagerAdapter;
import ir.hanzodev1375.ghostide.R;
import ir.theme.ThemeManager;
import ir.theme.ThemeUtils;
import ir.hanzodev1375.ghostide.codeeditors.setting.PreferencesUtils;

public class StoreActivity extends BaseCompat {

  private ViewPager2 viewPager;
  private BottomNavigationView bottomNav;
  private MaterialToolbar toolbar;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_store);

    View root = findViewById(R.id.mainRoot);
    View appBarLayout = findViewById(R.id.appBarLayout);
    View storeContent = findViewById(R.id.storeContent);
    toolbar = findViewById(R.id.toolbar);
    viewPager = findViewById(R.id.viewPager);
    bottomNav = findViewById(R.id.bottomNav);

    setSupportActionBar(toolbar);
    toolbar.setNavigationOnClickListener(v -> finish());

    setupBackgroundBlur(findViewById(R.id.backgroundIconStore), appBarLayout, storeContent);
    tintContentForBackground(appBarLayout, storeContent, bottomNav);

    View appBar = findViewById(R.id.appBarLayout);
    ViewCompat.setOnApplyWindowInsetsListener(
        appBar,
        (v, insets) -> {
          int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
          v.setPadding(0, top, 0, 0);
          return insets;
        });
    ViewCompat.setOnApplyWindowInsetsListener(
        bottomNav,
        (v, insets) -> {
          int bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
          v.setPadding(0, 0, 0, bottom);
          return insets;
        });

    ViewPagerAdapter adapter = new ViewPagerAdapter(this);
    viewPager.setAdapter(adapter);
    viewPager.setUserInputEnabled(true);
    viewPager.setOffscreenPageLimit(ViewPagerAdapter.PAGE_COUNT);
    viewPager.registerOnPageChangeCallback(
        new ViewPager2.OnPageChangeCallback() {
          @Override
          public void onPageSelected(int position) {
            syncNavItem(position);
            updateTitle(position);
          }
        });

    bottomNav.setOnItemSelectedListener(
        item -> {
          int id = item.getItemId();
          if (id == R.id.menu_projects) {
            viewPager.setCurrentItem(ViewPagerAdapter.PAGE_PROJECTS, true);
            return true;
          } else if (id == R.id.menu_themes) {
            viewPager.setCurrentItem(ViewPagerAdapter.PAGE_THEMES, true);
            return true;
          } else if (id == R.id.menu_fonts) {
            viewPager.setCurrentItem(ViewPagerAdapter.PAGE_FONTS, true);
            return true;
          } else if (id == R.id.menu_plugins) {
            viewPager.setCurrentItem(ViewPagerAdapter.PAGE_PLUGINS, true);
            return true;
          } else if (id == R.id.menu_icons) {
            viewPager.setCurrentItem(ViewPagerAdapter.PAGE_ICONS, true);
            return true;
          }
          return false;
        });
  }

  private void syncNavItem(int position) {
    int id;
    switch (position) {
      case ViewPagerAdapter.PAGE_THEMES:
        id = R.id.menu_themes;
        break;
      case ViewPagerAdapter.PAGE_FONTS:
        id = R.id.menu_fonts;
        break;
      case ViewPagerAdapter.PAGE_PLUGINS:
        id = R.id.menu_plugins;
        break;
      case ViewPagerAdapter.PAGE_ICONS:
        id = R.id.menu_icons;
        break;
      case ViewPagerAdapter.PAGE_PROJECTS:
      default:
        id = R.id.menu_projects;
        break;
    }
    bottomNav.setSelectedItemId(id);
  }

  private void updateTitle(int position) {
    int res;
    switch (position) {
      case ViewPagerAdapter.PAGE_THEMES:
        res = R.string.store_tab_themes;
        break;
      case ViewPagerAdapter.PAGE_FONTS:
        res = R.string.store_tab_fonts;
        break;
      case ViewPagerAdapter.PAGE_PLUGINS:
        res = R.string.store_tab_plugins;
        break;
      case ViewPagerAdapter.PAGE_ICONS:
        res = R.string.store_tab_icons;
        break;
      case ViewPagerAdapter.PAGE_PROJECTS:
      default:
        res = R.string.store_tab_projects;
        break;
    }
    toolbar.setTitle(res);
  }

  private void tintContentForBackground(View... views) {
    if (!new PreferencesUtils(this).isShowBackground()) {
      return;
    }
    ThemeUtils themeUtil = new ThemeUtils(new ThemeManager(this));
    var theme = themeUtil.getTheme();
    if (theme == null
        || theme.getActivity() == null
        || theme.getActivity().getBackground() == null) {
      return;
    }
    int bg = Color.parseColor(theme.getActivity().getBackground());
    int tint = ColorUtils.setAlphaComponent(bg, 128);
    for (View v : views) {
      if (v != null) {
        v.setBackgroundColor(tint);
      }
    }
  }
}
