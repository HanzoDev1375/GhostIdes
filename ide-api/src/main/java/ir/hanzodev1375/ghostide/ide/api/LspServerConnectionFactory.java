package ir.hanzodev1375.ghostide.ide.api;

@FunctionalInterface
public interface LspServerConnectionFactory {

  LspServerConnection create(LspServerRequest request);
}
