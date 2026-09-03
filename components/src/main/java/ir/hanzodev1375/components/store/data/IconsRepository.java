package ir.hanzodev1375.components.store.data;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import ir.hanzodev1375.components.store.api.IconsApi;
import ir.hanzodev1375.components.store.model.IconInfo;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class IconsRepository {

  private static final String ICONS_DIR = "ghostide/icons";
  private static final OkHttpClient client =
      new OkHttpClient.Builder()
          .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
          .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
          .build();

  public void search(Context context, String query, Callback<List<IconInfo>> callback) {
    IconsApi.searchIcons(
        context,
        query,
        new IconsApi.Callbacks() {
          @Override
          public void onSuccess(List<IconInfo> icons) {
            if (callback != null) callback.onSuccess(icons);
          }

          @Override
          public void onError(String message) {
            if (callback != null) callback.onError(message);
          }
        });
  }

  public String getIconsDir() {
    File root = Environment.getExternalStorageDirectory();
    return new File(root, ICONS_DIR).getAbsolutePath();
  }

  public boolean downloadIcon(IconInfo icon, int style, String targetDir) throws IOException {
    if (icon == null || icon.name == null || !IconsApi.isSupported(icon, style)) {
      throw new IOException("unsupported icon");
    }
    String url = IconsApi.svgUrl(icon, style);
    if (url == null) throw new IOException("no icon url");
    File dir = new File(targetDir);
    if (!dir.exists()) {
      dir.mkdirs();
      new File(Environment.getExternalStorageDirectory(), ICONS_DIR).mkdirs();
    }
    String fileName = icon.name + "_s" + style + ".svg";
    return downloadTo(url, new File(dir, fileName));
  }

  private boolean downloadTo(String url, File out) {
    try {
      Request request = new Request.Builder().url(url).get().build();
      Response response = client.newCall(request).execute();
      if (!response.isSuccessful()) {
        return false;
      }
      InputStream is = response.body().byteStream();
      try (FileOutputStream fos = new FileOutputStream(out)) {
        byte[] buffer = new byte[8192];
        int read;
        while ((read = is.read(buffer)) != -1) {
          fos.write(buffer, 0, read);
        }
      }
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public interface Callback<T> {
    void onSuccess(T data);

    void onError(String message);
  }
}
