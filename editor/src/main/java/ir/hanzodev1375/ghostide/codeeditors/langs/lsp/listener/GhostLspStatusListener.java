package ir.hanzodev1375.ghostide.codeeditors.langs.lsp.listener;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.github.rosemoe.sora.lsp.editor.LspEditor;
import io.github.rosemoe.sora.lsp.editor.LspEditorEventListener;
import io.github.rosemoe.sora.lsp.editor.LspEditorStatus;
import ir.hanzodev1375.ghostide.codeeditors.langs.lsp.LspUriBridge;

/**
 * Listener for LSP editor status changes. Logs status transitions and notifies a callback when
 * status is not IDLE.
 */
public class GhostLspStatusListener implements LspEditorEventListener {

  private static final String TAG = "GhostLsp";

  public interface OnLspStatusChanged {
    void onStatusChanged(
        @NonNull String filePath,
        @NonNull LspEditorStatus newStatus,
        @NonNull LspEditorStatus oldStatus);
  }

  @Nullable private OnLspStatusChanged callback;
  @Nullable private String filePath;

  public GhostLspStatusListener() {}

  public GhostLspStatusListener(@Nullable String filePath) {
    this.filePath = filePath;
  }

  public void setCallback(@Nullable OnLspStatusChanged callback) {
    this.callback = callback;
  }

  public void setFilePath(@Nullable String filePath) {
    this.filePath = filePath;
  }

  @Override
  public void onStatusChanged(
      @NonNull LspEditor editor, @NonNull LspEditorStatus newStatus, @NonNull LspEditorStatus oldStatus) {
    String path = filePath != null ? filePath : LspUriBridge.uri(editor).getPath();
    Log.d(TAG, "LSP status: " + oldStatus + " → " + newStatus + " [" + path + "]");

    if (newStatus == LspEditorStatus.CONNECTED) {
      Log.i(TAG, "LSP connected: " + path);
    } else if (newStatus == LspEditorStatus.DISCONNECTED) {
      Log.w(TAG, "LSP disconnected: " + path);
    } else if (newStatus == LspEditorStatus.CONNECTING) {
      Log.d(TAG, "LSP connecting: " + path);
    }

    if (newStatus != LspEditorStatus.IDLE && callback != null) {
      callback.onStatusChanged(path, newStatus, oldStatus);
    }
  }
}
