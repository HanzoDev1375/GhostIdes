package ir.hanzodev1375.ghostide.appicon;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

/**
 * Switches the app's launcher icon by enabling exactly one AppIcon's component (SplashActivity or one
 * of its activity-aliases) and disabling all the others.
 *
 * <p>Usage: AppIconManager.applyIcon(context, AppIcon.DARK); AppIcon current =
 * AppIconManager.getCurrentIcon(context);
 */
public final class AppIconManager {

  private static final String PREFS_NAME = "app_icon_prefs";
  private static final String KEY_SELECTED = "selected_icon";

  private AppIconManager() {}

  /** Enables {@code icon}'s component and disables every other icon's component. */
  public static void applyIcon(Context context, AppIcon icon) {
    Context app = context.getApplicationContext();
    PackageManager pm = app.getPackageManager();

    for (AppIcon candidate : AppIcon.values()) {
      int state =
          candidate == icon
              ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
              : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
      pm.setComponentEnabledSetting(
          new ComponentName(app, candidate.componentName), state, PackageManager.DONT_KILL_APP);
    }

    prefs(app).edit().putString(KEY_SELECTED, icon.name()).apply();
  }

  /** Returns the icon that was last applied (defaults to {@link AppIcon#DEFAULT}). */
  public static AppIcon getCurrentIcon(Context context) {
    String saved = prefs(context.getApplicationContext()).getString(KEY_SELECTED, null);
    if (saved == null) return AppIcon.DEFAULT;
    try {
      return AppIcon.valueOf(saved);
    } catch (IllegalArgumentException e) {
      return AppIcon.DEFAULT;
    }
  }

  private static SharedPreferences prefs(Context context) {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
  }
}
