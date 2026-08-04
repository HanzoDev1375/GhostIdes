package ir.hanzodev1375.ghostide;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.util.Log;
import com.downloader.PRDownloader;
import ir.hanzodev1375.ghostide.activity.ErrorManagerActivity;
import ir.hanzodev1375.ghostide.codeeditors.setting.PreferencesUtils;
import ir.hanzodev1375.ghostide.plugin.gpl.GplInstalledPlugins;
import ir.hanzodev1375.ghostide.plugin.gpl.GplPluginLoader;
import ir.hanzodev1375.ghostide.shizuku.ShizukuManager;
import ir.hanzodev1375.ghostide.themeengine.ThemeEngine;
import java.util.Calendar;

public class GhostIdeAppLoader extends Application {

  private static Context mApplicationContext;
  private static GhostIdeAppLoader loader;
  private final StringBuilder softwareInfo = new StringBuilder();
  private PreferencesUtils setting;
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
    PRDownloader.initialize(getApplicationContext());
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
  public PreferencesUtils getSetting(){
    return setting;
  }
}
