package ir.hanzodev1375.ghostide.postman.util;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.view.ViewGroup;

import eightbitlab.com.blurview.BlurTarget;
import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.RenderScriptBlur;

/**
 * Wires up a BlurView against the activity's root content view. Used sparingly, just for the
 * response-panel handle and the "save request" sheet, purely for polish.
 */
public class BlurUtils {

  public static void applyBlur(
      Activity activity, BlurView blurView, BlurTarget blurRoot, float radiusPx) {
    Drawable windowBackground = activity.getWindow().getDecorView().getBackground();
    blurView.setupWith(blurRoot).setFrameClearDrawable(windowBackground).setBlurRadius(radiusPx);
    blurView.setClipToOutline(true);
  }
}
