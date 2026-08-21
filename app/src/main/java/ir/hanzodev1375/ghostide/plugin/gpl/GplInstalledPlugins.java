package ir.hanzodev1375.ghostide.plugin.gpl;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Where installed {@code .gpl} files live, and the scan that (re)activates all of them. */
public final class GplInstalledPlugins {

  private static final String TAG = "GplInstalledPlugins";
  private static final String INSTALL_DIR_NAME = "gpl_plugins";
  private static final String GPL_EXTENSION = ".gpl";

  private GplInstalledPlugins() {}

  public static File installDir(Context context) {
    File dir = new File(context.getApplicationContext().getFilesDir(), INSTALL_DIR_NAME);
    if (!dir.exists()) {
      dir.mkdirs();
    }
    return dir;
  }

  public static List<File> listInstalled(Context context) {
    File[] files = installDir(context).listFiles((dir, name) -> name.endsWith(GPL_EXTENSION));
    if (files == null) {
      return Collections.emptyList();
    }
    return Arrays.asList(files);
  }

  /** Loads every installed plugin not already active. A broken plugin is skipped, not fatal. */
  public static void loadAll(Context context, GplPluginLoader loader) {
    for (File file : listInstalled(context)) {
      try {
        GplManifest manifest = GplManifestReader.read(file);
        if (manifest == null) {
          Log.w(TAG, "Skipping plugin with unreadable manifest: " + file);
          continue;
        }
        if (!loader.isLoaded(manifest.id())) {
          loader.load(file);
        }
      } catch (IOException | ReflectiveOperationException | RuntimeException e) {
        Log.w(TAG, "Skipping broken plugin " + file + ": " + e.getMessage());
      }
    }
  }
}
