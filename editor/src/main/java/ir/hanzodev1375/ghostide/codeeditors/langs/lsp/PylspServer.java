package ir.hanzodev1375.ghostide.codeeditors.langs.lsp;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.rosemoe.sora.lsp.client.connection.StreamConnectionProvider;
import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.CustomLanguageServerDefinition;
import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.LanguageServerDefinition;
import io.github.rosemoe.sora.lsp.editor.LspEditor;
import io.github.rosemoe.sora.lsp.editor.LspProject;
import io.github.rosemoe.sora.widget.CodeEditor;

import ir.hanzodev1375.ghostide.codeeditors.langs.formatHelp.DebianBootstrap;
import ir.hanzodev1375.ghostide.codeeditors.langs.python3.Python3Language;
import java.util.concurrent.CountDownLatch;
import android.os.Handler;
import android.os.Looper;
import ir.hanzodev1375.ghostide.codeeditors.langs.html.HtmlLanguage;
import io.github.rosemoe.sora.lsp.editor.LspLanguage;

/**
 * اتصال Language Server پایتون (pylsp / python-lsp-server) که داخل rootfsِ proot اجرا می شه.
 * ترنسپورت stdio از همون ProotStdioConnectionProvider موجود در پروژه استفاده می کنه، هیچ فرآیند
 * proot جدیدی خارج از اون کلاس ساخته نمی شه.
 *
 * <p>قبل از استفاده باید داخل ترمینال (rootfs proot) نصب بشه: pip install python-lsp-server جزئیات
 * کامل و عیب یابی داخل PYTHON_LSP_SETUP.md هست، حتما بخون.
 *
 * <p>نکته ی مهم: متد connectFile عملیات I/O سنگین (اجرای proot + هندشیک LSP) انجام می ده، پس هرگز
 * روی UI thread صداش نزن؛ توی یک Thread/Executor جدا اجرا کن.
 */
public class PylspServer {

  private static final String TAG = "PylspServer";
  private static final String SERVER_NAME = "pylsp";

  // مسیرهای احتمالی باینری pylsp داخل rootfs، بسته به اینکه pip با چه دسترسی ای نصبش کرده.
  private static final String[] CANDIDATE_PATHS = {
    "/usr/local/bin/pylsp", "/usr/bin/pylsp", "/root/.local/bin/pylsp"
  };

  // هر ریشه ی پروژه فقط یک LspProject داره؛ بین همه ی فایل های پایتونِ همون پروژه به اشتراک میره
  // تا برای هر تب یک پروسه ی pylsp جدا بالا نیاد.
  private static final Map<String, LspProject> projects = new HashMap<>();

  private PylspServer() {}

  /** مسیر باینری pylsp رو داخل rootfs پیدا می کنه؛ اگه نصب نباشه null برمی گردونه. */
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

  /**
   * تعریف زبان سرور رو می سازه. اگه اسم/امضای CustomLanguageServerDefinition توی کتابخونه ی فعلیت
   * با اینجا فرق داشت (احتمالش کمه ولی چون به AAR کامپایل شده دسترسی مستقیم نداشتم)، روی این متد
   * Ctrl+کلیک بزن توی اندروید استودیو و پارامترها رو match کن؛ همه ی ریسکِ کامپایل این پروژه فقط
   * همینجاست.
   */
  private static LanguageServerDefinition createDefinition(Context context, String executablePath) {
    List<String> noArgs =
        Collections.emptyList(); // pylsp پیش فرض از stdio سرو می کنه، آرگومان اضافه لازم نیست
    return new CustomLanguageServerDefinition(
        "py",
        workingDir -> new ProotStdioConnectionProvider(context, workingDir, executablePath, noArgs),
        SERVER_NAME,
        null, // extensionsOverride -> پیش فرض همون "py" کافیه
        null // expectedCapabilitiesOverride
        );
  }

  private static synchronized LspProject getOrCreateProject(
      Context context, String projectRoot, String executablePath) {
    LspProject project = projects.get(projectRoot);
    if (project == null) {
      project = new LspProject(projectRoot);
      project.addServerDefinition(createDefinition(context, executablePath));
      projects.put(projectRoot, project);
    }
    return project;
  }

  /**
   * فایل پایتونِ باز شده رو به pylsp وصل می کنه. حتما توی ترد جدا صدا بزن.
   *
   * @param context Context برنامه
   * @param projectRoot ریشه ی پروژه روی خودِ دستگاه (همون پوشه ای که در GhostIDE بازه)
   * @param filePath مسیر کامل فایل .py روی دستگاه (نه مسیر داخل proot)
   * @param editor ویجت CodeEditor که فایل توش باز شده
   * @return LspEditor ساخته شده (برای disconnectFile موقع بستن تب نگهش دار)، یا null اگه pylsp نصب
   *     نباشه
   */
  public static LspEditor connectFile(
      Context context, String projectRoot, String filePath, CodeEditor editor) {
    String executablePath = findInstalledExecutable(context);
    if (executablePath == null) {
      Log.e(TAG, "pylsp نصب نیست. داخل ترمینال proot اجرا کن: pip install python-lsp-server");
      return null;
    }

    LspProject project = getOrCreateProject(context, projectRoot, executablePath);
    final LspEditor[] holder = new LspEditor[1];
    final CountDownLatch latch = new CountDownLatch(1);

    new Handler(Looper.getMainLooper())
        .post(
            () -> {
              try {
                LspEditor e = project.createEditor(filePath);
                var py = new Python3Language(context);
                e.setWrapperLanguage(py);
                e.setEditor(editor);
                var lang = (LspLanguage) editor.getEditorLanguage();
                lang.setFormatter(py.getFormatter());
                holder[0] = e;
              } finally {
                latch.countDown();
              }
            });

    try {
      latch.await();
    } catch (InterruptedException ignored) {
      Thread.currentThread().interrupt();
    }

    LspEditor lspEditor = holder[0];
    if (lspEditor != null) {
      try {
        lspEditor.connectWithTimeoutBlocking();
      } catch (Exception e) {
        Log.e(TAG, "اتصال به pylsp ناموفق بود", e);
      }
    }
    return lspEditor;
  }

  /**
   * موقع بستن تب/فایل صدا بزن تا اتصال lsp این فایل بسته بشه. اسم دقیق متد آزادسازی (dispose) رو
   * مطمئن نیستم؛ اگه کامپایل نشد این خط رو موقتا کامنت کن، بقیه ی کد کار می کنه (فقط پروسه سروری تا
   * بسته شدن پروژه زنده می مونه، مشکل عملکردی نیست).
   */
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
