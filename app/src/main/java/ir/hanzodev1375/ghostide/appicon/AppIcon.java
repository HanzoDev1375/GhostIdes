package ir.hanzodev1375.ghostide.appicon;

import androidx.annotation.StringRes;
import ir.hanzodev1375.ghostide.R;

/**
 * Every selectable launcher icon. Each entry maps 1:1 to a component declared in
 * AndroidManifest.xml: either the real MainActivity (for the default icon) or an {@code
 * <activity-alias>} that targets MainActivity (for the alternates).
 *
 * <p>To add a new icon: 1. Add an {@code <activity-alias>} in AndroidManifest.xml
 * (enabled="false"), pointing at .MainActivity, with its own android:icon. 2. Add a
 * mipmap/adaptive-icon drawable for it. 3. Add one line here with its full component name, preview
 * drawable and label. No other code needs to change.
 */
public enum AppIcon {
  DEFAULT("ir.hanzodev1375.ghostide.MainActivity", R.mipmap.ic_lego, R.string.icon_name_default),
  DARK("ir.hanzodev1375.ghostide.IconAliasBlue", R.drawable.iconblue, R.string.icon_name_glassblue),
  HELLISH("ir.hanzodev1375.ghostide.IconAliasHellish",R.drawable.iconhellish,R.string.icon_name_hellish);
  public final String componentName;
  public final int previewRes;
  @StringRes public final int labelRes;

  AppIcon(String componentName, int previewRes, @StringRes int labelRes) {
    this.componentName = componentName;
    this.previewRes = previewRes;
    this.labelRes = labelRes;
  }
}
