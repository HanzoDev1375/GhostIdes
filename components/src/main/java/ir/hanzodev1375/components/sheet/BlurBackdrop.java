package ir.hanzodev1375.components.sheet;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;

public final class BlurBackdrop {

  private static final int MAX_ATTEMPTS = 10;

  private BlurBackdrop() {}

  public static Activity findActivity(Context context) {
    while (context instanceof ContextWrapper) {
      if (context instanceof Activity) return (Activity) context;
      context = ((ContextWrapper) context).getBaseContext();
    }
    return null;
  }

  public static void captureInto(Activity activity, ImageView imageView) {
    captureInto(activity, imageView, 0);
  }

  private static void captureInto(Activity activity, ImageView imageView, int attempt) {
    if (activity == null || imageView == null || attempt > MAX_ATTEMPTS) return;
    View content = activity.getWindow().getDecorView();
    if (content.getWidth() <= 0 || content.getHeight() <= 0) {
      content.post(() -> captureInto(activity, imageView, attempt + 1));
      return;
    }

    Drawable windowBackground = activity.getWindow().getDecorView().getBackground();
    if (windowBackground != null) imageView.setBackground(windowBackground);

    Bitmap bitmap =
        Bitmap.createBitmap(content.getWidth(), content.getHeight(), Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
    content.draw(canvas);
    imageView.setImageBitmap(bitmap);
  }
}
