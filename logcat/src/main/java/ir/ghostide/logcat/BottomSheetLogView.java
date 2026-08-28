package ir.ghostide.logcat;

import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import ir.hanzodev1375.components.sheet.BaseBlurBottomSheet;

/**
 * Log viewer shown inside a liquid-glass bottom sheet. Extends {@link BaseBlurBottomSheet} so the
 * sheet is rendered with the LiquidGlass backdrop (respecting PreferencesUtils.isBlurMod()).
 */
public class BottomSheetLogView extends BaseBlurBottomSheet {
  public static final String TAG = "BottomSheetLogView";
  private MaterialLogCatView logview;
  public BottomSheetLogView() {}

  @Override
  protected void onContentReady(@NonNull ViewGroup contentContainer) {
    setHasPeekMod(false);
    View view = getLayoutInflater().inflate(R.layout.fragment_logview, contentContainer, false);
    contentContainer.addView(view);
    logview = view.findViewById(R.id.logview);
  }

  @Nullable
  public MaterialLogCatView getLogView() {
    return logview;
  }

  /** Convenience passthrough to reload logs. */
  public void refreshLogs() {
    if (logview != null) {
      logview.refreshLogs();
    }
  }
}
