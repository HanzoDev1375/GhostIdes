package ir.hanzodev1375.components.animators;

import android.view.animation.BaseInterpolator;

public class SpringInterpolator extends BaseInterpolator {

  private final float damping;
  private final float frequency;

  public SpringInterpolator() {
    this(4.5f, 10f);
  }

  public SpringInterpolator(float damping, float frequency) {
    this.damping = damping;
    this.frequency = frequency;
  }

  /**
   * Damped harmonic spring. Returns 0 at t=0, 1 at t=1 and overshoots in between, mimicking a
   * physical spring settling on its target.
   */
  @Override
  public float getInterpolation(float t) {
    if (t <= 0f) return 0f;
    if (t >= 1f) return 1f;
    return 1f - (float) Math.exp(-damping * t) * (float) Math.cos(frequency * t);
  }
}
