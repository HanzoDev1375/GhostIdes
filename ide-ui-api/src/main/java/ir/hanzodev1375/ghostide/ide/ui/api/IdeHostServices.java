package ir.hanzodev1375.ghostide.ide.ui.api;

import android.content.Context;

import ir.hanzodev1375.ghostide.plugin.api.ServiceKey;

/** Service keys the host publishes so plugins can look them up from {@code services()}. */
public final class IdeHostServices {

  public static final ServiceKey<EditorHost> EDITOR_HOST =
      new ServiceKey<>("ir.hanzodev1375.ghostide.ui.editorHost", EditorHost.class);

  public static final ServiceKey<FileManagerHost> FILE_MANAGER_HOST =
      new ServiceKey<>("ir.hanzodev1375.ghostide.ui.fileManagerHost", FileManagerHost.class);

  /**
   * A {@link Context} scoped to the requesting plugin's own resources. Layout inflation for a
   * {@link PluginScreen} must go through this context, not the host Activity's default inflater,
   * or the plugin's own {@code R.layout} ids will not resolve:
   *
   * <pre>{@code
   * Context pluginContext = context.getServices().require(IdeHostServices.PLUGIN_ANDROID_CONTEXT);
   * LayoutInflater.from(pluginContext).cloneInContext(pluginContext).inflate(R.layout.my_screen, container, false);
   * }</pre>
   */
  public static final ServiceKey<Context> PLUGIN_ANDROID_CONTEXT =
      new ServiceKey<>("ir.hanzodev1375.ghostide.ui.pluginAndroidContext", Context.class);

  private IdeHostServices() {}
}
