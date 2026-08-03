package ir.hanzodev1375.ghostide.codeeditors.langs.lsp;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;
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

/**
 * اتصال Language Server جاوا (Eclipse JDT Language Server) که داخل rootfs پروت اجرا میشود. این کلاس
 * از ProotStdioConnectionProvider برای اجرای مستقیم java با آرگومان های jdtls استفاده میکند.
 *
 * <p>نصب داخل ترمینال proot:
 *
 * <pre>
 *   apt update && apt install -y default-jdk curl
 *   mkdir -p ~/jdtls && cd ~/jdtls
 *   curl -L -o jdtls.tar.gz https://download.eclipse.org/jdtls/snapshots/jdt-language-server-latest.tar.gz
 *   tar -xzf jdtls.tar.gz
 * </pre>
 *
 * <p>توجه: فایل فشرده jdtls ممکن است محتویات را در یک پوشه فرعی (مانند jdt-language-server-x.x.x)
 * استخراج کند. این کلاس به صورت پویا پوشه صحیح را که حاوی دایرکتوری plugins است، شناسایی میکند.
 */
public class JavaServer {

  private static final String TAG = "JavaServer";
  private static final String SERVER_NAME = "jdtls";
  private static final Set<String> SUPPORTED_EXTENSIONS = Collections.singleton("java");

  private static final String[] JAVA_CANDIDATE_PATHS = {"/usr/bin/java"};
  private static final String JDTLS_HOME = "/root/jdtls";
  private static final String[] CONFIG_DIR_CANDIDATES = {"config_linux_arm", "config_linux"};

  private static final Map<String, LspProject> projects = new HashMap<>();

  private JavaServer() {}

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
      if (new File(rootfs, candidate.substring(1)).exists()) {
        return candidate;
      }
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

  private static class JdtlsInfo {
    final File hostBaseDir;
    final String guestBasePath;

    JdtlsInfo(File hostBaseDir, String guestBasePath) {
      this.hostBaseDir = hostBaseDir;
      this.guestBasePath = guestBasePath;
    }
  }

  private static JdtlsInfo findJdtlsInfo(File rootfsHostDir) {
    if (rootfsHostDir == null || !rootfsHostDir.exists()) {
      return null;
    }
    File jdtlsDir = new File(rootfsHostDir, JDTLS_HOME.substring(1));
    if (jdtlsDir.exists() && jdtlsDir.isDirectory()) {
      if (new File(jdtlsDir, "plugins").exists()) {
        return new JdtlsInfo(jdtlsDir, JDTLS_HOME);
      }
      File[] children = jdtlsDir.listFiles();
      if (children != null) {
        for (File child : children) {
          if (child.isDirectory() && new File(child, "plugins").exists()) {
            String rootfsPath = rootfsHostDir.getAbsolutePath();
            String childPath = child.getAbsolutePath();
            if (childPath.startsWith(rootfsPath)) {
              String relative = childPath.substring(rootfsPath.length());
              String guestPath = relative.startsWith("/") ? relative : "/" + relative;
              return new JdtlsInfo(child, guestPath.replace("\\", "/"));
            }
          }
        }
      }
    }
    return null;
  }

  private static String findLauncherJarName(File jdtlsBaseDir) {
    File pluginsDir = new File(jdtlsBaseDir, "plugins");
    File[] files = pluginsDir.exists() ? pluginsDir.listFiles() : null;
    if (files == null) return null;
    for (File f : files) {
      if (f.getName().startsWith("org.eclipse.equinox.launcher_") && f.getName().endsWith(".jar")) {
        return f.getName();
      }
    }
    return null;
  }

  private static String findConfigDirName(File jdtlsBaseDir) {
    for (String candidate : CONFIG_DIR_CANDIDATES) {
      File dir = new File(jdtlsBaseDir, candidate);
      if (dir.exists() && dir.isDirectory()) {
        return candidate;
      }
    }
    return null;
  }

  public static boolean isInstalled(Context context) {
    File rootfs = DebianBootstrap.getRootfsDir(context);
    if (rootfs == null || !rootfs.exists()) return false;
    if (findJavaExecutable(context) == null) return false;
    return findJdtlsInfo(rootfs) != null;
  }

  private static String sanitize(String value) {
    return (value == null || value.isEmpty()) ? "root" : value.replaceAll("[^a-zA-Z0-9]+", "_");
  }

  private static LanguageServerDefinition createDefinition(
      Context context,
      String javaExecutable,
      String launcherJarName,
      String configDirName,
      String projectRoot,
      String guestBasePath) {

    String workspaceId = sanitize(projectRoot);

    File dataDir = new File(context.getCacheDir(), "jdtls-workspace/" + workspaceId);
    dataDir.mkdirs();

    File configurationDir = new File(context.getCacheDir(), "jdtls-config/" + workspaceId);
    configurationDir.mkdirs();

    String sharedConfigPath = guestBasePath + "/" + configDirName;

    List<String> args = new ArrayList<>();
    args.add("-Djdk.lang.Process.launchMechanism=FORK");
    args.add("-Djdk.xml.maxGeneralEntitySizeLimit=0");
    args.add("-Djdk.xml.totalEntitySizeLimit=0");
    args.add("-Declipse.application=org.eclipse.jdt.ls.core.id1");
    args.add("-Dosgi.bundles.defaultStartLevel=4");
    args.add("-Declipse.product=org.eclipse.jdt.ls.core.product");
    args.add("-Dlog.level=WARNING");
    args.add("-Xms256m");
    args.add("-Xmx1G");
    args.add("-XX:+UseG1GC");
    args.add("-XX:+TieredCompilation");
    args.add("-XX:TieredStopAtLevel=1");
    args.add("-Dorg.eclipse.jdt.ls.lombok.support=false");
    args.add("--add-modules=ALL-SYSTEM");
    args.add("--add-opens");
    args.add("java.base/java.util=ALL-UNNAMED");
    args.add("--add-opens");
    args.add("java.base/java.lang=ALL-UNNAMED");
    args.add("-Dosgi.checkConfiguration=false");
    args.add("-Dosgi.sharedConfiguration.area=" + sharedConfigPath);
    args.add("-Dosgi.sharedConfiguration.area.readOnly=true");
    args.add("-Dosgi.configuration.cascaded=true");
    args.add("-jar");
    args.add(guestBasePath + "/plugins/" + launcherJarName);
    args.add("-configuration");
    args.add(configurationDir.getAbsolutePath());
    args.add("-data");
    args.add(dataDir.getAbsolutePath());

    return new CustomLanguageServerDefinition(
        "java",
        workingDir -> new ProotStdioConnectionProvider(context, workingDir, javaExecutable, args),
        SERVER_NAME,
        null,
        null);
  }

  private static synchronized LspProject getOrCreateProject(
      Context context,
      String projectRoot,
      String javaExecutable,
      String launcherJarName,
      String configDirName,
      String guestBasePath) {
    LspProject project = projects.get(projectRoot);
    if (project == null) {
      project = new LspProject(projectRoot);
      project.addServerDefinition(
          createDefinition(
              context, javaExecutable, launcherJarName, configDirName, projectRoot, guestBasePath));
      projects.put(projectRoot, project);
    }
    return project;
  }

  public static LspEditor connectFile(
      Context context, String projectRoot, String filePath, CodeEditor editor) {
    String javaExecutable = findJavaExecutable(context);
    if (javaExecutable == null) {
      Log.e(TAG, "java نصب نیست. داخل ترمینال proot اجرا کن: apt install -y default-jdk");
      return null;
    }

    File rootfs = DebianBootstrap.getRootfsDir(context);
    JdtlsInfo jdtlsInfo = findJdtlsInfo(rootfs);
    if (jdtlsInfo == null) {
      Log.e(
          TAG,
          "jdtls نصب نیست یا ساختار پوشه‌ها صحیح نیست. طبق راهنمای بالای JavaServer.java توی "
              + JDTLS_HOME
              + " نصبش کن.");
      return null;
    }

    String launcherJarName = findLauncherJarName(jdtlsInfo.hostBaseDir);
    if (launcherJarName == null) {
      Log.e(TAG, "فایل launcher jar در پوشه plugins پیدا نشد.");
      return null;
    }

    String configDirName = findConfigDirName(jdtlsInfo.hostBaseDir);
    if (configDirName == null) {
      Log.e(
          TAG,
          "پوشه ی config (config_linux_arm یا config_linux) پیدا نشد. مطمئن شو tar.gz رو کامل extract کردی.");
      return null;
    }

    LspProject project =
        getOrCreateProject(
            context,
            projectRoot,
            javaExecutable,
            launcherJarName,
            configDirName,
            jdtlsInfo.guestBasePath);

    final LspEditor[] holder = new LspEditor[1];
    final CountDownLatch latch = new CountDownLatch(1);

    new Handler(Looper.getMainLooper())
        .post(
            () -> {
              try {
                LspEditor e = project.createEditor(filePath);
                var java = new JavaLanguage(context);
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
        Log.e(
            TAG, "اتصال به jdtls ناموفق بود (ممکنه فقط کند بالا اومده باشه، دوباره امتحان کن)", e);
      }
    }
    return lspEditor;
  }

  public static void disconnectFile(LspEditor lspEditor) {
    if (lspEditor == null) {
      return;
    }
    try {
      lspEditor.dispose();
    } catch (Exception e) {
      Log.e(TAG, "بستن اتصال jdtls با خطا مواجه شد", e);
    }
  }
}
