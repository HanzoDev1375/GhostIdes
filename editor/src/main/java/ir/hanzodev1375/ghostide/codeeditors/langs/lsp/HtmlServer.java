package ir.hanzodev1375.ghostide.codeeditors.langs.lsp;

import android.content.Context;
import android.util.Log;
import io.github.rosemoe.sora.lsp.editor.LspLanguage;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.CustomLanguageServerDefinition;
import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.LanguageServerDefinition;
import io.github.rosemoe.sora.lsp.editor.LspEditor;
import io.github.rosemoe.sora.lsp.editor.LspProject;
import io.github.rosemoe.sora.widget.CodeEditor;
import ir.hanzodev1375.ghostide.codeeditors.langs.html.HtmlLanguage;
import ir.hanzodev1375.ghostide.codeeditors.langs.formatHelp.DebianBootstrap;
import java.util.concurrent.CountDownLatch;
import android.os.Handler;
import android.os.Looper;

/**
 * اتصال Language Server اچ‌تی‌ام‌ال. از vscode-html-language-server (موجود در پکیج
 * vscode-langservers-extracted) استفاده می‌کنه که دقیقاً همون موتور هوشمند HTML مربوط به VSCode
 * هست.
 *
 * <p>نصب داخل ترمینال proot (نیاز به Node.js داره): npm install -g vscode-langservers-extracted
 *
 * <p>نکته: مثل TsServer، عملیات connectFile سنگین هست و حتماً باید توی ترد جدا (غیر از UI Thread)
 * صدا زده بشه، اما بخش ساخت LspEditor و ست کردن اون روی CodeEditor باید روی UI Thread اجرا بشه.
 */
public class HtmlServer {
  private static final String TAG = "HtmlServer";
  private static final String SERVER_NAME = "vscode-html-language-server";
  private static final Set<String> SUPPORTED_EXTENSIONS =
      new HashSet<>(Arrays.asList("html", "htm"));

  // بسته به اینکه npm prefix تو rootfs چیه، ممکنه تو یکی از این مسیرها باشه.
  private static final String[] CANDIDATE_PATHS = {
    "/usr/bin/vscode-html-language-server",
    "/usr/local/bin/vscode-html-language-server",
    "/usr/bin/html-languageserver", // برخی نسخه‌های قدیمی‌تر یا فورک‌ها
    "/usr/local/bin/html-languageserver"
  };

  private static final Map<String, LspProject> projects = new HashMap<>();
  private static final Set<String> registeredDefinitions = new HashSet<>();

  private HtmlServer() {}

  public static boolean isHtmlFile(String filePath) {
    return SUPPORTED_EXTENSIONS.contains(extensionOf(filePath));
  }

  private static String extensionOf(String filePath) {
    if (filePath == null) return "";
    int dot = filePath.lastIndexOf('.');
    if (dot < 0 || dot == filePath.length() - 1) return "";
    return filePath.substring(dot + 1).toLowerCase(Locale.ROOT);
  }

  /** مسیر باینری language server رو داخل rootfs پیدا می‌کنه؛ اگه نبود null. */
  public static String findInstalledExecutable(Context context) {
    File rootfs = DebianBootstrap.getRootfsDir(context);
    if (rootfs == null || !rootfs.exists()) {
      return null;
    }
    for (String candidate : CANDIDATE_PATHS) {
      File f = new File(rootfs, candidate.substring(1));
      if (f.exists()) {
        return candidate;
      }
    }
    return null;
  }

  public static boolean isInstalled(Context context) {
    return findInstalledExecutable(context) != null;
  }

  private static LanguageServerDefinition createDefinition(
      Context context, String executablePath, String ext) {
    // سرور HTML برای اجرای stdio نیاز به آرگومان --stdio داره
    List<String> args = Arrays.asList("--stdio");
    return new CustomLanguageServerDefinition(
        ext,
        workingDir -> new ProotStdioConnectionProvider(context, workingDir, executablePath, args),
        SERVER_NAME,
        null, // extensionsOverride
        null // expectedCapabilitiesOverride
        );
  }

  private static synchronized LspProject getOrCreateProject(String projectRoot) {
    LspProject project = projects.get(projectRoot);
    if (project == null) {
      project = new LspProject(projectRoot);
      projects.put(projectRoot, project);
    }
    return project;
  }

  private static synchronized void ensureDefinitionRegistered(
      LspProject project, Context context, String executablePath, String projectRoot, String ext) {
    String key = projectRoot + "::" + ext;
    if (!registeredDefinitions.contains(key)) {
      project.addServerDefinition(createDefinition(context, executablePath, ext));
      registeredDefinitions.add(key);
    }
  }

  /**
   * فایل HTML باز شده رو به language server وصل می‌کنه. حتماً توی ترد جدا صدا بزن.
   *
   * @return LspEditor ساخته شده (برای disconnectFile نگهش دار)، یا null اگه سرور نصب نباشه
   */
  public static LspEditor connectFile(
      Context context, String projectRoot, String filePath, CodeEditor editor) {
    String executablePath = findInstalledExecutable(context);
    if (executablePath == null) {
      Log.e(
          TAG,
          "vscode-html-language-server نصب نیست. داخل ترمینال proot اجرا کن: npm install -g vscode-langservers-extracted");
      return null;
    }

    String ext = extensionOf(filePath);
    LspProject project = getOrCreateProject(projectRoot);
    ensureDefinitionRegistered(project, context, executablePath, projectRoot, ext);

    final LspEditor[] holder = new LspEditor[1];
    final CountDownLatch latch = new CountDownLatch(1);

    new Handler(Looper.getMainLooper())
        .post(
            () -> {
              try {
                LspEditor e = project.createEditor(filePath);
                var html = new HtmlLanguage(context, filePath);
                e.setWrapperLanguage(html);
                e.setEditor(editor);
                var lang = (LspLanguage) editor.getEditorLanguage();
                lang.setFormatter(html.getFormatter());
                holder[0] = e;
              } finally {
                latch.countDown();
              }
            });

    try {
      latch.await();
    } catch (InterruptedException ignored) {
    }

    LspEditor lspEditor = holder[0];
    if (lspEditor == null) {
      return null;
    }

    try {
      lspEditor.connectWithTimeoutBlocking();
    } catch (Exception e) {
      Log.e(TAG, "اتصال به vscode-html-language-server ناموفق بود", e);
    }

    return lspEditor;
  }

  /** موقع بستن تب/فایل صدا بزن. */
  public static void disconnectFile(LspEditor lspEditor) {
    if (lspEditor == null) {
      return;
    }
    try {
      lspEditor.dispose();
    } catch (Exception e) {
      Log.e(TAG, "بستن اتصال lsp با خطا مواجه شد", e);
    }
  }
}
