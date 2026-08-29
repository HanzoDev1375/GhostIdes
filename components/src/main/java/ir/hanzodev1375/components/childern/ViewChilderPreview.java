package ir.hanzodev1375.components.childern;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A {@link ViewChilder} that renders the media background with rounded corners and a stroke,
 * matching the rest of the themed UI (every corner in the app is rounded and stroked).
 *
 * <p>The rounding is applied by clipping the drawing in {@link #dispatchDraw(Canvas)} and by
 * drawing a rounded stroke in {@link #onDraw(Canvas)}, so this works with any media child
 * (image / gif / video) regardless of its own background.
 *
 * <p>Unlike the plain {@link ViewChilder}, this container also exposes {@link #applyBlur(float)}
 * which re-applies the currently shown media with a new blur size on the fly, so the background
 * updates without leaving the activity.
 */
public class ViewChilderPreview extends ViewChilder {

  private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

  private float cornerRadius = 0f;
  private float strokeWidth = 0f;

  @Nullable private Path clipPath;

  public ViewChilderPreview(@NonNull Context context) {
    super(context);
  }

  public ViewChilderPreview(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  /** Applies rounded corners with a fill and stroke, then clips children to the rounded shape. */
  public void applyStyle(float cornerRadius, int fillColor, int strokeColor, float strokeWidth) {
    this.cornerRadius = cornerRadius;
    this.strokeWidth = strokeWidth;
    strokePaint.setStyle(Paint.Style.STROKE);
    strokePaint.setStrokeWidth(strokeWidth);
    strokePaint.setColor(strokeColor);
    setBackgroundColor(fillColor);
    invalidate();
  }

  /** Re-applies the current media with a new blur size without leaving the activity. */
  public void applyBlur(float blurSize) {
    String path = getCurrentPath();
    if (path == null || path.trim().isEmpty()) {
      return;
    }
    reload(path, blurSize);
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    if (cornerRadius > 0f && w > 0 && h > 0) {
      clipPath = new Path();
      clipPath.addRoundRect(
          new RectF(0, 0, w, h), cornerRadius, cornerRadius, Path.Direction.CW);
    } else {
      clipPath = null;
    }
  }

  @Override
  protected void dispatchDraw(Canvas canvas) {
    if (clipPath != null) {
      int save = canvas.save();
      canvas.clipPath(clipPath);
      super.dispatchDraw(canvas);
      canvas.restoreToCount(save);
    } else {
      super.dispatchDraw(canvas);
    }
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    if (clipPath != null && strokePaint.getStrokeWidth() > 0f) {
      canvas.drawPath(clipPath, strokePaint);
    }
  }
}
