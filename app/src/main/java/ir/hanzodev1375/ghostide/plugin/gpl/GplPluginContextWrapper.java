package ir.hanzodev1375.ghostide.plugin.gpl;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.util.Log;

import java.io.File;
import java.lang.reflect.Method;

/**
 * Wraps the host context so a plugin's {@code getResources()}/{@code getAssets()} resolve
 * against its own {@code .gpl} package instead of the host's, using the same {@code
 * AssetManager.addAssetPath} reflection technique long relied on by Android plugin frameworks
 * such as RePlugin and VirtualAPK. {@code addAssetPath} is not a public API; if a future Android
 * version removes or blocks it, {@link #create} falls back to the host's own resources so a
 * plugin still loads, just without its custom layouts/drawables.
 */
final class GplPluginContextWrapper extends ContextWrapper {

  private static final String TAG = "GplPluginContext";

  private final ClassLoader classLoader;
  private final Resources resources;

  private GplPluginContextWrapper(Context base, ClassLoader classLoader, Resources resources) {
    super(base);
    this.classLoader = classLoader;
    this.resources = resources;
  }

  static GplPluginContextWrapper create(Context hostContext, File gplFile, ClassLoader classLoader) {
    Resources resources = tryLoadPluginResources(hostContext, gplFile);
    return new GplPluginContextWrapper(hostContext, classLoader, resources);
  }

  private static Resources tryLoadPluginResources(Context hostContext, File gplFile) {
    try {
      AssetManager assetManager = AssetManager.class.getDeclaredConstructor().newInstance();
      Method addAssetPath = AssetManager.class.getMethod("addAssetPath", String.class);
      Object result = addAssetPath.invoke(assetManager, gplFile.getAbsolutePath());
      if (result instanceof Integer cookie && cookie == 0) {
        throw new IllegalStateException("addAssetPath rejected " + gplFile);
      }
      Resources hostResources = hostContext.getResources();
      return new Resources(assetManager, hostResources.getDisplayMetrics(), hostResources.getConfiguration());
    } catch (ReflectiveOperationException | RuntimeException e) {
      Log.w(TAG, "Falling back to host resources for " + gplFile + ": " + e.getMessage());
      return hostContext.getResources();
    }
  }

  @Override
  public ClassLoader getClassLoader() {
    return classLoader;
  }

  @Override
  public Resources getResources() {
    return resources;
  }

  @Override
  public AssetManager getAssets() {
    return resources.getAssets();
  }
}
