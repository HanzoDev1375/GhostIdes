package ir.hanzodev1375.ghostide.ide.ui.api;

import android.content.Context;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONObject;

/**
 * A ready-made {@link FileIconContributor} that loads icon mappings from a JSON file shipped in a
 * plugin's own assets, using exactly the same schema as the host's {@code data/file_icons.json}:
 *
 * <pre>{@code
 * {
 *   "asset_dir": "myicons",
 *   "extensions": { ".ghost": "file_type_ghost" },
 *   "filenames": { "makefile": "file_type_makefile" },
 *   "folders": { "components": "folder_type_components" },
 *   "defaults": { "file": "default_file", "folder": "default_folder" }
 * }
 * }</pre>
 *
 * <p>Icon names may reference either your own artwork placed under {@code asset_dir} in the plugin
 * assets, or any icon of the built-in {@code vscode_icons} set. Custom artwork is extracted once
 * into the plugin's private {@code filesDir/ghost_icons/} folder at construction time and served
 * back as {@code file://} URIs (Glide can only open host assets through {@code android_asset});
 * unknown names fall back to the built-in set untouched.
 *
 * <p>Usage inside {@code activate()}:
 *
 * <pre>{@code
 * Context pluginContext = context.getServices().require(IdeHostServices.PLUGIN_ANDROID_CONTEXT);
 * context.registerDisposable(context.getExtensions().register(
 *     PluginUiExtensionPoints.FILE_ICON_CONTRIBUTOR,
 *     new JsonFileIconContributor(pluginContext, "myicons.json")));
 * }</pre>
 */
public final class JsonFileIconContributor implements FileIconContributor {

  private static final String EXTRACT_DIR = "ghost_icons";
  static final String TAG = "JsonFileIconContributor";
  private final JSONObject extensions;
  private final JSONObject filenames;
  private final JSONObject folders;
  private final String defaultFile;
  private final String defaultFolder;
  private final List<String> extKeysSorted;
  private final Map<String, String> resolved = new HashMap<>();

  /**
   * @param pluginContext scoped plugin context from {@code IdeHostServices.PLUGIN_ANDROID_CONTEXT}
   * @param jsonAssetPath path of the mapping JSON inside the plugin assets, e.g. {@code
   *     "myicons.json"}
   */
  public JsonFileIconContributor(Context pluginContext, String jsonAssetPath) {
    JSONObject root = readJson(pluginContext, jsonAssetPath);
    String assetDir = root == null ? "" : root.optString("asset_dir", "");
    JSONObject def = root == null ? null : root.optJSONObject("defaults");
    defaultFile = def == null ? "" : def.optString("file", "");
    defaultFolder = def == null ? "" : def.optString("folder", "");

    extensions = root == null ? null : root.optJSONObject("extensions");
    filenames = root == null ? null : root.optJSONObject("filenames");
    folders = root == null ? null : root.optJSONObject("folders");

    List<String> keys = new ArrayList<>();
    if (extensions != null) {
      for (Iterator<String> it = extensions.keys(); it.hasNext(); ) keys.add(it.next());
      Collections.sort(keys, (a, b) -> b.length() - a.length());
    }
    extKeysSorted = keys;

    Set<String> custom = new HashSet<>();
    markCustom(extensions, custom);
    markCustom(filenames, custom);
    markCustom(folders, custom);

    File outDir = new File(new File(pluginContext.getFilesDir(), EXTRACT_DIR), safe(jsonAssetPath));
    extractArtwork(pluginContext, assetDir, bundledNames(pluginContext, assetDir, custom), outDir);
  }

  @Override
  public String getIcon(String filePath) {
    File file = new File(filePath);
    if (file.isDirectory()) {
      if (folders != null) {
        String hit = resolve(folders, file.getName().toLowerCase());
        if (hit != null) return hit;
      }
      return pick(defaultFolder);
    }
    String lower = file.getName().toLowerCase();
    if (!lower.isEmpty()) {
      if (filenames != null && filenames.has(lower)) return resolve(filenames, lower);
      for (String key : extKeysSorted) {
        int cut = lower.length() - key.length();
        if (cut > 0 && lower.charAt(cut - 1) == '.' && lower.endsWith(key)) {
          return resolve(extensions, key);
        }
      }
    }
    return pick(defaultFile);
  }

  /** Returns the URI for custom artwork, or the bare name for built-in icons. */
  private String resolve(JSONObject section, String key) {
    String name = section.optString(key, "");
    if (name.isEmpty()) return null;
    String uri = resolved.get(name);
    return uri != null ? uri : name;
  }

  private String pick(String name) {
    if (name == null || name.isEmpty()) return null;
    String uri = resolved.get(name);
    return uri != null ? uri : name;
  }

  private static JSONObject readJson(Context context, String path) {
    try (InputStream is = context.getAssets().open(path)) {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      byte[] buf = new byte[8192];
      int n;
      while ((n = is.read(buf)) > 0) out.write(buf, 0, n);
      return new JSONObject(out.toString("UTF-8"));
    } catch (Exception e) {
      return null;
    }
  }

  private static void markCustom(JSONObject section, Set<String> into) {
    if (section == null) return;
    for (Iterator<String> it = section.keys(); it.hasNext(); ) {
      String name = section.optString(it.next(), "");
      if (!name.isEmpty()) into.add(name);
    }
  }

  /** Keeps only the names that actually exist as {@code <name>.svg} in the plugin assets. */
  private static Set<String> bundledNames(Context context, String assetDir, Set<String> names) {
    Set<String> bundled = new HashSet<>();
    if (assetDir.isEmpty()) return bundled;
    try {
      for (String file : context.getAssets().list(assetDir)) {
        String name = file.endsWith(".svg") ? file.substring(0, file.length() - 4) : null;
        if (name != null && names.contains(name)) bundled.add(name);
      }
    } catch (Exception ignored) {
      Log.e(TAG, ignored.getLocalizedMessage());
    }
    return bundled;
  }

  private void extractArtwork(Context context, String assetDir, Set<String> names, File outDir) {
    if (names.isEmpty() || assetDir.isEmpty()) return;
    for (String name : names) {
      File target = new File(outDir, safe(name) + ".svg");
      try (InputStream is = context.getAssets().open(assetDir + "/" + name + ".svg")) {
        outDir.mkdirs();
        try (FileOutputStream fos = new FileOutputStream(target)) {
          byte[] buf = new byte[8192];
          int n;
          while ((n = is.read(buf)) > 0) fos.write(buf, 0, n);
        }
        resolved.put(name, "file://" + target.getAbsolutePath());
      } catch (Exception ignored) {
        Log.e(TAG, ignored.getMessage());
      }
    }
  }

  private static String safe(String raw) {
    return raw.replaceAll("[^a-zA-Z0-9._-]", "_");
  }
}
