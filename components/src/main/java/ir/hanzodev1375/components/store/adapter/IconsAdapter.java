package ir.hanzodev1375.components.store.adapter;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.listitem.ListItemCardView;
import com.google.android.material.listitem.ListItemViewHolder;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import ir.hanzodev1375.components.R;
import ir.hanzodev1375.components.store.api.IconsApi;
import ir.hanzodev1375.components.store.model.IconInfo;
import ir.hanzodev1375.components.utils.ComponentsPrefs;

public class IconsAdapter extends RecyclerView.Adapter<IconsAdapter.VH> {

  public interface OnDownloadClick {
    void onDownloadClick(IconInfo icon, int position);
  }

  private final List<IconInfo> items;
  private final OnDownloadClick listener;
  private int style = IconsApi.STYLE_OUTLINED;
  private Set<String> downloading = new HashSet<>();
  private Set<String> downloaded = new HashSet<>();

  public IconsAdapter(List<IconInfo> items, OnDownloadClick listener) {
    this.items = items;
    this.listener = listener;
  }

  public void updateItems(List<IconInfo> newItems) {
    items.clear();
    items.addAll(newItems);
    notifyDataSetChanged();
  }

  public void setStyle(int style) {
    this.style = style;
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
    View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_icon, parent, false);
    return new VH(v);
  }

  @Override
  public void onBindViewHolder(@NonNull VH holder, int position) {
    IconInfo item = items.get(position);
    String name = item.name != null ? item.name : "";
    holder.name.setText(name);
    holder.bind(position, getItemCount());
    int bgColor =
        MaterialColors.getColor(holder.card, R.attr.colorSurfaceContainerLow, Color.TRANSPARENT);
    boolean showBg = new ComponentsPrefs(holder.itemView.getContext()).isShowBackground();
    holder.card.setCardBackgroundColor(
        ColorStateList.valueOf(showBg ? ColorUtils.setAlphaComponent(bgColor, 128) : bgColor));

    String svgUrl = IconsApi.svgUrl(item, style);
    if (svgUrl != null) {
      Glide.with(holder.preview.getContext())
          .load(svgUrl)
          .error(R.drawable.ic_more_vert_24)
          .into(holder.preview);
    } else {
      holder.preview.setImageResource(R.drawable.ic_more_vert_24);
    }
    var gd = new GradientDrawable();
    gd.setCornerRadius(0.50f);
    gd.setColor(MaterialColors.getColor(holder.preview,R.attr.colorOnSurface));
    holder.preview.setBackground(gd);
    boolean supported = IconsApi.isSupported(item, style);
    holder.unsupported.setVisibility(supported ? View.GONE : View.VISIBLE);

    String key = name + "_s" + style;
    if (!supported) {
      holder.download.setEnabled(false);
      holder.download.setText(R.string.icons_unsupported);
    } else if (downloading.contains(key)) {
      holder.download.setEnabled(false);
      holder.download.setText(R.string.icons_downloading);
    } else if (downloaded.contains(key)) {
      holder.download.setEnabled(false);
      holder.download.setText(R.string.icons_downloaded);
    } else {
      holder.download.setEnabled(true);
      holder.download.setText(R.string.icons_download);
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
    final ImageView preview;
    final TextView name;
    final TextView unsupported;
    final MaterialButton download;

    VH(View v) {
      super(v);
      card = v.findViewById(R.id.listcard);
      preview = v.findViewById(R.id.iconPreview);
      name = v.findViewById(R.id.iconName);
      unsupported = v.findViewById(R.id.iconUnsupported);
      download = v.findViewById(R.id.iconDownloadButton);
    }
  }
}
