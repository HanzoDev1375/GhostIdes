package ir.hanzodev1375.components.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import ir.hanzodev1375.components.R;
import androidx.annotation.ColorInt;
import androidx.annotation.FloatRange;
import androidx.annotation.IntRange;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.palette.graphics.Palette;
import ir.theme.M3Theme;

/**
 * SegmentedAvatarView draws a circular avatar clipped inside a bounding circle, surrounded by a
 * Material 3 style segmented ring made of rounded arc segments with a blue-to-purple sweep gradient
 * (Instagram/Telegram "story ring" style).
 *
 * <p>The view never overrides setImageDrawable / setImageBitmap / setImageResource, so image
 * loaders (Glide, Picasso, Coil) work exactly as they would with a plain ImageView - they just call
 * the standard ImageView setters, and this view picks up the new Drawable the next time it draws.
 *
 * <p>Drawing strategy: - The avatar is clipped to a circle using a BitmapShader (not
 * Canvas#clipPath), because clipPath does not anti-alias correctly on a hardware-accelerated
 * canvas. BitmapShader-based circles are fully hardware-accelerated and smooth. - The ring is drawn
 * with a stroked Paint + SweepGradient shader, split into arc segments with Paint.Cap.ROUND caps. -
 * RectF and Shader objects are cached and only rebuilt when the geometry or colors that affect them
 * actually change.
 */
public class SegmentedAvatarView extends ImageViewAnimator {

  // ---- Defaults --------------------------------------------------------

  private static final int DEFAULT_SEGMENT_COUNT = 8;
  private static final float DEFAULT_GAP_ANGLE_DEG = 14f;
  private static final float DEFAULT_RING_WIDTH_DP = 6f;
  private static final float DEFAULT_RING_PADDING_DP = 6f;
  private static final int DEFAULT_RING_ALPHA = 255;
  private static final float RING_START_ANGLE_DEG = -90f; // 12 o'clock

  // ---- Configurable properties ------------------------------------------

  private float ringWidth;
  private float ringPadding;
  private int segmentCount;
  private float gapAngle;
  @ColorInt private int startColor;
  @ColorInt private int endColor;
  private float gradientRotation;
  private int ringAlpha;
  private boolean ringVisible;
  private boolean paletteColorEnabled = true;
  private float progress = 1f;
  private float ringRadiusOverride = 0f; // 0 = auto-size from the view bounds

  // ---- Cached drawing state (avoid allocations inside onDraw) ------------

  private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint avatarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final RectF ringRect = new RectF();
  private final Matrix gradientMatrix = new Matrix();
  private final Matrix avatarMatrix = new Matrix();

  private SweepGradient sweepGradient;
  private boolean ringShaderDirty = true;

  private Drawable avatarSourceDrawable;
  private Bitmap avatarBitmap;
  private BitmapShader avatarShader;
  private boolean avatarGeometryDirty = true;

  private float centerX;
  private float centerY;
  private float avatarRadius;
  private float ringRadius;

  public SegmentedAvatarView(Context context) {
    this(context, null);
  }

  public SegmentedAvatarView(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public SegmentedAvatarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    readAttributes(context, attrs);
    setupPaints();
    M3Theme.apply(this);
  }

  // ---- Attribute parsing --------------------------------------------------

  private void readAttributes(Context context, @Nullable AttributeSet attrs) {
    float density = context.getResources().getDisplayMetrics().density;

    // Sensible defaults first, then let XML attributes override them.
    ringWidth = DEFAULT_RING_WIDTH_DP * density;
    ringPadding = DEFAULT_RING_PADDING_DP * density;
    segmentCount = DEFAULT_SEGMENT_COUNT;
    gapAngle = DEFAULT_GAP_ANGLE_DEG;
    startColor = fallback(M3Theme.tertiary(), Color.TRANSPARENT);
    endColor = fallback(M3Theme.tertiaryContainer(), Color.TRANSPARENT);
    gradientRotation = 0f;
    ringAlpha = DEFAULT_RING_ALPHA;
    ringVisible = true;

    if (attrs == null) {
      return;
    }

    TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SegmentedAvatarView);
    try {
      ringWidth = a.getDimension(R.styleable.SegmentedAvatarView_ringWidth, ringWidth);
      ringPadding = a.getDimension(R.styleable.SegmentedAvatarView_ringPadding, ringPadding);
      segmentCount = a.getInteger(R.styleable.SegmentedAvatarView_segmentCount, segmentCount);
      gapAngle = a.getFloat(R.styleable.SegmentedAvatarView_gapAngle, gapAngle);
      startColor = a.getColor(R.styleable.SegmentedAvatarView_startColor, startColor);
      endColor = a.getColor(R.styleable.SegmentedAvatarView_endColor, endColor);
      gradientRotation =
          a.getFloat(R.styleable.SegmentedAvatarView_gradientRotation, gradientRotation);
      ringAlpha = a.getInteger(R.styleable.SegmentedAvatarView_ringAlpha, ringAlpha);
      ringVisible = a.getBoolean(R.styleable.SegmentedAvatarView_ringVisible, ringVisible);
      progress = a.getFloat(R.styleable.SegmentedAvatarView_progress, progress);
      ringRadiusOverride =
          a.getDimension(R.styleable.SegmentedAvatarView_ringRadius, ringRadiusOverride);
      paletteColorEnabled =
          a.getBoolean(R.styleable.SegmentedAvatarView_usingcolorpalette, paletteColorEnabled);
    } finally {
      a.recycle();
    }

    if (segmentCount < 1) {
      segmentCount = 1;
    }
  }

  private void setupPaints() {
    ringPaint.setStyle(Paint.Style.STROKE);
    ringPaint.setStrokeCap(Paint.Cap.ROUND);
    ringPaint.setStrokeWidth(ringWidth);
    ringPaint.setAlpha(ringAlpha);

    avatarPaint.setStyle(Paint.Style.FILL);
    avatarPaint.setFilterBitmap(true);
    avatarPaint.setDither(true);
  }

  // ---- Layout / geometry ---------------------------------------------------

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    recalculateGeometry(w, h);
    ringShaderDirty = true;
    avatarGeometryDirty = true;
  }

  /**
   * Computes the center point, ring radius and avatar radius from the current view size so that the
   * ring always scales with the view and the avatar never overlaps it: avatarRadius leaves
   * ringWidth + ringPadding of room outside its edge before the ring's stroke begins.
   */
  private void recalculateGeometry(int w, int h) {
    centerX = w / 2f;
    centerY = h / 2f;

    float halfSize = Math.min(w, h) / 2f;
    ringRadius = ringRadiusOverride > 0f ? ringRadiusOverride : (halfSize - ringWidth / 2f);
    avatarRadius = ringRadius - ringWidth / 2f - ringPadding;
    if (avatarRadius < 0f) {
      avatarRadius = 0f;
    }

    ringRect.set(
        centerX - ringRadius, centerY - ringRadius, centerX + ringRadius, centerY + ringRadius);
  }

  // ---- Ring shader ------------------------------------------------------

  /**
   * Rebuilds the SweepGradient only when colors or geometry actually changed. A three-stop gradient
   * (start -> end -> start) is used instead of a plain two-color sweep so the color wraps smoothly
   * with no hard seam at the 0/360 degree boundary, regardless of where a segment gap lands.
   */
  private void rebuildRingShaderIfNeeded() {
    if (!ringShaderDirty) {
      return;
    }
    int[] colors = {startColor, endColor, startColor};
    sweepGradient = new SweepGradient(centerX, centerY, colors, null);
    ringPaint.setShader(sweepGradient);
    applyGradientRotation();
    ringShaderDirty = false;
  }

  private void applyGradientRotation() {
    if (sweepGradient == null) {
      return;
    }
    gradientMatrix.reset();
    gradientMatrix.postRotate(gradientRotation, centerX, centerY);
    sweepGradient.setLocalMatrix(gradientMatrix);
  }

  // ---- Avatar shader (circular clip, hardware-accelerated) ----------------

  private void drawAvatar(Canvas canvas) {
    Drawable drawable = getDrawable();
    if (drawable == null || avatarRadius <= 0f) {
      return;
    }

    // Only re-extract the bitmap and rebuild the shader when the Drawable
    // instance actually changed (e.g. a new image finished loading).
    if (drawable != avatarSourceDrawable) {
      avatarSourceDrawable = drawable;
      avatarBitmap = extractBitmap(drawable);
      avatarShader =
          avatarBitmap != null
              ? new BitmapShader(avatarBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
              : null;
      avatarPaint.setShader(avatarShader);
      avatarGeometryDirty = true;
      if (paletteColorEnabled && avatarBitmap != null) {
        paletteColor(avatarBitmap);
      }
    }

    if (avatarShader == null) {
      return;
    }

    if (avatarGeometryDirty) {
      updateAvatarMatrix();
      avatarGeometryDirty = false;
    }

    canvas.drawCircle(centerX, centerY, avatarRadius, avatarPaint);
  }

  /**
   * Positions and scales the source bitmap inside the avatar circle the same way ImageView's
   * centerCrop would: the shorter bitmap dimension fills the circle's bounding box and the overflow
   * is cropped equally on both sides.
   */
  private void updateAvatarMatrix() {
    float diameter = avatarRadius * 2f;
    float bitmapWidth = avatarBitmap.getWidth();
    float bitmapHeight = avatarBitmap.getHeight();
    float scale = Math.max(diameter / bitmapWidth, diameter / bitmapHeight);

    float dx = (centerX - avatarRadius) - (bitmapWidth * scale - diameter) * 0.5f;
    float dy = (centerY - avatarRadius) - (bitmapHeight * scale - diameter) * 0.5f;

    avatarMatrix.reset();
    avatarMatrix.setScale(scale, scale);
    avatarMatrix.postTranslate(dx, dy);
    avatarShader.setLocalMatrix(avatarMatrix);
  }

  private Bitmap extractBitmap(Drawable drawable) {
    if (drawable instanceof BitmapDrawable) {
      Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
      if (bitmap != null) {
        return bitmap;
      }
    }
    int width = Math.max(drawable.getIntrinsicWidth(), 1);
    int height = Math.max(drawable.getIntrinsicHeight(), 1);
    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
    drawable.setBounds(0, 0, width, height);
    drawable.draw(canvas);
    return bitmap;
  }

  private void paletteColor(Bitmap bitmap) {
    if (bitmap == null || bitmap.isRecycled()) {
      return;
    }
    Palette.from(bitmap)
        .generate(
            palette -> {
              if (palette == null || !paletteColorEnabled) {
                return;
              }
              startColor = pickStartSwatchColor(palette, startColor);
              endColor = pickEndSwatchColor(palette, endColor);
              ringShaderDirty = true;
              invalidate();
            });
  }

  private int pickStartSwatchColor(Palette palette, int fallback) {
    Palette.Swatch swatch = palette.getVibrantSwatch();
    if (swatch == null) {
      swatch = palette.getLightVibrantSwatch();
    }
    if (swatch == null) {
      swatch = palette.getDominantSwatch();
    }
    return swatch != null ? swatch.getRgb() : fallback;
  }

  private int pickEndSwatchColor(Palette palette, int fallback) {
    Palette.Swatch swatch = palette.getDarkVibrantSwatch();
    if (swatch == null) {
      swatch = palette.getMutedSwatch();
    }
    if (swatch == null) {
      swatch = palette.getDarkMutedSwatch();
    }
    return swatch != null ? swatch.getRgb() : fallback;
  }

  // ---- Drawing -------------------------------------------------------------

  @Override
  protected void onDraw(Canvas canvas) {
    // Avatar is drawn first; the ring is always drawn after and stays
    // outside the avatar radius, so it can never overlap the image.
    drawAvatar(canvas);

    if (ringVisible) {
      rebuildRingShaderIfNeeded();
      drawSegmentedRing(canvas);
    }
  }

  private void drawSegmentedRing(Canvas canvas) {
    ringPaint.setAlpha(ringAlpha);

    float anglePerSegment = 360f / segmentCount;
    float sweepAngle = anglePerSegment - gapAngle;
    if (sweepAngle <= 0f) {
      return;
    }

    float clampedProgress = Math.max(0f, Math.min(1f, progress));
    float totalSweep = 360f * clampedProgress;

    for (int i = 0; i < segmentCount; i++) {
      float segmentStart = i * anglePerSegment;
      if (segmentStart >= totalSweep) {
        break;
      }

      float remaining = totalSweep - segmentStart;
      float drawSweep = Math.min(sweepAngle, remaining);
      if (drawSweep <= 0f) {
        continue;
      }

      float angle = RING_START_ANGLE_DEG + segmentStart + gapAngle / 2f;
      canvas.drawArc(ringRect, angle, drawSweep, false, ringPaint);
    }
  }

  // ---- Public API: setters / getters ---------------------------------------

  /** Sets the ring's stroke width, in pixels. */
  public void setRingWidth(float ringWidthPx) {
    this.ringWidth = ringWidthPx;
    ringPaint.setStrokeWidth(ringWidthPx);
    recalculateGeometry(getWidth(), getHeight());
    avatarGeometryDirty = true;
    invalidate();
  }

  public float getRingWidth() {
    return ringWidth;
  }

  /** Sets the gap, in pixels, between the avatar's edge and the ring's inner edge. */
  public void setRingPadding(float ringPaddingPx) {
    this.ringPadding = ringPaddingPx;
    recalculateGeometry(getWidth(), getHeight());
    avatarGeometryDirty = true;
    invalidate();
  }

  public float getRingPadding() {
    return ringPadding;
  }

  /** Sets how many arc segments make up the ring. Minimum 1. */
  public void setSegmentCount(int segmentCount) {
    this.segmentCount = Math.max(1, segmentCount);
    invalidate();
  }

  public int getSegmentCount() {
    return segmentCount;
  }

  /** Sets the gap angle, in degrees, subtracted from each segment's sweep. */
  public void setGapAngle(float gapAngleDegrees) {
    this.gapAngle = gapAngleDegrees;
    invalidate();
  }

  public float getGapAngle() {
    return gapAngle;
  }

  /** Sets the sweep gradient's starting color. */
  public void setStartColor(@ColorInt int startColor) {
    this.startColor = startColor;
    ringShaderDirty = true;
    invalidate();
  }

  @ColorInt
  public int getStartColor() {
    return startColor;
  }

  /** Sets the sweep gradient's ending color. */
  public void setEndColor(@ColorInt int endColor) {
    this.endColor = endColor;
    ringShaderDirty = true;
    invalidate();
  }

  @ColorInt
  public int getEndColor() {
    return endColor;
  }

  /**
   * Enables or disables automatic ring colors extracted from the avatar image via androidx.palette.
   * Enabled by default; disable this before calling setStartColor / setEndColor to keep manual
   * colors from being overwritten.
   */
  public void setPaletteColorEnabled(boolean paletteColorEnabled) {
    this.paletteColorEnabled = paletteColorEnabled;
    if (paletteColorEnabled && avatarBitmap != null) {
      paletteColor(avatarBitmap);
    }
  }

  public boolean isPaletteColorEnabled() {
    return paletteColorEnabled;
  }

  /** Rotates the gradient (in degrees) around the ring without recreating the shader. */
  public void setGradientRotation(float degrees) {
    this.gradientRotation = degrees;
    applyGradientRotation();
    invalidate();
  }

  public float getGradientRotation() {
    return gradientRotation;
  }

  /** Sets the ring's overall opacity, 0-255. */
  public void setRingAlpha(@IntRange(from = 0, to = 255) int alpha) {
    this.ringAlpha = alpha;
    invalidate();
  }

  public int getRingAlpha() {
    return ringAlpha;
  }

  /** Shows or hides the ring entirely. */
  public void setRingVisible(boolean visible) {
    this.ringVisible = visible;
    invalidate();
  }

  public boolean isRingVisible() {
    return ringVisible;
  }

  /** Draws only a fraction of the ring, 0f (nothing) to 1f (full ring). */
  public void setProgress(@FloatRange(from = 0.0, to = 1.0) float progress) {
    this.progress = progress;
    invalidate();
  }

  public float getProgress() {
    return progress;
  }

  /**
   * Overrides the auto-computed ring radius, in pixels. Pass 0 to go back to automatic sizing based
   * on the view's bounds.
   */
  public void setRingRadius(float radiusPx) {
    this.ringRadiusOverride = radiusPx;
    recalculateGeometry(getWidth(), getHeight());
    ringShaderDirty = true;
    avatarGeometryDirty = true;
    invalidate();
  }

  /** Returns the ring radius currently in effect (auto or overridden), in pixels. */
  public float getRingRadius() {
    return ringRadius;
  }

  private static int fallback(Integer value, int def) {
    return value != null ? value : def;
  }
}
