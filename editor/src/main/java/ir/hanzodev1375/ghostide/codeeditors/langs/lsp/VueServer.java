package ir.hanzodev1375.ghostide.codeeditors.langs.lsp;

import android.content.Context;
import android.util.Log;
import android.os.Handler;
import android.os.Looper;
import io.github.rosemoe.sora.lsp.editor.LspLanguage;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.CustomLanguageServerDefinition;
import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.LanguageServerDefinition;
import io.github.rosemoe.sora.lsp.editor.LspEditor;
import io.github.rosemoe.sora.lsp.editor.LspProject;
import io.github.rosemoe.sora.widget.CodeEditor;
import ir.hanzodev1375.ghostide.codeeditors.langs.vue.VueLanguage;
import ir.hanzodev1375.ghostide.codeeditors.langs.formatHelp.DebianBootstrap;

public class VueServer {
  private static final String TAG = "VueServer";
  private static final String SERVER_NAME = "vue-language-server";
  private static final Set<String> SUPPORTED_EXTENSIONS = new HashSet<>(Arrays.asList("vue"));
  private static final String[] CANDIDATE_PATHS = {
    "/usr/bin/vue-language-server", "/usr/local/bin/vue-language-server"
  };
  private static final Map<String, LspProject> projects = new HashMap<>();
  private static final Set<String> registeredDefinitions = new HashSet<>();

  private VueServer() {}

  public static boolean isVueFile(String filePath) {
    return SUPPORTED_EXTENSIONS.contains(extensionOf(filePath));
  }

  private static String extensionOf(String filePath) {
    if (filePath == null) return "";
    int dot = filePath.lastIndexOf('.');
    if (dot < 0 || dot == filePath.length() - 1) return "";
    return filePath.substring(dot + 1).toLowerCase(Locale.ROOT);
  }

  public static String findInstalledExecutable(Context context) {
    File rootfs = DebianBootstrap.getRootfsDir(context);
    if (rootfs == null || !rootfs.exists()) return null;
    for (String candidate : CANDIDATE_PATHS) {
      File f = new File(rootfs, candidate.substring(1));
      if (f.exists()) return candidate;
    }
    return null;
  }

  public static boolean isInstalled(Context context) {
    return findInstalledExecutable(context) != null;
  }

  private static LanguageServerDefinition createVueDefinition(
      Context context, String executablePath, String ext) {
    List<String> args = Arrays.asList("--stdio");
    return new CustomLanguageServerDefinition(
        ext,
        workingDir -> new ProotStdioConnectionProvider(context, workingDir, executablePath, args),
        SERVER_NAME,
        null,
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
    String key = projectRoot + "::" + ext + "::vue";
    if (!registeredDefinitions.contains(key)) {
      project.addServerDefinition(createVueDefinition(context, executablePath, ext));
      registeredDefinitions.add(key);
    }
  }

  public static LspEditor connectFile(
      Context context, String projectRoot, String filePath, CodeEditor editor) {
    String vueExecutable = findInstalledExecutable(context);
    if (vueExecutable == null) {
      Log.e(TAG, "vue-language-server نصب نیست");
      return null;
    }
    String ext = extensionOf(filePath);
    LspProject project = getOrCreateProject(projectRoot);
    ensureDefinitionRegistered(project, context, vueExecutable, projectRoot, ext);

    final LspEditor[] holder = new LspEditor[1];
    final CountDownLatch latch = new CountDownLatch(1);
    new Handler(Looper.getMainLooper())
        .post(
            () -> {
              try {
                LspEditor e = project.createEditor(filePath);
                var vue = new VueLanguage(context);
                e.setWrapperLanguage(vue);
                e.setEditor(editor);
                //var lang = (LspLanguage) editor.getEditorLanguage();
               // lang.setFormatter(vue.getFormatter());
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
    if (lspEditor == null) return null;
    try {
      lspEditor.connectWithTimeoutBlocking();
    } catch (Exception e) {
      Log.e(TAG, "اتصال به Vue LSP ناموفق بود", e);
    }
    return lspEditor;
  }

  public static void disconnectFile(LspEditor lspEditor) {
    if (lspEditor == null) return;
    try {
      lspEditor.dispose();
    } catch (Exception e) {
      Log.e(TAG, "خطا در قطع اتصال LSP", e);
    }
  }
}
