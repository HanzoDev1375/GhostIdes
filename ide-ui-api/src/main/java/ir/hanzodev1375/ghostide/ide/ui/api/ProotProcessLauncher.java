package ir.hanzodev1375.ghostide.ide.ui.api;

import java.util.List;

import ir.hanzodev1375.ghostide.ide.api.LspServerConnection;

/**
 * Published as a service by the host under {@link IdeHostServices#PROOT_PROCESS_LAUNCHER}. Lets a
 * plugin run a language server binary inside the same proot Linux rootfs the built-in Java and
 * C/C++ support already use, instead of re-implementing process sandboxing itself.
 */
public interface ProotProcessLauncher {

  /** Whether {@code guestExecutable} (an absolute path inside the rootfs) exists right now. */
  boolean isInstalled(String guestExecutable);

  /**
   * Builds a not-yet-started connection to {@code guestExecutable}. Pass it as the {@code
   * connectionFactory} result inside {@code LspServerDefinition}; the host calls {@code start()}
   * lazily, same as any other {@link LspServerConnection}.
   *
   * @param workingDir absolute path on the host filesystem, bind-mounted into the rootfs and used
   *     as the process's working directory
   * @param guestExecutable absolute path inside the rootfs, e.g. {@code "/usr/bin/rust-analyzer"}
   * @param args extra command-line arguments, may be empty
   */
  LspServerConnection launch(String workingDir, String guestExecutable, List<String> args);
}
