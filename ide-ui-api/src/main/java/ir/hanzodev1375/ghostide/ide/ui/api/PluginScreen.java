package ir.hanzodev1375.ghostide.ide.ui.api;

import androidx.fragment.app.Fragment;

/**
 * A whole screen contributed by a plugin, registered at {@link
 * PluginUiExtensionPoints#PLUGIN_SCREEN}. Android will not let dynamically loaded code declare a
 * new {@code <activity>} in the host manifest, so a plugin's "activity" is a {@link Fragment}
 * instead; the host's single, already-declared screen-hosting Activity creates and displays it.
 * The fragment's layout and resources come from the plugin's own {@code .gpl} package.
 */
public interface PluginScreen {

  String getId();

  String getTitle();

  Fragment createFragment();
}
