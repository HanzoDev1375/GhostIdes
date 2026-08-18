package ir.hanzodev1375.ghostide.codeeditors.langs.lsp;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.LanguageServerDefinition;
import io.github.rosemoe.sora.lsp.editor.LspEditor;
import io.github.rosemoe.sora.lsp.editor.LspProject;
import io.github.rosemoe.sora.widget.CodeEditor;
import ir.hanzodev1375.ghostide.codeeditors.langs.formatHelp.DebianBootstrap;

/**
 * Abstract base class for all LSP language servers.
 * <p>
 * Every server follows the same connection flow: locate the binary, obtain or create
 * an {@link LspProject}, register a {@link LanguageServerDefinition}, and attach the
 * file to the editor. This class centralises that boilerplate while leaving the
 * server-specific parts (binary name, extensions, language creation) to subclasses.
 * <p>
 * To add a new language server, create a class that extends this one and implement
 * the abstract methods.
 *
 * @author Ghost
 */
public abstract class LspContentImpl {

  protected final String tag;
  protected final String serverName;
  protected final Set<String> supportedExtensions;
  protected final String[] candidatePaths;

  private final Map<String, LspProject> projects = new HashMap<>();
  private final Set<String> registeredDefinitions = new HashSet<>();

  protected LspContentImpl(String tag, String serverName, Set<String> supportedExtensions,
                           String[] candidatePaths) {
    this.tag = tag;
    this.serverName = serverName;
    this.supportedExtensions = supportedExtensions;
    this.candidatePaths = candidatePaths;
  }

  // ─────────────────────── Abstract ───────────────────────

  /** Returns {@code true} if the given file path is handled by this server. */
  public abstract boolean isSupportedFile(String filePath);

  /** Builds the {@link LanguageServerDefinition} for a particular file extension. */
  protected abstract LanguageServerDefinition createDefinition(
      Context context, String executablePath, String ext);

  /**
   * Connects a file to its language server.
   * <p>
   * Default implementation: locate binary, obtain/create project, register definition,
   * then run the connection handshake on the main thread. Subclasses may override
   * this entirely or rely on {@link #onEditorCreated} for fine-grained control.
   */
  public LspEditor connectFile(Context context, String projectRoot,
                               String filePath, CodeEditor editor) {
    String executablePath = findInstalledExecutable(context);
    if (executablePath == null) {
      Log.e(tag, serverName + " is not installed.");
      return null;
    }

    String ext = extensionOf(filePath);
    LspProject project = getOrCreateProject(projectRoot);
    ensureDefinitionRegistered(project, context, executablePath, projectRoot, ext);

    final LspEditor[] holder = new LspEditor[1];
    final CountDownLatch latch = new CountDownLatch(1);

    new Handler(Looper.getMainLooper()).post(() -> {
      try {
        holder[0] = onEditorCreated(project.createEditor(filePath), editor);
      } finally {
        latch.countDown();
      }
    });

    try {
      latch.await();
    } catch (InterruptedException ignored) {
      Thread.currentThread().interrupt();
    }

    LspEditor lspEditor = holder[0];
    if (lspEditor == null) return null;

    try {
      lspEditor.connectWithTimeoutBlocking();
    } catch (Exception e) {
      Log.e(tag, "Failed to connect to " + serverName, e);
    }
    return lspEditor;
  }

  // ─────────────────────── Hooks ───────────────────────

  /**
   * Called after the {@link LspEditor} has been created so the subclass can attach
   * a wrapper language, enable features, or set a formatter.
   *
   * @return the same {@code lspEditor} instance (convenience for chaining)
   */
  protected LspEditor onEditorCreated(LspEditor lspEditor, CodeEditor editor) {
    return lspEditor;
  }

  /**
   * Unique key used to prevent duplicate server-definition registration per
   * project root and extension. Override when a server registers under a
   * different namespace (e.g. Emmet alongside HTML).
   */
  protected String definitionKey(String projectRoot, String ext) {
    return projectRoot + "::" + ext;
  }

  // ─────────────────────── Shared logic ───────────────────────

  public boolean isInstalled(Context context) {
    return findInstalledExecutable(context) != null;
  }

  public String findInstalledExecutable(Context context) {
    File rootfs = DebianBootstrap.getRootfsDir(context);
    if (rootfs == null || !rootfs.exists()) return null;
    for (String candidate : candidatePaths) {
      File f = new File(rootfs, candidate.substring(1));
      if (f.exists()) return candidate;
    }
    return null;
  }

  public void disconnectFile(LspEditor lspEditor) {
    if (lspEditor == null) return;
    try {
      lspEditor.dispose();
    } catch (Exception e) {
      Log.e(tag, "Failed to disconnect " + serverName, e);
    }
  }

  // ─────────────────────── Internal utilities ───────────────────────

  protected static String extensionOf(String filePath) {
    if (filePath == null) return "";
    int dot = filePath.lastIndexOf('.');
    if (dot < 0 || dot == filePath.length() - 1) return "";
    return filePath.substring(dot + 1).toLowerCase(Locale.ROOT);
  }

  protected synchronized LspProject getOrCreateProject(String projectRoot) {
    LspProject project = projects.get(projectRoot);
    if (project == null) {
      project = new LspProject(projectRoot);
      projects.put(projectRoot, project);
    }
    return project;
  }

  protected synchronized void ensureDefinitionRegistered(
      LspProject project, Context context, String executablePath,
      String projectRoot, String ext) {
    String key = definitionKey(projectRoot, ext);
    if (!registeredDefinitions.contains(key)) {
      project.addServerDefinition(createDefinition(context, executablePath, ext));
      registeredDefinitions.add(key);
    }
  }
}
