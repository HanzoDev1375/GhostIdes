package ir.hanzodev1375.components.views;

import android.content.Context;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import ir.hanzodev1375.components.R;
import ir.theme.M3Theme;
import android.view.animation.AccelerateDecelerateInterpolator;
import androidx.appcompat.widget.AppCompatImageView;
import ir.hanzodev1375.components.animators.AnimationManager;

public class ImageViewAnimator extends AppCompatImageView implements View.OnTouchListener {

  private static final float SCALE = 0.88f;
  private static final long DURATION = 120L;
  

  public ImageViewAnimator(Context context) {
    super(context);
    init();
  }

  public ImageViewAnimator(Context context, AttributeSet attrs) {
    super(context, attrs);

    init();
  }

  public ImageViewAnimator(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    init();
  }

  private void init() {
    setOnTouchListener(this);
    setClickable(true);
    M3Theme.apply(this);
  }

  @Override
  public boolean onTouch(View v, MotionEvent event) {
    switch (event.getAction()) {
      case MotionEvent.ACTION_DOWN:
        animateTo(SCALE);
        setColorFilter(
            fallback(M3Theme.primary(), 0),
            PorterDuff.Mode.SRC_IN);
        break;
      case MotionEvent.ACTION_UP:
      case MotionEvent.ACTION_CANCEL:
        clearColorFilter();
        animateTo(1.0f);
        performClick();
        break;
    }
    return true;
  }

  private void animateTo(float target) {
    clearAnimation();
    if (AnimationManager.getInstance(getContext()).areAnimationsEnabled()) {
      animate()
          .scaleX(target)
          .scaleY(target)
          .setDuration(DURATION)
          .setInterpolator(new AccelerateDecelerateInterpolator())
          .start();
    } else {
      setScaleX(target);
      setScaleY(target);
    }
  }

  private static int fallback(Integer value, int def) {
    return value != null ? value : def;
  }
}