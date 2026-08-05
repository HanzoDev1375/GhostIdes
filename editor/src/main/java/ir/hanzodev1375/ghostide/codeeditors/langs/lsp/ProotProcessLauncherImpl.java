package ir.hanzodev1375.ghostide.codeeditors.langs.lsp;

import android.content.Context;

import java.io.File;
import java.util.List;

import ir.hanzodev1375.ghostide.codeeditors.langs.formatHelp.DebianBootstrap;
import ir.hanzodev1375.ghostide.ide.api.LspServerConnection;
import ir.hanzodev1375.ghostide.ide.ui.api.ProotProcessLauncher;

public final class ProotProcessLauncherImpl implements ProotProcessLauncher {

  private final Context appContext;

  public ProotProcessLauncherImpl(Context context) {
    this.appContext = context.getApplicationContext();
  }

  @Override
  public boolean isInstalled(String guestExecutable) {
    File rootfs = DebianBootstrap.getRootfsDir(appContext);
    String relativeGuestPath =
        guestExecutable.startsWith("/") ? guestExecutable.substring(1) : guestExecutable;
    return new File(rootfs, relativeGuestPath).exists();
  }

  @Override
  public LspServerConnection launch(String workingDir, String guestExecutable, List<String> args) {
    return new ProotStdioConnectionProvider(appContext, workingDir, guestExecutable, args);
  }
}
