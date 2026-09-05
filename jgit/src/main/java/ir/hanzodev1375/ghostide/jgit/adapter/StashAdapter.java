package ir.hanzodev1375.ghostide.jgit.adapter;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import ir.hanzodev1375.ghostide.jgit.R;
import ir.hanzodev1375.ghostide.jgit.jgitandroid.model.StashInfo;
import ir.theme.M3Theme;

public class StashAdapter extends RecyclerView.Adapter<StashAdapter.ViewHolder> {
  private List<StashInfo> stashes = new ArrayList<>();
  private OnStashActionListener listener;

  public interface OnStashActionListener {
    void onPop(StashInfo stash);
    void onApply(StashInfo stash);
    void onDrop(StashInfo stash);
  }

  public void setOnStashActionListener(OnStashActionListener l) { this.listener = l; }

  public void submitList(List<StashInfo> list) {
    stashes = list != null ? list : new ArrayList<>();
    notifyDataSetChanged();
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.item_stash, parent, false);
    return new ViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    StashInfo stash = stashes.get(position);
    holder.tvMessage.setText("stash@{" + stash.getIndex() + "}: " + stash.getMessage());
    String date = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        .format(new Date(stash.getTimestamp()));
    holder.tvHash.setText(stash.getShortHash() + " · " + date);

    holder.btnPop.setOnClickListener(v -> { if (listener != null) listener.onPop(stash); });
    holder.btnApply.setOnClickListener(v -> { if (listener != null) listener.onApply(stash); });
    holder.btnDrop.setOnClickListener(v -> { if (listener != null) listener.onDrop(stash); });
    M3Theme.listCard(holder.itemView);
    Integer error = M3Theme.error();
    if (error != null) holder.btnDrop.setBackgroundTintList(ColorStateList.valueOf(error));
  }

  @Override
  public int getItemCount() { return stashes.size(); }

  static class ViewHolder extends RecyclerView.ViewHolder {
    TextView tvMessage, tvHash;
    Button btnPop, btnApply, btnDrop;

    ViewHolder(View itemView) {
      super(itemView);
      tvMessage = itemView.findViewById(R.id.tvStashMessage);
      tvHash = itemView.findViewById(R.id.tvStashHash);
      btnPop = itemView.findViewById(R.id.btnStashPop);
      btnApply = itemView.findViewById(R.id.btnStashApply);
      btnDrop = itemView.findViewById(R.id.btnStashDrop);
    }
  }
}
