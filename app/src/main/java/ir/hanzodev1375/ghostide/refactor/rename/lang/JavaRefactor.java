package ir.hanzodev1375.ghostide.refactor.rename.lang;

import ir.hanzodev1375.ghostide.refactor.rename.CancellationToken;
import ir.hanzodev1375.ghostide.refactor.rename.ProgressReporter;
import ir.hanzodev1375.ghostide.refactor.rename.RollbackManager;
import ir.hanzodev1375.ghostide.refactor.rename.model.RenameProgress;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

public final class JavaRefactor {

  public void rewrite(
      List<File> files,
      String oldPackage,
      String newPackage,
      RollbackManager rollbackManager,
      ProgressReporter reporter,
      CancellationToken token)
      throws Exception {
    JavaLexer lexer = new JavaLexer();
    PackageTextRewriter rewriter = new PackageTextRewriter();
    int total = files.size();
    for (int index = 0; index < total; index++) {
      token.throwIfCancelled();
      File file = files.get(index);
      reporter.report(
          new RenameProgress(RenameProgress.Phase.REWRITING_JAVA, index + 1, total, file.getName()));
      String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
      List<SourceRegion> regions = lexer.tokenize(content);
      PackageTextRewriter.Result result = rewriter.rewrite(content, regions, oldPackage, newPackage);
      if (result.getChangeCount() > 0) {
        rollbackManager.writeFile(file, result.getContent());
      }
    }
  }
}
