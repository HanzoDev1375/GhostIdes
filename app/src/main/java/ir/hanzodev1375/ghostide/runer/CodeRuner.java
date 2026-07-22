package ir.hanzodev1375.ghostide.runer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import ir.hanzodev1375.ghostide.terminal.activity.TerminalActivity;
import ir.hanzodev1375.ghostide.terminal.sheet.TerminalBottomSheetFragment;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class CodeRuner {

  private Context context;

  public CodeRuner(Context context) {
    this.context = context;
  }

  public void bindof(String path, boolean asBottomSheet) {
    String command = buildCommand(path);
    if (command == null) return;

    if (asBottomSheet) {
      runInBottomSheet(command);
    } else {
      runInActivity(command);
    }
  }

  public void runInActivity(String command) {
    Intent i = new Intent(context, TerminalActivity.class);
    i.putExtra(TerminalActivity.EXTRA_COMMAND, command);
    if (!(context instanceof Activity)) {
      i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }
    context.startActivity(i);
  }

  void runInBottomSheet(String command) {
    FragmentManager fm = resolveFragmentManager();
    if (fm == null) {
      runInActivity(command);
      return;
    }
    TerminalBottomSheetFragment.newInstance(command, null).show(fm, "terminal_sheet");
  }

  private FragmentManager resolveFragmentManager() {
    if (context instanceof FragmentActivity) {
      return ((FragmentActivity) context).getSupportFragmentManager();
    }
    return null;
  }

  private String buildCommand(String path) {
    if (path.endsWith(".c")) {
      return c(path);
    } else if (path.endsWith(".cpp")
        || path.endsWith(".h")
        || path.endsWith(".hpp")
        || path.endsWith(".cc")) {
      return cpp(path);
    } else if (path.endsWith(".py")) {
      return python(path);
    } else if (path.endsWith(".php")) {
      return php(path);
    } else if (path.endsWith(".go")) {
      return go(path);
    } else if (path.endsWith(".js")) {
      return node(path);
    } else if (path.endsWith(".ts")) {
      return typescript(path);
    } else if (path.endsWith(".lua")) {
      return lua(path);
    } else if (path.endsWith(".java")) {
      return java(path);
    } else if (path.endsWith(".scss") || path.endsWith(".sass")) {
      return sass(path);
    }
    return null;
  }

  private String python(String path) {
    File file = new File(path);
    File dir = file.getParentFile();
    if (dir == null) dir = new File(".");

    String moduleName = file.getName();
    int dot = moduleName.lastIndexOf('.');
    if (dot != -1) moduleName = moduleName.substring(0, dot);

    List<String> packageParts = new ArrayList<>();
    packageParts.add(moduleName);

    File current = dir;
    File root = dir;
    while (current != null && new File(current, "__init__.py").exists()) {
      packageParts.add(0, current.getName());
      root = current.getParentFile();
      current = current.getParentFile();
    }

    String setup =
        "if ! command -v python3 >/dev/null 2>&1; then apt update && apt install python3 -y; fi; ";

    if (packageParts.size() > 1 && root != null) {
      StringBuilder dotted = new StringBuilder();
      for (int i = 0; i < packageParts.size(); i++) {
        if (i > 0) dotted.append('.');
        dotted.append(packageParts.get(i));
      }
      return "clear; "
          + setup
          + "cd \""
          + root.getAbsolutePath()
          + "\" && python3 -m "
          + dotted;
    }

    return "clear; "
        + setup
        + "cd \""
        + dir.getAbsolutePath()
        + "\" && python3 \""
        + file.getName()
        + "\"";
  }

  private String php(String path) {
    File file = new File(path);
    File dir = file.getParentFile();
    if (dir == null) dir = new File(".");
    return "clear; "
        + "if ! command -v php >/dev/null 2>&1; then apt update && apt install php -y; fi; "
        + "cd \"" + dir.getAbsolutePath() + "\" && php \"" + file.getName() + "\"";
  }

  private String c(String path) {
    String exe = new File(path).getName();
    exe = exe.substring(0, exe.lastIndexOf('.'));

    return "clear; "
        + "if ! command -v gcc >/dev/null 2>&1; then apt update && apt install gcc -y; fi; "
        + "gcc \""
        + path
        + "\" -o \""
        + exe
        + "\" && ./"
        + exe;
  }

  private String cpp(String path) {
    String exe = new File(path).getName();
    exe = exe.substring(0, exe.lastIndexOf('.'));

    return "clear; "
        + "if ! command -v g++ >/dev/null 2>&1; then apt update && apt install g++ -y; fi; "
        + "g++ \""
        + path
        + "\" -o \""
        + exe
        + "\" && ./"
        + exe;
  }

  private String go(String path) {
    return "clear; "
        + "if ! command -v go >/dev/null 2>&1; then apt update && apt install golang-go -y; fi; "
        + "go run \""
        + path
        + "\"";
  }

  private String node(String path) {
    return "clear; "
        + "if ! command -v node >/dev/null 2>&1; then apt update && apt install nodejs npm -y; fi; "
        + "node \""
        + path
        + "\"";
  }

  private String typescript(String path) {
    return "clear; "
        + "if ! command -v ts-node >/dev/null 2>&1; then apt update && apt install nodejs npm -y && npm install -g typescript ts-node; fi; "
        + "ts-node \""
        + path
        + "\"";
  }

  private String lua(String path) {
    return "clear; "
        + "if ! command -v lua >/dev/null 2>&1; then apt update && apt install lua5.4 -y; fi; "
        + "lua \""
        + path
        + "\"";
  }

  private String java(String path) {
    File file = new File(path);
    File dir = file.getParentFile();
    if (dir == null) dir = new File(".");

    File gradleWrapper = findGradleWrapper(dir);
    if (gradleWrapper != null) {
      File projectRoot = gradleWrapper.getParentFile();
      return "clear; "
          + "if [ ! -x \"" + gradleWrapper.getAbsolutePath() + "\" ]; then chmod +x \"" + gradleWrapper.getAbsolutePath() + "\"; fi; "
          + "cd \"" + projectRoot.getAbsolutePath() + "\" && sh gradlew build";
    }

    File pomFile = findPomXml(dir);
    if (pomFile != null) {
      File projectRoot = pomFile.getParentFile();
      return "clear; "
          + "if ! command -v mvn >/dev/null 2>&1; then apt update && apt install maven -y; fi; "
          + "cd \"" + projectRoot.getAbsolutePath() + "\" && mvn package";
    }

    File gradleFile = findGradleBuildFile(dir);
    if (gradleFile != null) {
      File projectRoot = gradleFile.getParentFile();
      return "clear; "
          + "if ! command -v gradle >/dev/null 2>&1; then apt update && apt install gradle -y; fi; "
          + "cd \"" + projectRoot.getAbsolutePath() + "\" && gradle build";
    }

    String className = file.getName();
    int dot = className.lastIndexOf('.');
    if (dot != -1) className = className.substring(0, dot);

    String packageName = extractPackageName(file);
    String fqcn = (packageName != null && !packageName.isEmpty()) ? packageName + "." + className : className;

    File sourceRoot = resolvePackageRoot(dir, packageName);
    File outDir = new File(sourceRoot, ".ghostide_build");

    String compileTargets;
    if (sourceRoot.equals(dir)) {
      compileTargets = "\"" + dir.getAbsolutePath() + "\"/*.java";
    } else {
      compileTargets = "\"" + path + "\"";
    }

    return "clear; "
        + "if ! command -v javac >/dev/null 2>&1; then apt update && apt install default-jdk -y; fi; "
        + "mkdir -p \"" + outDir.getAbsolutePath() + "\" && javac -d \"" + outDir.getAbsolutePath() + "\" -sourcepath \"" + sourceRoot.getAbsolutePath() + "\" " + compileTargets;
  }

  private File findGradleWrapper(File dir) {
    while (dir != null) {
      File wrapper = new File(dir, "gradlew");
      if (wrapper.exists()) return wrapper;
      dir = dir.getParentFile();
    }
    return null;
  }

  private File findPomXml(File dir) {
    while (dir != null) {
      File pom = new File(dir, "pom.xml");
      if (pom.exists()) return pom;
      dir = dir.getParentFile();
    }
    return null;
  }

  private File findGradleBuildFile(File dir) {
    while (dir != null) {
      File buildGradle = new File(dir, "build.gradle");
      File buildGradleKts = new File(dir, "build.gradle.kts");
      if (buildGradle.exists()) return buildGradle;
      if (buildGradleKts.exists()) return buildGradleKts;
      dir = dir.getParentFile();
    }
    return null;
  }

  private File resolvePackageRoot(File dir, String packageName) {
    if (packageName == null || packageName.isEmpty()) return dir;
    String[] segments = packageName.split("\\.");
    File current = dir;
    for (int i = segments.length - 1; i >= 0; i--) {
      if (current == null || !current.getName().equals(segments[i])) {
        return dir;
      }
      current = current.getParentFile();
    }
    return current != null ? current : dir;
  }

  private String extractPackageName(File javaFile) {
    try (BufferedReader reader = new BufferedReader(new FileReader(javaFile))) {
      String line;
      while ((line = reader.readLine()) != null) {
        String trimmed = line.trim();
        if (trimmed.isEmpty()
            || trimmed.startsWith("//")
            || trimmed.startsWith("/*")
            || trimmed.startsWith("*")) {
          continue;
        }
        if (trimmed.startsWith("package ")) {
          String pkg = trimmed.substring("package ".length()).trim();
          if (pkg.endsWith(";")) pkg = pkg.substring(0, pkg.length() - 1).trim();
          return pkg;
        }
        break;
      }
    } catch (Exception e) {
    }
    return null;
  }

  private String sass(String path) {
    String fileName = new File(path).getName();
    String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
    String ext = path.endsWith(".scss") ? "scss" : "sass";
    String outputCss = baseName + ".css";
    String dir = new File(path).getParent();
    if (dir == null) dir = ".";

    return "clear; "
        + "echo '=== Compiling Sass to CSS ==='; "
        + "if ! command -v sass >/dev/null 2>&1; then "
        + "  echo 'Sass not found. Installing...'; "
        + "  apt update && apt install nodejs npm -y && npm install -g sass; "
        + "fi; "
        + "sass \"" + path + "\" \"" + dir + "/" + outputCss + "\" --style=expanded --no-source-map; "
        + "if [ $? -eq 0 ]; then "
        + "  echo ''; "
        + "  echo '✅ Compilation successful!'; "
        + "  echo '📄 Output: " + outputCss + "'; "
        + "  echo ''; "
        + "  echo '--- Generated CSS ---'; "
        + "  cat \"" + dir + "/" + outputCss + "\"; "
        + "else "
        + "  echo '❌ Compilation failed!'; "
        + "fi";
  }
}