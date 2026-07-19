package ir.hanzodev1375.ghostide.codeeditors.langs.lsp;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.CustomLanguageServerDefinition;
import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.LanguageServerDefinition;
import io.github.rosemoe.sora.lsp.editor.LspEditor;
import io.github.rosemoe.sora.lsp.editor.LspProject;
import io.github.rosemoe.sora.widget.CodeEditor;
import ir.hanzodev1375.ghostide.codeeditors.langs.csharp.CSharpLanguage;
import ir.hanzodev1375.ghostide.codeeditors.langs.formatHelp.DebianBootstrap;
import java.io.File;
import java.util.*;
import java.util.concurrent.CountDownLatch;

public class CsharpServer {

  private static final String TAG = "CsharpServer";
  private static final Set<String> SUPPORTED_EXTENSIONS =
      new HashSet<>(Collections.singletonList("cs"));
  private static final String[] CANDIDATE_PATHS = {"/root/omnisharp/run", "/opt/omnisharp/run"};
  private static final Map<String, LspProject> projects = new HashMap<>();
  private static final Set<String> registeredDefinitions = new HashSet<>();

  public static boolean isCsharpFile(String path) {
    return SUPPORTED_EXTENSIONS.contains(extensionOf(path));
  }

  private static String extensionOf(String path) {
    int dot = path.lastIndexOf('.');
    return dot > 0 ? path.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
  }

  public static String findExecutable(Context context) {
    File root = DebianBootstrap.getRootfsDir(context);
    if (root == null) return null;
    for (String p : CANDIDATE_PATHS) {
      if (new File(root, p.substring(1)).exists()) return p;
    }
    return null;
  }

  public static boolean isInstalled(Context context) {
    return findExecutable(context) != null;
  }

  private static synchronized LspProject getOrCreateProject(String projectRoot) {
    LspProject project = projects.get(projectRoot);
    if (project == null) {
      project = new LspProject(projectRoot);
      projects.put(projectRoot, project);
    }
    return project;
  }

  public static LspEditor connectFile(
      Context context, String projectRoot, String filePath, CodeEditor editor) {
    String exe = findExecutable(context);
    if (exe == null) {
      Log.e(TAG, "omnisharp پیدا نشد.");
      return null;
    }

    String ext = extensionOf(filePath);
    LspProject project = getOrCreateProject(projectRoot);

    String key = projectRoot + "::" + ext;
    if (!registeredDefinitions.contains(key)) {
      LanguageServerDefinition def =
          new CustomLanguageServerDefinition(
              ext,
              wd ->
                  new ProotStdioConnectionProvider(
                      context, wd, exe, Arrays.asList("-lsp", "-s", projectRoot)),
              "omnisharp",
              null,
              null);
      project.addServerDefinition(def);
      registeredDefinitions.add(key);
    }

    final LspEditor[] holder = new LspEditor[1];
    CountDownLatch latch = new CountDownLatch(1);
    new Handler(Looper.getMainLooper())
        .post(
            () -> {
              try {
                LspEditor e = project.createEditor(filePath);
                e.setWrapperLanguage(new CSharpLanguage());
                e.setEditor(editor);
                holder[0] = e;
              } finally {
                latch.countDown();
              }
            });

    try {
      latch.await();
    } catch (InterruptedException ignored) {
    }
    LspEditor lsp = holder[0];
    if (lsp != null) {
      try {
        lsp.connectWithTimeoutBlocking();
      } catch (Exception e) {
        Log.e(TAG, "اتصال به omnisharp ناموفق", e);
      }
    }
    return lsp;
  }

  public static void disconnectFile(LspEditor lsp) {
    if (lsp != null)
      try {
        lsp.dispose();
      } catch (Exception e) {
        Log.e(TAG, "خطا در بستن LSP سی‌شارپ", e);
      }
  }
}
