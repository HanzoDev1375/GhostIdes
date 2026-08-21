package ir.hanzodev1375.ghostide.adapters;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;

import ir.hanzodev1375.ghostide.activity.EditorActivity;
import ir.hanzodev1375.ghostide.ide.ui.api.CodeRunnerHost;
import ir.hanzodev1375.ghostide.ide.ui.api.EditorActionHandler;
import ir.hanzodev1375.ghostide.ide.ui.api.IdeHostServices;
import ir.hanzodev1375.ghostide.ide.ui.api.PluginUiExtensionPoints;
import ir.hanzodev1375.ghostide.plugin.api.GlobalRegistry;
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
          if (path == null) return;
          if (dispatchToPlugins(CMD_RUN_CURRENT_FILE, path, asBottomSheet)) return;
          delegate.bindof(path, asBottomSheet);
        });
  }

  @Override
  public void runFile(String filePath, boolean asBottomSheet) {
    mainHandler.post(
        () -> {
          if (dispatchToPlugins(CMD_RUN_FILE, filePath, asBottomSheet)) return;
          delegate.bindof(filePath, asBottomSheet);
        });
  }

  @Override
  public boolean isSupported(String filePath) {
    return delegate.isSupported(filePath);
  }

  private static final String CMD_RUN_FILE = "ghostide.runFile";
  private static final String CMD_RUN_CURRENT_FILE = "ghostide.runCurrentFile";

  private boolean dispatchToPlugins(String command, String filePath, boolean asBottomSheet) {
    List<EditorActionHandler> handlers =
        GlobalRegistry.extensions()
            .extensions(PluginUiExtensionPoints.EDITOR_ACTION_HANDLER);
    for (EditorActionHandler handler : handlers) {
      if (command.equals(handler.getCommandId())) {
        ArrayList<Object> args = new ArrayList<>();
        args.add(filePath);
        args.add(asBottomSheet);
        if (handler.execute(null, command, args)) {
          return true;
        }
      }
    }
    return false;
  }
}
