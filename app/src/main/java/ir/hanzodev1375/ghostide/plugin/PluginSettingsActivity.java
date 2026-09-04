package ir.hanzodev1375.ghostide.plugin;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import ir.hanzodev1375.components.sheet.customitemsheet.ui.DialogCompat;
import ir.theme.M3Theme;
public class PluginSettingsActivity extends AppCompatActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setTitle("Plugin Settings");
    LinearLayout layout = new LinearLayout(this);
    layout.setOrientation(LinearLayout.VERTICAL);
    layout.setPadding(16, 16, 16, 16);
    RecyclerView recyclerView = new RecyclerView(this);
    recyclerView.setLayoutManager(new LinearLayoutManager(this));
    List<Plugin> plugins = new ArrayList<>(PluginManager.getInstance().getAllPlugins().values());
    PluginSettingsAdapter adapter = new PluginSettingsAdapter(plugins, this);
    recyclerView.setAdapter(adapter);
    layout.addView(recyclerView);
    setContentView(layout);
    M3Theme.applyTopLevel(layout);
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
      tv.setPadding(16, 16, 16, 16);
      tv.setTextSize(18);
      return new ViewHolder(tv);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
      Plugin p = plugins.get(position);
      holder.textView.setText(p.getName() + " v" + p.getVersion());
      if (p.hasUI()) {
        holder.textView.setOnClickListener(
            v -> {
              View settingsView = p.getSettingsView(context);
              if (settingsView != null) {
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
