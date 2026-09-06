package ir.hanzodev1375.components.animators;

import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;

public class CubicBezierInterpolator {

  public static final Interpolator DEFAULT = new PathInterpolator(0.42f, 0f, 0.58f, 1f);
  public static final Interpolator EASE_OUT = new PathInterpolator(0f, 0f, 0f, 1f);
  public static final Interpolator EASE_OUT_QUINT = new PathInterpolator(0.22f, 1f, 0.36f, 1f);

  public static Interpolator create(float x1, float y1, float x2, float y2) {
    return new PathInterpolator(x1, y1, x2, y2);
  }

  private CubicBezierInterpolator() {}
}
