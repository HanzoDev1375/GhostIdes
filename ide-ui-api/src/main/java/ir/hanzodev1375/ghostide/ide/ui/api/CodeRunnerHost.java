package ir.hanzodev1375.ghostide.ide.ui.api;

import androidx.annotation.Nullable;
import java.util.function.Consumer;

/**
 * Published as a service by the host under {@link IdeHostServices#CODE_RUNNER_HOST}. Lets a plugin
 * run shell commands or source files exactly like the editor's run (FAB) button does: the command
 * is handed to the IDE terminal, either as a bottom sheet or a full screen.
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
 *
 * // run headlessly and read the output programmatically (blocking!)
 * ExecResult r = runner.exec("ls -1 /sdcard/Project", null);
 * if (r.exitCode() == 0) {  parse r.output() }
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

  /**
   * Runs {@code command} headlessly — no terminal UI — and returns once the process exits. Runs in
   * the app's shell environment ({@code sh -c}), which is not necessarily identical to the
   * interactive terminal session; binaries living inside the proot rootfs should go through {@link
   * ProotProcessLauncher} instead. <b>Blocking:</b> call from a background thread only.
   *
   * @param onOutputLine invoked per merged stdout/stderr line as it arrives, may be {@code null};
   *     called on the executing thread, so it must be thread-safe and fast
   * @return exit code plus the full collected output (empty string when the process failed to
   *     start, with a negative exit code)
   */
  ExecResult exec(String command, @Nullable Consumer<String> onOutputLine);

  /** Outcome of a headless {@link #exec(String, Consumer)} run. */
  record ExecResult(int exitCode, String output) {

    /** Convenience for "process ran and exited with 0". */
    public boolean success() {
      return exitCode == 0;
    }
  }
}
