package ir.hanzodev1375.ghostide.plugin.gpl;

import ir.hanzodev1375.ghostide.plugin.api.GhostPlugin;
import ir.hanzodev1375.ghostide.plugin.api.PluginDescriptor;

/** Everything the loader needs to keep track of and later unload one activated plugin. */
public final class LoadedGplPlugin {

  private final PluginDescriptor descriptor;
  private final GhostPlugin plugin;
  private final DefaultPluginContext context;

  LoadedGplPlugin(PluginDescriptor descriptor, GhostPlugin plugin, DefaultPluginContext context) {
    this.descriptor = descriptor;
    this.plugin = plugin;
    this.context = context;
  }

  public PluginDescriptor getDescriptor() {
    return descriptor;
  }

  public GhostPlugin getPlugin() {
    return plugin;
  }

  void unload() {
    plugin.deactivate();
    context.disposeAll();
  }
}
