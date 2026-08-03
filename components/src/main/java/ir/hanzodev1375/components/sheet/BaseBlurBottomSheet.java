package ir.hanzodev1375.components.sheet;

import android.graphics.Color;
import android.graphics.Outline;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.blankj.utilcode.util.SizeUtils;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import ir.hanzodev1375.components.R;
import ir.hanzodev1375.components.databinding.BaseBlurBottomSheetBinding;
import ir.hanzodev1375.ghostide.codeeditors.setting.PreferencesUtils;
import jp.wasabeef.blurry.Blurry;

/** root has LinearLayout pls adding call contentContainer.addView(#View,ViewGroup.LayoutParam) */
public abstract class BaseBlurBottomSheet extends BottomSheetDialogFragment {

  protected BaseBlurBottomSheetBinding binding;
  private boolean hasPeekMod = false;
  private PreferencesUtils app;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding = BaseBlurBottomSheetBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    View root = binding.getRoot();
    app = new PreferencesUtils(requireContext());
    float cornerRadius = getResources().getDimension(R.dimen.bottom_sheet_corner_radius);
    root.setClipToOutline(true);
    root.setOutlineProvider(
        new ViewOutlineProvider() {
          @Override
          public void getOutline(View v, Outline outline) {
            outline.setRoundRect(
                0, 0, v.getWidth(), v.getHeight() + (int) cornerRadius, cornerRadius);
          }
        });
    requireDialog().getWindow().setStatusBarColor(Color.TRANSPARENT);
    requireDialog().getWindow().setNavigationBarColor(Color.TRANSPARENT);
    root.addOnLayoutChangeListener(
        (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
          v.invalidateOutline();
        });
    expandSheet();
    onContentReady(binding.contentContainer);
  }

  private void expandSheet() {
    View bottomSheet = requireDialog().findViewById(R.id.design_bottom_sheet);
    if (bottomSheet != null) {
      BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
      behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
      behavior.setSkipCollapsed(true);

      bottomSheet.post(
          () -> {
            if (app.isBlurMod()) {
              bottomSheet.setBackgroundColor(Color.TRANSPARENT);
              View decorView =
                  requireActivity().getWindow().getDecorView().findViewById(android.R.id.content);
              Blurry.with(requireActivity())
                  .radius(24)
                  .sampling(4)
                  .async()
                  .capture(decorView)
                  .into(binding.blurBackground);
            }
          });
    }
  }

  protected abstract void onContentReady(ViewGroup contentContainer);

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }

  public boolean getHasPeekMod() {
    return this.hasPeekMod;
  }

  public void setHasPeekMod(boolean hasPeekMod) {
    this.hasPeekMod = hasPeekMod;
  }
}
