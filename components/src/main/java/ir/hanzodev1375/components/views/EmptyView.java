package ir.hanzodev1375.components.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.RecyclerView;
import ir.hanzodev1375.components.R;

public class EmptyView extends LinearLayout {

  private ImageView iconView;
  private TextView titleView;
  private TextView descriptionView;

  private int iconRes = R.drawable.ic_empty_state;
  private RecyclerView.Adapter<?> boundAdapter;

  private final RecyclerView.AdapterDataObserver dataObserver =
      new RecyclerView.AdapterDataObserver() {
        @Override
        public void onChanged() {
          syncWithAdapter();
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
          syncWithAdapter();
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
          syncWithAdapter();
        }
      };

  public EmptyView(Context context) {
    this(context, null);
  }

  public EmptyView(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public EmptyView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(context, attrs);
  }

  private void init(Context context, AttributeSet attrs) {
    setOrientation(LinearLayout.VERTICAL);
    setGravity(Gravity.CENTER);
    LayoutInflater.from(context).inflate(R.layout.view_empty_state, this, true);
    iconView = findViewById(R.id.emptyIcon);
    titleView = findViewById(R.id.emptyTitle);
    descriptionView = findViewById(R.id.emptyDescription);

    String title = context.getString(R.string.empty_view_default_title);
    String description = context.getString(R.string.empty_view_default_description);

    if (attrs != null) {
      TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.EmptyView);
      int attrIcon = a.getResourceId(R.styleable.EmptyView_emptyIcon, -1);
      if (attrIcon != -1) iconRes = attrIcon;
      CharSequence attrTitle = a.getText(R.styleable.EmptyView_emptyTitle);
      if (attrTitle != null) title = attrTitle.toString();
      CharSequence attrDescription = a.getText(R.styleable.EmptyView_emptyDescription);
      if (attrDescription != null) description = attrDescription.toString();
      a.recycle();
    }

    iconView.setImageResource(iconRes);
    titleView.setText(title);
    descriptionView.setText(description);
    setVisibility(GONE);
  }

  public void setIconRes(@DrawableRes int res) {
    this.iconRes = res;
    iconView.setImageResource(res);
  }

  public @DrawableRes int getIconRes() {
    return iconRes;
  }

  public void setTitle(CharSequence text) {
    titleView.setText(text);
  }

  public void setTitleRes(@StringRes int id) {
    titleView.setText(id);
  }

  public CharSequence getTitle() {
    return titleView.getText();
  }

  public void setDescription(CharSequence text) {
    descriptionView.setText(text);
  }

  public void setDescriptionRes(@StringRes int id) {
    descriptionView.setText(id);
  }

  public CharSequence getDescription() {
    return descriptionView.getText();
  }

  public boolean isShowing() {
    return getVisibility() == VISIBLE;
  }

  public void show() {
    setVisibility(VISIBLE);
  }

  public void hide() {
    setVisibility(GONE);
  }

  public void bindTo(RecyclerView recyclerView) {
    unbind();
    if (recyclerView == null) return;
    RecyclerView.Adapter<?> adapter = recyclerView.getAdapter();
    if (adapter == null) return;
    boundAdapter = adapter;
    adapter.registerAdapterDataObserver(dataObserver);
    syncWithAdapter();
  }

  public void unbind() {
    if (boundAdapter != null) {
      boundAdapter.unregisterAdapterDataObserver(dataObserver);
      boundAdapter = null;
    }
  }

  private void syncWithAdapter() {
    boolean empty = boundAdapter == null || boundAdapter.getItemCount() == 0;
    setVisibility(empty ? VISIBLE : GONE);
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    unbind();
  }
}
