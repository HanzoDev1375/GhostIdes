package ir.hanzodev1375.ghostide.ide.ui.api;

import java.util.List;

/**
 * Handles one client-side action/command coming from a plugin language server. Commands the
 * editor receives from LSP code actions are normally forwarded back to the server via {@code
 * workspace/executeCommand}. Registering an {@code EditorActionHandler} at {@link
 * PluginUiExtensionPoints#EDITOR_ACTION_HANDLER} lets a plugin intercept its own commands
 * locally instead, where the raw editor widget is available.
 *
 * <p>The {@code editor} argument is an <em>optional</em> output: it is the host's {@code
 * IdeEditor} behind the action, or {@code null} when no editor is attached. {@code IdeEditor}
 * itself lives in the host editor module (not in this API), so plugin authors that need the
 * concrete type add the host editor module as a {@code compileOnly} dependency and cast:
 *
 * <pre>{@code
 * public final class MyActionHandler implements EditorActionHandler {
 *   public String getCommandId() {
 *     return "com.example.myplugin.run";
 *   }
 *
 *   public boolean execute(Object editor, String command, List<Object> arguments) {
 *     if (editor instanceof IdeEditor ide) {      // add ':editor' as compileOnly to cast
 *       ide.getCurrentFilePath();
 *     }
 *     return true;
 *   }
 * }
 * }</pre>
 */
public interface EditorActionHandler {

  /** The command id this handler owns, e.g. {@code "com.example.myplugin.run"}. */
  String getCommandId();

  /**
   * Called when the editor receives a command whose id equals {@link #getCommandId()}. May run
   * on a background thread, so hop to the UI thread before touching the editor.
   *
   * @param editor the raw editor widget (host's {@code IdeEditor}) or {@code null}
   * @param command the exact command id that was received
   * @param arguments command arguments as sent by the language server, may be empty
   * @return {@code true} if the action was handled; {@code false} lets the editor forward the
   *     command to the language server as before
   */
  boolean execute(Object editor, String command, List<Object> arguments);
}
