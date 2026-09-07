package ir.hanzodev1375.components.store.event;

import ir.hanzodev1375.components.store.model.PluginItem;

/** Posted by the adapter when a plugin row is tapped. */
public class PluginStoreEvent {

  public final PluginItem plugin;

  public PluginStoreEvent(PluginItem plugin) {
    this.plugin = plugin;
  }
}