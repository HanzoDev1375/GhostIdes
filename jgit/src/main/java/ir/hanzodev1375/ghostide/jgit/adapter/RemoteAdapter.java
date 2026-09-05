package ir.hanzodev1375.ghostide.jgit.adapter;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import ir.hanzodev1375.ghostide.jgit.R;
import ir.hanzodev1375.ghostide.jgit.jgitandroid.model.RemoteInfo;
import ir.theme.M3Theme;

public class RemoteAdapter extends RecyclerView.Adapter<RemoteAdapter.ViewHolder> {
  private List<RemoteInfo> remotes = new ArrayList<>();
  private OnRemoteActionListener listener;

  public interface OnRemoteActionListener {
    void onRemove(RemoteInfo remote);

    void onPush(RemoteInfo remote);

    void onPull(RemoteInfo remote);

    void onFetch(RemoteInfo remote);
  }

  public void setOnRemoteActionListener(OnRemoteActionListener listener) {
    this.listener = listener;
  }

  public void submitList(List<RemoteInfo> list) {
    remotes = list != null ? list : new ArrayList<>();
    notifyDataSetChanged();
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view =
        LayoutInflater.from(parent.getContext()).inflate(R.layout.item_remote, parent, false);
    return new ViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    RemoteInfo remote = remotes.get(position);
    holder.tvName.setText(remote.getName());
    holder.tvUrl.setText(remote.getFetchUrl());

    holder.btnRemove.setOnClickListener(
        v -> {
          if (listener != null) listener.onRemove(remote);
        });
    holder.btnPush.setOnClickListener(
        v -> {
          if (listener != null) listener.onPush(remote);
        });
    holder.btnPull.setOnClickListener(
        v -> {
          if (listener != null) listener.onPull(remote);
        });
    holder.btnFetch.setOnClickListener(
        v -> {
          if (listener != null) listener.onFetch(remote);
        });
    M3Theme.listCard(holder.itemView);
    Integer error = M3Theme.error();
    if (error != null) holder.btnRemove.setBackgroundTintList(ColorStateList.valueOf(error));
  }

  @Override
  public int getItemCount() {
    return remotes.size();
  }

  static class ViewHolder extends RecyclerView.ViewHolder {
    TextView tvName, tvUrl;
    Button btnRemove, btnPush, btnPull, btnFetch;

    ViewHolder(View itemView) {
      super(itemView);
      tvName = itemView.findViewById(R.id.tvRemoteName);
      tvUrl = itemView.findViewById(R.id.tvRemoteUrl);
      btnRemove = itemView.findViewById(R.id.btnRemoveRemote);
      btnPush = itemView.findViewById(R.id.btnPush);
      btnPull = itemView.findViewById(R.id.btnPull);
      btnFetch = itemView.findViewById(R.id.btnFetch);
    }
  }
}
