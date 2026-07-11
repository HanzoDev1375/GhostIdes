package ir.hanzodev1375.ghostide.splitlayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import ir.hanzodev1375.ghostide.fragments.EditorFragment;
import ir.hanzodev1375.ghostide.models.TabModel;
import java.util.ArrayList;
import java.util.List;

public class SplitEditorPagerAdapter extends FragmentStateAdapter {
  private final List<TabModel> tabs = new ArrayList<>();

  public SplitEditorPagerAdapter(@NonNull Fragment hostFragment, List<TabModel> initialTabs) {
    super(hostFragment);
    if (initialTabs != null) {
      tabs.addAll(initialTabs);
    }
  }

  public void setTabs(List<TabModel> newTabs) {
    tabs.clear();
    if (newTabs != null) {
      tabs.addAll(newTabs);
    }
    notifyDataSetChanged();
  }

  public List<TabModel> getTabs() {
    return tabs;
  }

  @NonNull
  @Override
  public Fragment createFragment(int position) {
    return EditorFragment.newInstance(tabs.get(position).getFilePath());
  }

  @Override
  public int getItemCount() {
    return tabs.size();
  }

  @Override
  public long getItemId(int position) {
    return tabs.get(position).getFilePath().hashCode();
  }

  @Override
  public boolean containsItem(long itemId) {
    for (TabModel tab : tabs) {
      if (tab.getFilePath().hashCode() == itemId) {
        return true;
      }
    }
    return false;
  }
}
