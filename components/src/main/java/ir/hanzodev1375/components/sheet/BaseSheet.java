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
import com.example.liquidglass.LiquidGlassView;
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

              // شفاف کردن پس‌زمینه bottom sheet
              bottomSheet.setBackgroundColor(Color.TRANSPARENT);

              // مسیر شیشه‌ای: نمونه‌برداری پس‌زمینه از content view اکتیویتی
              LiquidGlassView glass = binding.glassView;
              glass.setBackdropSource(activity.findViewById(android.R.id.content));
              glass.setEnableDynamicBackground(true);

              // رفرش شیشه هنگام drag/ dismiss
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
