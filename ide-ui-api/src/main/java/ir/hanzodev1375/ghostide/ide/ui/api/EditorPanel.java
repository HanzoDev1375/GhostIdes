package ir.hanzodev1375.ghostide.ide.ui.api;

import android.view.View;

/**
 * A UI panel contributed by a plugin and hosted <em>inside</em> an existing screen such as the
 * editor, instead of taking over a whole Activity like {@link PluginScreen}. This is the VS Code
 * "webview / side panel" equivalent: a plugin can slide a chat view, an inspector, a settings pane
 * or any other UI into the running editor screen without the user ever leaving it.
 *
 * <p>Register an implementation at {@link PluginUiExtensionPoints#EDITOR_PANEL}. The host calls
 * {@link #createView()} once when the panel is first shown and keeps the returned {@link View} for
 * the rest of that Activity's lifetime, so create the view lazily and keep its state inside it.
 *
 * <p>Inflate layouts with the plugin's own scoped context, or your {@code R.layout} ids will not
 * resolve:
 *
 * <pre>{@code
 * Context pluginContext = context.getServices().require(IdeHostServices.PLUGIN_ANDROID_CONTEXT);
 * LayoutInflater.from(pluginContext).cloneInContext(pluginContext).inflate(R.layout.my_panel, root, false);
 * }</pre>
 */
public interface EditorPanel {

  String getId();

  String getTitle();

  View createView();

  default String getLastPath() {
    return null;
  }

  /**
   * How the host should display this panel. Defaults to {@link PluginStateMod#SIDESHEET}.
   *
   * <p>Plugins can either call {@link #setState(PluginStateMod)} anywhere in the panel (constructor,
   * {@link #createView()}, ...) or simply override this method to return a fixed mode.
   */
  default PluginStateMod getState() {
    return EditorPanelStateStore.get(getId());
  }

  /** Overrides the display mode used by the host. {@code null} resets back to the default. */
  default void setState(PluginStateMod state) {
    EditorPanelStateStore.set(getId(), state);
  }
}
