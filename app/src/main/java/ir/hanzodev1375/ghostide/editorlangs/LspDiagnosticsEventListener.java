package ir.hanzodev1375.ghostide.editorlangs;

import androidx.annotation.NonNull;
import io.github.rosemoe.sora.lsp.editor.LspEditor;
import io.github.rosemoe.sora.lsp.events.EventContext;
import io.github.rosemoe.sora.lsp.events.EventListener;
import java.util.List;
import org.eclipse.lsp4j.Diagnostic;

/**
 * Fires whenever the connected LSP server publishes a fresh diagnostic set for a file.
 *
 * <p>Sora's {@link LspEditor} emits the {@code editor/publishDiagnostics} event on its shared
 * {@code EventEmitter} every time {@code DefaultLanguageClient.publishDiagnostics} runs. Because
 * the emitter is shared per {@code LspProject}, the listener keeps an editors-identity check so it
 * only reacts to its own {@link LspEditor}.
 */
public final class LspDiagnosticsEventListener implements EventListener {

  /** Called once per diagnostics update. Runs on the LSP notification thread. */
  public interface Callback {
    void onDiagnosticsChanged(@NonNull LspEditor editor, @NonNull List<Diagnostic> diagnostics);
  }

  private static final String EVENT_PUBLISH_DIAGNOSTICS = "editor/publishDiagnostics";

  private final LspEditor editor;
  private final Callback callback;

  public LspDiagnosticsEventListener(@NonNull LspEditor editor, @NonNull Callback callback) {
    this.editor = editor;
    this.callback = callback;
  }

  @NonNull
  public LspEditor getEditor() {
    return editor;
  }

  @NonNull
  @Override
  public String getEventName() {
    return EVENT_PUBLISH_DIAGNOSTICS;
  }

  @Override
  public void handle(@NonNull EventContext context) {
    Object eventEditor = context.getOrNull("lsp-editor");
    if (eventEditor != editor) {
      return;
    }
    callback.onDiagnosticsChanged(editor, editor.getDiagnostics());
  }
}