package ir.hanzodev1375.ghostide.codeeditors.langs.lsp;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import io.github.rosemoe.sora.lsp.client.connection.StreamConnectionProvider;
import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.CustomLanguageServerDefinition;
import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.LanguageServerDefinition;
import io.github.rosemoe.sora.lsp.editor.LspEditor;
import io.github.rosemoe.sora.lsp.editor.LspProject;
import io.github.rosemoe.sora.widget.CodeEditor;

import ir.hanzodev1375.ghostide.ide.api.EditorExtensionPoints;
import ir.hanzodev1375.ghostide.ide.api.LspServerConnection;
import ir.hanzodev1375.ghostide.ide.api.LspServerDefinition;
import ir.hanzodev1375.ghostide.ide.api.LspServerProvider;
import ir.hanzodev1375.ghostide.ide.api.LspServerRequest;
import ir.hanzodev1375.ghostide.plugin.api.GlobalRegistry;

/**
 * Resolves the highest-priority {@link LspServerProvider} registered for a file and turns its
 * {@link LspServerDefinition} into the {@link LanguageServerDefinition} type Sora editor's {@code
 * LspProject} expects. {@link ir.hanzodev1375.ghostide.codeeditors.langs.lsp.LspRouter} calls this
 * before falling back to its built-in, hardcoded per-language classes.
 *
 * <p>{@link #connectFile} follows the exact {@code LspProject.createEditor} /
 * {@code connectWithTimeoutBlocking} sequence {@code JavaServer} already uses successfully; it
 * does not call {@code LspEditor.setWrapperLanguage}, so a registry-provided language gets LSP
 * features (completion, diagnostics, hover) but not the language-specific formatter integration
 * a hand-written built-in like {@code JavaServer} wires up separately.
 */
public final class LspExtensionBridge {

  private static final String TAG = "LspExtensionBridge";
  private static final Map<String, LspProject> PROJECTS = new ConcurrentHashMap<>();

  private LspExtensionBridge() {}

  public static LspServerProvider findProvider(LspServerRequest request) {
    List<LspServerProvider> providers =
        GlobalRegistry.extensions().extensions(EditorExtensionPoints.LSP_SERVER_PROVIDER);
    for (LspServerProvider provider : providers) {
      if (provider.supports(request)) {
        return provider;
      }
    }
    return null;
  }

  public static LspServerProvider findProvider(String projectRoot, String filePath) {
    if (projectRoot == null || filePath == null) {
      return null;
    }
    return findProvider(new LspServerRequest(new File(projectRoot), new File(filePath)));
  }

  /**
   * Looks up a provider using the file's own parent directory as a stand-in project root, for
   * call sites that only know a file path (support/installed checks happen before a project is
   * open). {@link LspServerRequest#extension()} is all a well-behaved {@code supports()} needs.
   */
  public static LspServerProvider findProviderForFile(String filePath) {
    if (filePath == null) {
      return null;
    }
    File file = new File(filePath);
    File projectRoot = file.getParentFile() != null ? file.getParentFile() : file;
    return findProvider(new LspServerRequest(projectRoot, file));
  }

  public static LanguageServerDefinition toSoraDefinition(
      LspServerDefinition definition, LspServerRequest request) {
    return new CustomLanguageServerDefinition(
        request.extension(),
        workingDir -> toStreamConnectionProvider(definition, request),
        definition.getDisplayName(),
        definition.getExpectedCapabilities(),
        null);
  }

  private static StreamConnectionProvider toStreamConnectionProvider(
      LspServerDefinition definition, LspServerRequest request) {
    LspServerConnection connection = definition.getConnectionFactory().create(request);
    if (connection instanceof StreamConnectionProvider soraProvider) {
      return soraProvider;
    }
    return new LspServerConnectionStreamAdapter(connection);
  }

  public static LspEditor connectFile(
      Context context,
      LspServerProvider provider,
      String projectRoot,
      String filePath,
      CodeEditor editor) {
    LspServerRequest request = new LspServerRequest(new File(projectRoot), new File(filePath));
    LspServerDefinition definition = provider.createDefinition(request);
    String projectKey = provider.getId() + "|" + projectRoot;
    LspProject project =
        PROJECTS.computeIfAbsent(
            projectKey,
            key -> {
              LspProject created = new LspProject(projectRoot);
              created.addServerDefinition(toSoraDefinition(definition, request));
              return created;
            });

    final LspEditor[] holder = new LspEditor[1];
    final CountDownLatch latch = new CountDownLatch(1);
    new Handler(Looper.getMainLooper())
        .post(
            () -> {
              try {
                LspEditor lspEditor = project.createEditor(filePath);
                lspEditor.setEditor(editor);
                holder[0] = lspEditor;
              } finally {
                latch.countDown();
              }
            });
    try {
      latch.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    LspEditor lspEditor = holder[0];
    if (lspEditor != null) {
      try {
        lspEditor.connectWithTimeoutBlocking();
      } catch (Exception e) {
        Log.e(TAG, "Failed to connect to plugin LSP provider " + provider.getId(), e);
      }
    }
    return lspEditor;
  }
}
