package ir.hanzodev1375.ghostide.onboarding;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.AccelerateDecelerateInterpolator;

final class OnboardingAnim {

  private OnboardingAnim() {}

  private static final OvershootInterpolator OVERSHOOT = new OvershootInterpolator(1.2f);
  private static final DecelerateInterpolator DECEL = new DecelerateInterpolator();
  private static final AccelerateDecelerateInterpolator ACCEL_DECEL =
      new AccelerateDecelerateInterpolator();

  static void revealCard(View card, long delay) {
    card.setAlpha(0f);
    card.setScaleX(0.92f);
    card.setScaleY(0.92f);
    card.setTranslationY(40f);
    AnimatorSet set = new AnimatorSet();
    set.playTogether(
        ObjectAnimator.ofFloat(card, "alpha", 0f, 1f),
        ObjectAnimator.ofFloat(card, "scaleX", 0.92f, 1f),
        ObjectAnimator.ofFloat(card, "scaleY", 0.92f, 1f),
        ObjectAnimator.ofFloat(card, "translationY", 40f, 0f));
    set.setDuration(500);
    set.setInterpolator(ACCEL_DECEL);
    set.setStartDelay(delay);
    set.start();
  }

  static void slideUpFade(View view, long delay) {
    view.setAlpha(0f);
    view.setTranslationY(60f);
    AnimatorSet set = new AnimatorSet();
    set.playTogether(
        ObjectAnimator.ofFloat(view, "alpha", 0f, 1f),
        ObjectAnimator.ofFloat(view, "translationY", 60f, 0f));
    set.setDuration(450);
    set.setInterpolator(DECEL);
    set.setStartDelay(delay);
    set.start();
  }

  static void slideUpFadeBounce(View view, long delay) {
    view.setAlpha(0f);
    view.setTranslationY(50f);
    view.setScaleX(0.8f);
    view.setScaleY(0.8f);
    AnimatorSet set = new AnimatorSet();
    set.playTogether(
        ObjectAnimator.ofFloat(view, "alpha", 0f, 1f),
        ObjectAnimator.ofFloat(view, "translationY", 50f, 0f),
        ObjectAnimator.ofFloat(view, "scaleX", 0.8f, 1f),
        ObjectAnimator.ofFloat(view, "scaleY", 0.8f, 1f));
    set.setDuration(550);
    set.setInterpolator(OVERSHOOT);
    set.setStartDelay(delay);
    set.start();
  }

  static void scaleIn(View view, long delay) {
    view.setAlpha(0f);
    view.setScaleX(0.3f);
    view.setScaleY(0.3f);
    AnimatorSet set = new AnimatorSet();
    set.playTogether(
        ObjectAnimator.ofFloat(view, "alpha", 0f, 1f),
        ObjectAnimator.ofFloat(view, "scaleX", 0.3f, 1f),
        ObjectAnimator.ofFloat(view, "scaleY", 0.3f, 1f));
    set.setDuration(600);
    set.setInterpolator(OVERSHOOT);
    set.setStartDelay(delay);
    set.start();
  }

  static void slideRightFade(View view, long delay) {
    view.setAlpha(0f);
    view.setTranslationX(-120f);
    AnimatorSet set = new AnimatorSet();
    set.playTogether(
        ObjectAnimator.ofFloat(view, "alpha", 0f, 1f),
        ObjectAnimator.ofFloat(view, "translationX", -120f, 0f));
    set.setDuration(400);
    set.setInterpolator(DECEL);
    set.setStartDelay(delay);
    set.start();
  }

  static void pop(View view, long delay) {
    view.setAlpha(0f);
    view.setScaleX(0f);
    view.setScaleY(0f);
    AnimatorSet set = new AnimatorSet();
    set.playTogether(
        ObjectAnimator.ofFloat(view, "alpha", 0f, 1f),
        ObjectAnimator.ofFloat(view, "scaleX", 0f, 1f),
        ObjectAnimator.ofFloat(view, "scaleY", 0f, 1f));
    set.setDuration(450);
    set.setInterpolator(OVERSHOOT);
    set.setStartDelay(delay);
    set.start();
  }

  static void dividerExpand(View view, long delay) {
    view.setAlpha(0f);
    view.setScaleX(0f);
    AnimatorSet set = new AnimatorSet();
    set.playTogether(
        ObjectAnimator.ofFloat(view, "alpha", 0f, 1f),
        ObjectAnimator.ofFloat(view, "scaleX", 0f, 1f));
    set.setDuration(400);
    set.setInterpolator(DECEL);
    set.setStartDelay(delay);
    set.start();
  }

  static void shimmer(View view) {
    view.animate()
        .alpha(0.7f)
        .setDuration(300)
        .setInterpolator(ACCEL_DECEL)
        .withEndAction(
            () ->
                view.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .setInterpolator(ACCEL_DECEL)
                    .start())
        .start();
  }

  static void bounceOnce(View view, long delay) {
    AnimatorSet set = new AnimatorSet();
    set.playTogether(
        ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.15f, 1f),
        ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.15f, 1f));
    set.setDuration(350);
    set.setInterpolator(ACCEL_DECEL);
    set.setStartDelay(delay);
    set.start();
  }
}