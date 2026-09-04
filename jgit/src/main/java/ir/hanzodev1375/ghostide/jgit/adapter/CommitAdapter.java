package ir.hanzodev1375.ghostide.jgit.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import ir.hanzodev1375.ghostide.jgit.R;
import ir.hanzodev1375.ghostide.jgit.jgitandroid.model.CommitInfo;
import ir.theme.M3Theme;

public class CommitAdapter extends RecyclerView.Adapter<CommitAdapter.ViewHolder> {
  private List<CommitInfo> commits = new ArrayList<>();
  private final SimpleDateFormat sdf =
      new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault());
  private OnCommitClickListener listener;

  public interface OnCommitClickListener {
    void onClick(CommitInfo commit);
  }

  public void setOnCommitClickListener(OnCommitClickListener listener) {
    this.listener = listener;
  }

  public void submitList(List<CommitInfo> list) {
    commits = list != null ? list : new ArrayList<>();
    notifyDataSetChanged();
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view =
        LayoutInflater.from(parent.getContext()).inflate(R.layout.item_commit, parent, false);
    return new ViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    CommitInfo commit = commits.get(position);
    holder.tvHash.setText(commit.getShortHash());
    holder.tvMessage.setText(commit.getMessage());
    String date = sdf.format(new Date(commit.getTimestamp()));
    holder.tvAuthor.setText(commit.getAuthor() + " - " + date);
    holder.itemView.setOnClickListener(
        v -> {
          if (listener != null) listener.onClick(commit);
        });
    M3Theme.listCard(holder.itemView);
  }

  @Override
  public int getItemCount() {
    return commits.size();
  }

  static class ViewHolder extends RecyclerView.ViewHolder {
    TextView tvHash, tvMessage, tvAuthor;

    ViewHolder(View itemView) {
      super(itemView);
      tvHash = itemView.findViewById(R.id.tvCommitHash);
      tvMessage = itemView.findViewById(R.id.tvCommitMessage);
      tvAuthor = itemView.findViewById(R.id.tvCommitAuthor);
    }
  }
}
