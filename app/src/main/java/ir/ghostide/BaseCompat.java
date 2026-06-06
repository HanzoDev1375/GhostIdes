package ir.hanzodev1375.ghostide.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import ir.hanzodev1375.ghostide.R;
import ir.hanzodev1375.ghostide.codeeditors.setting.PreferencesUtils;
import ir.hanzodev1375.ghostide.codeeditors.setting.Constants;

public class BaseCompat extends AppCompatActivity
    implements SharedPreferences.OnSharedPreferenceChangeListener {

  private PreferencesUtils prefs;

  @Override
  protected void attachBaseContext(Context newBase) {
    
    prefs = new PreferencesUtils(newBase);
    int themeIndex = prefs.getAppTheme();
    int themeResId = getThemeResId(themeIndex);
    Context context = new ContextThemeWrapper(newBase, themeResId);
    super.attachBaseContext(context);
  }

  @Override
  protected void onCreate(Bundle arg0) {
    prefs = new PreferencesUtils(this);
    EdgeToEdge.enable(this);
    super.onCreate(arg0);
    getWindow().setNavigationBarColor(Color.TRANSPARENT);
    getWindow().setStatusBarColor(Color.TRANSPARENT);
  }

  @Override
  protected void onResume() {
    super.onResume();
    
    prefs.getDefaultPreferences().registerOnSharedPreferenceChangeListener(this);
  }

  @Override
  protected void onPause() {
    super.onPause();
    prefs.getDefaultPreferences().unregisterOnSharedPreferenceChangeListener(this);
  }

  private int getThemeResId(int index) {
    try {
      int[] styles = getResources().getIntArray(R.array.theme_styles);
      if (styles != null && index >= 0 && index < styles.length) {
        return styles[index];
      }
    } catch (Exception e) {
      Log.e("BaseCompat", "Error getting theme resource ID", e);
    }
    return R.style.AppTheme; 
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    if (Constants.SharedPreferenceKeys.KEY_APP_THEME.equals(key)) {
      runOnUiThread(() -> recreate());
    }
  }
}
