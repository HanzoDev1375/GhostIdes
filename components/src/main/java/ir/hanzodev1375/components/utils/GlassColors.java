package ir.hanzodev1375.components.utils;

import android.view.View;
import androidx.core.graphics.ColorUtils;
import com.google.android.material.card.MaterialCardView;

/** Helps making material backgrounds semi-transparent so the glass surface shows through. */
public final class GlassColors {

  private GlassColors() {}

  /** Set <code>view</code> background to <code>color</code> with the given alpha (0..255). */
  public static void setBackgroundAlpha(View view, int color, int alpha) {
    int translucent = ColorUtils.setAlphaComponent(color, alpha);
    if (view instanceof MaterialCardView) {
      ((MaterialCardView) view).setCardBackgroundColor(translucent);
    } else {
      view.setBackgroundColor(translucent);
    }
  }
}