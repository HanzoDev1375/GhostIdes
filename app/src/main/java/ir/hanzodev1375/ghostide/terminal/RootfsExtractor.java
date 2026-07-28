package ir.hanzodev1375.ghostide.terminal;

import android.content.Context;
import ir.hanzodev1375.ghostide.R;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;

/**
 * یه rootfs (مثل Debian از linuxcontainers.org) که فرمتش tar.xz هست رو استخراج میکنه. باید رو یه
 * ترد جدا (نه UI thread) صدا زده بشه چون کاملاً بلاکینگ و طولانیه.
 *
 * <p>لایه‌ی xz فقط تو جاوا باز میشه (بایت‌به‌بایت، به پرمیژن‌ها کاری نداره)؛ خودِ extract با
 * tar واقعیِ اندروید (toybox) زیرِ proot -0 --link2symlink انجام میشه تا پرمیژن‌ها/هاردلینک‌ها
 * درست حفظ بشن (دقیقاً مثل Termux/proot-distro و Xed-Editor).
 *
 * <p>progress دیگه از رویِ خطوطِ verbose تار (-v) نمی‌خونیم — چون بافرِ پایپ محدوده و جاوا
 * سرعتِ خودِ tar رو گیر می‌نداخت — به‌جاش با یه ترد جدا هر ۴۰۰ms تعداد فایل‌های زیرِ destDir رو
 * می‌شماریم؛ هم سریع‌تره هم tail خروجی برای دیباگ تمیز می‌مونه.
 */
public final class RootfsExtractor {

  private static final File SYSTEM_TAR = new File("/system/bin/tar");
  private static final long PROGRESS_POLL_INTERVAL_MS = 400;

  private RootfsExtractor() {}

  public interface ProgressListener {
    /** روی همون ترد استخراج صدا زده میشه؛ اگه میخوای UI رو آپدیت کنی خودت post کن به main thread. */
    void onProgress(int extractedEntries);
  }

  public static void extract(
      Context context, File tarXzFile, File destDir, ProgressListener listener) throws IOException {
    if (!destDir.exists() && !destDir.mkdirs()) {
      throw new IOException(
          context.getString(R.string.terminal_error_cannot_create_dest_dir, destDir));
    }

    if (!SYSTEM_TAR.exists()) {
      throw new IOException(context.getString(R.string.terminal_error_no_system_tar));
    }

    File plainTar = new File(destDir.getParentFile(), destDir.getName() + ".extract.tar");
    AtomicBoolean extracting = new AtomicBoolean(true);
    Thread progressThread = startProgressPolling(destDir, extracting, listener);

    try {
      decompressXz(tarXzFile, plainTar);

      String command =
          "tar --exclude='dev/*' -xf '"
              + plainTar.getAbsolutePath()
              + "' -C '"
              + destDir.getAbsolutePath()
              + "'";

      ProotExec.Result result;
      try {
        result = ProotExec.run(context, new File("/"), Collections.emptyList(), command);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IOException(context.getString(R.string.terminal_error_extract_interrupted), e);
      }

      if (!result.isSuccess()) {
        throw new IOException(
            context.getString(
                R.string.terminal_error_extract_failed_exit, result.exitCode, result.tailOutput));
      }

      if (listener != null) listener.onProgress(countEntries(destDir));
    } finally {
      extracting.set(false);
      progressThread.interrupt();
      plainTar.delete();
    }
  }

  private static Thread startProgressPolling(
      File destDir, AtomicBoolean extracting, ProgressListener listener) {
    Thread t =
        new Thread(
            () -> {
              while (extracting.get()) {
                try {
                  Thread.sleep(PROGRESS_POLL_INTERVAL_MS);
                } catch (InterruptedException e) {
                  return;
                }
                if (listener != null) listener.onProgress(countEntries(destDir));
              }
            },
            "rootfs-extract-progress");
    t.setDaemon(true);
    t.start();
    return t;
  }

  private static int countEntries(File dir) {
    File[] children = dir.listFiles();
    if (children == null) return 0;
    int count = 0;
    for (File child : children) {
      count++;
      if (child.isDirectory()) count += countEntries(child);
    }
    return count;
  }

  private static void decompressXz(File xzFile, File outTar) throws IOException {
    try (InputStream fileIn = new FileInputStream(xzFile);
        InputStream buffered = new BufferedInputStream(fileIn);
        XZCompressorInputStream xzIn = new XZCompressorInputStream(buffered);
        OutputStream out = new FileOutputStream(outTar)) {
      byte[] buffer = new byte[1 << 16];
      int len;
      while ((len = xzIn.read(buffer)) != -1) {
        out.write(buffer, 0, len);
      }
    }
  }
}
