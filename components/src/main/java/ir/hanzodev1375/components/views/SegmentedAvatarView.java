package ir.hanzodev1375.components.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.animation.OvershootInterpolator;
import ir.hanzodev1375.components.R;
import ir.hanzodev1375.components.colors.AccentPalette;
import ir.hanzodev1375.components.effect.StarParticlesView;
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

  // ---- Premium login star + particles --------------------------------------

  private static final int DEFAULT_BADGE_GRADIENT_TOP = 0xFF6FD5FF;
  private static final int DEFAULT_BADGE_GRADIENT_BOTTOM = 0xFF8E4EFF;

  /** مبنای طراحی حلقه/ذرات؛ وقتی رنگ آواتار «اکسنت» می‌شه این دو استاپ به سمتش شیفت می‌خورن. */
  private static final int BASE_RING_START = DEFAULT_BADGE_GRADIENT_TOP;

  private static final int BASE_RING_END = DEFAULT_BADGE_GRADIENT_BOTTOM;
  private static final int LOGIN_PARTICLE_COUNT = 70;

  private int badgeGradientTop = DEFAULT_BADGE_GRADIENT_TOP;
  private int badgeGradientBottom = DEFAULT_BADGE_GRADIENT_BOTTOM;

  private StarParticlesView.Drawable starParticles;
  private boolean loggedIn;
  private float loginFxAlpha;
  private ValueAnimator loginFxAnimator;
  private final OvershootInterpolator badgePopInterpolator = new OvershootInterpolator(3.5f);

  private final Path badgeStarPath = new Path();
  private final Paint badgeHaloPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint badgeStarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private LinearGradient badgeStarGradient;
  private float badgeX;
  private float badgeY;
  private float badgeRadius;

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
    setupLoginFx(context);
  }

  private void setupLoginFx(Context context) {
    float density = context.getResources().getDisplayMetrics().density;
    starParticles =
        new StarParticlesView.Drawable(
            LOGIN_PARTICLE_COUNT, density, StarParticlesView.getRefreshRate(context, 60f));
    starParticles.isCircle = true;
    starParticles.useRotate = true;
    starParticles.useBlur = true;
    starParticles.roundEffect = true;
    starParticles.checkBounds = true;
    starParticles.minLifeTime = 1800;
    starParticles.randLifeTime = 700;
    starParticles.setStartFromCenter(true);
    starParticles.setGradient(badgeGradientTop, badgeGradientBottom);
    starParticles.init();
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

    updateLoginFxGeometry();
  }

  /**
   * Places the premium star badge on the avatar's bottom-right edge (on the 45° line, straddling the
   * avatar circle like Telegram) and sizes the particle spawn area around the badge itself.
   */
  private void updateLoginFxGeometry() {
    if (starParticles == null || avatarRadius <= 0f) {
      return;
    }
    badgeRadius = avatarRadius * 0.48f;
    badgeStarGradient = null;
    float cos45 = 0.7071067812f;
    float d = avatarRadius * 0.97f;
    badgeX = centerX + d * cos45;
    badgeY = centerY + d * cos45;

    float half = badgeRadius * 1.6f;
    starParticles.rect.set(badgeX - half, badgeY - half, badgeX + half, badgeY + half);
    starParticles.rect2.set(-half, -half, getWidth() + half, getHeight() + half);
    starParticles.excludeRect.setEmpty();
    if (loggedIn) {
      starParticles.resetPositions();
    }
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
              int accent = pickStartSwatchColor(palette, 0);
              if (accent != 0) {
                applyAvatarAccent(accent);
              }
            });
  }

  /**
   * رنگ اصلی (Swatch خوشه‌ای) آواتار → «اکسنت»: حلقه و گرادیان ستاره/ذرات با AccentPalette به سمت
   * hue اکسنت شیفت می‌خورن.
   */
  private void applyAvatarAccent(int accent) {
    boolean isDark = isBackgroundDark();
    int newStart =
        AccentPalette.changeAccent(
            BASE_RING_START, accent, BASE_RING_START, isDark, startColor);
    int newEnd = AccentPalette.recolor(accent, BASE_RING_END);

    // Keep the ring and the star/particle effect bright: if the avatar palette resolves to a very
    // dark colour the particles would otherwise render as black smudges. Fall back to the default
    // premium gradient instead.
    if (AccentPalette.perceivedBrightness(newStart) < 0.30f) {
      newStart = BASE_RING_START;
    }
    if (AccentPalette.perceivedBrightness(newEnd) < 0.30f) {
      newEnd = BASE_RING_END;
    }

    badgeGradientTop = newStart;
    badgeGradientBottom = newEnd;
    if (starParticles != null) {
      starParticles.setGradient(badgeGradientTop, badgeGradientBottom);
    }
    startColor = newStart;
    endColor = newEnd;
    ringShaderDirty = true;
    badgeStarGradient = null;
    invalidate();
  }

  /** بک‌گراند فعلی تم (سرفیسِ M3)؛ روی این رنگ آواتار/حلقه رسم می‌شود. */
  private boolean isBackgroundDark() {
    Integer bg = M3Theme.surfaceContainer();
    int backgroundColor = bg != null ? bg : 0xFFFFFFFF;
    return AccentPalette.perceivedBrightness(backgroundColor) < 0.5f;
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

    drawLoginFx(canvas);
  }

  // ---- Premium login star + particles --------------------------------------

  /** Draws the star badge (bottom-right of the avatar) and the particles around it. */
  private void drawLoginFx(Canvas canvas) {
    if (starParticles == null || loginFxAlpha <= 0.01f) {
      return;
    }
    float alpha = Math.min(1f, loginFxAlpha);
    float scale = badgePopInterpolator.getInterpolation(alpha);

    int save = canvas.save();
    canvas.translate(badgeX, badgeY);
    canvas.scale(scale, scale, 0f, 0f);
    drawStarBadge(canvas, alpha);
    canvas.restoreToCount(save);

    starParticles.onDraw(canvas, alpha);

    if (!starParticles.paused) {
      invalidate();
    }
  }

  /** Star draw at origin: soft subtle glow and a gradient 4-pointed premium star (no white disc). */
  private void drawStarBadge(Canvas canvas, float alpha) {
    badgeHaloPaint.setColor(badgeGradientBottom);
    badgeHaloPaint.setAlpha((int) (60 * alpha));
    canvas.drawCircle(0f, 0f, badgeRadius * 1.12f, badgeHaloPaint);

    if (badgeStarGradient == null) {
      badgeStarGradient =
          new LinearGradient(
              0f,
              -badgeRadius * 0.9f,
              0f,
              badgeRadius * 0.9f,
              badgeGradientTop,
              badgeGradientBottom,
              Shader.TileMode.CLAMP);
      badgeStarPaint.setShader(badgeStarGradient);
    }
    badgeStarPaint.setAlpha((int) (255 * alpha));
    buildStarPath(badgeStarPath, 0f, 0f, badgeRadius * 0.9f, badgeRadius * 0.42f, 4);
    canvas.drawPath(badgeStarPath, badgeStarPaint);
  }

  private void buildStarPath(
      Path path, float cx, float cy, float outerR, float innerR, int points) {
    path.reset();
    double step = Math.PI / points;
    for (int i = 0; i < points * 2; i++) {
      double r = (i % 2 == 0) ? outerR : innerR;
      double angle = -Math.PI / 2 + i * step;
      float x = (float) (cx + Math.cos(angle) * r);
      float y = (float) (cy + Math.sin(angle) * r);
      if (i == 0) {
        path.moveTo(x, y);
      } else {
        path.lineTo(x, y);
      }
    }
    path.close();
  }

  /**
   * Shows the premium star + particles when the user is logged in (small burst from the badge) and
   * fades them away when logged out.
   */
  public void setLoggedIn(boolean loggedIn) {
    if (this.loggedIn == loggedIn) {
      return;
    }
    this.loggedIn = loggedIn;

    if (loginFxAnimator != null) {
      loginFxAnimator.cancel();
    }

    if (loggedIn) {
      starParticles.setPaused(false);
      starParticles.setStartFromCenter(true);
      updateLoginFxGeometry();
      loginFxAlpha = 0f;
    }

    loginFxAnimator = ValueAnimator.ofFloat(0f, 1f);
    loginFxAnimator.setDuration(loggedIn ? 650 : 350);
    loginFxAnimator.setInterpolator(
        loggedIn ? new OvershootInterpolator(1.4f) : new OvershootInterpolator(1f));
    loginFxAnimator.addUpdateListener(
        animation -> {
          float t = (float) animation.getAnimatedValue();
          loginFxAlpha = loggedIn ? t : (1f - t);
          invalidate();
        });
    loginFxAnimator.addListener(
        new AnimatorListenerAdapter() {
          @Override
          public void onAnimationEnd(Animator animation) {
            loginFxAlpha = loggedIn ? 1f : 0f;
            // Stop the particle render loop once the entrance/exit animation has settled so the
            // view does not invalidate continuously while just sitting there. The static badge
            // star is still drawn via loginFxAlpha.
            starParticles.setPaused(true);
            invalidate();
          }
        });
    loginFxAnimator.start();
    invalidate();
  }

  public boolean isLoggedIn() {
    return loggedIn;
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    if (starParticles != null && loginFxAlpha > 0.01f && loginFxAnimator != null
        && loginFxAnimator.isRunning()) {
      starParticles.setPaused(false);
    }
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    if (starParticles != null) {
      starParticles.setPaused(true);
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
