package ir.hanzodev1375.components.store.event;

import ir.hanzodev1375.components.store.model.PluginItem;

public class PluginSetupRequestEvent {

  public final PluginItem plugin;

  public PluginSetupRequestEvent(PluginItem plugin) {
    this.plugin = plugin;
  }
}