package ir.hanzodev1375.ghostide.customui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.liquidglass.LiquidGlassFab;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.shape.ShapeAppearanceModel;
import ir.hanzodev1375.components.sheet.customitemsheet.ui.GlassCompat;

import ir.hanzodev1375.ghostide.R;
import ir.hanzodev1375.ghostide.codeeditors.setting.PreferencesUtils;

public class FloatingToolbarView extends FrameLayout {

  private enum State {
    COLLAPSED,
    EXPANDING,
    EXPANDED,
    COLLAPSING
  }

  private State state = State.COLLAPSED;

  private LinearLayout root;
  private LiquidGlassFab fab;
  private GlassCompat cardView;
  private RecyclerView recyclerView;

  private AnimatorSet currentAnimation;
  private Orientation orientation = Orientation.Left;
  private int fabCustomSize;
  private PreferencesUtils setting;
  private boolean glassWarmedUp = false;

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

  public FloatingToolbarView(
      @NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
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
    fab.setEnableDynamicBackground(true);
    cardView.setEnableDynamicBackground(true);
    setFabCustomSize(dp(70));
    fab.setOnClickListener(v -> toggleToolbar());
    applyOrientation();
    setElevation(0);
    cardView.setElevation(0);
    if (setting.isGlassMaterialColor()) {
      fab.setGlassTint(MaterialColors.getColor(fab, R.attr.colorSurface), 0.6f);
    }
  }

  public void bindOfAcivity(Activity v) {
    if (v != null) {
      fab.setBackdropSource(v.findViewById(R.id.backdropContent));
      cardView.setBackdropSource(v.findViewById(R.id.backdropContent));
    }
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    warmUpCardViewGlass();
  }

  private void warmUpCardViewGlass() {
    if (glassWarmedUp) return;
    glassWarmedUp = true;
    int prevVisibility = cardView.getVisibility();
    float prevAlpha = cardView.getAlpha();
    cardView.setAlpha(0f);
    cardView.setVisibility(VISIBLE);
    cardView.invalidate();
    post(
        () -> {
          cardView.setVisibility(prevVisibility);
          cardView.setAlpha(prevAlpha);
        });
  }

  private void applyOrientation() {
    root.removeAllViews();
    int fabSize = fabCustomSize > 0 ? fabCustomSize : LayoutParams.WRAP_CONTENT;
    LinearLayout.LayoutParams fabParams = new LinearLayout.LayoutParams(fabSize, fabSize);
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
    if (state == State.EXPANDED) {
      cardView.setVisibility(VISIBLE);
      cardView.setScaleX(1f);
      cardView.setAlpha(1f);
    } else {
      cardView.setVisibility(GONE);
      cardView.setScaleX(0f);
    }
  }

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
    setFabCustomSize(dp(58));
    cardView.setVisibility(VISIBLE);
    cardView.setScaleX(0f);
    cardView.setAlpha(0f);
    cardView.invalidate();
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
    set.addListener(
        new AnimatorListenerAdapter() {
          @Override
          public void onAnimationEnd(Animator animation) {
            if (state == State.EXPANDING) {
              state = State.EXPANDED;
              playWiggleAnimation();
              cardView.post(() -> cardView.invalidate());
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
    setFabCustomSize(dp(70));
    ValueAnimator scaleX = ValueAnimator.ofFloat(1f, 0f);
    scaleX.setDuration(220);
    scaleX.setInterpolator(new AccelerateInterpolator());
    scaleX.addUpdateListener(a -> cardView.setScaleX((float) a.getAnimatedValue()));
    cardView.invalidate();
    ValueAnimator alphaOut = ValueAnimator.ofFloat(1f, 0f);
    alphaOut.setDuration(220);
    alphaOut.setInterpolator(new AccelerateInterpolator());
    alphaOut.addUpdateListener(a -> cardView.setAlpha((float) a.getAnimatedValue()));

    set.playTogether(scaleX, alphaOut);
    set.addListener(
        new AnimatorListenerAdapter() {
          @Override
          public void onAnimationEnd(Animator animation) {
            if (state == State.COLLAPSING) {
              cardView.setVisibility(GONE);
              state = State.COLLAPSED;
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
      cardView
          .animate()
          .translationX(-delta)
          .setDuration(40)
          .withEndAction(
              () ->
                  cardView
                      .animate()
                      .translationX(delta)
                      .setDuration(60)
                      .withEndAction(
                          () ->
                              cardView
                                  .animate()
                                  .translationX(-delta / 2)
                                  .setDuration(40)
                                  .withEndAction(
                                      () ->
                                          cardView
                                              .animate()
                                              .translationX(0)
                                              .setDuration(30)
                                              .start())
                                  .start())
                      .start())
          .start();
    } else {
      cardView
          .animate()
          .translationX(delta)
          .setDuration(40)
          .withEndAction(
              () ->
                  cardView
                      .animate()
                      .translationX(-delta)
                      .setDuration(60)
                      .withEndAction(
                          () ->
                              cardView
                                  .animate()
                                  .translationX(delta / 2)
                                  .setDuration(40)
                                  .withEndAction(
                                      () ->
                                          cardView
                                              .animate()
                                              .translationX(0)
                                              .setDuration(30)
                                              .start())
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

  public LiquidGlassFab getFab() {
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

  void setFabCustomSize(int size) {
    this.fabCustomSize = size;
    fab.setLayoutParams(new LinearLayout.LayoutParams(size, size));
  }
}
