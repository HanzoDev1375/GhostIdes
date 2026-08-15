package ir.hanzodev1375.ghostide.ide.ui.api;

/**
 * Published as a service by the host under {@link IdeHostServices#CODE_RUNNER_HOST}. Lets a
 * plugin run shell commands or source files exactly like the editor's run (FAB) button does:
 * the command is handed to the IDE terminal, either as a bottom sheet or a full screen.
 *
 * <pre>{@code
 * CodeRunnerHost runner = context.getServices().require(IdeHostServices.CODE_RUNNER_HOST);
 *
 * // run an arbitrary shell command (asBottomSheet = true -> terminal as a bottom sheet)
 * runner.runShell("python3 main.py", true);
 *
 * // run the file that is currently open in the editor, like pressing the FAB
 * runner.runCurrentFile();
 *
 * // run a specific file by path, full screen terminal
 * runner.runFile("/sdcard/Project/main.py", false);
 * }</pre>
 */
public interface CodeRunnerHost {

  /** Runs {@code command} in the terminal; opens as a bottom sheet when possible. */
  default void runShell(String command) {
    runShell(command, true);
  }

  void runShell(String command, boolean asBottomSheet);

  /** Runs the currently open file like the editor FAB does (terminal mode from settings). */
  default void runCurrentFile() {
    runCurrentFile(true);
  }

  void runCurrentFile(boolean asBottomSheet);

  /** Runs {@code filePath} like the FAB would for it; terminal opens as a bottom sheet. */
  default void runFile(String filePath) {
    runFile(filePath, true);
  }

  void runFile(String filePath, boolean asBottomSheet);

  /** Whether the runner knows how to execute a file (known extension / registered language). */
  boolean isSupported(String filePath);
}
