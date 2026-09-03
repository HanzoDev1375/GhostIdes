package ir.hanzodev1375.components.store.adapter;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.listitem.ListItemCardView;

import com.google.android.material.listitem.ListItemViewHolder;
import java.util.List;
import java.util.Set;

import ir.hanzodev1375.components.R;
import ir.hanzodev1375.components.store.model.FontInfo;
import ir.hanzodev1375.components.utils.ComponentsPrefs;

public class FontsAdapter extends RecyclerView.Adapter<FontsAdapter.VH> {

  public interface OnDownloadClick {
    void onDownloadClick(FontInfo font, int position);
  }

  private final List<FontInfo> items;
  private final OnDownloadClick listener;
  private Set<String> downloading = new java.util.HashSet<>();
  private Set<String> downloaded = new java.util.HashSet<>();

  public FontsAdapter(List<FontInfo> items, OnDownloadClick listener) {
    this.items = items;
    this.listener = listener;
  }

  public void updateItems(List<FontInfo> newItems) {
    items.clear();
    items.addAll(newItems);
    notifyDataSetChanged();
  }

  public void setDownloading(Set<String> set) {
    downloading = set;
    notifyDataSetChanged();
  }

  public void setDownloaded(Set<String> set) {
    downloaded = set;
    notifyDataSetChanged();
  }

  @NonNull
  @Override
  public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_font, parent, false);
    return new VH(v);
  }

  @Override
  public void onBindViewHolder(@NonNull VH holder, int position) {
    FontInfo item = items.get(position);
    holder.name.setText(item.family != null ? item.family : "");
    holder.category.setText(item.category != null ? item.category : "");
    holder.bind(position,getItemCount());
    int bgColor =
        MaterialColors.getColor(
            holder.card,
            R.attr.colorSurfaceContainerLow,
            Color.TRANSPARENT);
    boolean showBg = new ComponentsPrefs(holder.itemView.getContext()).isShowBackground();
    holder.card.setCardBackgroundColor(
        ColorStateList.valueOf(showBg ? ColorUtils.setAlphaComponent(bgColor, 128) : bgColor));

    String family = item.family;
    if (downloading.contains(family)) {
      holder.download.setText(R.string.fonts_downloading);
      holder.download.setEnabled(false);
    } else if (downloaded.contains(family)) {
      holder.download.setText(R.string.fonts_downloaded);
      holder.download.setEnabled(false);
    } else {
      holder.download.setText(R.string.fonts_download);
      holder.download.setEnabled(true);
    }
    holder.download.setOnClickListener(
        v -> {
          if (listener != null) listener.onDownloadClick(item, holder.getBindingAdapterPosition());
        });
  }

  @Override
  public int getItemCount() {
    return items.size();
  }

  static class VH extends ListItemViewHolder {
    final ListItemCardView card;
    final TextView preview;
    final TextView name;
    final TextView category;
    final MaterialButton download;

    VH(View v) {
      super(v);
      card = v.findViewById(R.id.listcard);
      preview = v.findViewById(R.id.fontPreview);
      name = v.findViewById(R.id.fontName);
      category = v.findViewById(R.id.fontCategory);
      download = v.findViewById(R.id.downloadButton);
    }
  }
}
