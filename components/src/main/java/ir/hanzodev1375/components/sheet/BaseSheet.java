package ir.hanzodev1375.components.sheet;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Outline;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.Window;
import androidx.annotation.NonNull;
import com.blankj.utilcode.util.SizeUtils;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import ir.hanzodev1375.components.R;
import ir.hanzodev1375.components.databinding.BaseBlurBottomSheetBinding;
import ir.hanzodev1375.ghostide.codeeditors.setting.PreferencesUtils;
import jp.wasabeef.blurry.Blurry;

public class BaseSheet extends BottomSheetDialog {

  private BaseBlurBottomSheetBinding binding;
  private boolean hasPeekMod = false;
  private PreferencesUtils app;
  private boolean contentAdded = false;

  public BaseSheet(@NonNull Context context) {
    super(context);
  }

  public BaseSheet(@NonNull Context context, int style) {
    super(context, style);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    app = new PreferencesUtils(getContext());
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
      float cornerRadius =
          getContext().getResources().getDimension(R.dimen.bottom_sheet_corner_radius);
      root.setClipToOutline(true);
      root.setOutlineProvider(
          new ViewOutlineProvider() {
            @Override
            public void getOutline(View v, Outline outline) {
              outline.setRoundRect(
                  0, 0, v.getWidth(), v.getHeight() + (int) cornerRadius, cornerRadius);
            }
          });
      root.addOnLayoutChangeListener(
          (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
              v.invalidateOutline());

      expandSheet();
    }

    if (view != null && !contentAdded) {
      binding.contentContainer.addView(
          view,
          new ViewGroup.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
      contentAdded = true;
    }
  }

  @Override
  public void setContentView(int layoutResID) {
    View view = LayoutInflater.from(getContext()).inflate(layoutResID, null);
    setContentView(view);
  }

 private void expandSheet() {
    View bottomSheet = findViewById(R.id.design_bottom_sheet);
    if (bottomSheet != null) {
        BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        behavior.setSkipCollapsed(true);

        bottomSheet.post(() -> {
            
            if (app.isBlurMod()) {
                bottomSheet.setBackgroundColor(Color.TRANSPARENT);
                View decorView = getWindow().getDecorView().findViewById(android.R.id.content);
                Blurry.with(getContext())
                        .radius(24)
                        .sampling(4)
                        .async()
                        .capture(decorView)
                        .into(binding.blurBackground);
            }
        });
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
