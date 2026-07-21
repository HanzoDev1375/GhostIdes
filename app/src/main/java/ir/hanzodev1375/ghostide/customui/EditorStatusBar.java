package ir.hanzodev1375.ghostide.customui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import ir.hanzodev1375.ghostide.R;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import ir.hanzodev1375.ghostide.databinding.EditorStatusBarBinding;

/**
 * A reusable, VSCode-style bottom editor status bar.
 *
 * <p>Shows four clickable sections — Language, Encoding, Indentation and Status — each with an icon
 * and a label, separated by thin vertical dividers, inside a rounded, outlined Material 3 style
 * container.
 *
 * <p><b>Setup</b>
 *
 * <ul>
 *   <li>Enable ViewBinding in the module's build.gradle: {@code android { buildFeatures {
 *       viewBinding true } } }
 *   <li>Requires minSdk 21+ (uses {@link RippleDrawable} and {@link ViewOutlineProvider}).
 *   <li>No Material Components library dependency — the rounded, outlined surface is built by hand
 *       with {@link GradientDrawable}.
 * </ul>
 *
 * <p><b>Note:</b> change the rounded background only through {@link #setCornerRadius}, {@link
 * #setBackgroundColor}, {@link #setStrokeColor} and {@link #setStrokeWidth}. Calling the inherited
 * {@link View#setBackground} directly replaces the drawable those methods manage.
 */
public class EditorStatusBar extends FrameLayout {

  private static final float DEFAULT_CORNER_RADIUS_DP = 18f;
  private static final float DEFAULT_STROKE_WIDTH_DP = 1f;
  private static final float DEFAULT_TEXT_SIZE_SP = 12f;
  private static final int DEFAULT_SECTION_PADDING_DP = 12;
  private static final int DEFAULT_ICON_SIZE_DP = 16;
  private static final int DEFAULT_ICON_TEXT_SPACING_DP = 6;

  @ColorInt private static final int DEFAULT_BACKGROUND_COLOR = Color.parseColor("#1C1B1F");
  @ColorInt private static final int DEFAULT_STROKE_COLOR = Color.parseColor("#33FFFFFF");
  @ColorInt private static final int DEFAULT_TEXT_COLOR = Color.parseColor("#E6E1E5");
  @ColorInt private static final int DEFAULT_ICON_TINT = Color.parseColor("#E6E1E5");
  @ColorInt private static final int DEFAULT_DIVIDER_COLOR = Color.parseColor("#33FFFFFF");
  @ColorInt private static final int DEFAULT_RIPPLE_COLOR = Color.parseColor("#1FFFFFFF");

  @ColorInt private static final int STATUS_COLOR_CONNECTED = Color.parseColor("#4CAF50");
  @ColorInt private static final int STATUS_COLOR_WARNING = Color.parseColor("#FFC107");
  @ColorInt private static final int STATUS_COLOR_ERROR = Color.parseColor("#F44336");
  @ColorInt private static final int STATUS_COLOR_IDLE = Color.parseColor("#9E9E9E");
  private static final int STATUS_DOT_SIZE_DP = 8;

  public enum StatusIndicator {
    IDLE,
    CONNECTING,
    CONNECTED,
    WARNING,
    ERROR
  }

  private EditorStatusBarBinding binding;

  private float cornerRadius;
  @ColorInt private int backgroundColorValue;
  @ColorInt private int strokeColor;
  private float strokeWidth;

  @ColorInt private int textColor;
  @ColorInt private int iconTint;
  @ColorInt private int dividerColor;
  @ColorInt private int rippleColor;

  private float textSize;
  private Typeface typeface;

  private int sectionPadding;
  private int iconSize;
  private int iconTextSpacing;

  private GradientDrawable backgroundDrawable;
  private RippleDrawable rippleLanguage;
  private RippleDrawable rippleEncoding;
  private RippleDrawable rippleIndentation;
  private RippleDrawable rippleStatus;
  private GradientDrawable statusDotDrawable;
  private StatusIndicator currentStatusIndicator = StatusIndicator.IDLE;

  public EditorStatusBar(@NonNull Context context) {
    this(context, null);
  }

  public EditorStatusBar(@NonNull Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public EditorStatusBar(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(context, attrs, defStyleAttr);
  }

  private void init(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    binding = EditorStatusBarBinding.inflate(LayoutInflater.from(context), this, true);

    cornerRadius = dpToPx(DEFAULT_CORNER_RADIUS_DP);
    backgroundColorValue = DEFAULT_BACKGROUND_COLOR;
    strokeColor = DEFAULT_STROKE_COLOR;
    strokeWidth = dpToPx(DEFAULT_STROKE_WIDTH_DP);
    textColor = DEFAULT_TEXT_COLOR;
    iconTint = DEFAULT_ICON_TINT;
    dividerColor = DEFAULT_DIVIDER_COLOR;
    rippleColor = DEFAULT_RIPPLE_COLOR;
    textSize = spToPx(DEFAULT_TEXT_SIZE_SP);
    typeface = Typeface.DEFAULT;

    sectionPadding = (int) dpToPx(DEFAULT_SECTION_PADDING_DP);
    iconSize = (int) dpToPx(DEFAULT_ICON_SIZE_DP);
    iconTextSpacing = (int) dpToPx(DEFAULT_ICON_TEXT_SPACING_DP);

    if (attrs != null) {
      TypedArray a =
          context.obtainStyledAttributes(attrs, R.styleable.EditorStatusBar, defStyleAttr, 0);
      try {
        cornerRadius = a.getDimension(R.styleable.EditorStatusBar_esb_cornerRadius, cornerRadius);
        backgroundColorValue =
            a.getColor(R.styleable.EditorStatusBar_esb_backgroundColor, backgroundColorValue);
        strokeColor = a.getColor(R.styleable.EditorStatusBar_esb_strokeColor, strokeColor);
        strokeWidth = a.getDimension(R.styleable.EditorStatusBar_esb_strokeWidth, strokeWidth);

        textColor = a.getColor(R.styleable.EditorStatusBar_esb_textColor, textColor);
        iconTint = a.getColor(R.styleable.EditorStatusBar_esb_iconTint, iconTint);
        dividerColor = a.getColor(R.styleable.EditorStatusBar_esb_dividerColor, dividerColor);
        rippleColor = a.getColor(R.styleable.EditorStatusBar_esb_rippleColor, rippleColor);

        textSize = a.getDimension(R.styleable.EditorStatusBar_esb_textSize, textSize);
        sectionPadding =
            a.getDimensionPixelSize(R.styleable.EditorStatusBar_esb_sectionPadding, sectionPadding);
        iconSize = a.getDimensionPixelSize(R.styleable.EditorStatusBar_esb_iconSize, iconSize);
        iconTextSpacing =
            a.getDimensionPixelSize(
                R.styleable.EditorStatusBar_esb_iconTextSpacing, iconTextSpacing);

        String fontFamily = a.getString(R.styleable.EditorStatusBar_esb_fontFamily);
        if (fontFamily != null) {
          typeface = Typeface.create(fontFamily, Typeface.NORMAL);
        }

        applyInitialText(a);
        applyInitialIcons(a);
        applyInitialVisibility(a);
      } finally {
        a.recycle();
      }
    }

    setupBackground();
    setupRipples();
    applyTextColor();
    applyIconTintToAll();
    applyTextSizeToAll();
    applyTypefaceToAll();
    applySectionPaddingToAll();
    applyIconSizeToAll();
    applyIconTextSpacingToAll();
    applyDividerColor();
    updateDividerVisibility();
    ensureStatusDotDrawable();
  }

  private void applyInitialText(TypedArray a) {
    String languageText = a.getString(R.styleable.EditorStatusBar_esb_languageText);
    String encodingText = a.getString(R.styleable.EditorStatusBar_esb_encodingText);
    String indentationText = a.getString(R.styleable.EditorStatusBar_esb_indentationText);
    String statusText = a.getString(R.styleable.EditorStatusBar_esb_statusText);
    if (languageText != null) binding.textLanguage.setText(languageText);
    if (encodingText != null) binding.textEncoding.setText(encodingText);
    if (indentationText != null) binding.textIndentation.setText(indentationText);
    if (statusText != null) binding.textStatus.setText(statusText);
  }

  private void applyInitialIcons(TypedArray a) {
    Drawable languageIcon = a.getDrawable(R.styleable.EditorStatusBar_esb_languageIcon);
    Drawable encodingIcon = a.getDrawable(R.styleable.EditorStatusBar_esb_encodingIcon);
    Drawable indentationIcon = a.getDrawable(R.styleable.EditorStatusBar_esb_indentationIcon);
    Drawable statusIcon = a.getDrawable(R.styleable.EditorStatusBar_esb_statusIcon);
    if (languageIcon != null) binding.iconLanguage.setImageDrawable(languageIcon);
    if (encodingIcon != null) binding.iconEncoding.setImageDrawable(encodingIcon);
    if (indentationIcon != null) binding.iconIndentation.setImageDrawable(indentationIcon);
    if (statusIcon != null) binding.iconStatus.setImageDrawable(statusIcon);
  }

  private void applyInitialVisibility(TypedArray a) {
    if (!a.getBoolean(R.styleable.EditorStatusBar_esb_languageVisible, true)) {
      binding.sectionLanguage.setVisibility(GONE);
    }
    if (!a.getBoolean(R.styleable.EditorStatusBar_esb_encodingVisible, true)) {
      binding.sectionEncoding.setVisibility(GONE);
    }
    if (!a.getBoolean(R.styleable.EditorStatusBar_esb_indentationVisible, true)) {
      binding.sectionIndentation.setVisibility(GONE);
    }
    if (!a.getBoolean(R.styleable.EditorStatusBar_esb_statusVisible, true)) {
      binding.sectionStatus.setVisibility(GONE);
    }
  }

  private void setupBackground() {
    backgroundDrawable = new GradientDrawable();
    backgroundDrawable.setShape(GradientDrawable.RECTANGLE);
    backgroundDrawable.setColor(backgroundColorValue);
    backgroundDrawable.setCornerRadius(cornerRadius);
    backgroundDrawable.setStroke(Math.round(strokeWidth), strokeColor);
    binding.getRoot().setBackground(backgroundDrawable);
    setBackgroundColor(Color.TRANSPARENT);
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    invalidateOutline();
  }

  private void setupRipples() {
    rippleLanguage = createRipple();
    rippleEncoding = createRipple();
    rippleIndentation = createRipple();
    rippleStatus = createRipple();

    binding.sectionLanguage.setBackground(rippleLanguage);
    binding.sectionEncoding.setBackground(rippleEncoding);
    binding.sectionIndentation.setBackground(rippleIndentation);
    binding.sectionStatus.setBackground(rippleStatus);
  }

  private RippleDrawable createRipple() {
    ColorStateList colorStateList = ColorStateList.valueOf(rippleColor);

    return new RippleDrawable(colorStateList, null, new ColorDrawable(Color.WHITE));
  }

  private void applyRippleColor() {
    ColorStateList colorStateList = ColorStateList.valueOf(rippleColor);
    rippleLanguage.setColor(colorStateList);
    rippleEncoding.setColor(colorStateList);
    rippleIndentation.setColor(colorStateList);
    rippleStatus.setColor(colorStateList);
  }

  private void applyTextColor() {
    binding.textLanguage.setTextColor(textColor);
    binding.textEncoding.setTextColor(textColor);
    binding.textIndentation.setTextColor(textColor);
    binding.textStatus.setTextColor(textColor);
  }

  private void applyIconTintToAll() {
    applyIconTint(binding.iconLanguage);
    applyIconTint(binding.iconEncoding);
    applyIconTint(binding.iconIndentation);
  }

  private void applyIconTint(ImageView imageView) {

    if (imageView.getDrawable() != null) {
      imageView.setColorFilter(iconTint, PorterDuff.Mode.SRC_IN);
    }
  }

  private void applyTextSizeToAll() {
    binding.textLanguage.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
    binding.textEncoding.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
    binding.textIndentation.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
    binding.textStatus.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
  }

  private void applyTypefaceToAll() {
    binding.textLanguage.setTypeface(typeface);
    binding.textEncoding.setTypeface(typeface);
    binding.textIndentation.setTypeface(typeface);
    binding.textStatus.setTypeface(typeface);
  }

  private void applySectionPaddingToAll() {
    applySectionPadding(binding.sectionLanguage);
    applySectionPadding(binding.sectionEncoding);
    applySectionPadding(binding.sectionIndentation);
    applySectionPadding(binding.sectionStatus);
  }

  private void applySectionPadding(View section) {
    section.setPaddingRelative(
        sectionPadding, section.getPaddingTop(), sectionPadding, section.getPaddingBottom());
  }

  private void applyIconSizeToAll() {
    applyIconSize(binding.iconLanguage);
    applyIconSize(binding.iconEncoding);
    applyIconSize(binding.iconIndentation);
    applyIconSize(binding.iconStatus);
  }

  private void applyIconSize(ImageView icon) {
    ViewGroup.LayoutParams params = icon.getLayoutParams();
    params.width = iconSize;
    params.height = iconSize;
    icon.setLayoutParams(params);
  }

  private void applyIconTextSpacingToAll() {
    applyIconTextSpacing(binding.iconLanguage);
    applyIconTextSpacing(binding.iconEncoding);
    applyIconTextSpacing(binding.iconIndentation);
    applyIconTextSpacing(binding.iconStatus);
  }

  private void applyIconTextSpacing(ImageView icon) {
    ViewGroup.LayoutParams lp = icon.getLayoutParams();
    if (lp instanceof MarginLayoutParams) {
      ((MarginLayoutParams) lp).setMarginEnd(iconTextSpacing);
      icon.setLayoutParams(lp);
    }
  }

  private void applyDividerColor() {
    binding.dividerLanguageEncoding.setBackgroundColor(dividerColor);
    binding.dividerEncodingIndentation.setBackgroundColor(dividerColor);
    binding.dividerIndentationStatus.setBackgroundColor(dividerColor);
  }

  /**
   * A divider is shown only when both the section before and after it are visible, so hiding a
   * section never leaves a divider floating next to nothing.
   */
  private void updateDividerVisibility() {
    boolean lang = isLanguageVisible();
    boolean enc = isEncodingVisible();
    boolean ind = isIndentationVisible();
    boolean stat = isStatusVisible();

    binding.dividerLanguageEncoding.setVisibility(lang && enc ? VISIBLE : GONE);
    binding.dividerEncodingIndentation.setVisibility(enc && ind ? VISIBLE : GONE);
    binding.dividerIndentationStatus.setVisibility(ind && stat ? VISIBLE : GONE);
  }

  /**
   * @param radiusPx corner radius, in pixels.
   */
  public void setCornerRadius(float radiusPx) {
    this.cornerRadius = radiusPx;
    if (backgroundDrawable != null) {
      backgroundDrawable.setCornerRadius(radiusPx);
    }
    invalidateOutline();
  }

  public float getCornerRadius() {
    return cornerRadius;
  }


  @ColorInt
  public int getBackgroundColor() {
    return backgroundColorValue;
  }

  public void setBackgroundColorValue(@ColorInt int backgroundColorValue) {
    this.backgroundColorValue = backgroundColorValue;
    if (backgroundDrawable != null) {
      backgroundDrawable.setColor(backgroundColorValue);
    }
  }

  public void setStrokeColor(@ColorInt int color) {
    this.strokeColor = color;
    if (backgroundDrawable != null) {
      backgroundDrawable.setStroke(Math.round(strokeWidth), strokeColor);
    }
  }

  @ColorInt
  public int getStrokeColor() {
    return strokeColor;
  }

  /**
   * @param widthPx stroke width, in pixels.
   */
  public void setStrokeWidth(float widthPx) {
    this.strokeWidth = widthPx;
    if (backgroundDrawable != null) {
      backgroundDrawable.setStroke(Math.round(strokeWidth), strokeColor);
    }
  }

  public float getStrokeWidth() {
    return strokeWidth;
  }

  public void setLanguageText(String text) {
    binding.textLanguage.setText(text);
  }

  public String getLanguageText() {
    return binding.textLanguage.getText().toString();
  }

  public void setEncodingText(String text) {
    binding.textEncoding.setText(text);
  }

  public String getEncodingText() {
    return binding.textEncoding.getText().toString();
  }

  public void setIndentationText(String text) {
    binding.textIndentation.setText(text);
  }

  public String getIndentationText() {
    return binding.textIndentation.getText().toString();
  }

  public void setStatusText(String text) {
    binding.textStatus.setText(text);
  }

  public String getStatusText() {
    return binding.textStatus.getText().toString();
  }

  public void setLanguageIcon(@Nullable Drawable drawable) {
    binding.iconLanguage.setImageDrawable(drawable);
    applyIconTint(binding.iconLanguage);
  }

  @Nullable
  public Drawable getLanguageIcon() {
    return binding.iconLanguage.getDrawable();
  }

  public void setEncodingIcon(@Nullable Drawable drawable) {
    binding.iconEncoding.setImageDrawable(drawable);
    applyIconTint(binding.iconEncoding);
  }

  @Nullable
  public Drawable getEncodingIcon() {
    return binding.iconEncoding.getDrawable();
  }

  public void setIndentationIcon(@Nullable Drawable drawable) {
    binding.iconIndentation.setImageDrawable(drawable);
    applyIconTint(binding.iconIndentation);
  }

  @Nullable
  public Drawable getIndentationIcon() {
    return binding.iconIndentation.getDrawable();
  }

  public void setStatusIcon(@Nullable Drawable drawable) {
    binding.iconStatus.setImageDrawable(drawable);
    applyIconTint(binding.iconStatus);
  }

  @Nullable
  public Drawable getStatusIcon() {
    return binding.iconStatus.getDrawable();
  }

  
  public void setStatusIndicator(@NonNull StatusIndicator state, @Nullable String label) {
    ensureStatusDotDrawable();
    currentStatusIndicator = state;

    int color;
    switch (state) {
      case CONNECTED:
        color = STATUS_COLOR_CONNECTED;
        break;
      case CONNECTING:
      case WARNING:
        color = STATUS_COLOR_WARNING;
        break;
      case ERROR:
        color = STATUS_COLOR_ERROR;
        break;
      case IDLE:
      default:
        color = STATUS_COLOR_IDLE;
        break;
    }

    statusDotDrawable.setColor(color);

    binding.iconStatus.clearColorFilter();

    if (label != null) {
      binding.textStatus.setText(label);
    }
  }

  /** Overload بدون تغییر متن - فقط رنگِ نقطه آپدیت می شه. */
  public void setStatusIndicator(@NonNull StatusIndicator state) {
    setStatusIndicator(state, null);
  }

  @NonNull
  public StatusIndicator getStatusIndicator() {
    return currentStatusIndicator;
  }

  private void ensureStatusDotDrawable() {
    if (statusDotDrawable != null) {
      return;
    }
    statusDotDrawable = new GradientDrawable();
    statusDotDrawable.setShape(GradientDrawable.OVAL);
    int dotSizePx = (int) dpToPx(STATUS_DOT_SIZE_DP);
    statusDotDrawable.setSize(dotSizePx, dotSizePx);
    statusDotDrawable.setColor(STATUS_COLOR_IDLE);
    binding.iconStatus.setImageDrawable(statusDotDrawable);
  }

  public void setTextColor(@ColorInt int color) {
    this.textColor = color;
    applyTextColor();
  }

  @ColorInt
  public int getTextColor() {
    return textColor;
  }

  public void setIconTint(@ColorInt int color) {
    this.iconTint = color;
    applyIconTintToAll();
  }

  @ColorInt
  public int getIconTint() {
    return iconTint;
  }

  public void setDividerColor(@ColorInt int color) {
    this.dividerColor = color;
    applyDividerColor();
  }

  @ColorInt
  public int getDividerColor() {
    return dividerColor;
  }

  public void setRippleColor(@ColorInt int color) {
    this.rippleColor = color;
    applyRippleColor();
  }

  @ColorInt
  public int getRippleColor() {
    return rippleColor;
  }

  /**
   * Sets the text size for all four sections, in SP — mirrors {@link
   * android.widget.TextView#setTextSize(float)}.
   */
  public void setTextSize(float sp) {
    this.textSize = spToPx(sp);
    applyTextSizeToAll();
  }

  /**
   * @return the current text size in pixels (mirrors TextView#getTextSize()).
   */
  public float getTextSize() {
    return textSize;
  }

  public void setTypeface(Typeface typeface) {
    this.typeface = typeface;
    applyTypefaceToAll();
  }

  public Typeface getTypeface() {
    return typeface;
  }

  /**
   * @param paddingPx horizontal padding, in pixels, applied inside each section.
   */
  public void setSectionPadding(int paddingPx) {
    this.sectionPadding = paddingPx;
    applySectionPaddingToAll();
  }

  public int getSectionPadding() {
    return sectionPadding;
  }

  /**
   * @param sizePx icon width and height, in pixels.
   */
  public void setIconSize(int sizePx) {
    this.iconSize = sizePx;
    applyIconSizeToAll();
  }

  public int getIconSize() {
    return iconSize;
  }

  /**
   * @param spacingPx gap, in pixels, between each icon and its label.
   */
  public void setIconTextSpacing(int spacingPx) {
    this.iconTextSpacing = spacingPx;
    applyIconTextSpacingToAll();
  }

  public int getIconTextSpacing() {
    return iconTextSpacing;
  }

  public void setLanguageVisible(boolean visible) {
    binding.sectionLanguage.setVisibility(visible ? VISIBLE : GONE);
    updateDividerVisibility();
  }

  public boolean isLanguageVisible() {
    return binding.sectionLanguage.getVisibility() == VISIBLE;
  }

  public void setEncodingVisible(boolean visible) {
    binding.sectionEncoding.setVisibility(visible ? VISIBLE : GONE);
    updateDividerVisibility();
  }

  public boolean isEncodingVisible() {
    return binding.sectionEncoding.getVisibility() == VISIBLE;
  }

  public void setIndentationVisible(boolean visible) {
    binding.sectionIndentation.setVisibility(visible ? VISIBLE : GONE);
    updateDividerVisibility();
  }

  public boolean isIndentationVisible() {
    return binding.sectionIndentation.getVisibility() == VISIBLE;
  }

  public void setStatusVisible(boolean visible) {
    binding.sectionStatus.setVisibility(visible ? VISIBLE : GONE);
    updateDividerVisibility();
  }

  public boolean isStatusVisible() {
    return binding.sectionStatus.getVisibility() == VISIBLE;
  }

  public void setOnLanguageClickListener(@Nullable OnClickListener listener) {
    binding.sectionLanguage.setOnClickListener(listener);
  }

  public void setOnEncodingClickListener(@Nullable OnClickListener listener) {
    binding.sectionEncoding.setOnClickListener(listener);
  }

  public void setOnIndentationClickListener(@Nullable OnClickListener listener) {
    binding.sectionIndentation.setOnClickListener(listener);
  }

  public void setOnStatusClickListener(@Nullable OnClickListener listener) {
    binding.sectionStatus.setOnClickListener(listener);
  }

  private float dpToPx(float dp) {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
  }

  private float spToPx(float sp) {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP, sp, getResources().getDisplayMetrics());
  }
}
