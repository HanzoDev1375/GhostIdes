package ir.hanzodev1375.ghostide.activity.pluginmanager;

import android.content.Context;

import java.io.File;

import ir.hanzodev1375.ghostide.activity.FileManagerActivity;
import ir.hanzodev1375.ghostide.ide.ui.api.FileManagerHost;

/**
 * Wraps one {@link FileManagerActivity} instance. {@link #getSelectedFile()} always returns
 * {@code null}: this screen has no single-file selection concept yet, only the directory
 * currently being browsed.
 */
public final class FileManagerHostAdapter implements FileManagerHost {

  private final FileManagerActivity activity;

  public FileManagerHostAdapter(FileManagerActivity activity) {
    this.activity = activity;
  }

  @Override
  public File getRootDirectory() {
    String path = activity.getCurrentDirectoryPath();
    return path == null ? null : new File(path);
  }

  @Override
  public File getSelectedFile() {
    return null;
  }

  @Override
  public void refresh() {
    activity.refreshCurrentDirectory();
  }

  @Override
  public void openFile(File file) {
    activity.setupClick(file.getAbsolutePath(), file.getName());
  }

  @Override
  public Context getContext() {
    return activity;
  }
}
