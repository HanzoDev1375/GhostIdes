package ir.hanzodev1375.ghostide.plugin.gpl;

import android.util.Log;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipFile;

/** Reads {@code assets/plugin.json} directly out of a {@code .gpl} file's zip entries. */
public final class GplManifestReader {

  private static final String TAG = "GplManifestReader";
  private static final String MANIFEST_ENTRY = "assets/plugin.json";

  private GplManifestReader() {}

  public static GplManifest read(File gplFile) {
    try {
      try (var zip = new ZipFile(gplFile)) {
        var entry = zip.getEntry(MANIFEST_ENTRY);
        if (entry == null) {
          throw new IOException("gpl package is missing " + MANIFEST_ENTRY + ": " + gplFile);
        }
        String json;
        try (var stream = zip.getInputStream(entry)) {
          var buffer = new ByteArrayOutputStream();
          byte[] chunk = new byte[8192];
          int read;
          while ((read = stream.read(chunk)) != -1) {
            buffer.write(chunk, 0, read);
          }
          json = buffer.toString(StandardCharsets.UTF_8.name());
        }
        var object = new JSONObject(json);
        return new GplManifest(
            object.getString("id"),
            object.getString("name"),
            object.getString("version"),
            object.getString("entryClass"),
            object.optString("description", ""),
            object.optInt("minHostVersion", 0),
            object.has("icon") ? object.optString("icon", null) : null);
      } catch (org.json.JSONException e) {
        throw new IOException("Malformed " + MANIFEST_ENTRY + " in " + gplFile, e);
      }
    } catch (Exception err) {
      Log.e(TAG, "Failed to read manifest from " + gplFile, err);
     // throw new IOException("Failed to read manifest from " + gplFile, err);
    }
    return null;
  }

  /**
   * Raw bytes of {@code manifest.icon()} from inside {@code gplFile}, or {@code null} if the plugin
   * declared no icon or the entry is missing.
   */
  public static byte[] readIconBytes(File gplFile, GplManifest manifest) {
    if (manifest.icon() == null) {
      return null;
    }
    try (var zip = new ZipFile(gplFile)) {
      var entry = zip.getEntry("assets/" + manifest.icon());
      if (entry == null) {
        return null;
      }
      try (var stream = zip.getInputStream(entry)) {
        var buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int read;
        while ((read = stream.read(chunk)) != -1) {
          buffer.write(chunk, 0, read);
        }
        return buffer.toByteArray();
      }
    } catch (IOException e) {
      return null;
    }
  }
}
