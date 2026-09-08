package ir.hanzodev1375.components.store.event;

import ir.hanzodev1375.components.store.model.PluginSetupActionData;
import java.util.List;

public class PluginSetupEvent {

  public final String name;
  public final String iconUrl;
  public final List<PluginSetupActionData> actions;

  public PluginSetupEvent(String name, String iconUrl, List<PluginSetupActionData> actions) {
    this.name = name;
    this.iconUrl = iconUrl;
    this.actions = actions != null ? actions : List.of();
  }
}