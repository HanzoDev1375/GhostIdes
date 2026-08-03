package ir.hanzodev1375.components.colors.model;

import com.google.gson.annotations.SerializedName;

public class ColorItem {
  @SerializedName("name")
  private String name;

  @SerializedName("hex")
  private String hex;

  public String getName() {
    return name;
  }

  public String getHex() {
    return hex;
  }
}
