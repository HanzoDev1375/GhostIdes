package ir.hanzodev1375.ghostide.jgit.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import ir.hanzodev1375.ghostide.jgit.R;
import ir.hanzodev1375.ghostide.jgit.diff.GitDiffViewer;
import ir.hanzodev1375.ghostide.jgit.jgitandroid.datamanager.GitManager;
import ir.hanzodev1375.ghostide.jgit.jgitandroid.datamanager.GitViewModel;
import ir.theme.M3Theme;

public class DiffViewerFragment extends Fragment {

  private GitDiffViewer diffViewer;
  private ProgressBar progressBar;
  private TextView emptyText;
  private GitViewModel viewModel;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_diff_viewer, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    diffViewer = view.findViewById(R.id.diffViewer);
    progressBar = view.findViewById(R.id.progressBar);
    emptyText = view.findViewById(R.id.emptyText);
    viewModel = new ViewModelProvider(requireActivity()).get(GitViewModel.class);
    M3Theme.apply(view);
  }

  @Override
  public void onResume() {
    super.onResume();
    loadFullDiff();
  }

  private void loadFullDiff() {
    String repoPath = viewModel.currentRepoPath.getValue();
    if (repoPath == null || repoPath.isEmpty()) {
      showEmptyState("Repository path not found");
      return;
    }

    progressBar.setVisibility(View.VISIBLE);
    emptyText.setVisibility(View.GONE);
    diffViewer.setVisibility(View.GONE);

    new Thread(
            () -> {
              GitManager gitManager = new GitManager(repoPath);
              if (gitManager.openRepository()) {
                String diff = gitManager.getFullDiff();
                if (getActivity() != null) {
                  getActivity()
                      .runOnUiThread(
                          () -> {
                            progressBar.setVisibility(View.GONE);
                            if (diff != null
                                && !diff.isEmpty()
                                && !diff.equals("No changes detected.")) {
                              diffViewer.setVisibility(View.VISIBLE);
                              diffViewer.parseDiffOutput(diff);
                              diffViewer.applyMaterial3();
                            } else {
                              showEmptyState("No changes to display");
                            }
                          });
                }
                gitManager.close();
              } else {
                if (getActivity() != null) {
                  getActivity().runOnUiThread(() -> showEmptyState("Cannot open repository"));
                }
              }
            })
        .start();
  }

  private void showEmptyState(String message) {
    progressBar.setVisibility(View.GONE);
    diffViewer.setVisibility(View.GONE);
    emptyText.setVisibility(View.VISIBLE);
    emptyText.setText(message);
  }
}
