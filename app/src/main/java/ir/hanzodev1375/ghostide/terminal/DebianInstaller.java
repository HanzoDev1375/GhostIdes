package ir.hanzodev1375.ghostide.terminal;

import android.content.Context;
import android.os.Build;
import com.downloader.Error;
import com.downloader.OnDownloadListener;
import com.downloader.PRDownloader;
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
  // پوشه‌های snapshot اسمشون timestamp هست، مثلاً "20260712_07:36/" — هاردکد کردنِ یه تاریخ ثابت
  // اشتباهه چون این پوشه‌ها مرتب پاک/عوض میشن؛ همیشه باید جدیدترینش رو از index بگیریم.
  private static final Pattern SNAPSHOT_PATTERN = Pattern.compile("(\\d{8}_\\d{2}:\\d{2})/");
  private static final ExecutorService resolveExecutor = Executors.newSingleThreadExecutor();
  private static final OkHttpClient http = new OkHttpClient();

  public interface InstallListener {
    void onDownloadProgress(int percent);

    void onExtractProgress(int extractedEntries);

    void onSuccess();

    void onError(String message);
  }

  private static int activeDownloadId = -1;

  public static boolean isInstalled(Context context) {
    return DebianBootstrap.isInstalled(context);
  }

  /**
   * معماریِ linuxcontainers.org رو از اولین ABI پشتیبانی‌شده‌ی خودِ گوشی حدس میزنه — همون معماری‌ای
   * که برای انتخاب libproot.so از jniLibs هم استفاده میشه. اگه این با معماریِ rootfs یکی نباشه
   * (مثلاً proot آرم۶۴ ولی rootfs آرم۳۲/armhf)، اجرای /bin/bash داخل rootfs روی خیلی از گوشی‌های
   * جدید اصلاً fail میشه (کرنل دیگه سازگاریِ ۳۲بیتی نداره) و سشن بی‌صدا فوراً می‌میره.
   */
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
    return "arm64"; // فال‌بک منطقی چون اکثر گوشی‌های امروزی arm64 هستن
  }

  /** معماریِ گوشی رو خودکار تشخیص میده و جدیدترین snapshot موجودش رو دانلود میکنه. */
  public static void installDebian(Context context, InstallListener listener) {
    String arch = detectArch();
    resolveExecutor.execute(
        () -> {
          try {
            String url = resolveLatestRootfsUrl(arch);
            installFrom(context, url, listener);
          } catch (IOException e) {
            if (listener != null) {
              listener.onError(
                  "پیدا کردن آخرین rootfs برای معماریِ " + arch + " fail شد: " + e.getMessage());
            }
          }
        });
  }

  /** صفحه‌ی index رو می‌گیره و بین پوشه‌های snapshot، جدیدترین (بزرگ‌ترین timestamp) رو برمیگردونه. */
  private static String resolveLatestRootfsUrl(String arch) throws IOException {
    String indexUrl = IMAGES_BASE + arch + "/default/";
    Request request = new Request.Builder().url(indexUrl).build();
    String latestSnapshot = null;
    try (Response response = http.newCall(request).execute()) {
      if (!response.isSuccessful() || response.body() == null) {
        throw new IOException("HTTP " + response.code() + " از " + indexUrl);
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
      throw new IOException("هیچ snapshot‌ای تو " + indexUrl + " پیدا نشد");
    }
    return indexUrl + latestSnapshot.replace(":", "%3A") + "/rootfs.tar.xz";
  }

  /** اگه خواستی از یه لینک دیگه (مثلاً کپیِ خودت رو گیت‌هاب) نصب کنی. */
  public static void installFrom(Context context, String url, InstallListener listener) {
    File downloadDir = context.getCacheDir();
    String fileName = "debian-rootfs.tar.xz";
    File downloadedFile = new File(downloadDir, fileName);

    activeDownloadId =
        PRDownloader.download(url, downloadDir.getAbsolutePath(), fileName)
            .build()
            .setOnProgressListener(
                progress -> {
                  if (listener == null || progress.totalBytes <= 0) return;
                  int percent = (int) (progress.currentBytes * 100 / progress.totalBytes);
                  listener.onDownloadProgress(percent);
                })
            .start(
                new OnDownloadListener() {
                  @Override
                  public void onDownloadComplete() {
                    DebianBootstrap.installFromTarXz(
                        context,
                        downloadedFile,
                        new DebianBootstrap.InstallCallback() {
                          @Override
                          public void onProgress(int extractedEntries) {
                            if (listener != null) listener.onExtractProgress(extractedEntries);
                          }

                          @Override
                          public void onSuccess() {
                            downloadedFile.delete(); // خودِ tar.xz خام دیگه لازم نیست
                            if (listener != null) listener.onSuccess();
                          }

                          @Override
                          public void onError(Exception e) {
                            if (listener != null) {
                              listener.onError("استخراج fail شد: " + e.getMessage());
                            }
                          }
                        });
                  }

                  @Override
                  public void onError(Error error) {
                    if (listener != null) {
                      listener.onError(
                          "دانلود fail شد: "
                              + (error != null ? error.getServerErrorMessage() : "نامشخص"));
                    }
                  }
                });
  }

  public static void cancelInstall() {
    if (activeDownloadId != -1) {
      PRDownloader.cancel(activeDownloadId);
      activeDownloadId = -1;
    }
  }
}
