package ir.hanzodev1375.ghostide.ide.ui.api;

import android.content.Context;
import java.io.File;

/**
 * Published as a service by the host under {@link IdeHostServices#EDITOR_HOST}. Describes only
 * the capabilities a plugin needs from the editor screen, never the concrete {@code
 * EditorActivity}/{@code IdeEditor} classes, so {@code :app} stays the only module that knows
 * about them.
 */
public interface EditorHost {

  File getProjectRoot();

  File getOpenFile();

  String getEditorText();

  void setEditorText(String text);

  void openFile(File file);

  Context getContext();
}
