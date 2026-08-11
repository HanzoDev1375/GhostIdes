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
}
