package ir.hanzodev1375.ghostide.jgit.adapter;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;
import ir.hanzodev1375.components.utils.GlassColors;
import ir.theme.M3Theme;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import ir.hanzodev1375.ghostide.jgit.R;
import ir.hanzodev1375.ghostide.jgit.jgitandroid.model.TagInfo;

public class TagAdapter extends RecyclerView.Adapter<TagAdapter.ViewHolder> {
  private List<TagInfo> tags = new ArrayList<>();
  private OnTagActionListener listener;

  public interface OnTagActionListener {
    void onDelete(TagInfo tag);
  }

  public void setOnTagActionListener(OnTagActionListener l) { this.listener = l; }

  public void submitList(List<TagInfo> list) {
    tags = list != null ? list : new ArrayList<>();
    notifyDataSetChanged();
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    return new ViewHolder(LayoutInflater.from(parent.getContext())
        .inflate(R.layout.item_tag, parent, false));
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    TagInfo tag = tags.get(position);
    GlassColors.setBackgroundAlpha(
        holder.itemView,
        fallback(M3Theme.surface(), 0),
        150);
    holder.tvName.setText(tag.getName());
    holder.tvHash.setText(tag.getHash()
        + (tag.getTimestamp() > 0
            ? " · " + new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                .format(new Date(tag.getTimestamp()))
            : ""));
    if (tag.getMessage() != null && !tag.getMessage().isEmpty()) {
      holder.tvMessage.setVisibility(View.VISIBLE);
      holder.tvMessage.setText(tag.getMessage().trim());
    } else {
      holder.tvMessage.setVisibility(View.GONE);
    }
    holder.btnDelete.setOnClickListener(v -> { if (listener != null) listener.onDelete(tag); });
    M3Theme.listCard(holder.itemView);
    Integer error = M3Theme.errorContainer();
    if (error != null) holder.btnDelete.setBackgroundTintList(ColorStateList.valueOf(error));
    Integer onError = M3Theme.onErrorContainer();
    if (onError != null) holder.btnDelete.setTextColor(onError);
  }

  @Override
  public int getItemCount() { return tags.size(); }

  static class ViewHolder extends RecyclerView.ViewHolder {
    MaterialTextView tvName, tvHash, tvMessage;
    MaterialButton btnDelete;
    ViewHolder(View v) {
      super(v);
      tvName = v.findViewById(R.id.tvTagName);
      tvHash = v.findViewById(R.id.tvTagHash);
      tvMessage = v.findViewById(R.id.tvTagMessage);
      btnDelete = v.findViewById(R.id.btnDeleteTag);
    }
  }

  private static int fallback(Integer value, int def) {
    return value != null ? value : def;
  }
}
