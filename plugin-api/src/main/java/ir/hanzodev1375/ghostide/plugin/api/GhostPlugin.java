package ir.hanzodev1375.ghostide.plugin.api;

import java.util.Collections;
import java.util.List;

/**
 * Entry point of a GhostIDE plugin. The runtime instantiates the class named by {@link
 * PluginDescriptor#getEntryClass()} through a no-argument constructor, then calls {@link
 * #activate(PluginContext)}.
 *
 * <p>A plugin registers its contributions through {@code context.getExtensions()} and must
 * register the returned {@link Disposable} with {@link
 * PluginContext#registerDisposable(Disposable)} so unload can release them deterministically.
 */
public interface GhostPlugin {

  default List<PluginSetupAction> getSetupActions() {
    return Collections.emptyList();
  }

  void activate(PluginContext context);

  default void deactivate() {}
}
