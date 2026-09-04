package ir.hanzodev1375.ghostide.plugin;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import java.util.ArrayList;
import java.util.List;
import ir.hanzodev1375.components.sheet.customitemsheet.ui.DialogCompat;
import ir.hanzodev1375.components.sheet.customitemsheet.ui.FabGlass;
import ir.hanzodev1375.components.sheet.customitemsheet.ui.GlassCompat;
import ir.hanzodev1375.ghostide.R;
import ir.hanzodev1375.ghostide.activity.BaseCompat;
import ir.theme.M3Theme;

public class PluginSettingsActivity extends BaseCompat {

  private RecyclerView recyclerView;
  private List<Plugin> plugins = new ArrayList<>();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_plugin_settings);
    setTitle("Plugin Settings");

    MaterialToolbar toolbar = findViewById(R.id.toolbar);
    toolbar.setTitle("Plugin Settings");
    Integer onSurface = M3Theme.onSurface();
    if (onSurface != null) {
      toolbar.setTitleTextColor(onSurface);
      toolbar.setSubtitleTextColor(onSurface);
    }
    toolbar.setNavigationIcon(R.drawable.ic_back);
    toolbar.setNavigationContentDescription(getString(R.string.title_settings));
    toolbar.setNavigationOnClickListener(v -> finish());

    GlassCompat glass = findViewById(R.id.glassBackdrop);
    glass.setEnableDynamicBackground(true);
    glass.setEnableChromaticAberration(true);
    glass.setEnableEdgeHighlight(true);
    glass.setEnableSensorHighlight(false);
    glass.setBackdropSource(findViewById(android.R.id.content));

    recyclerView = findViewById(R.id.recycler);
    recyclerView.setLayoutManager(new LinearLayoutManager(this));

    FabGlass fab = findViewById(R.id.refreshFab);
    fab.setEnableDynamicBackground(true);
    fab.setOnClickListener(v -> refreshPlugins());

    View root = findViewById(R.id.pluginSettingsRoot);
    ViewCompat.setOnApplyWindowInsetsListener(
        toolbar,
        (v, insets) -> {
          int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
          v.setPadding(0, top, 0, 0);
          return insets;
        });
    ViewCompat.setOnApplyWindowInsetsListener(
        root,
        (v, insets) -> {
          int bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
          v.setPadding(0, 0, 0, bottom);
          return insets;
        });

    refreshPlugins();
  }

  private void refreshPlugins() {
    plugins = new ArrayList<>(PluginManager.getInstance().getAllPlugins().values());
    recyclerView.setAdapter(new PluginSettingsAdapter(plugins, this));
  }

  private static class PluginSettingsAdapter
      extends RecyclerView.Adapter<PluginSettingsAdapter.ViewHolder> {
    private final List<Plugin> plugins;
    private final Context context;

    PluginSettingsAdapter(List<Plugin> plugins, Context context) {
      this.plugins = plugins;
      this.context = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
      TextView tv = new TextView(context);
      tv.setPadding(24, 20, 24, 20);
      tv.setTextSize(18);
      return new ViewHolder(tv);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
      Plugin p = plugins.get(position);
      holder.textView.setText(p.getName() + " v" + p.getVersion());
      M3Theme.text(holder.textView);
      if (p.hasUI()) {
        holder.textView.setOnClickListener(
            v -> {
              View settingsView = p.getSettingsView(context);
              if (settingsView != null) {
                M3Theme.applyTopLevel(settingsView);
                new DialogCompat(context)
                    .setTitle(p.getName() + " Settings")
                    .setView(settingsView)
                    .setPositiveButton("OK", null)
                    .show();
              }
            });
      } else {
        holder.textView.setOnClickListener(null);
      }
    }

    @Override
    public int getItemCount() {
      return plugins.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
      TextView textView;

      ViewHolder(TextView tv) {
        super(tv);
        textView = tv;
      }
    }
  }
}