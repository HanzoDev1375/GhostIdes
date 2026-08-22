package ir.hanzodev1375.ghostide.materialfileicon.core;

import android.widget.ImageView;
import java.io.File;

public class FileIconHelper {

  private String filePath;
  private String mimeType;

  private boolean isDynamicFolderEnabled;
  private boolean isEnvironmentEnabled;

  private JsonFileIconHelper jsonHelper;

  public FileIconHelper(String filePath) {
    this(filePath, "");
  }

  public FileIconHelper(String filePath, String mimeType) {
    this.filePath = filePath == null ? "" : filePath;
    this.mimeType = mimeType == null ? "" : mimeType;
    rebuild();
  }

  private void rebuild() {
    jsonHelper = new JsonFileIconHelper(filePath);
  }

  public void setFilePath(String filePath) {
    this.filePath = filePath == null ? "" : filePath;
    rebuild();
  }

  public String getFilePath() {
    return filePath;
  }

  public void setMimeType(String mimeType) {
    this.mimeType = mimeType;
  }

  public String getMimeType() {
    return mimeType;
  }

  public String getFileIcon() {
    if (isDirectory() && !isDynamicFolderEnabled)
      return jsonHelper.getIconUri(false);
    return jsonHelper.getIconUri();
  }

  public String getFileIconName() {
    return jsonHelper.getIconName(isDirectory() && isDynamicFolderEnabled);
  }

  public void setDynamicFolderEnabled(boolean isDynamicFolderEnabled) {
    this.isDynamicFolderEnabled = isDynamicFolderEnabled;
  }

  public boolean isDynamicFolderEnabled() {
    return isDynamicFolderEnabled;
  }

  public void setEnvironmentEnabled(boolean isEnvironmentEnabled) {
    this.isEnvironmentEnabled = isEnvironmentEnabled;
  }

  public boolean isEnvironmentEnabled() {
    return isEnvironmentEnabled;
  }

  public void bindIcon(ImageView imageView) {
    JsonFileIconHelper.load(imageView.getContext());
    jsonHelper.bindIcon(imageView);
  }

  private boolean isDirectory() {
    File f = new File(filePath);
    return f.exists() && f.isDirectory();
  }
}
