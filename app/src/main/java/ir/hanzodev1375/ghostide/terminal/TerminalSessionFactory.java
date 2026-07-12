package ir.hanzodev1375.ghostide.terminal;

import android.content.Context;
import android.os.Environment;
import com.termux.terminal.TerminalSession;
import com.termux.terminal.TerminalSessionClient;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * یه شل ساده‌ی خودِ اندروید (/system/bin/sh) رو اجرا میکنه؛ چون GhostIDE، ترموکس نیست و
 * userland نصب‌شده‌ی ترموکس (زیر /data/data/com.termux/...) رو نداره و اصولاً هم به یه اپ
 * دیگه دسترسی sandbox-شده نداره. اگه کاربر خودش ترموکس رو نصب کرده و میخواد به اون وصل بشه،
 * اون یه مسیر کاملاً جدا (مثلاً RUN_COMMAND intent) میخواد، نه این کلاس.
 */
public final class TerminalSessionFactory {

  private TerminalSessionFactory() {}

  private static final String[] CANDIDATE_SHELLS = {
    "/system/bin/sh", "/system/xbin/sh", "/bin/sh",
  };

  public static TerminalSession createSession(
      Context context, String workingDirectory, TerminalSessionClient client) {
    String shellPath = resolveShellPath();
    String cwd = resolveCwd(context, workingDirectory);
    String[] env = buildEnvironment(context, cwd);
    String[] args = null;
    return new TerminalSession(shellPath, cwd, args, env, null, client);
  }

  private static String resolveShellPath() {
    for (String candidate : CANDIDATE_SHELLS) {
      if (new File(candidate).exists()) return candidate;
    }
    return CANDIDATE_SHELLS[0];
  }

  private static String resolveCwd(Context context, String workingDirectory) {
    if (workingDirectory != null && new File(workingDirectory).isDirectory()) {
      return workingDirectory;
    }
    File external = Environment.getExternalStorageDirectory();
    if (external != null && external.isDirectory()) return external.getAbsolutePath();
    return context.getFilesDir().getAbsolutePath();
  }

  private static String[] buildEnvironment(Context context, String cwd) {
    List<String> env = new ArrayList<>();
    env.add("HOME=" + cwd);
    env.add("PATH=/sbin:/system/sbin:/system/bin:/system/xbin:/vendor/bin");
    env.add("TERM=xterm-256color");
    env.add("TMPDIR=" + context.getCacheDir().getAbsolutePath());
    env.add("COLORTERM=truecolor");
    return env.toArray(new String[0]);
  }
}
