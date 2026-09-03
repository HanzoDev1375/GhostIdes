package ir.hanzodev1375.components.views;

import android.app.Application;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.StringRes;
import com.example.liquidglass.LiquidGlassView;
import ir.hanzodev1375.components.R;

public final class GhostToast {

  public static final int LENGTH_SHORT = Toast.LENGTH_SHORT;
  public static final int LENGTH_LONG = Toast.LENGTH_LONG;

  private static Context context;
  private static Toast t;
  private static boolean showicon = false;
  private static int iconres = 0;

  private GhostToast() {}

  public static void bindOfApp(Application app) {
    context = app.getApplicationContext();
  }

  public static GhostToast makeText(CharSequence text) {
    return makeText(text, LENGTH_SHORT);
  }

  public static GhostToast makeText(CharSequence text, int duration) {
    if (context == null) {
      throw new IllegalStateException(
          "GhostToast is not initialized. Call GhostToast.bindOfApp(Application) first.");
    }

    View v = LayoutInflater.from(context).inflate(R.layout.layout_toast, null, false);
    TextView textView = v.findViewById(R.id.texttoast);
    ImageView icon = v.findViewById(R.id.icontoast);
    LiquidGlassView glass = v.findViewById(R.id.glasstoast);
    glass.setBackdropSource(v.findViewById(R.id.toastbackdrop));

    textView.setText(text);
    if (iconres != 0) {
      icon.setImageResource(iconres);
      icon.setVisibility(View.VISIBLE);
    } else if (showicon) {
      icon.setVisibility(View.VISIBLE);
    } else {
      icon.setVisibility(View.GONE);
    }

    t = new Toast(context);
    t.setDuration(duration);
    t.setView(v);
    return new GhostToast();
  }

  public static GhostToast makeText(Context ignored, CharSequence text) {
    return makeText(text, LENGTH_SHORT);
  }

  public static GhostToast makeText(Context ignored, CharSequence text, int duration) {
    return makeText(text, duration);
  }

  public static GhostToast makeText(Context ignored, @StringRes int text, int duration) {
    return makeText(context.getString(text), duration);
  }

  public GhostToast show() {
    if (t != null) {
      t.show();
    }
    return this;
  }

  public static GhostToast show(CharSequence text) {
    return makeText(text).show();
  }

  public static boolean getShowicon() {
    return showicon;
  }

  public static void setShowicon(boolean showicon) {
    GhostToast.showicon = showicon;
  }

  public static int getIconRes() {
    return iconres;
  }

  public static void setIconRes(int iconres) {
    GhostToast.iconres = iconres;
  }
}
