package ir.hanzodev1375.ghostide.appicon;

import androidx.annotation.StringRes;
import ir.hanzodev1375.ghostide.R;

/**
 * Every selectable launcher icon. Each entry maps 1:1 to a component declared in
 * AndroidManifest.xml: either the real SplashActivity (for the default icon) or an {@code
 * <activity-alias>} that targets SplashActivity (for the alternates).
 *
 * <p>To add a new icon: 1. Add an {@code <activity-alias>} in AndroidManifest.xml
 * (enabled="false"), pointing at .SplashActivity, with its own android:icon. 2. Add a
 * mipmap/adaptive-icon drawable for it. 3. Add one line here with its full component name, preview
 * drawable and label. No other code needs to change.
 */
public enum AppIcon {
  DEFAULT("ir.hanzodev1375.ghostide.SplashActivity", R.mipmap.ic_lego, R.string.icon_name_default),
  DARK("ir.hanzodev1375.ghostide.IconAliasBlue", R.drawable.iconblue, R.string.icon_name_glassblue),
  HELLISH(
      "ir.hanzodev1375.ghostide.IconAliasHellish",
      R.drawable.iconhellish,
      R.string.icon_name_hellish),
  ATOMGHOST(
      "ir.hanzodev1375.ghostide.IconAliasatomicghost",
      R.drawable.atomicghost,
      R.string.icon_atomicghost),
  EVILICON(
      "ir.hanzodev1375.ghostide.IconAliasdevilishpurple",
      R.drawable.devilishpurple,
      R.string.icon_devilishpurple),
  NEON(
      "ir.hanzodev1375.ghostide.IconAliasneonpurple",
      R.drawable.neonpurple,
      R.string.icon_neonpurple),
  BLACKMAGIC(
      "ir.hanzodev1375.ghostide.IconAliasblackmagic",
      R.drawable.blackmagic,
      R.string.icon_blackmagic),
  PURPLEWITCH(
      "ir.hanzodev1375.ghostide.IconAliaspurplewitch",
      R.drawable.purplewitch,
      R.string.icon_purplewitch);
  public final String componentName;
  public final int previewRes;
  @StringRes public final int labelRes;

  AppIcon(String componentName, int previewRes, @StringRes int labelRes) {
    this.componentName = componentName;
    this.previewRes = previewRes;
    this.labelRes = labelRes;
  }
}
