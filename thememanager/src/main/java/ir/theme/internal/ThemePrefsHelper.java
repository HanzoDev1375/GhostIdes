package ir.theme.internal;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public final class ThemePrefsHelper {

  private final SharedPreferences defaultPrefs;
  private final SharedPreferences namedPrefs;

  public ThemePrefsHelper(Context context) {
    this.defaultPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    this.namedPrefs =
        context.getSharedPreferences(ThemeConstKeys.PREFS_NAME, Context.MODE_PRIVATE);
  }

  public String getAppThemeFile() {
    return defaultPrefs.getString(ThemeConstKeys.KEY_APP_THEME_FILE, "");
  }

  public void setAppThemeFile(String filePath) {
    defaultPrefs.edit().putString(ThemeConstKeys.KEY_APP_THEME_FILE, filePath).apply();
  }

  public String getThemeJson() {
    return namedPrefs.getString(ThemeConstKeys.KEY_THEME, null);
  }

  public void putThemeJson(String json) {
    namedPrefs.edit().putString(ThemeConstKeys.KEY_THEME, json).apply();
  }

  public void removeThemeJson() {
    namedPrefs.edit().remove(ThemeConstKeys.KEY_THEME).apply();
  }
}
