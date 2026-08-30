package ir.hanzodev1375.components;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.TransitionManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.ColorUtils;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.transition.platform.MaterialSharedAxis;
import ir.hanzodev1375.components.utils.ComponentsPrefs;

@MainThread
public class SearchLayout extends FrameLayout {

  private EditText editText;
  private ImageButton clearButton;
  private ImageView searchIcon;
  private OnSearchListener onSearchListener;
  private OnTextChangedListener onTextChangedListener;
  ComponentsPrefs setting;

  public SearchLayout(@NonNull Context context) {
    super(context);
    init(context);
  }

  public SearchLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }

  private void init(Context context) {
    setting = new ComponentsPrefs(context);
    LayoutInflater.from(context).inflate(R.layout.search_layout, this, true);

    View rootView = findViewById(R.id.rootView);
    editText = findViewById(R.id.etSearch);
    clearButton = findViewById(R.id.btnClear);
    searchIcon = findViewById(R.id.ivSearchIcon);
    clearButton.setVisibility(View.INVISIBLE);
    clearButton.setAlpha(0f);

    setVisibility(GONE);
    setupListeners();

    GradientDrawable gd =
        (GradientDrawable) ResourcesCompat.getDrawable(getResources(), R.drawable.bg_shape, null);
    if (gd == null) return;

    post(
        () -> {
          gd.setColor(
              setting.isShowBackground()
                  ? ColorUtils.setAlphaComponent(
                      MaterialColors.getColor(rootView, R.attr.colorSurfaceContainerHigh), 90)
                  : MaterialColors.getColor(rootView, R.attr.colorSurfaceContainerHigh));
          gd.setStroke(
              2,
              setting.isShowBackground()
                  ? ColorUtils.setAlphaComponent(
                      MaterialColors.getColor(rootView, R.attr.colorOutlineVariant), 90)
                  : MaterialColors.getColor(rootView, R.attr.colorOutlineVariant));

          rootView.setBackground(gd);
        });
  }

  private void setupListeners() {
    editText.addTextChangedListener(
        new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {
            String text = s.toString();
            showClearButton(!text.isEmpty());
            if (onTextChangedListener != null) {
              onTextChangedListener.onTextChanged(text);
            }
          }

          @Override
          public void afterTextChanged(Editable s) {}
        });

    clearButton.setOnClickListener(
        v -> {
          clear();
          requestFocusForEditText();
        });

    editText.setOnEditorActionListener(
        (v, actionId, event) -> {
          if (actionId == EditorInfo.IME_ACTION_SEARCH) {
            performSearch();
            return true;
          }
          return false;
        });

    searchIcon.setOnClickListener(v -> performSearch());
  }

  private void showClearButton(boolean show) {
    clearButton.setAlpha(1f);
    clearButton.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
  }

  private void performSearch() {
    String query = editText.getText().toString();
    if (onSearchListener != null && !query.trim().isEmpty()) {
      onSearchListener.onSearch(query);
    }
  }

  // متدهای عمومی
  public void setOnSearchListener(OnSearchListener listener) {
    this.onSearchListener = listener;
  }

  public void setOnTextChangedListener(OnTextChangedListener listener) {
    this.onTextChangedListener = listener;
  }

  public String getQuery() {
    return editText.getText().toString();
  }

  public void setQuery(String query) {
    editText.setText(query);
    editText.setSelection(query.length());
  }

  public void clear() {
    editText.getText().clear();
  }

  public void requestFocusForEditText() {
    editText.requestFocus();
  }

  public interface OnSearchListener {
    void onSearch(String query);
  }

  public interface OnTextChangedListener {
    void onTextChanged(String text);
  }

  public void setIconClose(int icon) {
    if (icon == 0) {
      throw new IllegalArgumentException("icon res not found call setIconClose(#int.class)");
    } else {
      clearButton.setImageResource(icon);
    }
  }

  public void setIconSearch(int icon) {
    if (icon == 0) {
      throw new IllegalArgumentException("icon res not found call setIconSearch(#int.class)");
    } else searchIcon.setImageResource(icon);
  }

  public boolean isShow() {
    return getVisibility() == VISIBLE;
  }

  public void show() {
    var material = new MaterialSharedAxis(MaterialSharedAxis.Z, true);
    if (getParent() instanceof ViewGroup) {
      TransitionManager.beginDelayedTransition((ViewGroup) getParent(), material);
    }
    setVisibility(VISIBLE);
  }

  public void hide() {
    var material = new MaterialSharedAxis(MaterialSharedAxis.Z, false);
    if (getParent() instanceof ViewGroup) {
      TransitionManager.beginDelayedTransition((ViewGroup) getParent(), material);
    }
    setVisibility(GONE);
  }
}
