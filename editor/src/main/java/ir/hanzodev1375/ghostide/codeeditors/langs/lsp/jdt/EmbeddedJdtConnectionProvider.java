package ir.hanzodev1375.ghostide.codeeditors.langs.lsp.jdt;

import android.content.Context;

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
import ir.hanzodev1375.ghostide.javacore.JdtLanguageServer;

/**
 * A {@link StreamConnectionProvider} that hosts the in-process Eclipse JDT Java language server. No
 * external jdtls/node/JVM is required: the LSP4j {@link JdtLanguageServer} runs inside the editor
 * process and is bridged to the Sora LSP client through paired in-memory pipes speaking standard
 * JSON-RPC/stdio framing — the same pattern as {@code EmbeddedGthConnectionProvider}.
 */
public class EmbeddedJdtConnectionProvider implements StreamConnectionProvider {

  private static final int BUFFER_SIZE = 64 * 1024;

  private final Context context;
  private final String projectRoot;

  private Launcher<LanguageClient> launcher;
  private Future<Void> listeningFuture;
  private PipedInputStream clientInput;
  private PipedOutputStream clientOutput;
  private volatile boolean closed = true;

  public EmbeddedJdtConnectionProvider(Context context, String projectRoot) {
    this.context = context;
    this.projectRoot = projectRoot;
  }

  @Override
  public void start() throws IOException {
    try {
      // serverToClient: the server writes responses that the editor client reads.
      PipedOutputStream serverOut = new PipedOutputStream();
      clientInput = new PipedInputStream(serverOut, BUFFER_SIZE);

      // clientToServer: the editor client writes requests that the server reads.
      PipedOutputStream clientOutWrite = new PipedOutputStream();
      PipedInputStream serverInput = new PipedInputStream(clientOutWrite, BUFFER_SIZE);

      JdtLanguageServer server = new JdtLanguageServer(projectRoot);
      server.setClasspathProvider(new AndroidJavaClasspathProvider(context));
      server.setErrorListener(new AndroidJdtErrorReporter(context));
      launcher = LSPLauncher.createServerLauncher(server, serverInput, serverOut);
      server.connect(launcher.getRemoteProxy());
      listeningFuture = launcher.startListening();

      clientOutput = clientOutWrite;
      closed = false;
    } catch (Throwable t) {
      ir.hanzodev1375.ghostide.javacore.JdtRuntime.report(
          "start", "JDT in-process server failed to start", t, true);
      throw new IOException("JDT server start failed", t);
    }
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
