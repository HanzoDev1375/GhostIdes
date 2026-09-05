package ir.hanzodev1375.ghostide.jgit.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import ir.hanzodev1375.components.views.GhostToast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import ir.hanzodev1375.ghostide.jgit.R;
import ir.hanzodev1375.ghostide.jgit.jgitandroid.datamanager.GitViewModel;
import ir.theme.M3Theme;

public class GitignoreFragment extends Fragment {
  private GitViewModel viewModel;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_gitignore, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
    viewModel = new ViewModelProvider(requireActivity()).get(GitViewModel.class);

    TextInputEditText editContent = view.findViewById(R.id.editGitIgnore);
    TextInputEditText editPattern = view.findViewById(R.id.editPattern);
    MaterialButton btnAdd = view.findViewById(R.id.btnAddPattern);
    MaterialButton btnSave = view.findViewById(R.id.btnSaveGitIgnore);
    M3Theme.apply(view);
    viewModel.gitIgnoreContent.observe(
        getViewLifecycleOwner(),
        content -> {
          if (content != null) editContent.setText(content);
        });
    viewModel.loadGitIgnore();

    btnAdd.setOnClickListener(
        v -> {
          String pattern =
              editPattern.getText() != null ? editPattern.getText().toString().trim() : "";
          if (pattern.isEmpty()) return;
          // append to editor directly for immediate feedback
          String current = editContent.getText() != null ? editContent.getText().toString() : "";
          String newContent =
              current.isEmpty()
                  ? pattern + "\n"
                  : (current.endsWith("\n")
                      ? current + pattern + "\n"
                      : current + "\n" + pattern + "\n");
          editContent.setText(newContent);
          editPattern.setText("");
        });

    btnSave.setOnClickListener(
        v -> {
          String content = editContent.getText() != null ? editContent.getText().toString() : "";
          viewModel.saveGitIgnore(content);
        });

    viewModel.operationResult.observe(
        getViewLifecycleOwner(),
        result -> {
          if (result != null)
            GhostToast.makeText(getContext(), result.getMessage(), GhostToast.LENGTH_SHORT).show();
        });
  }
}
