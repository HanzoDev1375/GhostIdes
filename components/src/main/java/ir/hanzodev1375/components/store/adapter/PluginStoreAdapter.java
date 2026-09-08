package ir.hanzodev1375.components.store.adapter;

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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ir.hanzodev1375.components.R;
import ir.hanzodev1375.components.store.event.PluginStoreEvent;
import ir.hanzodev1375.components.store.model.PluginItem;
import ir.theme.M3Theme;
import org.greenrobot.eventbus.EventBus;

public class PluginStoreAdapter extends RecyclerView.Adapter<PluginStoreAdapter.VH> {

  private final List<PluginItem> items = new ArrayList<>();
  private Set<String> busy = new HashSet<>();
  private Set<String> installed = new HashSet<>();

  public void updateItems(List<PluginItem> newItems) {
    items.clear();
    if (newItems != null) {
      items.addAll(newItems);
    }
    notifyDataSetChanged();
  }

  public void setBusy(Set<String> set) {
    busy = set;
    notifyDataSetChanged();
  }

  public void setInstalled(Set<String> set) {
    installed = set;
    notifyDataSetChanged();
  }

  @NonNull
  @Override
  public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_plugin, parent, false);
    return new VH(view);
  }

  @Override
  public void onBindViewHolder(@NonNull VH holder, int position) {
    PluginItem item = items.get(position);
    holder.name.setText(item.name());
    Glide.with(holder.icon).load(item.icon()).into(holder.icon);

    Integer surface = M3Theme.surfaceContainerLow();
    int bg = surface != null ? ColorUtils.setAlphaComponent(surface, 128) : Color.TRANSPARENT;
    holder.card.setCardBackgroundColor(ColorStateList.valueOf(bg));

    if (installed.contains(item.name())) {
      holder.action.setText(R.string.pluginstore_installed_button);
      holder.action.setEnabled(false);
    } else if (busy.contains(item.name())) {
      holder.action.setText(R.string.pluginstore_busy);
      holder.action.setEnabled(false);
    } else {
      holder.action.setText(R.string.pluginstore_install);
      holder.action.setEnabled(true);
    }

    holder.action.setOnClickListener(v -> EventBus.getDefault().post(new PluginStoreEvent(item)));
    holder.card.setOnClickListener(v -> EventBus.getDefault().post(new PluginStoreEvent(item)));
    M3Theme.listCard(holder.itemView);
  }

  @Override
  public int getItemCount() {
    return items.size();
  }

  static class VH extends RecyclerView.ViewHolder {
    final MaterialCardView card;
    final ImageView icon;
    final TextView name;
    final MaterialButton action;

    VH(View v) {
      super(v);
      card = v.findViewById(R.id.pluginCard);
      icon = v.findViewById(R.id.pluginIcon);
      name = v.findViewById(R.id.pluginName);
      action = v.findViewById(R.id.pluginAction);
    }
  }
}