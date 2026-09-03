package ir.hanzodev1375.components.store.data;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import ir.hanzodev1375.components.store.api.FontsApi;
import ir.hanzodev1375.components.store.model.FontInfo;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class FontsRepository {

  private static final String FONTS_DIR = "ghostide/fonts";
  private static final OkHttpClient client =
      new OkHttpClient.Builder()
          .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
          .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
          .build();

  public void search(Context context, String query, Callback<List<FontInfo>> callback) {
    FontsApi.searchFonts(
        context,
        query,
        new FontsApi.Callbacks() {
          @Override
          public void onSuccess(List<FontInfo> fonts) {
            if (callback != null) callback.onSuccess(fonts);
          }

          @Override
          public void onError(String message) {
            if (callback != null) callback.onError(message);
          }
        });
  }

  public String getFontsDir() {
    File root = Environment.getExternalStorageDirectory();
    return new File(root, FONTS_DIR).getAbsolutePath();
  }

  public boolean downloadFont(FontInfo font, String targetDir) throws IOException {
    if (font == null || font.family == null) {
      throw new IOException("no font family");
    }
    File dir = new File(targetDir);
    if (!dir.exists()) {
      dir.mkdirs();
      File publicDir = new File(Environment.getExternalStorageDirectory(), FONTS_DIR);
      publicDir.mkdirs();
    }
    List<String> urls = FontsApi.fontFileUrls(font.family);
    if (urls.isEmpty()) {
      throw new IOException("no font files");
    }
    boolean any = false;
    int index = 0;
    for (String url : urls) {
      String fileName = sanitize(font.family) + "_" + (++index) + FontsApi.fileExtensionOf(url);
      File out = new File(dir, fileName);
      if (downloadTo(url, out)) {
        any = true;
      }
    }
    return any;
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

  private static String sanitize(String variant) {
    return variant.replaceAll("[^A-Za-z0-9_-]", "_");
  }

  public interface Callback<T> {
    void onSuccess(T data);

    void onError(String message);
  }
}
