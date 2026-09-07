package ir.hanzodev1375.components.store.api;

import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import ir.hanzodev1375.components.store.model.PluginDoc;
import ir.hanzodev1375.components.store.model.PluginItem;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PluginStoreApi {

  private static final String OPEN_JSON_URL =
      "https://raw.githubusercontent.com/HanzoDev1375/ghostideplugins/main/open.json";

  private static final OkHttpClient CLIENT =
      new OkHttpClient.Builder()
          .connectTimeout(15, TimeUnit.SECONDS)
          .readTimeout(30, TimeUnit.SECONDS)
          .build();

  private static final Gson GSON = new Gson();

  public interface ListCallback {
    void onSuccess(List<PluginItem> plugins);

    void onError(String message);
  }

  public interface DocCallback {
    void onSuccess(PluginDoc doc);

    void onError(String message);
  }

  public static void fetchPlugins(ListCallback callback) {
    Request request = new Request.Builder().url(OPEN_JSON_URL).get().build();
    CLIENT
        .newCall(request)
        .enqueue(
            new Callback() {
              @Override
              public void onFailure(Call call, IOException e) {
                postListError(callback, e.getMessage());
              }

              @Override
              public void onResponse(Call call, Response response) throws IOException {
                try (Response resp = response) {
                  if (!resp.isSuccessful() || resp.body() == null) {
                    postListError(callback, "HTTP " + resp.code());
                    return;
                  }
                  String json = resp.body().string();
                  Type listType = new TypeToken<List<PluginItem>>() {}.getType();
                  List<PluginItem> items = GSON.fromJson(json, listType);
                  postListSuccess(callback, items != null ? items : new ArrayList<>());
                } catch (Exception e) {
                  postListError(callback, e.getMessage());
                }
              }
            });
  }

  public static void fetchDoc(String docUrl, DocCallback callback) {
    Request request = new Request.Builder().url(docUrl).get().build();
    CLIENT
        .newCall(request)
        .enqueue(
            new Callback() {
              @Override
              public void onFailure(Call call, IOException e) {
                postDocError(callback, e.getMessage());
              }

              @Override
              public void onResponse(Call call, Response response) throws IOException {
                try (Response resp = response) {
                  if (!resp.isSuccessful() || resp.body() == null) {
                    postDocError(callback, "HTTP " + resp.code());
                    return;
                  }
                  String json = resp.body().string();
                  PluginDoc doc = GSON.fromJson(json, PluginDoc.class);
                  postDocSuccess(callback, doc != null ? doc : new PluginDoc("", "", ""));
                } catch (Exception e) {
                  postDocError(callback, e.getMessage());
                }
              }
            });
  }

  private static void postListSuccess(ListCallback callback, List<PluginItem> items) {
    if (callback == null) return;
    new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(items));
  }

  private static void postListError(ListCallback callback, String message) {
    if (callback == null) return;
    new Handler(Looper.getMainLooper()).post(() -> callback.onError(message));
  }

  private static void postDocSuccess(DocCallback callback, PluginDoc doc) {
    if (callback == null) return;
    new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(doc));
  }

  private static void postDocError(DocCallback callback, String message) {
    if (callback == null) return;
    new Handler(Looper.getMainLooper()).post(() -> callback.onError(message));
  }
}
