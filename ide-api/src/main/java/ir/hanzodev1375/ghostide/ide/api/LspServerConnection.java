package ir.hanzodev1375.ghostide.ide.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A language server connection. Implementations must start lazily in {@link #start()}, never in a
 * constructor, and must send server diagnostics to stderr rather than {@link #getOutputStream()}
 * so the LSP stdio framing is never corrupted.
 */
public interface LspServerConnection extends AutoCloseable {

  void start() throws IOException;

  OutputStream getOutputStream();

  InputStream getInputStream();

  boolean isClosed();

  @Override
  void close();
}
