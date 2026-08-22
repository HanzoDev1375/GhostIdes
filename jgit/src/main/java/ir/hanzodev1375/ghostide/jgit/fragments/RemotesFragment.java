package ir.hanzodev1375.ghostide.jgit.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import ir.hanzodev1375.components.views.EmptyView;
import ir.hanzodev1375.ghostide.jgit.R;
import ir.hanzodev1375.ghostide.jgit.adapter.RemoteAdapter;
import ir.hanzodev1375.ghostide.jgit.dialogs.AddRemoteDialog;
import ir.hanzodev1375.ghostide.jgit.jgitandroid.datamanager.GitViewModel;
import ir.hanzodev1375.ghostide.jgit.jgitandroid.model.RemoteInfo;
import ir.hanzodev1375.ghostide.jgit.GitHubClient;

public class RemotesFragment extends Fragment {
  private GitViewModel viewModel;
  private RemoteAdapter adapter;
  private GitHubClient gitHubClient;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_remotes, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
    viewModel = new ViewModelProvider(requireActivity()).get(GitViewModel.class);
    gitHubClient = new GitHubClient(requireContext());

    RecyclerView recyclerView = view.findViewById(R.id.recyclerViewRemotes);
    recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    adapter = new RemoteAdapter();
    recyclerView.setAdapter(adapter);
    ((EmptyView) view.findViewById(R.id.emptyView)).bindTo(recyclerView);

    viewModel.remotes.observe(getViewLifecycleOwner(), adapter::submitList);

    adapter.setOnRemoteActionListener(
        new RemoteAdapter.OnRemoteActionListener() {
          @Override
          public void onRemove(RemoteInfo remote) {
            new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Remove Remote")
                .setMessage("Remove '" + remote.getName() + "'?")
                .setPositiveButton("Remove", (d, w) -> viewModel.removeRemote(remote.getName()))
                .setNegativeButton("Cancel", null)
                .show();
          }

          @Override
          public void onPush(RemoteInfo remote) {
            performPush(remote);
          }

          @Override
          public void onPull(RemoteInfo remote) {
            performPull(remote);
          }

          @Override
          public void onFetch(RemoteInfo remote) {
            performFetch(remote);
          }
        });

    view.findViewById(R.id.btnAddRemote).setOnClickListener(v -> showAddRemoteDialog());

    viewModel.operationResult.observe(
        getViewLifecycleOwner(),
        result -> {
          if (result != null)
            Toast.makeText(getContext(), result.getMessage(), Toast.LENGTH_SHORT).show();
        });

    viewModel.pushPullResult.observe(
        getViewLifecycleOwner(),
        result -> {
          if (result != null) {
            if (!result.isSuccess() && isConflictError(result.getMessage())) {
              showConflictDialog(result.getMessage());
            } else {
              Toast.makeText(getContext(), result.getMessage(), Toast.LENGTH_LONG).show();
            }
            if (result.isSuccess()
                && ("pull".equals(result.getOperation())
                    || "fetch".equals(result.getOperation()))) {
              viewModel.refreshAll();
            }
          }
        });
  }

  private boolean isConflictError(String message) {
    if (message == null) return false;
    String lower = message.toLowerCase();
    return lower.contains("conflict")
        || lower.contains("merge conflict")
        || lower.contains("cannot merge")
        || lower.contains("merging is not possible");
  }

  private void showConflictDialog(String errorMessage) {
    new MaterialAlertDialogBuilder(requireContext())
        .setTitle("⚠️ Merge Conflict")
        .setMessage(
            "Conflicts were detected while pulling.\n\n"
                + "Conflicting files are marked in the Changes tab.\n"
                + "Please resolve each conflict manually, then stage and commit the files.\n\n"
                + "Details: "
                + errorMessage)
        .setPositiveButton(
            "Go to Changes",
            (d, w) -> {
              if (getActivity() != null) {
                androidx.viewpager2.widget.ViewPager2 viewPager =
                    getActivity().findViewById(ir.hanzodev1375.ghostide.jgit.R.id.viewPager);
                if (viewPager != null) viewPager.setCurrentItem(0, true);
              }
              viewModel.refreshChangedFiles();
            })
        .setNegativeButton("Dismiss", null)
        .show();
  }

  private void performFetch(RemoteInfo remote) {
    String token = gitHubClient.getToken();
    if (token == null || token.isEmpty()) {
      Toast.makeText(
              getContext(), "GitHub token not found. Please login first.", Toast.LENGTH_SHORT)
          .show();
      return;
    }
    Toast.makeText(getContext(), "Fetching from " + remote.getName() + "...", Toast.LENGTH_SHORT)
        .show();
    viewModel.fetch(remote.getName(), "oauth2", token);
  }

  private void performPush(RemoteInfo remote) {
    if (viewModel.changedFiles.getValue() != null && !viewModel.changedFiles.getValue().isEmpty()) {
      new MaterialAlertDialogBuilder(requireContext())
          .setTitle("Uncommitted changes")
          .setMessage("You have uncommitted changes. Please commit them before pushing.")
          .setPositiveButton("OK", null)
          .show();
      return;
    }

    String branch = viewModel.currentBranch.getValue();
    if (branch == null || branch.isEmpty()) {
      Toast.makeText(getContext(), "Could not get current branch", Toast.LENGTH_SHORT).show();
      return;
    }

    String token = gitHubClient.getToken();
    if (token == null || token.isEmpty()) {
      Toast.makeText(
              getContext(), "GitHub token not found. Please login first.", Toast.LENGTH_SHORT)
          .show();
      return;
    }
    viewModel.push(remote.getName(), branch, "oauth2", token);
  }

  private void performPull(RemoteInfo remote) {
    if (viewModel.changedFiles.getValue() != null && !viewModel.changedFiles.getValue().isEmpty()) {
      new MaterialAlertDialogBuilder(requireContext())
          .setTitle("Uncommitted changes")
          .setMessage("You have uncommitted changes. Pull may cause conflicts. Continue anyway?")
          .setPositiveButton("Continue", (d, w) -> doPull(remote))
          .setNegativeButton("Cancel", null)
          .show();
    } else {
      doPull(remote);
    }
  }

  private void doPull(RemoteInfo remote) {
    String token = gitHubClient.getToken();
    if (token == null || token.isEmpty()) {
      Toast.makeText(
              getContext(), "GitHub token not found. Please login first.", Toast.LENGTH_SHORT)
          .show();
      return;
    }
    viewModel.pull(remote.getName(), null, "oauth2", token);
  }

  private void showAddRemoteDialog() {
    AddRemoteDialog dialog = new AddRemoteDialog();
    dialog.setOnRemoteAddListener((name, url) -> viewModel.addRemote(name, url));
    dialog.show(getChildFragmentManager(), "addRemote");
  }
}
