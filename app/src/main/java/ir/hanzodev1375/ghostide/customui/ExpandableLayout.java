package ir.hanzodev1375.ghostide.customui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import ir.hanzodev1375.components.animators.AnimationManager;
import ir.hanzodev1375.ghostide.R;
import ir.hanzodev1375.ghostide.codeeditors.setting.PreferencesUtils;

public class ExpandableLayout extends LinearLayout {
  private TextView titleView;
  private ImageView arrowIcon;
  private RecyclerView recyclerView;
  private boolean isExpanded = false;
  private PreferencesUtils appsetting;
  private MaterialCardView card;

  public ExpandableLayout(@NonNull Context context) {
    super(context);
    init(context);
  }

  public ExpandableLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }

  public ExpandableLayout(
      @NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(context);
  }

  private void init(Context context) {
    setOrientation(VERTICAL);
    LayoutInflater.from(context).inflate(R.layout.layout_expandable, this, true);
    titleView = findViewById(R.id.expandable_title);
    arrowIcon = findViewById(R.id.expandable_arrow);
    recyclerView = findViewById(R.id.expandable_recycler);
    card = findViewById(R.id.cardEx);
    findViewById(R.id.expandable_header).setOnClickListener(v -> toggle());
    recyclerView.setVisibility(GONE);
    appsetting = new PreferencesUtils(context);
    stepCard();
  }

  public void setTitle(String title) {
    titleView.setText(title);
  }

  public RecyclerView getRecyclerView() {
    return recyclerView;
  }

  void stepCard() {
    int colororgin = MaterialColors.getColor(card, R.attr.colorSurface);
    int strokecolor = MaterialColors.getColor(card, R.attr.colorOnSurfaceVariant);
    card.setCardBackgroundColor(
        appsetting.isShowBackground() ? ColorUtils.setAlphaComponent(colororgin, 128) : colororgin);
    card.setStrokeColor(
        appsetting.isShowBackground()
            ? ColorUtils.setAlphaComponent(strokecolor, 128)
            : strokecolor);
  }

  public void toggle() {
    if (isExpanded) collapse();
    else expand();
  }

  public void expand() {
    if (AnimationManager.getInstance(getContext()).areAnimationsEnabled()) {
      if (isExpanded) return;
      isExpanded = true;

      recyclerView.setVisibility(View.VISIBLE);
      recyclerView.setAlpha(0f);
      recyclerView.setTranslationY(-20f);

      recyclerView.postOnAnimation(
          () -> {
            recyclerView.animate().alpha(1f).translationY(0f).setDuration(150).start();
          });

      arrowIcon.animate().rotation(90).setDuration(150).start();
    } else recyclerView.setVisibility(VISIBLE);
  }

  public void collapse() {
    if (AnimationManager.getInstance(getContext()).areAnimationsEnabled()) {
      if (!isExpanded) return;
      isExpanded = false;
      recyclerView
          .animate()
          .alpha(0f)
          .translationY(-20f)
          .setDuration(150)
          .withEndAction(() -> recyclerView.setVisibility(View.GONE))
          .start();
      arrowIcon.animate().rotation(0).setDuration(150).start();
    } else recyclerView.setVisibility(GONE);
  }

  public boolean isExpanded() {
    return isExpanded;
  }
}
