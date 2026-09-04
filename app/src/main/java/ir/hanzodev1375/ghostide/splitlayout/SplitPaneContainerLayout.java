package ir.hanzodev1375.ghostide.splitlayout;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import ir.theme.M3Theme;
import ir.hanzodev1375.ghostide.models.TabModel;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;


public class SplitPaneContainerLayout extends LinearLayout {

  /** null یعنی پین اصلی (primary) فعاله */
  public interface OnActivePaneChangedListener {
    void onActivePaneChanged(@Nullable EditorPaneFragment activePane);
  }

  private View primaryPaneHost;
  private FragmentActivity hostActivity;
  private List<TabModel> sharedTabs = new ArrayList<>();
  private EditorPaneFragment.PaneActionListener actionListener;
  private OnActivePaneChangedListener activePaneChangedListener;
  private final List<EditorPaneFragment> extraPanes = new ArrayList<>();
  private EditorPaneFragment activePane = null;
  private int rows = 1;
  private int cols = 1;
  private int dividerColor = Color.GRAY;

  public SplitPaneContainerLayout(Context context) {
    super(context);
    setOrientation(VERTICAL);
  }

  public SplitPaneContainerLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
    setOrientation(VERTICAL);
  }

  public SplitPaneContainerLayout(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    setOrientation(VERTICAL);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    if (getChildCount() > 0) {
      primaryPaneHost = getChildAt(0);
    }
    dividerColor = fallback(M3Theme.outlineVariant(), Color.GRAY);
  }

  public void initialize(
      FragmentActivity activity, EditorPaneFragment.PaneActionListener listener) {
    this.hostActivity = activity;
    this.actionListener = listener;
  }

  public void setOnActivePaneChangedListener(OnActivePaneChangedListener listener) {
    this.activePaneChangedListener = listener;
  }

  /** @return پینی که کاربر آخرین بار توش لمس/تایپ کرده؛ null یعنی پین اصلی */
  @Nullable
  public EditorPaneFragment getActivePane() {
    return activePane;
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent ev) {
    if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
      updateActivePaneFromTouch(ev.getRawX(), ev.getRawY());
    }
    // فقط برای تشخیص "کجا لمس شد" استفاده می‌کنیم، هیچ‌وقت خودمون تاچ رو مصرف نمی‌کنیم
    return super.onInterceptTouchEvent(ev);
  }

  private void updateActivePaneFromTouch(float rawX, float rawY) {
    if (!isSplit()) {
      setActivePane(null);
      return;
    }
    Rect bounds = new Rect();
    int[] loc = new int[2];
    for (EditorPaneFragment pane : extraPanes) {
      View root = pane.getView();
      if (root == null) continue;
      root.getLocationOnScreen(loc);
      bounds.set(loc[0], loc[1], loc[0] + root.getWidth(), loc[1] + root.getHeight());
      if (bounds.contains((int) rawX, (int) rawY)) {
        setActivePane(pane);
        return;
      }
    }
    // اگر داخل هیچ‌کدوم از extraPanes نبود، یعنی پین اصلی لمس شده
    setActivePane(null);
  }

  private void setActivePane(@Nullable EditorPaneFragment pane) {
    if (activePane == pane) return;
    activePane = pane;
    if (activePaneChangedListener != null) {
      activePaneChangedListener.onActivePaneChanged(activePane);
    }
  }

  public boolean isSplit() {
    return rows * cols > 1;
  }

  public int getRows() {
    return rows;
  }

  public int getCols() {
    return cols;
  }


  public void applySplit(int newRows, int newCols, List<TabModel> currentTabs) {
    if (primaryPaneHost == null || hostActivity == null) return;
    this.sharedTabs = currentTabs != null ? currentTabs : new ArrayList<>();
    newRows = clamp(newRows);
    newCols = clamp(newCols);
    if (newRows == 1 && newCols == 1) {
      collapseToSingle();
      return;
    }
    rows = newRows;
    cols = newCols;
    rebuildGrid();
  }

  public void exitSplit() {
    applySplit(1, 1, sharedTabs);
  }

  private int clamp(int value) {
    return Math.max(1, Math.min(3, value));
  }

  private void collapseToSingle() {
    if (rows == 1 && cols == 1) return;
    setActivePane(null);
    detachExtraPanes();
    removeAllViews();
    if (primaryPaneHost.getParent() != null) {
      ((ViewGroup) primaryPaneHost.getParent()).removeView(primaryPaneHost);
    }
    addView(
        primaryPaneHost, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    rows = 1;
    cols = 1;
  }

  private void rebuildGrid() {
    setActivePane(null);
    detachExtraPanes();
    if (primaryPaneHost.getParent() != null) {
      ((ViewGroup) primaryPaneHost.getParent()).removeView(primaryPaneHost);
    }
    removeAllViews();

    boolean primaryPlaced = false;
    for (int r = 0; r < rows; r++) {
      if (r > 0) addHorizontalDivider();
      LinearLayout rowLayout = new LinearLayout(getContext());
      rowLayout.setOrientation(HORIZONTAL);
      addView(rowLayout, new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));

      for (int c = 0; c < cols; c++) {
        if (c > 0) addVerticalDivider(rowLayout);
        LinearLayout.LayoutParams cellLp =
            new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);

        if (!primaryPlaced) {
          rowLayout.addView(primaryPaneHost, cellLp);
          primaryPlaced = true;
        } else {
          FrameLayout cellHost = new FrameLayout(getContext());
          cellHost.setId(View.generateViewId());
          rowLayout.addView(cellHost, cellLp);

          EditorPaneFragment pane = EditorPaneFragment.newInstance();
          pane.setSharedData(sharedTabs, actionListener);
          hostActivity
              .getSupportFragmentManager()
              .beginTransaction()
              .replace(cellHost.getId(), pane)
              .commitNowAllowingStateLoss();
          extraPanes.add(pane);
        }
      }
    }
  }

  private void detachExtraPanes() {
    if (extraPanes.isEmpty() || hostActivity == null) return;
    var transaction = hostActivity.getSupportFragmentManager().beginTransaction();
    for (EditorPaneFragment pane : extraPanes) {
      transaction.remove(pane);
    }
    transaction.commitNowAllowingStateLoss();
    extraPanes.clear();
  }

  private void addHorizontalDivider() {
    View divider = new View(getContext());
    divider.setBackgroundColor(dividerColor);
    addView(divider, new LayoutParams(LayoutParams.MATCH_PARENT, dp(1)));
  }

  private void addVerticalDivider(LinearLayout rowLayout) {
    View divider = new View(getContext());
    divider.setBackgroundColor(dividerColor);
    rowLayout.addView(
        divider, new LinearLayout.LayoutParams(dp(1), LinearLayout.LayoutParams.MATCH_PARENT));
  }

  private int dp(float value) {
    return (int) (value * getResources().getDisplayMetrics().density);
  }


  public void notifyTabsChanged(List<TabModel> currentTabs) {
    this.sharedTabs = currentTabs != null ? currentTabs : new ArrayList<>();
    for (EditorPaneFragment pane : extraPanes) {
      pane.refreshTabs(sharedTabs);
    }
  }

  public void notifyTabDirty(String filePath, boolean dirty) {
    for (EditorPaneFragment pane : extraPanes) {
      pane.updateDirty(filePath, dirty);
    }
  }

  public void notifyGitStatus(Predicate<String> isChanged) {
    for (EditorPaneFragment pane : extraPanes) {
      pane.updateGitStatus(isChanged);
    }
  }

  private static int fallback(Integer value, int def) {
    return value != null ? value : def;
  }
}
