package ir.hanzodev1375.ghostide.jgit.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import ir.hanzodev1375.components.views.GhostToast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import ir.hanzodev1375.ghostide.jgit.R;
import ir.hanzodev1375.ghostide.jgit.jgitandroid.datamanager.GitViewModel;
import ir.hanzodev1375.ghostide.jgit.jgitandroid.model.ConflictFile;
import ir.hanzodev1375.components.sheet.customitemsheet.ui.DialogCompat;
public class ConflictResolverFragment extends Fragment {
  private GitViewModel viewModel;
  private ConflictAdapter adapter;
  private TextView tvNoConflicts;

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_conflict_resolver, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
    viewModel = new ViewModelProvider(requireActivity()).get(GitViewModel.class);
    tvNoConflicts = view.findViewById(R.id.tvNoConflicts);

    RecyclerView recyclerView = view.findViewById(R.id.recyclerViewConflicts);
    recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    adapter = new ConflictAdapter();
    recyclerView.setAdapter(adapter);

    viewModel.conflictFiles.observe(getViewLifecycleOwner(), conflicts -> {
      adapter.submitList(conflicts);
      boolean empty = conflicts == null || conflicts.isEmpty();
      tvNoConflicts.setVisibility(empty ? View.VISIBLE : View.GONE);
      recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
    });

    viewModel.refreshConflictFiles();

    viewModel.operationResult.observe(getViewLifecycleOwner(), result -> {
      if (result != null)
        GhostToast.makeText(getContext(), result.getMessage(), GhostToast.LENGTH_SHORT).show();
    });
  }

  // ─── Inner Adapter ────────────────────────────────────────────
  private class ConflictAdapter extends RecyclerView.Adapter<ConflictAdapter.VH> {
    private List<ConflictFile> list = new ArrayList<>();

    void submitList(List<ConflictFile> l) {
      list = l != null ? l : new ArrayList<>();
      notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
      View v = LayoutInflater.from(parent.getContext())
          .inflate(R.layout.item_conflict_file, parent, false);
      return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
      ConflictFile cf = list.get(position);
      holder.tvPath.setText(cf.getPath());

      holder.btnOurs.setOnClickListener(v ->
          new DialogCompat(requireContext())
              .setTitle("Use Ours")
              .setMessage("Keep your local version of:\n" + cf.getPath() + "?")
              .setPositiveButton("Confirm", (d, w) ->
                  viewModel.resolveConflictWithOurs(cf.getPath()))
              .setNegativeButton("Cancel", null).show());

      holder.btnTheirs.setOnClickListener(v ->
          new DialogCompat(requireContext())
              .setTitle("Use Theirs")
              .setMessage("Use incoming version of:\n" + cf.getPath() + "?")
              .setPositiveButton("Confirm", (d, w) ->
                  viewModel.resolveConflictWithTheirs(cf.getPath()))
              .setNegativeButton("Cancel", null).show());

      holder.btnEdit.setOnClickListener(v -> showManualEditor(cf));
    }

    @Override
    public int getItemCount() { return list.size(); }

    class VH extends RecyclerView.ViewHolder {
      TextView tvPath;
      Button btnOurs, btnTheirs, btnEdit;
      VH(View v) {
        super(v);
        tvPath = v.findViewById(R.id.tvConflictPath);
        btnOurs = v.findViewById(R.id.btnUseOurs);
        btnTheirs = v.findViewById(R.id.btnUseTheirs);
        btnEdit = v.findViewById(R.id.btnEditManual);
      }
    }
  }

  private void showManualEditor(ConflictFile cf) {
    View dialogView = LayoutInflater.from(requireContext())
        .inflate(android.R.layout.simple_list_item_1, null);

    // Show ours vs theirs as a simple chooser with preview
    String preview = "─── OURS ───\n" + truncate(cf.getOursContent(), 300)
        + "\n─── THEIRS ───\n" + truncate(cf.getTheirsContent(), 300);

    new DialogCompat(requireContext())
        .setTitle("Manual Resolve: " + cf.getPath())
        .setMessage(preview)
        .setPositiveButton("Use Ours", (d, w) ->
            viewModel.resolveConflictWithOurs(cf.getPath()))
        .setNeutralButton("Use Theirs", (d, w) ->
            viewModel.resolveConflictWithTheirs(cf.getPath()))
        .setNegativeButton("Cancel", null)
        .show();
  }

  private String truncate(String s, int max) {
    if (s == null) return "";
    return s.length() > max ? s.substring(0, max) + "…" : s;
  }
}
