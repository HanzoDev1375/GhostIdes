package ir.hanzodev1375.ghostide.codeeditors.langs.java;

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
 * # اگر wget نداره، اول نصبش کن: apt install -y wget wget
 * https://github.com/google/google-java-format/releases/download/v1.25.2/google-java-format-1.25.2-all-deps.jar
 */
public class JavaCodeFormatter {

  private static final String TAG = "JavaFormatter";
  private static final String GOOGLE_JAVA_FORMAT_JAR =
      "/root/google-java-format-1.25.2-all-deps.jar";

  public String formatJava(Context context, String code, String style) {
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
    command.add("/usr/bin/java");
    command.add("-jar");
    command.add(GOOGLE_JAVA_FORMAT_JAR);
    if ("aosp".equalsIgnoreCase(style)) {
      command.add("--aosp");
    } else {
      command.add("--google");
    }
    command.add("-");

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

      try (BufferedWriter writer =
          new BufferedWriter(
              new OutputStreamWriter(
                  process.getOutputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
        writer.write(code);
        writer.flush();
      }

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
        Log.e(TAG, "google-java-format خطا: " + exitCode);
        return code;
      }

    } catch (Exception e) {
      Log.e(TAG, "خطا در اجرا", e);
      return null;
    }
  }

  public String formatJava(Context context, String code) {
    return formatJava(context, code, "google");
  }
}
