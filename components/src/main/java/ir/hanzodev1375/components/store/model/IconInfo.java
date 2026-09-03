package ir.hanzodev1375.components.store.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class IconInfo {

  @SerializedName("name")
  public String name;

  @SerializedName("version")
  public int version;

  @SerializedName("popularity")
  public long popularity;

  @SerializedName("unsupported_families")
  public List<String> unsupportedFamilies;

  @SerializedName("categories")
  public List<String> categories;

  @SerializedName("tags")
  public List<String> tags;

  public boolean supports(String familyName) {
    return unsupportedFamilies == null || !unsupportedFamilies.contains(familyName);
  }
}
