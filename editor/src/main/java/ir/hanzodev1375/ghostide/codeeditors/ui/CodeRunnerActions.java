package ir.hanzodev1375.ghostide.codeeditors.ui;

import java.util.List;

import ir.hanzodev1375.ghostide.codeeditors.IdeEditor;
import ir.hanzodev1375.ghostide.ide.ui.api.CodeRunnerHost;
import ir.hanzodev1375.ghostide.ide.ui.api.EditorActionHandler;
import ir.hanzodev1375.ghostide.ide.ui.api.IdeHostServices;
import ir.hanzodev1375.ghostide.ide.ui.api.PluginUiExtensionPoints;
import ir.hanzodev1375.ghostide.plugin.api.GlobalRegistry;

/**
 * Bridges LSP command actions to the host's {@link CodeRunnerHost}. Commands the editor receives
 * from code actions (sent by a plugin language server) are normally forwarded back to the server
 * via {@code workspace/executeCommand}; this class lets the editor handle the built-in {@code
 * ghostide.*} run commands locally instead, and lets plugins intercept their own commands through
 * an {@link EditorActionHandler} registered at {@link PluginUiExtensionPoints#EDITOR_ACTION_HANDLER}.
 */
public final class CodeRunnerActions {

  /** {@code arguments = [String shellCommand, Boolean asBottomSheet?]} */
  public static final String CMD_RUN_SHELL = "ghostide.runShell";

  /** {@code arguments = [String filePath, Boolean asBottomSheet?]} */
  public static final String CMD_RUN_FILE = "ghostide.runFile";

  /** {@code arguments = [Boolean asBottomSheet?]} — runs the currently open file like the FAB. */
  public static final String CMD_RUN_CURRENT_FILE = "ghostide.runCurrentFile";

  private CodeRunnerActions() {}

  /** Whether {@code command} is one the host runs locally instead of forwarding to the server. */
  public static boolean isGhostCommand(String command) {
    return CMD_RUN_SHELL.equals(command)
        || CMD_RUN_FILE.equals(command)
        || CMD_RUN_CURRENT_FILE.equals(command);
  }

  /**
   * Tries to handle an LSP command locally. Built-in {@code ghostide.*} run commands are handled
   * first; afterwards any plugin-registered {@link EditorActionHandler} with a matching {@link
   * EditorActionHandler#getCommandId()} gets a chance, with the raw editor widget passed along.
   *
   * @param editor the {@code IdeEditor} behind the action (never null at call sites)
   * @param command the command id received from the language server
   * @param arguments command arguments sent by the language server, may be empty
   * @return {@code true} if handled locally, {@code false} if the caller should forward it to the
   *     language server as before
   */
  public static boolean execute(IdeEditor editor, String command, List<Object> arguments) {
    if (command == null) {
      return false;
    }
    if (runBuiltin(command, arguments)) {
      return true;
    }
    for (EditorActionHandler handler :
        GlobalRegistry.extensions().extensions(PluginUiExtensionPoints.EDITOR_ACTION_HANDLER)) {
      if (command.equals(handler.getCommandId()) && handler.execute(editor, command, arguments)) {
        return true;
      }
    }
    return false;
  }

  private static boolean runBuiltin(String command, List<Object> arguments) {
    CodeRunnerHost runner = GlobalRegistry.services().get(IdeHostServices.CODE_RUNNER_HOST);
    if (runner == null) {
      return false;
    }
    switch (command) {
      case CMD_RUN_CURRENT_FILE:
        runner.runCurrentFile(asBottomSheet(arguments, 0));
        return true;
      case CMD_RUN_FILE:
        String file = arg(arguments, 0);
        if (file == null) {
          return false;
        }
        runner.runFile(file, asBottomSheet(arguments, 1));
        return true;
      case CMD_RUN_SHELL:
        String shell = arg(arguments, 0);
        if (shell == null) {
          return false;
        }
        runner.runShell(shell, asBottomSheet(arguments, 1));
        return true;
      default:
        return false;
    }
  }

  private static String arg(List<Object> arguments, int index) {
    if (arguments == null || index >= arguments.size()) {
      return null;
    }
    Object value = arguments.get(index);
    return value == null ? null : String.valueOf(value);
  }

  private static boolean asBottomSheet(List<Object> arguments, int index) {
    if (arguments == null || index >= arguments.size()) {
      return true;
    }
    Object value = arguments.get(index);
    return value instanceof Boolean ? (Boolean) value : true;
  }
}
