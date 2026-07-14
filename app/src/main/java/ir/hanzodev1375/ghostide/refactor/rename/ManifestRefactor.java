package ir.hanzodev1375.ghostide.refactor.rename;

import ir.hanzodev1375.ghostide.refactor.rename.model.RenameProgress;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ManifestRefactor {

  private static final Pattern PACKAGE_ATTRIBUTE =
      Pattern.compile("(<manifest[^>]*?\\bpackage\\s*=\\s*\")([^\"]*)(\")");
  private static final Pattern AUTHORITIES_ATTRIBUTE =
      Pattern.compile("(android:authorities\\s*=\\s*\")([^\"]*)(\")");

  public void rewrite(
      List<File> manifestFiles,
      String oldPackage,
      String newPackage,
      RollbackManager rollbackManager,
      ProgressReporter reporter,
      CancellationToken token)
      throws Exception {
    int total = manifestFiles.size();
    for (int index = 0; index < total; index++) {
      token.throwIfCancelled();
      File file = manifestFiles.get(index);
      reporter.report(
          new RenameProgress(
              RenameProgress.Phase.REWRITING_MANIFEST, index + 1, total, file.getName()));
      String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
      String rewritten = rewritePackageAttribute(content, oldPackage, newPackage);
      rewritten = rewriteAuthorities(rewritten, oldPackage, newPackage);
      if (!rewritten.equals(content)) {
        rollbackManager.writeFile(file, rewritten);
      }
    }
  }

  private String rewritePackageAttribute(String content, String oldPackage, String newPackage) {
    Matcher matcher = PACKAGE_ATTRIBUTE.matcher(content);
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

  private String rewriteAuthorities(String content, String oldPackage, String newPackage) {
    Matcher matcher = AUTHORITIES_ATTRIBUTE.matcher(content);
    StringBuilder result = new StringBuilder();
    int lastEnd = 0;
    while (matcher.find()) {
      result.append(content, lastEnd, matcher.start());
      String value = matcher.group(2);
      String[] tokens = value.split(";", -1);
      StringBuilder rebuilt = new StringBuilder();
      for (int i = 0; i < tokens.length; i++) {
        if (i > 0) {
          rebuilt.append(';');
        }
        rebuilt.append(rewriteAuthorityToken(tokens[i], oldPackage, newPackage));
      }
      result.append(matcher.group(1)).append(rebuilt).append(matcher.group(3));
      lastEnd = matcher.end();
    }
    result.append(content, lastEnd, content.length());
    return result.toString();
  }

  private String rewriteAuthorityToken(String token, String oldPackage, String newPackage) {
    if (token.startsWith(".")) {
      return token;
    }
    if (token.equals(oldPackage)) {
      return newPackage;
    }
    if (token.startsWith(oldPackage + ".")) {
      return newPackage + token.substring(oldPackage.length());
    }
    return token;
  }
}
