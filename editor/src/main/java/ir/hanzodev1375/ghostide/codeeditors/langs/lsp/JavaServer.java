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
 * اتصال Language Server جاوا (Eclipse JDT Language Server، معروف به jdtls) که داخل rootfsِ proot
 * اجرا می شه. دقیقاً هم ساختار PylspServer/ClangdServer، از همون ProotStdioConnectionProvider
 * استفاده می کنه؛ فقط چون jdtls یک باینری تکی نیست (یه اپلیکیشن جاوایی روی OSGi/Equinox هست)، به
 * جای اجرای مستقیم یک exe، "java" رو با -jar روی launcher jarِ jdtls اجرا می کنیم.
 *
 * <p>نصب داخل ترمینال proot (دو مرحله - هم JDK هم خودِ jdtls لازمه):
 *
 * <pre>
 *   apt update && apt install -y default-jdk curl
 *   mkdir -p ~/jdtls &amp;&amp; cd ~/jdtls
 *   curl -L -o jdtls.tar.gz https://download.eclipse.org/jdtls/snapshots/jdt-language-server-latest.tar.gz
 *   tar -xzf jdtls.tar.gz
 * </pre>
 *
 * انتظار می ره بعد از این دستورات، پوشه ی ~/jdtls شامل plugins/org.eclipse.equinox.launcher_*.jar
 * و یکی از پوشه های config_linux_arm یا config_linux باشه (همینا رو isInstalled چک می کنه). تارباله
 * ی jdtls چند تا پوشه ی config_* پلتفرم-مخصوص داره (config_linux_arm, config_linux, config_mac,
 * ...)؛ چون گوشی arm64 هست، config_linux_arm در اولویته و فقط اگه نبود میره سراغ config_linux.
 * لازم نیست دستور "jdtls --version" کار کنه - اون اسکریپت wrapper پایتونیِ داخل bin/ هست که اصلا
 * استفاده نمی کنیم؛ ما مستقیم java رو با -jar روی launcher jar صدا می زنیم.
 *
 * <p><b>محدودیت مهم و صادقانه:</b> jdt.ls برای فایل های .java خام (بدون بیلد سیستم واقعی) و برای
 * کتابخونه ی استاندارد JDK خوب کار می کنه، ولی چون اینجا نمی تونه Android Gradle Plugin رو واقعا
 * اجرا کنه (نه SDK داره نه شبکه/گریدل دیمون قابل اطمینان روی گوشی)، ایمپورت های androidx و کلاس های
 * تولیدی مثل R.java رو resolve نمی کنه - فقط JDK استاندارد و فایل های همون پروژه رو می شناسه. برای
 * resolve کامل باید بعداً یک classpath دستی (jar های SDK/AndroidX) به تعریف پروژه اضافه بشه؛ اون
 * یک کار جدا و بزرگ تره.
 *
 * <p>نکته ی دیگه: jdt.ls به خاطر cold start جی وی ام + ایندکس کردن پروژه، کندتر از clangd/pylsp بالا
 * میاد. اگه connectWithTimeoutBlocking() یه تایم اوت داخلی کوتاه داشته باشه، ممکنه اولین اتصال با
 * timeout شکست بخوره حتی وقتی همه چی درسته - دوباره امتحان کن.
 *
 * <p>نکته ی فنی: چون jdtls "خودش" هم داخل rootfs نصب شده (نه زیر پوشه ی داده ی برنامه که bind میشه
 * با همون مسیر)، مسیر jar و config باید مسیر "داخل" rootfs باشن (مثلا /root/jdtls/...)، نه
 * File.getAbsolutePath() روی آبجکت File ای که با join کردن rootfs (مسیر روی خودِ دستگاه) ساخته شده
 * - اون یه مسیر کاملاً متفاوته و اگه اشتباهی پاس داده بشه jdtls اصلاً پیدا نمی شه.
 *
 * <p>connectFile عملیات I/O سنگین انجام می ده، حتما توی ترد جدا صداش بزن، نه روی UI thread.
 */
public class JavaServer {

  private static final String TAG = "JavaServer";
  private static final String SERVER_NAME = "jdtls";
  private static final Set<String> SUPPORTED_EXTENSIONS = Collections.singleton("java");

  // مسیرهای احتمالی جاوا داخل rootfs، بعد از apt install default-jdk (یا openjdk-XX-jdk)
  private static final String[] JAVA_CANDIDATE_PATHS = {"/usr/bin/java"};

  // پوشه ای که jdt.ls توش extract شده (طبق راهنمای نصب بالای فایل) - این یک مسیر GUEST (داخل
  // rootfs) هست، نه مسیر روی خودِ دستگاه.
  private static final String JDTLS_HOME = "/root/jdtls";

  // تارباله ی jdtls چند تا پوشه ی config پلتفرم-مخصوص داره؛ چون گوشی arm64 هست config_linux_arm
  // اولویت داره، ولی اگه (مثلا روی امولاتور x86) نبود، config_linux هم fallback جواب می ده.
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

  /**
   * مسیر GUEST باینری java رو پیدا می کنه (اگه نصب نباشه null). اول candidate ثابت رو چک می کنه،
   * بعد به عنوان fallback پوشه های /usr/lib/jvm رو می گرده (اسم پوشه ی JDK بسته به نسخه فرق می کنه،
   * مثلا java-17-openjdk-arm64).
   */
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

  /**
   * اسم فایل launcher jarِ equinox رو داخل پوشه ی نصبِ jdtls پیدا می کنه (فقط اسم فایل، چون هر
   * ریلیز یک ورژن متفاوت توی اسم داره؛ مثلا org.eclipse.equinox.launcher_1.6.900.jar). فقط اسم
   * برمی گردونه، نه مسیر کامل - مسیر GUEST باید دستی با JDTLS_HOME ساخته بشه.
   */
  private static String findLauncherJarName(File rootfsHostDir) {
    File pluginsDir = new File(rootfsHostDir, JDTLS_HOME.substring(1) + "/plugins");
    File[] files = pluginsDir.exists() ? pluginsDir.listFiles() : null;
    if (files == null) return null;
    for (File f : files) {
      if (f.getName().startsWith("org.eclipse.equinox.launcher_") && f.getName().endsWith(".jar")) {
        return f.getName();
      }
    }
    return null;
  }

  /**
   * اسم پوشه ی config مناسب رو پیدا می کنه (config_linux_arm در اولویت، بعد config_linux به عنوان
   * fallback). فقط اسم پوشه برمی گردونه، نه مسیر کامل - مسیر GUEST باید دستی با JDTLS_HOME ساخته
   * بشه (دقیقا مثل findLauncherJarName).
   */
  private static String findConfigDirName(File rootfsHostDir) {
    for (String candidate : CONFIG_DIR_CANDIDATES) {
      File dir = new File(rootfsHostDir, JDTLS_HOME.substring(1) + "/" + candidate);
      if (dir.exists() && dir.isDirectory()) {
        return candidate;
      }
    }
    return null;
  }

  /** آیا هم JDK و هم jdtls نصب و آماده ن. */
  public static boolean isInstalled(Context context) {
    File rootfs = DebianBootstrap.getRootfsDir(context);
    if (rootfs == null || !rootfs.exists()) return false;
    if (findJavaExecutable(context) == null) return false;
    return findConfigDirName(rootfs) != null && findLauncherJarName(rootfs) != null;
  }

  private static String sanitize(String value) {
    return (value == null || value.isEmpty()) ? "root" : value.replaceAll("[^a-zA-Z0-9]+", "_");
  }

  /**
   * تعریف زبان سرور رو می سازه. اگه اسم/امضای CustomLanguageServerDefinition توی کتابخونه ی فعلیت
   * با اینجا فرق داشت، مثل بقیه ی *Server ها روی این متد Ctrl+کلیک بزن و پارامترها رو match کن.
   */
  private static LanguageServerDefinition createDefinition(
      Context context,
      String javaExecutable,
      String launcherJarName,
      String configDirName,
      String projectRoot) {
    // این پوشه زیر cache خودِ برنامه است، پس همون مسیر (بدون تغییر) هم روی دستگاه و هم داخل
    // proot دیده می شه (چون کل پوشه ی داده ی برنامه با یک -b bind میشه، نه remap).
    File dataDir = new File(context.getCacheDir(), "jdtls-workspace/" + sanitize(projectRoot));
    dataDir.mkdirs();

    List<String> args = new ArrayList<>();
    args.add("-Declipse.application=org.eclipse.jdt.ls.core.id1");
    args.add("-Dosgi.bundles.defaultStartLevel=4");
    args.add("-Declipse.product=org.eclipse.jdt.ls.core.product");
    args.add("-Dlog.level=ALL");
    args.add("-Xmx1G");
    args.add("--add-modules=ALL-SYSTEM");
    args.add("--add-opens");
    args.add("java.base/java.util=ALL-UNNAMED");
    args.add("--add-opens");
    args.add("java.base/java.lang=ALL-UNNAMED");
    args.add("-jar");
    args.add(JDTLS_HOME + "/plugins/" + launcherJarName); // مسیر GUEST، نه مسیر روی دستگاه
    args.add("-configuration");
    args.add(JDTLS_HOME + "/" + configDirName); // مسیر GUEST (config_linux_arm یا config_linux)
    args.add("-data");
    args.add(dataDir.getAbsolutePath()); // این یکی مسیر هم-دستگاه-هم-guest است (بالا توضیح داده شد)

    return new CustomLanguageServerDefinition(
        "java",
        workingDir ->
            new ProotStdioConnectionProvider(context, workingDir, javaExecutable, args),
        SERVER_NAME,
        null, // extensionsOverride
        null // expectedCapabilitiesOverride
        );
  }

  private static synchronized LspProject getOrCreateProject(
      Context context,
      String projectRoot,
      String javaExecutable,
      String launcherJarName,
      String configDirName) {
    LspProject project = projects.get(projectRoot);
    if (project == null) {
      project = new LspProject(projectRoot);
      project.addServerDefinition(
          createDefinition(context, javaExecutable, launcherJarName, configDirName, projectRoot));
      projects.put(projectRoot, project);
    }
    return project;
  }

  /**
   * فایل جاوای باز شده رو به jdtls وصل می کنه. حتما توی ترد جدا صدا بزن.
   *
   * @param context Context برنامه
   * @param projectRoot ریشه ی پروژه روی خودِ دستگاه (پوشه ای که GhostIDE بازش کرده)
   * @param filePath مسیر کامل فایل .java روی دستگاه (نه مسیر داخل proot)
   * @param editor ویجت CodeEditor که فایل توش باز شده
   * @return LspEditor ساخته شده (برای disconnectFile موقع بستن تب نگهش دار)، یا null اگه JDK/jdtls
   *     نصب نباشن یا اتصال شکست بخوره
   */
  public static LspEditor connectFile(
      Context context, String projectRoot, String filePath, CodeEditor editor) {
    String javaExecutable = findJavaExecutable(context);
    if (javaExecutable == null) {
      Log.e(TAG, "java نصب نیست. داخل ترمینال proot اجرا کن: apt install -y default-jdk");
      return null;
    }

    File rootfs = DebianBootstrap.getRootfsDir(context);
    String launcherJarName = rootfs != null ? findLauncherJarName(rootfs) : null;
    if (launcherJarName == null) {
      Log.e(
          TAG,
          "jdtls نصب نیست یا launcher jar پیدا نشد. طبق راهنمای بالای JavaServer.java توی "
              + JDTLS_HOME
              + " نصبش کن.");
      return null;
    }

    String configDirName = findConfigDirName(rootfs);
    if (configDirName == null) {
      Log.e(
          TAG,
          "پوشه ی config (config_linux_arm یا config_linux) توی "
              + JDTLS_HOME
              + " پیدا نشد. مطمئن شو tar.gz رو کامل extract کردی.");
      return null;
    }

    LspProject project =
        getOrCreateProject(context, projectRoot, javaExecutable, launcherJarName, configDirName);

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
        Log.e(TAG, "اتصال به jdtls ناموفق بود (ممکنه فقط کند بالا اومده باشه، دوباره امتحان کن)", e);
      }
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
      Log.e(TAG, "بستن اتصال jdtls با خطا مواجه شد", e);
    }
  }
}
