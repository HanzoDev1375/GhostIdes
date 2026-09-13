package ir.hanzodev1375.ghostide.codeeditors.ui;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import com.example.liquidglass.GlassMaterial;
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme;
import ir.hanzodev1375.components.sheet.customitemsheet.ui.GlassCompat;
import java.util.List;

/** منوی شیشه‌ای واقعی — دقیقاً طبق ObjectUtil.showGlassMenu */
public final class EditorGlassMenu {

  public interface OnEditorGlassMenuItemClickListener {
    void onItemClick(int position);
  }

  private EditorGlassMenu() {}

  public static void show(
      Context context,
      View anchor,
      List<CharSequence> items,
      EditorColorScheme scheme,
      OnEditorGlassMenuItemClickListener listener) {
    show(context, anchor, null, items, scheme, listener);
  }

  public static void show(
      Context context,
      View anchor,
      View tokenParent,
      List<CharSequence> items,
      EditorColorScheme scheme,
      OnEditorGlassMenuItemClickListener listener) {
    Activity activity = findActivity(context);
    if (activity == null || activity.isFinishing() || activity.isDestroyed() || activity.getWindow() == null) return;
    if (anchor == null || !anchor.isAttachedToWindow()) return;

    int[] loc = new int[2];
    anchor.getLocationOnScreen(loc);
    float density = activity.getResources().getDisplayMetrics().density;
    int padding = (int) (8 * density);

    int textColor = Color.WHITE;
    if (scheme != null) {
      textColor = scheme.getColor(EditorColorScheme.COMPLETION_WND_TEXT_SECONDARY);
    }

    GlassCompat glassView = new GlassCompat(activity);
    glassView.setLayoutParams(
        new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    glassView.setCornerRadius(24f * activity.getResources().getDisplayMetrics().density);
    glassView.setRefractionHeight(66f);
    glassView.setBevelWidth(10f);
    glassView.setMaterial(GlassMaterial.REGULAR);
    glassView.setDispersionStrength(0.12f);
    glassView.setEnableDynamicBackground(true);
    glassView.setEnableSensorHighlight(true);
    glassView.setEnableAdaptiveTint(true);
    View backdrop = activity.findViewById(android.R.id.content);
    if (backdrop == null && tokenParent != null) backdrop = tokenParent;
    glassView.setBackdropSource(backdrop);
    glassView.setAlpha(0f);

    PopupWindow popupWindow =
        new PopupWindow(
            glassView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true);
    popupWindow.setElevation(0);
    LinearLayout container = new LinearLayout(activity);
    container.setOrientation(LinearLayout.VERTICAL);
    container.setPadding(padding, padding, padding, padding);
    for (int i = 0; i < items.size(); i++) {
      TextView tv = new TextView(activity);
      tv.setText(items.get(i));
      tv.setTextColor(textColor);
      tv.setTextSize(12f);
      tv.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
      tv.setPadding(
          (int) (24 * density), (int) (14 * density), (int) (24 * density), (int) (14 * density));
      final int index = i;
      tv.setOnClickListener(
          v -> {
            if (popupWindow != null) popupWindow.dismiss();
            if (listener != null) listener.onItemClick(index);
          });
      container.addView(tv);
    }

    glassView.addView(container);

    popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    popupWindow.setOutsideTouchable(true);

    glassView.measure(
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
    int menuHeight = glassView.getMeasuredHeight();
    int screenHeight = activity.getResources().getDisplayMetrics().heightPixels;
    int spaceBelow = screenHeight - (loc[1] + anchor.getHeight());
    int spaceAbove = loc[1];
    int y = loc[1] + anchor.getHeight();
    if (spaceBelow < menuHeight && spaceAbove > spaceBelow) {
      y = loc[1] - menuHeight;
    }
    if (activity.isFinishing() || activity.isDestroyed() || !anchor.isAttachedToWindow()) return;
    try {
      popupWindow.showAtLocation(anchor, Gravity.TOP | Gravity.START, loc[0], y);
    } catch (WindowManager.BadTokenException e) {
      return;
    }

    ValueAnimator fadeIn = ValueAnimator.ofFloat(0f, 1f);
    fadeIn.setDuration(160);
    fadeIn.setInterpolator(new DecelerateInterpolator(1.5f));
    fadeIn.addUpdateListener(
        a -> {
          glassView.setAlpha((float) a.getAnimatedValue());
          glassView.invalidate();
        });
    fadeIn.start();
  }

  private static Activity findActivity(Context context) {
    if (context instanceof Activity) return (Activity) context;
    if (context instanceof ContextWrapper) {
      return findActivity(((ContextWrapper) context).getBaseContext());
    }
    return null;
  }
}
