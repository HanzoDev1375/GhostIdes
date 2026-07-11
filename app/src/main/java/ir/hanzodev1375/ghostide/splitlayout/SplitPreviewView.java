package ir.hanzodev1375.ghostide.splitlayout;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.PathInterpolator;
import ir.theme.ThemeManager;
import ir.theme.ThemeUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * این کلاس با کمک کلود ساختیم چون نیاز به انیمیشن مستقیم در خود View داریم همچین رنگ با کد ادیتور
 * هماهنگ بشه چون فقط در ادیتور اکتویتی کال میشه نویسنده : گوست جوون
 */
public class SplitPreviewView extends View {

  private static final float GAP_DP = 4f;
  private static final float CORNER_DP = 4f;
  private static final long ANIM_DURATION_MS = 300L;
  private static final PathInterpolator SMOOTH_INTERPOLATOR =
      new PathInterpolator(0.22f, 1f, 0.36f, 1f);
  private ThemeManager theme;

  private int rows = 1;
  private int cols = 1;
  private final Paint cellPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private List<RectF> fromCells = new ArrayList<>();
  private List<RectF> toCells = new ArrayList<>();
  private float progress = 1f;
  private ValueAnimator morphAnimator;

  public SplitPreviewView(Context context) {
    super(context);
    init();
  }

  public SplitPreviewView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  public SplitPreviewView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init();
  }

  private void init() {
    theme = new ThemeManager(getContext());
    var themeUtil = new ThemeUtils(theme); // inject thememanager
    var get = themeUtil.getTheme().getEditor();
    if (get == null) {
      return;
    }

    int fill = Color.parseColor(get.getAttributeName());
    int stroke = Color.parseColor(get.getAttributeValue());
    cellPaint.setStyle(Paint.Style.FILL);
    cellPaint.setColor(fill);
    strokePaint.setStyle(Paint.Style.STROKE);
    strokePaint.setColor(stroke);
    strokePaint.setStrokeWidth(dp(1.5f));
  }

  public void setGrid(int newRows, int newCols) {
    newRows = Math.max(1, newRows);
    newCols = Math.max(1, newCols);
    if (newRows == rows && newCols == cols) return;

    if (getWidth() <= 0 || getHeight() <= 0) {

      rows = newRows;
      cols = newCols;
      invalidate();
      return;
    }

    List<RectF> currentCells = currentInterpolatedCells();

    rows = newRows;
    cols = newCols;
    fromCells = currentCells;
    toCells = buildCells(rows, cols, getWidth(), getHeight());

    if (morphAnimator != null) morphAnimator.cancel();
    progress = 0f;
    morphAnimator = ValueAnimator.ofFloat(0f, 1f);
    morphAnimator.setDuration(ANIM_DURATION_MS);
    morphAnimator.setInterpolator(SMOOTH_INTERPOLATOR);
    morphAnimator.addUpdateListener(
        anim -> {
          progress = (float) anim.getAnimatedValue();
          invalidate();
        });
    morphAnimator.start();
  }

  private List<RectF> buildCells(int r, int c, int w, int h) {
    List<RectF> cells = new ArrayList<>(r * c);
    float gap = dp(GAP_DP);
    float cellW = (w - gap * (c - 1)) / (float) c;
    float cellH = (h - gap * (r - 1)) / (float) r;
    for (int row = 0; row < r; row++) {
      for (int col = 0; col < c; col++) {
        float left = col * (cellW + gap);
        float top = row * (cellH + gap);
        cells.add(new RectF(left, top, left + cellW, top + cellH));
      }
    }
    return cells;
  }

  private List<RectF> currentInterpolatedCells() {
    if (fromCells.isEmpty() && toCells.isEmpty()) {
      return buildCells(rows, cols, getWidth(), getHeight());
    }
    int maxCount = Math.max(fromCells.size(), toCells.size());
    List<RectF> result = new ArrayList<>(maxCount);
    for (int i = 0; i < maxCount; i++) {
      result.add(lerpForIndex(i, progress));
    }
    return result;
  }

  private RectF lerpForIndex(int index, float t) {
    RectF from = index < fromCells.size() ? fromCells.get(index) : collapsedPoint(toCells, index);
    RectF to = index < toCells.size() ? toCells.get(index) : collapsedPoint(fromCells, index);
    return new RectF(
        lerp(from.left, to.left, t),
        lerp(from.top, to.top, t),
        lerp(from.right, to.right, t),
        lerp(from.bottom, to.bottom, t));
  }

  private RectF collapsedPoint(List<RectF> reference, int index) {
    if (!reference.isEmpty()) {
      RectF r = reference.get(Math.min(index, reference.size() - 1));
      return new RectF(r.centerX(), r.centerY(), r.centerX(), r.centerY());
    }
    float cx = getWidth() / 2f;
    float cy = getHeight() / 2f;
    return new RectF(cx, cy, cx, cy);
  }

  private float lerp(float a, float b, float t) {
    return a + (b - a) * t;
  }

  private float alphaForIndex(int index, float t) {
    boolean existsInFrom = index < fromCells.size();
    boolean existsInTo = index < toCells.size();
    if (existsInFrom && existsInTo) return 1f;
    if (existsInTo) return t;
    return 1f - t;
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    int w = getWidth();
    int h = getHeight();
    if (w <= 0 || h <= 0) return;

    float corner = dp(CORNER_DP);
    int maxCount = Math.max(fromCells.size(), toCells.size());

    if (maxCount == 0) {
      for (RectF r : buildCells(rows, cols, w, h)) {
        canvas.drawRoundRect(r, corner, corner, cellPaint);
        canvas.drawRoundRect(r, corner, corner, strokePaint);
      }
      return;
    }

    int baseFillAlpha = cellPaint.getAlpha();
    int baseStrokeAlpha = strokePaint.getAlpha();
    for (int i = 0; i < maxCount; i++) {
      RectF r = lerpForIndex(i, progress);
      float alpha = alphaForIndex(i, progress);
      cellPaint.setAlpha((int) (baseFillAlpha * alpha));
      strokePaint.setAlpha((int) (baseStrokeAlpha * alpha));
      canvas.drawRoundRect(r, corner, corner, cellPaint);
      canvas.drawRoundRect(r, corner, corner, strokePaint);
    }
    cellPaint.setAlpha(baseFillAlpha);
    strokePaint.setAlpha(baseStrokeAlpha);
  }

  private float dp(float value) {
    return value * getResources().getDisplayMetrics().density;
  }
}
