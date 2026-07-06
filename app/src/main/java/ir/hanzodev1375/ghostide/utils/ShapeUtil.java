package ir.hanzodev1375.ghostide.utils;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.view.View;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.google.android.material.R;
import ir.hanzodev1375.ghostide.activity.BaseCompat;

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

  public static Drawable shape(float topRadius, Context context) {
    float r = dpToPx(context, topRadius);
    ShapeAppearanceModel model =
        ShapeAppearanceModel.builder()
            .setTopLeftCornerSize(r)
            .setTopRightCornerSize(r)
            .setBottomLeftCornerSize(0)
            .setBottomRightCornerSize(0)
            .build();

    MaterialShapeDrawable drawable = new MaterialShapeDrawable(model);
    drawable.setFillColor(ColorStateList.valueOf(getSurfaceColor(context)));
    drawable.setElevation(0);

    ColorStateList rippleColor = ColorStateList.valueOf(getRippleColor(context));
    return new RippleDrawable(rippleColor, drawable, null);
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
    return drawable;
  }

  public static int getcolorSurfaceContainer(View v) {
    return MaterialColors.getColor(v, R.attr.colorSurfaceContainer);
  }

  public static int getcolorSurfaceContainer(Context v) {
    return MaterialColors.getColor(v, R.attr.colorSurfaceContainer, 0);
  }

  private static Drawable createRippleDrawable(Drawable content, View view) {
    ColorStateList rippleColor = ColorStateList.valueOf(getRippleColor(view));
    return new RippleDrawable(rippleColor, content, null);
  }

  private static int getSurfaceColor(View view) {
    return MaterialColors.getColor(view, R.attr.colorSurface);
  }

  public static int getRippleColor(View view) {
    return MaterialColors.getColor(view, R.attr.colorSurfaceContainerHighest);
  }

  private static int getRippleColor(Context view) {
    return MaterialColors.getColor(view, R.attr.colorSurfaceContainerHighest, 0);
  }

  private static float dpToPx(View view, float dp) {
    return dp * view.getResources().getDisplayMetrics().density;
  }

  private static float dpToPx(Context context, float dp) {
    return dp * context.getResources().getDisplayMetrics().density;
  }

  private static int getSurfaceColor(Context context) {
    return MaterialColors.getColor(context, R.attr.colorSurface, 0);
  }

  public static int getcolorPrimaryContainer(View v) {
    return MaterialColors.getColor(v, R.attr.colorPrimaryContainer, 0);
  }
}
