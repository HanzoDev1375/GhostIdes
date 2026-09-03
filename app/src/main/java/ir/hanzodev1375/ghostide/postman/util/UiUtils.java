package ir.hanzodev1375.ghostide.postman.util;

import android.content.Context;

import android.graphics.Color;
import android.view.View;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import ir.hanzodev1375.ghostide.R;
import ir.hanzodev1375.ghostide.codeeditors.setting.PreferencesUtils;
import ir.theme.ThemeManager;
import ir.theme.ThemeUtils;

/** Small shared helpers for turning HTTP methods / status codes into the right accent color. */
public class UiUtils {

  public static int methodColor(Context context, String method) {
    if (method == null) return ContextCompat.getColor(context, R.color.method_default);
    switch (method.toUpperCase()) {
      case "GET":
        return ContextCompat.getColor(context, R.color.method_get);
      case "POST":
        return ContextCompat.getColor(context, R.color.method_post);
      case "PUT":
        return ContextCompat.getColor(context, R.color.method_put);
      case "PATCH":
        return ContextCompat.getColor(context, R.color.method_patch);
      case "DELETE":
        return ContextCompat.getColor(context, R.color.method_delete);
      default:
        return ContextCompat.getColor(context, R.color.method_default);
    }
  }

  public static int statusColor(Context context, int code) {
    if (code >= 200 && code < 300) return ContextCompat.getColor(context, R.color.status_success);
    if (code >= 300 && code < 400) return ContextCompat.getColor(context, R.color.status_redirect);
    if (code >= 400 && code < 500)
      return ContextCompat.getColor(context, R.color.status_client_error);
    if (code >= 500) return ContextCompat.getColor(context, R.color.status_server_error);
    return ContextCompat.getColor(context, R.color.status_neutral);
  }

  public static String statusLabel(int code, String message) {
    if (code <= 0) return "—";
    if (message == null || message.isEmpty()) return String.valueOf(code);
    return code + " " + message;
  }

  public static void fixUi(View topEdge, View bottomEdge) {
    ViewCompat.setOnApplyWindowInsetsListener(
        topEdge,
        (v, insets) -> {
          Insets status = insets.getInsets(WindowInsetsCompat.Type.statusBars());
          v.setPadding(v.getPaddingLeft(), status.top, v.getPaddingRight(), v.getPaddingBottom());
          return insets;
        });
    ViewCompat.setOnApplyWindowInsetsListener(
        bottomEdge,
        (v, insets) -> {
          Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
          Insets nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
          int bottom = Math.max(ime.bottom, nav.bottom);
          v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), bottom);
          return insets;
        });
  }

  public static void fixUi(View target) {
    fixUi(target, target);
  } 

  public static void fixBottomBar(View target) {
    ViewCompat.setOnApplyWindowInsetsListener(
        target,
        (v, insets) -> {
          Insets nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
          v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), nav.bottom);
          return insets;
        });
  }

  /**
   * وقتی حالت بک‌گراند فعال باشد، رنگ بک‌گراند تم را با شفافیت ۱۲۸ (الفا) روی ویوهای محتوا اعمال
   * می‌کند تا تصویر پس‌زمینه پشت آن‌ها دیده شود. باید بعد از setupBackgroundBlur صدا زده شود.
   */
  public static void tintContentForBackground(Context context, View... views) {
    if (!new PreferencesUtils(context).isShowBackground()) {
      return;
    }
    ThemeUtils themeUtil = new ThemeUtils(new ThemeManager(context));
    var theme = themeUtil.getTheme();
    if (theme == null
        || theme.getActivity() == null
        || theme.getActivity().getBackground() == null) {
      return;
    }
    int bg = Color.parseColor(theme.getActivity().getBackground());
    int tint = ColorUtils.setAlphaComponent(bg, 128);
    for (View v : views) {
      if (v != null) {
        v.setBackgroundColor(tint);
      }
    }
  }
}
