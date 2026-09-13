package ir.hanzodev1375.ghostide.utils;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import com.example.liquidglass.GlassMaterial;
import ir.hanzodev1375.components.sheet.customitemsheet.ui.GlassCompat;
import ir.hanzodev1375.ghostide.GhostIdeAppLoader;
import ir.hanzodev1375.ghostide.R;
import ir.theme.M3Theme;
import ir.theme.ThemeManager;
import java.util.List;

public class EditorGlassMenu {

  public static class GlassMenuItem {
    final String title;
    final int iconRes;

    public GlassMenuItem(String title) {
      this(title, 0);
    }

    public GlassMenuItem(String title, int iconRes) {
      this.title = title;
      this.iconRes = iconRes;
    }
  }

  public interface OnGlassMenuItemClickListener {
    void onItemClick(int position, String title);
  }

  public static void showGlassMenu(
      Activity activity,
      View anchor,
      List<GlassMenuItem> items,
      OnGlassMenuItemClickListener listener) {
    if (activity == null || activity.isFinishing() || activity.isDestroyed() || activity.getWindow() == null) {
      return;
    }
    if (anchor == null || !anchor.isAttachedToWindow()) {
      return;
    }
    int[] loc = new int[2];
    anchor.getLocationOnScreen(loc);

    int menuColor = fallback(M3Theme.surface(), 0);
    int textColor = fallback(M3Theme.onSurface(), 0);
    try {
      var setting = GhostIdeAppLoader.getInstance().getSetting();
      if (setting.isShowBackground()) {
        var themeManager = new ThemeManager(activity);
        var ghostTheme = themeManager.getTheme();
        if (ghostTheme != null && ghostTheme.getEditor() != null) {
          menuColor = Color.parseColor(ghostTheme.getEditor().getCompletionWndBackground());
          textColor = Color.parseColor(ghostTheme.getEditor().getCompletionWndTextPrimary());
        }
      }
    } catch (Exception ignored) {
    }

    int padding = (int) (8 * activity.getResources().getDisplayMetrics().density);

    GlassCompat glassView = new GlassCompat(activity);
    glassView.setLayoutParams(
        new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    glassView.setGlassTint(menuColor);
    glassView.setCornerRadius(24f * activity.getResources().getDisplayMetrics().density);
    glassView.setRefractionHeight(66f);
    glassView.setBevelWidth(10f);
    glassView.setMaterial(GlassMaterial.REGULAR);
    glassView.setDispersionStrength(0.12f);
    glassView.setEnableDynamicBackground(true);
    glassView.setEnableSensorHighlight(true);
    glassView.setEnableAdaptiveTint(true);
    glassView.setBackdropSource(activity.findViewById(android.R.id.content));
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
      GlassMenuItem item = items.get(i);
      View v = LayoutInflater.from(activity).inflate(R.layout.glass_menu_item, container, false);
      ImageView iconView = v.findViewById(R.id.menuItemIcon);
      if (iconView != null) {
        if (item.iconRes != 0) {
          iconView.setVisibility(View.VISIBLE);
          iconView.setImageResource(item.iconRes);
          iconView.setColorFilter(textColor);
        } else {
          iconView.setVisibility(View.GONE);
        }
      }
      TextView textView = v.findViewById(R.id.menuItemText);
      textView.setText(item.title);
      final int index = i;
      textView.setOnClickListener(
          vvvv -> {
            if (popupWindow != null) {
              popupWindow.dismiss();
            }
            if (listener != null) listener.onItemClick(index, item.title);
          });
      container.addView(v);
    }

    glassView.addView(container);

    popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    popupWindow.setOutsideTouchable(true);
    popupWindow.setAnimationStyle(R.style.GlassMenuFadeAnimation);

    glassView.measure(
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
    int menuHeight = glassView.getMeasuredHeight();
    var dm = activity.getResources().getDisplayMetrics();
    int screenHeight = dm.heightPixels;
    int spaceBelow = screenHeight - (loc[1] + anchor.getHeight());
    int spaceAbove = loc[1];
    int y = loc[1] + anchor.getHeight();
    if (spaceBelow < menuHeight && spaceAbove > spaceBelow) {
      y = loc[1] - menuHeight;
    }
    if (activity.isFinishing() || activity.isDestroyed() || !anchor.isAttachedToWindow()) {
      return;
    }
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

  private static int fallback(Integer value, int def) {
    return value != null ? value : def;
  }
}