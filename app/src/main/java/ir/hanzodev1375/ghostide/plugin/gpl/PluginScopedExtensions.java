package ir.hanzodev1375.ghostide.plugin.gpl;

import java.util.List;

import ir.hanzodev1375.ghostide.plugin.api.Disposable;
import ir.hanzodev1375.ghostide.plugin.api.ExtensionPoint;
import ir.hanzodev1375.ghostide.plugin.api.ExtensionRegistration;
import ir.hanzodev1375.ghostide.plugin.api.MutableExtensionRegistry;

/**
 * Delegates to the shared extension registry but stamps every registration made through a plugin's
 * {@link ir.hanzodev1375.ghostide.plugin.api.PluginContext} with that plugin's own id. Without
 * this, plugins register under {@code PluginIds.CORE}, so {@link
 * ir.hanzodev1375.ghostide.plugin.api.GlobalRegistry} can no longer tell which host owns a
 * contribution and unload/cleanup and per-plugin lookups silently break.
 */
final class PluginScopedExtensions implements MutableExtensionRegistry {

  private final MutableExtensionRegistry delegate;
  private final String ownerPluginId;

  PluginScopedExtensions(MutableExtensionRegistry delegate, String ownerPluginId) {
    this.delegate = delegate;
    this.ownerPluginId = ownerPluginId;
  }

  @Override
  public <T> Disposable register(
      ExtensionPoint<T> point, T extension, String ownerPluginId, int priority) {
    return delegate.register(point, extension, ownerPluginId, priority);
  }

  @Override
  public <T> Disposable register(ExtensionPoint<T> point, T extension) {
    return delegate.register(point, extension, ownerPluginId, 0);
  }

  @Override
  public void unregisterOwner(String ownerPluginId) {
    delegate.unregisterOwner(ownerPluginId);
  }

  @Override
  public <T> List<ExtensionRegistration<T>> registrations(ExtensionPoint<T> point) {
    return delegate.registrations(point);
  }
}
