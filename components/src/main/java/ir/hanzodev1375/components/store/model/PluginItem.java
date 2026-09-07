package ir.hanzodev1375.components.store.model;

import com.google.gson.annotations.SerializedName;

public record PluginItem(
    @SerializedName("name") String name,
    @SerializedName("icon") String icon,
    @SerializedName("doc") String doc,
    @SerializedName("gplfile") String gplfile,
    @SerializedName("source") String source) {

  public PluginItem {
    if (name == null) name = "";
    if (icon == null) icon = "";
    if (doc == null) doc = "";
    if (gplfile == null) gplfile = "";
    if (source == null) source = "";
  }

  public boolean hasGpl() {
    return gplfile != null && !gplfile.trim().isEmpty();
  }

  public boolean hasSource() {
    return source != null && !source.trim().isEmpty();
  }
}