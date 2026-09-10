package ir.hanzodev1375.ghostide.store;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.blankj.utilcode.util.FileIOUtils;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ir.hanzodev1375.components.store.api.ThemesApi;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ThemeStorePreviewViewModel extends AndroidViewModel {

  public static final int STATE_IDLE = 0;
  public static final int STATE_DOWNLOADING = 1;
  public static final int STATE_READY = 2;
  public static final int STATE_ERROR = 3;

  private static final OkHttpClient client =
      new OkHttpClient.Builder()
          .connectTimeout(15, TimeUnit.SECONDS)
          .readTimeout(60, TimeUnit.SECONDS)
          .build();

  private final MutableLiveData<Integer> state = new MutableLiveData<>(STATE_IDLE);
  private final MutableLiveData<Integer> progress = new MutableLiveData<>(0);

  private File appliedThemeFile;

  public ThemeStorePreviewViewModel(@NonNull Application application) {
    super(application);
  }

  public LiveData<Integer> getState() {
    return state;
  }

  public LiveData<Integer> getProgress() {
    return progress;
  }

  public File getAppliedThemeFile() {
    return appliedThemeFile;
  }

  public void download(String url) {
    if (url == null || url.isEmpty()) {
      state.postValue(STATE_ERROR);
      return;
    }
    Integer current = state.getValue();
    if (current != null && current == STATE_DOWNLOADING) {
      return;
    }
    state.postValue(STATE_DOWNLOADING);
    progress.postValue(0);

    new Thread(
            () -> {
              try {
                File dir = new File(getApplication().getCacheDir(), "theme_preview");
                clearCache(dir);
                dir.mkdirs();

                int dot = url.lastIndexOf('/');
                File out = new File(dir, "preview.gth");
                downloadFile(url, out);

                String themeDirUrl =
                    dot >= 0 ? url.substring(0, url.lastIndexOf('/') + 1) : ThemesApi.REPO_BASE;
                downloadBackground(out, themeDirUrl);

                if (out.exists()) {
                  appliedThemeFile = out;
                  state.postValue(STATE_READY);
                } else {
                  state.postValue(STATE_ERROR);
                }
              } catch (Exception e) {
                state.postValue(STATE_ERROR);
              }
            })
        .start();
  }

  private void downloadFile(String url, File out) throws Exception {
    Request request = new Request.Builder().url(url).get().build();
    try (Response response = client.newCall(request).execute()) {
      if (!response.isSuccessful() || response.body() == null) {
        throw new IllegalStateException("bad response");
      }
      long total = response.body().contentLength();
      try (FileOutputStream fos = new FileOutputStream(out);
          InputStream is = response.body().byteStream()) {
        byte[] buffer = new byte[8192];
        int read;
        long done = 0;
        while ((read = is.read(buffer)) != -1) {
          fos.write(buffer, 0, read);
          done += read;
          if (total > 0) {
            int percent = (int) ((done * 100) / total);
            progress.postValue(percent);
          }
        }
      }
    }
  }

  private void clearCache(File dir) {
    if (dir == null || !dir.exists()) return;
    File[] files = dir.listFiles();
    if (files == null) return;
    for (File f : files) {
      f.delete();
    }
  }

  private void downloadBackground(File themeFile, String themeDirUrl) {
    try {
      String json = FileIOUtils.readFile2String(themeFile);
      JsonObject root = JsonParser.parseString(json).getAsJsonObject();
      if (!root.has("widget")) return;
      JsonObject widget = root.getAsJsonObject("widget");
      if (!widget.has("imagepath")) return;
      String imagepath = widget.get("imagepath").getAsString();
      if (imagepath == null || imagepath.isEmpty()) return;
      if (imagepath.startsWith("http")
          || imagepath.startsWith("/")
          || imagepath.startsWith("content:")
          || imagepath.startsWith("file:")) {
        return;
      }
      while (imagepath.startsWith("../")) {
        imagepath = imagepath.substring(3);
      }
      if (imagepath.startsWith("./")) {
        imagepath = imagepath.substring(2);
      }
      String bgUrl = themeDirUrl + imagepath;
      String bgName = new File(bgUrl).getName();
      File bg = new File(themeFile.getParentFile(), bgName);
      Request request = new Request.Builder().url(bgUrl).get().build();
      try (Response response = client.newCall(request).execute()) {
        if (response.isSuccessful() && response.body() != null) {
          try (OutputStream fos = new FileOutputStream(bg);
              InputStream is = response.body().byteStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
              fos.write(buffer, 0, read);
            }
          }
        }
      }
    } catch (Exception ignored) {
    }
  }
}