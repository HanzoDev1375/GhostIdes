package ir.hanzodev1375.ghostide.terminal;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import ir.hanzodev1375.ghostide.R;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * هماهنگ‌کننده‌ی نصب Debian. خودِ دانلود tar.xz مسئولیت شما با PRDownloader هست؛ این کلاس فقط
 * دو کار میکنه: چک اینکه از قبل نصب شده یا نه، و استخراجِ فایلِ دانلودشده رو یه ترد پس‌زمینه.
 */
public final class DebianBootstrap {

  private DebianBootstrap() {}

  private static final String LOG_TAG = "GHOST_DEBIAN_BOOTSTRAP";
  private static final String ROOTFS_DIR_NAME = "rootfs/debian";
  private static final ExecutorService executor = Executors.newSingleThreadExecutor();
  private static final Handler mainHandler = new Handler(Looper.getMainLooper());

  public interface InstallCallback {
    /** روی main thread صدا زده میشه. */
    void onProgress(int extractedEntries);

    /** روی main thread صدا زده میشه. */
    void onSuccess();

    /** روی main thread صدا زده میشه. */
    void onError(Exception e);
  }

  public static File getRootfsDir(Context context) {
    return new File(context.getFilesDir(), ROOTFS_DIR_NAME);
  }

  /** چک میکنه rootfs از قبل کامل استخراج شده یا نه (وجودِ /bin/bash به‌عنوان نشونه‌ی نصب کامل). */
  public static boolean isInstalled(Context context) {
    return new File(getRootfsDir(context), "bin/bash").exists();
  }

  public static void uninstall(Context context, Runnable onDone) {
    File rootfs = getRootfsDir(context);
    executor.execute(
        () -> {
          deleteRecursive(rootfs);
          if (onDone != null) mainHandler.post(onDone);
        });
  }

  private static void deleteRecursive(File file) {
    File[] children = file.listFiles();
    if (children != null) {
      for (File child : children) deleteRecursive(child);
    }
    file.delete();
  }

  /**
   * فایل tar.xz رو رو یه ترد پس‌زمینه استخراج میکنه.
   *
   * <p>نکته: بعضی وقتا tar زیرِ proot با یه warning بی‌ضرر exit code غیرصفر برمیگردونه با اینکه
   * استخراج واقعاً کامل شده (/bin/bash روی دیسک هست). برای همین اگه extract exception بده ولی
   * isInstalled() true باشه، fail واقعی حسابش نمیکنیم — فقط لاگ میکنیم و موفقیت رو گزارش میدیم؛
   * وگرنه کاربر یه "fail" کاذب می‌بینه با اینکه ترمینال Debian واقعاً کار میکنه.
   */
  public static void installFromTarXz(
      Context context, File downloadedTarXz, InstallCallback callback) {
    File rootfs = getRootfsDir(context);
    executor.execute(
        () -> {
          try {
            RootfsExtractor.extract(
                context,
                downloadedTarXz,
                rootfs,
                count ->
                    mainHandler.post(
                        () -> {
                          if (callback != null) callback.onProgress(count);
                        }));
            runFirstBootSetup(context, rootfs);
            mainHandler.post(
                () -> {
                  if (callback != null) callback.onSuccess();
                });
          } catch (IOException e) {
            if (isInstalled(context)) {
              Log.w(
                  LOG_TAG,
                  "extract() ارور داد ولی bin/bash موجوده، به‌عنوان موفقیت ادامه میدیم: "
                      + e.getMessage(),
                  e);
              try {
                runFirstBootSetup(context, rootfs);
              } catch (IOException setupError) {
                Log.w(LOG_TAG, "firstBootSetup بعد از recovery هم fail شد", setupError);
              }
              mainHandler.post(
                  () -> {
                    if (callback != null) callback.onSuccess();
                  });
            } else {
              mainHandler.post(
                  () -> {
                    if (callback != null) callback.onError(e);
                  });
            }
          }
        });
  }

  private static void runFirstBootSetup(Context context, File rootfsDir) throws IOException {
    File etc = new File(rootfsDir, "etc");
    if (!etc.exists() && !etc.mkdirs()) {
      throw new IOException(
          context.getString(R.string.terminal_error_cannot_create_etc_dir, etc));
    }

    writeFile(new File(etc, "resolv.conf"), "nameserver 8.8.8.8\nnameserver 8.8.4.4\n");

    writeFile(
        new File(etc, "hosts"),
        "127.0.0.1   localhost\n"
            + "::1         localhost ip6-localhost ip6-loopback\n"
            + "ff02::1     ip6-allnodes\n"
            + "ff02::2     ip6-allrouters\n");

    appendGroupLinesIfMissing(
        new File(etc, "group"),
        new String[] {
          "inet:x:3003",
          "everybody:x:9997",
          "android_app:x:20455",
          "android_debug:x:50455",
          "android_external_storage:x:1077"
        });
  }

  private static void writeFile(File file, String content) throws IOException {
    try (java.io.FileOutputStream out = new java.io.FileOutputStream(file)) {
      out.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
  }

  private static void appendGroupLinesIfMissing(File groupFile, String[] lines)
      throws IOException {
    String existing = "";
    if (groupFile.exists()) {
      existing =
          new String(
              java.nio.file.Files.readAllBytes(groupFile.toPath()),
              java.nio.charset.StandardCharsets.UTF_8);
    }
    StringBuilder toAppend = new StringBuilder();
    for (String line : lines) {
      String gid = line.substring(line.lastIndexOf(':') + 1);
      boolean alreadyPresent = existing.contains(":" + gid + "\n") || existing.endsWith(":" + gid);
      if (!alreadyPresent) {
        toAppend.append(line).append('\n');
      }
    }
    if (toAppend.length() > 0) {
      try (java.io.FileOutputStream out = new java.io.FileOutputStream(groupFile, true)) {
        out.write(toAppend.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
      }
    }
  }
}
