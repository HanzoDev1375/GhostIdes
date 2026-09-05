package ir.hanzodev1375.ghostide.jgit.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import ir.hanzodev1375.components.views.GhostToast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import ir.hanzodev1375.components.views.EmptyView;
import ir.hanzodev1375.ghostide.jgit.R;
import ir.hanzodev1375.ghostide.jgit.adapter.FileChangeAdapter;
import ir.hanzodev1375.ghostide.jgit.dialogs.CommitDialog;
import ir.hanzodev1375.ghostide.jgit.jgitandroid.datamanager.GitViewModel;
import ir.hanzodev1375.ghostide.jgit.jgitandroid.model.FileChange;
import ir.theme.M3Theme;
import java.io.File;

public class ChangedFilesFragment extends Fragment {
  private GitViewModel viewModel;
  private FileChangeAdapter adapter;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_changed_files, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
    viewModel = new ViewModelProvider(requireActivity()).get(GitViewModel.class);

    RecyclerView recyclerView = view.findViewById(R.id.recyclerViewChanges);
    recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    adapter = new FileChangeAdapter();
    recyclerView.setAdapter(adapter);
    ((EmptyView) view.findViewById(R.id.emptyView)).bindTo(recyclerView);
    M3Theme.apply(view);
    viewModel.changedFiles.observe(getViewLifecycleOwner(), adapter::submitList);

    adapter.setOnItemClickListener(
        new FileChangeAdapter.OnItemClickListener() {
          @Override
          public void onStageClick(FileChange change) {
            viewModel.stageFile(change.getPath());
            String repoPath = viewModel.currentRepoPath.getValue();
            if (repoPath != null) {
              String fullPath = new File(repoPath, change.getPath()).getAbsolutePath();
              viewModel.setSelectedDiffFile(fullPath);
            }
          }

          @Override
          public void onUnstageClick(FileChange change) {
            viewModel.unstageFile(change.getPath());
          }

          @Override
          public void onDiscardClick(FileChange change) {
            viewModel.discardChanges(change.getPath());
          }
        });

    view.findViewById(R.id.btnStageAll).setOnClickListener(v -> viewModel.stageAllFiles());
    view.findViewById(R.id.btnCommit).setOnClickListener(v -> showCommitDialog());

    viewModel.operationResult.observe(
        getViewLifecycleOwner(),
        result -> {
          if (result != null)
            GhostToast.makeText(getContext(), result.getMessage(), GhostToast.LENGTH_SHORT).show();
        });
  }

  private void showCommitDialog() {
    CommitDialog dialog = new CommitDialog();
    dialog.setOnCommitListener((msg, author, email) -> viewModel.commit(msg, author, email));
    dialog.show(getChildFragmentManager(), "commit");
  }
}
