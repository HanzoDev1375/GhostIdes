package ir.hanzodev1375.ghostide.codeeditors.dependencychecker;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Looks up the latest published version of a Maven artifact from Maven Central, Google Maven and
 * the JetBrains (Kotlin/Compose) repository.
 *
 * <p>Requests run on a background executor so the caller never blocks the UI thread. Three sources
 * are queried in order: Maven Central first (search API), then Google Maven (metadata.xml) for
 * artifacts that are only hosted in the Google repository, and finally JetBrains for Kotlin/Compose
 * artifacts.
 */
public final class MavenVersionChecker {

  public interface Callback {
    /** @param newest the newest found version, or null when the lookup failed/found nothing. */
    void onResult(String newest);
  }

  private static final int TIMEOUT_MS = 8000;
  private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2);
  private static final OkHttpClient CLIENT =
      new OkHttpClient.Builder().connectTimeout(TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS)
          .readTimeout(TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS)
          .build();

  private MavenVersionChecker() {}

  /** Looks up the latest version of {@code group:name} and reports it on the {@code callback}. */
  public static void check(String group, String name, Callback callback) {
    EXECUTOR.execute(
        () -> {
          String found = queryMavenCentral(group, name);
          if (found == null) {
            found = queryGoogleMaven(group, name);
          }
          if (found == null) {
            found = queryJetBrains(group, name);
          }
          final String result = found;
          if (callback != null) callback.onResult(result);
        });
  }

  // ── Maven Central search API ─────────────────────────────────────────────

  private static String queryMavenCentral(String group, String name) {
    String url =
        "https://search.maven.org/solrsearch/select?q=g:%22" + urlEncode(group)
            + "%22+AND+a:%22" + urlEncode(name) + "%22&rows=1&wt=json";
    String body = get(url);
    if (body == null) return null;
    try {
      JsonObject root = JsonParser.parseString(body).getAsJsonObject();
      JsonObject response = root.getAsJsonObject("response");
      if (response == null) return null;
      JsonArray docs = response.getAsJsonArray("docs");
      if (docs == null || docs.size() == 0) return null;
      JsonObject doc = docs.get(0).getAsJsonObject();
      JsonElement latest = doc.get("latestVersion");
      return latest == null || latest.isJsonNull() ? null : latest.getAsString();
    } catch (Exception e) {
      return null;
    }
  }

  // ── Google Maven metadata.xml ────────────────────────────────────────────

  private static final Pattern LATEST_TAG = Pattern.compile("<latest>(.*?)</latest>");

  private static String queryGoogleMaven(String group, String name) {
    String path = group.replace('.', '/') + "/" + name;
    String url = "https://dl.google.com/dl/android/maven2/" + path + "/maven-metadata.xml";
    String body = get(url);
    if (body == null) return null;
    try {
      Matcher m = LATEST_TAG.matcher(body);
      if (m.find()) {
        String latest = m.group(1).trim();
        if (!latest.isEmpty()) return latest;
      }
    } catch (Exception ignored) {
    }
    return null;
  }

  // ── JetBrains (Kotlin/Compose) metadata.xml ──────────────────────────────

  private static String queryJetBrains(String group, String name) {
    String path = group.replace('.', '/') + "/" + name;
    String url =
        "https://maven.pkg.jetbrains.space/public/p/compose/dev/" + path + "/maven-metadata.xml";
    String body = get(url);
    if (body == null) return null;
    try {
      Matcher m = LATEST_TAG.matcher(body);
      if (m.find()) {
        String latest = m.group(1).trim();
        if (!latest.isEmpty()) return latest;
      }
    } catch (Exception ignored) {
    }
    return null;
  }

  // ── Shared HTTP helper ───────────────────────────────────────────────────

  private static String get(String url) {
    Request request = new Request.Builder().url(url).build();
    try (Response response = CLIENT.newCall(request).execute()) {
      if (!response.isSuccessful() || response.body() == null) return null;
      return response.body().string();
    } catch (IOException e) {
      return null;
    }
  }

  private static String urlEncode(String s) {
    try {
      return URLEncoder.encode(s, "UTF-8");
    } catch (Exception e) {
      return s;
    }
  }
}
