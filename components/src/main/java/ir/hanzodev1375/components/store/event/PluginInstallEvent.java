package ir.hanzodev1375.components.store.event;

import ir.hanzodev1375.components.store.model.PluginItem;

/** The single install event: posted so the host (app module) installs the .gpl. */
public class PluginInstallEvent {

  public final PluginItem plugin;

  public PluginInstallEvent(PluginItem plugin) {
    this.plugin = plugin;
  }
}