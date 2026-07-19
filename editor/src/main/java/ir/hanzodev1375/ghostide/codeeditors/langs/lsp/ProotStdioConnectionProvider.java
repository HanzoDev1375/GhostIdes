package ir.hanzodev1375.ghostide.codeeditors.langs.lsp;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.github.rosemoe.sora.lsp.client.connection.StreamConnectionProvider;
import ir.hanzodev1375.ghostide.codeeditors.langs.formatHelp.DebianBootstrap;

/**
 * یک StreamConnectionProvider عمومی و قابل استفاده ی مجدد برای هر Language Server (clangd، jdtls،
 * pylsp و ...) که باید داخل rootfs دبیانِ proot اجرا بشه.
 *
 * <p>تفاوت کلیدی اش با CppFormatter موجود در پروژه:
 *
 * <ul>
 *   <li>پروسه یک بار مصرف نیست؛ تا وقتی ادیتور وصله زنده می مونه (stdio پایدار).
 *   <li>stderr را قاطیِ stdout نمی کنیم (pb.redirectErrorStream(false))، چون پروتکل JSON-RPC روی
 *       stdout با هر بایت اضافه خراب می شه. stderr جدا و فقط برای لاگ خونده می شه.
 * </ul>
 *
 * این کلاس هیچ فرضی درباره ی زبان خاصی نداره؛ مسیر باینری و آرگومان ها از بیرون داده می شن.
 */
public class ProotStdioConnectionProvider implements StreamConnectionProvider {

  private static final String TAG = "ProotLSP";

  private final Context appContext;
  private final String workingDir; // مسیر ریشه ی پروژه روی خود دستگاه (هم bind میشه هم -w)
  private final String guestExecutable; // مسیر باینری داخل rootfs، مثلا "/usr/bin/clangd"
  private final List<String> args;

  private Process process;
  private Thread stderrPump;
  private volatile boolean closed = true;

  public ProotStdioConnectionProvider(
      Context context, String workingDir, String guestExecutable, List<String> args) {
    this.appContext = context.getApplicationContext();
    this.workingDir = workingDir;
    this.guestExecutable = guestExecutable;
    this.args = args != null ? args : new ArrayList<>();
  }

  @Override
  public void start() throws IOException {
    File rootfs = DebianBootstrap.getRootfsDir(appContext);
    if (!rootfs.exists() || !rootfs.isDirectory()) {
      throw new IOException("rootfs دبیان پیدا نشد: " + rootfs.getAbsolutePath());
    }

    String relativeGuestPath =
        guestExecutable.startsWith("/") ? guestExecutable.substring(1) : guestExecutable;
    File serverBinary = new File(rootfs, relativeGuestPath);
    if (!serverBinary.exists()) {
      throw new IOException(
          "باینری "
              + guestExecutable
              + " داخل rootfs پیدا نشد. "
              + "اول باید داخل proot دبیان نصبش کنی (مثلا: apt install clangd).");
    }

    String nativeLibDir = appContext.getApplicationInfo().nativeLibraryDir;
    File prootBinary = new File(nativeLibDir, "libproot.so");
    if (!prootBinary.exists()) {
      throw new IOException("libproot.so پیدا نشد: " + prootBinary.getAbsolutePath());
    }
    File loaderBinary = new File(nativeLibDir, "libloader.so");
    if (!loaderBinary.exists()) {
      throw new IOException("libloader.so پیدا نشد: " + loaderBinary.getAbsolutePath());
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
    command.add(appContext.getFilesDir().getParentFile().getAbsolutePath());
    command.add("-b");
    command.add(new File(workingDir).getAbsolutePath());

    command.add("-w");
    command.add(workingDir);
    command.add(guestExecutable);
    command.addAll(args);

    ProcessBuilder pb = new ProcessBuilder(command);
    pb.redirectErrorStream(false); // مهم: نباید stderr قاطیِ stdout بشه

    pb.environment().clear();
    pb.environment()
        .put("PROOT_TMP_DIR", appContext.getCacheDir().getAbsolutePath() + "/proot-tmp");
    pb.environment().put("PROOT_LOADER", loaderBinary.getAbsolutePath());
    pb.environment().put("LD_LIBRARY_PATH", nativeLibDir);
    pb.environment()
        .put("PATH", "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/system/bin");
    pb.environment().put("HOME", "/root");
    pb.environment().put("LANG", "C.UTF-8");
    pb.environment().put("LC_ALL", "C.UTF-8");
    process = pb.start();
    closed = false;

    stderrPump =
        new Thread(
            () -> {
              try (BufferedReader reader =
                  new BufferedReader(
                      new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                  Log.d(TAG, "[" + guestExecutable + "] " + line);
                }
              } catch (IOException ignored) {
                // پروسه بسته شده، طبیعیه
              }
            },
            "lsp-stderr-" + guestExecutable);
    stderrPump.setDaemon(true);
    stderrPump.start();
  }

  @Override
  public InputStream getInputStream() {
    return process.getInputStream();
  }

  @Override
  public OutputStream getOutputStream() {
    return process.getOutputStream();
  }

  @Override
  public boolean isClosed() {
    return closed || process == null || !process.isAlive();
  }

  @Override
  public void close() {
    closed = true;
    if (process != null) {
      process.destroy();
      try {
        if (!process.waitFor(2, TimeUnit.SECONDS)) {
          process.destroyForcibly();
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        process.destroyForcibly();
      }
    }
    if (stderrPump != null) {
      stderrPump.interrupt();
    }
  }
}
