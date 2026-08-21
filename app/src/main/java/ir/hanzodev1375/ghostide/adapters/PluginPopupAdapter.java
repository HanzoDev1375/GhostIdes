package ir.hanzodev1375.ghostide.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import ir.hanzodev1375.ghostide.R;
import ir.hanzodev1375.ghostide.interfaces.OnItemClickListener;
import ir.hanzodev1375.ghostide.plugin.gpl.GplManifest;
import ir.hanzodev1375.ghostide.plugin.gpl.GplManifestReader;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class PluginPopupAdapter extends RecyclerView.Adapter<PluginPopupAdapter.VH> {

  public record PluginItem(String id, String name, File gplFile, GplManifest manifest) {}

  private final List<PluginItem> items = new ArrayList<>();
  private final OnItemClickListener<PluginItem> listener;
  private int fallbackIconRes = R.mipmap.ic_lego_foreground;

  public PluginPopupAdapter(OnItemClickListener<PluginItem> listener) {
    this.listener = listener;
  }

  public void setFallbackIcon(int res) {
    this.fallbackIconRes = res;
  }

  public void submit(List<PluginItem> newItems) {
    items.clear();
    items.addAll(newItems);
    notifyDataSetChanged();
  }

  @NonNull
  @Override
  public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View v =
        LayoutInflater.from(parent.getContext())
            .inflate(R.layout.glass_plugin_item, parent, false);
    return new VH(v);
  }

  @Override
  public void onBindViewHolder(@NonNull VH holder, int position) {
    PluginItem item = items.get(position);
    holder.name.setText(item.name());

    byte[] iconBytes = GplManifestReader.readIconBytes(item.gplFile(), item.manifest());
    if (iconBytes != null) {
      Bitmap bitmap = BitmapFactory.decodeByteArray(iconBytes, 0, iconBytes.length);
      Glide.with(holder.icon.getContext())
          .asBitmap()
          .load(bitmap)
          .centerInside()
          .into(holder.icon);
    } else {
      holder.icon.setImageResource(fallbackIconRes);
    }

    holder.itemView.setOnClickListener(
        v -> listener.onClick(v, item, holder.getBindingAdapterPosition()));
  }

  @Override
  public int getItemCount() {
    return items.size();
  }

  static final class VH extends RecyclerView.ViewHolder {
    final ImageView icon;
    final TextView name;

    VH(@NonNull View itemView) {
      super(itemView);
      icon = itemView.findViewById(R.id.pluginIcon);
      name = itemView.findViewById(R.id.pluginName);
    }
  }
}
