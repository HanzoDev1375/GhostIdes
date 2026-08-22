package ir.hanzodev1375.ghostide.jgit.fragments;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import ir.hanzodev1375.components.views.EmptyView;
import ir.hanzodev1375.ghostide.jgit.jgitandroid.datamanager.GitViewModel;
import ir.hanzodev1375.ghostide.jgit.adapter.CommitAdapter;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.os.Bundle;
import android.view.View;
import ir.hanzodev1375.ghostide.jgit.R;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;

public class CommitHistoryFragment extends Fragment {
  private GitViewModel viewModel;
  private CommitAdapter adapter;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_commit_history, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
    viewModel = new ViewModelProvider(requireActivity()).get(GitViewModel.class);
    RecyclerView recyclerView = view.findViewById(R.id.recyclerViewCommits);
    recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    adapter = new CommitAdapter();
    recyclerView.setAdapter(adapter);
    ((EmptyView) view.findViewById(R.id.emptyView)).bindTo(recyclerView);

    viewModel.commitHistory.observe(getViewLifecycleOwner(), adapter::submitList);
    view.findViewById(R.id.btnRefreshHistory)
        .setOnClickListener(v -> viewModel.refreshCommitHistory());

    adapter.setOnCommitClickListener(
        commit ->
            CommitDiffBottomSheetFragment.newInstance(commit)
                .show(getChildFragmentManager(), "commitDiff"));
  }
}
