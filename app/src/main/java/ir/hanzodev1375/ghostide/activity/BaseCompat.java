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
import ir.hanzodev1375.components.childern.ViewChilder;
import ir.hanzodev1375.ghostide.themeengine.Theme;
import java.util.ArrayList;
import java.util.List;
import ir.hanzodev1375.ghostide.codeeditors.setting.PreferencesUtils;
import ir.hanzodev1375.ghostide.themeengine.ThemeEngine;
import ir.hanzodev1375.ghostide.utils.LocaleHelper;
import ir.theme.GhostTheme;
import ir.theme.ThemeManager;
import ir.theme.ThemeUtils;
import android.view.View;

public class BaseCompat extends AppCompatActivity
    implements SharedPreferences.OnSharedPreferenceChangeListener {

  private PreferencesUtils prefs;
  private Theme lastTheme;
  private AnimationManager animMgr;
  private List<BaseCompat> ACTIVITIES = new ArrayList<>();

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
    if (animMgr.areAnimationsEnabled()) {
      MaterialSharedAxis enter = new MaterialSharedAxis(MaterialSharedAxis.Z, true);
      enter.setDuration(350);
      getWindow().setEnterTransition(enter);
    } else {
      getWindow().setEnterTransition(null);
    }
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

  protected void setupBackgroundBlur(ViewChilder backgroundView, View... tintViews) {
    boolean showBg = new PreferencesUtils(this).isShowBackground();
    ThemeUtils themeUtil = new ThemeUtils(new ThemeManager(this));
    GhostTheme theme = themeUtil.getTheme();
    boolean hasImage =
        theme != null
            && theme.getWidget() != null
            && theme.getWidget().getImagepath() != null
            && !theme.getWidget().getImagepath().isEmpty();

    if (!showBg) {
      if (backgroundView != null) backgroundView.clear();
      return;
    }

    getWindow().setStatusBarColor(Color.TRANSPARENT);
    getWindow().setNavigationBarColor(Color.TRANSPARENT);

    if (backgroundView != null) {
      if (hasImage) {
        backgroundView.setVisibility(View.VISIBLE);
        themeUtil.applyImageBackground(backgroundView);
      } else {
        backgroundView.clear();
      }
    }

    if (hasImage && theme.getActivity() != null && theme.getActivity().getBackground() != null) {
      int bgColor = Color.parseColor(theme.getActivity().getBackground());
      for (View v : tintViews) {
        if (v != null && v != backgroundView) {
          v.setBackgroundColor(bgColor);
        }
      }
    }
  }

  @Override
  public void startActivity(Intent i) {

    if (animMgr.areAnimationsEnabled()) {
      ActivityOptions op = ActivityOptions.makeSceneTransitionAnimation(this);
      MaterialSharedAxis enter = new MaterialSharedAxis(MaterialSharedAxis.Z, true);
      enter.setDuration(350);
      MaterialSharedAxis exit = new MaterialSharedAxis(MaterialSharedAxis.Z, false);
      exit.setDuration(350);
      MaterialSharedAxis reenter = new MaterialSharedAxis(MaterialSharedAxis.Y, true);
      reenter.setDuration(350);
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
  public AnimationManager getAnimationManager() {
    return animMgr;
  }

  /**
   * شروع یک اکتیویتی با Shared Element Transition. هر دو ویو (مبدا و مقصد) باید transitionName
   * یکسان داشته باشند.
   */
  protected void startActivityWithSharedElement(
      Intent intent, View sharedView, String transitionName) {
    if (sharedView == null || transitionName == null) {
      startActivity(intent);
      return;
    }
    sharedView.setTransitionName(transitionName);
    if (animMgr.areAnimationsEnabled()) {
      ActivityOptions op =
          ActivityOptions.makeSceneTransitionAnimation(this, sharedView, transitionName);
      MaterialSharedAxis exit = new MaterialSharedAxis(MaterialSharedAxis.Z, false);
      exit.setDuration(350);
      getWindow().setExitTransition(exit);
      super.startActivity(intent, op.toBundle());
    } else {
      getWindow().setExitTransition(null);
      super.startActivity(intent);
    }
  }
}
