package ir.hanzodev1375.components.store.model;

import com.google.gson.annotations.SerializedName;

public record PluginDoc(
    @SerializedName("devname") String devname,
    @SerializedName("icon") String icon,
    @SerializedName("note") String note) {

  public PluginDoc {
    if (devname == null) devname = "";
    if (icon == null) icon = "";
    if (note == null) note = "";
  }
}