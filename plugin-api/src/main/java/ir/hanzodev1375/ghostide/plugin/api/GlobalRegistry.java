package ir.hanzodev1375.ghostide.plugin.api;

/**
 * Single shared {@link MutableExtensionRegistry} and {@link MutableServiceRegistry} for the whole
 * process. Feature modules such as {@code :editor} read from it to resolve providers; {@code
 * :app} writes built-in and plugin-contributed registrations into it. Keeping this holder in
 * {@code :plugin-api} lets both sides depend on it without depending on each other.
 */
public final class GlobalRegistry {

  private static final DefaultExtensionRegistry EXTENSIONS = new DefaultExtensionRegistry();
  private static final DefaultServiceRegistry SERVICES = new DefaultServiceRegistry();

  private GlobalRegistry() {}

  public static MutableExtensionRegistry extensions() {
    return EXTENSIONS;
  }

  public static MutableServiceRegistry services() {
    return SERVICES;
  }
}
