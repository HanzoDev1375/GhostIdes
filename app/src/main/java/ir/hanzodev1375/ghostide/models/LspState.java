package ir.hanzodev1375.ghostide.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.github.rosemoe.sora.lsp.editor.LspEditorStatus;

public final class LspState {

  public enum Status {
    IDLE,
    CONNECTING,
    CONNECTED,
    DISCONNECTED,
    ERROR
  }

  @NonNull public final Status status;
  @Nullable public final String filePath;
  @Nullable public final String errorMessage;

  private LspState(
      @NonNull Status status, @Nullable String filePath, @Nullable String errorMessage) {
    this.status = status;
    this.filePath = filePath;
    this.errorMessage = errorMessage;
  }

  public static LspState idle() {
    return new LspState(Status.IDLE, null, null);
  }

  public static LspState connecting(@NonNull String filePath) {
    return new LspState(Status.CONNECTING, filePath, null);
  }

  public static LspState connected(@NonNull String filePath) {
    return new LspState(Status.CONNECTED, filePath, null);
  }

  public static LspState error(@NonNull String filePath, @Nullable String message) {
    return new LspState(Status.ERROR, filePath, message);
  }

  public static LspState fromStatus(@NonNull String filePath, @NonNull LspEditorStatus status) {
    switch (status) {
      case CONNECTING:
        return connecting(filePath);
      case CONNECTED:
        return connected(filePath);
      case DISCONNECTED:
        return new LspState(Status.DISCONNECTED, filePath, null);
      case IDLE:
      default:
        return idle();
    }
  }

  public boolean isConnecting() {
    return status == Status.CONNECTING;
  }

  public boolean isConnected() {
    return status == Status.CONNECTED;
  }

  public boolean hasError() {
    return status == Status.ERROR;
  }
}
