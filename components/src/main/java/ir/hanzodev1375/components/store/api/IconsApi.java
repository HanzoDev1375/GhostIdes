package ir.hanzodev1375.components.store.api;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ir.hanzodev1375.components.R;
import ir.hanzodev1375.components.store.model.IconInfo;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class IconsApi {

  public static final String HOST = "https://fonts.gstatic.com";
  public static final String METADATA_URL = "https://fonts.google.com/metadata/icons";

  public static final int STYLE_FILLED = 0;
  public static final int STYLE_OUTLINED = 1;
  public static final int STYLE_ROUND = 2;
  public static final int STYLE_SHARP = 3;
  public static final int STYLE_TWO_TONE = 4;
  public static final int STYLE_COUNT = 5;

  private static final String[] FAMILY_SLUGS = {
    "materialicons",
    "materialiconsoutlined",
    "materialiconsround",
    "materialiconssharp",
    "materialiconstwotone"
  };

  private static final String[] FAMILY_NAMES = {
    "Material Icons",
    "Material Icons Outlined",
    "Material Icons Round",
    "Material Icons Sharp",
    "Material Icons Two Tone"
  };

  private static final OkHttpClient client =
      new OkHttpClient.Builder()
          .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
          .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
          .build();
  private static final Gson gson = new Gson();
  private static List<IconInfo> cachedIcons;

  public interface Callbacks {
    void onSuccess(List<IconInfo> icons);

    void onError(String message);
  }

  public static void searchIcons(Context context, String query, Callbacks callback) {
    if (cachedIcons != null) {
      postSuccess(callback, filter(cachedIcons, query));
      return;
    }
    Request request = new Request.Builder().url(METADATA_URL).get().build();
    client
        .newCall(request)
        .enqueue(
            new Callback() {
              @Override
              public void onFailure(Call call, IOException e) {
                postError(
                    callback, context.getString(R.string.icons_error_network, e.getMessage()));
              }

              @Override
              public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                  postError(
                      callback, context.getString(R.string.icons_error_http, response.code()));
                  return;
                }
                try {
                  String json = response.body().string();
                  List<IconInfo> list = parse(json);
                  cachedIcons = list;
                  postSuccess(callback, filter(list, query));
                } catch (Exception e) {
                  postError(
                      callback, context.getString(R.string.icons_error_parse, e.getMessage()));
                }
              }
            });
  }

  public static String familySlug(int style) {
    if (style < 0 || style >= STYLE_COUNT) {
      return FAMILY_SLUGS[STYLE_OUTLINED];
    }
    return FAMILY_SLUGS[style];
  }

  public static String familyName(int style) {
    if (style < 0 || style >= STYLE_COUNT) {
      return FAMILY_NAMES[STYLE_OUTLINED];
    }
    return FAMILY_NAMES[style];
  }

  public static boolean isSupported(IconInfo icon, int style) {
    return icon == null || icon.supports(familyName(style));
  }

  public static String svgUrl(IconInfo icon, int style) {
    if (icon == null || icon.name == null) return null;
    return HOST + "/s/i/" + familySlug(style) + "/" + icon.name + "/v" + icon.version + "/24px.svg";
  }

  private static List<IconInfo> parse(String json) {
    if (json == null) return new ArrayList<>();
    int start = json.indexOf('{');
    if (start < 0) return new ArrayList<>();
    JsonObject root = JsonParser.parseString(json.substring(start)).getAsJsonObject();
    List<IconInfo> out = new ArrayList<>();
    if (root == null || !root.has("icons")) return out;
    JsonArray icons = root.getAsJsonArray("icons");
    for (int i = 0; i < icons.size(); i++) {
      IconInfo info = gson.fromJson(icons.get(i), IconInfo.class);
      if (info != null && info.name != null && !info.name.isEmpty()) {
        out.add(info);
      }
    }
    return out;
  }

  private static List<IconInfo> filter(List<IconInfo> source, String query) {
    if (source == null) return new ArrayList<>();
    if (query == null || query.trim().isEmpty()) {
      return source.size() > 200 ? new ArrayList<>(source.subList(0, 200)) : source;
    }
    String q = query.trim().toLowerCase(Locale.ROOT);
    List<IconInfo> out = new ArrayList<>();
    for (IconInfo icon : source) {
      if (matches(icon, q)) {
        out.add(icon);
        if (out.size() >= 200) break;
      }
    }
    return out;
  }

  private static boolean matches(IconInfo icon, String q) {
    if (icon.name != null && icon.name.toLowerCase(Locale.ROOT).contains(q)) return true;
    if (icon.tags != null) {
      for (String tag : icon.tags) {
        if (tag != null && tag.toLowerCase(Locale.ROOT).contains(q)) return true;
      }
    }
    return false;
  }

  private static void postSuccess(Callbacks callback, List<IconInfo> list) {
    new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(list));
  }

  private static void postError(Callbacks callback, String message) {
    new Handler(Looper.getMainLooper()).post(() -> callback.onError(message));
  }
}
