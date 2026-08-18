package ir.hanzodev1375.ghostide.customui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;

import ir.hanzodev1375.ghostide.R;
import ir.hanzodev1375.ghostide.codeeditors.setting.PreferencesUtils;
import ir.hanzodev1375.ghostide.utils.ShapeUtil;

public class FloatingToolbarView extends FrameLayout {

  private enum State {
    COLLAPSED,
    EXPANDING,
    EXPANDED,
    COLLAPSING
  }

  private State state = State.COLLAPSED;

  private LinearLayout root;
  private FloatingActionButton fab;
  private FrameLayout cardView;
  private RecyclerView recyclerView;

  private AnimatorSet currentAnimation;
  private Orientation orientation = Orientation.Left;
  private PreferencesUtils setting;

  public enum Orientation {
    Left,
    Right
  }

  public FloatingToolbarView(@NonNull Context context) {
    super(context);
    init(context);
  }

  public FloatingToolbarView(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }

  public FloatingToolbarView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(context);
  }

  private void init(Context context) {
    setting = new PreferencesUtils(context);
    LayoutInflater.from(context).inflate(R.layout.view_floating_action_toolbar, this, true);

    root = findViewById(R.id.floating_root);
    fab = findViewById(R.id.floating_fab);
    cardView = findViewById(R.id.floating_card_view);
    recyclerView = findViewById(R.id.floating_recycler_view);

    recyclerView.setLayoutManager(new LinearLayoutManager(context, RecyclerView.HORIZONTAL, false));

    updateShapeForOrientation();

    fab.setOnClickListener(v -> toggleToolbar());

    int defcolor = MaterialColors.getColor(fab, R.attr.colorPrimaryContainer);
    int defIcon = MaterialColors.getColor(fab, R.attr.colorOnPrimaryContainer);
    fab.setBackgroundTintList(
        ColorStateList.valueOf(
            setting.isShowBackground()
                ? ColorUtils.setAlphaComponent(defcolor, 128)
                : defcolor));
    fab.setImageTintList(
        ColorStateList.valueOf(
            setting.isShowBackground()
                ? ColorUtils.setAlphaComponent(defIcon, 128)
                : defIcon));

    applyOrientation();
  }

  private void updateShapeForOrientation() {
    float roundedCorner = dp(20);
    float flatCorner = dp(20);

    ShapeAppearanceModel model;
    if (orientation == Orientation.Right) {
      model =
          ShapeAppearanceModel.builder()
              .setTopLeftCornerSize(roundedCorner)
              .setTopRightCornerSize(flatCorner)
              .setBottomLeftCornerSize(roundedCorner)
              .setBottomRightCornerSize(flatCorner)
              .build();
    } else {
      model =
          ShapeAppearanceModel.builder()
              .setTopLeftCornerSize(flatCorner)
              .setTopRightCornerSize(roundedCorner)
              .setBottomLeftCornerSize(flatCorner)
              .setBottomRightCornerSize(roundedCorner)
              .build();
    }

    MaterialShapeDrawable drawable = new MaterialShapeDrawable(model);
    drawable.setFillColor(
        ColorStateList.valueOf(
            setting.isShowBackground()
                ? ColorUtils.setAlphaComponent(
                    ShapeUtil.getcolorSurfaceContainer(cardView), 128)
                : ShapeUtil.getcolorSurfaceContainer(cardView)));
    drawable.setStroke(
        3.3f,
        ColorStateList.valueOf(
            setting.isShowBackground()
                ? ColorUtils.setAlphaComponent(
                    ShapeUtil.getcolorPrimaryContainer(cardView), 128)
                : ShapeUtil.getcolorPrimaryContainer(cardView)));
    drawable.setElevation(dp(8));
    cardView.setBackground(drawable);
  }

  private void applyOrientation() {
    root.removeAllViews();
    LinearLayout.LayoutParams fabParams =
        new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    LinearLayout.LayoutParams cardParams =
        new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, dp(56));

    if (orientation == Orientation.Right) {
      root.addView(fab, fabParams);
      root.addView(cardView, cardParams);
      cardParams.leftMargin = dp(2);
      cardParams.rightMargin = 0;
      cardView.setPivotX(0f);
    } else {
      root.addView(cardView, cardParams);
      root.addView(fab, fabParams);
      cardParams.rightMargin = dp(2);
      cardParams.leftMargin = 0;
      cardView.setPivotX(1f);
    }
    cardView.setLayoutParams(cardParams);
  }

  public void setOrientation(Orientation orientation) {
    this.orientation = orientation;
    applyOrientation();
    updateShapeForOrientation();
    if (state == State.EXPANDED) {
      cardView.setVisibility(VISIBLE);
      cardView.setScaleX(1f);
      cardView.setAlpha(1f);
    } else {
      cardView.setVisibility(GONE);
      cardView.setScaleX(0f);
    }
  }

  // ──────────────────── Expand ────────────────────

  private void toggleToolbar() {
    if (state == State.EXPANDED || state == State.EXPANDING) {
      collapse();
    } else {
      expand();
    }
  }

  public void expand() {
    if (state == State.EXPANDED || state == State.EXPANDING) return;
    cancelCurrentAnimation();
    state = State.EXPANDING;

    fab.setCustomSize(dp(58));
    cardView.setVisibility(VISIBLE);
    cardView.setScaleX(0f);
    cardView.setAlpha(0f);

    AnimatorSet set = new AnimatorSet();

    ValueAnimator scaleX = ValueAnimator.ofFloat(0f, 1f);
    scaleX.setDuration(280);
    scaleX.setInterpolator(new DecelerateInterpolator(1.5f));
    scaleX.addUpdateListener(a -> cardView.setScaleX((float) a.getAnimatedValue()));

    ValueAnimator alphaIn = ValueAnimator.ofFloat(0f, 1f);
    alphaIn.setDuration(280);
    alphaIn.setInterpolator(new DecelerateInterpolator(1.5f));
    alphaIn.addUpdateListener(a -> cardView.setAlpha((float) a.getAnimatedValue()));

    set.playTogether(scaleX, alphaIn);
    set.addListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationEnd(Animator animation) {
        if (state == State.EXPANDING) {
          state = State.EXPANDED;
          playWiggleAnimation();
        }
        currentAnimation = null;
      }

      @Override
      public void onAnimationCancel(Animator animation) {
        currentAnimation = null;
      }
    });

    currentAnimation = set;
    set.start();
  }

  // ──────────────────── Collapse ────────────────────

  public void collapse() {
    if (state == State.COLLAPSED || state == State.COLLAPSING) return;
    cancelCurrentAnimation();
    state = State.COLLAPSING;

    AnimatorSet set = new AnimatorSet();

    ValueAnimator scaleX = ValueAnimator.ofFloat(1f, 0f);
    scaleX.setDuration(220);
    scaleX.setInterpolator(new AccelerateInterpolator());
    scaleX.addUpdateListener(a -> cardView.setScaleX((float) a.getAnimatedValue()));

    ValueAnimator alphaOut = ValueAnimator.ofFloat(1f, 0f);
    alphaOut.setDuration(220);
    alphaOut.setInterpolator(new AccelerateInterpolator());
    alphaOut.addUpdateListener(a -> cardView.setAlpha((float) a.getAnimatedValue()));

    set.playTogether(scaleX, alphaOut);
    set.addListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationEnd(Animator animation) {
        if (state == State.COLLAPSING) {
          cardView.setVisibility(GONE);
          state = State.COLLAPSED;
          fab.setCustomSize(dp(70));
        }
        currentAnimation = null;
      }

      @Override
      public void onAnimationCancel(Animator animation) {
        currentAnimation = null;
      }
    });

    currentAnimation = set;
    set.start();
  }

  // ──────────────────── Wiggle ────────────────────

  private void playWiggleAnimation() {
    int delta = dp(4);
    if (orientation == Orientation.Left) {
      cardView.animate().translationX(-delta).setDuration(40)
          .withEndAction(() -> cardView.animate().translationX(delta).setDuration(60)
              .withEndAction(() -> cardView.animate().translationX(-delta / 2).setDuration(40)
                  .withEndAction(() -> cardView.animate().translationX(0).setDuration(30).start())
                  .start())
              .start())
          .start();
    } else {
      cardView.animate().translationX(delta).setDuration(40)
          .withEndAction(() -> cardView.animate().translationX(-delta).setDuration(60)
              .withEndAction(() -> cardView.animate().translationX(delta / 2).setDuration(40)
                  .withEndAction(() -> cardView.animate().translationX(0).setDuration(30).start())
                  .start())
              .start())
          .start();
    }
  }

  // ──────────────────── Animation Interruption ────────────────────

  private void cancelCurrentAnimation() {
    if (currentAnimation != null) {
      currentAnimation.cancel();
      currentAnimation = null;
    }
  }

  // ──────────────────── Public API ────────────────────

  public RecyclerView getRecyclerView() {
    return recyclerView;
  }

  public FloatingActionButton getFab() {
    return fab;
  }

  public boolean isExpanded() {
    return state == State.EXPANDED;
  }

  public void dismiss() {
    if (isExpanded()) {
      collapse();
    }
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    cancelCurrentAnimation();
  }

  private int dp(float value) {
    return (int)
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
  }
}
