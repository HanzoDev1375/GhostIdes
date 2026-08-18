package ir.hanzodev1375.ghostide.codeeditors.langs.lsp;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.CustomLanguageServerDefinition;
import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.LanguageServerDefinition;
import io.github.rosemoe.sora.lsp.editor.LspEditor;
import io.github.rosemoe.sora.lsp.editor.LspLanguage;
import io.github.rosemoe.sora.lsp.editor.LspProject;
import io.github.rosemoe.sora.widget.CodeEditor;
import ir.hanzodev1375.ghostide.codeeditors.langs.formatHelp.DebianBootstrap;
import ir.hanzodev1375.ghostide.codeeditors.langs.java.JavaLanguage;

public class JavaServer {

  private static final String TAG = "JavaServer";
  private static final String SERVER_NAME = "jdtls";
  private static final Set<String> SUPPORTED_EXTENSIONS = Collections.singleton("java");

  private static final String[] JAVA_CANDIDATE_PATHS = {"/usr/bin/java"};
  private static final String[] JDTLS_CANDIDATE_PATHS = {
    "/root/jdtls/bin/jdtls", "/opt/jdtls/bin/jdtls", "/usr/local/bin/jdtls", "/usr/bin/jdtls"
  };

  private static final Map<String, LspProject> projects = new HashMap<>();

  private JavaServer() {}

  public static String findJdtlsExecutable(Context context) {
    File rootfs = DebianBootstrap.getRootfsDir(context);
    if (rootfs == null || !rootfs.exists()) return null;
    for (String candidate : JDTLS_CANDIDATE_PATHS) {
      if (new File(rootfs, candidate.substring(1)).exists()) return candidate;
    }
    return null;
  }

  public static boolean isInstalled(Context context) {
    if (findJavaExecutable(context) == null) return false;
    return findJdtlsExecutable(context) != null;
  }

  // ---------------------------------------------------------------------
  // تنظیمات jdtls به‌عنوان invisible project — هیچ فایلی رو دیسک پروژه نمی‌سازه.
  // sourcePaths/referencedLibraries از initializationOptions.settings.java میره.
  // ---------------------------------------------------------------------

  private static Map<String, Object> buildInvisibleProjectInitOptions(
      File rootfs, File projectRoot, List<File> sourceRoots, List<File> libJars) {

    List<String> relativeSourcePaths = new ArrayList<>();
    for (File src : sourceRoots) {
      relativeSourcePaths.add(AndroidClasspathResolver.relativize(projectRoot, src));
    }

    List<String> libraryPaths = new ArrayList<>();
    for (File jar : libJars) {
      libraryPaths.add(AndroidClasspathResolver.toGuestLibraryPath(rootfs, jar));
    }

    Map<String, Object> project = new HashMap<>();
    project.put("sourcePaths", relativeSourcePaths);
    project.put("referencedLibraries", libraryPaths);

    Map<String, Object> gradleEnabled = new HashMap<>();
    gradleEnabled.put("enabled", false);
    Map<String, Object> mavenEnabled = new HashMap<>();
    mavenEnabled.put("enabled", false);
    Map<String, Object> importSettings = new HashMap<>();
    importSettings.put("gradle", gradleEnabled);
    importSettings.put("maven", mavenEnabled);

    Map<String, Object> java = new HashMap<>();
    java.put("project", project);
    java.put("import", importSettings);

    Map<String, Object> settings = new HashMap<>();
    settings.put("java", java);

    Map<String, Object> initializationOptions = new HashMap<>();
    initializationOptions.put("settings", settings);
    return initializationOptions;
  }

  // ---------------------------------------------------------------------

  private static LanguageServerDefinition createDefinition(
      Context context,
      String javaExecutable,
      String jdtlsExecutable,
      String projectRoot,
      List<File> sourceRoots,
      List<File> libJars) {

    String workspaceId = sanitize(projectRoot);
    File dataDir = new File(context.getCacheDir(), "jdtls-workspace/" + workspaceId);
    dataDir.mkdirs();

    List<String> args = new ArrayList<>();
    args.add("--java-executable");
    args.add(javaExecutable);
    args.add("--jvm-arg=-Djdk.lang.Process.launchMechanism=FORK");
    args.add("--jvm-arg=-Djdk.xml.maxGeneralEntitySizeLimit=0");
    args.add("--jvm-arg=-Djdk.xml.totalEntitySizeLimit=0");
    args.add("--jvm-arg=-Dlog.level=WARNING");
    args.add("--jvm-arg=-Xms256m");
    args.add("--jvm-arg=-Xmx1G");
    args.add("--jvm-arg=-XX:+UseG1GC");
    args.add("--jvm-arg=-XX:+TieredCompilation");
    args.add("--jvm-arg=-XX:TieredStopAtLevel=1");
    args.add("--jvm-arg=-Dorg.eclipse.jdt.ls.lombok.support=false");
    args.add("--jvm-arg=--add-modules=ALL-SYSTEM");
    args.add("--jvm-arg=--add-opens");
    args.add("--jvm-arg=java.base/java.util=ALL-UNNAMED");
    args.add("--jvm-arg=--add-opens");
    args.add("--jvm-arg=java.base/java.lang=ALL-UNNAMED");
    args.add("-data");
    args.add(dataDir.getAbsolutePath());

    Map<String, Object> initOptions =
        buildInvisibleProjectInitOptions(
            DebianBootstrap.getRootfsDir(context), new File(projectRoot), sourceRoots, libJars);

    return new CustomLanguageServerDefinition(
        "java",
        workingDir -> new ProotStdioConnectionProvider(context, workingDir, jdtlsExecutable, args),
        SERVER_NAME,
        null,
        null) {
      @Override
      public Object getInitializationOptions(URI uri) {
        return initOptions;
      }
    };
  }

  private static synchronized LspProject getOrCreateProject(
      Context context, String projectRoot, String javaExecutable, String jdtlsExecutable) {
    LspProject project = projects.get(projectRoot);
    if (project == null) {
      File projectRootFile = new File(projectRoot);
      List<File> sourceRoots = AndroidClasspathResolver.findJavaSourceRoots(projectRootFile);
      List<File> libJars = AndroidClasspathResolver.findLibraryJars(context, projectRootFile);
      Log.i(
          TAG,
          "jdtls invisible-project: "
              + sourceRoots.size()
              + " سورس‌روت، "
              + libJars.size()
              + " jar");

      project = new LspProject(projectRoot);
      project.addServerDefinition(
          createDefinition(
              context, javaExecutable, jdtlsExecutable, projectRoot, sourceRoots, libJars));
      projects.put(projectRoot, project);
    }
    return project;
  }

  public static LspEditor connectFile(
      Context context, String projectRoot, String filePath, CodeEditor editor) {
    String javaExecutable = findJavaExecutable(context);
    if (javaExecutable == null) {
      Log.e(TAG, "java نصب نیست. داخل rootfs دبیان یک JDK نصب کن.");
      return null;
    }

    String jdtlsExecutable = findJdtlsExecutable(context);
    if (jdtlsExecutable == null) {
      Log.e(TAG, "jdtls پیدا نشد تو هیچکدوم از مسیرهای شناخته‌شده.");
      return null;
    }
    LspProject project = getOrCreateProject(context, projectRoot, javaExecutable, jdtlsExecutable);

    final LspEditor[] holder = new LspEditor[1];
    final CountDownLatch latch = new CountDownLatch(1);

    new Handler(Looper.getMainLooper())
        .post(
            () -> {
              try {
                LspEditor e = project.createEditor(filePath);
                JavaLanguage java = new JavaLanguage(context);
                e.setWrapperLanguage(java);
                e.setEditor(editor);
                var lang = (LspLanguage) editor.getEditorLanguage();
                lang.setFormatter(java.getFormatter());
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
        Log.e(TAG, "اتصال به jdtls ناموفق بود", e);
      }
    }
    return lspEditor;
  }

  public static boolean isJavaFile(String filePath) {
    return SUPPORTED_EXTENSIONS.contains(extensionOf(filePath));
  }

  private static String extensionOf(String filePath) {
    if (filePath == null) return "";
    int dot = filePath.lastIndexOf('.');
    if (dot < 0 || dot == filePath.length() - 1) return "";
    return filePath.substring(dot + 1).toLowerCase(Locale.ROOT);
  }

  public static String findJavaExecutable(Context context) {
    File rootfs = DebianBootstrap.getRootfsDir(context);
    if (rootfs == null || !rootfs.exists()) return null;

    for (String candidate : JAVA_CANDIDATE_PATHS) {
      if (new File(rootfs, candidate.substring(1)).exists()) return candidate;
    }

    File jvmDir = new File(rootfs, "usr/lib/jvm");
    File[] installs = jvmDir.exists() ? jvmDir.listFiles() : null;
    if (installs != null) {
      for (File install : installs) {
        if (new File(install, "bin/java").exists()) {
          return "/usr/lib/jvm/" + install.getName() + "/bin/java";
        }
      }
    }
    return null;
  }

  private static String sanitize(String value) {
    return (value == null || value.isEmpty()) ? "root" : value.replaceAll("[^a-zA-Z0-9]+", "_");
  }

  public static void disconnectFile(LspEditor lspEditor) {
    if (lspEditor == null) return;
    try {
      lspEditor.dispose();
    } catch (Exception e) {
      Log.e(TAG, "بستن اتصال jdtls با خطا مواجه شد", e);
    }
  }
}
