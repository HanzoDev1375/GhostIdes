package ir.hanzodev1375.components.store.data;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

import ir.hanzodev1375.components.store.api.PluginStoreApi;
import ir.hanzodev1375.components.store.model.PluginDoc;
import ir.hanzodev1375.components.store.model.PluginItem;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PluginRepository {

  private static final String TAG = "PluginRepository";
  private static final String SOURCE_DIR = "ghostide/source";
  public static final String GPL_PLUGINS_DIR = "ghostide/plugins";
  private static final String GPL_FILE_NAME = ".gpl";

  private static final OkHttpClient CLIENT =
      new OkHttpClient.Builder()
          .connectTimeout(15, TimeUnit.SECONDS)
          .readTimeout(60, TimeUnit.SECONDS)
          .writeTimeout(60, TimeUnit.SECONDS)
          .build();

  public interface Callback<T> {
    void onSuccess(T data);

    void onError(String message);
  }

  public void fetchPlugins(Callback<List<PluginItem>> callback) {
    if (callback == null) return;
    PluginStoreApi.fetchPlugins(
        new PluginStoreApi.ListCallback() {
          @Override
          public void onSuccess(List<PluginItem> plugins) {
            callback.onSuccess(plugins);
          }

          @Override
          public void onError(String message) {
            callback.onError(message);
          }
        });
  }

  public void fetchDoc(PluginItem item, Callback<PluginDoc> callback) {
    if (callback == null) return;
    if (item == null || item.doc() == null || item.doc().trim().isEmpty()) {
      callback.onError("no doc url");
      return;
    }
    PluginStoreApi.fetchDoc(
        item.doc(),
        new PluginStoreApi.DocCallback() {
          @Override
          public void onSuccess(PluginDoc doc) {
            callback.onSuccess(doc);
          }

          @Override
          public void onError(String message) {
            callback.onError(message);
          }
        });
  }

  /** Directory where downloaded/installed .gpl packages persist on external storage. */
  public static File pluginsDir() {
    return new File(Environment.getExternalStorageDirectory(), GPL_PLUGINS_DIR);
  }

  /** Download the .gpl package into {@code /storage/emulated/0/ghostide/plugins/} and return it. */
  public void downloadGpl(Context context, PluginItem item, Callback<File> callback) {
    if (item == null || !item.hasGpl()) {
      postError(callback, "no gpl url");
      return;
    }
    try {
      File out = downloadGplSync(context, item);
      postSuccess(callback, out);
    } catch (Exception e) {
      Log.e(TAG, "gpl download failed", e);
      postError(callback, e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
    }
  }

  /** Blocking variant used by the app-side installer (runs on a background thread). */
  public File downloadGplSync(Context context, PluginItem item) throws IOException {
    if (item == null || !item.hasGpl()) {
      throw new IOException("no gpl url");
    }
    File dir = pluginsDir();
    if (!dir.exists() && !dir.mkdirs()) {
      throw new IOException("could not create plugins dir " + dir);
    }
    File out = new File(dir, safeName(item.name()) + GPL_FILE_NAME);
    Request request = new Request.Builder().url(toGithubRawUrl(item.gplfile())).get().build();
    try (Response response = CLIENT.newCall(request).execute()) {
      if (!response.isSuccessful() || response.body() == null) {
        throw new IOException("HTTP " + response.code());
      }
      writeTo(response.body().byteStream(), out);
    }
    return out;
  }

  /** Turns a github.com/.../blob/... URL into the raw file URL so it can be downloaded. */
  static String toGithubRawUrl(String url) {
    if (url == null || url.trim().isEmpty()) {
      return "";
    }
    String blob = "/blob/";
    int i = url.indexOf(blob);
    if (i < 0) {
      return url;
    }
    String base = url.substring(0, i);
    String rest = url.substring(i + blob.length());
    if (!base.contains("github.com")) {
      return url;
    }
    String repo = base.substring(base.indexOf("github.com") + "github.com".length());
    return "https://raw.githubusercontent.com" + repo + rest;
  }

  /** Download the plugin source into /sdcard/ghostide/source/<name> via the GitHub API. */
  public void downloadSource(PluginItem item, Callback<File> callback) {
    if (item == null || !item.hasSource()) {
      postError(callback, "no source url");
      return;
    }
    File root = Environment.getExternalStorageDirectory();
    File destDir = new File(new File(root, SOURCE_DIR), safeName(item.name()));
    try {
      String apiUrl = sourceTreeApiUrl(item.source());
      if (apiUrl == null) {
        postError(callback, "bad source url");
        return;
      }
      if (!destDir.exists() && !destDir.mkdirs()) {
        postError(callback, "could not create source dir");
        return;
      }
      walkSource(apiUrl, destDir);
      postSuccess(callback, destDir);
    } catch (Exception e) {
      Log.e(TAG, "source download failed", e);
      postError(callback, e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
    }
  }

  private static String sourceTreeApiUrl(String url) {
    int com = url.indexOf(".com/");
    if (com < 0) return null;
    String rest = url.substring(com + 5);
    String[] parts = rest.split("/");
    if (parts.length < 2) return null;

    StringBuilder api = new StringBuilder("https://api.github.com/repos/");
    api.append(parts[0]).append('/').append(parts[1]).append("/contents");

    int i = 2;
    if (i >= parts.length) {
      return api.append("?ref=main").toString();
    }
    if (parts[i].equals("tree")) {
      i++;
    }
    if (i >= parts.length) {
      return api.append("?ref=main").toString();
    }
    String branch = parts[i];
    i++;
    if (branch.equals("refs") && i < parts.length && parts[i].equals("heads")) {
      i++;
      if (i < parts.length) {
        branch = parts[i];
        i++;
      } else {
        return null;
      }
    }
    for (int j = i; j < parts.length; j++) {
      api.append('/').append(parts[j]);
    }
    return api.append("?ref=").append(branch).toString();
  }

  private void walkSource(String apiUrl, File destDir) throws Exception {
    String json = getString(apiUrl);
    if (json == null) return;
    JsonArray array = JsonParser.parseString(json).getAsJsonArray();
    for (int k = 0; k < array.size(); k++) {
      JsonObject entry = array.get(k).getAsJsonObject();
      String name = entry.has("name") ? entry.get("name").getAsString() : null;
      if (name == null) continue;
      File child = new File(destDir, name);
      String type = entry.has("type") ? entry.get("type").getAsString() : "";
      if ("dir".equals(type)) {
        walkSource(entry.get("url").getAsString(), child);
      } else if ("file".equals(type)) {
        String downloadUrl =
            entry.has("download_url") && !entry.get("download_url").isJsonNull()
                ? entry.get("download_url").getAsString()
                : null;
        if (downloadUrl != null) {
          String content = getString(downloadUrl);
          if (content != null) {
            File parent = child.getParentFile();
            if (parent != null && !parent.exists()) {
              parent.mkdirs();
            }
            writeTo(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), child);
          }
        }
      }
    }
  }

  private String getString(String url) throws IOException {
    Request request = new Request.Builder().url(url).get().build();
    try (Response response = CLIENT.newCall(request).execute()) {
      if (!response.isSuccessful() || response.body() == null) return null;
      return response.body().string();
    }
  }

  private void writeTo(InputStream input, File out) throws IOException {
    try (InputStream in = input; FileOutputStream fos = new FileOutputStream(out)) {
      byte[] buffer = new byte[8192];
      int read;
      while ((read = in.read(buffer)) != -1) {
        fos.write(buffer, 0, read);
      }
    }
  }

  private static String safeName(String name) {
    return name.replaceAll("[^A-Za-z0-9_.-]", "_");
  }

  private <T> void postSuccess(Callback<T> callback, T data) {
    if (callback == null) return;
    new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(data));
  }

  private <T> void postError(Callback<T> callback, String message) {
    if (callback == null) return;
    new Handler(Looper.getMainLooper()).post(() -> callback.onError(message));
  }
}