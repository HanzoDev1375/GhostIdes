package ir.hanzodev1375.ghostide.jgit;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.tabs.TabLayout;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import ir.hanzodev1375.components.sheet.BaseBlurBottomSheet;
import ir.hanzodev1375.ghostide.jgit.adapter.EventAdapter;
import ir.hanzodev1375.ghostide.jgit.adapter.RepoAdapter;
import ir.hanzodev1375.ghostide.jgit.model.GitHubEvent;
import ir.hanzodev1375.ghostide.jgit.model.GitHubRepo;
import java.lang.reflect.Type;
import java.util.List;

public class GitHubProfileSheet extends BaseBlurBottomSheet {

  private GitHubClient gitHub;
  private RecyclerView recyclerView;
  private ProgressBar progressBar;
  private ViewGroup layoutEmpty;
  private TabLayout tabLayout;
  private ImageView ivAvatar;
  private TextView tvName, tvUsername;
  private final Gson gson = new Gson();

  public static GitHubProfileSheet newInstance() {
    return new GitHubProfileSheet();
  }

  @Override
  protected void onContentReady(ViewGroup contentContainer) {
    View root =
        getLayoutInflater()
            .inflate(R.layout.fragment_github_profile_sheet, contentContainer, false);
    contentContainer.addView(
        root,
        new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

    gitHub = new GitHubClient(requireContext());

    ivAvatar = root.findViewById(R.id.ivAvatar);
    tvName = root.findViewById(R.id.tvName);
    tvUsername = root.findViewById(R.id.tvUsername);
    tabLayout = root.findViewById(R.id.tabLayout);
    progressBar = root.findViewById(R.id.progressBar);
    layoutEmpty = root.findViewById(R.id.layoutEmpty);
    recyclerView = root.findViewById(R.id.recyclerView);

    recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

    tvName.setText(gitHub.getName());
    tvUsername.setText("@" + gitHub.getUsername());
    Glide.with(this)
        .load(gitHub.getAvatarUrl())
        .circleCrop()
        .placeholder(R.drawable.person_24px)
        .into(ivAvatar);

    tabLayout.addTab(tabLayout.newTab().setIcon(R.drawable.deployed_code_24px).setText("Repos"));
    tabLayout.addTab(tabLayout.newTab().setIcon(R.drawable.star_24px).setText("Starred"));
    tabLayout.addTab(tabLayout.newTab().setIcon(R.drawable.ic_bolt).setText("Activity"));
    tabLayout.addTab(tabLayout.newTab().setIcon(R.drawable.rss_feed_24px).setText("Feed"));

    loadRepos();
    setHasPeekMod(false);

    tabLayout.addOnTabSelectedListener(
        new TabLayout.OnTabSelectedListener() {
          @Override
          public void onTabSelected(TabLayout.Tab tab) {
            switch (tab.getPosition()) {
              case 0 -> loadRepos();
              case 1 -> loadStarred();
              case 2 -> loadActivity();
              case 3 -> loadFeed();
            }
          }

          @Override
          public void onTabUnselected(TabLayout.Tab tab) {}

          @Override
          public void onTabReselected(TabLayout.Tab tab) {}
        });
  }

  private void loadRepos() {
    showLoading();
    gitHub.getArray(
        "https://api.github.com/users/" + gitHub.getUsername() + "/repos?per_page=50&sort=updated",
        new GitHubClient.GitHubArrayCallback() {
          @Override
          public void onSuccess(org.json.JSONArray response) {
            Type type = new TypeToken<List<GitHubRepo>>() {}.getType();
            List<GitHubRepo> repos = gson.fromJson(response.toString(), type);
            showAdapter(new RepoAdapter(repos), repos != null && !repos.isEmpty());
          }

          @Override
          public void onFailure(String errorMessage) {
            showEmpty();
          }
        });
  }

  private void loadStarred() {
    showLoading();
    gitHub.getArray(
        "https://api.github.com/users/" + gitHub.getUsername() + "/starred?per_page=50",
        new GitHubClient.GitHubArrayCallback() {
          @Override
          public void onSuccess(org.json.JSONArray response) {
            Type type = new TypeToken<List<GitHubRepo>>() {}.getType();
            List<GitHubRepo> repos = gson.fromJson(response.toString(), type);
            showAdapter(new RepoAdapter(repos), repos != null && !repos.isEmpty());
          }

          @Override
          public void onFailure(String errorMessage) {
            showEmpty();
          }
        });
  }

  private void loadFeed() {
    showLoading();
    gitHub.getReceivedEvents(
        gitHub.getUsername(),
        new GitHubClient.GitHubArrayCallback() {
          @Override
          public void onSuccess(org.json.JSONArray response) {
            Type type = new TypeToken<List<GitHubEvent>>() {}.getType();
            List<GitHubEvent> events = gson.fromJson(response.toString(), type);
            showAdapter(new EventAdapter(events), events != null && !events.isEmpty());
          }

          @Override
          public void onFailure(String errorMessage) {
            showEmpty();
          }
        });
  }

  private void loadActivity() {
    showLoading();
    gitHub.getArray(
        "https://api.github.com/users/" + gitHub.getUsername() + "/events?per_page=30",
        new GitHubClient.GitHubArrayCallback() {
          @Override
          public void onSuccess(org.json.JSONArray response) {
            Type type = new TypeToken<List<GitHubEvent>>() {}.getType();
            List<GitHubEvent> events = gson.fromJson(response.toString(), type);
            showAdapter(new EventAdapter(events), events != null && !events.isEmpty());
          }

          @Override
          public void onFailure(String errorMessage) {
            showEmpty();
          }
        });
  }

  private void showLoading() {
    if (!isAdded()) return;
    requireActivity()
        .runOnUiThread(
            () -> {
              progressBar.setVisibility(View.VISIBLE);
              recyclerView.setVisibility(View.GONE);
              layoutEmpty.setVisibility(View.GONE);
            });
  }

  private void showEmpty() {
    if (!isAdded()) return;
    requireActivity()
        .runOnUiThread(
            () -> {
              progressBar.setVisibility(View.GONE);
              recyclerView.setVisibility(View.GONE);
              layoutEmpty.setVisibility(View.VISIBLE);
            });
  }

  private void showAdapter(RecyclerView.Adapter<?> adapter, boolean hasData) {
    if (!isAdded()) return;
    requireActivity()
        .runOnUiThread(
            () -> {
              progressBar.setVisibility(View.GONE);
              if (hasData) {
                layoutEmpty.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                recyclerView.addItemDecoration(new MarginItemDecoration(requireContext()));
                recyclerView.setAdapter(adapter);
              } else {
                showEmpty();
              }
            });
  }

  public static class MarginItemDecoration extends RecyclerView.ItemDecoration {
    private final int itemMargin;

    public MarginItemDecoration(Context context) {
      itemMargin = 2;
    }

    @Override
    public void getItemOffsets(
        @NonNull Rect outRect,
        @NonNull View view,
        @NonNull RecyclerView parent,
        @NonNull RecyclerView.State state) {
      int position = parent.getChildAdapterPosition(view);
      if (position != state.getItemCount() - 1) {
        outRect.bottom = itemMargin;
      }
    }
  }
}
