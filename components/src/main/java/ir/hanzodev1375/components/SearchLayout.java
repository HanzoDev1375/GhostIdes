package ir.hanzodev1375.components;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.TransitionManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.transition.platform.MaterialSharedAxis;
import ir.hanzodev1375.ghostide.codeeditors.setting.PreferencesUtils;

@MainThread
public class SearchLayout extends FrameLayout {

  private EditText editText;
  private ImageButton clearButton;
  private ImageView searchIcon;
  private OnSearchListener onSearchListener;
  private OnTextChangedListener onTextChangedListener;
  PreferencesUtils setting;

  public SearchLayout(@NonNull Context context) {
    super(context);
    init(context, null, 0);
  }

  public SearchLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    init(context, attrs, 0);
  }

  private void init(Context context, AttributeSet attrs, int defStyleAttr) {
    setting = new PreferencesUtils(getContext());
    
    LayoutInflater.from(context).inflate(R.layout.search_layout, this, true);

    editText = findViewById(R.id.etSearch);
    clearButton = findViewById(R.id.btnClear);
    searchIcon = findViewById(R.id.ivSearchIcon);
    clearButton.setVisibility(View.INVISIBLE);
    clearButton.setAlpha(0f);
    setVisibility(GONE);
    setupListeners();
    
    View v = findViewById(R.id.rootView);
    //  if(setting.isShowBackground())
    
    GradientDrawable gd = (GradientDrawable) v.getBackground().mutate();
    gd.setColor(
        setting.isShowBackground()
            ? ColorUtils.setAlphaComponent(
                MaterialColors.getColor(v, R.attr.colorSurfaceContainerHigh), 90)
            : MaterialColors.getColor(v, R.attr.colorSurfaceContainerHigh));
    gd.setStroke(
        2,
        setting.isShowBackground()
            ? ColorUtils.setAlphaComponent(
                MaterialColors.getColor(v, R.attr.colorOutlineVariant), 90)
            : MaterialColors.getColor(v, R.attr.colorOutlineVariant));
            
  }

  private void setupListeners() {
    editText.addTextChangedListener(
        new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {
            String text = s.toString();
            if (text.isEmpty()) {
              animateClearButton(false);
            } else {
              animateClearButton(true);
            }
            if (onTextChangedListener != null) {
              onTextChangedListener.onTextChanged(text);
            }
          }

          @Override
          public void afterTextChanged(Editable s) {}
        });

    clearButton.setOnClickListener(
        v -> {
          editText.getText().clear();
          editText.requestFocus();
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

  /**
   * نمایش (فید این) یا نامرئی سازی (فید اوت) دکمه Clear
   *
   * @param show true = VISIBLE + fade in, false = INVISIBLE + fade out
   */
  private void animateClearButton(boolean show) {
    clearButton.animate().cancel();

    if (show) {
      if (clearButton.getVisibility() != View.VISIBLE) {
        clearButton.setVisibility(View.VISIBLE);
        clearButton.setAlpha(0f);
      }
      clearButton
          .animate()
          .alpha(1f)
          .setDuration(200)
          .setInterpolator(new DecelerateInterpolator())
          .start();
    } else {
      clearButton
          .animate()
          .alpha(0f)
          .setDuration(150)
          .setInterpolator(new AccelerateInterpolator())
          .withEndAction(() -> clearButton.setVisibility(View.INVISIBLE))
          .start();
    }
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
    TransitionManager.beginDelayedTransition(this, material);
    setVisibility(VISIBLE);
  }

  public void hide() {
    var material = new MaterialSharedAxis(MaterialSharedAxis.Z, false);
    TransitionManager.beginDelayedTransition(this, material);
    setVisibility(GONE);
  }
}
