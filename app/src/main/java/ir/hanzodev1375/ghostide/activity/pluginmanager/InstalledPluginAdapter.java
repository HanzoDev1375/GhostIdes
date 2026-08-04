package ir.hanzodev1375.ghostide.activity.pluginmanager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import ir.hanzodev1375.ghostide.R;

/** Shows the currently installed plugins and lets the user filter or uninstall one. */
public final class InstalledPluginAdapter extends RecyclerView.Adapter<InstalledPluginAdapter.ViewHolder> {

  /** Called when the user taps the uninstall button on a row. */
  public interface OnUninstallListener {
    void onUninstall(InstalledPluginInfo plugin);
  }

  private final List<InstalledPluginInfo> allPlugins = new ArrayList<>();
  private final List<InstalledPluginInfo> visiblePlugins = new ArrayList<>();
  private final OnUninstallListener uninstallListener;
  private String query = "";

  public InstalledPluginAdapter(OnUninstallListener uninstallListener) {
    this.uninstallListener = uninstallListener;
  }

  public void submit(List<InstalledPluginInfo> plugins) {
    allPlugins.clear();
    allPlugins.addAll(plugins);
    applyFilter();
  }

  public void filter(String query) {
    this.query = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    applyFilter();
  }

  private void applyFilter() {
    visiblePlugins.clear();
    if (query.isEmpty()) {
      visiblePlugins.addAll(allPlugins);
    } else {
      for (InstalledPluginInfo plugin : allPlugins) {
        if (plugin.manifest().name().toLowerCase(Locale.ROOT).contains(query)
            || plugin.manifest().id().toLowerCase(Locale.ROOT).contains(query)) {
          visiblePlugins.add(plugin);
        }
      }
    }
    notifyDataSetChanged();
  }

  public boolean isEmpty() {
    return visiblePlugins.isEmpty();
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view =
        LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_installed_plugin, parent, false);
    return new ViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    InstalledPluginInfo plugin = visiblePlugins.get(position);
    holder.name.setText(plugin.manifest().name());
    holder.version.setText(plugin.manifest().version());
    holder.description.setText(plugin.manifest().description());
    holder.uninstall.setOnClickListener(v -> uninstallListener.onUninstall(plugin));
  }

  @Override
  public int getItemCount() {
    return visiblePlugins.size();
  }

  static final class ViewHolder extends RecyclerView.ViewHolder {

    final TextView name;
    final TextView version;
    final TextView description;
    final ImageButton uninstall;

    ViewHolder(@NonNull View itemView) {
      super(itemView);
      name = itemView.findViewById(R.id.pluginName);
      version = itemView.findViewById(R.id.pluginVersion);
      description = itemView.findViewById(R.id.pluginDescription);
      uninstall = itemView.findViewById(R.id.btnUninstall);
    }
  }
}
