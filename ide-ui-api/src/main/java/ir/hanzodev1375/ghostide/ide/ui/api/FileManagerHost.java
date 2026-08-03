package ir.hanzodev1375.ghostide.ide.ui.api;

import android.content.Context;
import java.io.File;

/** Published as a service by the host under {@link IdeHostServices#FILE_MANAGER_HOST}. */
public interface FileManagerHost {

  File getRootDirectory();

  File getSelectedFile();

  void refresh();

  void openFile(File file);

  Context getContext();
}
