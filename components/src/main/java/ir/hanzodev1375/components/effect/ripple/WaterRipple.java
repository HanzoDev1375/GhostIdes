package ir.hanzodev1375.components.effect.ripple;

import android.content.Context;
import android.os.Build;
import android.view.View;

import ir.hanzodev1375.components.animators.AnimationManager;

public class WaterRipple {

  private static RippleEffect currentRipple;

  private WaterRipple() {}

  /**
   * Play the "stone thrown into water" ripple effect on the given root view. No-op (and crash-safe)
   * when animations are disabled, the root view is not ready, or the device cannot render the
   * effect.
   *
   * @param root the view to ripple (usually the window decor view)
   * @param x X coordinate of the impact point (in window coordinates)
   * @param y Y coordinate of the impact point (in window coordinates)
   * @param intensity ripple strength, clamps to [0.3 .. 0.9] internally
   */
  public static void ripple(View root, float x, float y, float intensity) {
    if (root == null || root.getWidth() == 0 || root.getHeight() == 0) {
      return;
    }
    try {
      Context context = root.getContext();
      if (context == null) {
        return;
      }
      AnimationManager animationManager = AnimationManager.getInstance(context);
      animationManager.registerReceiver(context);
      if (!animationManager.areAnimationsEnabled()) {
        return;
      }
      if (currentRipple == null || currentRipple.view != root) {
        currentRipple = create(root);
      }
      float clamped = Math.min(0.9f, Math.max(0.3f, intensity));
      currentRipple.animate(x, y, clamped);
    } catch (Throwable ignored) {
      // A cosmetic effect must never crash the app.
    }
  }

  private static RippleEffect create(View root) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && SuperRipple.supports()) {
      try {
        return new SuperRipple(root);
      } catch (Throwable ignored) {
        // AGSL is not available on this device -> scale-based fallback.
      }
    }
    return new SuperRippleFallback(root);
  }

  /**
   * Clears the cached ripple instance. Call this if the referenced root view has been
   * detached/destroyed to avoid leaking it.
   */
  public static void clear() {
    currentRipple = null;
  }
}
