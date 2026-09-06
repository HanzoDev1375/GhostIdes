package ir.hanzodev1375.ghostide.codeeditors.langs.lsp;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class GhostThemeLspAssets {

  private static final String TAG = "GhostThemeLspAssets";
  private static final String ASSET_BASE = "node_modules/ghost-theme-lsp";
  private static final String REL_BUNDLE = "dist/ghost-theme-lsp.js";
  private static final String VERSION_MARKER = ".version";

  private GhostThemeLspAssets() {}

  public static File bundleFile(Context context) {
    return new File(context.getFilesDir(), ASSET_BASE + "/" + REL_BUNDLE);
  }

  public static void install(Context context) {
    File hostDir = new File(context.getFilesDir(), ASSET_BASE);
    String assetVersion = readAssetVersion(context);
    File marker = new File(hostDir, VERSION_MARKER);
    if (bundleFile(context).exists() && marker.exists() && lineEquals(marker, assetVersion)) {
      return;
    }
    try {
      deleteRecursive(hostDir);
      copyAssetTree(context, ASSET_BASE, hostDir);
      writeLine(marker, assetVersion == null ? "unknown" : assetVersion);
      Log.i(
          TAG, "ghost-theme-lsp installed (v" + (assetVersion != null ? assetVersion : "?") + ")");
    } catch (IOException e) {
      Log.e(TAG, "failed to install ghost-theme-lsp assets", e);
    }
  }

  /**
   * Ensures the bundle is present and returns it, or {@code null} when it cannot be produced. Only
   * copies when the bundle is missing, so it never re-extracts an already-valid copy.
   */
  public static File ensureInstalled(Context context) {
    File bundle = bundleFile(context);
    if (!bundle.exists()) {
      File hostDir = new File(context.getFilesDir(), ASSET_BASE);
      try {
        deleteRecursive(hostDir);
        copyAssetTree(context, ASSET_BASE, hostDir);
      } catch (IOException e) {
        Log.e(TAG, "failed to copy ghost-theme-lsp assets on demand", e);
      }
    }
    return bundle.exists() ? bundle : null;
  }

  /** Maps a host file under the (proot-bound) app files dir to its guest path. */
  public static String guestPathOf(Context context, File hostFile) {
    return hostFile.getAbsolutePath();
  }

  private static String readAssetVersion(Context context) {
    try (InputStream in = context.getAssets().open(ASSET_BASE + "/package.json");
        BufferedReader reader =
            new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
      StringBuilder sb = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) sb.append(line).append('\n');
      return new JSONObject(sb.toString()).optString("version", null);
    } catch (Exception e) {
      return null;
    }
  }

  private static boolean lineEquals(File file, String value) {
    if (value == null) return false;
    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
      return value.equals(reader.readLine());
    } catch (IOException e) {
      return false;
    }
  }

  private static void writeLine(File file, String value) throws IOException {
    File parent = file.getParentFile();
    if (parent != null && !parent.exists() && !parent.mkdirs()) {
      throw new IOException("cannot create dir: " + parent);
    }
    try (FileOutputStream out = new FileOutputStream(file)) {
      out.write(value.getBytes(StandardCharsets.UTF_8));
    }
  }

  private static void copyAssetTree(Context context, String assetPath, File target)
      throws IOException {
    String[] children = context.getAssets().list(assetPath);
    if (children == null || children.length == 0) {
      File parent = target.getParentFile();
      if (parent != null && !parent.exists() && !parent.mkdirs()) {
        throw new IOException("cannot create dir: " + parent);
      }
      try (InputStream in = context.getAssets().open(assetPath);
          FileOutputStream out = new FileOutputStream(target)) {
        byte[] buffer = new byte[16384];
        int read;
        while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
      }
      return;
    }
    if (!target.exists() && !target.mkdirs()) {
      throw new IOException("cannot create dir: " + target);
    }
    for (String child : children) {
      copyAssetTree(context, assetPath + "/" + child, new File(target, child));
    }
  }

  private static void deleteRecursive(File file) {
    File[] children = file.listFiles();
    if (children != null) {
      for (File child : children) deleteRecursive(child);
    }
    file.delete();
  }
}
