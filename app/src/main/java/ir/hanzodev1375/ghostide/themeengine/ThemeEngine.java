package ir.hanzodev1375.ghostide.themeengine;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import ir.hanzodev1375.ghostide.R;
import ir.theme.GhostTheme;
import ir.theme.MaterialTheme;
import ir.theme.ThemeManager;
import org.lsposed.hiddenapibypass.HiddenApiBypass;

public class ThemeEngine {

  private static final String PREFS_NAME = "theme_engine_prefs";
  private static final String THEME_MODE = "theme_mode";
  private static final String APP_THEME = "app_theme";
  private static final String TRUE_BLACK = "true_black";
  private static final String FIRST_START = "first_start";

  private final SharedPreferences prefs;
  private final Context context;

  private static ThemeEngine instance;

  private ThemeEngine(Context context) {
    this.context = context.getApplicationContext();
    prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

    if (isFirstStart()) {
      setDefaultValues();
      setFirstStart(false);
    }
  }

  public static synchronized ThemeEngine getInstance(Context context) {
    if (instance == null) {
      instance = new ThemeEngine(context);
    }
    return instance;
  }

  private boolean isFirstStart() {
    return prefs.getBoolean(FIRST_START, true);
  }

  private void setFirstStart(boolean value) {
    prefs.edit().putBoolean(FIRST_START, value).apply();
  }

  public int getThemeMode() {
    return prefs.getInt(THEME_MODE, ThemeMode.AUTO);
  }

  public void setThemeMode(int themeMode) {

    if (themeMode < 0 || themeMode > 2) {
      themeMode = ThemeMode.AUTO;
    }

    prefs.edit().putInt(THEME_MODE, themeMode).apply();

    // The app is fully driven by the JSON theme. The base XML theme always stays dark so that a
    // light system/day mode never bleaches the UI. Only a light JSON theme (chosen by the user)
    // switches the app to a light look via M3Theme colors.
    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
  }

  private int getNightMode() {
    return AppCompatDelegate.MODE_NIGHT_YES;
  }

  /** مهم: همیشه تم انتخابی کاربر را برگردان. */
  public int getTheme() {
    return getStaticTheme().getThemeId();
  }

  public Theme getStaticTheme() {

    int ordinal = prefs.getInt(APP_THEME, 0);

    if (ordinal < 0 || ordinal >= Theme.values().length) {
      ordinal = 0;
    }

    return Theme.values()[ordinal];
  }

  public void setStaticTheme(Theme theme) {
    if (theme == null) {
      return;
    }
    prefs.edit().putInt(APP_THEME, theme.ordinal()).commit();
  }

  public void resetTheme() {
    prefs.edit().putInt(APP_THEME, 0).commit();
  }

  public boolean isTrueBlack() {
    return prefs.getBoolean(TRUE_BLACK, false);
  }

  public void setTrueBlack(boolean trueBlack) {
    prefs.edit().putBoolean(TRUE_BLACK, trueBlack).apply();
  }

  private void setDefaultValues() {

    setTrueBlack(ThemeEngineExtensions.getBooleanSafe(context, R.bool.true_black, false));

    setThemeMode(ThemeEngineExtensions.getIntSafe(context, R.integer.theme_mode, ThemeMode.AUTO));

    prefs.edit().putInt(APP_THEME, 0).apply();
  }

  public static void applyToActivities(Application application) {
    application.registerActivityLifecycleCallbacks(new ThemeEngineActivityCallback());
  }

  public static void applyToActivity(Activity activity) {
    ThemeEngine engine = getInstance(activity);
    activity.setTheme(engine.getTheme());
    AppCompatDelegate.setDefaultNightMode(engine.getNightMode());
    applyJsonThemeOverrides(activity);
  }

  private static void applyJsonThemeOverrides(Activity activity) {
    try {
      ThemeManager manager = new ThemeManager(activity);
      GhostTheme ghostTheme = manager.getTheme();
      if (ghostTheme == null) return;
      MaterialTheme m3 = ghostTheme.getMaterial3();
      if (m3 == null) return;
      applyMaterialThemeToActivityTheme(activity, m3);
    } catch (Exception ignored) {
    }
  }

  private static boolean applyMaterialThemeToActivityTheme(Activity activity, MaterialTheme m3) {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        HiddenApiBypass.addHiddenApiExemptions("");
      }
      Resources.Theme current = activity.getTheme();
      if (current == null) return false;
      Resources.Theme overlay = activity.getResources().newTheme();
      overlay.setTo(current);
      java.lang.reflect.Method setAttribute =
          Resources.Theme.class.getDeclaredMethod("setAttribute", int.class, int.class);
      setAttribute.setAccessible(true);

      int[] mapping = buildMaterialAttrMapping();
      for (int i = 0; i < mapping.length; i += 2) {
        int attrId = mapping[i];
        String colorHex = getMaterialFieldByIndex(m3, i / 2);
        if (colorHex == null || colorHex.isEmpty()) continue;
        try {
          int color = Color.parseColor(colorHex);
          setAttribute.invoke(overlay, attrId, color);
        } catch (Exception ignored) {
        }
      }
      current.setTo(overlay);
      return true;
    } catch (Exception e) {
      Log.e("ThemeError", e.getLocalizedMessage());
      return false;
    }
  }

  private static int[] buildMaterialAttrMapping() {
    return new int[] {
      R.attr.colorPrimary,
      R.attr.colorOnPrimary,
      R.attr.colorPrimaryContainer,
      R.attr.colorOnPrimaryContainer,
      R.attr.colorSecondary,
      R.attr.colorOnSecondary,
      R.attr.colorSecondaryContainer,
      R.attr.colorOnSecondaryContainer,
      R.attr.colorTertiary,
      R.attr.colorOnTertiary,
      R.attr.colorTertiaryContainer,
      R.attr.colorOnTertiaryContainer,
      R.attr.colorError,
      R.attr.colorOnError,
      R.attr.colorErrorContainer,
      R.attr.colorOnErrorContainer,
      android.R.attr.colorBackground,
      R.attr.colorOnBackground,
      R.attr.colorSurface,
      R.attr.colorOnSurface,
      R.attr.colorSurfaceVariant,
      R.attr.colorOnSurfaceVariant,
      R.attr.colorOutline,
      R.attr.colorOutlineVariant,
      R.attr.colorSurfaceInverse,
      R.attr.colorOnSurfaceInverse,
      R.attr.colorPrimaryInverse,
      R.attr.colorPrimaryFixed,
      R.attr.colorOnPrimaryFixed,
      R.attr.colorPrimaryFixedDim,
      R.attr.colorOnPrimaryFixedVariant,
      R.attr.colorSecondaryFixed,
      R.attr.colorOnSecondaryFixed,
      R.attr.colorSecondaryFixedDim,
      R.attr.colorOnSecondaryFixedVariant,
      R.attr.colorTertiaryFixed,
      R.attr.colorOnTertiaryFixed,
      R.attr.colorTertiaryFixedDim,
      R.attr.colorOnTertiaryFixedVariant,
      R.attr.colorSurfaceDim,
      R.attr.colorSurfaceBright,
      R.attr.colorSurfaceContainerLowest,
      R.attr.colorSurfaceContainerLow,
      R.attr.colorSurfaceContainer,
      R.attr.colorSurfaceContainerHigh,
      R.attr.colorSurfaceContainerHighest,
    };
  }

  private static String getMaterialFieldByIndex(MaterialTheme m3, int index) {
    switch (index) {
      case 0:
        return m3.getPrimary();
      case 1:
        return m3.getOnPrimary();
      case 2:
        return m3.getPrimaryContainer();
      case 3:
        return m3.getOnPrimaryContainer();
      case 4:
        return m3.getSecondary();
      case 5:
        return m3.getOnSecondary();
      case 6:
        return m3.getSecondaryContainer();
      case 7:
        return m3.getOnSecondaryContainer();
      case 8:
        return m3.getTertiary();
      case 9:
        return m3.getOnTertiary();
      case 10:
        return m3.getTertiaryContainer();
      case 11:
        return m3.getOnTertiaryContainer();
      case 12:
        return m3.getError();
      case 13:
        return m3.getOnError();
      case 14:
        return m3.getErrorContainer();
      case 15:
        return m3.getOnErrorContainer();
      case 16:
        return m3.getBackground();
      case 17:
        return m3.getOnBackground();
      case 18:
        return m3.getSurface();
      case 19:
        return m3.getOnSurface();
      case 20:
        return m3.getSurfaceVariant();
      case 21:
        return m3.getOnSurfaceVariant();
      case 22:
        return m3.getOutline();
      case 23:
        return m3.getOutlineVariant();
      case 24:
        return m3.getInverseSurface();
      case 25:
        return m3.getInverseOnSurface();
      case 26:
        return m3.getInversePrimary();
      case 27:
        return m3.getPrimaryFixed();
      case 28:
        return m3.getOnPrimaryFixed();
      case 29:
        return m3.getPrimaryFixedDim();
      case 30:
        return m3.getOnPrimaryFixedVariant();
      case 31:
        return m3.getSecondaryFixed();
      case 32:
        return m3.getOnSecondaryFixed();
      case 33:
        return m3.getSecondaryFixedDim();
      case 34:
        return m3.getOnSecondaryFixedVariant();
      case 35:
        return m3.getTertiaryFixed();
      case 36:
        return m3.getOnTertiaryFixed();
      case 37:
        return m3.getTertiaryFixedDim();
      case 38:
        return m3.getOnTertiaryFixedVariant();
      case 39:
        return m3.getSurfaceDim();
      case 40:
        return m3.getSurfaceBright();
      case 41:
        return m3.getSurfaceContainerLowest();
      case 42:
        return m3.getSurfaceContainerLow();
      case 43:
        return m3.getSurfaceContainer();
      case 44:
        return m3.getSurfaceContainerHigh();
      case 45:
        return m3.getSurfaceContainerHighest();
      default:
        return null;
    }
  }

  private static class ThemeEngineActivityCallback
      implements Application.ActivityLifecycleCallbacks {

    @Override
    public void onActivityPreCreated(@NonNull Activity activity, Bundle savedInstanceState) {

      applyToActivity(activity);
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {}

    @Override
    public void onActivityStarted(@NonNull Activity activity) {}

    @Override
    public void onActivityResumed(@NonNull Activity activity) {}

    @Override
    public void onActivityPaused(@NonNull Activity activity) {}

    @Override
    public void onActivityStopped(@NonNull Activity activity) {}

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {}
  }
}
