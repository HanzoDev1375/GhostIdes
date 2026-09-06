package ir.hanzodev1375.components.effect.ripple;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Outline;
import android.graphics.Path;
import android.os.Build;
import android.view.RoundedCorner;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.WindowInsets;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;

import ir.hanzodev1375.components.animators.CubicBezierInterpolator;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class SuperRippleFallback extends RippleEffect {

  public static class Effect {
    public final ValueAnimator animator;
    public final float cx, cy;
    public final float intensity;
    public float t;
    public float duration;

    private Effect(
        float cx, float cy,
        float intensity,
        ValueAnimator animator
    ) {
      this.cx = cx;
      this.cy = cy;
      this.intensity = intensity;
      this.animator = animator;
    }
  }

  public final float[] radii = new float[8];
  private final Path outlineProviderPath = new Path();
  private final ViewOutlineProvider outlineProvider = new ViewOutlineProvider() {
    @Override
    public void getOutline(View view, Outline outline) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radii[0]);
      }
    }
  };

  public final ArrayList<Effect> effects = new ArrayList<>();
  public final int MAX_COUNT = 10;

  public SuperRippleFallback(View view) {
    super(view);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      final WindowInsets insets = view.getRootWindowInsets();
      final RoundedCorner topLeftCorner = insets == null ? null : insets.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT);
      final RoundedCorner topRightCorner = insets == null ? null : insets.getRoundedCorner(RoundedCorner.POSITION_TOP_RIGHT);
      final RoundedCorner bottomLeftCorner = insets == null ? null : insets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_LEFT);
      final RoundedCorner bottomRightCorner = insets == null ? null : insets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_RIGHT);

      radii[0] = radii[1] = topLeftCorner == null ? 0 : topLeftCorner.getRadius();
      radii[2] = radii[3] = topRightCorner == null ? 0 : topRightCorner.getRadius();
      radii[4] = radii[5] = bottomRightCorner == null ? 0 : bottomRightCorner.getRadius();
      radii[6] = radii[7] = bottomLeftCorner == null ? 0 : bottomLeftCorner.getRadius();
    }

    outlineProviderPath.rewind();
    outlineProviderPath.addRoundRect(0, 0, view.getWidth(), view.getHeight(), radii, Path.Direction.CW);
  }

  @Override
  public void animate(float cx, float cy, float intensity) {
    if (effects.size() >= MAX_COUNT) return;

    final float duration = 0.5f;

    final ValueAnimator animator = ValueAnimator.ofFloat(0f, duration);
    final Effect effect = new Effect(cx, cy, intensity, animator);
    effect.duration = duration;

    animator.addUpdateListener(anm -> {
      effect.t = (float) anm.getAnimatedValue();
      updateProperties();
    });
    animator.addListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationEnd(Animator animation) {
        effects.remove(effect);
        updateProperties();
      }
    });
    animator.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
    animator.setDuration((long) (duration * 1000L));

    effects.add(effect);
    updateProperties();

    animator.start();
  }

  private void updateProperties() {
    float s = 1f, px = 0, py = 0, ps = 0;
    for (Effect effect : effects) {
      float t = effect.t / effect.duration;
      float x = (1f - (float) Math.sin(Math.PI * t));
      s *= (1f - .04f * effect.intensity) + .04f * effect.intensity * x;
      px += effect.cx * 1f;
      py += effect.cy * 1f;
      ps += 1f;
    }
    if (ps < 1) {
      px += view.getWidth() / 2f * (1f - ps);
      py += view.getHeight() / 2f * (1f - ps);
      ps = 1f;
    }
    view.setScaleX(s);
    view.setScaleY(s);
    view.setPivotX(px / ps);
    view.setPivotY(py / ps);
    if (view.getOutlineProvider() != (effects.isEmpty() ? null : outlineProvider)) {
      view.setOutlineProvider(effects.isEmpty() ? null : outlineProvider);
      view.invalidate();
    }
  }
}