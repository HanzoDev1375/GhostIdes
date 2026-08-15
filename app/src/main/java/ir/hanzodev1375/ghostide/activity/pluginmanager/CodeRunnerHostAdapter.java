package ir.hanzodev1375.ghostide.activity.pluginmanager;

import android.os.Handler;
import android.os.Looper;

import ir.hanzodev1375.ghostide.activity.EditorActivity;
import ir.hanzodev1375.ghostide.ide.ui.api.CodeRunnerHost;
import ir.hanzodev1375.ghostide.plugin.PluginPanelHost;
import ir.hanzodev1375.ghostide.runer.CodeRuner;

/** Wraps one {@link EditorActivity} + {@link CodeRuner}; registered alongside its lifecycle. */
public final class CodeRunnerHostAdapter implements CodeRunnerHost {

  private final EditorActivity activity;
  private final CodeRuner delegate;
  private final Handler mainHandler = new Handler(Looper.getMainLooper());

  public CodeRunnerHostAdapter(EditorActivity activity) {
    this.activity = activity;
    this.delegate = new CodeRuner(activity);
  }

  @Override
  public void runShell(String command, boolean asBottomSheet) {
    mainHandler.post(() -> delegate.runShell(command, asBottomSheet));
  }

  @Override
  public void runCurrentFile(boolean asBottomSheet) {
    mainHandler.post(
        () -> {
          String path = PluginPanelHost.resolveLastPath(activity::getCurrentFilePath);
          if (path != null) {
            delegate.bindof(path, asBottomSheet);
          }
        });
  }

  @Override
  public void runFile(String filePath, boolean asBottomSheet) {
    mainHandler.post(() -> delegate.bindof(filePath, asBottomSheet));
  }

  @Override
  public boolean isSupported(String filePath) {
    return delegate.isSupported(filePath);
  }
}
