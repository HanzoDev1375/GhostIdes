package ir.hanzodev1375.ghostide.plugin.gpl;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import ir.hanzodev1375.ghostide.plugin.api.MutableServiceRegistry;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import dalvik.system.DexClassLoader;
import dalvik.system.InMemoryDexClassLoader;

import ir.hanzodev1375.ghostide.ide.ui.api.IdeHostServices;
import ir.hanzodev1375.ghostide.plugin.api.CoreServices;
import ir.hanzodev1375.ghostide.plugin.api.GhostPlugin;
import ir.hanzodev1375.ghostide.plugin.api.GlobalRegistry;
import ir.hanzodev1375.ghostide.plugin.api.LayeredServiceRegistry;
import ir.hanzodev1375.ghostide.plugin.api.PluginDescriptor;

public final class GplPluginLoader {

  private static final String TAG = "GplPluginLoader";
  private static volatile GplPluginLoader instance;

  private final Context appContext;
  private final Map<String, LoadedGplPlugin> loaded = new HashMap<>();

  public GplPluginLoader(Context appContext) {
    this.appContext = appContext.getApplicationContext();
  }

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

  public synchronized LoadedGplPlugin load(File gplFile)
      throws IOException, ReflectiveOperationException {
    GplManifest manifest = GplManifestReader.read(gplFile);
    if (manifest == null) {
      throw new IOException("Could not read manifest from " + gplFile);
    }
    LoadedGplPlugin existing = loaded.get(manifest.id());
    if (existing != null) {
      return existing;
    }

    ClassLoader classLoader;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      ByteBuffer[] dexBuffers = readDexBuffers(gplFile);
      Log.d(
          TAG,
          "Loading " + manifest.id() + " from " + dexBuffers.length + " in-memory dex buffer(s)");
      classLoader =
          new InMemoryDexClassLoader(
              dexBuffers,
              appContext.getApplicationInfo().nativeLibraryDir,
              appContext.getClassLoader());
    } else {
      File optDir = new File(appContext.getCodeCacheDir(), "gpl/" + manifest.id());
      if (!optDir.exists() && !optDir.mkdirs()) {
        throw new IOException("Could not create dex cache dir " + optDir);
      }
      String dexPath = extractDexFiles(gplFile, optDir);
      Log.d(TAG, "Loading " + manifest.id() + " from dex files: " + dexPath);
      classLoader =
          new DexClassLoader(
              dexPath,
              optDir.getAbsolutePath(),
              appContext.getApplicationInfo().nativeLibraryDir,
              appContext.getClassLoader());
    }

    Context pluginAndroidContext = GplPluginContextWrapper.create(appContext, gplFile, classLoader);

    Class<?> entryClass = classLoader.loadClass(manifest.entryClass());
    Object instance = entryClass.getDeclaredConstructor().newInstance();
    if (!(instance instanceof GhostPlugin plugin)) {
      throw new IllegalStateException(
          manifest.entryClass() + " does not implement " + GhostPlugin.class.getName());
    }

    PluginDescriptor descriptor =
        PluginDescriptor.builder(
                manifest.id(), manifest.name(), manifest.version(), manifest.entryClass(),manifest.icon())
            .description(manifest.description())
            .source(gplFile.getAbsolutePath())
            .build();

    MutableServiceRegistry pluginServices = new LayeredServiceRegistry(GlobalRegistry.services());
    pluginServices.register(IdeHostServices.PLUGIN_ANDROID_CONTEXT, pluginAndroidContext);
    pluginServices.register(
        CoreServices.PLUGIN_STORAGE, new PluginStorageImpl(appContext, manifest.id()));

    DefaultPluginContext pluginContext =
        new DefaultPluginContext(
            descriptor,
            GlobalRegistry.extensions(),
            pluginServices,
            new AndroidPluginLogger(manifest.id()));

    plugin.activate(pluginContext);

    LoadedGplPlugin loadedPlugin = new LoadedGplPlugin(descriptor, plugin, pluginContext);
    loaded.put(manifest.id(), loadedPlugin);
    return loadedPlugin;
  }

  private static ByteBuffer[] readDexBuffers(File gplFile) throws IOException {
    List<ByteBuffer> buffers = new ArrayList<>();
    try (ZipFile zip = new ZipFile(gplFile)) {
      Enumeration<? extends ZipEntry> entries = zip.entries();
      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        String name = entry.getName();
        if (!name.matches("classes\\d*\\.dex")) {
          continue;
        }
        try (InputStream input = zip.getInputStream(entry)) {
          byte[] data = new byte[(int) entry.getSize()];
          int offset = 0;
          int read;
          while (offset < data.length
              && (read = input.read(data, offset, data.length - offset)) != -1) {
            offset += read;
          }
          buffers.add(ByteBuffer.wrap(data));
        }
      }
    }
    if (buffers.isEmpty()) {
      throw new IOException("No classes*.dex entries found in " + gplFile);
    }
    return buffers.toArray(new ByteBuffer[0]);
  }

  private static String extractDexFiles(File gplFile, File optDir) throws IOException {
    StringBuilder dexPath = new StringBuilder();
    try (ZipFile zip = new ZipFile(gplFile)) {
      Enumeration<? extends ZipEntry> entries = zip.entries();
      int count = 0;
      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        String name = entry.getName();
        if (!name.matches("classes\\d*\\.dex")) {
          continue;
        }
        File extracted = new File(optDir, name);
        if (extracted.exists()) {
          extracted.delete();
        }
        try (InputStream input = zip.getInputStream(entry);
            OutputStream output = new FileOutputStream(extracted)) {
          byte[] buffer = new byte[8192];
          int read;
          while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
          }
        }
        if (count > 0) {
          dexPath.append(File.pathSeparator);
        }
        dexPath.append(extracted.getAbsolutePath());
        count++;
      }
    }
    if (dexPath.length() == 0) {
      throw new IOException("No classes*.dex entries found in " + gplFile);
    }
    return dexPath.toString();
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

  public synchronized List<PluginDescriptor> getLoadedDescriptors() {
    return loaded.values().stream().map(LoadedGplPlugin::getDescriptor).collect(java.util.stream.Collectors.toList());
  }

  public synchronized LoadedGplPlugin getLoaded(String pluginId) {
    return loaded.get(pluginId);
  }
}
