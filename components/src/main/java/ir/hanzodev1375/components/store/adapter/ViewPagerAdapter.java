package ir.hanzodev1375.components.store.adapter;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import ir.hanzodev1375.components.R;
import ir.hanzodev1375.components.store.fragments.FontsFragment;
import ir.hanzodev1375.components.store.fragments.IconsFragment;
import ir.hanzodev1375.components.store.fragments.StoreSectionFragment;
import ir.hanzodev1375.components.store.fragments.WebFragments;

public class ViewPagerAdapter extends FragmentStateAdapter {

  public static final int PAGE_PROJECTS = 0;
  public static final int PAGE_THEMES = 1;
  public static final int PAGE_FONTS = 2;
  public static final int PAGE_PLUGINS = 3;
  public static final int PAGE_ICONS = 4;
  public static final int PAGE_COUNT = 5;

  private final Context context;

  public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
    super(fragmentActivity);
    this.context = fragmentActivity;
  }

  @NonNull
  @Override
  public Fragment createFragment(int position) {
    switch (position) {
      case PAGE_THEMES:
        return StoreSectionFragment.newInstance(
            R.drawable.ic_outline_palette,
            context.getString(R.string.store_tab_themes),
            context.getString(R.string.store_no_items_themes));
      case PAGE_FONTS:
        return new FontsFragment();
      case PAGE_PLUGINS:
        return StoreSectionFragment.newInstance(
            R.drawable.ic_outline_extension,
            context.getString(R.string.store_tab_plugins),
            context.getString(R.string.store_no_items_plugins));
      case PAGE_ICONS:
        return new IconsFragment();
      case PAGE_PROJECTS:
      default:
        return new WebFragments();
    }
  }

  @Override
  public int getItemCount() {
    return PAGE_COUNT;
  }
}
