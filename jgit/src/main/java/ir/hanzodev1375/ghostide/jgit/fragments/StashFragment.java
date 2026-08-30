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
import ir.hanzodev1375.ghostide.jgit.adapter.StashAdapter;
import ir.hanzodev1375.ghostide.jgit.jgitandroid.datamanager.GitViewModel;
import ir.hanzodev1375.ghostide.jgit.jgitandroid.model.StashInfo;
import ir.hanzodev1375.components.sheet.customitemsheet.ui.DialogCompat;
public class StashFragment extends Fragment {
  private GitViewModel viewModel;
  private StashAdapter adapter;

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_stash, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
    viewModel = new ViewModelProvider(requireActivity()).get(GitViewModel.class);

    RecyclerView recyclerView = view.findViewById(R.id.recyclerViewStash);
    recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    adapter = new StashAdapter();
    recyclerView.setAdapter(adapter);
    ((EmptyView) view.findViewById(R.id.emptyView)).bindTo(recyclerView);

    viewModel.stashList.observe(getViewLifecycleOwner(), adapter::submitList);
    viewModel.refreshStashList();

    adapter.setOnStashActionListener(new StashAdapter.OnStashActionListener() {
      @Override
      public void onPop(StashInfo stash) {
        new DialogCompat(requireContext())
            .setTitle("Pop Stash")
            .setMessage("Apply stash@{" + stash.getIndex() + "} and remove it?")
            .setPositiveButton("Pop", (d, w) -> viewModel.stashPop(stash.getIndex()))
            .setNegativeButton("Cancel", null).show();
      }

      @Override
      public void onApply(StashInfo stash) {
        viewModel.stashApply(stash.getIndex());
      }

      @Override
      public void onDrop(StashInfo stash) {
        new DialogCompat(requireContext())
            .setTitle("Drop Stash")
            .setMessage("Delete stash@{" + stash.getIndex() + "}? This cannot be undone.")
            .setPositiveButton("Drop", (d, w) -> viewModel.stashDrop(stash.getIndex()))
            .setNegativeButton("Cancel", null).show();
      }
    });

    EditText editMessage = view.findViewById(R.id.editStashMessage);
    view.findViewById(R.id.btnStashSave).setOnClickListener(v -> {
      String msg = editMessage.getText().toString().trim();
      viewModel.stashSave(msg.isEmpty() ? null : msg);
      editMessage.setText("");
    });

    viewModel.operationResult.observe(getViewLifecycleOwner(), result -> {
      if (result != null)
        Toast.makeText(getContext(), result.getMessage(), Toast.LENGTH_SHORT).show();
    });
  }
}
