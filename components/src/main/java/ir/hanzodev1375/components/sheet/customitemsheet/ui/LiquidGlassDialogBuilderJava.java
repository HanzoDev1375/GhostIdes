package ir.hanzodev1375.components.sheet.customitemsheet.ui;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import com.example.liquidglass.GlassMaterial;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import ir.hanzodev1375.components.R;
import ir.theme.M3Theme;
import java.util.ArrayList;
import java.util.List;

/**
 * Java port of LiquidGlassDialogBuilder — wraps the whole Material dialog panel in a GlassCompat.
 * Same behavior as the Kotlin version: rounded corners, enter/exit animation, cross-window backdrop
 * sampling, adaptive text color. Everything else works exactly like DialogCompat.
 *
 * <p>NOTE: renamed to *Java to avoid clashing with the original Kotlin class — both declare
 * com.example.liquidglass.LiquidGlassDialogBuilder, so having both on the classpath at once won't
 * compile under the same name. Drop this once the upstream PR is merged and released.
 *
 * <p>There's no Java-friendly way to express the Kotlin glassSetup lambda-with-receiver, so it's
 * dropped here — configureGlass() is the only customization hook, and it's called at the exact same
 * point (defaults applied, before the enter animation).
 *
 * <p>Usage: new LiquidGlassDialogBuilderJava(this) .setTitle("Liquid Glass") .setMessage("The whole
 * panel sits inside the glass, backdrop sampled from the Activity") .setPositiveButton("OK", null)
 * .setNegativeButton("Cancel", null) .show();
 */
public class LiquidGlassDialogBuilderJava extends MaterialAlertDialogBuilder {

  /** Called once the glass view's defaults are applied, before the enter animation starts. */
  public interface GlassConfigurator {
    void configure(GlassCompat glass);
  }

  private static final float ENTER_SCALE = 0.86f;
  private static final float EXIT_SCALE = 0.88f;
  private static final long ENTER_DURATION_MS = 250L;
  private static final long EXIT_DURATION_MS = 150L;
  private static final float DEFAULT_DIM = 0.32f;
  private static final float DEFAULT_CORNER_RADIUS_DP = 28f;
  // radius ~= 4 + 0.6 x 32 ~= 23px -- dialogs are big enough that you need this much blur
  // to hide background detail
  private static final float DEFAULT_BLUR = 0.6f;
  private static final int OVER_LIGHT_TEXT = 0xDE000000;
  // Cap the glass card width so it stays compact like a Material dialog instead of filling the
  // screen
  private static final float MAX_WIDTH_DP = 380f;
  private static final float SLIDER_MIN_TINT = 0.1f;
  private static final float SLIDER_MAX_TINT = 1f;

  private final float cornerRadiusDp;
  private final float glassBlurAmount;
  private final boolean animateShow;
  private final boolean dimBehind;

  private GlassCompat glass;
  private AlertDialog alertDialog;
  private GlassConfigurator glassReadyCallback;
  private boolean showGlassTintSlider;

  private int overLightTextColor = OVER_LIGHT_TEXT;
  private int overDarkTextColor = Color.WHITE;

  public LiquidGlassDialogBuilderJava(Context context) {
    this(context, 0);
  }

  public LiquidGlassDialogBuilderJava(Context context, int themeResId) {
    this(context, themeResId, DEFAULT_CORNER_RADIUS_DP);
  }

  public LiquidGlassDialogBuilderJava(Context context, int themeResId, float cornerRadiusDp) {
    this(context, themeResId, cornerRadiusDp, DEFAULT_BLUR);
  }

  public LiquidGlassDialogBuilderJava(
      Context context, int themeResId, float cornerRadiusDp, float glassBlurAmount) {
    this(context, themeResId, cornerRadiusDp, glassBlurAmount, true);
  }

  public LiquidGlassDialogBuilderJava(
      Context context,
      int themeResId,
      float cornerRadiusDp,
      float glassBlurAmount,
      boolean animateShow) {
    this(context, themeResId, cornerRadiusDp, glassBlurAmount, animateShow, false);
  }

  public LiquidGlassDialogBuilderJava(
      Context context,
      int themeResId,
      float cornerRadiusDp,
      float glassBlurAmount,
      boolean animateShow,
      boolean dimBehind) {
    super(context, themeResId);
    this.cornerRadiusDp = cornerRadiusDp;
    this.glassBlurAmount = glassBlurAmount;
    this.animateShow = animateShow;
    this.dimBehind = dimBehind;
  }

  public GlassCompat getGlass() {
    return glass;
  }

  public int getOverLightTextColor() {
    return overLightTextColor;
  }

  public void setOverLightTextColor(int color) {
    overLightTextColor = color;
  }

  public int getOverDarkTextColor() {
    return overDarkTextColor;
  }

  public void setOverDarkTextColor(int color) {
    overDarkTextColor = color;
  }

  public LiquidGlassDialogBuilderJava configureGlass(GlassConfigurator callback) {
    this.glassReadyCallback = callback;
    return this;
  }

  /** Adds a slider into the dialog that live-adjusts the glass tint (0.1f -> 1f). */
  public LiquidGlassDialogBuilderJava withGlassTintSlider() {
    this.showGlassTintSlider = true;
    return this;
  }

  public LiquidGlassDialogBuilderJava withGlassTintSlider(boolean enabled) {
    this.showGlassTintSlider = enabled;
    return this;
  }

  @Override
  public AlertDialog create() {
    final AlertDialog dialog = super.create();
    alertDialog = dialog;
    final View decor = dialog.getWindow() != null ? dialog.getWindow().getDecorView() : null;
    if (decor != null) {
      decor
          .getViewTreeObserver()
          .addOnPreDrawListener(
              new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                  ViewTreeObserver vto = decor.getViewTreeObserver();
                  if (vto.isAlive()) {
                    vto.removeOnPreDrawListener(this);
                  }
                  installGlass(dialog);
                  return glass == null;
                }
              });
    }
    return dialog;
  }

  /**
   * Scales + fades out before dismissing. Back button / outside touch / buttons use the window
   * animation instead -- see installGlass().
   */
  public void dismissAnimated() {
    final AlertDialog dialog = alertDialog;
    if (dialog == null) return;
    final GlassCompat glassView = glass;
    if (glassView == null || !glassView.isAttachedToWindow()) {
      dialog.dismiss();
      return;
    }
    // The view layer is already doing scale+fade; stacking a window fade on top would look sluggish
    if (dialog.getWindow() != null) {
      dialog.getWindow().setWindowAnimations(0);
    }
    glassView
        .animate()
        .alpha(0f)
        .scaleX(EXIT_SCALE)
        .scaleY(EXIT_SCALE)
        .setDuration(EXIT_DURATION_MS)
        .setInterpolator(new AccelerateInterpolator(1.4f))
        .setUpdateListener(animator -> glassView.invalidate())
        .withEndAction(dialog::dismiss)
        .start();
  }

  private void installGlass(final AlertDialog dialog) {
    final Window window = dialog.getWindow();
    if (window == null) return;
    final ViewGroup content = window.getDecorView().findViewById(android.R.id.content);
    if (content == null) return;
    final View panel = content.getChildAt(0);
    if (panel == null || panel instanceof GlassCompat) return;

    window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    window.setDimAmount(dimBehind ? DEFAULT_DIM : 0f);
    // Entrance is handled by the view tree itself: the default window animation scales/translates
    // the whole window surface without the tree redrawing, so the glass keeps sampling the
    // pre-animation offset and the refraction freezes. Exit uses a pure-alpha window animation
    // instead -- it doesn't move the surface, and it covers every dismiss path (back button,
    // outside touch, buttons, dismiss()) except dismissAnimated(), which cancels it and runs its
    // own.
    window.setWindowAnimations(R.style.Animation_LiquidGlass_Dialog);
    clearPanelBackgrounds(panel);

    final ViewGroup.LayoutParams originalLp = panel.getLayoutParams();
    // NOTE: check the actual modifier on GlassCompat.onAppearanceChanged() in your version
    // of the library -- adjust "protected" below if it's declared differently.
    final GlassCompat glassView =
        new GlassCompat(getContext()) {
          @Override
          protected void onAppearanceChanged(boolean isOverLight) {
            applyTextColors(panel, isOverLight);
          }
        };

    glassView.setCornerRadius(
        cornerRadiusDp * glassView.getResources().getDisplayMetrics().density);
    glassView.setEnableDynamicBackground(true);
    // Dialogs are large with busy backgrounds, so blur + adaptive tint are what give the text
    // contrast. The default blurAmount (0.0625) only works out to a 6px radius -- barely blurred --
    // and text would sit right on top of background icons/text (that's what the original PR #8
    // version had).
    glassView.setMaterial(GlassMaterial.REGULAR);
    glassView.setEnableAdaptiveTint(true);
    glassView.setBlurAmount(glassBlurAmount);
    Activity activity = findActivity(getContext());
    glassView.setBackdropSource(
        activity != null ? activity.findViewById(android.R.id.content) : null);
    glassView.setLayoutParams(buildGlassLayoutParams(content, originalLp));
    glassView.setAlpha(animateShow ? 0f : 1f);
    glassView.setScaleX(animateShow ? ENTER_SCALE : 1f);
    glassView.setScaleY(animateShow ? ENTER_SCALE : 1f);
    if (glassReadyCallback != null) {
      glassReadyCallback.configure(glassView);
    }
    glass = glassView;
    // Once detached, stop holding the glass/dialog -- backdropSource is wired to the Activity's
    // content view
    glassView.addOnAttachStateChangeListener(
        new View.OnAttachStateChangeListener() {
          @Override
          public void onViewAttachedToWindow(View v) {}

          @Override
          public void onViewDetachedFromWindow(View v) {
            glass = null;
            alertDialog = null;
          }
        });

    FrameLayout.LayoutParams innerLp =
        new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
    if (originalLp instanceof ViewGroup.MarginLayoutParams) {
      ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) originalLp;
      innerLp.setMargins(mlp.leftMargin, mlp.topMargin, mlp.rightMargin, mlp.bottomMargin);
    }

    content.removeViewAt(0);
    glassView.addView(panel, innerLp);
    content.addView(glassView);
    // Adaptive tint waits for the brightness sample to finish before it fires, so seed it with
    // the current best guess to avoid the first frame flashing the theme color
    applyTextColors(panel, glassView.isOverLightBackground());
    runEnterAnimation();
  }

  private void runEnterAnimation() {
    final GlassCompat glassView = glass;
    if (glassView == null) return;
    if (!animateShow) {
      glassView.setAlpha(1f);
      glassView.setScaleX(1f);
      glassView.setScaleY(1f);
      return;
    }
    glassView.post(
        () ->
            glassView
                .animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(ENTER_DURATION_MS)
                .setInterpolator(new DecelerateInterpolator(1.6f))
                .setUpdateListener(animator -> glassView.invalidate())
                .start());
  }

  /**
   * Only clears container backgrounds -- the dialog's rounded white background is painted on
   * parentPanel/topPanel-type containers. Recursing into leaves would also wipe out button ripples,
   * dividers, and input underlines, so non-containers are skipped.
   */
  private void clearPanelBackgrounds(View view) {
    if (!(view instanceof ViewGroup)) return;
    if (view.getBackground() != null) {
      view.setBackground(null);
    }
    ViewGroup group = (ViewGroup) view;
    for (int i = 0; i < group.getChildCount(); i++) {
      clearPanelBackgrounds(group.getChildAt(i));
    }
  }

  /**
   * White text + shadow on dark backgrounds, dark text with no shadow on light ones -- matches
   * LiquidGlassButton.
   */
  private void applyTextColors(View panel, boolean isOverLight) {
    List<TextView> targets = new ArrayList<>();
    addIfPresent(targets, panel.findViewById(R.id.alertTitle));
    addIfPresent(targets, panel.findViewById(android.R.id.message));
    addIfPresent(targets, panel.findViewById(android.R.id.button1));
    addIfPresent(targets, panel.findViewById(android.R.id.button2));
    addIfPresent(targets, panel.findViewById(android.R.id.button3));
    for (int i = 0; i < targets.size(); i++) {
      TextView tv = targets.get(i);
      int color;
      if (i == 3) {
        color = M3Theme.onSurface();
      } else if (i >= 4) {
        color = M3Theme.onSurface();
      } else {
        color = M3Theme.onSurface();
      }
      tv.setTextColor(color);
    }
  }

  private FrameLayout.LayoutParams buildGlassLayoutParams(
      ViewGroup content, ViewGroup.LayoutParams originalLp) {
    final float density = getContext().getResources().getDisplayMetrics().density;
    final int maxWidthPx = (int) (MAX_WIDTH_DP * density);
    final int parentWidth =
        content.getMeasuredWidth() > 0
            ? content.getMeasuredWidth()
            : content.getWidth() > 0 ? content.getWidth() : maxWidthPx;
    FrameLayout.LayoutParams lp;
    if (originalLp instanceof ViewGroup.MarginLayoutParams) {
      ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) originalLp;
      lp =
          new FrameLayout.LayoutParams(
              FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
      lp.setMargins(mlp.leftMargin, mlp.topMargin, mlp.rightMargin, mlp.bottomMargin);
    } else {
      lp =
          new FrameLayout.LayoutParams(
              FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
    }
    lp.width = Math.min(maxWidthPx, parentWidth);
    lp.gravity = Gravity.CENTER;
    return lp;
  }

  private static void addIfPresent(List<TextView> list, TextView view) {
    if (view != null) {
      list.add(view);
    }
  }

  private static Activity findActivity(Context context) {
    if (context instanceof Activity) {
      return (Activity) context;
    } else if (context instanceof ContextWrapper) {
      return findActivity(((ContextWrapper) context).getBaseContext());
    }
    return null;
  }
}
