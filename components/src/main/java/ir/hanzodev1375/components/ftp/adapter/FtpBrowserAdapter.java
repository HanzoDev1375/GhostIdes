package ir.hanzodev1375.components.ftp.adapter;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import ir.hanzodev1375.components.ftp.model.FtpEntry;
import java.util.ArrayList;
import java.util.List;

import ir.hanzodev1375.components.R;
import ir.theme.M3Theme;

public class FtpBrowserAdapter extends RecyclerView.Adapter<FtpBrowserAdapter.VH> {

  private List<FtpEntry> items = new ArrayList<>();

  public interface OnItemClick {
    void onClick(FtpEntry entry);
  }

  public interface OnMoreClick {
    void onMore(FtpEntry entry, View anchor);
  }

  private OnItemClick itemClick;
  private OnMoreClick moreClick;

  public void setOnItemClick(OnItemClick l) {
    itemClick = l;
  }

  public void setOnMoreClick(OnMoreClick l) {
    moreClick = l;
  }

  public void submitList(List<FtpEntry> newList) {
    DiffUtil.DiffResult diff =
        DiffUtil.calculateDiff(
            new DiffUtil.Callback() {
              @Override
              public int getOldListSize() {
                return items.size();
              }

              @Override
              public int getNewListSize() {
                return newList.size();
              }

              @Override
              public boolean areItemsTheSame(int o, int n) {
                return items.get(o).getPath().equals(newList.get(n).getPath());
              }

              @Override
              public boolean areContentsTheSame(int o, int n) {
                return items.get(o).getName().equals(newList.get(n).getName())
                    && items.get(o).getSize() == newList.get(n).getSize();
              }
            });
    items = new ArrayList<>(newList);
    diff.dispatchUpdatesTo(this);
  }

  @NonNull
  @Override
  public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View v =
        LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ftp_file, parent, false);
    return new VH(v);
  }

  @Override
  public void onBindViewHolder(@NonNull VH h, int position) {
    FtpEntry entry = items.get(position);

    h.tvName.setText(entry.getName());
    h.tvSize.setText(entry.getFormattedSize());

    if (entry.isDirectory()) {
      h.ivIcon.setImageResource(android.R.drawable.ic_menu_agenda);
      h.ivIcon.setImageTintList(
          ColorStateList.valueOf(fallback(M3Theme.primary(), 0)));
      h.ivMore.setVisibility(View.VISIBLE);
    } else {
      h.ivIcon.setImageResource(android.R.drawable.ic_menu_edit);
      h.ivIcon.setImageTintList(
          ColorStateList.valueOf(fallback(M3Theme.onSurfaceVariant(), 0)));
      h.ivMore.setVisibility(View.VISIBLE);
    }

    h.card.setOnClickListener(
        v -> {
          if (itemClick != null) itemClick.onClick(entry);
        });

    h.ivMore.setOnClickListener(
        v -> {
          if (moreClick != null) moreClick.onMore(entry, v);
        });
    M3Theme.listCard(h.itemView);
  }

  @Override
  public int getItemCount() {
    return items.size();
  }

  static class VH extends RecyclerView.ViewHolder {
    MaterialCardView card;
    ImageView ivIcon;
    TextView tvName;
    TextView tvSize;
    ImageButton ivMore;

    VH(@NonNull View v) {
      super(v);
      card = v.findViewById(R.id.card);
      ivIcon = v.findViewById(R.id.ivIcon);
      tvName = v.findViewById(R.id.tvName);
      tvSize = v.findViewById(R.id.tvSize);
      ivMore = v.findViewById(R.id.ivMore);
    }
  }

  private static int fallback(Integer value, int def) {
    return value != null ? value : def;
  }
}