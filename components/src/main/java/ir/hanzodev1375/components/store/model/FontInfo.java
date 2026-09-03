package ir.hanzodev1375.components.store.model;

import com.google.gson.annotations.SerializedName;
import java.util.HashMap;
import java.util.Map;

public class FontInfo {

  @SerializedName("family")
  public String family;

  @SerializedName("category")
  public String category;

  public Map<String, String> files;

  public void extractFiles(com.google.gson.JsonObject fontsObj) {
    if (fontsObj == null) return;
    files = new HashMap<>();
    for (String key : fontsObj.keySet()) {
      com.google.gson.JsonObject fontVariant = fontsObj.getAsJsonObject(key);
      if (fontVariant.has("url")) {
        String url = fontVariant.get("url").getAsString();
        files.put(key, url);
      }
    }
  }
}
