package ir.hanzodev1375.ghostide.terminal;

import android.content.Context;
import android.os.Build;
import com.downloader.Error;
import com.downloader.OnDownloadListener;
import com.downloader.PRDownloader;
import ir.hanzodev1375.ghostide.R;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public final class DebianInstaller {

  private DebianInstaller() {}

  private static final String IMAGES_BASE =
      "https://images.linuxcontainers.org/images/debian/bookworm/";
  private static final Pattern SNAPSHOT_PATTERN = Pattern.compile("(\\d{8}_\\d{2}:\\d{2})/");
  private static final ExecutorService resolveExecutor = Executors.newSingleThreadExecutor();
  private static final OkHttpClient http = new OkHttpClient();

  public interface InstallListener {
    void onDownloadProgress(int percent);

    void onExtractProgress(int extractedEntries);

    void onSuccess();

    void onError(String message);
  }

  /** فاز فعلیِ نصب. static و مستقل از عمر Activity/Fragment. */
  public enum Phase {
    IDLE,
    DOWNLOADING,
    EXTRACTING,
    DONE,
    ERROR
  }

  private static volatile Phase phase = Phase.IDLE;
  private static volatile int progressValue = 0;
  private static volatile InstallListener uiListener;
  private static int activeDownloadId = -1;

  public static boolean isInstalling() {
    return phase == Phase.DOWNLOADING || phase == Phase.EXTRACTING;
  }

  public static Phase getPhase() {
    return phase;
  }

  /**
   * یه UI تازه (مثلاً Activity‌ای که بعد از بیرون‌رفتن و برگشتن دوباره ساخته شده) رو به نصبِ در
   * حالِ اجرا وصل میکنه و فوراً یه callback با آخرین progress شناخته‌شده میده — بدون این‌که
   * دانلود/استخراج از اول شروع بشه. اگه در حالِ حاضر نصبی در جریان نباشه، این فقط listener رو
   * برای آینده ثبت میکنه.
   */
  public static void attach(InstallListener listener) {
    uiListener = listener;
    if (listener == null) return;
    if (phase == Phase.DOWNLOADING) listener.onDownloadProgress(progressValue);
    else if (phase == Phase.EXTRACTING) listener.onExtractProgress(progressValue);
  }

  /** فقط اگه UI فعلی همین listener باشه قطعش کن (تا یه attach جدید رو اشتباهی پاک نکنه). */
  public static void detach(InstallListener listener) {
    if (uiListener == listener) uiListener = null;
  }

  public static boolean isInstalled(Context context) {
    return DebianBootstrap.isInstalled(context);
  }

  private static String detectArch() {
    for (String abi : Build.SUPPORTED_ABIS) {
      switch (abi) {
        case "arm64-v8a":
          return "arm64";
        case "armeabi-v7a":
          return "armhf";
        case "x86_64":
          return "amd64";
        case "x86":
          return "i386";
      }
    }
    return "arm64";
  }

  public static void installDebian(Context context, InstallListener listener) {
    phase = Phase.DOWNLOADING;
    progressValue = 0;
    uiListener = listener;

    String arch = detectArch();
    resolveExecutor.execute(
        () -> {
          try {
            String url = resolveLatestRootfsUrl(context, arch);
            startDownloadAndExtract(context, url);
          } catch (IOException e) {
            fail(context.getString(R.string.terminal_error_resolve_rootfs_failed, arch, e.getMessage()));
          }
        });
  }

  private static String resolveLatestRootfsUrl(Context context, String arch) throws IOException {
    String indexUrl = IMAGES_BASE + arch + "/default/";
    Request request = new Request.Builder().url(indexUrl).build();
    String latestSnapshot = null;
    try (Response response = http.newCall(request).execute()) {
      if (!response.isSuccessful() || response.body() == null) {
        throw new IOException(
            context.getString(R.string.terminal_error_http_status, response.code(), indexUrl));
      }
      String html = response.body().string();
      Matcher matcher = SNAPSHOT_PATTERN.matcher(html);
      while (matcher.find()) {
        String snapshot = matcher.group(1);
        if (latestSnapshot == null || snapshot.compareTo(latestSnapshot) > 0) {
          latestSnapshot = snapshot;
        }
      }
    }
    if (latestSnapshot == null) {
      throw new IOException(context.getString(R.string.terminal_error_no_snapshot_found, indexUrl));
    }
    return indexUrl + latestSnapshot.replace(":", "%3A") + "/rootfs.tar.xz";
  }

  /** اگه خواستی از یه لینک دیگه نصب کنی. UI رو با attach() وصل کن. */
  public static void installFrom(Context context, String url, InstallListener listener) {
    phase = Phase.DOWNLOADING;
    progressValue = 0;
    uiListener = listener;
    startDownloadAndExtract(context, url);
  }

  private static void startDownloadAndExtract(Context context, String url) {
    File downloadDir = context.getCacheDir();
    String fileName = "debian-rootfs.tar.xz";
    File downloadedFile = new File(downloadDir, fileName);

    activeDownloadId =
        PRDownloader.download(url, downloadDir.getAbsolutePath(), fileName)
            .build()
            .setOnProgressListener(
                progress -> {
                  if (progress.totalBytes <= 0) return;
                  int percent = (int) (progress.currentBytes * 100 / progress.totalBytes);
                  progressValue = percent;
                  if (uiListener != null) uiListener.onDownloadProgress(percent);
                })
            .start(
                new OnDownloadListener() {
                  @Override
                  public void onDownloadComplete() {
                    phase = Phase.EXTRACTING;
                    progressValue = 0;
                    DebianBootstrap.installFromTarXz(
                        context,
                        downloadedFile,
                        new DebianBootstrap.InstallCallback() {
                          @Override
                          public void onProgress(int extractedEntries) {
                            progressValue = extractedEntries;
                            if (uiListener != null) uiListener.onExtractProgress(extractedEntries);
                          }

                          @Override
                          public void onSuccess() {
                            downloadedFile.delete();
                            phase = Phase.DONE;
                            if (uiListener != null) uiListener.onSuccess();
                          }

                          @Override
                          public void onError(Exception e) {
                            fail(
                                context.getString(
                                    R.string.terminal_error_extract_failed_prefix, e.getMessage()));
                          }
                        });
                  }

                  @Override
                  public void onError(Error error) {
                    String detail =
                        error != null
                            ? error.getServerErrorMessage()
                            : context.getString(R.string.terminal_unknown_error);
                    fail(context.getString(R.string.terminal_error_download_failed_prefix, detail));
                  }
                });
  }

  private static void fail(String message) {
    phase = Phase.ERROR;
    if (uiListener != null) uiListener.onError(message);
  }

  public static void cancelInstall() {
    if (activeDownloadId != -1) {
      PRDownloader.cancel(activeDownloadId);
      activeDownloadId = -1;
    }
    phase = Phase.IDLE;
  }
}
