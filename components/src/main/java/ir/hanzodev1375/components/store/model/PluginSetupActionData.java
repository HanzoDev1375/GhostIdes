package ir.hanzodev1375.components.store.model;

public record PluginSetupActionData(String id, String label, String command, String description) {

  public PluginSetupActionData {
    if (id == null) id = "";
    if (label == null) label = "";
    if (command == null) command = "";
    if (description == null) description = "";
  }
}