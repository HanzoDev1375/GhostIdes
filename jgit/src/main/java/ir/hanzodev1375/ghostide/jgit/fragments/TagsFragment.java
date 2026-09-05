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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import ir.hanzodev1375.components.views.EmptyView;
import ir.hanzodev1375.ghostide.jgit.R;
import ir.hanzodev1375.ghostide.jgit.adapter.TagAdapter;
import ir.hanzodev1375.ghostide.jgit.jgitandroid.datamanager.GitViewModel;
import ir.hanzodev1375.ghostide.jgit.jgitandroid.model.TagInfo;
import ir.hanzodev1375.components.sheet.customitemsheet.ui.DialogCompat;
import ir.theme.M3Theme;

public class TagsFragment extends Fragment {
  private GitViewModel viewModel;
  private TagAdapter adapter;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_tags, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
    viewModel = new ViewModelProvider(requireActivity()).get(GitViewModel.class);

    RecyclerView rv = view.findViewById(R.id.recyclerViewTags);
    rv.setLayoutManager(new LinearLayoutManager(getContext()));
    adapter = new TagAdapter();
    rv.setAdapter(adapter);
    EmptyView emptyView = view.findViewById(R.id.emptyView);
    emptyView.bindTo(rv);
    M3Theme.apply(view);
    TextInputEditText editName = view.findViewById(R.id.editTagName);
    TextInputEditText editMsg = view.findViewById(R.id.editTagMessage);
    MaterialButton btnCreate = view.findViewById(R.id.btnCreateTag);

    viewModel.tags.observe(getViewLifecycleOwner(), tags -> adapter.submitList(tags));
    viewModel.refreshTags();

    adapter.setOnTagActionListener(
        tag ->
            new DialogCompat(requireContext())
                .setTitle(getString(R.string.tag_delete_confirm_title))
                .setMessage(getString(R.string.tag_delete_confirm_msg, tag.getName()))
                .setPositiveButton(
                    getString(R.string.delete), (d, w) -> viewModel.deleteTag(tag.getName()))
                .setNegativeButton(getString(R.string.cancel), null)
                .show());

    btnCreate.setOnClickListener(
        v -> {
          String name = editName.getText() != null ? editName.getText().toString().trim() : "";
          if (name.isEmpty()) {
            editName.setError("Required");
            return;
          }
          String msg = editMsg.getText() != null ? editMsg.getText().toString().trim() : "";
          viewModel.createTag(name, msg.isEmpty() ? null : msg);
          editName.setText("");
          editMsg.setText("");
        });

    viewModel.operationResult.observe(
        getViewLifecycleOwner(),
        result -> {
          if (result != null)
            GhostToast.makeText(getContext(), result.getMessage(), GhostToast.LENGTH_SHORT).show();
        });
  }
}
