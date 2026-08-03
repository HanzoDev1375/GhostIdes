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

import ir.hanzodev1375.ghostide.codeeditors.langs.php.PhpLanguage;
import ir.hanzodev1375.ghostide.codeeditors.langs.formatHelp.DebianBootstrap;
import java.util.concurrent.CountDownLatch;
import android.os.Handler;
import android.os.Looper;
import io.github.rosemoe.sora.lsp.editor.LspLanguage;
import org.eclipse.lsp4j.ServerCapabilities;

/**
 * curl -fsSL https://deb.nodesource.com/setup_lts.x | bash - apt install -y nodejs node -v npm
 * cache clean --force npm install -g intelephense
 */
public class PhpServer {

  private static final String TAG = "PhpServer";
  private static final String SERVER_NAME = "intelephense";

  private static final Set<String> SUPPORTED_EXTENSIONS =
      new HashSet<>(Arrays.asList("php", "php5", "phtml"));

  private static final String[] CANDIDATE_PATHS = {
    "/usr/local/bin/intelephense", "/usr/bin/intelephense"
  };

  private static final Map<String, LspProject> projects = new HashMap<>();
  private static final Set<String> registeredDefinitions = new HashSet<>();

  private PhpServer() {}

  public static boolean isPhpFile(String filePath) {
    return SUPPORTED_EXTENSIONS.contains(extensionOf(filePath));
  }

  private static String extensionOf(String filePath) {
    if (filePath == null) return "";
    int dot = filePath.lastIndexOf('.');
    if (dot < 0 || dot == filePath.length() - 1) return "";
    return filePath.substring(dot + 1).toLowerCase(Locale.ROOT);
  }

  /** مسیر باینری intelephense رو داخل rootfs پیدا می کنه؛ اگه نبود null. */
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
    List<String> args = Arrays.asList("--stdio");
    ServerCapabilities cap = new ServerCapabilities();
    cap.setCodeActionProvider(true);
    cap.setInlayHintProvider(true);
    cap.setRenameProvider(true);
    cap.setReferencesProvider(true);

    return new CustomLanguageServerDefinition(
        ext,
        workingDir -> new ProotStdioConnectionProvider(context, workingDir, executablePath, args),
        SERVER_NAME,
        cap,
        null);
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
   * فایل PHP باز شده رو به intelephense وصل می کنه. حتما توی ترد جدا صدا بزن.
   *
   * @return LspEditor ساخته شده، یا null اگه سرور نصب نباشه
   */
  public static LspEditor connectFile(
      Context context, String projectRoot, String filePath, CodeEditor editor) {
    String executablePath = findInstalledExecutable(context);
    if (executablePath == null) {
      Log.e(TAG, "intelephense نصب نیست. داخل ترمینال proot اجرا کن: npm install -g intelephense");
      return null;
    }

    String ext = extensionOf(filePath);
    LspProject project = getOrCreateProject(projectRoot);
    ensureDefinitionRegistered(project, context, executablePath, projectRoot, ext);
    String emmetExecutable = EmmetServer.findInstalledExecutable(context);
    if (emmetExecutable != null) {
      EmmetServer.ensureDefinitionRegistered(project, context, emmetExecutable, projectRoot, ext);
      Log.d(TAG, "Emmet Language Server نیز به پروژه اضافه شد.");
    } else {
      Log.d(TAG, "Emmet Language Server نصب نیست، فقط از HTML Server استفاده می‌شود.");
    }

    final LspEditor[] holder = new LspEditor[1];
    final CountDownLatch latch = new CountDownLatch(1);

    new Handler(Looper.getMainLooper())
        .post(
            () -> {
              try {
                LspEditor e = project.createEditor(filePath);
                var html = new PhpLanguage(context);
                e.setWrapperLanguage(html);
                e.setEditor(editor);
                e.setEnableInlayHint(true);
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
      Log.e(TAG, "اتصال به intelephense ناموفق بود", e);
    }

    return lspEditor;
  }
}
