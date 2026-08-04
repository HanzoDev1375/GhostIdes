package ir.hanzodev1375.ghostide.plugin.gpl;

import android.content.Context;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import dalvik.system.DexClassLoader;

import ir.hanzodev1375.ghostide.ide.ui.api.IdeHostServices;
import ir.hanzodev1375.ghostide.plugin.api.GhostPlugin;
import ir.hanzodev1375.ghostide.plugin.api.GlobalRegistry;
import ir.hanzodev1375.ghostide.plugin.api.MutableServiceRegistry;
import ir.hanzodev1375.ghostide.plugin.api.PluginDescriptor;

/**
 * Loads a {@code .gpl} package: reads {@code assets/plugin.json}, opens a {@link
 * DexClassLoader} on the package itself, instantiates the declared entry class through {@link
 * GhostPlugin}, and calls {@link GhostPlugin#activate}. One instance tracks every plugin it has
 * loaded so the same id is never activated twice and so {@link #unload} can reverse everything.
 */
public final class GplPluginLoader {

  private static volatile GplPluginLoader instance;

  private final Context appContext;
  private final Map<String, LoadedGplPlugin> loaded = new HashMap<>();

  public GplPluginLoader(Context appContext) {
    this.appContext = appContext.getApplicationContext();
  }

  /**
   * Shared instance for the whole process. The app-startup scan and the Plugin Manager screen
   * must use this rather than their own instance, or each would think the other's plugins were
   * never loaded and try to activate them a second time.
   */
  public static GplPluginLoader getInstance(Context context) {
    if (instance == null) {
      synchronized (GplPluginLoader.class) {
        if (instance == null) {
          instance = new GplPluginLoader(context);
        }
      }
    }
    return instance;
  }

  public synchronized LoadedGplPlugin load(File gplFile) throws IOException, ReflectiveOperationException {
    GplManifest manifest = GplManifestReader.read(gplFile);
    LoadedGplPlugin existing = loaded.get(manifest.id());
    if (existing != null) {
      return existing;
    }

    File optDir = new File(appContext.getCodeCacheDir(), "gpl/" + manifest.id());
    if (!optDir.exists() && !optDir.mkdirs()) {
      throw new IOException("Could not create dex cache dir " + optDir);
    }
    DexClassLoader classLoader =
        new DexClassLoader(
            gplFile.getAbsolutePath(),
            optDir.getAbsolutePath(),
            appContext.getApplicationInfo().nativeLibraryDir,
            appContext.getClassLoader());

    Context pluginAndroidContext = GplPluginContextWrapper.create(appContext, gplFile, classLoader);

    Class<?> entryClass = classLoader.loadClass(manifest.entryClass());
    Object instance = entryClass.getDeclaredConstructor().newInstance();
    if (!(instance instanceof GhostPlugin plugin)) {
      throw new IllegalStateException(
          manifest.entryClass() + " does not implement " + GhostPlugin.class.getName());
    }

    PluginDescriptor descriptor =
        PluginDescriptor.builder(manifest.id(), manifest.name(), manifest.version(), manifest.entryClass())
            .description(manifest.description())
            .source(gplFile.getAbsolutePath())
            .build();

    MutableServiceRegistry pluginServices = GlobalRegistry.services().copy();
    pluginServices.register(IdeHostServices.PLUGIN_ANDROID_CONTEXT, pluginAndroidContext);

    DefaultPluginContext pluginContext =
        new DefaultPluginContext(
            descriptor, GlobalRegistry.extensions(), pluginServices, new AndroidPluginLogger(manifest.id()));

    plugin.activate(pluginContext);

    LoadedGplPlugin loadedPlugin = new LoadedGplPlugin(descriptor, plugin, pluginContext);
    loaded.put(manifest.id(), loadedPlugin);
    return loadedPlugin;
  }

  public synchronized void unload(String pluginId) {
    LoadedGplPlugin plugin = loaded.remove(pluginId);
    if (plugin == null) {
      return;
    }
    plugin.unload();
    GlobalRegistry.extensions().unregisterOwner(pluginId);
  }

  public synchronized boolean isLoaded(String pluginId) {
    return loaded.containsKey(pluginId);
  }
}
