package ir.hanzodev1375.ghostide.jgit.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import ir.hanzodev1375.components.views.EmptyView;
import ir.hanzodev1375.ghostide.jgit.R;
import ir.hanzodev1375.ghostide.jgit.adapter.BranchAdapter;
import ir.hanzodev1375.ghostide.jgit.jgitandroid.datamanager.GitViewModel;

public class BranchesFragment extends Fragment {
  private GitViewModel viewModel;
  private BranchAdapter adapter;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_branches, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
    viewModel = new ViewModelProvider(requireActivity()).get(GitViewModel.class);
    RecyclerView recyclerView = view.findViewById(R.id.recyclerViewBranches);
    recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    adapter = new BranchAdapter();
    recyclerView.setAdapter(adapter);
    ((EmptyView) view.findViewById(R.id.emptyView)).bindTo(recyclerView);

    viewModel.branches.observe(
        getViewLifecycleOwner(),
        list -> {
          String current = viewModel.currentBranch.getValue();
          adapter.submitList(list, current);
        });

    adapter.setOnBranchActionListener(
        new BranchAdapter.OnBranchActionListener() {
          @Override
          public void onCheckout(String branchName) {
            viewModel.checkoutBranch(branchName);
          }

          @Override
          public void onDelete(String branchName) {
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Branch")
                .setMessage("Delete branch '" + branchName + "'?")
                .setPositiveButton("Delete", (d, w) -> viewModel.deleteBranch(branchName))
                .setNegativeButton("Cancel", null).show();
          }

          @Override
          public void onMerge(String branchName) {
            String current = viewModel.currentBranch.getValue();
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Merge Branch")
                .setMessage("Merge '" + branchName + "' into '" + current + "'?")
                .setPositiveButton("Merge", (d, w) -> viewModel.mergeBranch(branchName))
                .setNegativeButton("Cancel", null).show();
          }

          @Override
          public void onRebase(String branchName) {
            String current = viewModel.currentBranch.getValue();
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Rebase")
                .setMessage("Rebase '" + current + "' onto '" + branchName + "'?\n\nThis rewrites commit history.")
                .setPositiveButton("Rebase", (d, w) -> viewModel.rebaseBranch(branchName))
                .setNegativeButton("Cancel", null).show();
          }
        });

    EditText etNewBranch = view.findViewById(R.id.editBranchName);
    view.findViewById(R.id.btnCreateBranch)
        .setOnClickListener(
            v -> {
              String name = etNewBranch.getText().toString().trim();
              if (!name.isEmpty()) viewModel.createBranch(name);
              else Toast.makeText(getContext(), "Enter branch name", Toast.LENGTH_SHORT).show();
            });

    viewModel.operationResult.observe(
        getViewLifecycleOwner(),
        result -> {
          if (result != null)
            Toast.makeText(getContext(), result.getMessage(), Toast.LENGTH_SHORT).show();
        });
  }
}
