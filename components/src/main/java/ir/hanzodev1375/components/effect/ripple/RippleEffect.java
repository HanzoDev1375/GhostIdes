package ir.hanzodev1375.components.effect.ripple;

import android.view.View;

public abstract class RippleEffect {

  public final View view;

  public RippleEffect(View view) {
    this.view = view;
  }

  public void animate(float cx, float cy, float intensity) {}
}
