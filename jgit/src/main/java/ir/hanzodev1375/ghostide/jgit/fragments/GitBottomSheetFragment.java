package ir.hanzodev1375.ghostide.jgit.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import ir.hanzodev1375.components.views.GhostToast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;
import com.bumptech.glide.Glide;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import ir.hanzodev1375.components.sheet.BaseBlurBottomSheet;
import ir.hanzodev1375.components.views.SegmentedAvatarView;
import ir.hanzodev1375.ghostide.codeeditors.setting.PreferencesUtils;
import ir.hanzodev1375.ghostide.jgit.R;
import ir.hanzodev1375.ghostide.jgit.jgitandroid.datamanager.GitViewModel;
import ir.hanzodev1375.ghostide.jgit.jgitandroid.RepositoryStatus;
import ir.hanzodev1375.ghostide.jgit.adapter.ViewPagerAdapter;
import ir.hanzodev1375.ghostide.jgit.model.GitTab;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import ir.hanzodev1375.components.sheet.customitemsheet.ui.DialogCompat;
import ir.theme.M3Theme;

public class GitBottomSheetFragment extends BaseBlurBottomSheet {

  private static final int TAB_CHANGES = 0;
  private static final int TAB_HISTORY = 1;
  private static final int TAB_BRANCHES = 2;
  private static final int TAB_REMOTES = 3;

  private GitViewModel viewModel;
  private LinearProgressIndicator progressBar;
  private ViewPager2 viewPager;
  private String repoPath;
  private boolean isInitialized = false;

  public void setRepoPath(String repoPath) {
    this.repoPath = repoPath;
  }

  public GitBottomSheetFragment() {}

  public static GitBottomSheetFragment newInstance(String repoPath) {
    GitBottomSheetFragment fragment = new GitBottomSheetFragment();
    Bundle args = new Bundle();
    args.putString("repo_path", repoPath);
    fragment.setArguments(args);
    return fragment;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getArguments() != null) {
      repoPath = getArguments().getString("repo_path");
    }
  }

  @Override
  protected void onContentReady(ViewGroup contentContainer) {
    View view = getLayoutInflater().inflate(R.layout.bottom_sheet_git, contentContainer, false);
    contentContainer.addView(view);
    viewModel = new ViewModelProvider(requireActivity()).get(GitViewModel.class);
    setHasPeekMod(false);
    progressBar = view.findViewById(R.id.progressBar);
    setupHeader(view);
    M3Theme.apply(view);
    viewModel.progressMessage.observe(
        getViewLifecycleOwner(),
        msg -> {
          progressBar.setVisibility(msg != null ? View.VISIBLE : View.GONE);
          if (msg != null) {
            GhostToast.makeText(getContext(), msg, GhostToast.LENGTH_SHORT).show();
          }
        });

    viewModel.repositoryStatus.observe(
        getViewLifecycleOwner(),
        status -> {
          if (status == RepositoryStatus.OPENED || status == RepositoryStatus.INITIALIZED) {
            PreferencesUtils prefsUtils = new PreferencesUtils(requireContext());
            if (prefsUtils.hasGitLocalUserConfig()) {
              viewModel.setUserConfig(
                  prefsUtils.getGitLocalUserName(), prefsUtils.getGitLocalUserEmail());
            } else {
              showUserConfigDialog();
            }
          }
        });

    setupViewPager(view);

    viewModel.commitCompleted.observe(
        getViewLifecycleOwner(),
        completed -> {
          if (Boolean.TRUE.equals(completed) && viewPager != null) {
            viewPager.post(() -> viewPager.setCurrentItem(TAB_REMOTES, true));
          }
        });

    if (!isInitialized && repoPath != null && !repoPath.isEmpty()) {
      File gitDir = new File(repoPath, ".git");
      if (gitDir.exists() && gitDir.isDirectory()) {
        viewModel.openExistingRepository(repoPath);
      } else {
        viewModel.initializeRepository(repoPath);
      }
      isInitialized = true;
    } else if (repoPath == null || repoPath.isEmpty()) {
      GhostToast.makeText(getContext(), "مسیر مخزن تنظیم نشده است!", GhostToast.LENGTH_SHORT)
          .show();
      dismiss();
    }
    M3Theme.applyTopLevel(view);
  }

  private void setupHeader(View root) {
    SegmentedAvatarView ivAvatar = root.findViewById(R.id.ivGitAvatar);
    TextView tvUsername = root.findViewById(R.id.tvGitUsername);
    TextView tvRepoName = root.findViewById(R.id.tvGitRepoName);

    PreferencesUtils prefsUtils = new PreferencesUtils(requireContext());
    String username = prefsUtils.getGitHubUsername();
    String avatarUrl = prefsUtils.getGitHubAvatarUrl();

    tvUsername.setText(!TextUtils.isEmpty(username) ? "@" + username : "کاربر مهمان");
    tvRepoName.setText(repoPath != null && !repoPath.isEmpty() ? new File(repoPath).getName() : "");

    if (!TextUtils.isEmpty(avatarUrl)) {
      Glide.with(this).load(avatarUrl).into(ivAvatar);
    }
  }

  private void setupViewPager(View root) {
    List<GitTab> tabs = new ArrayList<>();
    tabs.add(new GitTab(getString(R.string.tab_changes), new ChangedFilesFragment()));
    tabs.add(new GitTab(getString(R.string.tab_history), new CommitHistoryFragment()));
    tabs.add(new GitTab(getString(R.string.tab_branches), new BranchesFragment()));
    tabs.add(new GitTab(getString(R.string.tab_remotes), new RemotesFragment()));
    tabs.add(new GitTab(getString(R.string.tab_stash), new StashFragment()));
    tabs.add(new GitTab(getString(R.string.tab_conflicts), new ConflictResolverFragment()));
    tabs.add(new GitTab(getString(R.string.tab_reset), new ResetFragment()));
    tabs.add(new GitTab(getString(R.string.tab_tags), new TagsFragment()));
    tabs.add(new GitTab(getString(R.string.tab_gitignore), new GitignoreFragment()));
    tabs.add(new GitTab(getString(R.string.tab_blame), new BlameFragment()));
    tabs.add(new GitTab(getString(R.string.tab_diff), new DiffViewerFragment()));

    ViewPager2 pager = root.findViewById(R.id.viewPager);
    viewPager = pager;
    ViewPagerAdapter adapter = new ViewPagerAdapter(requireActivity(), tabs);
    pager.setAdapter(adapter);

    TabLayout tabLayout = root.findViewById(R.id.tabLayout);
    new TabLayoutMediator(
            tabLayout, pager, (tab, position) -> tab.setText(tabs.get(position).getTitle()))
        .attach();
  }

  private void showUserConfigDialog() {
    PreferencesUtils prefsUtils = new PreferencesUtils(requireContext());

    DialogCompat builder = new DialogCompat(requireContext());
    builder.setTitle("Git User Configuration");

    View view = LayoutInflater.from(requireContext()).inflate(R.layout.git_local_config, null);
    EditText etName = view.findViewById(R.id.etGitUserName);
    EditText etEmail = view.findViewById(R.id.etGitUserEmail);

    etName.setText(prefsUtils.getGitLocalUserName());
    etEmail.setText(prefsUtils.getGitLocalUserEmail());

    builder.setView(view);
    builder.setPositiveButton(
        "Save",
        (d, w) -> {
          String name = etName.getText().toString().trim();
          String email = etEmail.getText().toString().trim();
          if (name.isEmpty() || email.isEmpty()) {
            GhostToast.makeText(getContext(), "Name and email required", GhostToast.LENGTH_SHORT)
                .show();
            return;
          }
          prefsUtils.setGitLocalUserName(name);
          prefsUtils.setGitLocalUserEmail(email);
          viewModel.setUserConfig(name, email);
          GhostToast.makeText(getContext(), "Saved", GhostToast.LENGTH_SHORT).show();
        });
    builder.setNegativeButton("Cancel", null);
    builder.show();
  }
}
