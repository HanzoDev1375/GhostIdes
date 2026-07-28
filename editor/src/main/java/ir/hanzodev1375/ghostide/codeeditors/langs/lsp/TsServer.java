package ir.hanzodev1375.ghostide.codeeditors.langs.lsp;

import android.content.Context;
import android.util.Log;

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

import ir.hanzodev1375.ghostide.codeeditors.langs.js.JsLanguage;
import ir.hanzodev1375.ghostide.codeeditors.langs.formatHelp.DebianBootstrap;
import java.util.concurrent.CountDownLatch;
import android.os.Handler;
import android.os.Looper;
import io.github.rosemoe.sora.lsp.editor.LspLanguage;

/**
 * اتصال Language Server جاوااسکریپت. دیگه از typescript-language-server استفاده نمی کنه چون
 * TypeScript 7 (که npm الان به صورت پیش فرض نصب می کنه) کامپایلرش رو با Go بازنویسی کرده و دیگه
 * tsserver.js قدیمی رو نداره؛ typescript-language-server هنوز باهاش سازگار نشده.
 *
 * <p>به جاش مستقیم از حالت LSP بومیِ خودِ tsc استفاده می کنیم: «tsc --lsp -stdio». نیازی به پکیج
 * جدا (typescript-language-server) نیست، فقط خودِ typescript کافیه.
 *
 * <p>نصب داخل ترمینال proot (نیاز به Node.js داره): npm install -g typescript
 *
 * <p>نکته: connectFile عملیات I/O سنگین انجام می ده، حتما توی ترد جدا صداش بزن.
 */
public class TsServer {

  private static final String TAG = "TsServer";
  private static final String SERVER_NAME = "tsc --lsp";

  private static final Set<String> SUPPORTED_EXTENSIONS =
      new HashSet<>(Arrays.asList("js", "mjs", "cjs", "jsx", "ts", "tsx"));

  // بسته به اینکه npm prefix تو rootfs چیه، معمولا یکی از این هاست (npm root -g رو چک کن).
  private static final String[] CANDIDATE_PATHS = {"/usr/bin/tsc", "/usr/local/bin/tsc"};

  private static final Map<String, LspProject> projects = new HashMap<>();
  private static final Set<String> registeredDefinitions = new HashSet<>();

  private TsServer() {}

  public static boolean isJsFile(String filePath) {
    return SUPPORTED_EXTENSIONS.contains(extensionOf(filePath));
  }

  private static String extensionOf(String filePath) {
    if (filePath == null) return "";
    int dot = filePath.lastIndexOf('.');
    if (dot < 0 || dot == filePath.length() - 1) return "";
    return filePath.substring(dot + 1).toLowerCase(Locale.ROOT);
  }

  /** مسیر باینری tsc رو داخل rootfs پیدا می کنه؛ اگه نبود null. */
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

  /** همون نکته ی ریسکِ constructor که در PylspServer/ClangdServer گفتم، اینجا هم صدق می کنه. */
  private static LanguageServerDefinition createDefinition(
      Context context, String executablePath, String ext) {
    List<String> args = Arrays.asList("--lsp", "-stdio");
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
   * فایل JS باز شده رو به «tsc --lsp» وصل می کنه. حتما توی ترد جدا صدا بزن.
   *
   * @return LspEditor ساخته شده (برای disconnectFile نگهش دار)، یا null اگه tsc نصب نباشه
   */
  public static LspEditor connectFile(
      Context context, String projectRoot, String filePath, CodeEditor editor) {
    String executablePath = findInstalledExecutable(context);
    if (executablePath == null) {
      Log.e(TAG, "tsc نصب نیست. داخل ترمینال proot اجرا کن: npm install -g typescript");
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
                var js = new JsLanguage(context, filePath);
                e.setWrapperLanguage(js);
                e.setEditor(editor);
                e.setEnableInlayHint(true);
                e.setEnableSignatureHelp(true);
                e.setEnableHover(true);
                var lang = (LspLanguage) editor.getEditorLanguage();
                lang.setFormatter(js.getFormatter());
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
      Log.e(TAG, "اتصال به tsc --lsp ناموفق بود", e);
    }

    return lspEditor;
  }

  /** موقع بستن تب/فایل صدا بزن. اگه dispose کامپایل نشد کامنتش کن. */
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
