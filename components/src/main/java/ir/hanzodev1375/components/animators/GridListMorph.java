package ir.hanzodev1375.components.animators;

import android.util.Log;
import android.view.View;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import ir.hanzodev1375.components.animators.SpringInterpolator;

public class GridListMorph {

  private static final String TAG = "GridListMorph";

  private static final long DEFAULT_DURATION = 320L;
  private static final long DEFAULT_STAGGER = 26L;
  private static final float DEFAULT_FOLD_SCALE = 0.86f;

  private GridListMorph() {}

  /**
   * Switches the recycler view between list and grid layout managers with a springy, staggered
   * pulse. Nothing happens if the layout manager type does not change.
   */
  public static void toggle(RecyclerView recyclerView, int spanCount) {
    try {
      if (recyclerView == null || recyclerView.getLayoutManager() == null) return;
      RecyclerView.LayoutManager current = recyclerView.getLayoutManager();
      boolean isGrid = current instanceof GridLayoutManager;
      if (spanCount > 1 && isGrid && ((GridLayoutManager) current).getSpanCount() == spanCount) {
        return;
      }
      if (spanCount <= 1 && current instanceof LinearLayoutManager && !isGrid) {
        return;
      }
      RecyclerView.LayoutManager target;
      if (spanCount > 1) {
        target = new GridLayoutManager(recyclerView.getContext(), spanCount);
      } else {
        target = new LinearLayoutManager(recyclerView.getContext());
      }
      morph(recyclerView, target);
    } catch (Throwable tr) {
      Log.w(TAG, "toggle failed", tr);
    }
  }

  /**
   * Morphs the recycler view to the given layout manager: the visible items shrink/fade out in a
   * stagger, the layout manager is swapped and the new items bounce in with a spring overshoot.
   */
  public static void morph(RecyclerView recyclerView, RecyclerView.LayoutManager target) {
    morph(recyclerView, target, DEFAULT_DURATION, DEFAULT_STAGGER, DEFAULT_FOLD_SCALE);
  }

  public static void morph(
      final RecyclerView recyclerView,
      final RecyclerView.LayoutManager target,
      final long duration,
      final long stagger,
      final float foldScale) {
    if (recyclerView == null || target == null) return;
    try {

      final int visibleCount = recyclerView.getChildCount();
      if (visibleCount <= 0) {
        swapLayout(recyclerView, target);
        return;
      }

      final long[] delays = new long[visibleCount];
      for (int i = 0; i < visibleCount; i++) {
        delays[i] = i * stagger;
      }

      final View[] children = new View[visibleCount];
      for (int i = 0; i < visibleCount; i++) {
        children[i] = recyclerView.getChildAt(i);
      }

      for (int i = 0; i < visibleCount; i++) {
        final View child = children[i];
        if (child == null) continue;
        child
            .animate()
            .scaleX(foldScale)
            .scaleY(foldScale)
            .alpha(0.4f)
            .setDuration(duration / 2)
            .setStartDelay(delays[i])
            .setInterpolator(CubicBezierInterpolator.EASE_OUT)
            .start();
      }

      final long foldOutEnd = delays[visibleCount - 1] + duration / 2;
      recyclerView.postDelayed(
          () -> {
            swapLayout(recyclerView, target);
            recyclerView.post(
                () -> {
                  final int newVisible = recyclerView.getChildCount();
                  for (int i = 0; i < newVisible; i++) {
                    final View child = recyclerView.getChildAt(i);
                    if (child == null) continue;
                    child.setScaleX(foldScale);
                    child.setScaleY(foldScale);
                    child.setAlpha(0f);
                    child
                        .animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .alpha(1f)
                        .setDuration(duration)
                        .setStartDelay(i * stagger)
                        .setInterpolator(new SpringInterpolator())
                        .start();
                  }
                });
          },
          foldOutEnd + 20);
    } catch (Throwable tr) {
      Log.w(TAG, "morph failed", tr);
      try {
        swapLayout(recyclerView, target);
      } catch (Throwable tr2) {
        Log.w(TAG, "secondary swap failed", tr2);
      }
    }
  }

  private static void swapLayout(RecyclerView recyclerView, RecyclerView.LayoutManager target) {
    recyclerView.setLayoutManager(target);
    recyclerView.invalidate();
  }
}
