package ir.hanzodev1375.ghostide.codeeditors.langs.formatHelp;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * هماهنگ‌کننده‌ی نصب Debian. خودِ دانلود tar.xz مسئولیت شما با PRDownloader هست؛ این کلاس فقط دو
 * کار میکنه: چک اینکه از قبل نصب شده یا نه، و استخراجِ فایلِ دانلودشده رو یه ترد پس‌زمینه.
 */
public final class DebianBootstrap {

  private DebianBootstrap() {}

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

  /**
   * کل پوشه‌ی rootfs رو پاک میکنه (مثلاً وقتی یه بار با معماریِ اشتباه دانلود/استخراج شده و باید از
   * اول تمیز نصب بشه). روی ترد پس‌زمینه اجرا میشه چون ممکنه چند صد مگابایت/هزاران فایل باشه.
   */
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
}
