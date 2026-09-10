package ir.theme;

import android.graphics.Color;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds transient {@link GhostTheme} instances during a theme transition. Each frame the
 * transition blends the old and new palettes and feeds the result to {@link M3Theme#setPreviewTheme}
 * so every color getter reads the interpolated values instead of the persisted theme.
 */
public final class ThemePreviewBuilder {

  private static final String[] SECTIONS = {"material3", "widget", "activity", "editor"};

  private final GhostTheme base;
  private final JsonObject baseTree;
  private final Gson gson = new Gson();

  public ThemePreviewBuilder(GhostTheme base) {
    this.base = base != null ? base : new GhostTheme();
    this.baseTree = gson.toJsonTree(this.base).getAsJsonObject();
  }

  /** Extracts every usable color key (e.g. {@code material3.surfaceContainer}) from a theme. */
  public static Map<String, Integer> palette(GhostTheme theme) {
    Map<String, Integer> result = new LinkedHashMap<>();
    if (theme == null) {
      return result;
    }
    try {
      JsonObject root = new Gson().toJsonTree(theme).getAsJsonObject();
      for (String section : SECTIONS) {
        JsonObject obj = root.getAsJsonObject(section);
        if (obj == null) {
          continue;
        }
        for (String key : obj.keySet()) {
          String value = toText(obj.get(key));
          Integer color = parse(value);
          if (color != null) {
            result.put(section + "." + key, color);
          }
        }
      }
    } catch (Exception ignored) {
    }
    return result;
  }

  /** Blends two palettes into one at the given progress (0.0 old, 1.0 new). */
  public static Map<String, Integer> blendPalettes(
      Map<String, Integer> oldColors, Map<String, Integer> newColors, float progress) {
    Map<String, Integer> out = new HashMap<>();
    for (Map.Entry<String, Integer> e : newColors.entrySet()) {
      Integer old = oldColors.get(e.getKey());
      Integer blended =
          old != null
              ? androidx.core.graphics.ColorUtils.blendARGB(old, e.getValue(), progress)
              : e.getValue();
      out.put(e.getKey(), blended);
    }
    for (String key : oldColors.keySet()) {
      if (!newColors.containsKey(key)) {
        out.put(key, oldColors.get(key));
      }
    }
    return out;
  }

  /** Produces a full theme whose color fields are replaced by the given palette. */
  public GhostTheme build(Map<String, Integer> palette) {
    JsonObject merged = deepCopy(baseTree);
    JsonObject overrides = new JsonObject();
    for (Map.Entry<String, Integer> e : palette.entrySet()) {
      int dot = e.getKey().indexOf('.');
      if (dot <= 0 || dot == e.getKey().length() - 1) {
        continue;
      }
      String section = e.getKey().substring(0, dot);
      String field = e.getKey().substring(dot + 1);
      JsonObject obj = overrides.getAsJsonObject(section);
      if (obj == null) {
        obj = new JsonObject();
        overrides.add(section, obj);
      }
      obj.add(field, new JsonPrimitive(String.format("#%08X", e.getValue())));
    }
    deepMerge(merged, overrides);
    try {
      return gson.fromJson(merged, GhostTheme.class);
    } catch (Exception ignored) {
      return base;
    }
  }

  private static String toText(Object value) {
    return value instanceof JsonPrimitive && ((JsonPrimitive) value).isString()
        ? ((JsonPrimitive) value).getAsString()
        : null;
  }

  private static Integer parse(String hex) {
    if (hex == null || hex.isEmpty()) {
      return null;
    }
    try {
      return Color.parseColor(hex);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private static JsonObject deepCopy(JsonObject src) {
    JsonObject copy = new JsonObject();
    for (Map.Entry<String, com.google.gson.JsonElement> e : src.entrySet()) {
      copy.add(e.getKey(), e.getValue().deepCopy());
    }
    return copy;
  }

  private static void deepMerge(JsonObject target, JsonObject patch) {
    for (String key : patch.keySet()) {
      com.google.gson.JsonElement patchValue = patch.get(key);
      if (patchValue.isJsonObject()) {
        JsonObject targetObj = target.getAsJsonObject(key);
        if (targetObj == null) {
          targetObj = new JsonObject();
          target.add(key, targetObj);
        }
        deepMerge(targetObj, patchValue.getAsJsonObject());
      } else {
        target.add(key, patchValue.deepCopy());
      }
    }
  }
}