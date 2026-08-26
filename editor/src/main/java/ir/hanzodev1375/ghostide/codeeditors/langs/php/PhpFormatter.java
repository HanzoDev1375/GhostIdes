package ir.hanzodev1375.ghostide.codeeditors.langs.php;

import android.content.Context;
import android.util.Log;

import ir.hanzodev1375.ghostide.codeeditors.langs.formatHelp.DebianBootstrap;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * apt install php-codesniffer -y which phpcbf # خروجی: /usr/bin/phpcbf echo "<?php function
 * test(){echo 'hi';}" | phpcbf --standard=PSR12 --stdin-path=test.php فراموش نشود کتابخانه را نصب
 * کنید
 */
public class PhpFormatter {

  private static final String TAG = "PhpFormatter";

  public String formatPhp(Context context, String code) {
    if (code == null || code.isEmpty()) {
      return code;
    }

    File rootfs = DebianBootstrap.getRootfsDir(context);
    if (!rootfs.exists() || !rootfs.isDirectory()) {
      Log.e(TAG, "rootfs دبیان پیدا نشد: " + rootfs.getAbsolutePath());
      return null;
    }

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

    // ۱. ایجاد فایل موقت در کش برنامه (دسترسی از داخل اندروید)
    File cacheDir = context.getCacheDir();
    File tempFile = new File(cacheDir, "temp_php_code.php");
    try (FileWriter writer = new FileWriter(tempFile)) {
      writer.write(code);
      writer.flush();
    } catch (Exception e) {
      Log.e(TAG, "خطا در نوشتن فایل موقت", e);
      return null;
    }

    // ۲. مسیر فایل در داخل proot (چون cacheDir را با -b bind کرده‌ایم)
    // مسیر واقعی: /data/data/ir.hanzodev1375.ghostide/cache/temp_php_code.php
    String dataDir = context.getFilesDir().getParentFile().getAbsolutePath();
    String tempFilePathInProot = dataDir + "/cache/temp_php_code.php";

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
    command.add(dataDir); // bind کردن پوشه دیتا
    command.add("-w");
    command.add("/root");
    command.add("/usr/bin/phpcbf");
    command.add("--standard=PSR12");
    command.add(tempFilePathInProot);

    ProcessBuilder pb = new ProcessBuilder(command);
    pb.redirectErrorStream(true);

    pb.environment().clear();
    pb.environment().put("PROOT_TMP_DIR", context.getCacheDir().getAbsolutePath() + "/proot-tmp");
    pb.environment().put("PROOT_LOADER", loaderBinary.getAbsolutePath());
    pb.environment().put("LD_LIBRARY_PATH", nativeLibDir);
    pb.environment()
        .put("PATH", "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/system/bin");

    try {
      Log.d(TAG, "دستور: " + String.join(" ", command));

      Process process = pb.start();

      // خواندن خروجی (برای دیباگ)
      StringBuilder output = new StringBuilder();
      try (BufferedReader reader =
          new BufferedReader(
              new InputStreamReader(
                  process.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
        String line;
        while ((line = reader.readLine()) != null) {
          output.append(line).append('\n');
        }
      }

      int exitCode = process.waitFor();
      Log.d(TAG, "Exit code: " + exitCode);
      Log.d(TAG, "خروجی phpcbf: " + output.toString());

      if (exitCode == 0 || exitCode == 1) { // 1 هم معمولاً به معنی اصلاح فایل است
        // خواندن فایل فرمت‌شده
        StringBuilder formattedCode = new StringBuilder();
        try (BufferedReader fileReader =
            new BufferedReader(
                new InputStreamReader(
                    new FileInputStream(tempFile), java.nio.charset.StandardCharsets.UTF_8))) {
          String line;
          while ((line = fileReader.readLine()) != null) {
            if (formattedCode.length() > 0) formattedCode.append('\n');
            formattedCode.append(line);
          }
        }
        return formattedCode.toString();
      } else {
        Log.e(TAG, "phpcbf با خطا مواجه شد. Exit code: " + exitCode);
        Log.e(TAG, "خروجی خطا: " + output);
        return code;
      }

    } catch (Exception e) {
      Log.e(TAG, "خطا در اجرای proot/phpcbf", e);
      return null;
    } finally {
      if (tempFile.exists()) {
        tempFile.delete();
      }
    }
  }
}
