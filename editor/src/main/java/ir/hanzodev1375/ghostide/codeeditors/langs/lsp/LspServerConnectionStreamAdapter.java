package ir.hanzodev1375.ghostide.codeeditors.langs.lsp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import io.github.rosemoe.sora.lsp.client.connection.StreamConnectionProvider;
import ir.hanzodev1375.ghostide.ide.api.LspServerConnection;

/**
 * Delegates every {@link StreamConnectionProvider} call to a wrapped {@link LspServerConnection}
 * so any registered {@code LspServerProvider}, built-in or plugin-supplied, can back a Sora editor
 * {@code CustomLanguageServerDefinition} without Sora types leaking into {@code :ide-api}.
 */
final class LspServerConnectionStreamAdapter implements StreamConnectionProvider {

  private final LspServerConnection connection;

  LspServerConnectionStreamAdapter(LspServerConnection connection) {
    this.connection = connection;
  }

  @Override
  public void start() throws IOException {
    connection.start();
  }

  @Override
  public InputStream getInputStream() {
    return connection.getInputStream();
  }

  @Override
  public OutputStream getOutputStream() {
    return connection.getOutputStream();
  }

  @Override
  public boolean isClosed() {
    return connection.isClosed();
  }

  @Override
  public void close() {
    connection.close();
  }
}
