package ir.theme;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.listitem.ListItemCardView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.progressindicator.BaseProgressIndicator;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;

public final class M3Theme {

  private static final String KEY_SHOW_BACKGROUND = "filemanager_showbackgroundtheme";

  private static Context appContext;
  private static boolean showBackground;

  private M3Theme() {}

  public static void init(Context context) {
    appContext = context != null ? context.getApplicationContext() : null;
    reloadMode();
  }

  public static void reloadMode() {
    if (appContext == null) {
      return;
    }
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
    showBackground = prefs.getBoolean(KEY_SHOW_BACKGROUND, false);
  }

  public static Integer surfaceContainer() {
    return color(m3() != null ? m3().getSurfaceContainer() : null);
  }

  public static Integer surfaceContainerHigh() {
    return color(m3() != null ? m3().getSurfaceContainerHigh() : null);
  }

  public static Integer surfaceContainerLow() {
    return color(m3() != null ? m3().getSurfaceContainerLow() : null);
  }

  public static Integer surface() {
    return color(m3() != null ? m3().getSurface() : null);
  }

  public static Integer onSurface() {
    return color(m3() != null ? m3().getOnSurface() : null);
  }

  public static Integer primary() {
    return color(m3() != null ? m3().getPrimary() : null);
  }

  public static Integer onPrimary() {
    return color(m3() != null ? m3().getOnPrimary() : null);
  }

  public static Integer primaryContainer() {
    return color(m3() != null ? m3().getPrimaryContainer() : null);
  }

  public static Integer onPrimaryContainer() {
    return color(m3() != null ? m3().getOnPrimaryContainer() : null);
  }

  public static Integer secondary() {
    return color(m3() != null ? m3().getSecondary() : null);
  }

  public static Integer onSecondary() {
    return color(m3() != null ? m3().getOnSecondary() : null);
  }

  public static Integer secondaryContainer() {
    return color(m3() != null ? m3().getSecondaryContainer() : null);
  }

  public static Integer onSecondaryContainer() {
    return color(m3() != null ? m3().getOnSecondaryContainer() : null);
  }

  public static Integer tertiary() {
    return color(m3() != null ? m3().getTertiary() : null);
  }

  public static Integer onTertiary() {
    return color(m3() != null ? m3().getOnTertiary() : null);
  }

  public static Integer tertiaryContainer() {
    return color(m3() != null ? m3().getTertiaryContainer() : null);
  }

  public static Integer onTertiaryContainer() {
    return color(m3() != null ? m3().getOnTertiaryContainer() : null);
  }

  public static Integer error() {
    return color(m3() != null ? m3().getError() : null);
  }

  public static Integer onError() {
    return color(m3() != null ? m3().getOnError() : null);
  }

  public static Integer errorContainer() {
    return color(m3() != null ? m3().getErrorContainer() : null);
  }

  public static Integer onErrorContainer() {
    return color(m3() != null ? m3().getOnErrorContainer() : null);
  }

  public static Integer background() {
    return color(m3() != null ? m3().getBackground() : null);
  }

  public static Integer secondaryFixed() {
    return color(m3() != null ? m3().getSecondaryFixed() : null);
  }

  public static Integer onSecondaryFixed() {
    return color(m3() != null ? m3().getOnSecondaryFixed() : null);
  }

  public static Integer secondaryFixedDim() {
    return color(m3() != null ? m3().getSecondaryFixedDim() : null);
  }

  public static Integer onSecondaryFixedVariant() {
    return color(m3() != null ? m3().getOnSecondaryFixedVariant() : null);
  }

  public static Integer surfaceVariant() {
    return color(m3() != null ? m3().getSurfaceVariant() : null);
  }

  public static Integer onSurfaceVariant() {
    return color(m3() != null ? m3().getOnSurfaceVariant() : null);
  }

  public static Integer outline() {
    return color(m3() != null ? m3().getOutline() : null);
  }

  public static Integer outlineVariant() {
    return color(m3() != null ? m3().getOutlineVariant() : null);
  }

  public static Integer surfaceDim() {
    return color(m3() != null ? m3().getSurfaceDim() : null);
  }

  public static Integer surfaceBright() {
    return color(m3() != null ? m3().getSurfaceBright() : null);
  }

  public static Integer surfaceContainerLowest() {
    return color(m3() != null ? m3().getSurfaceContainerLowest() : null);
  }

  public static Integer surfaceContainerHighest() {
    return color(m3() != null ? m3().getSurfaceContainerHighest() : null);
  }

  public static Integer onBackground() {
    return color(m3() != null ? m3().getOnBackground() : null);
  }

  public static Integer tertiaryFixed() {
    return color(m3() != null ? m3().getTertiaryFixed() : null);
  }

  public static Integer onTertiaryFixed() {
    return color(m3() != null ? m3().getOnTertiaryFixed() : null);
  }

  public static Integer tertiaryFixedDim() {
    return color(m3() != null ? m3().getTertiaryFixedDim() : null);
  }

  public static Integer onTertiaryFixedVariant() {
    return color(m3() != null ? m3().getOnTertiaryFixedVariant() : null);
  }

  public static Integer inverseSurface() {
    return color(m3() != null ? m3().getInverseSurface() : null);
  }

  public static Integer inverseOnSurface() {
    return color(m3() != null ? m3().getInverseOnSurface() : null);
  }

  public static Integer inversePrimary() {
    return color(m3() != null ? m3().getInversePrimary() : null);
  }

  public static Integer scrim() {
    return color(m3() != null ? m3().getScrim() : null);
  }

  public static Integer shadow() {
    return color(m3() != null ? m3().getShadow() : null);
  }

  public static Integer surfaceTint() {
    return color(m3() != null ? m3().getSurfaceTint() : null);
  }

  public static Integer primaryFixed() {
    return color(m3() != null ? m3().getPrimaryFixed() : null);
  }

  public static Integer onPrimaryFixed() {
    return color(m3() != null ? m3().getOnPrimaryFixed() : null);
  }

  public static Integer primaryFixedDim() {
    return color(m3() != null ? m3().getPrimaryFixedDim() : null);
  }

  public static Integer onPrimaryFixedVariant() {
    return color(m3() != null ? m3().getOnPrimaryFixedVariant() : null);
  }

  public static void apply(View root) {
    if (root == null) {
      return;
    }
    applyShallow(root);
    if (root instanceof ViewGroup) {
      ViewGroup group = (ViewGroup) root;
      for (int i = 0; i < group.getChildCount(); i++) {
        apply(group.getChildAt(i));
      }
    }
  }

  /**
   * Lightweight theming for a single RecyclerView item. Uses a bounded traversal that only walks
   * the subtree of the supplied {@code item} view (a recycled item's tree is tiny, ~5-15 views)
   * WITHOUT ever escaping into the parent activity, so it stays cheap even when thousands of items
   * are bound. Unlike {@link #apply(View)}, this never themes siblings or ancestors, so
   * RecyclerView bind operations do not re-theme the whole screen.
   */
  public static void listCard(View item) {
    if (item == null) {
      return;
    }
    java.util.ArrayDeque<View> stack = new java.util.ArrayDeque<>();
    stack.push(item);
    while (!stack.isEmpty()) {
      View v = stack.pop();
      applyShallow(v);
      if (v instanceof ViewGroup) {
        ViewGroup group = (ViewGroup) v;
        for (int i = group.getChildCount() - 1; i >= 0; i--) {
          stack.push(group.getChildAt(i));
        }
      }
    }
  }

  /**
   * Themes only the top-level children of a large layout. Unlike {@link #apply(View)} it does not
   * recurse into nested containers (RecyclerViews, nested scroll containers), so it is safe to run
   * on activities whose root tree is very large. Use it in place of {@code apply()} on big,
   * RecyclerView-heavy activities.
   */
  public static void applyTopLevel(View root) {
    if (root == null) {
      return;
    }
    applyShallow(root);
    if (root instanceof ViewGroup) {
      ViewGroup group = (ViewGroup) root;
      for (int i = 0; i < group.getChildCount(); i++) {
        View child = group.getChildAt(i);
        applyShallow(child);
        if (child instanceof ViewGroup) {
          ViewGroup g2 = (ViewGroup) child;
          for (int j = 0; j < g2.getChildCount(); j++) {
            applyShallow(g2.getChildAt(j));
          }
        }
      }
    }
  }

  public static void applyShallow(View v) {
    if (v == null) {
      return;
    }
    try {
      if (v instanceof MaterialButton) {
        materialButton((MaterialButton) v);
      } else if (v instanceof FloatingActionButton) {
        fabView((FloatingActionButton) v);
      } else if (v instanceof ListItemCardView) {
        listItemCardView((ListItemCardView) v);
      } else if (v instanceof MaterialCardView) {
        cardView((MaterialCardView) v);
      } else if (v instanceof TextInputLayout) {
        textInputLayout((TextInputLayout) v);
      } else if (v instanceof TabLayout) {
        tabLayout((TabLayout) v);
      } else if (v instanceof ChipGroup) {
        chipGroupView((ChipGroup) v);
      } else if (v instanceof Chip) {
        chipView((Chip) v);
      } else if (v instanceof MaterialSwitch) {
        materialSwitch((MaterialSwitch) v);
      } else if (v instanceof SwitchMaterial) {
        switchView((SwitchMaterial) v);
      } else if (v instanceof MaterialCheckBox) {
        checkboxView((CheckBox) v);
      } else if (v instanceof RadioButton) {
        radioView((RadioButton) v);
      } else if (v instanceof BottomNavigationView) {
        bottomNav((BottomNavigationView) v);
      } else if (v instanceof MaterialToolbar) {
        toolbar((MaterialToolbar) v);
      } else if (v instanceof BaseProgressIndicator) {
        progress((BaseProgressIndicator<?>) v);
      } else if (v instanceof Slider) {
        slider((Slider) v);
      } else if (v instanceof ShapeableImageView) {
        imageView((ImageView) v);
      } else if (v instanceof MaterialTextView) {
        textView((TextView) v);
      } else if (v instanceof TextView) {
        textView((TextView) v);
      } else if (v instanceof EditText) {
        editText((EditText) v);
      } else if (v instanceof CheckBox) {
        checkboxView((CheckBox) v);
      } else if (v instanceof ImageView) {
        // imageView((ImageView) v);
      } else if (v instanceof SeekBar) {
        seekBar((SeekBar) v);
      } else if (v instanceof ProgressBar) {
        progressBar((ProgressBar) v);
      }
    } catch (Throwable ignored) {
    }
  }

  public static void text(TextView... views) {
    for (TextView v : views) {
      if (v != null) {
        textView(v);
      }
    }
  }

  public static void card(MaterialCardView... views) {
    for (MaterialCardView v : views) {
      if (v != null) {
        cardView(v);
      }
    }
  }

  public static void button(MaterialButton... views) {
    for (MaterialButton v : views) {
      if (v != null) {
        materialButton(v);
      }
    }
  }

  public static void fab(FloatingActionButton... views) {
    for (FloatingActionButton v : views) {
      if (v != null) {
        fabView(v);
      }
    }
  }

  public static void tabs(TabLayout... views) {
    for (TabLayout v : views) {
      if (v != null) {
        tabLayout(v);
      }
    }
  }

  public static void chip(Chip... views) {
    for (Chip v : views) {
      if (v != null) {
        chipView(v);
      }
    }
  }

  public static void checkbox(CheckBox... views) {
    for (CheckBox v : views) {
      if (v != null) {
        checkboxView(v);
      }
    }
  }

  public static void radio(RadioButton... views) {
    for (RadioButton v : views) {
      if (v != null) {
        radioView(v);
      }
    }
  }

  public static void toggle(SwitchMaterial... views) {
    for (SwitchMaterial v : views) {
      if (v != null) {
        switchView(v);
      }
    }
  }

  public static void toggle(MaterialSwitch... views) {
    for (MaterialSwitch v : views) {
      if (v != null) {
        materialSwitch(v);
      }
    }
  }

  public static void input(TextInputLayout... views) {
    for (TextInputLayout v : views) {
      if (v != null) {
        textInputLayout(v);
      }
    }
  }

  public static void image(ImageView... views) {
    for (ImageView v : views) {
      if (v != null) {
        imageView(v);
      }
    }
  }

  public static void imageB(ImageButton... v) {
    for (var it : v) {
      if (it != null) {
        imageButton(it);
      }
    }
  }

  public static void imageButton(ImageButton button) {
    ColorStateList buttons =
        new ColorStateList(
            new int[][] {
              new int[] {android.R.attr.state_focused},
              new int[] {android.R.attr.state_hovered},
              new int[] {-android.R.attr.state_enabled},
              new int[] {}
            },
            new int[] {onSurface(), onPrimary(), onSurfaceVariant(), primary()});
    button.setImageTintList(buttons);
  }

  public static void materialButton(MaterialButton b) {
    Integer primary = color(m3() != null ? m3().getPrimary() : null);
    Integer onPrimary = color(m3() != null ? m3().getOnPrimary() : null);
    Integer onPrimaryContainer =
        fallback(
            m3() != null ? m3().getOnPrimaryContainer() : null,
            m3() != null ? m3().getOnPrimary() : null);

    if (b.isChecked()) {
      Integer container = fallback(color(m3() != null ? m3().getPrimaryContainer() : null), primary);
      Integer onContainer = fallback(onPrimaryContainer, onPrimary);
      if (container != null) {
        b.setBackgroundTintList(ColorStateList.valueOf(container));
        try {
          b.setStrokeColor(ColorStateList.valueOf(container));
        } catch (Throwable ignored) {
        }
      }
      if (onContainer != null) {
        b.setTextColor(onContainer);
        b.setIconTint(ColorStateList.valueOf(onContainer));
        try {
          b.setRippleColor(ColorStateList.valueOf(surfaceAlpha(onContainer)));
        } catch (Throwable ignored) {
        }
      }
      return;
    }

    if (b.getStrokeWidth() > 0) {
      Integer stroke =
          fallback(
              color(m3() != null ? m3().getOutlineVariant() : null),
              color(m3() != null ? m3().getOutline() : null),
              onPrimary);
      Integer text = fallback(primary, color(m3() != null ? m3().getOnSurfaceVariant() : null));
      b.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
      if (stroke != null) {
        try {
          b.setStrokeColor(ColorStateList.valueOf(stroke));
        } catch (Throwable ignored) {
        }
      }
      if (text != null) {
        b.setTextColor(text);
        b.setIconTint(ColorStateList.valueOf(text));
      }
      Integer ripple = fallback(color(m3() != null ? m3().getOnSurface() : null), text);
      if (ripple != null) {
        try {
          b.setRippleColor(ColorStateList.valueOf(surfaceAlpha(ripple)));
        } catch (Throwable ignored) {
        }
      }
      return;
    }

    boolean filled = b.getStrokeWidth() <= 0;
    if (filled && primary != null) {
      b.setBackgroundTintList(ColorStateList.valueOf(primary));
    }
    if (!filled && primary != null) {
      try {
        b.setStrokeColor(ColorStateList.valueOf(primary));
      } catch (Throwable ignored) {
      }
    }
    if (onPrimary != null) {
      b.setTextColor(onPrimary);
      b.setIconTint(ColorStateList.valueOf(onPrimary));
    }
    if (onPrimaryContainer != null) {
      try {
        b.setRippleColor(ColorStateList.valueOf(surfaceAlpha(onPrimaryContainer)));
      } catch (Throwable ignored) {
      }
    }
  }

  public static void toggleGroup(MaterialButtonToggleGroup group) {
    if (group == null) {
      return;
    }
    group.addOnButtonCheckedListener(
        (g, checkedId, isChecked) -> {
          for (int i = 0; i < g.getChildCount(); i++) {
            View v = g.getChildAt(i);
            if (v instanceof MaterialButton) {
              materialButton((MaterialButton) v);
            }
          }
        });
    for (int i = 0; i < group.getChildCount(); i++) {
      View v = group.getChildAt(i);
      if (v instanceof MaterialButton) {
        materialButton((MaterialButton) v);
      }
    }
  }

  public static void fabView(FloatingActionButton fab) {
    Integer container = fallback(m3().getPrimaryContainer(), m3().getPrimary());
    Integer onContainer = fallback(m3().getOnPrimaryContainer(), m3().getOnPrimary());
    if (container != null) {
      fab.setBackgroundTintList(ColorStateList.valueOf(container));
    }
    if (onContainer != null) {
      fab.setColorFilter(onContainer);
    }
    if (container != null) {
      try {
        fab.setRippleColor(ColorStateList.valueOf(surfaceAlpha(container)));
      } catch (Throwable ignored) {
      }
    }
  }

  public static void cardView(MaterialCardView cardView) {
    Integer surface = surfaced(cardView, true);
    Integer stroke = fallback(m3().getOutlineVariant(), m3().getOutline());
    if (surface != null) {
      cardView.setCardBackgroundColor(surface);
    }
    if (stroke != null) {
      try {
        cardView.setStrokeColor(ColorStateList.valueOf(stroke));
      } catch (Throwable ignored) {
      }
    }
  }

  public static void listItemCardView(ListItemCardView cardView) {
    Integer surface = surfaced(cardView, true);
    Integer stroke = fallback(m3().getOutlineVariant(), m3().getOutline());
    if (surface != null) {
      cardView.setCardBackgroundColor(surface);
    }
    if (stroke != null) {
      try {
        cardView.setStrokeColor(ColorStateList.valueOf(stroke));
      } catch (Throwable ignored) {
      }
    }
  }

  public static void textInputLayout(TextInputLayout input) {
    Integer primary = fallback(m3().getPrimary(), m3().getSecondary());
    Integer onSurface = fallback(m3().getOnSurface(), m3().getOnSurfaceVariant());
    Integer onSurfaceVariant = fallback(m3().getOnSurfaceVariant(), m3().getOnSurface());
    Integer error = fallback(m3().getError(), m3().getOnError());
    Integer outline = fallback(m3().getOutline(), m3().getOutlineVariant());
    Integer outlineVariant = fallback(m3().getOutlineVariant(), m3().getOutline());
    Integer disabled = color(m3().getOnSurface());
    Integer boxBg = surfaced(input, true);

    if (primary != null && outline != null && outlineVariant != null && error != null) {
      try {
        int enabledStroke = primary;
        int focusedStroke = primary;
        int errorStroke = error;
        int disabledStroke = outlineVariant;
        ColorStateList strokeColors =
            new ColorStateList(
                new int[][] {
                  new int[] {android.R.attr.state_focused},
                  new int[] {android.R.attr.state_hovered},
                  new int[] {-android.R.attr.state_enabled},
                  new int[] {}
                },
                new int[] {focusedStroke, errorStroke, disabledStroke, enabledStroke});
        input.setBoxStrokeColorStateList(strokeColors);
      } catch (Throwable ignored) {
      }
    }

    if (onSurface != null && onSurfaceVariant != null) {
      try {
        int enabledHint = onSurfaceVariant;
        int focusedHint = primary;
        int errorHint = error;
        int disabledHint = disabled != null ? disabled : onSurfaceVariant;
        ColorStateList hintColors =
            new ColorStateList(
                new int[][] {
                  new int[] {android.R.attr.state_focused},
                  new int[] {android.R.attr.state_hovered},
                  new int[] {-android.R.attr.state_enabled},
                  new int[] {}
                },
                new int[] {focusedHint, errorHint, disabledHint, enabledHint});
        input.setDefaultHintTextColor(hintColors);
      } catch (Throwable ignored) {
      }
    }

    if (boxBg != null) {
      try {
        input.setBoxBackgroundColor(boxBg);
      } catch (Throwable ignored) {
      }
    }

    if (onSurfaceVariant != null) {
      try {
        input.setPrefixTextColor(ColorStateList.valueOf(onSurfaceVariant));
        input.setSuffixTextColor(ColorStateList.valueOf(onSurfaceVariant));
      } catch (Throwable ignored) {
      }
    }
  }

  public static void tabLayout(TabLayout tabs) {
    Integer selected = fallback(m3().getOnSurface(), m3().getOnSurfaceVariant());
    Integer unselected = fallback(m3().getOnSurfaceVariant(), m3().getOnSurface());
    Integer ind = fallback(m3().getPrimary(), m3().getSecondary());
    if (selected != null) {
      try {
        tabs.setTabTextColors(unselected != null ? unselected : selected, selected);
      } catch (Throwable ignored) {
      }
    }
    if (ind != null) {
      try {
        tabs.setSelectedTabIndicatorColor(ind);
      } catch (Throwable ignored) {
      }
    }
  }

  public static void chipGroupView(ChipGroup group) {
    for (int i = 0; i < group.getChildCount(); i++) {
      View c = group.getChildAt(i);
      if (c instanceof Chip) {
        chipView((Chip) c);
      }
    }
  }

  public static void chipView(Chip chip) {
    Integer surface = surfaced(chip, true);
    Integer onSurface = color(m3().getOnSurface());
    Integer accent = fallback(m3().getPrimary(), m3().getSecondary());
    Integer onAccent = fallback(m3().getOnPrimary(), m3().getOnPrimaryContainer());
    Integer outlineVar = fallback(m3().getOutlineVariant(), m3().getOutline());

    if (surface != null) {
      try {
        int enabledBg = surface;
        int pressedBg = fallback(color(m3().getPrimaryContainer()), surface);
        chip.setChipBackgroundColor(
            new ColorStateList(
                new int[][] {new int[] {android.R.attr.state_pressed}, new int[] {}},
                new int[] {pressedBg}));
      } catch (Throwable ignored) {
      }
    }

    if (onSurface != null) {
      try {
        chip.setTextColor(onSurface);
      } catch (Throwable ignored) {
      }
    }

    if (outlineVar != null) {
      try {
        int enabledStroke = outlineVar;
        int pressedStroke = fallback(accent, outlineVar);
        chip.setChipStrokeColor(
            new ColorStateList(
                new int[][] {new int[] {android.R.attr.state_pressed}, new int[] {}},
                new int[] {pressedStroke}));
      } catch (Throwable ignored) {
      }
    }

    if (accent != null) {
      try {
        chip.setChipIconTint(ColorStateList.valueOf(accent));
        chip.setCheckedIconTint(ColorStateList.valueOf(accent));
        chip.setCloseIconTint(ColorStateList.valueOf(onAccent != null ? onAccent : accent));
      } catch (Throwable ignored) {
      }
    }

    if (accent != null) {
      try {
        chip.setRippleColor(ColorStateList.valueOf(surfaceAlpha(accent)));
      } catch (Throwable ignored) {
      }
    }
  }

  public static void switchView(SwitchMaterial sw) {
    Integer checkedTrack = fallback(m3().getPrimaryContainer(), m3().getPrimary());
    Integer uncheckedTrack = fallback(m3().getSurfaceContainerHighest(), m3().getSurfaceVariant());
    Integer checkedThumb = fallback(m3().getOnPrimaryContainer(), m3().getOnPrimary());
    Integer uncheckedThumb = fallback(m3().getOnSurfaceVariant(), m3().getOnSurface());
    if (sw.isChecked()) {
      if (checkedTrack != null) {
        sw.setTrackTintList(active(surfaceAlpha(checkedTrack)));
      }
      if (checkedThumb != null) {
        sw.setThumbTintList(active(checkedThumb));
      }
    } else {
      if (uncheckedTrack != null) {
        sw.setTrackTintList(active(uncheckedTrack));
      }
      if (uncheckedThumb != null) {
        sw.setThumbTintList(active(uncheckedThumb));
      }
    }
  }

  public static void materialSwitch(MaterialSwitch sw) {
    var stateThumb =
        new ColorStateList(
            new int[][] {
              new int[] {android.R.attr.state_checked, android.R.attr.state_enabled},
              new int[] {-android.R.attr.state_checked, android.R.attr.state_enabled},
              new int[] {-android.R.attr.state_enabled}
            },
            new int[] {
              onPrimary(), // checked + enabled
              surfaceVariant(), // unchecked + enabled
              surfaceContainerHighest() // disabled
            });
    sw.setThumbTintList(stateThumb);

    var stateIcon =
        new ColorStateList(
            new int[][] {
              new int[] {android.R.attr.state_checked, android.R.attr.state_enabled},
              new int[] {-android.R.attr.state_checked, android.R.attr.state_enabled},
              new int[] {-android.R.attr.state_enabled}
            },
            new int[] {
              onPrimary(), // checked + enabled
              onSurfaceVariant(), // unchecked + enabled
              onSurface() // disabled
            });
    sw.setThumbIconTintList(stateIcon);

    // Track background color (filled with primary when on, surfaceContainerHighest when off).
    var stateTrack =
        new ColorStateList(
            new int[][] {
              new int[] {android.R.attr.state_checked, android.R.attr.state_enabled},
              new int[] {-android.R.attr.state_checked, android.R.attr.state_enabled},
              new int[] {-android.R.attr.state_enabled}
            },
            new int[] {
              primary(), // checked + enabled
              surfaceContainerHighest(), // unchecked + enabled
              surfaceContainerHighest() // disabled
            });
    sw.setTrackTintList(stateTrack);

    // Track decoration (the icon rendered on top of the track).
    var stateTrackIcon =
        new ColorStateList(
            new int[][] {
              new int[] {android.R.attr.state_checked, android.R.attr.state_enabled},
              new int[] {-android.R.attr.state_checked, android.R.attr.state_enabled},
              new int[] {-android.R.attr.state_enabled}
            },
            new int[] {
              onPrimary(), // checked + enabled
              surfaceVariant(), // unchecked + enabled
              surfaceVariant() // disabled
            });
    sw.setTrackDecorationTintList(stateTrackIcon);
  }

  public static void checkboxView(CheckBox cb) {
    Integer accent = fallback(m3().getPrimary(), m3().getSecondary());
    Integer onSurface = fallback(m3().getOnSurfaceVariant(), m3().getOnSurface());
    Integer disabled = fallback(m3().getOnSurface(), m3().getSurface());
    if (accent != null && onSurface != null && disabled != null) {
      cb.setButtonTintList(
          new ColorStateList(
              new int[][] {
                new int[] {android.R.attr.state_checked, android.R.attr.state_enabled},
                new int[] {android.R.attr.state_enabled},
                new int[] {-android.R.attr.state_enabled}
              },
              new int[] {accent, onSurface, disabled}));
    }
  }

  public static void radioView(RadioButton rb) {
    Integer accent = fallback(m3().getPrimary(), m3().getSecondary());
    Integer onSurface = fallback(m3().getOnSurfaceVariant(), m3().getOnSurface());
    Integer disabled = fallback(m3().getOnSurface(), m3().getOnSurface());
    if (accent != null && onSurface != null && disabled != null) {
      rb.setButtonTintList(
          new ColorStateList(
              new int[][] {
                new int[] {android.R.attr.state_checked, android.R.attr.state_enabled},
                new int[] {android.R.attr.state_enabled},
                new int[] {-android.R.attr.state_enabled}
              },
              new int[] {accent, onSurface, disabled}));
    }
  }

  public static void bottomNav(BottomNavigationView nav) {
    Integer item = fallback(m3().getOnSurfaceVariant(), m3().getOnSurface());
    Integer selected = fallback(m3().getPrimary(), m3().getSecondary());
    if (item != null && selected != null) {
      try {
        ColorStateList sl =
            new ColorStateList(
                new int[][] {new int[] {android.R.attr.state_checked}, new int[] {}},
                new int[] {selected, item});
        nav.setItemIconTintList(sl);
        nav.setItemTextColor(sl);
        nav.setItemActiveIndicatorColor(
            ColorStateList.valueOf(color(m3().getSecondaryContainer())));
        nav.setBackgroundTintList(ColorStateList.valueOf(color(m3().getSurfaceBright())));
      } catch (Throwable ignored) {
      }
    }
  }

  public static void toolbar(MaterialToolbar tb) {
    Integer title = color(m3().getOnSurface());
    if (title != null) {
      tb.setTitleTextColor(title);
      tb.setSubtitleTextColor(title);
    }
  }

  public static void progress(BaseProgressIndicator<?> p) {
    Integer accent = fallback(m3().getPrimary(), m3().getSecondary());
    Integer track = fallback(m3().getPrimaryContainer(), m3().getSurfaceVariant());
    if (accent != null) {
      try {
        p.setIndicatorColor(accent);
      } catch (Throwable ignored) {
      }
    }
    if (track != null) {
      try {
        p.setTrackColor(track);
      } catch (Throwable ignored) {
      }
    }
  }

  public static void slider(Slider s) {
    Integer accent = fallback(m3().getPrimary(), m3().getSecondary());
    Integer inactiveTrack = fallback(m3().getSurfaceContainerHighest(), m3().getSurfaceVariant());
    if (accent != null) {
      try {
        s.setThumbTintList(active(accent));
        s.setTrackActiveTintList(active(accent));
      } catch (Throwable ignored) {
      }
    }
    if (inactiveTrack != null) {
      try {
        s.setTrackInactiveTintList(ColorStateList.valueOf(inactiveTrack));
      } catch (Throwable ignored) {
      }
    }
  }

  public static void textView(TextView tv) {
    Integer onSurface = fallback(color(m3().getOnSurface()), color(widgetNullableText()));
    if (onSurface != null) {
      tv.setTextColor(onSurface);
    }
    if (tv.getHint() != null) {
      Integer hint = fallback(color(m3().getOnSurfaceVariant()), color(hintString()));
      if (hint != null) {
        tv.setHintTextColor(hint);
      }
    }
  }

  public static void editText(EditText et) {
    Integer text = fallback(color(m3().getOnSurface()), color(widgetNullableText()));
    Integer hint = fallback(color(m3().getOnSurfaceVariant()), color(hintString()));
    if (text != null) {
      et.setTextColor(text);
    }
    if (hint != null) {
      et.setHintTextColor(hint);
    }
    if (Build.VERSION.SDK_INT >= 29) {
      try {
        et.setTextCursorDrawable(null);
      } catch (Throwable ignored) {
      }
    }
  }

  public static void imageView(ImageView iv) {
    Integer tint =
        fallback(
            color(m3().getOnSurfaceVariant()),
            color(m3().getOnSurface()),
            color(widgetNullableImageTint()));
    if (tint != null) {
      iv.setColorFilter(tint);
    }
  }

  public static void progressBar(ProgressBar pb) {
    Integer accent = fallback(color(m3().getPrimary()), color(widgetNullableAccent()));
    if (accent != null) {
      pb.setProgressTintList(active(accent));
    }
  }

  public static void seekBar(SeekBar sb) {
    Integer accent = fallback(color(m3().getPrimary()), color(widgetNullableAccent()));
    if (accent != null) {
      sb.setProgressTintList(active(accent));
      sb.setThumbTintList(active(accent));
    }
  }

  private static @ColorInt int surfaceAlpha(int c) {
    if (showBackground) {
      return ColorUtils.setAlphaComponent(c, 128);
    }
    return c;
  }

  private static ColorStateList active(int c) {
    return new ColorStateList(
        new int[][] {new int[] {android.R.attr.state_enabled}, new int[] {}},
        new int[] {c, color(m3() != null ? m3().getOnSurfaceVariant() : null)});
  }

  private static @ColorInt int surfaced(View v, boolean alpha) {
    Integer c = color(m3() != null ? m3().getSurfaceContainer() : null);
    if (c == null) {
      c = color(m3() != null ? m3().getSurface() : null);
    }
    if (c == null) {
      c = color(widgetSurface());
    }
    if (c == null) {
      return 0;
    }
    return alpha ? surfaceAlpha(c) : c;
  }

  @Nullable
  public static Integer color(@Nullable String hex) {
    if (hex == null || hex.isEmpty()) {
      return null;
    }
    try {
      return Color.parseColor(hex);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private static Integer fallback(@Nullable String a, @Nullable String b) {
    Integer ca = color(a);
    if (ca != null) {
      return ca;
    }
    Integer cb = color(b);
    return cb != null ? cb : null;
  }

  private static Integer fallback(Integer... candidates) {
    for (Integer c : candidates) {
      if (c != null) {
        return c;
      }
    }
    return null;
  }

  private static MaterialTheme m3() {
    GhostTheme g = theme();
    if (g == null) {
      return null;
    }
    return g.getMaterial3();
  }

  private static WidgetTheme widget() {
    GhostTheme g = theme();
    if (g == null) {
      return null;
    }
    return g.getWidget();
  }

  private static GhostTheme theme() {
    if (appContext == null) {
      return null;
    }
    try {
      return ThemeManager.getDefault(appContext).getTheme();
    } catch (RuntimeException ignored) {
      return null;
    }
  }

  private static String widgetSurface() {
    WidgetTheme w = widget();
    return w != null ? w.getSurface() : null;
  }

  @Nullable
  private static String widgetNullableText() {
    WidgetTheme w = widget();
    return w != null ? w.getText() : null;
  }

  @Nullable
  private static String hintString() {
    WidgetTheme w = widget();
    return w != null ? w.getHint() : null;
  }

  @Nullable
  private static String widgetNullableAccent() {
    WidgetTheme w = widget();
    return w != null ? w.getAccent() : null;
  }

  @Nullable
  private static String widgetNullableImageTint() {
    WidgetTheme w = widget();
    return w != null ? w.getImageTint() : null;
  }
}
