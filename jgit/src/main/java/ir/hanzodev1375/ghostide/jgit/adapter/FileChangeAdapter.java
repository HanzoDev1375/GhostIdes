package ir.hanzodev1375.ghostide.jgit.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import ir.hanzodev1375.ghostide.jgit.R;
import ir.hanzodev1375.ghostide.jgit.jgitandroid.model.FileChange;
import ir.theme.M3Theme;

public class FileChangeAdapter extends RecyclerView.Adapter<FileChangeAdapter.ViewHolder> {
  private List<FileChange> changes = new ArrayList<>();
  private OnItemClickListener listener;

  public interface OnItemClickListener {
    void onStageClick(FileChange change);

    void onUnstageClick(FileChange change);

    void onDiscardClick(FileChange change);
  }

  public void setOnItemClickListener(OnItemClickListener listener) {
    this.listener = listener;
  }

  public void submitList(List<FileChange> list) {
    changes = list != null ? list : new ArrayList<>();
    notifyDataSetChanged();
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view =
        LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file_change, parent, false);
    return new ViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    FileChange change = changes.get(position);
    holder.tvFileName.setText(change.getPath());
    holder.tvChangeType.setText(change.getChangeType().name());
    holder.chkStage.setChecked(change.isStaged());

    holder.itemView.setOnLongClickListener(
        v -> {
          if (listener != null) {
            if (change.isStaged()) listener.onUnstageClick(change);
            else listener.onDiscardClick(change);
          }
          return true;
        });

    holder.itemView.setOnClickListener(
        v -> {
          if (listener != null && !change.isStaged()) {
            listener.onStageClick(change);
          }
        });
    M3Theme.listCard(holder.itemView);
  }

  @Override
  public int getItemCount() {
    return changes.size();
  }

  static class ViewHolder extends RecyclerView.ViewHolder {
    TextView tvFileName, tvChangeType;
    CheckBox chkStage;

    ViewHolder(View itemView) {
      super(itemView);
      tvFileName = itemView.findViewById(R.id.tvFileName);
      tvChangeType = itemView.findViewById(R.id.tvChangeType);
      chkStage = itemView.findViewById(R.id.chkStage);
    }
  }
}
