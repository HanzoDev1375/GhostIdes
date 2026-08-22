package ir.hanzodev1375.ghostide.ide.ui.api;

/**
 * Contributes custom file icons, overriding the built-in icon set for the paths a plugin decides.
 *
 * <p>Register an implementation at {@link PluginUiExtensionPoints#FILE_ICON_CONTRIBUTOR}.
 * Contributors are consulted in descending priority order before the default {@code
 * file_icons.json} mapping; the first non-null answer wins. Returning {@code null} means "I don't
 * handle this path" and the next contributor &mdash; or the built-in set &mdash; takes over.
 *
 * <p>{@link #getIcon} runs on bind time for every visible row of the file manager and every editor
 * tab, so keep it fast and free of I/O on the UI thread.
 */
public interface FileIconContributor {

  /**
   * Returns the icon for the given absolute file path.
   *
   * <p>The returned string is interpreted as follows:
   *
   * <ul>
   *   <li>Contains {@code "://"} &rarr; treated as a full URI ({@code file://}, {@code content://},
   *       ...) and loaded directly by Glide. Use this for artwork shipped with your plugin.
   *   <li>Anything else &rarr; treated as an icon name from the built-in set, resolved as {@code
   *       vscode_icons/<name>.svg} inside the host assets (e.g. {@code file_type_kotlin}).
   *   <li>{@code null} or blank &rarr; fall through to the next contributor / default mapping.
   * </ul>
   *
   * @param filePath absolute path of the file or directory being displayed
   * @return icon URI, built-in icon name, or {@code null} to pass
   */
  String getIcon(String filePath);
}
