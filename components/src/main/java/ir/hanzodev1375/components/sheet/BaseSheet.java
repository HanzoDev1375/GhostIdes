package ir.hanzodev1375.components.sheet;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Outline;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.Window;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.color.MaterialColors;
import ir.hanzodev1375.components.R;
import ir.hanzodev1375.components.databinding.BaseBlurBottomSheetBinding;
import ir.hanzodev1375.ghostide.codeeditors.setting.PreferencesUtils;

public class BaseSheet extends BottomSheetDialog {

  private BaseBlurBottomSheetBinding binding;
  private boolean hasPeekMod = false;
  private final PreferencesUtils app;
  private boolean contentAdded = false;

  public BaseSheet(@NonNull Context context) {
    super(context);
    app = new PreferencesUtils(context);
  }

  public BaseSheet(@NonNull Context context, int style) {
    super(context, style);
    app = new PreferencesUtils(context);
  }

  @Override
  public void setContentView(View view) {
    if (binding == null) {
      binding = BaseBlurBottomSheetBinding.inflate(LayoutInflater.from(getContext()));
      super.setContentView(binding.getRoot());

      Window window = getWindow();
      if (window != null) {
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
      }

      View root = binding.getRoot();

      root.addOnLayoutChangeListener(
          (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
              v.invalidateOutline());

      expandSheet();
    }

    if (view != null && !contentAdded) {
      ViewGroup.LayoutParams params = view.getLayoutParams();
      if (params == null) {
        params =
            new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      }
      binding.contentContainer.addView(view, params);
      contentAdded = true;
    }
  }

  @Override
  public void setContentView(int layoutResID) {
    View view = LayoutInflater.from(getContext()).inflate(layoutResID, null);
    setContentView(view);
  }

  private void expandSheet() {
    FrameLayout bottomSheet = findViewById(R.id.design_bottom_sheet);
    if (bottomSheet != null) {
      BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
      behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
      behavior.setSkipCollapsed(true);

      if (app.isBlurMod()) {
        bottomSheet.post(
            () -> {
              bottomSheet.setBackgroundColor(0);
              float cornerRadius =
                  getContext().getResources().getDimension(R.dimen.bottom_sheet_corner_radius);
              binding.layoutblur.setClipToOutline(true);
              binding.layoutblur.setOutlineProvider(
                  new ViewOutlineProvider() {
                    @Override
                    public void getOutline(View v, Outline outline) {
                      outline.setRoundRect(
                          0, 0, v.getWidth(), v.getHeight() + (int) cornerRadius, cornerRadius);
                    }
                  });
              binding
                  .layoutblur
                  .setupWith(binding.blurTarget)
                  .setFrameClearDrawable(getWindow().getDecorView().getBackground())
                  .setBlurEnabled(true)
                  //     .setOverlayColor(MaterialColors.getColor(binding.layoutblur,
                  // R.attr.colorSurface))
                  .setBlurRadius(18f);
            });
      }
    }
  }

  @Override
  public void dismiss() {
    binding = null;
    super.dismiss();
  }

  public boolean getHasPeekMod() {
    return hasPeekMod;
  }

  public void setHasPeekMod(boolean hasPeekMod) {
    this.hasPeekMod = hasPeekMod;
  }
}
