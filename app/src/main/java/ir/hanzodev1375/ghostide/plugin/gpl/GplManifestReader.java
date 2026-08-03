package ir.hanzodev1375.ghostide.plugin.gpl;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Reads {@code assets/plugin.json} directly out of a {@code .gpl} file's zip entries. */
public final class GplManifestReader {

  private static final String MANIFEST_ENTRY = "assets/plugin.json";

  private GplManifestReader() {}

  public static GplManifest read(File gplFile) throws IOException {
    try (ZipFile zip = new ZipFile(gplFile)) {
      ZipEntry entry = zip.getEntry(MANIFEST_ENTRY);
      if (entry == null) {
        throw new IOException("gpl package is missing " + MANIFEST_ENTRY + ": " + gplFile);
      }
      String json;
      try (InputStream stream = zip.getInputStream(entry)) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int read;
        while ((read = stream.read(chunk)) != -1) {
          buffer.write(chunk, 0, read);
        }
        json = buffer.toString(StandardCharsets.UTF_8.name());
      }
      JSONObject object = new JSONObject(json);
      return new GplManifest(
          object.getString("id"),
          object.getString("name"),
          object.getString("version"),
          object.getString("entryClass"),
          object.optString("description", ""),
          object.optInt("minHostVersion", 0));
    } catch (org.json.JSONException e) {
      throw new IOException("Malformed " + MANIFEST_ENTRY + " in " + gplFile, e);
    }
  }
}
