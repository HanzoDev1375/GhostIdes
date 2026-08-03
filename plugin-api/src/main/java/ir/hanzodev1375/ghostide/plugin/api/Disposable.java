package ir.hanzodev1375.ghostide.plugin.api;

/**
 * A single unit of cleanup owned by a plugin registration. Every extension or service
 * registration returns a {@code Disposable}; plugins must register it with {@link
 * PluginContext#registerDisposable(Disposable)} so the runtime can release it on unload.
 */
@FunctionalInterface
public interface Disposable {

  Disposable NONE = () -> {};

  void dispose();
}
