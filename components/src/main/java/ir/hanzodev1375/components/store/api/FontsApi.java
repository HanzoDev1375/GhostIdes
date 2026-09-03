package ir.hanzodev1375.components.store.api;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ir.hanzodev1375.components.R;
import ir.hanzodev1375.components.store.model.FontInfo;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class FontsApi {

  private static final String METADATA_URL = "https://fonts.google.com/metadata/fonts";
  private static final String CSS2_URL = "https://fonts.googleapis.com/css2?family=";
  private static final OkHttpClient client =
      new OkHttpClient.Builder()
          .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
          .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
          .build();

  private static final Pattern FONT_URL_PATTERN =
      Pattern.compile("https://fonts\\.gstatic\\.com/[^)\\s]+\\.(ttf|woff2|otf)");

  private static List<FontInfo> cachedFonts;

  public interface Callbacks {
    void onSuccess(List<FontInfo> fonts);

    void onError(String message);
  }

  public static void searchFonts(Context context, String query, Callbacks callback) {
    if (cachedFonts != null) {
      new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(filter(cachedFonts, query)));
      return;
    }

    Request request = new Request.Builder().url(METADATA_URL).get().build();
    client
        .newCall(request)
        .enqueue(
            new Callback() {
              @Override
              public void onFailure(Call call, IOException e) {
                postError(callback, context.getString(R.string.fonts_error_network, e.getMessage()));
              }

              @Override
              public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                  postError(callback, context.getString(R.string.fonts_error_http, response.code()));
                  return;
                }
                try {
                  String json = response.body().string();
                  List<FontInfo> list = parse(json);
                  cachedFonts = list;
                  postSuccess(callback, filter(list, query));
                } catch (Exception e) {
                  postError(callback, context.getString(R.string.fonts_error_parse, e.getMessage()));
                }
              }
            });
  }

  private static List<FontInfo> parse(String json) {
    int start = json.indexOf('{');
    if (start < 0) return new ArrayList<>();
    JsonObject root = JsonParser.parseString(json.substring(start)).getAsJsonObject();
    List<FontInfo> out = new ArrayList<>();
    if (root == null || !root.has("familyMetadataList")) return out;
    JsonArray items = root.getAsJsonArray("familyMetadataList");
    for (int i = 0; i < items.size(); i++) {
      JsonObject item = items.get(i).getAsJsonObject();
      FontInfo info = new FontInfo();
      info.family = item.has("family") ? item.get("family").getAsString() : null;
      info.category = item.has("category") ? item.get("category").getAsString() : "";
      if (info.family != null && !info.family.isEmpty()) {
        out.add(info);
      }
    }
    return out;
  }

  private static List<FontInfo> filter(List<FontInfo> source, String query) {
    if (source == null) return new ArrayList<>();
    if (query == null || query.trim().isEmpty()) {
      return source.size() > 100 ? new ArrayList<>(source.subList(0, 100)) : source;
    }
    String q = query.trim().toLowerCase(Locale.ROOT);
    List<FontInfo> out = new ArrayList<>();
    for (FontInfo f : source) {
      if (f.family != null && f.family.toLowerCase(Locale.ROOT).contains(q)) {
        out.add(f);
        if (out.size() >= 100) break;
      }
    }
    return out;
  }

  public static String normalizeHttps(String url) {
    if (url == null) return null;
    return url.startsWith("http://") ? "https://" + url.substring(7) : url;
  }

  /**
   * از CSS2 گوگل فونت، URL های مستقیم فایل فونت رو برمی‌داره. متادادات فونت‌های گوگل دیگه فیلد
   * «url» نداره، ولی css2 برای هر خانواده فایل‌های واقعی (.ttf/.woff2) رو به صورت مستقیم داخل
   * {@code url(...)} برمی‌گردونه — نه ZIP. به همین دلیل دانلود از این مسیر انجام می‌شه.
   */
  public static List<String> fontFileUrls(String family) {
    if (family == null || family.trim().isEmpty()) return new ArrayList<>();
    String f = family.trim().replace(' ', '+');
    String cssUrl = CSS2_URL + f + "&display=swap";
    Request request = new Request.Builder().url(cssUrl).get().build();
    try (Response response = client.newCall(request).execute()) {
      if (!response.isSuccessful()) return new ArrayList<>();
      String css = response.body().string();
      List<String> urls = new ArrayList<>();
      Set<String> seen = new HashSet<>();
      Matcher m = FONT_URL_PATTERN.matcher(css);
      while (m.find()) {
        String url = m.group();
        if (seen.add(url)) urls.add(url);
      }
      return urls;
    } catch (Exception e) {
      return new ArrayList<>();
    }
  }

  public static String fileExtensionOf(String url) {
    if (url == null) return ".ttf";
    int query = url.indexOf('?');
    if (query >= 0) {
      url = url.substring(0, query);
    }
    int dot = url.lastIndexOf('.');
    int slash = url.lastIndexOf('/');
    if (dot > slash && dot < url.length() - 1) {
      return "." + url.substring(dot + 1);
    }
    return ".ttf";
  }

  private static void postSuccess(Callbacks callback, List<FontInfo> list) {
    new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(list));
  }

  private static void postError(Callbacks callback, String message) {
    new Handler(Looper.getMainLooper()).post(() -> callback.onError(message));
  }
}
