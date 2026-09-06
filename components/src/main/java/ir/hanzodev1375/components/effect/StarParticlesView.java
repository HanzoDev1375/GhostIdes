package ir.hanzodev1375.components.effect;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.CornerPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import androidx.annotation.ColorInt;
import androidx.core.math.MathUtils;

import java.util.ArrayList;
import java.util.Random;

public class StarParticlesView extends View {

  private static final Random RANDOM = new Random();
  private final float density;
  private final float dt;
  private Drawable drawable;
  private boolean particlesEnabled = true;
  private int size;

  public StarParticlesView(Context context) {
    this(context, 75);
  }

  public StarParticlesView(Context context, int particlesCount) {
    super(context);
    density = context.getResources().getDisplayMetrics().density;
    dt = getRefreshRate(context, 60f);
    drawable = new Drawable(Math.max(1, particlesCount), density, dt);
    configure();
  }

  /** نرخ نوسان نمایشگر (فریم/ثانیه) — برای حرکت ذرات بدون توجه به رفرش‌ریت دستگاه. */
  public static float getRefreshRate(Context context, float fallback) {
    try {
      Display display =
          ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
      float rate = display.getRefreshRate();
      return rate > 0 ? rate : fallback;
    } catch (Throwable t) {
      return fallback;
    }
  }

  // ---- API ---------------------------------------------------------------

  /** تعداد ذرات ستاره (موتور ذرات دوباره ساخته می‌شود و موقعیت‌ها بازتولید می‌شوند). */
  public void setParticlesCount(int count) {
    drawable = new Drawable(Math.max(1, count), density, dt);
    configure();
    invalidate();
  }

  /** دو رنگ گرادیان ستاره‌ها (پیش‌فرض آبی→بنفش پریمیوم). */
  public void setParticleColors(@ColorInt int top, @ColorInt int bottom) {
    drawable.setGradient(top, bottom);
    invalidate();
  }

  /** رنگ ثابت ستاره‌ها (وقتی {@link #setUseGradient(boolean)} غیرفعال باشد استفاده می‌شود). */
  public void setColor(@ColorInt int color) {
    drawable.setColor(color);
    invalidate();
  }

  public void setUseGradient(boolean use) {
    drawable.setUseGradient(use);
    invalidate();
  }

  /** خاموش/روشن کردن کل ذرات (مثلاً حالت ذخیره انرژی — مشابه {@code LiteMode} تلگرام). */
  public void setParticlesEnabled(boolean enabled) {
    if (particlesEnabled != enabled) {
      particlesEnabled = enabled;
      invalidate();
    }
  }

  public boolean isParticlesEnabled() {
    return particlesEnabled;
  }

  /**
   * فعال کردن «انفجار از مرکز»: ذرات از وسط پخش میشن. تغییر حالت یعنی موقعیت‌ها دوباره ساخته میشن.
   */
  public void setStartFromCenter(boolean fromCenter) {
    drawable.setStartFromCenter(fromCenter);
    invalidate();
  }

  public boolean isStartFromCenter() {
    return drawable.startFromCenter;
  }

  /** بازتولید موقعیت همه‌ی ذرات (بعد از تغییر اندازه/محدوده). */
  public void resetPositions() {
    drawable.resetPositions();
  }

  /** توقف موقت حرکت (ذرات سر جاشون میمونن — مناسب زمانی که ویو گم می‌شه). */
  public void setPaused(boolean paused) {
    drawable.setPaused(paused);
  }

  public boolean isPaused() {
    return drawable.paused;
  }

  /**
   * شتاب ناگهانی ذرات بر اساس شدت حرکت (مثل تلگرام که موقع swipe شدت وزش رو زیاد می‌کنه).
   *
   * @param velocitySum جمع سرعت؛ هر چی بیشتر، ذرات تندتر پخش میشن.
   */
  public void flingParticles(float velocitySum) {
    drawable.flingParticles(velocitySum);
  }

  public void setFlingDisabled(boolean disabled) {
    drawable.flingDisabled = disabled;
  }

  public Drawable getDrawableLayer() {
    return drawable;
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    int sizeInternal = (getMeasuredWidth() << 16) + getMeasuredHeight();
    drawable.rect.set(0, 0, getStarsRectWidth(), dp(140));
    drawable.rect.offset(
        (getMeasuredWidth() - drawable.rect.width()) / 2,
        (getMeasuredHeight() - drawable.rect.height()) / 2);
    drawable.rect2.set(-dp(15), -dp(15), getMeasuredWidth() + dp(15), getMeasuredHeight() + dp(15));
    if (size != sizeInternal) {
      size = sizeInternal;
      drawable.resetPositions();
    }
  }

  protected int getStarsRectWidth() {
    return dp(140);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    if (!particlesEnabled) {
      return;
    }
    drawable.onDraw(canvas);
    if (!drawable.paused) {
      invalidate();
    }
  }

  private int dp(float value) {
    return (int) Math.ceil(value * density);
  }

  private float dpf2(float value) {
    return value * density;
  }

  private void configure() {
    drawable.type = 100;
    drawable.roundEffect = true;
    drawable.useRotate = true;
    drawable.useBlur = true;
    drawable.checkBounds = true;
    drawable.size1 = 4;
    drawable.k1 = drawable.k2 = drawable.k3 = 0.98f;
    drawable.init();
  }

  private static float clamp(float value, float min, float max) {
    return Math.max(min, Math.min(max, value));
  }

  private static float clamp01(float value) {
    return clamp(value, 0f, 1f);
  }

  /**
   * موتور خالص ذرات ستاره — فقط {@link Canvas} می‌خواد و مستقل از View؛ برای تعبیه در ویوهای مشتق
   * (مثل {@code SegmentedAvatarView}) هم مناسبه. محدوده‌ی رندر با فیلد {@link #rect} تنظیم می‌شه و
   * هر فریم با {@link #onDraw(Canvas, float)} کشیده می‌شه.
   */
  public static class Drawable {

    public RectF rect = new RectF();
    public RectF rect2 = new RectF();
    public RectF excludeRect = new RectF();

    public boolean paused;
    public boolean startFromCenter;
    public boolean isCircle = true;
    public boolean checkTime = true;
    public boolean checkBounds = false;
    public boolean useBlur = false;
    public boolean useScale;
    public float speedScale = 1f;
    public boolean flingDisabled;
    public float centerOffsetX = 0, centerOffsetY = 0;
    public float excludeRadius = 0;
    public long minLifeTime = 2000;
    public int randLifeTime = 1000;

    /** این آلفای «محوشدگی در پایان عمر» همیشه پیش می‌رود؛ اعمال خارجی alpha را multiply می‌کند. */
    public int size1 = 14, size2 = 12, size3 = 10;

    public float k1 = 0.85f, k2 = 0.85f, k3 = 0.9f;
    public boolean roundEffect = true;
    public boolean useRotate;
    public int type = 100;

    public final int count;
    public final float density;
    public final float dt;

    public Paint paint = new Paint();

    private int starColor = 0xFFB2B7FF;
    private int gradientTop = 0xFF6FD5FF;
    private int gradientBottom = 0xFF8E4EFF;
    private boolean useGradient = true;

    private final ArrayList<Particle> particles = new ArrayList<>();
    private Bitmap[] stars = new Bitmap[3];
    private Matrix[] matrices;
    private float[][] points;
    private int[] pointsCount;
    private float[] rotationAngles;

    private final OvershootInterpolator overshootInterpolator = new OvershootInterpolator(2.0f);
    private long prevTime;
    private int lastParticleI;
    private long pausedTime;

    public Drawable(int count, float density, float dt) {
      this.count = count;
      this.density = density;
      this.dt = dt;
    }

    // ---- Colors --------------------------------------------------------

    public void setColor(@ColorInt int color) {
      starColor = color;
      generateBitmaps();
    }

    public void setGradient(@ColorInt int top, @ColorInt int bottom) {
      gradientTop = top;
      gradientBottom = bottom;
      useGradient = true;
      generateBitmaps();
    }

    public void setUseGradient(boolean use) {
      useGradient = use;
      generateBitmaps();
    }

    // ---- Lifecycle -----------------------------------------------------

    public void init() {
      generateBitmaps();
      if (useRotate) {
        initRotationArrays();
      }
      if (particles.isEmpty()) {
        for (int i = 0; i < count; i++) {
          particles.add(new Particle());
        }
      }
    }

    private void initRotationArrays() {
      final int n = stars.length;
      matrices = new Matrix[n];
      points = new float[n][];
      pointsCount = new int[n];
      rotationAngles = new float[n];
      for (int i = 0; i < n; i++) {
        matrices[i] = new Matrix();
        points[i] = new float[Math.max(1, count) * 2];
      }
    }

    public void resetPositions() {
      long time = System.currentTimeMillis();
      for (int i = 0; i < particles.size(); i++) {
        particles.get(i).genPosition(time);
      }
    }

    public void setStartFromCenter(boolean fromCenter) {
      if (startFromCenter != fromCenter) {
        startFromCenter = fromCenter;
        resetPositions();
      }
    }

    public void setPaused(boolean paused) {
      if (this.paused == paused) {
        return;
      }
      this.paused = paused;
      if (paused) {
        pausedTime = System.currentTimeMillis();
      } else {
        long now = System.currentTimeMillis();
        for (int i = 0; i < particles.size(); i++) {
          particles.get(i).lifeTime += now - pausedTime;
        }
      }
    }

    /** شتاب وزش ذرات (burst) — بعد از ۲٫۶ ثانیه به حالت عادی برمی‌گردد. */
    public void flingParticles(float velocitySum) {
      if (flingDisabled) {
        return;
      }
      float maxSpeed = 15f;
      if (velocitySum < 60) {
        maxSpeed = 5f;
      } else if (velocitySum < 180) {
        maxSpeed = 9f;
      }
      final float target = maxSpeed;
      AnimatorSet animatorSet = new AnimatorSet();
      ValueAnimator.AnimatorUpdateListener updateListener =
          animation -> speedScale = (float) animation.getAnimatedValue();

      ValueAnimator a1 = ValueAnimator.ofFloat(1f, target);
      a1.addUpdateListener(updateListener);
      a1.setDuration(600);

      ValueAnimator a2 = ValueAnimator.ofFloat(target, 1f);
      a2.addUpdateListener(updateListener);
      a2.setDuration(2000);

      animatorSet.playTogether(a1, a2);
      animatorSet.start();
    }

    // ---- Rendering -----------------------------------------------------

    public void onDraw(Canvas canvas) {
      onDraw(canvas, 1f);
    }

    public void onDraw(Canvas canvas, float alpha) {
      long time = System.currentTimeMillis();
      long diff = MathUtils.clamp(time - prevTime, 4, 50);
      if (useRotate) {
        final float cx = rect.centerX() + centerOffsetX;
        final float cy = rect.centerY() + centerOffsetY;
        for (int i = 0; i < matrices.length; i++) {
          rotationAngles[i] += 360f * (diff / (40000f + i * 10000f));
          matrices[i].setRotate(rotationAngles[i], cx, cy);
          pointsCount[i] = 0;
        }
        for (int i = 0; i < particles.size(); i++) {
          particles.get(i).updatePoint();
        }
        for (int i = 0; i < matrices.length; i++) {
          matrices[i].mapPoints(points[i], 0, points[i], 0, pointsCount[i]);
          pointsCount[i] = 0;
        }
      }

      for (int i = 0; i < particles.size(); i++) {
        Particle particle = particles.get(i);
        if (paused) {
          particle.draw(canvas, pausedTime, alpha);
        } else {
          particle.draw(canvas, time, alpha);
        }
        if (checkTime) {
          if (time > particle.lifeTime) {
            particle.genPosition(time);
          }
        }
        if (checkBounds) {
          if (!rect2.contains(particle.drawingX, particle.drawingY)) {
            particle.genPosition(time);
          }
        }
      }
      prevTime = time;
    }

    // ---- Internals -----------------------------------------------------

    private void generateBitmaps() {
      final int n = stars.length;
      for (int i = 0; i < n; i++) {
        int s;
        float k = k1;
        if (i == 0) {
          s = dp(size1);
        } else if (i == 1) {
          k = k2;
          s = dp(size2);
        } else {
          k = k3;
          s = dp(size3);
        }
        s = Math.max(2, s);

        if (stars[i] != null && stars[i].getWidth() == s && stars[i].getHeight() == s) {
          continue;
        }

        stars[i] = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(stars[i]);

        Path path = new Path();
        int sizeHalf = s >> 1;
        int mid = (int) (sizeHalf * k);
        path.moveTo(0, sizeHalf);
        path.lineTo(mid, mid);
        path.lineTo(sizeHalf, 0);
        path.lineTo(s - mid, mid);
        path.lineTo(s, sizeHalf);
        path.lineTo(s - mid, s - mid);
        path.lineTo(sizeHalf, s);
        path.lineTo(mid, s - mid);
        path.lineTo(0, sizeHalf);
        path.close();

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        if (useGradient) {
          p.setShader(
              new LinearGradient(0, 0, s, s, gradientTop, gradientBottom, Shader.TileMode.CLAMP));
        } else {
          p.setColor(starColor);
        }
        if (roundEffect) {
          p.setPathEffect(new CornerPathEffect(dpf2(size1 / 5f)));
        }
        p.setAlpha(useBlur ? 60 : 120);
        canvas.drawPath(path, p);
        p.setPathEffect(null);
        p.setAlpha(255);
        if (useBlur) {
          // شبیه‌سازی نرمی (blur) با آلفای پایین؛ نیازی به stackBlur سنگین تلگرام نیست چون
          // برای ذرات کوچک تفاوت محسوسی نداره و فقط سرعتِ فریم رو کم می‌کنه.
          stars[i] = stars[i].extractAlpha();
        }
      }
    }

    private int dp(float value) {
      return (int) Math.ceil(value * density);
    }

    private float dpf2(float value) {
      return value * density;
    }

    private class Particle {
      public long lifeTime;
      private final int i;
      private float scale = 1f;
      private float x, y;
      private float x2, y2;
      private float drawingX, drawingY;
      private float vecX, vecY;
      private int starIndex;
      private int alpha;
      private float randomRotate;
      private float inProgress;

      Particle() {
        i = lastParticleI++;
      }

      void updatePoint() {
        final int c = pointsCount[starIndex];
        points[starIndex][2 * c] = x;
        points[starIndex][2 * c + 1] = y;
        pointsCount[starIndex]++;
      }

      void draw(Canvas canvas, long time, float alpha) {
        if (useRotate) {
          final int c = pointsCount[starIndex];
          drawingX = points[starIndex][2 * c];
          drawingY = points[starIndex][2 * c + 1];
          pointsCount[starIndex]++;
        } else {
          drawingX = x;
          drawingY = y;
        }
        boolean skipDraw = false;
        if (!excludeRect.isEmpty() && excludeRect.contains(drawingX, drawingY)) {
          skipDraw = true;
        }
        if (!skipDraw) {
          canvas.save();
          canvas.translate(drawingX, drawingY);
          if (randomRotate != 0) {
            canvas.rotate(
                randomRotate, stars[starIndex].getWidth() / 2f, stars[starIndex].getHeight() / 2f);
          }
          float outProgress = 0f;
          if (checkTime && lifeTime - time < 200) {
            outProgress = 1f - (lifeTime - time) / 150f;
            outProgress = clamp01(outProgress);
          }
          if (inProgress < 1f) {
            canvas.scale(
                overshootInterpolator.getInterpolation(inProgress),
                overshootInterpolator.getInterpolation(inProgress),
                0,
                0);
          }
          Paint p = Drawable.this.paint;
          p.setAlpha((int) (this.alpha * (1f - outProgress) * alpha));
          final Bitmap bitmap = stars[starIndex];
          if (useScale) {
            canvas.scale(
                scale * (1f - outProgress) * alpha * inProgress,
                scale * (1f - outProgress) * alpha * inProgress);
          }
          canvas.drawBitmap(bitmap, -(bitmap.getWidth() >> 1), -(bitmap.getHeight() >> 1), p);
          canvas.restore();
        }
        if (!paused) {
          float speed = dp(4) * (dt / 660f);
          speed *= speedScale;
          x += vecX * speed;
          y += vecY * speed;
          if (inProgress != 1f) {
            inProgress += dt / 200;
            if (inProgress > 1f) {
              inProgress = 1f;
            }
          }
        }
      }

      void genPosition(long time) {
        starIndex = Math.abs(RANDOM.nextInt() % stars.length);
        lifeTime = time + minLifeTime + RANDOM.nextInt(randLifeTime);
        randomRotate = 0;
        if (useScale) {
          scale = .4f + .6f * RANDOM.nextFloat();
        }

        if (isCircle) {
          float r =
              (Math.abs(RANDOM.nextInt() % 1000) / 1000f) * (rect.width() - excludeRadius)
                  + excludeRadius;
          float a = Math.abs(RANDOM.nextInt() % 360);
          x = rect.centerX() + centerOffsetX + (float) (r * Math.sin(Math.toRadians(a)));
          y = rect.centerY() + centerOffsetY + (float) (r * Math.cos(Math.toRadians(a)));
        } else {
          x = rect.left + Math.abs(RANDOM.nextInt() % Math.max(1, (int) rect.width()));
          y = rect.top + Math.abs(RANDOM.nextInt() % Math.max(1, (int) rect.height()));
        }

        double a;
        if (startFromCenter) {
          a = RANDOM.nextDouble() * Math.PI * 2.0;
        } else {
          a =
              Math.atan2(
                  y - (rect.centerY() + centerOffsetY), x - (rect.centerX() + centerOffsetX));
        }
        vecX = (float) Math.cos(a);
        vecY = (float) Math.sin(a);
        alpha = (int) (255 * ((50 + RANDOM.nextInt(50)) / 100f));
        randomRotate = (int) (45 * ((RANDOM.nextInt() % 100) / 100f));
        inProgress = 0;
        if (startFromCenter) {
          final float rr =
              (.6f + 1.2f * RANDOM.nextFloat()) * Math.min(rect.width(), rect.height()) / 2f;
          x2 = x = rect.centerX() + centerOffsetX + (float) Math.cos(a) * rr;
          y2 = y = rect.centerY() + centerOffsetY + (float) Math.sin(a) * rr;
        }
      }
    }
  }
}
