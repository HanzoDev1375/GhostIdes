package ir.hanzodev1375.ghostide.codeeditors.langs.lsp;

import java.util.List;

import io.github.rosemoe.sora.lsp.client.connection.StreamConnectionProvider;
import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.CustomLanguageServerDefinition;
import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.LanguageServerDefinition;

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
 */
public final class LspExtensionBridge {

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

  public static LanguageServerDefinition toSoraDefinition(
      LspServerDefinition definition, LspServerRequest request) {
    return new CustomLanguageServerDefinition(
        request.extension(),
        workingDir -> toStreamConnectionProvider(definition, request),
        definition.getDisplayName(),
        definition.getExpectedCapabilities(),
        definition.getInitializationOptions());
  }

  private static StreamConnectionProvider toStreamConnectionProvider(
      LspServerDefinition definition, LspServerRequest request) {
    LspServerConnection connection = definition.getConnectionFactory().create(request);
    if (connection instanceof StreamConnectionProvider soraProvider) {
      return soraProvider;
    }
    return new LspServerConnectionStreamAdapter(connection);
  }
}
