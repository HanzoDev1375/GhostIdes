package ir.hanzodev1375.ghostide.terminal;

import android.content.Context;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;

/**
 * یه rootfs (مثل Debian از linuxcontainers.org) که فرمتش tar.xz هست رو استخراج میکنه. باید رو یه
 * ترد جدا (نه UI thread) صدا زده بشه چون کاملاً بلاکینگ و طولانیه.
 *
 * <p><b>⚠️ چرا این کلاس عوض شد:</b> نسخه‌ی قبلی خودش با commons-compress فایل‌به‌فایلِ rootfs رو
 * می‌ساخت (mkdir/FileOutputStream/setExecutable...) و پرمیژن‌ها و هاردلینک‌های آرشیو رو کامل و
 * درست حفظ نمی‌کرد. نتیجه‌ش این بود که اولین apt/dpkg با ارور
 *
 * <pre>dpkg: error: error creating new backup file '/var/lib/dpkg/status-old': Permission denied</pre>
 *
 * روبرو می‌شد. چون فایل‌ها بیرون از لایه‌ی فیک‌روتِ proot، مستقیم با UID واقعیِ اپ اندروید ساخته
 * می‌شدن، نه با tar واقعی زیرِ proot -0 --link2symlink. Termux/proot-distro و Xed-Editor هردو
 * دقیقاً همینو انجام می‌دن: خودِ استخراج rootfs رو هم زیرِ proot، با tar واقعیِ اندروید (toybox،
 * /system/bin/tar) انجام می‌دن.
 *
 * <p>برای همین این نسخه فقط لایه‌ی xz رو تو جاوا باز می‌کنه (این کار صرفاً یه کپیِ بایت‌به‌بایتِ
 * ساده‌ست و به پرمیژن‌ها کاری نداره، پس امنه)، و خودِ extract را می‌سپاره به {@link ProotExec}
 * که tar واقعی رو زیرِ proot صدا می‌زنه.
 *
 * <p>نیاز به این dependency تو build.gradle ماژول app (از قبل هست، دست نخورده):
 *
 * <pre>{@code
 * implementation 'org.apache.commons:commons-compress:1.26.0'
 * implementation 'org.tukaani:xz:1.9'
 * }</pre>
 */
public final class RootfsExtractor {

  private static final File SYSTEM_TAR = new File("/system/bin/tar");

  private RootfsExtractor() {}

  public interface ProgressListener {
    /** روی همون ترد استخراج صدا زده میشه؛ اگه میخوای UI رو آپدیت کنی خودت post کن به main thread. */
    void onProgress(int extractedEntries);
  }

  public static void extract(
      Context context, File tarXzFile, File destDir, ProgressListener listener) throws IOException {
    if (!destDir.exists() && !destDir.mkdirs()) {
      throw new IOException("نمیشه پوشه‌ی مقصد رو ساخت: " + destDir);
    }

    if (!SYSTEM_TAR.exists()) {
      // اکثر دستگاه‌های اندرویدی (از 6.0 به بعد، از طریق toybox) این باینری رو دارن؛ اگه یه
      // دستگاه خاص نداشتش، به‌جای خراب‌کردنِ خاموش rootfs، همینجا با یه پیام روشن fail کن.
      throw new IOException(
          "روی این دستگاه /system/bin/tar پیدا نشد؛ بدون یه tar واقعی نمیشه rootfs رو با پرمیژن‌های"
              + " درست استخراج کرد. راه‌حل جایگزین: یه busybox/tar استاتیک با اپ باندل کن و مسیرش رو"
              + " اینجا جایگزین کن.");
    }

    File plainTar = new File(destDir.getParentFile(), destDir.getName() + ".extract.tar");
    try {
      decompressXz(tarXzFile, plainTar);

      String command =
          "tar --exclude='dev/*' -xf '"
              + plainTar.getAbsolutePath()
              + "' -C '"
              + destDir.getAbsolutePath()
              + "' -v";

      int[] count = {0};
      ProotExec.Result result;
      try {
        result =
            ProotExec.run(
                context,
                new File("/"),
                Collections.emptyList(),
                command,
                line -> {
                  count[0]++;
                  if (listener != null && count[0] % 200 == 0) listener.onProgress(count[0]);
                });
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IOException("استخراج زیر proot قطع شد", e);
      }

      if (!result.isSuccess()) {
        throw new IOException(
            "استخراج زیرِ proot شکست خورد (exit="
                + result.exitCode
                + ").\nخروجیِ آخر:\n"
                + result.tailOutput);
      }

      if (listener != null) listener.onProgress(count[0]);
    } finally {
      plainTar.delete();
    }
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
