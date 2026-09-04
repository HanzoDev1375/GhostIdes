package ir.hanzodev1375.ghostide.project;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.blankj.utilcode.util.FileIOUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ModuleCreator {

  private DslType type = DslType.GROOVY;
  private final Context context;
  private final Handler mainHandler = new Handler(Looper.getMainLooper());
  private List<LibraryManager.Library> libraries = Collections.emptyList();
  private List<String> selectedModules = Collections.emptyList();

  public interface OnModuleResult {
    void onSuccess(String modulePath);

    void onError(String message);
  }

  public ModuleCreator(Context context) {
    this.context = context;
  }

  public void setDslType(DslType type) {
    this.type = type;
  }

  public void setLibraries(List<LibraryManager.Library> libraries) {
    this.libraries = libraries != null ? libraries : Collections.emptyList();
  }

  public void setSelectedModules(List<String> selectedModules) {
    this.selectedModules = selectedModules != null ? selectedModules : Collections.emptyList();
  }

  public void create(
      String projectRootPath, String moduleName, String modulePackage, OnModuleResult callback) {
    new Thread(
            () -> {
              try {
                String result = buildModule(projectRootPath, moduleName, modulePackage);
                mainHandler.post(() -> callback.onSuccess(result));
              } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
              }
            })
        .start();
  }

  private String buildModule(String rootPath, String moduleName, String modulePackage)
      throws IOException {
    if (moduleName == null || !moduleName.matches("[a-zA-Z][a-zA-Z0-9_-]*")) {
      throw new IOException(
          "Invalid module name \"" + moduleName + "\". Use letters, digits, - or _.");
    }
    if (modulePackage == null || !isValidPackage(modulePackage)) {
      throw new IOException(
          "Invalid package \"" + modulePackage + "\". Example: com.example.mylib");
    }

    File root = new File(rootPath);
    boolean useKts = (type == DslType.KOTLIN);

    File settingsFile = new File(root, useKts ? "settings.gradle.kts" : "settings.gradle");
    if (!settingsFile.exists()) {
      settingsFile = new File(root, useKts ? "settings.gradle" : "settings.gradle.kts");
      if (!settingsFile.exists()) {
        throw new IOException(
            "No settings.gradle or settings.gradle.kts found in project root: " + rootPath);
      }
    }

    boolean hasVersionCatalog = new File(root, "gradle/libs.versions.toml").exists();
    File moduleDir = new File(root, moduleName);
    if (moduleDir.exists()) {
      throw new IOException("Module \"" + moduleName + "\" already exists.");
    }

    String pkgPath = modulePackage.replace('.', '/');
    new File(moduleDir, "src/main/java/" + pkgPath).mkdirs();
    new File(moduleDir, "src/main/res").mkdirs();
    new File(moduleDir, "src/test/java/" + pkgPath).mkdirs();
    new File(moduleDir, "src/androidTest/java/" + pkgPath).mkdirs();

    writeBuildGradle(moduleDir, modulePackage, useKts, hasVersionCatalog);
    writeAndroidManifest(moduleDir);
    writeProguardFiles(moduleDir);
    writeGitignore(moduleDir);
    writeSampleClass(moduleDir, pkgPath, moduleName, modulePackage);
    registerInSettings(settingsFile, moduleName, useKts);

    boolean appFound = addDependencyToApp(root, moduleName);
    if (!appFound) {
      throw new IOException(
          "Module created, but no app/build.gradle found. Add implementation project(':') manually.");
    }

    if (!libraries.isEmpty()) {
      LibraryManager.addLibraries(root, moduleDir, useKts, libraries);
    }

    if (!selectedModules.isEmpty()) {
      addModuleDependencies(root, moduleName, useKts, selectedModules);
    }

    return moduleDir.getAbsolutePath();
  }

  private void writeBuildGradle(
      File moduleDir, String modulePackage, boolean useKts, boolean hasVersionCatalog)
      throws IOException {
    String fileName = useKts ? "build.gradle.kts" : "build.gradle";
    String content;

    if (useKts) {
      if (hasVersionCatalog) {
        content =
            "plugins {\n"
                + "    alias(libs.plugins.android.library)\n"
                + "}\n\n"
                + "android {\n"
                + "    namespace = \""
                + modulePackage
                + "\"\n"
                + "    compileSdk {\n"
                + "        version = release(36)\n"
                + "    }\n\n"
                + "    defaultConfig {\n"
                + "        minSdk = 21\n"
                + "        consumerProguardFiles(\"consumer-rules.pro\")\n"
                + "    }\n\n"
                + "    compileOptions {\n"
                + "        sourceCompatibility = JavaVersion.VERSION_17\n"
                + "        targetCompatibility = JavaVersion.VERSION_17\n"
                + "    }\n"
                + "}\n\n"
                + "dependencies {\n"
                + "    implementation(libs.androidx.appcompat)\n"
                + "    implementation(libs.material)\n"
                + "}\n";
      } else {
        content =
            "plugins {\n"
                + "    id(\"com.android.library\")\n"
                + "}\n\n"
                + "android {\n"
                + "    namespace = \""
                + modulePackage
                + "\"\n"
                + "    compileSdk = 36\n\n"
                + "    defaultConfig {\n"
                + "        minSdk = 26\n"
                + "        consumerProguardFiles(\"consumer-rules.pro\")\n"
                + "    }\n\n"
                + "    compileOptions {\n"
                + "        sourceCompatibility = JavaVersion.VERSION_17\n"
                + "        targetCompatibility = JavaVersion.VERSION_17\n"
                + "    }\n"
                + "}\n\n"
                + "dependencies {\n"
                + "    implementation(\"androidx.appcompat:appcompat:1.7.0\")\n"
                + "    implementation(\"com.google.android.material:material:1.12.0\")\n"
                + "}\n";
      }
    } else {
      if (hasVersionCatalog) {
        content =
            "plugins {\n"
                + "    alias(libs.plugins.android.library)\n"
                + "}\n\n"
                + "android {\n"
                + "    namespace '"
                + modulePackage
                + "'\n"
                + "    compileSdk {\n"
                + "        version = release(36)\n"
                + "    }\n\n"
                + "    defaultConfig {\n"
                + "        minSdk 21\n"
                + "        consumerProguardFiles \"consumer-rules.pro\"\n"
                + "    }\n\n"
                + "    compileOptions {\n"
                + "        sourceCompatibility = JavaVersion.VERSION_17\n"
                + "        targetCompatibility = JavaVersion.VERSION_17\n"
                + "    }\n"
                + "}\n\n"
                + "dependencies {\n"
                + "    implementation libs.androidx.appcompat\n"
                + "    implementation libs.material\n"
                + "}\n";
      } else {
        content =
            "plugins {\n"
                + "    id 'com.android.library'\n"
                + "}\n\n"
                + "android {\n"
                + "    namespace '"
                + modulePackage
                + "'\n"
                + "    compileSdk 36\n\n"
                + "    defaultConfig {\n"
                + "        minSdk 26\n"
                + "        consumerProguardFiles \"consumer-rules.pro\"\n"
                + "    }\n\n"
                + "    compileOptions {\n"
                + "        sourceCompatibility = JavaVersion.VERSION_17\n"
                + "        targetCompatibility = JavaVersion.VERSION_17\n"
                + "    }\n"
                + "}\n\n"
                + "dependencies {\n"
                + "    implementation 'androidx.appcompat:appcompat:1.7.0'\n"
                + "    implementation 'com.google.android.material:material:1.12.0'\n"
                + "}\n";
      }
    }

    writeRaw(content, new File(moduleDir, fileName));
  }

  private void writeAndroidManifest(File moduleDir) throws IOException {
    writeRaw(
        "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">\n\n</manifest>\n",
        new File(moduleDir, "src/main/AndroidManifest.xml"));
  }

  private void writeProguardFiles(File moduleDir) throws IOException {
    writeRaw(
        "# Consumer ProGuard rules for this module.\n", new File(moduleDir, "consumer-rules.pro"));
    writeRaw(
        "# Add project specific ProGuard rules here.\n", new File(moduleDir, "proguard-rules.pro"));
  }

  private void writeGitignore(File moduleDir) throws IOException {
    writeRaw("/build\n", new File(moduleDir, ".gitignore"));
  }

  private void writeSampleClass(
      File moduleDir, String pkgPath, String moduleName, String modulePackage) throws IOException {
    String className = toClassName(moduleName);
    String content =
        "package "
            + modulePackage
            + ";\n\n"
            + "public class "
            + className
            + " {\n\n"
            + "    private static "
            + className
            + " instance;\n\n"
            + "    private "
            + className
            + "() {}\n\n"
            + "    public static "
            + className
            + " getInstance() {\n"
            + "        if (instance == null) {\n"
            + "            instance = new "
            + className
            + "();\n"
            + "        }\n"
            + "        return instance;\n"
            + "    }\n\n"
            + "    public String hello() {\n"
            + "        return \"Hello from "
            + className
            + "!\";\n"
            + "    }\n\n"
            + "}\n";
    writeRaw(content, new File(moduleDir, "src/main/java/" + pkgPath + "/" + className + ".java"));
  }

  private void registerInSettings(File settingsFile, String moduleName, boolean useKts)
      throws IOException {
    String current = readFile(settingsFile);
    for (String line : current.split("\n")) {
      String trimmed = line.trim();
      if (trimmed.startsWith("//") || trimmed.startsWith("#")) continue;
      if (trimmed.contains("\":" + moduleName + "\"") || trimmed.contains("':" + moduleName + "'"))
        return;
    }
    String newInclude =
        useKts ? "include(\":" + moduleName + "\")" : "include ':" + moduleName + "'";
    writeRaw(appendAfterLastInclude(current, newInclude), settingsFile);
  }

  private String appendAfterLastInclude(String content, String newLine) {
    Pattern p = Pattern.compile("(?m)^\\s*include[^\\n]*$");
    Matcher m = p.matcher(content);
    int lastEnd = -1;
    while (m.find()) lastEnd = m.end();
    if (lastEnd != -1) {
      return content.substring(0, lastEnd) + "\n" + newLine + content.substring(lastEnd);
    }
    return content.stripTrailing() + "\n\n" + newLine + "\n";
  }

  private boolean addDependencyToApp(File root, String moduleName) throws IOException {
    File appGradle = new File(root, "app/build.gradle.kts");
    if (!appGradle.exists()) appGradle = new File(root, "app/build.gradle");
    if (!appGradle.exists()) return false;

    boolean isKts = appGradle.getName().endsWith(".kts");
    String current = readFile(appGradle);
    String dep =
        isKts
            ? "    implementation(project(\":" + moduleName + "\"))"
            : "    implementation project(':" + moduleName + "')";

    if (current.contains("project(\":" + moduleName + "\")")
        || current.contains("project(':" + moduleName + "')")) return true;

    writeRaw(insertIntoDependencies(current, dep), appGradle);
    return true;
  }

  private void addModuleDependencies(
      File root, String newModuleName, boolean newModuleUseKts, List<String> modules)
      throws IOException {
    String newDep;
    if (newModuleUseKts) {
      newDep = "    implementation(project(\":" + newModuleName + "\"))";
    } else {
      newDep = "    implementation project(':" + newModuleName + "')";
    }
    String checkStr = "project(\":" + newModuleName + "\")";
    String checkStr2 = "project(':" + newModuleName + "')";

    for (String mod : modules) {
      File buildFile = new File(root, mod + "/build.gradle.kts");
      if (!buildFile.exists()) buildFile = new File(root, mod + "/build.gradle");
      if (!buildFile.exists()) continue;

      String content = readFile(buildFile);
      if (content.contains(checkStr) || content.contains(checkStr2)) continue;

      writeRaw(insertIntoDependencies(content, newDep), buildFile);
    }
  }

  private String insertIntoDependencies(String content, String dep) {
    Pattern startPat = Pattern.compile("(?m)^dependencies\\s*\\{");
    Matcher startMatcher = startPat.matcher(content);
    if (!startMatcher.find()) {
      return content.stripTrailing() + "\n\ndependencies {\n" + dep + "\n}\n";
    }
    int blockStart = startMatcher.end();
    int depth = 1;
    int i = blockStart;
    while (i < content.length() && depth > 0) {
      char c = content.charAt(i);
      if (c == '{') depth++;
      else if (c == '}') depth--;
      i++;
    }
    int closingBrace = i - 1;
    return content.substring(0, closingBrace) + dep + "\n" + content.substring(closingBrace);
  }

  private boolean isValidPackage(String pkg) {
    if (pkg == null || !pkg.contains(".")) return false;
    for (String seg : pkg.split("\\.", -1)) {
      if (seg.isEmpty() || !Character.isLetter(seg.charAt(0))) return false;
      if (!seg.matches("[a-zA-Z][a-zA-Z0-9_]*")) return false;
    }
    return true;
  }

  private String toClassName(String name) {
    StringBuilder sb = new StringBuilder();
    for (String part : name.split("[-_]")) {
      if (!part.isEmpty()) {
        sb.append(Character.toUpperCase(part.charAt(0)));
        if (part.length() > 1) sb.append(part.substring(1));
      }
    }
    return sb.length() > 0 ? sb.toString() : "Module";
  }

  private String readFile(File f) {
    return FileIOUtils.readFile2String(f);
  }

  private void writeRaw(String content, File dest) throws IOException {
    dest.getParentFile().mkdirs();
    try (FileOutputStream fos = new FileOutputStream(dest)) {
      fos.write(content.getBytes(StandardCharsets.UTF_8));
    }
  }
}
