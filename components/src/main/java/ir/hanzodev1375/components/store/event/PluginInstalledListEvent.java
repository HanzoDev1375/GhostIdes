package ir.hanzodev1375.components.store.event;

import java.util.List;

/** Posted by the host after startup so the ViewModel can mark already-installed plugins. */
public class PluginInstalledListEvent {

  public final List<String> installedIds;

  public PluginInstalledListEvent(List<String> installedIds) {
    this.installedIds = installedIds;
  }
}