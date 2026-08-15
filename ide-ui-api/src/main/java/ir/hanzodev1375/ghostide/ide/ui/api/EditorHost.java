package ir.hanzodev1375.ghostide.ide.ui.api;

import android.content.Context;
import androidx.annotation.Nullable;
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

  /**
   * Optional access to the raw editor widget behind the current tab. This is the host's {@code
   * IdeEditor} (an {@code io.github.rosemoe.sora.widget.CodeEditor} subclass), exposed as {@code
   * Object} because {@code IdeEditor} lives in the host editor module, not in this API. Plugins
   * that need the concrete type add the host editor module as a {@code compileOnly} dependency
   * and cast:
   *
   * <pre>{@code
   * Object raw = editorHost.getEditor();
   * if (raw instanceof IdeEditor ide) {   // add ':editor' as compileOnly to cast
   *   ide.getLspEditor();
   *   ide.getCurrentFilePath();
   * }
   * }</pre>
   *
   * @return the editor widget behind the current tab, or {@code null} when no file is open
   */
  @Nullable
  Object getEditor();
}
