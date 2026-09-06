package ir.hanzodev1375.ghostide;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Process;
import android.util.Log;
import com.downloader.PRDownloader;
import ir.hanzodev1375.components.views.GhostToast;
import ir.hanzodev1375.ghostide.adapters.UiFeedbackHostImpl;
import ir.hanzodev1375.ghostide.activity.ErrorManagerActivity;
import ir.hanzodev1375.ghostide.codeeditors.langs.lsp.GhostThemeLspAssets;
import ir.hanzodev1375.ghostide.codeeditors.langs.lsp.ProotProcessLauncherImpl;
import ir.hanzodev1375.ghostide.codeeditors.setting.PreferencesUtils;
import ir.hanzodev1375.ghostide.ide.ui.api.FileIconContributor;
import ir.hanzodev1375.ghostide.ide.ui.api.IdeHostServices;
import ir.hanzodev1375.ghostide.ide.ui.api.PluginUiExtensionPoints;
import ir.hanzodev1375.ghostide.materialfileicon.core.JsonFileIconHelper;
import ir.hanzodev1375.ghostide.plugin.api.GlobalRegistry;
import ir.hanzodev1375.ghostide.plugin.gpl.GplInstalledPlugins;
import ir.hanzodev1375.ghostide.plugin.gpl.GplPluginLoader;
import ir.hanzodev1375.ghostide.shizuku.ShizukuManager;
import ir.hanzodev1375.ghostide.themeengine.ThemeEngine;
import ir.theme.M3Theme;
import ir.theme.ThemeManager;
import ir.theme.ThemeUtils;

public class GhostIdeAppLoader extends Application {

  private static Context mApplicationContext;
  private static GhostIdeAppLoader loader;
  private final StringBuilder softwareInfo = new StringBuilder();
  private PreferencesUtils setting;
  private ThemeUtils theme;

  public static Context getContext() {
    return mApplicationContext;
  }

  public static GhostIdeAppLoader getInstance() {
    return loader;
  }

  @Override
  public void onCreate() {
    super.onCreate();
    loader = this;
    setting = new PreferencesUtils(this);
    mApplicationContext = getApplicationContext();
    ShizukuManager.registerListeners();
    ThemeEngine.applyToActivities(this);
    M3Theme.init(this);
    var themeManager = new ThemeManager(this);
    theme = new ThemeUtils(themeManager);
    GhostToast.bindOfApp(this);
    PRDownloader.initialize(getApplicationContext());
    GhostThemeLspAssets.install(this);
    GlobalRegistry.services()
        .register(IdeHostServices.PROOT_PROCESS_LAUNCHER, new ProotProcessLauncherImpl(this));
    UiFeedbackHostImpl uiFeedbackHost = new UiFeedbackHostImpl();
    registerActivityLifecycleCallbacks(uiFeedbackHost);
    GlobalRegistry.services()
        .register(IdeHostServices.UI_FEEDBACK, uiFeedbackHost);
    JsonFileIconHelper.setExternalResolver(
        path -> {
          for (FileIconContributor contributor :
              GlobalRegistry.extensions()
                  .extensions(PluginUiExtensionPoints.FILE_ICON_CONTRIBUTOR)) {
            try {
              String icon = contributor.getIcon(path);
              if (icon != null && !icon.trim().isEmpty()) return icon;
            } catch (Throwable ignored) {
            }
          }
          return null;
        });
    GplInstalledPlugins.loadAll(this, GplPluginLoader.getInstance(this));

    Thread.setDefaultUncaughtExceptionHandler(
        new Thread.UncaughtExceptionHandler() {
          @Override
          public void uncaughtException(Thread thread, Throwable throwable) {
            Intent intent = new Intent(getApplicationContext(), ErrorManagerActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.putExtra("error", Log.getStackTraceString(throwable));
            startActivity(intent);
            Process.killProcess(Process.myPid());
            System.exit(1);
          }
        });
  }

  public void restartApp() {
    Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
    if (intent != null) {
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
      startActivity(intent);
    }
    Process.killProcess(Process.myPid());
  }

  public boolean isSdkS() {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
  }

  public boolean isSdkQ() {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
  }

  public String getVersion() {
    try {
      PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
      return info.versionName;
    } catch (PackageManager.NameNotFoundException e) {
      return null;
    }
  }

  public PreferencesUtils getSetting() {
    return setting;
  }

  public ThemeUtils getThemeUtils() {
    return theme;
  }
}
