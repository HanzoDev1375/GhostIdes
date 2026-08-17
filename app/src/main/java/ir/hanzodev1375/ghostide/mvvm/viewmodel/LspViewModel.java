package ir.hanzodev1375.ghostide.mvvm.viewmodel;

import android.app.Application;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import io.github.rosemoe.sora.lsp.editor.LspEditor;
import ir.hanzodev1375.ghostide.codeeditors.langs.lsp.LspRouter;
import ir.hanzodev1375.ghostide.codeeditors.langs.lsp.listener.GhostLspStatusListener;
import ir.hanzodev1375.ghostide.models.LspState;
import io.github.rosemoe.sora.widget.CodeEditor;

public class LspViewModel extends AndroidViewModel {

  private static final String TAG = "LspViewModel";

  private final MutableLiveData<LspState> state = new MutableLiveData<>(LspState.idle());
  private final MutableLiveData<String> error = new MutableLiveData<>();

  @Nullable private volatile LspEditor lspEditor;
  @Nullable private GhostLspStatusListener statusListener;

  public LspViewModel(@NonNull Application app) {
    super(app);
  }

  public LiveData<LspState> getState() {
    return state;
  }

  public LiveData<String> getError() {
    return error;
  }

  @Nullable
  public LspEditor getLspEditor() {
    return lspEditor;
  }

  public boolean isConnected() {
    LspEditor editor = lspEditor;
    return editor != null && editor.isConnected();
  }

  public void connect(String projectRoot, String filePath, CodeEditor editor) {
    if (filePath == null || editor == null) return;

    LspState current = state.getValue();
    if (current != null && current.isConnecting()) return;

    state.postValue(LspState.connecting(filePath));

    new Thread(
            () -> {
              try {
                LspEditor connected =
                    LspRouter.connectFile(
                        getApplication().getApplicationContext(), projectRoot, filePath, editor);

                if (connected == null) {
                  state.postValue(LspState.error(filePath, "سرور LSP یافت نشد"));
                  return;
                }

                lspEditor = connected;

                statusListener = new GhostLspStatusListener(filePath);
                statusListener.setCallback(
                    (path, newStatus, oldStatus) ->
                        state.postValue(LspState.fromStatus(path, newStatus)));
                connected.setEventListener(statusListener);

                state.postValue(LspState.connected(filePath));
              } catch (Exception e) {
                Log.e(TAG, "اتصال LSP ناموفق: " + filePath, e);
                state.postValue(LspState.error(filePath, e.getMessage()));
                error.postValue(e.getMessage());
              }
            })
        .start();
  }

  public void disconnect() {
    LspEditor editor = lspEditor;
    if (editor != null) {
      try {
        editor.dispose();
      } catch (Exception e) {
        Log.e(TAG, "بستن اتصال LSP خطا داشت", e);
      }
      lspEditor = null;
      statusListener = null;
    }
    state.postValue(LspState.idle());
  }

  public void applySettings(
      boolean enableHover, boolean enableInlayHint, boolean enableSignatureHelp) {
    LspEditor editor = lspEditor;
    if (editor == null) return;
    editor.setEnableHover(enableHover);
    editor.setEnableInlayHint(enableInlayHint);
    editor.setEnableSignatureHelp(enableSignatureHelp);
  }

  @Override
  protected void onCleared() {
    super.onCleared();
    disconnect();
  }
}
