package ir.hanzodev1375.ghostide.utils;

import android.content.Context;
import android.content.SharedPreferences;
import static ir.hanzodev1375.ghostide.utils.ConstKeys.*;

public class PathManager {

  private final SharedPreferences prefs;

  public PathManager(Context context) {
    prefs = context.getSharedPreferences(PrfsName, Context.MODE_PRIVATE);
  }

  public void savePath(boolean save) {
    prefs.edit().putBoolean(KEY_SAVE_PATH, save).apply();
  }

  public boolean isSaveEnabled() {
    return prefs.getBoolean(KEY_SAVE_PATH, false);
  }

  public void setLastPath(String path) {
    if (isSaveEnabled()) {
      prefs.edit().putString(KEY_LAST_PATH, path).apply();
    }
  }

  public String getLastPath() {
    String saved = prefs.getString(KEY_LAST_PATH, null);
    if (saved != null && new java.io.File(saved).exists()) {
      return saved;
    }
    return android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
  }

  public void clearLastPath() {
    prefs.edit().remove(KEY_LAST_PATH).apply();
  }
}
