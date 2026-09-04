package ir.hanzodev1375.ghostide.history;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import ir.hanzodev1375.ghostide.materialfileicon.core.FileIconHelper;
import com.bumptech.glide.Glide;

import com.google.android.material.listitem.ListItemCardView;
import com.google.android.material.listitem.ListItemViewHolder;
import java.util.ArrayList;
import java.util.List;
import ir.theme.M3Theme;
import ir.hanzodev1375.ghostide.R;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

  public interface OnHistoryClickListener {
    void onHistoryClick(HistoryEntity item);
  }

  private List<HistoryEntity> items = new ArrayList<>();
  private final OnHistoryClickListener clickListener;

  public HistoryAdapter(OnHistoryClickListener listener) {
    this.clickListener = listener;
  }

  public void submitList(List<HistoryEntity> newList) {
    List<HistoryEntity> safeNew = newList != null ? newList : new ArrayList<>();
    DiffUtil.DiffResult result =
        DiffUtil.calculateDiff(
            new DiffUtil.Callback() {
              @Override
              public int getOldListSize() {
                return items.size();
              }

              @Override
              public int getNewListSize() {
                return safeNew.size();
              }

              @Override
              public boolean areItemsTheSame(int oldPos, int newPos) {
                // مقایسه با path (چون id نداریم)
                return items.get(oldPos).path.equals(safeNew.get(newPos).path);
              }

              @Override
              public boolean areContentsTheSame(int oldPos, int newPos) {
                HistoryEntity oldItem = items.get(oldPos);
                HistoryEntity newItem = safeNew.get(newPos);
                return oldItem.path.equals(newItem.path)
                    && oldItem.name.equals(newItem.name)
                    && oldItem.isDirectory == newItem.isDirectory
                    && oldItem.timestamp == newItem.timestamp;
              }
            });
    items = safeNew;
    result.dispatchUpdatesTo(this);
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view =
        LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
    return new ViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    holder.bindItem(items.get(position));
    M3Theme.listCard(holder.itemView);
  }

  @Override
  public int getItemCount() {
    return items.size();
  }

  class ViewHolder extends ListItemViewHolder {
    ImageView ivIcon;
    TextView tvName;
    TextView tvPath;
    ListItemCardView card;

    ViewHolder(@NonNull View itemView) {
      super(itemView);
      ivIcon = itemView.findViewById(R.id.ivIcon);
      tvName = itemView.findViewById(R.id.tvName);
      tvPath = itemView.findViewById(R.id.tvPath);
      card = itemView.findViewById(R.id.historyCard);

      itemView.setOnClickListener(
          v -> {
            int pos = getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && clickListener != null) {
              clickListener.onHistoryClick(items.get(pos));
            }
          });
    }

    void bindItem(HistoryEntity item) {
      tvName.setText(item.name);
      tvPath.setText(item.path);
      var iconHelper = new FileIconHelper(item.path);
      iconHelper.setDynamicFolderEnabled(true);
      iconHelper.setEnvironmentEnabled(true);
      Glide.with(ivIcon.getContext())
          .load(iconHelper.getFileIcon())
          .error(R.drawable.ic_close)
          .into(ivIcon);

      var gd = new GradientDrawable();
      gd.setColor(fallback(M3Theme.surface(), 0));
      gd.setStroke(
          1, fallback(M3Theme.outline(), 0));
      gd.setCornerRadius(8);
      ivIcon.setPadding(5, 5, 5, 5);
      ivIcon.setBackground(gd);

      tvPath.setTextColor(
          fallback(M3Theme.onSurfaceVariant(), 0));
    }
  }

  private static int fallback(Integer value, int def) {
    return value != null ? value : def;
  }
}
