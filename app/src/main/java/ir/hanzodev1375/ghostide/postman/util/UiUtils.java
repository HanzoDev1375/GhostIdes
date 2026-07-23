package ir.hanzodev1375.ghostide.postman.util;

import android.content.Context;

import android.view.View;
import androidx.core.content.ContextCompat;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import ir.hanzodev1375.ghostide.R;

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

  public static void fixUi(View target) {
    ViewCompat.setOnApplyWindowInsetsListener(
        target,
        (v, insets) -> {
          Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());
          Insets navInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars());

          int bottomInset = Math.max(imeInsets.bottom, navInsets.bottom);
          v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), bottomInset);
          return insets;
        });
  }
}
