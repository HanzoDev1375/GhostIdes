package ir.hanzodev1375.ghostide.terminal;

import android.content.Context;
import android.util.Log;
import ir.hanzodev1375.ghostide.R;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * اجرای یه دستورِ یه‌بارمصرف (غیرتعاملی) زیر proot، بدون نیاز به pty/TerminalSession.
 *
 * <p>چرا این کلاس لازمه: قبلاً RootfsExtractor خودش با commons-compress فایل‌به‌فایل rootfs رو
 * می‌ساخت (mkdir/FileOutputStream/setExecutable...) و پرمیژن‌ها و هاردلینک‌های آرشیو رو کامل
 * حفظ نمی‌کرد. نتیجه‌ش این بود که اولین apt/dpkg با
 *
 * <pre>dpkg: error: error creating new backup file '/var/lib/dpkg/status-old': Permission denied</pre>
 *
 * روبرو می‌شد — چون فایل‌ها بیرون از لایه‌ی فیک‌روتِ proot، با UID واقعیِ خودِ اپ اندروید ساخته
 * شده بودن، نه با یه tar واقعی زیرِ proot -0 --link2symlink. Termux/proot-distro و Xed-Editor
 * هردو دقیقاً همین کارو می‌کنن: خودِ استخراج رو هم زیرِ proot، با tar واقعیِ اندروید
 * (toybox، مسیر /system/bin/tar) انجام می‌دن، نه با یه پیاده‌سازیِ دستیِ tar. این کلاس همون
 * الگو رو با ProcessBuilder + libproot.so پیاده می‌کنه.
 */
public final class ProotExec {

  private static final String LOG_TAG = "GHOST_PROOT_EXEC";
  private static final String PROOT_LIBRARY_NAME = "libproot.so";
  private static final String LOADER_LIBRARY_NAME = "libloader.so";
  private static final String LOADER32_LIBRARY_NAME = "libloader32.so";
  private static final int TAIL_LINES = 80;

  private ProotExec() {}

  /** برای گزارشِ خط‌به‌خطِ خروجی (مثلاً برای شمردنِ فایل‌های استخراج‌شده با tar -v). */
  public interface LineListener {
    void onLine(String line);
  }

  public static final class Result {
    public final int exitCode;
    public final String tailOutput;

    Result(int exitCode, String tailOutput) {
      this.exitCode = exitCode;
      this.tailOutput = tailOutput;
    }

    public boolean isSuccess() {
      return exitCode == 0;
    }
  }

  public static Result run(Context context, File guestRoot, List<String> extraBinds, String command)
      throws IOException, InterruptedException {
    return run(context, guestRoot, extraBinds, command, null);
  }

  /**
   * @param guestRoot مسیری که به‌عنوان "-r" (guest rootfs) به proot داده میشه. برای استخراجِ
   *     اولیه‌ی rootfs از "/" (خودِ اندروید) استفاده کن، چون تا وقتی rootfs دبیان خالیه،
   *     /system/bin/sh و /system/bin/tar باید در دسترس باشن. برای دستورات داخلِ یه rootfs
   *     آماده، مسیر خودِ اون rootfs رو بده.
   * @param command دستوری که با «/system/bin/sh -c» اجرا میشه.
   * @param lineListener اختیاری؛ برای گزارشِ پیشرفت خط‌به‌خط.
   */
  public static Result run(
      Context context,
      File guestRoot,
      List<String> extraBinds,
      String command,
      LineListener lineListener)
      throws IOException, InterruptedException {

    String nativeLibDir = context.getApplicationInfo().nativeLibraryDir;

    File prootBinary = new File(nativeLibDir, PROOT_LIBRARY_NAME);
    if (!prootBinary.exists()) {
      throw new IOException(
          context.getString(R.string.terminal_error_libproot_not_found, prootBinary.getAbsolutePath()));
    }
    File loaderBinary = new File(nativeLibDir, LOADER_LIBRARY_NAME);
    if (!loaderBinary.exists()) {
      throw new IOException(
          context.getString(R.string.terminal_error_libloader_not_found, loaderBinary.getAbsolutePath()));
    }

    File tmpDir = new File(context.getCacheDir(), "proot-exec-tmp/" + System.nanoTime());
    tmpDir.mkdirs();

    List<String> cmd = new ArrayList<>();
    cmd.add(prootBinary.getAbsolutePath());
    cmd.add("--kill-on-exit");
    cmd.add("-0");
    cmd.add("--link2symlink");
    cmd.add("-r");
    cmd.add(guestRoot.getAbsolutePath());
    cmd.add("-b");
    cmd.add("/dev");
    cmd.add("-b");
    cmd.add("/proc");
    cmd.add("-b");
    cmd.add("/sys");
    // پوشه‌ی private اپ رو صریحاً بایند کن (دقیقاً مثل $PRIVATE_DIR تو Xed-Editor)، حتی وقتی
    // guestRoot خودِ "/" هست؛ روی بعضی دستگاه‌ها بدون این بایندِ صریح از داخل proot دیده نمیشه.
    cmd.add("-b");
    cmd.add(context.getFilesDir().getParentFile().getAbsolutePath());
    if (extraBinds != null) {
      for (String bind : extraBinds) {
        cmd.add("-b");
        cmd.add(bind);
      }
    }
    cmd.add("-w");
    cmd.add("/");
    cmd.add("/system/bin/sh");
    cmd.add("-c");
    cmd.add(command);

    ProcessBuilder pb = new ProcessBuilder(cmd);
    pb.redirectErrorStream(true);
    pb.environment().clear();
    pb.environment().put("PROOT_TMP_DIR", tmpDir.getAbsolutePath());
    pb.environment().put("PROOT_LOADER", loaderBinary.getAbsolutePath());
    pb.environment().put("LD_LIBRARY_PATH", nativeLibDir);
    File loader32 = new File(nativeLibDir, LOADER32_LIBRARY_NAME);
    if (loader32.exists()) {
      pb.environment().put("PROOT_LOADER_32", loader32.getAbsolutePath());
    }
    pb.environment()
        .put("PATH", "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/system/bin");

    Log.i(LOG_TAG, "exec: " + cmd);

    Process process = pb.start();
    ArrayDeque<String> tail = new ArrayDeque<>();
    try (BufferedReader reader =
        new BufferedReader(
            new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (lineListener != null) lineListener.onLine(line);
        tail.addLast(line);
        if (tail.size() > TAIL_LINES) tail.removeFirst();
      }
    }
    int exitCode = process.waitFor();
    deleteRecursive(tmpDir);

    String tailOutput = String.join("\n", tail);
    Log.i(LOG_TAG, "exit=" + exitCode);
    return new Result(exitCode, tailOutput);
  }

  private static void deleteRecursive(File file) {
    File[] children = file.listFiles();
    if (children != null) {
      for (File child : children) deleteRecursive(child);
    }
    file.delete();
  }
}
