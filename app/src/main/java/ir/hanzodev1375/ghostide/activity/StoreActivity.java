package ir.hanzodev1375.ghostide.activity;

import android.os.Bundle;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import ir.hanzodev1375.components.store.adapter.ViewPagerAdapter;
import ir.hanzodev1375.ghostide.R;

public class StoreActivity extends BaseCompat {

  private ViewPager2 viewPager;
  private BottomNavigationView bottomNav;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_store);

    ViewCompat.setOnApplyWindowInsetsListener(
        findViewById(R.id.mainRoot),
        (v, insets) -> {
          int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
          int bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
          v.setPadding(0, top, 0, bottom);
          return insets;
        });

    viewPager = findViewById(R.id.viewPager);
    bottomNav = findViewById(R.id.bottomNav);

    ViewPagerAdapter adapter = new ViewPagerAdapter(this);
    viewPager.setAdapter(adapter);
    viewPager.setUserInputEnabled(false);

    bottomNav.setOnItemSelectedListener(
        item -> {
          if (item.getItemId() == R.id.menu_projects) {
            viewPager.setCurrentItem(0);
            return true;
          }
          return false;
        });
  }
}