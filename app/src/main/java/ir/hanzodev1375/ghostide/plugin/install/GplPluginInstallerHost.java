package ir.hanzodev1375.ghostide.plugin.install;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ir.hanzodev1375.components.store.data.PluginRepository;
import ir.hanzodev1375.components.store.event.PluginInstallEvent;
import ir.hanzodev1375.components.store.event.PluginInstalledListEvent;
import ir.hanzodev1375.components.store.event.PluginResultEvent;
import ir.hanzodev1375.components.store.event.PluginSetupEvent;
import ir.hanzodev1375.components.store.event.PluginSetupRequestEvent;
import ir.hanzodev1375.components.store.model.PluginItem;
import ir.hanzodev1375.components.store.model.PluginSetupActionData;
import ir.hanzodev1375.ghostide.plugin.api.PluginSetupAction;
import ir.hanzodev1375.ghostide.plugin.gpl.GplInstalledPlugins;
import ir.hanzodev1375.ghostide.plugin.gpl.GplManifest;
import ir.hanzodev1375.ghostide.plugin.gpl.GplManifestReader;
import ir.hanzodev1375.ghostide.plugin.gpl.GplPluginLoader;
import ir.hanzodev1375.ghostide.plugin.gpl.LoadedGplPlugin;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/** App-side listener that installs store plugins via the GPL loader. */
public class GplPluginInstallerHost {

  private static final String TAG = "GplPluginInstallerHost";
  private static final String GPL_EXTENSION = ".gpl";

  private final Context context;
  private final ExecutorService executor = Executors.newSingleThreadExecutor();
  private final PluginRepository repository = new PluginRepository();

  public GplPluginInstallerHost(Context context) {
    this.context = context.getApplicationContext();
  }

  public void register() {
    if (!EventBus.getDefault().isRegistered(this)) {
      EventBus.getDefault().register(this);
    }
    executor.execute(() -> EventBus.getDefault().post(new PluginInstalledListEvent(installedIds())));
  }

  public void unregister() {
    EventBus.getDefault().unregister(this);
    executor.shutdownNow();
  }

  @Subscribe(threadMode = ThreadMode.BACKGROUND)
  public void onSetupRequest(PluginSetupRequestEvent event) {
    PluginItem item = event.plugin;
    if (item == null) {
      return;
    }
    emitSetupIfAny(item);
  }

  @Subscribe(threadMode = ThreadMode.BACKGROUND)
  public void onInstall(PluginInstallEvent event) {
    PluginItem item = event.plugin;
    if (item == null || !item.hasGpl()) {
      postFailure(item, "no gpl url");
      return;
    }
    try {
      File downloaded = repository.downloadGplSync(context, item);
      GplManifest manifest = GplManifestReader.read(downloaded);
      if (manifest == null) {
        downloaded.delete();
        postFailure(item, "invalid gpl manifest");
        return;
      }
      File installed =
          new File(GplInstalledPlugins.installDir(context), manifest.id() + GPL_EXTENSION);
      if (!installed.getAbsolutePath().equals(downloaded.getAbsolutePath())) {
        if (installed.exists() && !installed.delete()) {
          downloaded.delete();
          postFailure(item, "could not replace existing plugin file");
          return;
        }
        if (!downloaded.renameTo(installed)) {
          downloaded.delete();
          postFailure(item, "could not move plugin file");
          return;
        }
      }
      GplPluginLoader.getInstance(context).load(installed);
      postSuccess(item, manifest.name());
      emitSetupFromFile(item, installed);
    } catch (Exception e) {
      Log.e(TAG, "install failed", e);
      postFailure(item, e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
    }
  }

  private void postSuccess(PluginItem item, String name) {
    EventBus.getDefault().post(new PluginResultEvent(item, true, name));
  }

  private void postFailure(PluginItem item, String message) {
    EventBus.getDefault().post(new PluginResultEvent(item, false, message));
  }

  private void emitSetupFromFile(PluginItem item, File file) {
    if (file == null) {
      return;
    }
    try {
      GplPluginLoader loader = GplPluginLoader.getInstance(context);
      LoadedGplPlugin loadedPlugin = loader.load(file);
      List<PluginSetupAction> setupActions = loadedPlugin.getPlugin().getSetupActions();
      List<PluginSetupActionData> data = new ArrayList<>();
      if (setupActions != null) {
        for (PluginSetupAction action : setupActions) {
          data.add(
              new PluginSetupActionData(
                  action.id(), action.label(), action.command(), action.description()));
        }
      }
      if (data.isEmpty()) {
        return;
      }
      EventBus.getDefault().post(new PluginSetupEvent(item.name(), item.icon(), data));
    } catch (Exception e) {
      Log.e(TAG, "setup emit failed", e);
    }
  }

  private void emitSetupIfAny(PluginItem item) {
    File file = findInstalledFile(item);
    if (file == null) {
      try {
        file = repository.downloadGplSync(context, item);
      } catch (IOException e) {
        Log.e(TAG, "setup fallback download failed", e);
        return;
      }
    }
    emitSetupFromFile(item, file);
  }

  private File findInstalledFile(PluginItem item) {
    File dir = GplInstalledPlugins.installDir(context);
    File[] files = dir.listFiles((d, name) -> name.endsWith(GPL_EXTENSION));
    if (files != null) {
      for (File file : files) {
        try {
          GplManifest manifest = GplManifestReader.read(file);
          String fileName = file.getName().substring(0, file.getName().length() - GPL_EXTENSION.length());
          if (manifest != null
              && (manifest.name().equals(item.name())
                  || manifest.id().equalsIgnoreCase(item.name())
                  || fileName.equalsIgnoreCase(item.name()))) {
            return file;
          }
        } catch (RuntimeException e) {
          Log.w(TAG, "skip unreadable manifest " + file, e);
        }
      }
    }
    return null;
  }

  private List<String> installedIds() {
    List<String> ids = new ArrayList<>();
    File dir = GplInstalledPlugins.installDir(context);
    File[] files = dir.listFiles((d, name) -> name.endsWith(GPL_EXTENSION));
    if (files != null) {
      for (File file : files) {
        try {
          GplManifest manifest = GplManifestReader.read(file);
          if (manifest != null) {
            ids.add(manifest.name());
          }
        } catch (RuntimeException e) {
          Log.w(TAG, "skip unreadable manifest " + file, e);
        }
      }
    }
    return ids;
  }
}