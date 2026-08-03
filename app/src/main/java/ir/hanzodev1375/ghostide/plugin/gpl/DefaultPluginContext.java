package ir.hanzodev1375.ghostide.plugin.gpl;

import java.util.ArrayList;
import java.util.List;

import ir.hanzodev1375.ghostide.plugin.api.Disposable;
import ir.hanzodev1375.ghostide.plugin.api.MutableExtensionRegistry;
import ir.hanzodev1375.ghostide.plugin.api.MutableServiceRegistry;
import ir.hanzodev1375.ghostide.plugin.api.PluginContext;
import ir.hanzodev1375.ghostide.plugin.api.PluginDescriptor;
import ir.hanzodev1375.ghostide.plugin.api.PluginLogger;

/**
 * Extensions register into the shared {@code GlobalRegistry.extensions()}, but services use a
 * per-plugin copy so each plugin can be given its own scoped {@code Context} without leaking it
 * to other plugins.
 */
final class DefaultPluginContext implements PluginContext {

  private final PluginDescriptor descriptor;
  private final MutableExtensionRegistry extensions;
  private final MutableServiceRegistry services;
  private final PluginLogger logger;
  private final List<Disposable> disposables = new ArrayList<>();

  DefaultPluginContext(
      PluginDescriptor descriptor,
      MutableExtensionRegistry extensions,
      MutableServiceRegistry services,
      PluginLogger logger) {
    this.descriptor = descriptor;
    this.extensions = extensions;
    this.services = services;
    this.logger = logger;
  }

  @Override
  public PluginDescriptor getDescriptor() {
    return descriptor;
  }

  @Override
  public MutableExtensionRegistry getExtensions() {
    return extensions;
  }

  @Override
  public MutableServiceRegistry getServices() {
    return services;
  }

  @Override
  public PluginLogger getLogger() {
    return logger;
  }

  @Override
  public Disposable registerDisposable(Disposable disposable) {
    disposables.add(disposable);
    return disposable;
  }

  void disposeAll() {
    for (int i = disposables.size() - 1; i >= 0; i--) {
      disposables.get(i).dispose();
    }
    disposables.clear();
  }
}
