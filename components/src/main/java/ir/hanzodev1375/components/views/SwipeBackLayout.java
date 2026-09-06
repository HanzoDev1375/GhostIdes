package ir.hanzodev1375.components.views;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.customview.widget.ViewDragHelper;

public class SwipeBackLayout extends FrameLayout {

  public interface OnSwipeBackListener {
    void onSwipeBackStart();

    void onSwipeBackCompleted();

    void onSwipeBackCancelled();
  }

  private static final float COMPLETE_THRESHOLD = 0.4f;
  private static final float MAX_SCRIM_ALPHA = 200f;

  private final Paint scrimPaint = new Paint();
  private final Paint edgePaint = new Paint();
  private ViewDragHelper dragHelper;
  private OnSwipeBackListener listener;
  private Activity activity;
  private boolean enabled = true;
  private boolean edgeOnly = true;
  private float dragPercent;
  private boolean finishSettling;
  private boolean cancelSettling;

  public SwipeBackLayout(@NonNull Context context) {
    this(context, null);
  }

  public SwipeBackLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public SwipeBackLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    float density = getResources().getDisplayMetrics().density;
    dragHelper = ViewDragHelper.create(this, 1f, new DragCallback());
    dragHelper.setEdgeTrackingEnabled(ViewDragHelper.EDGE_LEFT);
    scrimPaint.setAntiAlias(false);
    edgePaint.setAntiAlias(false);
    edgePaint.setColor(0xFFFFFFFF);
    setBackgroundColor(0xFF0C0E12);
  }

  /**
   * Wraps the activity content so swiping from the left edge slides it away
   * and finishes the activity. Returns the attached layout for customization.
   */
  public static SwipeBackLayout attach(@Nullable Activity activity) {
    if (activity == null || activity.isFinishing()) {
      return null;
    }
    ViewGroup content = activity.findViewById(android.R.id.content);
    if (content == null) {
      return null;
    }
    if (content.getParent() instanceof SwipeBackLayout) {
      return (SwipeBackLayout) content.getParent();
    }

    FrameLayout target = new FrameLayout(activity);
    while (content.getChildCount() > 0) {
      View child = content.getChildAt(0);
      if (child == null) break;
      content.removeView(child);
      target.addView(child);
    }

    SwipeBackLayout layout = new SwipeBackLayout(activity);
    layout.activity = activity;
    layout.addView(target);
    content.addView(
        layout,
        new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    layout.setListener(
        new OnSwipeBackListener() {
          @Override
          public void onSwipeBackStart() {}

          @Override
          public void onSwipeBackCompleted() {
            activity.finish();
            activity.overridePendingTransition(0, 0);
          }

          @Override
          public void onSwipeBackCancelled() {}
        });
    return layout;
  }

  public void setListener(@Nullable OnSwipeBackListener listener) {
    this.listener = listener;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  /** Restricts the gesture to the left edge. Default true. */
  public void setEdgeOnly(boolean edgeOnly) {
    this.edgeOnly = edgeOnly;
  }

  private class DragCallback extends ViewDragHelper.Callback {

    @Override
    public boolean tryCaptureView(@NonNull View child, int pointerId) {
      if (!enabled) return false;
      if (getChildCount() == 0 || child != getChildAt(0)) return false;
      if (edgeOnly && !dragHelper.isEdgeTouched(ViewDragHelper.EDGE_LEFT, pointerId)) return false;
      return true;
    }

    @Override
    public int getViewHorizontalDragRange(@NonNull View child) {
      return getWidth();
    }

    @Override
    public int clampViewPositionHorizontal(@NonNull View child, int left, int dx) {
      return Math.max(0, Math.min(left, getWidth()));
    }

    @Override
    public int clampViewPositionVertical(@NonNull View child, int top, int dy) {
      return child.getTop();
    }

    @Override
    public void onViewCaptured(@NonNull View child, int activePointerId) {
      if (listener != null) listener.onSwipeBackStart();
    }

    @Override
    public void onViewPositionChanged(@NonNull View child, int left, int top, int dx, int dy) {
      dragPercent = getWidth() == 0 ? 0f : left / (float) getWidth();
      invalidate();
    }

    @Override
    public void onViewReleased(@NonNull View child, float xvel, float yvel) {
      float velocity = TypedValue.applyDimension(
          TypedValue.COMPLEX_UNIT_DIP, 900f, getResources().getDisplayMetrics());
      boolean finishByVelocity = xvel > velocity && dragPercent > 0.1f;
      boolean finishByPosition = dragPercent >= COMPLETE_THRESHOLD;
      if (finishByVelocity || finishByPosition) {
        finishSettling = true;
        cancelSettling = false;
        if (dragHelper.settleCapturedViewAt(getWidth(), child.getTop())) {
          postInvalidateOnAnimation();
        } else {
          finishNow();
        }
      } else {
        finishSettling = false;
        cancelSettling = true;
        if (dragHelper.settleCapturedViewAt(0, child.getTop())) {
          postInvalidateOnAnimation();
        } else {
          cancelSettling = false;
        }
      }
    }

    @Override
    public void onViewDragStateChanged(int state) {
      if (state == ViewDragHelper.STATE_IDLE) {
        if (finishSettling) {
          finishSettling = false;
          finishNow();
        } else if (cancelSettling) {
          cancelSettling = false;
          if (listener != null) listener.onSwipeBackCancelled();
        }
      }
    }
  }

  private void finishNow() {
    if (listener != null) listener.onSwipeBackCompleted();
  }

  @Override
  public void computeScroll() {
    super.computeScroll();
    if (dragHelper != null && dragHelper.continueSettling(true)) {
      postInvalidateOnAnimation();
    }
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent ev) {
    if (!enabled) return super.onInterceptTouchEvent(ev);
    try {
      boolean intercepted = dragHelper.shouldInterceptTouchEvent(ev);
      return intercepted || super.onInterceptTouchEvent(ev);
    } catch (Throwable tr) {
      return super.onInterceptTouchEvent(ev);
    }
  }

  @Override
  public boolean onTouchEvent(MotionEvent ev) {
    if (!enabled) return super.onTouchEvent(ev);
    try {
      dragHelper.processTouchEvent(ev);
    } catch (Throwable tr) {
      finishSettling = false;
      cancelSettling = true;
    }
    return true;
  }

  @Override
  protected void dispatchDraw(Canvas canvas) {
    super.dispatchDraw(canvas);
    if (dragPercent <= 0f || getChildCount() == 0) return;

    float alpha = MAX_SCRIM_ALPHA * dragPercent;
    scrimPaint.setColor(Color.argb((int) alpha, 0, 0, 0));
    canvas.drawRect(0, 0, getWidth(), getHeight(), scrimPaint);

    View child = getChildAt(0);
    if (child == null) return;
    int right = child.getLeft() + child.getWidth();
    if (right >= 0 && right <= getWidth()) {
      edgePaint.setShader(
          new LinearGradient(right, 0, right + dp(24), 0, 0x66FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
      canvas.drawRect(right, 0, right + dp(24), getHeight(), edgePaint);
      edgePaint.setShader(null);
    }
  }

  private int dp(float value) {
    return Math.round(value * getResources().getDisplayMetrics().density);
  }
}