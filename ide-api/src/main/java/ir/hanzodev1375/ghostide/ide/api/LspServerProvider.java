package ir.hanzodev1375.ghostide.ide.api;

import ir.hanzodev1375.ghostide.plugin.api.ConfigurableExtension;

/**
 * Contributes a language server to the editor. Register an instance at {@link
 * EditorExtensionPoints#LSP_SERVER_PROVIDER}.
 *
 * <pre>{@code
 * public final class RustLspProvider implements LspServerProvider {
 *   public String getId() {
 *     return "com.example.rust-analyzer";
 *   }
 *
 *   public boolean supports(LspServerRequest request) {
 *     return "rs".equalsIgnoreCase(request.extension());
 *   }
 *
 *   public LspServerDefinition createDefinition(LspServerRequest request) {
 *     return LspServerDefinition.builder(getId(), Set.of("rs"), "rust-analyzer", RustConnection::new)
 *         .build();
 *   }
 * }
 * }</pre>
 */
public interface LspServerProvider extends ConfigurableExtension {

  default int getPriority() {
    return 0;
  }

  boolean supports(LspServerRequest request);

  LspServerDefinition createDefinition(LspServerRequest request);
}
