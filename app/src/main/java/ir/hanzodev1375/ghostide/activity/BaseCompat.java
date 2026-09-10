package ir.hanzodev1375.ghostide.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.ActivityOptions;
import android.view.animation.DecelerateInterpolator;
import androidx.annotation.NonNull;
import com.google.android.material.transition.platform.MaterialSharedAxis;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import ir.hanzodev1375.components.animators.AnimationManager;
import ir.hanzodev1375.components.childern.ViewChilder;
import ir.theme.M3Theme;
import ir.hanzodev1375.ghostide.codeeditors.setting.PreferencesUtils;
import ir.hanzodev1375.ghostide.utils.LocaleHelper;
import ir.theme.GhostTheme;
import ir.theme.ThemeBus;
import ir.theme.ThemeManager;
import ir.theme.ThemePreviewBuilder;
import ir.theme.ThemeUtils;
import android.view.View;
import java.util.Map;

public class BaseCompat extends AppCompatActivity
    implements SharedPreferences.OnSharedPreferenceChangeListener,
        ThemeBus.ThemeChangeListener {

  private PreferencesUtils prefs;
  private AnimationManager animMgr;
  private ValueAnimator themeAnimator;

  @Override
  protected void attachBaseContext(Context newBase) {
    prefs = new PreferencesUtils(newBase);
    super.attachBaseContext(LocaleHelper.applyLocale(newBase));
  }

  @Override
  protected void onCreate(Bundle arg0) {
    prefs = new PreferencesUtils(this);
    EdgeToEdge.enable(this);
    super.onCreate(arg0);
    ThemeBus.getInstance().register(this);
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

    applyJsonThemeBackground();
    new Handler(Looper.getMainLooper())
        .post(
            () -> {
              applyJsonThemeBackground();
              View decor = getWindow().getDecorView();
              if (decor != null) {
                M3Theme.applyTopLevel(decor);
              }
            });
  }

  /**
   * The app is fully driven by the JSON theme. The root background always comes from the theme's
   * surface color, unless a background image is enabled in the theme (then the image is shown by
   * {@link #setupBackgroundBlur} on the activities that have a background view). This keeps the UI
   * independent from the XML light theme, so switching the device to day mode has no effect unless
   * the user picks a light JSON theme.
   */
  private void applyJsonThemeBackground() {
    if (isFinishing()) {
      return;
    }
    View decor = getWindow().getDecorView();
    if (decor == null) {
      return;
    }
    try {
      boolean showBackground = new PreferencesUtils(this).isShowBackground();
      boolean hasImage = false;
      try {
        GhostTheme theme = new ThemeUtils(new ThemeManager(this)).getTheme();
        hasImage =
            theme != null
                && theme.getWidget() != null
                && theme.getWidget().getImagepath() != null
                && !theme.getWidget().getImagepath().isEmpty();
      } catch (Throwable ignoredImg) {
      }
      if (!showBackground || !hasImage) {
        Integer surface = M3Theme.surface();
        if (surface == null) {
          surface = M3Theme.surfaceContainer();
        }
        if (surface != null) {
          decor.setBackgroundColor(surface);
          View content = decor.findViewById(android.R.id.content);
          if (content != null) {
            content.setBackgroundColor(surface);
          }
        }
      }
    } catch (Throwable ignored) {
    }
  }

  @Override
  protected void onDestroy() {
    if (themeAnimator != null) {
      themeAnimator.cancel();
    }
    ThemeBus.getInstance().unregister(this);
    super.onDestroy();
  }

  @Override
  protected void onResume() {
    super.onResume();

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

  @Override
  public void onThemeChanged(GhostTheme oldTheme, GhostTheme newTheme, boolean animated) {
    if (isFinishing()) {
      return;
    }
    if (themeAnimator != null) {
      themeAnimator.cancel();
    }
    boolean canAnimate =
        animated
            && oldTheme != null
            && newTheme != null
            && ValueAnimator.areAnimatorsEnabled()
            && animMgr != null
            && animMgr.areAnimationsEnabled();
    if (canAnimate) {
      startThemeTransition(oldTheme, newTheme);
    } else {
      M3Theme.setPreviewTheme(null);
      applyThemeFinal();
    }
  }

  /** Re-applies the current theme immediately on this activity, without recreating it. */
  public final void reapplyThemeLive() {
    if (isFinishing()) {
      return;
    }
    if (themeAnimator != null) {
      themeAnimator.cancel();
    }
    M3Theme.setPreviewTheme(null);
    applyThemeFinal();
  }

  private void startThemeTransition(GhostTheme oldTheme, GhostTheme newTheme) {
    if (themeAnimator != null) {
      themeAnimator.cancel();
    }
    Map<String, Integer> oldPalette = ThemePreviewBuilder.palette(oldTheme);
    Map<String, Integer> newPalette = ThemePreviewBuilder.palette(newTheme);
    ThemePreviewBuilder previewBuilder = new ThemePreviewBuilder(newTheme);
    ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
    animator.setDuration(250L);
    animator.setInterpolator(new DecelerateInterpolator());
    animator.addUpdateListener(
        a -> {
          float t = a.getAnimatedFraction();
          Map<String, Integer> palette = ThemePreviewBuilder.blendPalettes(oldPalette, newPalette, t);
          M3Theme.setPreviewTheme(previewBuilder.build(palette));
          applyThemeNow();
        });
    animator.addListener(
        new AnimatorListenerAdapter() {
          @Override
          public void onAnimationEnd(Animator animation) {
            finishThemeTransition(newTheme);
          }

          @Override
          public void onAnimationCancel(Animator animation) {
            finishThemeTransition(newTheme);
          }
        });
    themeAnimator = animator;
    animator.start();
  }

  private void finishThemeTransition(GhostTheme newTheme) {
    if (themeAnimator != null) {
      themeAnimator.removeAllListeners();
      themeAnimator.removeAllUpdateListeners();
      themeAnimator.cancel();
    }
    themeAnimator = null;
    M3Theme.setPreviewTheme(newTheme);
    applyThemeFinal();
  }

  private void applyThemeNow() {
    M3Theme.applyTopLevel(getWindow().getDecorView());
    ThemeUtils themeUtils = new ThemeUtils(new ThemeManager(this));
    themeUtils.applyActivity(this);
    applyJsonThemeBackground();
    applyOwnTheme(themeUtils);
  }

  /** One-shot full restyle: repaints every themed view in the window's decor tree. */
  private void applyThemeFinal() {
    if (isFinishing()) {
      return;
    }
    View decor = getWindow().getDecorView();
    if (decor != null) {
      M3Theme.apply(decor);
    }
    applyThemeNow();
  }

  protected void applyOwnTheme(ThemeUtils themeUtils) {}

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
        getWindow().getDecorView().setBackgroundColor(M3Theme.surface());
        getWindow().setNavigationBarColor(M3Theme.surface());
        getWindow().setStatusBarColor(M3Theme.surface());
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
