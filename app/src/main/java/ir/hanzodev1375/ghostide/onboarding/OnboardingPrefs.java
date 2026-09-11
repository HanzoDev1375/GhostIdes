package ir.hanzodev1375.ghostide.onboarding;

import android.content.Context;
import android.content.SharedPreferences;

// این کلاس باید چدا باشه تا تداخل نداشته باشه چون یک باز کال میشه
public final class OnboardingPrefs {

  private static final String PREFS = "onboarding";
  private static final String KEY_COMPLETED = "completed";

  /** بعد از اتمام تست مقدار این flag را false کن تا فقط در اولین نصب نمایش داده شود. */
  public static final boolean FORCE_SHOW = true;

  private OnboardingPrefs() {}

  public static boolean shouldShow(Context context) {
    if (FORCE_SHOW) return true;
    return !getPrefs(context).getBoolean(KEY_COMPLETED, false);
  }

  public static void markCompleted(Context context) {
    getPrefs(context).edit().putBoolean(KEY_COMPLETED, true).apply();
  }

  public static void reset(Context context) {
    getPrefs(context).edit().clear().apply();
  }

  private static SharedPreferences getPrefs(Context context) {
    return context
        .getApplicationContext()
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
  }
}