package ir.hanzodev1375.ghostide.postman.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * Tiny wrapper around SharedPreferences for the handful of settings this app has: theme mode,
 * request timeout and the "disable SSL verification" testing toggle.
 */
public class PrefsManager {

  private static final String PREFS_NAME = "ghostide_prefs";
  private static final String KEY_THEME_MODE = "theme_mode";
  private static final String KEY_TIMEOUT_SECONDS = "timeout_seconds";
  private static final String KEY_DISABLE_SSL = "disable_ssl_verification";

  private static final int DEFAULT_TIMEOUT_SECONDS = 30;

  private final SharedPreferences prefs;

  public PrefsManager(Context context) {
    prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
  }

  public int getThemeMode() {
    return prefs.getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
  }

  public void setThemeMode(int mode) {
    prefs.edit().putInt(KEY_THEME_MODE, mode).apply();
  }

  public int getTimeoutSeconds() {
    return prefs.getInt(KEY_TIMEOUT_SECONDS, DEFAULT_TIMEOUT_SECONDS);
  }

  public void setTimeoutSeconds(int seconds) {
    prefs.edit().putInt(KEY_TIMEOUT_SECONDS, seconds).apply();
  }

  public boolean isSslVerificationDisabled() {
    return prefs.getBoolean(KEY_DISABLE_SSL, false);
  }

  public void setSslVerificationDisabled(boolean disabled) {
    prefs.edit().putBoolean(KEY_DISABLE_SSL, disabled).apply();
  }
}
