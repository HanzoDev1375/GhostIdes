package ir.hanzodev1375.ghostide.codeeditors.langs.lsp;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import ir.hanzodev1375.ghostide.codeeditors.langs.formatHelp.DebianBootstrap;

/**
 * پیدا کردن classpath واقعی یه پروژهی Android/Gradle: سورسروتها (src/main/java)، android.jar
 * (بالاترین API level نصبشده)، و library ها. برای پروژههای گریدلی، کلاسپث از خودِ گریدل پرسیده میشه
 * (init script ای که compileClasspath رو چاپ میکنه) و نتیجه کش میشه؛ اگه گریدل در دسترس نباشه یا
 * fail بشه، همون اسکن دستیِ کش گریدل/libs بهعنوان fallback انجام میشه. هم JavaServer (LSP) هم
 * CodeRuner (کامپایل/اجرا) از همین استفاده میکنن تا رفتارشون یکی باشه.
 */
public final class AndroidClasspathResolver {

  private AndroidClasspathResolver() {}

  private static final Set<String> SKIP_DIR_NAMES =
      new HashSet<>(
          Arrays.asList(
              ".git",
              ".gradle",
              ".idea",
              "build",
              "bin",
              ".settings",
              ".metadata",
              "node_modules",
              ".jdtls-meta",
              ".gradle-cache",
              ".ghostide_build"));
  private static final int MAX_SCAN_DEPTH = 10;

  // چیدمانهای سورسروت که به ترتیب اولویت چک میشن (مثل Android Studio): اول چیدمان
  // گِرِدل (src/main/java)، بعد src/java، بعد src/main/kotlin، و در نهایت src/ و java/
  // برای پروژههای Java سادهی غیر گِرِدل.
  private static final String[] SOURCE_ROOT_CANDIDATES = {
    "src/main/java", "src/java", "src/main/kotlin", "src", "java"
  };

  // ریشههای احتمالی SDK نصبشده (نسبت به rootfs). فقط زیرپوشهی platforms/android-*
  // چک میشه، نه کل SDK (که شامل build-tools/sources/system-images/emulator و غیره هم میشه).
  private static final String[] ANDROID_SDK_ROOT_CANDIDATES = {
    "root/android-sdk", "opt/android-sdk", "usr/lib/android-sdk"
  };

  private static final String[] GRADLE_BUILD_FILE_NAMES = {"build.gradle", "build.gradle.kts"};
  private static final String[] GRADLE_PROJECT_MARKERS = {
    "settings.gradle", "settings.gradle.kts", "gradlew"
  };

  /** حداکثر انتظار برای کوئریِ گریدل؛ بعدش fallback به اسکن دستی. */
  private static final long GRADLE_TIMEOUT_SECONDS = 90;

  private static final String GHOST_CP_PREFIX = "GHOSTCP:";
  private static final String GRADLE_TASK_NAME = "ghostidePrintClasspath";
  private static final String LOG_TAG = "GhostClasspath";

  public static List<File> findJavaSourceRoots(File root) {
    List<File> found = new ArrayList<>();
    scanForSourceRoots(root, 0, found);
    if (found.isEmpty()) {
      found.add(root);
    }
    return found;
  }

  private static void scanForSourceRoots(File dir, int depth, List<File> found) {
    if (dir == null || depth > MAX_SCAN_DEPTH || !dir.isDirectory()) return;
    if (depth > 0 && SKIP_DIR_NAMES.contains(dir.getName())) return;

    for (String candidate : SOURCE_ROOT_CANDIDATES) {
      File src = new File(dir, candidate);
      if (src.isDirectory()) {
        found.add(src);
        break;
      }
    }

    File[] children = dir.listFiles(File::isDirectory);
    if (children == null) return;
    for (File child : children) {
      scanForSourceRoots(child, depth + 1, found);
    }
  }

  /**
   * ریشهی واقعیِ پروژه رو با بالا رفتن از فایلِ بازشده پیدا میکنه — نه صرفاً پوشهی والدِ فایل.
   * اول دنبال نشونههای گریدل (build.gradle / settings.gradle / gradlew) میگرده؛ اگه پیدا نشد،
   * بالاتر از الگوی src/main/java یا src/main/kotlin میره تا ریشهی ماژول (پدربزرگِ src) رو پیدا
   * کنه؛ اگه هیچکدوم پیدا نشد (مثلاً یه فایل جاوای تکی بدون ساختار پروژه)، پوشهی خودِ فایل
   * برگردونده میشه. بدون این متد، جیدیتیال «ریشهی پروژه» رو همون پوشهی بستهی فایل درنظر
   * میگیره و برای هر فایلی expected package رو خالی محاسبه میکنه.
   */
  public static File findProjectRoot(File file) {
    File dir = file.isDirectory() ? file : file.getParentFile();
    if (dir == null) return file;

    File gradleRoot = findGradleProjectRoot(dir);
    if (gradleRoot != null) return gradleRoot;

    File cursor = dir;
    while (cursor != null) {
      String name = cursor.getName();
      if (name.equals("java") || name.equals("kotlin")) {
        File parent = cursor.getParentFile();
        File srcDir = parent != null ? parent.getParentFile() : null;
        if (srcDir != null && srcDir.getName().equals("src") && srcDir.getParentFile() != null) {
          return srcDir.getParentFile(); // ریشهی ماژول، یعنی پدربزرگِ src
        }
      }
      cursor = cursor.getParentFile();
    }
    return dir;
  }

  public static List<File> findLibraryJars(Context context, File projectRoot) {
    List<File> jars = new ArrayList<>();
    Set<String> seenNames = new HashSet<>();
    File aarCacheDir = new File(context.getCacheDir(), "aar-extracted");

    collectJarsFromLibsDirs(projectRoot, 0, jars, seenNames, aarCacheDir);

    File rootfs = DebianBootstrap.getRootfsDir(context);
    File gradleRoot = findGradleProjectRoot(projectRoot);

    if (gradleRoot != null && rootfs != null) {
      // کلاسپث واقعی رو از خودِ گریدل بپرس (کششده بر اساس mtime فایلهای build).
      List<File> gradleJars = queryGradleClasspath(context, rootfs, gradleRoot, aarCacheDir);
      if (gradleJars != null && !gradleJars.isEmpty()) {
        for (File jar : gradleJars) {
          if (seenNames.add(jar.getAbsolutePath())) jars.add(jar);
        }
      } else {
        // گریدل fail/تایماوت شد؛ fallback به اسکن کل کش گریدل.
        Log.w(LOG_TAG, "gradle classpath query failed; falling back to manual cache scan");
        File gradleCache = new File(rootfs, "root/.gradle/caches/modules-2/files-2.1");
        collectJarsFromGradleCache(gradleCache, jars, seenNames, aarCacheDir);
      }
    } else if (rootfs != null) {
      File gradleCache = new File(rootfs, "root/.gradle/caches/modules-2/files-2.1");
      collectJarsFromGradleCache(gradleCache, jars, seenNames, aarCacheDir);
    }

    File androidJar = findAndroidJar(rootfs);
    if (androidJar != null) {
      boolean alreadyOnClasspath = false;
      for (File jar : jars) {
        if (jar.getName().equals("android.jar")) {
          alreadyOnClasspath = true;
          break;
        }
      }
      if (!alreadyOnClasspath && seenNames.add("android.jar")) {
        jars.add(androidJar);
      }
    }

    return jars;
  }

  /**
   * ریشهی واقعیِ پروژهی گریدل رو با بالا رفتن پیدا میکنه. یه settings.gradle یا gradlew همیشه
   * نشونهی قطعیِ ریشهست و فوراً برگردونده میشه؛ یه build.gradleی تنها (بدون settings.gradle
   * بالاترش) فقط بهعنوان fallback نگه داشته میشه، چون معمولاً متعلق به یه ماژول (مثل app/) هست
   * نه ریشهی واقعی پروژه — قبلاً build.gradle در همون سطح اول باعث میشد رو ماژول متوقف بشه و
   * هیچوقت به ریشهای که local.properties (مسیر SDK) و settings.gradle توشه نرسه.
   */
  public static File findGradleProjectRoot(File dir) {
    File nearestBuildFileRoot = null;
    while (dir != null) {
      for (String marker : GRADLE_PROJECT_MARKERS) {
        if (new File(dir, marker).exists()) return dir;
      }
      if (nearestBuildFileRoot == null) {
        for (String name : GRADLE_BUILD_FILE_NAMES) {
          if (new File(dir, name).isFile()) {
            nearestBuildFileRoot = dir;
            break;
          }
        }
      }
      dir = dir.getParentFile();
    }
    return nearestBuildFileRoot;
  }

  /**
   * کلاسپث واقعی پروژه رو از خودِ گریدل میپرسه (compileClasspath سورستها + configurations) و نتیجه
   * رو بر اساس mtime فایلهای build کش میکنه. اگه گریدل نصب نباشه، fail بشه یا تایماوت بده، null
   * برمیگردونه تا caller به اسکن دستی fallback کنه.
   */
  private static List<File> queryGradleClasspath(
      Context context, File rootfs, File gradleRoot, File aarCacheDir) {
    if (!new File(rootfs, "bin/sh").exists()) return null;
    if (findGradleExecutable(rootfs) == null) return null;

    File cacheDir = new File(context.getCacheDir(), "gradle-classpath-cache");
    File cacheFile = new File(cacheDir, cacheKey(gradleRoot) + ".txt");
    if (cacheFile.isFile()) {
      List<File> cached = readCachedJars(cacheFile);
      if (cached != null) return cached;
    }

    File initScript = ensureGradleInitScript(cacheDir);
    if (initScript == null) return null;

    List<File> jars = runGradleClasspathQuery(context, rootfs, gradleRoot, initScript, aarCacheDir);
    if (jars == null || jars.isEmpty()) {
      initScript.delete();
      return null;
    }
    writeCachedJars(cacheFile, jars);
    return jars;
  }

  /** init scriptی که compileClasspath هر پروژه/ماژول رو با پیشوند GHOSTCP چاپ میکنه. */
  private static File ensureGradleInitScript(File cacheDir) {
    File init = new File(cacheDir, "init.gradle");
    if (init.isFile()) return init;
    try {
      cacheDir.mkdirs();
      String script =
          """
          gradle.projectsLoaded {
            rootProject.tasks.register('ghostidePrintClasspath') {
              doLast {
                rootProject.allprojects.each { p ->
                  def files = []
                  def ss = p.extensions.findByName('sourceSets')
                  if (ss != null) {
                    try { files += ss.getByName('main').compileClasspath.files } catch (Exception ignored) {}
                  }
                  def cfg = p.configurations.findByName('compileClasspath')
                  if (cfg != null) {
                    try { files += cfg.files } catch (Exception ignored) {}
                  }
                  files = files.findAll { it.isFile() && (it.name.endsWith('.jar') || it.name.endsWith('.aar')) }
                      .collect { it.absolutePath }
                      .unique()
                  files.each { println 'GHOSTCP:' + it }
                }
              }
            }
          }
          """;
      try (FileOutputStream out = new FileOutputStream(init)) {
        out.write(script.getBytes(StandardCharsets.UTF_8));
      }
      return init;
    } catch (Exception e) {
      return null;
    }
  }

  /** یه بار gradle رو زیر proot اجرا میکنه تا مسیرهای compileClasspath رو چاپ کنه. */
  private static List<File> runGradleClasspathQuery(
      Context context, File rootfs, File gradleRoot, File initScript, File aarCacheDir) {
    String nativeLibDir = context.getApplicationInfo().nativeLibraryDir;
    File prootBinary = new File(nativeLibDir, "libproot.so");
    File loaderBinary = new File(nativeLibDir, "libloader.so");
    if (!prootBinary.exists() || !loaderBinary.exists()) return null;

    String gradleExecutable = findGradleExecutable(rootfs);
    if (gradleExecutable == null) return null;
    String sdkGuestPath = findAndroidSdkGuestPath(rootfs);

    List<String> cmd = new ArrayList<>();
    cmd.add(prootBinary.getAbsolutePath());
    cmd.add("--kill-on-exit");
    cmd.add("-0");
    cmd.add("--link2symlink");
    cmd.add("-r");
    cmd.add(rootfs.getAbsolutePath());
    cmd.add("-b");
    cmd.add("/dev");
    cmd.add("-b");
    cmd.add("/proc");
    cmd.add("-b");
    cmd.add("/sys");
    // دایرکتوری private اپ رو بایند کن تا init script (که تو cache اپه) و کلاسپث قابل مشاهده باشه.
    cmd.add("-b");
    cmd.add(context.getFilesDir().getParentFile().getAbsolutePath());
    cmd.add("-b");
    cmd.add(gradleRoot.getAbsolutePath());
    cmd.add("-w");
    cmd.add(gradleRoot.getAbsolutePath());
    cmd.add(gradleExecutable);
    cmd.add("-q");
    cmd.add("--no-daemon");
    cmd.add("--console=plain");
    cmd.add("--init-script");
    cmd.add(initScript.getAbsolutePath());
    cmd.add(GRADLE_TASK_NAME);

    ProcessBuilder pb = new ProcessBuilder(cmd);
    pb.redirectErrorStream(true);
    pb.environment().clear();
    pb.environment().put("PROOT_TMP_DIR", context.getCacheDir().getAbsolutePath() + "/proot-tmp");
    pb.environment().put("PROOT_LOADER", loaderBinary.getAbsolutePath());
    pb.environment().put("LD_LIBRARY_PATH", nativeLibDir);
    pb.environment()
        .put("PATH", "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/system/bin");
    pb.environment().put("HOME", "/root");
    pb.environment().put("LANG", "C.UTF-8");
    pb.environment().put("LC_ALL", "C.UTF-8");
    if (sdkGuestPath != null) {
      pb.environment().put("ANDROID_HOME", sdkGuestPath);
      pb.environment().put("ANDROID_SDK_ROOT", sdkGuestPath);
    }

    Log.i(LOG_TAG, "gradle query: " + gradleRoot.getAbsolutePath());

    try {
      Process process = pb.start();
      List<File> jars = new ArrayList<>();
      String line;
      try (BufferedReader reader =
          new BufferedReader(
              new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
        while ((line = reader.readLine()) != null) {
          if (line.startsWith(GHOST_CP_PREFIX)) {
            File hostFile = toHostFile(rootfs, line.substring(GHOST_CP_PREFIX.length()));
            if (hostFile.getName().endsWith(".aar")) {
              File extracted = extractClassesJarFromAar(hostFile, aarCacheDir);
              if (extracted != null) jars.add(extracted);
            } else if (hostFile.getName().endsWith(".jar")) {
              jars.add(hostFile);
            }
          } else {
            Log.d(LOG_TAG, line);
          }
        }
      }
      boolean finished = process.waitFor(GRADLE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
      if (!finished) {
        Log.w(LOG_TAG, "gradle timed out after " + GRADLE_TIMEOUT_SECONDS + "s");
        process.destroyForcibly();
        return null;
      }
      return jars;
    } catch (Exception e) {
      Log.w(LOG_TAG, "gradle query failed", e);
      return null;
    }
  }

  private static String findGradleExecutable(File rootfs) {
    for (String guestPath :
        new String[] {"/usr/bin/gradle", "/usr/local/bin/gradle", "/bin/gradle"}) {
      if (new File(rootfs, guestPath.substring(1)).isFile()) return guestPath;
    }
    return null;
  }

  /** مسیر چاپشدهی guest-side رو به مسیر host روی اندروید نگاشت میکنه (rootfs یا بایند مستقیم). */
  private static File toHostFile(File rootfs, String guestPath) {
    File fromRoot = new File(rootfs, guestPath);
    if (fromRoot.isFile()) return fromRoot;
    File direct = new File(guestPath);
    if (direct.isFile()) return direct;
    return fromRoot;
  }

  private static String cacheKey(File gradleRoot) {
    StringBuilder sb = new StringBuilder(gradleRoot.getAbsolutePath());
    for (String name : GRADLE_BUILD_FILE_NAMES) {
      File f = new File(gradleRoot, name);
      if (f.isFile()) {
        sb.append('|')
            .append(name)
            .append(':')
            .append(f.lastModified())
            .append(':')
            .append(f.length());
      }
    }
    for (String marker : GRADLE_PROJECT_MARKERS) {
      File f = new File(gradleRoot, marker);
      if (f.exists()) sb.append('|').append(marker).append(':').append(f.lastModified());
    }
    File props = new File(gradleRoot, "gradle.properties");
    if (props.isFile()) sb.append("|gradle.properties:").append(props.lastModified());
    return sha256(sb.toString());
  }

  private static String sha256(String input) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
      StringBuilder hex = new StringBuilder();
      for (byte b : hash) {
        hex.append(String.format("%02x", b & 0xff));
      }
      return hex.toString();
    } catch (Exception e) {
      return Integer.toHexString(input.hashCode());
    }
  }

  private static List<File> readCachedJars(File cacheFile) {
    List<File> jars = new ArrayList<>();
    try (BufferedReader reader =
        new BufferedReader(
            new InputStreamReader(new FileInputStream(cacheFile), StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.isEmpty()) continue;
        File f = new File(line);
        if (f.isFile()) jars.add(f);
      }
    } catch (Exception e) {
      return null;
    }
    return jars.isEmpty() ? null : jars;
  }

  private static void writeCachedJars(File cacheFile, List<File> jars) {
    try {
      StringBuilder sb = new StringBuilder();
      for (File jar : jars) sb.append(jar.getAbsolutePath()).append('\n');
      cacheFile.getParentFile().mkdirs();
      try (FileOutputStream out = new FileOutputStream(cacheFile)) {
        out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
      }
    } catch (Exception e) {
      // کش اختیاریه؛ بدون اون هم همهچی درست کار میکنه
    }
  }

  /** فقط زیر platforms/android-* میگرده و بالاترین API level موجود رو برمیگردونه. */
  public static File findAndroidJar(File rootfs) {
    if (rootfs == null) return null;
    File best = null;
    int bestApi = Integer.MIN_VALUE;
    for (String sdkRootCandidate : ANDROID_SDK_ROOT_CANDIDATES) {
      File platformsDir = new File(rootfs, sdkRootCandidate + "/platforms");
      File[] platforms =
          platformsDir.isDirectory() ? platformsDir.listFiles(File::isDirectory) : null;
      if (platforms == null) continue;
      for (File platform : platforms) {
        File jar = new File(platform, "android.jar");
        if (!jar.isFile()) continue;
        int api = parseApiLevel(platform.getName());
        if (api > bestApi) {
          bestApi = api;
          best = jar;
        }
      }
    }
    return best;
  }

  /**
   * مسیر guest-side ریشهی SDK (مثلاً "/root/android-sdk")، برای export ANDROID_HOME تو شل proot.
   */
  public static String findAndroidSdkGuestPath(File rootfs) {
    if (rootfs == null) return null;
    for (String candidate : ANDROID_SDK_ROOT_CANDIDATES) {
      File platformsDir = new File(rootfs, candidate + "/platforms");
      if (platformsDir.isDirectory()) {
        return "/" + candidate;
      }
    }
    return null;
  }

  private static int parseApiLevel(String platformDirName) {
    if (platformDirName == null || !platformDirName.startsWith("android-")) return -1;
    try {
      return Integer.parseInt(platformDirName.substring("android-".length()));
    } catch (NumberFormatException e) {
      return 0; // نسخهی preview/کد-نامی؛ رد نکن ولی اولویتش از یه نسخهی عددی کمتره
    }
  }

  private static void collectJarsFromLibsDirs(
      File dir, int depth, List<File> jars, Set<String> seenNames, File aarCacheDir) {
    if (dir == null || depth > MAX_SCAN_DEPTH || !dir.isDirectory()) return;
    if (SKIP_DIR_NAMES.contains(dir.getName())) return;

    File[] children = dir.listFiles();
    if (children == null) return;
    for (File child : children) {
      if (child.isDirectory()) {
        collectJarsFromLibsDirs(child, depth + 1, jars, seenNames, aarCacheDir);
      } else if ("libs".equals(dir.getName())) {
        if (child.getName().endsWith(".jar")) {
          if (seenNames.add(child.getName())) jars.add(child);
        } else if (child.getName().endsWith(".aar")) {
          File extracted = extractClassesJarFromAar(child, aarCacheDir);
          if (extracted != null && seenNames.add(child.getName() + "!classes.jar")) {
            jars.add(extracted);
          }
        }
      }
    }
  }

  private static void collectJarsFromGradleCache(
      File cacheRoot, List<File> jars, Set<String> seenNames, File aarCacheDir) {
    if (cacheRoot == null || !cacheRoot.isDirectory()) return;
    File[] groups = cacheRoot.listFiles(File::isDirectory);
    if (groups == null) return;
    for (File group : groups) {
      File[] artifacts = group.listFiles(File::isDirectory);
      if (artifacts == null) continue;
      for (File artifact : artifacts) {
        File[] versions = artifact.listFiles(File::isDirectory);
        if (versions == null) continue;
        for (File version : versions) {
          File[] hashes = version.listFiles(File::isDirectory);
          if (hashes == null) continue;
          for (File hash : hashes) {
            File[] files = hash.listFiles();
            if (files == null) continue;
            for (File f : files) {
              String n = f.getName();
              String key = artifact.getName() + "/" + version.getName() + "/" + n;
              if (n.endsWith(".jar")
                  && !n.endsWith("-sources.jar")
                  && !n.endsWith("-javadoc.jar")) {
                if (seenNames.add(key)) jars.add(f);
              } else if (n.endsWith(".aar")) {
                File extracted = extractClassesJarFromAar(f, aarCacheDir);
                if (extracted != null && seenNames.add(key + "!classes.jar")) {
                  jars.add(extracted);
                }
              }
            }
          }
        }
      }
    }
  }

  /**
   * classes.jar رو از داخل یه .aar (که خودش یه zipه) استخراج میکنه و تو aarCacheDir کش میکنه، تا هر
   * بار مجبور به extract دوباره نباشیم. اگه .aar خالی از کلاس بود (فقط ریسورس)، null برمیگردونه.
   */
  private static File extractClassesJarFromAar(File aarFile, File aarCacheDir) {
    try {
      aarCacheDir.mkdirs();
      String cacheName = aarFile.getAbsolutePath().replaceAll("[^a-zA-Z0-9]+", "_") + ".jar";
      File extracted = new File(aarCacheDir, cacheName);
      if (extracted.isFile() && extracted.lastModified() >= aarFile.lastModified()) {
        return extracted;
      }
      try (ZipFile zip = new ZipFile(aarFile)) {
        ZipEntry entry = zip.getEntry("classes.jar");
        if (entry == null) return null;
        try (InputStream in = zip.getInputStream(entry);
            FileOutputStream out = new FileOutputStream(extracted)) {
          byte[] buffer = new byte[8192];
          int read;
          while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
          }
        }
      }
      return extracted;
    } catch (Exception e) {
      return null;
    }
  }

  public static String relativize(File base, File target) {
    String basePath = base.getAbsolutePath();
    String targetPath = target.getAbsolutePath();
    if (targetPath.startsWith(basePath)) {
      String rel = targetPath.substring(basePath.length());
      if (rel.startsWith(File.separator)) rel = rel.substring(1);
      return rel.isEmpty() ? "." : rel;
    }
    return targetPath;
  }

  /**
   * مسیر یه jar رو از دید داخلِ proot (guest) محاسبه میکنه، نه از دید اندروید (host). چون jdtls
   * با «-r rootfs» اجرا میشه، ریشهی «/» براش همون rootfsه؛ پس jarهایی که از خودِ rootfs میان
   * (android.jar، کش گریدل) باید پیشوند rootfs ازشون حذف بشه، وگرنه jdtls دنبال یه مسیر
   * تودرتوی غیرواقعی (rootfs-در-rootfs) میگرده و پیدا نمیکنه — دقیقاً همون چیزی که باعث
   * میشد android.jar و کل کش گریدل resolve نشن با اینکه واقعاً رو دیسک بودن. jarهایی که
   * بیرون rootfsان (مثل app/libs یا کش خودِ اپ) با «-b» عیناً به همون مسیر bind میشن، پس
   * مسیر host براشون از قبل هم مسیر معتبر داخل guest هست و دستنخورده برمیگرده.
   */
  public static String toGuestLibraryPath(File rootfs, File jar) {
    if (rootfs != null) {
      String rootfsPath = rootfs.getAbsolutePath();
      String jarPath = jar.getAbsolutePath();
      if (jarPath.startsWith(rootfsPath)) {
        String rel = jarPath.substring(rootfsPath.length());
        if (!rel.startsWith("/")) rel = "/" + rel;
        return rel;
      }
    }
    return jar.getAbsolutePath();
  }

  /** jarها رو با جداکنندهی کلاسپث لینوکس (":") به هم میچسبونه، برای استفاده تو دستور شل. */
  public static String joinClasspath(List<File> jars) {
    StringBuilder sb = new StringBuilder();
    for (File jar : jars) {
      if (sb.length() > 0) sb.append(':');
      sb.append(jar.getAbsolutePath());
    }
    return sb.toString();
  }
}
