package ir.hanzodev1375.ghostide.activity.pluginmanager;

import android.content.Context;

import java.io.File;

import ir.hanzodev1375.ghostide.activity.EditorActivity;
import ir.hanzodev1375.ghostide.codeeditors.IdeEditor;
import ir.hanzodev1375.ghostide.ide.ui.api.EditorHost;

/** Wraps one {@link EditorActivity} instance; registered and disposed alongside its lifecycle. */
public final class EditorHostAdapter implements EditorHost {

  private final EditorActivity activity;

  public EditorHostAdapter(EditorActivity activity) {
    this.activity = activity;
  }

  @Override
  public File getProjectRoot() {
    String path = activity.getCurrentFilePath();
    if (path == null) {
      return activity.getFilesDir();
    }
    File file = new File(path);
    File parent = file.getParentFile();
    return parent != null ? parent : file;
  }

  @Override
  public File getOpenFile() {
    String path = activity.getCurrentFilePath();
    return path == null ? null : new File(path);
  }

  @Override
  public String getEditorText() {
    IdeEditor editor = activity.getEditor();
    return editor == null ? null : editor.getText().toString();
  }

  @Override
  public void setEditorText(String text) {
    IdeEditor editor = activity.getEditor();
    if (editor != null) {
      editor.setText(text);
    }
  }

  @Override
  public void openFile(File file) {
    activity.openFile(file.getAbsolutePath());
  }

  @Override
  public Context getContext() {
    return activity;
  }
}
