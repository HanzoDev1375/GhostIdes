package ir.hanzodev1375.ghostide.refactor.rename;

import ir.hanzodev1375.ghostide.refactor.rename.model.ModuleType;
import ir.hanzodev1375.ghostide.refactor.rename.model.RenameProgress;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GradleRefactor {

  private static final Pattern NAMESPACE_VALUE =
      Pattern.compile("(\\bnamespace\\s*=?\\s*['\"])([^'\"]*)(['\"])");
  private static final Pattern APPLICATION_ID_VALUE =
      Pattern.compile("(\\bapplicationId\\s*=?\\s*['\"])([^'\"]*)(['\"])");

  public void rewrite(
      List<File> gradleFiles,
      String oldPackage,
      String newPackage,
      ModuleType moduleType,
      RollbackManager rollbackManager,
      ProgressReporter reporter,
      CancellationToken token)
      throws Exception {
    int total = gradleFiles.size();
    for (int index = 0; index < total; index++) {
      token.throwIfCancelled();
      File file = gradleFiles.get(index);
      reporter.report(
          new RenameProgress(
              RenameProgress.Phase.REWRITING_GRADLE, index + 1, total, file.getName()));
      String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
      String rewritten = rewriteExactValue(content, NAMESPACE_VALUE, oldPackage, newPackage);
      if (moduleType == ModuleType.APPLICATION) {
        rewritten = rewriteExactValue(rewritten, APPLICATION_ID_VALUE, oldPackage, newPackage);
      }
      if (!rewritten.equals(content)) {
        rollbackManager.writeFile(file, rewritten);
      }
    }
  }

  private String rewriteExactValue(
      String content, Pattern pattern, String oldPackage, String newPackage) {
    Matcher matcher = pattern.matcher(content);
    StringBuilder result = new StringBuilder();
    int lastEnd = 0;
    while (matcher.find()) {
      result.append(content, lastEnd, matcher.start());
      String value = matcher.group(2);
      if (value.equals(oldPackage)) {
        result.append(matcher.group(1)).append(newPackage).append(matcher.group(3));
      } else {
        result.append(matcher.group());
      }
      lastEnd = matcher.end();
    }
    result.append(content, lastEnd, content.length());
    return result.toString();
  }
}
