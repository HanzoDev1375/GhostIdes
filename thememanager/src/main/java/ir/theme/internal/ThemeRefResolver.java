package ir.theme.internal;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import ir.theme.GhostTheme;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves relative theme references written as "@block.key" inside a {@code .gth} theme file.
 *
 * <p>Example: in the "widget" section the value of {@code "surface"} may be
 * {@code "@material3.surfaceContainer"} — this resolver follows the chain (also across multiple
 * hops) until it lands on a concrete color. Cycles, unknown blocks/keys and over-deep chains are
 * left untouched (the raw reference is returned) instead of throwing, so a hand-typed mistake is
 * visible in the editor (and flagged by the LSP) instead of silently breaking the theme.
 */
public final class ThemeRefResolver {

  public static final String[] BLOCKS = {"activity", "editor", "widget", "material3"};

  private static final int MAX_DEPTH = 16;

  private static final Map<String, Method> METHOD_CACHE = new ConcurrentHashMap<>();

  private ThemeRefResolver() {}

  /** Resolves every {@code @block.key} value inside the JSON tree, in place. */
  public static JsonObject resolveJson(JsonObject root) {
    if (root == null) {
      return root;
    }
    for (int b = 0; b < BLOCKS.length; b++) {
      JsonElement blockEl = root.get(BLOCKS[b]);
      if (blockEl == null || !blockEl.isJsonObject()) {
        continue;
      }
      JsonObject block = blockEl.getAsJsonObject();
      for (Map.Entry<String, JsonElement> entry : new ArrayList<>(block.entrySet())) {
        JsonElement value = entry.getValue();
        if (value != null
            && value.isJsonPrimitive()
            && value.getAsJsonPrimitive().isString()) {
          String raw = value.getAsString();
          if (raw != null && raw.startsWith("@")) {
            block.addProperty(entry.getKey(), resolveValue(root, raw));
          }
        }
      }
    }
    return root;
  }

  /**
   * Resolves a single raw value (e.g. {@code "@material3.surface"}). Non-reference values are
   * returned unchanged. Unresolvable references keep their raw {@code @...} form.
   */
  public static String resolveValue(JsonObject root, String raw) {
    if (root == null || raw == null || !raw.startsWith("@")) {
      return raw;
    }
    Set<String> visited = new HashSet<>();
    String current = raw;
    int depth = 0;
    while (current != null && current.startsWith("@") && depth < MAX_DEPTH) {
      if (!visited.add(current)) {
        break;
      }
      String[] parts = current.substring(1).split("\\.", 2);
      if (parts.length != 2) {
        return current;
      }
      JsonObject block = blockObj(root, parts[0]);
      if (block == null || !block.has(parts[1])) {
        return current;
      }
      JsonElement value = block.get(parts[1]);
      if (value == null
          || !value.isJsonPrimitive()
          || !value.getAsJsonPrimitive().isString()) {
        break;
      }
      current = value.getAsString();
      depth++;
    }
    return current;
  }

  private static JsonObject blockObj(JsonObject root, String block) {
    JsonElement el = root.get(block);
    return (el != null && el.isJsonObject()) ? el.getAsJsonObject() : null;
  }

  // ---- Typed resolution against a GhostTheme (for the visual editors) ------------------

  /** Resolves a single value stored on a {@code GhostTheme}; non-references pass through. */
  public static String resolve(GhostTheme theme, String value) {
    if (value == null || !value.startsWith("@")) {
      return value;
    }
    String[] parts = value.substring(1).split("\\.", 2);
    if (parts.length != 2) {
      return value;
    }
    return resolve(theme, parts[0], parts[1]);
  }

  /** Resolves "{@code block.key}" across the typed theme object, following reference chains. */
  public static String resolve(GhostTheme theme, String block, String key) {
    if (theme == null) {
      return null;
    }
    Set<String> visited = new HashSet<>();
    for (int depth = 0; depth < MAX_DEPTH; depth++) {
      String ref = block + "." + key;
      if (!visited.add(ref)) {
        return "@" + ref;
      }
      String value = read(theme, block, key);
      if (value == null) {
        return "@" + ref;
      }
      if (!value.startsWith("@")) {
        return value;
      }
      String[] parts = value.substring(1).split("\\.", 2);
      if (parts.length != 2) {
        return value;
      }
      block = parts[0];
      key = parts[1];
    }
    return read(theme, block, key);
  }

  private static String read(GhostTheme theme, String block, String key) {
    Object holder;
    switch (block) {
      case "activity":
        holder = theme.getActivity();
        break;
      case "editor":
        holder = theme.getEditor();
        break;
      case "widget":
        holder = theme.getWidget();
        break;
      case "material3":
        holder = theme.getMaterial3();
        break;
      default:
        return null;
    }
    if (holder == null || key == null || key.isEmpty()) {
      return null;
    }
    String getterName =
        "get" + Character.toUpperCase(key.charAt(0)) + key.substring(1);
    String cacheKey = holder.getClass().getName() + "#" + getterName;
    try {
      Method method = METHOD_CACHE.get(cacheKey);
      if (method == null) {
        method = holder.getClass().getMethod(getterName);
        METHOD_CACHE.put(cacheKey, method);
      }
      Object result = method.invoke(holder);
      return result instanceof String ? (String) result : null;
    } catch (Exception e) {
      return null;
    }
  }
}