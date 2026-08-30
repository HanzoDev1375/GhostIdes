package ir.hanzodev1375.components.sheet;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import ir.hanzodev1375.components.R;
import ir.hanzodev1375.components.sheet.customitemsheet.ui.GlassCompat;
import ir.hanzodev1375.components.databinding.BaseBlurBottomSheetBinding;
import ir.hanzodev1375.ghostide.codeeditors.setting.PreferencesUtils;

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
    requireDialog().getWindow().setStatusBarColor(Color.TRANSPARENT);
    requireDialog().getWindow().setNavigationBarColor(Color.TRANSPARENT);
    expandSheet();
    onContentReady(binding.contentContainer);
  }

  private void expandSheet() {
    FrameLayout bottomSheet = requireDialog().findViewById(R.id.design_bottom_sheet);
    if (bottomSheet != null) {
      BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
      behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
      behavior.setSkipCollapsed(true);

      if (app.isBlurMod()) {
        bottomSheet.post(
            () -> {
              if (binding == null) return;
              Activity activity = getActivity();
              if (activity == null) return;
              bottomSheet.setBackgroundColor(Color.TRANSPARENT);
              GlassCompat glass = binding.glassView;
              glass.setBackdropSource(activity.findViewById(android.R.id.content));
              glass.setEnableDynamicBackground(true);
            });
      }
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
