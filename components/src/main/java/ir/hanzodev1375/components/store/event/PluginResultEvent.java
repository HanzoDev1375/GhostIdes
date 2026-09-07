package ir.hanzodev1375.components.store.event;

import ir.hanzodev1375.components.store.model.PluginItem;

/** Posted by the host after an install attempt so the ViewModel can update the UI. */
public class PluginResultEvent {

  public final PluginItem plugin;
  public final boolean success;
  public final String message;

  public PluginResultEvent(PluginItem plugin, boolean success, String message) {
    this.plugin = plugin;
    this.success = success;
    this.message = message;
  }
}