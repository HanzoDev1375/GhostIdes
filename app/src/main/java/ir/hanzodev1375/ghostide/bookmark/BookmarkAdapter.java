package ir.hanzodev1375.ghostide.bookmark;

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
import com.google.android.material.color.MaterialColors;
import com.google.android.material.listitem.ListItemCardView;
import com.google.android.material.listitem.ListItemViewHolder;

import java.util.ArrayList;
import java.util.List;

import ir.hanzodev1375.ghostide.R;

public class BookmarkAdapter extends RecyclerView.Adapter<BookmarkAdapter.ViewHolder> {

  public interface OnBookmarkClickListener {
    void onBookmarkClick(BookmarkEntity item);
  }

  public interface OnBookmarkRemoveListener {
    void onBookmarkRemove(BookmarkEntity item);
  }

  private List<BookmarkEntity> items = new ArrayList<>();
  private final OnBookmarkClickListener clickListener;
  private final OnBookmarkRemoveListener removeListener;

  public BookmarkAdapter(
      OnBookmarkClickListener clickListener, OnBookmarkRemoveListener removeListener) {
    this.clickListener = clickListener;
    this.removeListener = removeListener;
  }

  public void submitList(List<BookmarkEntity> newList) {
    List<BookmarkEntity> safeNew = newList != null ? newList : new ArrayList<>();
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
              public boolean areItemsTheSame(int o, int n) {
                return items.get(o).id == safeNew.get(n).id;
              }

              @Override
              public boolean areContentsTheSame(int o, int n) {
                return items.get(o).path.equals(safeNew.get(n).path);
              }
            });
    items = safeNew;
    result.dispatchUpdatesTo(this);
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view =
        LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bookmark, parent, false);
    return new ViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    holder.bind(items.get(position));
  }

  @Override
  public int getItemCount() {
    return items.size();
  }

  class ViewHolder extends ListItemViewHolder {
    ImageView ivIcon;
    TextView tvName;
    TextView tvPath;
    ImageView ivRemove;
    ListItemCardView card;

    ViewHolder(@NonNull View itemView) {
      super(itemView);
      ivIcon = itemView.findViewById(R.id.ivIcon);
      tvName = itemView.findViewById(R.id.tvName);
      tvPath = itemView.findViewById(R.id.tvPath);
      ivRemove = itemView.findViewById(R.id.ivRemove);
      card = itemView.findViewById(R.id.bookmarkCard);

      itemView.setOnClickListener(
          v -> {
            int pos = getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && clickListener != null)
              clickListener.onBookmarkClick(items.get(pos));
          });

      ivRemove.setOnClickListener(
          v -> {
            int pos = getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && removeListener != null)
              removeListener.onBookmarkRemove(items.get(pos));
          });
    }

    void bind(BookmarkEntity item) {
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
      gd.setColor(MaterialColors.getColor(ivIcon, com.google.android.material.R.attr.colorSurface));
      gd.setStroke(
          1, MaterialColors.getColor(ivIcon, com.google.android.material.R.attr.colorOutline));
      gd.setCornerRadius(8);
      ivIcon.setPadding(5, 5, 5, 5);
      ivIcon.setBackground(gd);

      tvPath.setTextColor(
          MaterialColors.getColor(
              tvPath, com.google.android.material.R.attr.colorOnSurfaceVariant));
    }
  }
}
