package ir.hanzodev1375.ghostide.codeeditors.langs.lsp.gth;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.Future;

import io.github.rosemoe.sora.lsp.client.connection.StreamConnectionProvider;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;

/**
 * A {@link StreamConnectionProvider} that hosts the GhostIDE theme language server <i>inside</i>
 * the editor process. No node/npm/proot binary is required: the LSP4j {@link
 * GhostThemeLanguageServer} runs in-process and is bridged to the Sora LSP client through paired
 * in-memory pipes speaking standard JSON-RPC/stdio framing.
 */
public class EmbeddedGthConnectionProvider implements StreamConnectionProvider {

  private static final int BUFFER_SIZE = 64 * 1024;

  private Launcher<LanguageClient> launcher;
  private java.util.concurrent.Future<Void> listeningFuture;
  private PipedInputStream clientInput;
  private PipedOutputStream clientOutput;
  private volatile boolean closed = true;

  public EmbeddedGthConnectionProvider() {}

  @Override
  public void start() throws IOException {
    // serverToClient: the server writes responses that the editor client reads.
    PipedOutputStream serverOut = new PipedOutputStream();
    clientInput = new PipedInputStream(serverOut, BUFFER_SIZE);

    // clientToServer: the editor client writes requests that the server reads.
    PipedOutputStream clientOutWrite = new PipedOutputStream();
    PipedInputStream serverInput = new PipedInputStream(clientOutWrite, BUFFER_SIZE);

    GhostThemeLanguageServer server = new GhostThemeLanguageServer();
    launcher = LSPLauncher.createServerLauncher(server, serverInput, serverOut);
    server.connect(launcher.getRemoteProxy());
    listeningFuture = launcher.startListening();

    clientOutput = clientOutWrite;
    closed = false;
  }

  @Override
  public InputStream getInputStream() {
    return clientInput;
  }

  @Override
  public OutputStream getOutputStream() {
    return clientOutput;
  }

  @Override
  public boolean isClosed() {
    return closed || launcher == null;
  }

  @Override
  public void close() {
    closed = true;
    if (listeningFuture != null) {
      listeningFuture.cancel(true);
    }
    closeQuietly(clientInput);
    closeQuietly(clientOutput);
  }

  private static void closeQuietly(Closeable closeable) {
    if (closeable == null) return;
    try {
      closeable.close();
    } catch (IOException ignored) {
      // Nothing useful to do here.
    }
  }
}
