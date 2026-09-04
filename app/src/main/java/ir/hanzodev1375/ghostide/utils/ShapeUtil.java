package ir.hanzodev1375.ghostide.utils;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.View;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;
import ir.hanzodev1375.ghostide.activity.BaseCompat;
import ir.hanzodev1375.ghostide.codeeditors.setting.PreferencesUtils;
import ir.theme.M3Theme;

public class ShapeUtil {
  private static final float RADIUS_DP = 24f;

  public static Drawable top(View view) {
    return createRippleDrawable(createShapeDrawable(view, RADIUS_DP, RADIUS_DP, 0, 0), view);
  }

  public static Drawable bottom(View view) {
    return createRippleDrawable(createShapeDrawable(view, 0, 0, RADIUS_DP, RADIUS_DP), view);
  }

  public static Drawable middel(View view) {
    return createRippleDrawable(createShapeDrawable(view, 0, 0, 0, 0), view);
  }

  public static Drawable shapeCustomView(Context context) {
    float r = dpToPx(context, 20f);
    ShapeAppearanceModel model =
        ShapeAppearanceModel.builder()
            .setTopLeftCornerSize(r)
            .setTopRightCornerSize(r)
            .setBottomLeftCornerSize(r)
            .setBottomRightCornerSize(r)
            .build();

    MaterialShapeDrawable drawable = new MaterialShapeDrawable(model);
    drawable.setFillColor(ColorStateList.valueOf(getSurfaceColor(context)));
    drawable.setStroke(3, ColorStateList.valueOf(getcolorSurfaceContainer(context)));
    drawable.setElevation(0);

    ((BaseCompat) context)
        .getWindow()
        .getDecorView()
        .setBackgroundColor(getcolorSurfaceContainer(context));

    ColorStateList rippleColor = ColorStateList.valueOf(getRippleColor(context));
    return new RippleDrawable(rippleColor, drawable, null);
  }

  public static Drawable shape(float topRadiusDp, Context context) {
    float r = dpToPx(context, topRadiusDp);
    var bg = new GradientDrawable();
    bg.setColor(getSurfaceColor(context));
    bg.setCornerRadii(
        new float[] {
          r,
          r, // بالا-چپ
          r,
          r, // بالا-راست
          0,
          0, // پایین-راست
          0,
          0 // پایین-چپ
        });
    PreferencesUtils appsetting = new PreferencesUtils(context);
    bg.setAlpha(appsetting.isShowBackground() ? 128 : 255);
    ColorStateList rippleColor = ColorStateList.valueOf(getRippleColor(context));
    return new RippleDrawable(rippleColor, bg, null);
  }

  public static Drawable shape(float topRadiusDp, Context context, int color) {
    float r = dpToPx(context, topRadiusDp);
    var bg = new GradientDrawable();
    bg.setColor(color);
    bg.setCornerRadii(new float[] {r, r, r, r, 0, 0, 0, 0});
    PreferencesUtils appsetting = new PreferencesUtils(context);
    bg.setAlpha(appsetting.isShowBackground() ? 128 : 255);
    ColorStateList rippleColor = ColorStateList.valueOf(getRippleColor(context));
    return new RippleDrawable(rippleColor, bg, null);
  }

  private static MaterialShapeDrawable createShapeDrawable(
      View view, float topLeft, float topRight, float bottomLeft, float bottomRight) {
    float r = dpToPx(view, RADIUS_DP);
    ShapeAppearanceModel model =
        ShapeAppearanceModel.builder()
            .setTopLeftCornerSize(topLeft > 0 ? r : 0)
            .setTopRightCornerSize(topRight > 0 ? r : 0)
            .setBottomLeftCornerSize(bottomLeft > 0 ? r : 0)
            .setBottomRightCornerSize(bottomRight > 0 ? r : 0)
            .build();

    MaterialShapeDrawable drawable = new MaterialShapeDrawable(model);
    drawable.setFillColor(ColorStateList.valueOf(getcolorSurfaceContainer(view)));
    drawable.setElevation(0);
    PreferencesUtils appsetting = new PreferencesUtils(view.getContext());
    drawable.setAlpha(appsetting.isShowBackground() ? 100 : 255);
    return drawable;
  }

  public static int getcolorSurfaceContainer(View v) {
    Integer c = M3Theme.surfaceContainer();
    return c != null ? c : 0;
  }

  public static int getcolorSurfaceContainer(Context v) {
    Integer c = M3Theme.surfaceContainer();
    return c != null ? c : 0;
  }

  private static Drawable createRippleDrawable(Drawable content, View view) {
    ColorStateList rippleColor = ColorStateList.valueOf(getRippleColor(view));
    return new RippleDrawable(rippleColor, content, null);
  }

  private static int getSurfaceColor(View view) {
    Integer c = M3Theme.surface();
    return c != null ? c : 0;
  }

  public static int getRippleColor(View view) {
    Integer c = M3Theme.surfaceContainerHighest();
    return c != null ? c : 0;
  }

  private static int getRippleColor(Context view) {
    Integer c = M3Theme.surfaceContainerHighest();
    return c != null ? c : 0;
  }

  private static float dpToPx(View view, float dp) {
    return dp * view.getResources().getDisplayMetrics().density;
  }

  private static float dpToPx(Context context, float dp) {
    return dp * context.getResources().getDisplayMetrics().density;
  }

  private static int getSurfaceColor(Context context) {
    Integer c = M3Theme.surface();
    return c != null ? c : 0;
  }

  public static int getcolorPrimaryContainer(View v) {
    Integer c = M3Theme.primaryContainer();
    return c != null ? c : 0;
  }
}
