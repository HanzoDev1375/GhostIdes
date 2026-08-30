package ir.hanzodev1375.components.sheet;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import ir.hanzodev1375.components.R;
import ir.hanzodev1375.components.sheet.customitemsheet.ui.GlassCompat;
import ir.hanzodev1375.components.databinding.BaseBlurBottomSheetBinding;
import ir.hanzodev1375.components.utils.ComponentsPrefs;

public class BaseSheet extends BottomSheetDialog {

  private BaseBlurBottomSheetBinding binding;
  private boolean hasPeekMod = false;
  private final ComponentsPrefs app;
  private boolean contentAdded = false;

  public BaseSheet(@NonNull Context context) {
    super(context);
    app = new ComponentsPrefs(context);
  }

  public BaseSheet(@NonNull Context context, int style) {
    super(context, style);
    app = new ComponentsPrefs(context);
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
        // غیرفعال کردن انیمیشن ویندوز (جلوگیری از فریز شدن شیشه)
        window.setWindowAnimations(0);
      }

      View root = binding.getRoot();
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
              if (binding == null) return;
              Activity activity = BlurBackdrop.findActivity(getContext());
              if (activity == null) return;
              bottomSheet.setBackgroundColor(Color.TRANSPARENT);
              GlassCompat glass = binding.glassView;
              glass.setBackdropSource(activity.findViewById(android.R.id.content));
              glass.setEnableDynamicBackground(true);
              behavior.addBottomSheetCallback(
                  new BottomSheetBehavior.BottomSheetCallback() {
                    @Override
                    public void onStateChanged(@NonNull View bottomSheet, int newState) {
                      glass.invalidate();
                    }

                    @Override
                    public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                      glass.invalidate();
                    }
                  });
              setDismissWithAnimation(true);
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
