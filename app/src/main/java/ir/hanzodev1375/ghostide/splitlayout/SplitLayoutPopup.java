package ir.hanzodev1375.ghostide.splitlayout;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import ir.hanzodev1375.components.animators.AnimationManager;
import ir.hanzodev1375.ghostide.R;
import ir.theme.ThemeManager;
import ir.theme.ThemeUtils;

public class SplitLayoutPopup {
  public interface OnSplitChangeListener {
    void onApplySplit(int rows, int cols);

    void onExitSplit();
  }

  private final PopupWindow popupWindow;
  private final MaterialButtonToggleGroup rowsGroup;
  private final MaterialButtonToggleGroup colsGroup;
  private final SplitPreviewView preview;
  private final MaterialButton actionButton;
  private ThemeManager theme;
  private MaterialCardView root;
  private int selectedRows = 1;
  private int selectedCols = 2;
  private OnSplitChangeListener listener;

  public SplitLayoutPopup(Context context) {
    View content = LayoutInflater.from(context).inflate(R.layout.popup_split_layout, null);

    rowsGroup = content.findViewById(R.id.rowsToggleGroup);
    colsGroup = content.findViewById(R.id.colsToggleGroup);
    preview = content.findViewById(R.id.splitPreview);
    actionButton = content.findViewById(R.id.splitActionButton);
    root = content.findViewById(R.id.cardRoot);

    popupWindow =
        new PopupWindow(
            content,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true);
    popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    if (!AnimationManager.getInstance(context).areAnimationsEnabled()) {
      popupWindow.setAnimationStyle(0);
      actionButton.clearAnimation();
    } else popupWindow.setAnimationStyle(R.style.SplitPopupAnimation);

    popupWindow.setOutsideTouchable(true);
    popupWindow.setElevation(dp(context, 12f));

    rowsGroup.addOnButtonCheckedListener(
        (group, checkedId, isChecked) -> {
          if (!isChecked) return;
          selectedRows = rowsValueOf(checkedId);
          onSelectionChanged();
        });
    colsGroup.addOnButtonCheckedListener(
        (group, checkedId, isChecked) -> {
          if (!isChecked) return;
          selectedCols = colsValueOf(checkedId);
          onSelectionChanged();
        });

    actionButton.setOnClickListener(
        v -> {
          if (selectedRows == 1 && selectedCols == 1) {
            if (listener != null) listener.onExitSplit();
          } else {
            if (listener != null) listener.onApplySplit(selectedRows, selectedCols);
          }
          popupWindow.dismiss();
        });

    rowsGroup.check(R.id.row1Button);
    colsGroup.check(R.id.col2Button);
    theme = new ThemeManager(context);
    var themeUtils = new ThemeUtils(theme);
    var get = themeUtils.getTheme().getEditor();
    if (get == null) {
      return;
    }
    root.setCardBackgroundColor(Color.parseColor(get.getCompletionWndBackground()));
    root.setStrokeColor(Color.parseColor(get.getCompletionWndCorner()));
    actionButton.setIconTint(
        ColorStateList.valueOf(Color.parseColor(get.getCompletionWndTextPrimary())));
    actionButton.setBackgroundTintList(
        ColorStateList.valueOf(Color.parseColor(get.getCompletionWndBackground())));
    onSelectionChanged();
  }

  public void setOnSplitChangeListener(OnSplitChangeListener listener) {
    this.listener = listener;
  }

  public void setCurrentState(boolean isSplit, int rows, int cols) {
    selectedRows = isSplit ? clamp(rows) : 1;
    selectedCols = isSplit ? clamp(cols) : 2;
    rowsGroup.check(rowsButtonId(selectedRows));
    colsGroup.check(colsButtonId(selectedCols));
    onSelectionChanged();
  }

  public void show(View anchor) {
    popupWindow.showAsDropDown(anchor, 0, dpInt(anchor.getContext(), 8f), Gravity.END);
  }

  public void dismiss() {
    if (popupWindow.isShowing()) popupWindow.dismiss();
  }

  private void onSelectionChanged() {
    preview.setGrid(selectedRows, selectedCols);
    boolean willExit = selectedRows == 1 && selectedCols == 1;
    Context context = actionButton.getContext();
    actionButton.setText(
        willExit
            ? context.getString(R.string.split_layout_exit)
            : context.getString(R.string.split_layout_apply));
    actionButton.setIcon(
        ContextCompat.getDrawable(
            context, willExit ? R.drawable.ic_single_pane : R.drawable.ic_layout_grid));
  }

  private int clamp(int value) {
    return Math.max(1, Math.min(3, value));
  }

  private int rowsValueOf(int checkedId) {
    if (checkedId == R.id.row2Button) return 2;
    if (checkedId == R.id.row3Button) return 3;
    return 1;
  }

  private int colsValueOf(int checkedId) {
    if (checkedId == R.id.col2Button) return 2;
    if (checkedId == R.id.col3Button) return 3;
    return 1;
  }

  private int rowsButtonId(int rows) {
    if (rows == 2) return R.id.row2Button;
    if (rows == 3) return R.id.row3Button;
    return R.id.row1Button;
  }

  private int colsButtonId(int cols) {
    if (cols == 2) return R.id.col2Button;
    if (cols == 3) return R.id.col3Button;
    return R.id.col1Button;
  }

  private float dp(Context context, float value) {
    return value * context.getResources().getDisplayMetrics().density;
  }

  private int dpInt(Context context, float value) {
    return (int) dp(context, value);
  }
}
