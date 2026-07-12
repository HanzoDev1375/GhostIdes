package ir.hanzodev1375.ghostide.activity;

import android.app.ActivityOptions;
import androidx.annotation.NonNull;
import com.google.android.material.transition.platform.MaterialSharedAxis;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import ir.hanzodev1375.components.animators.AnimationManager;
import ir.hanzodev1375.ghostide.themeengine.Theme;
import java.util.ArrayList;
import java.util.List;
import ir.hanzodev1375.ghostide.codeeditors.setting.PreferencesUtils;
import ir.hanzodev1375.ghostide.themeengine.ThemeEngine;
import ir.hanzodev1375.ghostide.utils.LocaleHelper;

public class BaseCompat extends AppCompatActivity
    implements SharedPreferences.OnSharedPreferenceChangeListener {

  private PreferencesUtils prefs;
  private Theme lastTheme;
  AnimationManager animMgr;
  private  List<BaseCompat> ACTIVITIES = new ArrayList<>();

  @Override
  protected void attachBaseContext(Context newBase) {
    prefs = new PreferencesUtils(newBase);
    super.attachBaseContext(LocaleHelper.applyLocale(newBase));
  }

  @Override
  protected void onCreate(Bundle arg0) {
    prefs = new PreferencesUtils(this);
    EdgeToEdge.enable(this);
    ThemeEngine.applyToActivity(this);
    lastTheme = ThemeEngine.getInstance(this).getStaticTheme();
    super.onCreate(arg0);
    ACTIVITIES.add(this);
    getWindow().setNavigationBarColor(Color.TRANSPARENT);
    getWindow().setStatusBarColor(Color.TRANSPARENT);
    animMgr = AnimationManager.getInstance(this);
  }

  @Override
  protected void onDestroy() {
    ACTIVITIES.remove(this);
    super.onDestroy();
  }

  @Override
  protected void onResume() {
    super.onResume();

    Theme currentTheme = ThemeEngine.getInstance(this).getStaticTheme();
    if (currentTheme != lastTheme) {
      recreate();
      return;
    }

    prefs.getDefaultPreferences().registerOnSharedPreferenceChangeListener(this);
    animMgr.registerReceiver(this);
  }

  @Override
  protected void onPause() {
    super.onPause();
    prefs.getDefaultPreferences().unregisterOnSharedPreferenceChangeListener(this);
    animMgr.unregisterReceiver(this);
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {}

  public void recreateAllActivities() {
    List<BaseCompat> copy = new ArrayList<>(ACTIVITIES);
    for (BaseCompat activity : copy) {
      if (activity == null) {
        continue;
      }
      if (activity.isFinishing()) {
        continue;
      }
      activity.runOnUiThread(activity::recreate);
    }
  }

  @Override
  public void startActivity(Intent i) {

    if (animMgr.areAnimationsEnabled()) {
      ActivityOptions op = ActivityOptions.makeSceneTransitionAnimation(this);
      MaterialSharedAxis enter = new MaterialSharedAxis(MaterialSharedAxis.Z, true);
      enter.setDuration(1000);
      MaterialSharedAxis exit = new MaterialSharedAxis(MaterialSharedAxis.Z, false);
      exit.setDuration(1000);
      MaterialSharedAxis reenter = new MaterialSharedAxis(MaterialSharedAxis.Y, true);
      reenter.setDuration(1000);
      getWindow().setExitTransition(exit);
      getWindow().setEnterTransition(enter);
      getWindow().setReenterTransition(reenter);
      super.startActivity(i, op.toBundle());
    } else {
      getWindow().setExitTransition(null);
      getWindow().setEnterTransition(null);
      getWindow().setReenterTransition(null);
      super.startActivity(i);
    }
  }
  @NonNull
  public AnimationManager getAnimationManager(){
    return animMgr;
  }
}
