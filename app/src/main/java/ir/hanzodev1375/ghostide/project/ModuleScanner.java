package ir.hanzodev1375.ghostide.project;

import com.blankj.utilcode.util.FileIOUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ModuleScanner {

  private static final Pattern INCLUDE_PATTERN =
      Pattern.compile("include\\s*\\(?['\"]:(\\w[\\w-]*)['\"]\\)?");

  public static List<String> scanModules(String projectRootPath) {
    if (projectRootPath == null || projectRootPath.isEmpty()) {
      return Collections.emptyList();
    }

    File root = new File(projectRootPath);
    String content = readSettingsFile(root);
    if (content == null) {
      return Collections.emptyList();
    }

    List<String> modules = new ArrayList<>();
    Matcher matcher = INCLUDE_PATTERN.matcher(content);
    while (matcher.find()) {
      modules.add(matcher.group(1));
    }

    Collections.sort(modules);
    return modules;
  }

  private static String readSettingsFile(File root) {
    File kts = new File(root, "settings.gradle.kts");
    if (kts.exists()) {
      return FileIOUtils.readFile2String(kts);
    }

    File groovy = new File(root, "settings.gradle");
    if (groovy.exists()) {
      return FileIOUtils.readFile2String(groovy);
    }

    return null;
  }
}
