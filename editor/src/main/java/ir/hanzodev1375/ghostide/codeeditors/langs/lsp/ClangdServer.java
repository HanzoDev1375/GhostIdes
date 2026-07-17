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

import ir.hanzodev1375.ghostide.codeeditors.langs.cpp.CppLanguage;
import ir.hanzodev1375.ghostide.codeeditors.langs.formatHelp.DebianBootstrap;

/**
 * اتصال Language Server سی پلاس پلاس (clangd) که داخل rootfsِ proot اجرا می شه.
 * دقیقا هم ساختار PylspServer، از همون ProotStdioConnectionProvider استفاده می کنه.
 *
 * نصب داخل ترمینال proot:
 *   apt update && apt install -y clangd
 *
 * نکته: connectFile عملیات I/O سنگین انجام می ده (proot spawn + هندشیک LSP)، حتما توی
 * ترد جدا صداش بزن، نه روی UI thread.
 */
public class ClangdServer {

  private static final String TAG = "ClangdServer";
  private static final String SERVER_NAME = "clangd";

  // پسوندهایی که clangd پوشش می ده (همون هایی که LanguageManager به CppLanguage می ده)
  private static final Set<String> SUPPORTED_EXTENSIONS =
      new HashSet<>(Arrays.asList("cpp", "cxx", "cc", "hpp", "hxx", "h"));

  private static final String[] CANDIDATE_PATHS = {
    "/usr/bin/clangd",
    "/usr/local/bin/clangd",
    "/usr/bin/clangd-18",
    "/usr/bin/clangd-17",
    "/usr/bin/clangd-16",
    "/usr/bin/clangd-15",
    "/usr/bin/clangd-14",
    "/usr/bin/clangd-12"
  };

  private static final Map<String, LspProject> projects = new HashMap<>();
  // کلید: projectRoot + "::" + ext -> جلوگیری از رجیستر تکراری تعریف سرور برای یک پسوند
  private static final Set<String> registeredDefinitions = new HashSet<>();

  private ClangdServer() {}

  public static boolean isCppFile(String filePath) {
    return SUPPORTED_EXTENSIONS.contains(extensionOf(filePath));
  }

  private static String extensionOf(String filePath) {
    if (filePath == null) return "";
    int dot = filePath.lastIndexOf('.');
    if (dot < 0 || dot == filePath.length() - 1) return "";
    return filePath.substring(dot + 1).toLowerCase(Locale.ROOT);
  }

  /** مسیر باینری clangd رو داخل rootfs پیدا می کنه؛ اگه نصب نباشه null برمی گردونه. */
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
   * همون نکته ی PylspServer اینجا هم صدق می کنه: تنها بخشی که بدون دسترسی به AAR کامپایل شده
   * صد در صد مطمئن نیستم همین constructor هست. اگه ارور داد Ctrl+کلیک بزن روی
   * CustomLanguageServerDefinition و پارامترها رو match کن.
   */
  private static LanguageServerDefinition createDefinition(
      Context context, String executablePath, String ext) {
    List<String> args = Arrays.asList("--background-index");
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
   * فایل C++ باز شده رو به clangd وصل می کنه. حتما توی ترد جدا صدا بزن.
   *
   * @param context Context برنامه
   * @param projectRoot ریشه ی پروژه روی خودِ دستگاه (پوشه ای که clangd باید ایندکس کنه،
   *     ایده آل ریشه ای هست که compile_commands.json توش باشه، ولی پوشه ی فایل هم کار می کنه)
   * @param filePath مسیر کامل فایل .cpp/.h/... روی دستگاه (نه مسیر داخل proot)
   * @param editor ویجت CodeEditor که فایل توش باز شده
   * @return LspEditor ساخته شده (برای disconnectFile موقع بستن تب نگهش دار)، یا null اگه clangd نصب نباشه
   */
  public static LspEditor connectFile(
      Context context, String projectRoot, String filePath, CodeEditor editor) {
    String executablePath = findInstalledExecutable(context);
    if (executablePath == null) {
      Log.e(TAG, "clangd نصب نیست. داخل ترمینال proot اجرا کن: apt install -y clangd");
      return null;
    }

    String ext = extensionOf(filePath);
    LspProject project = getOrCreateProject(projectRoot);
    ensureDefinitionRegistered(project, context, executablePath, projectRoot, ext);

    LspEditor lspEditor = project.createEditor(filePath);
    lspEditor.setWrapperLanguage(new CppLanguage(context));
    lspEditor.setEditor(editor);

    try {
      lspEditor.connectWithTimeoutBlocking();
    } catch (Exception e) {
      Log.e(TAG, "اتصال به clangd ناموفق بود", e);
    }

    return lspEditor;
  }

  /** موقع بستن تب/فایل صدا بزن. اگه dispose کامپایل نشد کامنتش کن، مشکل عملکردی نیست. */
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
