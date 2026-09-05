package ir.hanzodev1375.ghostide.jgit.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;
import ir.hanzodev1375.ghostide.jgit.R;
import ir.hanzodev1375.ghostide.jgit.adapter.BlameAdapter;
import ir.hanzodev1375.ghostide.jgit.jgitandroid.datamanager.GitViewModel;
import ir.theme.M3Theme;

public class BlameFragment extends Fragment {
  private GitViewModel viewModel;
  private BlameAdapter adapter;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_blame, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
    viewModel = new ViewModelProvider(requireActivity()).get(GitViewModel.class);

    RecyclerView rv = view.findViewById(R.id.recyclerViewBlame);
    rv.setLayoutManager(new LinearLayoutManager(getContext()));
    adapter = new BlameAdapter();
    rv.setAdapter(adapter);
    M3Theme.apply(view);

    ProgressBar progress = view.findViewById(R.id.blameProgress);
    MaterialTextView tvEmpty = view.findViewById(R.id.tvBlameEmpty);
    TextInputEditText editPath = view.findViewById(R.id.editBlamePath);
    MaterialButton btnBlame = view.findViewById(R.id.btnLoadBlame);

    // If a file was selected in diff/changes, pre-fill
    viewModel.selectedDiffFile.observe(
        getViewLifecycleOwner(),
        path -> {
          if (path != null && !path.isEmpty()) editPath.setText(path);
        });

    viewModel.progressMessage.observe(
        getViewLifecycleOwner(),
        msg -> {
          boolean loading = "Loading blame...".equals(msg);
          progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        });

    viewModel.blameResult.observe(
        getViewLifecycleOwner(),
        result -> {
          if (result == null) return;
          boolean empty = result.isEmpty();
          tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
          rv.setVisibility(empty ? View.GONE : View.VISIBLE);
          if (empty) tvEmpty.setText("No blame data found for this file");
          else adapter.submitList(result);
        });

    btnBlame.setOnClickListener(
        v -> {
          String path = editPath.getText() != null ? editPath.getText().toString().trim() : "";
          if (path.isEmpty()) {
            editPath.setError("Required");
            return;
          }
          viewModel.loadBlame(path);
        });
  }
}
