package ir.hanzodev1375.components.store.api;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import ir.hanzodev1375.components.R;
import ir.hanzodev1375.components.store.model.ThemeItem;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ThemesApi {

  public static final String REPO_BASE =
      "https://raw.githubusercontent.com/HanzoDev1375/ghostidetheme/main/";
  private static final String THEMES_URL = REPO_BASE + "theme.json";

  private static final OkHttpClient client =
      new OkHttpClient.Builder()
          .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
          .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
          .build();
  private static final Gson gson = new Gson();

  public interface Callbacks {
    void onSuccess(List<ThemeItem> themes);

    void onError(String message);
  }

  public interface TextCallbacks {
    void onSuccess(String content);

    void onError(String message);
  }

  public static void fetchText(Context context, String url, TextCallbacks callbacks) {
    if (url == null || url.isEmpty()) {
      postError(callbacks, context.getString(R.string.themes_description_failed));
      return;
    }
    Request request = new Request.Builder().url(url).get().build();
    client
        .newCall(request)
        .enqueue(
            new Callback() {
              @Override
              public void onFailure(Call call, IOException e) {
                postError(callbacks, context.getString(R.string.themes_description_failed));
              }

              @Override
              public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) {
                  postError(callbacks, context.getString(R.string.themes_description_failed));
                  return;
                }
                try {
                  String content = response.body().string();
                  postSuccess(callbacks, content);
                } catch (IOException e) {
                  postError(callbacks, context.getString(R.string.themes_description_failed));
                }
              }
            });
  }

  public static void fetchThemes(Context context, Callbacks callbacks) {
    Request request = new Request.Builder().url(THEMES_URL).get().build();
    client
        .newCall(request)
        .enqueue(
            new Callback() {
              @Override
              public void onFailure(Call call, IOException e) {
                postError(
                    callbacks, context.getString(R.string.themes_error_network, e.getMessage()));
              }

              @Override
              public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                  postError(
                      callbacks, context.getString(R.string.themes_error_http, response.code()));
                  return;
                }
                try {
                  String json = response.body().string();
                  Type type = new TypeToken<List<ThemeItem>>() {}.getType();
                  List<ThemeItem> list = gson.fromJson(json, type);
                  postSuccess(callbacks, list == null ? new ArrayList<>() : list);
                } catch (JsonSyntaxException | IOException e) {
                  postError(
                      callbacks, context.getString(R.string.themes_error_parse, e.getMessage()));
                }
              }
            });
  }

  private static void postSuccess(Callbacks callbacks, List<ThemeItem> list) {
    new Handler(Looper.getMainLooper()).post(() -> callbacks.onSuccess(list));
  }

  private static void postError(Callbacks callbacks, String message) {
    new Handler(Looper.getMainLooper()).post(() -> callbacks.onError(message));
  }

  private static void postSuccess(TextCallbacks callbacks, String content) {
    new Handler(Looper.getMainLooper()).post(() -> callbacks.onSuccess(content));
  }

  private static void postError(TextCallbacks callbacks, String message) {
    new Handler(Looper.getMainLooper()).post(() -> callbacks.onError(message));
  }
}
