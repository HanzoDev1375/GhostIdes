package ir.hanzodev1375.ghostide.codeeditors.langs.cpp;

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

public class CppFormatter {

  private static final String TAG = "CppFormatter";
  /**
   * فرمت کردن کد C++ با استفاده از clang-format داخل محیط proot Debian
   *
   * @param context Context برنامه
   * @param code کد خام C++
   * @param style استایل (Google, LLVM, Mozilla, ...)
   * @return کد فرمت شده به صورت String، یا null در صورت خطا
   */
  public String formatCpp(Context context, String code, String style) {
    if (code == null || code.isEmpty()) {
      return code;
    }

    // 1. مسیر rootfs دبیان
    File rootfs = DebianBootstrap.getRootfsDir(context);
    if (!rootfs.exists() || !rootfs.isDirectory()) {
      Log.e(TAG, "rootfs دبیان پیدا نشد: " + rootfs.getAbsolutePath());
      return null;
    }

    // 2. مسیر فایل های native (libproot.so و libloader.so)
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

    // 3. ساخت دستور proot
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
    command.add("/usr/bin/clang-format");
    command.add("--style=" + style);
    command.add("-"); // خواندن از stdin

    ProcessBuilder pb = new ProcessBuilder(command);
    pb.redirectErrorStream(true);

    // Environment variables مورد نیاز proot
    pb.environment().clear();
    pb.environment().put("PROOT_TMP_DIR", context.getCacheDir().getAbsolutePath() + "/proot-tmp");
    pb.environment().put("PROOT_LOADER", loaderBinary.getAbsolutePath());
    pb.environment().put("LD_LIBRARY_PATH", nativeLibDir);
    pb.environment()
        .put("PATH", "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/system/bin");

    try {
      Process process = pb.start();

      // 4. نوشتن کد به stdin پروسه
      try (BufferedWriter writer =
          new BufferedWriter(
              new OutputStreamWriter(
                  process.getOutputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
        writer.write(code);
        writer.flush();
      } // بسته شدن خودکار writer باعث ارسال EOF به clang-format میشه

      // 5. خواندن خروجی فرمت شده از stdout
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
        Log.e(TAG, "clang-format با خطا مواجه شد. Exit code: " + exitCode);
        Log.e(TAG, "خروجی خطا: " + output);
        return code; // در صورت خطا، کد اصلی رو برگردون
      }

    } catch (Exception e) {
      Log.e(TAG, "خطا در اجرای proot/clang-format", e);
      return null;
    }
  }
}
