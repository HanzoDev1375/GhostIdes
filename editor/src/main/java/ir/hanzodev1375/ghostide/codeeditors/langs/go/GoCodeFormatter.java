package ir.hanzodev1375.ghostide.codeeditors.langs.go;

import android.content.Context;
import android.util.Log;

import ir.hanzodev1375.ghostide.codeeditors.langs.formatHelp.DebianBootstrap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * فرمت‌کننده کد Go با استفاده از gofmt در محیط Debian مجازی (proot) نیاز به نصب gofmt در rootfs
 * دبیان دارد (معمولاً همراه با Go نصب می‌شود)
 */
public class GoCodeFormatter {

  private static final String TAG = "GoFormatter";
  private static final String GOFMT_PATH = "/usr/bin/gofmt"; // مسیر پیش‌فرض gofmt

  /**
   * فرمت کردن کد Go با سبک استاندارد (gofmt)
   *
   * @param context Context برنامه
   * @param code کد خام Go
   * @return کد فرمت‌شده، یا null در صورت خطای بحرانی، یا خود کد اصلی در صورت خطای gofmt
   */
  public String formatGo(Context context, String code) {
    if (code == null || code.isEmpty()) {
      return code;
    }

    // بررسی وجود دایرکتوری rootfs دبیان
    File rootfs = DebianBootstrap.getRootfsDir(context);
    if (!rootfs.exists() || !rootfs.isDirectory()) {
      Log.e(TAG, "rootfs دبیان پیدا نشد: " + rootfs.getAbsolutePath());
      return null;
    }

    // بررسی وجود باینری‌های proot و loader
    String nativeLibDir = context.getApplicationInfo().nativeLibraryDir;
    File prootBinary = new File(nativeLibDir, "libproot.so");
    if (!prootBinary.exists()) {
      Log.e(TAG, "libproot.so پیدا نشد: " + prootBinary.getAbsolutePath());
      return null;
    }

    File loaderBinary = new File(nativeLibDir, "libloader.so");
    if (!loaderBinary.exists()) {
      Log.e(TAG, "libloader.so پیدا نشد: " + loaderBinary.getAbsolutePath());
      return null;
    }

    // ساخت دستور proot برای اجرای gofmt
    List<String> command = new ArrayList<>();
    command.add(prootBinary.getAbsolutePath());
    command.add("--kill-on-exit");
    command.add("-0");
    command.add("--link2symlink");
    command.add("-r");
    command.add(rootfs.getAbsolutePath());
    command.add("-b");
    command.add("/dev");
    command.add("-b");
    command.add("/proc");
    command.add("-b");
    command.add("/sys");
    command.add("-b");
    command.add(context.getFilesDir().getParentFile().getAbsolutePath());
    command.add("-w");
    command.add("/root");
    command.add(GOFMT_PATH);

    ProcessBuilder pb = new ProcessBuilder(command);
    pb.redirectErrorStream(true);
    pb.environment().clear();
    pb.environment().put("PROOT_TMP_DIR", context.getCacheDir().getAbsolutePath() + "/proot-tmp");
    pb.environment().put("PROOT_LOADER", loaderBinary.getAbsolutePath());
    pb.environment().put("LD_LIBRARY_PATH", nativeLibDir);
    pb.environment()
        .put("PATH", "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/system/bin");

    try {
      Process process = pb.start();

      // نوشتن کد ورودی به stdin پروسه
      try (BufferedWriter writer =
          new BufferedWriter(
              new OutputStreamWriter(
                  process.getOutputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
        writer.write(code);
        writer.flush();
      }

      // خواندن خروجی فرمت‌شده
      StringBuilder output = new StringBuilder();
      try (BufferedReader reader =
          new BufferedReader(
              new InputStreamReader(
                  process.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
        String line;
        while ((line = reader.readLine()) != null) {
          if (output.length() > 0) output.append('\n');
          output.append(line);
        }
      }

      int exitCode = process.waitFor();
      if (exitCode == 0) {
        return output.toString();
      } else {
        Log.e(TAG, "gofmt با خطا مواجه شد: " + exitCode);
        // در صورت خطا (مثلاً سینتکس نامعتبر)، کد اصلی را برمی‌گردانیم
        return code;
      }

    } catch (Exception e) {
      Log.e(TAG, "خطا در اجرای gofmt", e);
      return null;
    }
  }
}
