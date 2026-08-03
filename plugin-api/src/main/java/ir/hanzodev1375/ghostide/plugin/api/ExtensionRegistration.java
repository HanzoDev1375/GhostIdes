package ir.hanzodev1375.ghostide.plugin.api;

/**
 * One contribution registered at an {@link ExtensionPoint}.
 *
 * @param <T> the extension contract type
 */
public record ExtensionRegistration<T>(
    ExtensionPoint<T> point, T extension, String ownerPluginId, int priority) {}
