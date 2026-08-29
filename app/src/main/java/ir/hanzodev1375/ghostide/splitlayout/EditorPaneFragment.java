package ir.hanzodev1375.ghostide.splitlayout;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.skydoves.powermenu.PowerMenuItem;
import ir.hanzodev1375.ghostide.R;
import ir.hanzodev1375.ghostide.codeeditors.IdeEditor;
import ir.hanzodev1375.ghostide.customui.TabCustomView;
import ir.hanzodev1375.ghostide.databinding.FragmentEditorPaneBinding;
import ir.hanzodev1375.ghostide.fragments.EditorFragment;
import ir.hanzodev1375.ghostide.models.TabModel;
import ir.theme.ThemeManager;
import ir.theme.ThemeUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;


public class EditorPaneFragment extends Fragment {

  public interface PaneActionListener {
    void onCloseTab(String filePath);

    void onCloseOthers(String filePath);

    void onCloseAll();

    void onTogglePin(String filePath);
  }

  private FragmentEditorPaneBinding binding;
  private SplitEditorPagerAdapter adapter;
  private TabLayoutMediator mediator;
  private List<TabModel> tabs = new ArrayList<>();
  private PaneActionListener actionListener;

  public static EditorPaneFragment newInstance() {
    return new EditorPaneFragment();
  }

  public void setSharedData(List<TabModel> tabs, PaneActionListener listener) {
    this.tabs = tabs != null ? tabs : new ArrayList<>();
    this.actionListener = listener;
    if (adapter != null) {
      adapter.setTabs(this.tabs);
    }
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
    binding = FragmentEditorPaneBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    adapter = new SplitEditorPagerAdapter(this, tabs);
    binding.paneViewPager.setAdapter(adapter);
    binding.paneViewPager.setUserInputEnabled(false);

    mediator =
        new TabLayoutMediator(
            binding.paneTab,
            binding.paneViewPager,
            (tab, position) -> {
              if (position < tabs.size()) {
                TabCustomView customView = new TabCustomView(requireContext());
                customView.bind(tabs.get(position));
                tab.setCustomView(customView);
              }
            });
    mediator.attach();

    binding.paneTab.addOnTabSelectedListener(
        new TabLayout.OnTabSelectedListener() {
          @Override
          public void onTabSelected(TabLayout.Tab tab) {
            int position = tab.getPosition();
            if (binding.paneViewPager.getCurrentItem() != position) {
              binding.paneViewPager.setCurrentItem(position, false);
            }
            ThemeManager themeManager = new ThemeManager(requireContext());
            ThemeUtils themeUtils = new ThemeUtils(themeManager);
            themeUtils.applyTabLayout(binding.paneTab, getCurrentFilePath());
          }

          @Override
          public void onTabUnselected(TabLayout.Tab tab) {}

          @Override
          public void onTabReselected(TabLayout.Tab tab) {
            showPaneTabMenu(tab.view, tab.getPosition());
          }
        });

    if (!tabs.isEmpty()) {
      binding.paneViewPager.setCurrentItem(tabs.size() - 1, false);
    }

    ThemeManager themeManager = new ThemeManager(requireContext());
    ThemeUtils themeUtils = new ThemeUtils(themeManager);
    themeUtils.applyTabLayout(binding.paneTab,getCurrentFilePath());
  }

  private void showPaneTabMenu(View anchor, int position) {
    if (actionListener == null || position < 0 || position >= tabs.size()) return;
    String filePath = tabs.get(position).getFilePath();

    ThemeManager themeManager = new ThemeManager(requireContext());
    ThemeUtils themeUtils = new ThemeUtils(themeManager);
    var menu = themeUtils.apply(requireContext());
    menu.addItem(new PowerMenuItem(getString(R.string.close)));
    menu.addItem(new PowerMenuItem(getString(R.string.closeother)));
    menu.addItem(new PowerMenuItem(getString(R.string.closeall)));
    menu.addItem(new PowerMenuItem(getString(R.string.pin)));
    menu.setOnMenuItemClickListener(
        (pos, item) -> {
          switch (pos) {
            case 0 -> actionListener.onCloseTab(filePath);
            case 1 -> actionListener.onCloseOthers(filePath);
            case 2 -> actionListener.onCloseAll();
            case 3 -> actionListener.onTogglePin(filePath);
          }
        });
    menu.showAsDropDown(anchor);
  }


  public void refreshTabs(List<TabModel> newTabs) {
    this.tabs = newTabs != null ? newTabs : new ArrayList<>();
    if (adapter == null || binding == null) return;
    int current = binding.paneViewPager.getCurrentItem();
    adapter.setTabs(this.tabs);
    if (!this.tabs.isEmpty()) {
      int safe = Math.min(current, this.tabs.size() - 1);
      binding.paneViewPager.setCurrentItem(safe, false);
    }
  }

  public void updateDirty(String filePath, boolean dirty) {
    int index = indexOf(filePath);
    if (index < 0 || binding == null) return;
    TabLayout.Tab layoutTab = binding.paneTab.getTabAt(index);
    if (layoutTab != null && layoutTab.getCustomView() instanceof TabCustomView) {
      ((TabCustomView) layoutTab.getCustomView()).setHasStar(dirty);
    }
  }

  public void updateGitStatus(Predicate<String> isChanged) {
    if (binding == null) return;
    for (int i = 0; i < tabs.size(); i++) {
      TabLayout.Tab layoutTab = binding.paneTab.getTabAt(i);
      if (layoutTab != null && layoutTab.getCustomView() instanceof TabCustomView) {
        ((TabCustomView) layoutTab.getCustomView())
            .setGitChanged(isChanged.test(tabs.get(i).getFilePath()));
      }
    }
  }


  @Nullable
  public IdeEditor getEditor() {
    if (binding == null || adapter == null || adapter.getItemCount() == 0) return null;
    int position = binding.paneViewPager.getCurrentItem();
    long itemId = adapter.getItemId(position);
    Fragment fragment = getChildFragmentManager().findFragmentByTag("f" + itemId);
    if (fragment instanceof EditorFragment) {
      return ((EditorFragment) fragment).getEditor();
    }
    return null;
  }

  @Nullable
  public String getCurrentFilePath() {
    if (binding == null || tabs.isEmpty()) return null;
    int position = binding.paneViewPager.getCurrentItem();
    if (position < 0 || position >= tabs.size()) return null;
    return tabs.get(position).getFilePath();
  }

  private int indexOf(String filePath) {
    if (filePath == null) return -1;
    for (int i = 0; i < tabs.size(); i++) {
      if (filePath.equals(tabs.get(i).getFilePath())) return i;
    }
    return -1;
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    if (mediator != null) {
      mediator.detach();
      mediator = null;
    }
    binding = null;
  }
}
