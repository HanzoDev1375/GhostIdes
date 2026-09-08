package ir.hanzodev1375.components.store.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.listitem.ListItemCardView;
import com.google.android.material.listitem.ListItemViewHolder;
import java.util.List;
import ir.hanzodev1375.components.R;
import ir.hanzodev1375.components.store.api.ThemesApi;
import ir.hanzodev1375.components.store.model.ThemeItem;
import ir.hanzodev1375.components.utils.ComponentsPrefs;
import ir.theme.M3Theme;

public class ThemesAdapter extends RecyclerView.Adapter<ThemesAdapter.VH> {

  public interface OnThemeClick {
    void onThemeClick(ThemeItem theme, int position);
  }

  private final List<ThemeItem> items;
  private final OnThemeClick listener;
  private final Context context;

  public ThemesAdapter(Context context, List<ThemeItem> items, OnThemeClick listener) {
    this.context = context;
    this.items = items;
    this.listener = listener;
  }

  public void updateItems(List<ThemeItem> newItems) {
    items.clear();
    items.addAll(newItems);
    notifyDataSetChanged();
  }

  @NonNull
  @Override
  public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_theme, parent, false);
    return new VH(v);
  }

  @Override
  public void onBindViewHolder(@NonNull VH holder, int position) {
    ThemeItem item = items.get(position);
    holder.name.setText(item.name() != null ? item.name() : "");
    holder.dev.setText(
        context.getString(R.string.themes_dev_version, item.devname(), item.version()));
    holder.bind(position, getItemCount());

    int bgColor = fallback(M3Theme.surfaceContainerLow(), Color.TRANSPARENT);
    boolean showBg = new ComponentsPrefs(holder.itemView.getContext()).isShowBackground();
    holder.card.setCardBackgroundColor(
        ColorStateList.valueOf(showBg ? ColorUtils.setAlphaComponent(bgColor, 128) : bgColor));
    Glide.with(context)
        .load(item.icon())
        .placeholder(R.drawable.ic_outline_palette)
        .error(R.drawable.ic_outline_palette)
        .into(holder.icon);

    M3Theme.text(holder.name, holder.dev);

    holder.itemView.setOnClickListener(
        v -> {
          if (listener != null) listener.onThemeClick(item, holder.getBindingAdapterPosition());
        });
    M3Theme.listCard(holder.itemView);
  }

  @Override
  public int getItemCount() {
    return items.size();
  }

  static class VH extends ListItemViewHolder {
    final ListItemCardView card;
    final ImageView icon;
    final TextView name;
    final TextView dev;

    VH(View v) {
      super(v);
      card = v.findViewById(R.id.listcard);
      icon = v.findViewById(R.id.themeIcon);
      name = v.findViewById(R.id.themeName);
      dev = v.findViewById(R.id.themeDev);
    }
  }

  private static int fallback(Integer value, int def) {
    return value != null ? value : def;
  }
}
