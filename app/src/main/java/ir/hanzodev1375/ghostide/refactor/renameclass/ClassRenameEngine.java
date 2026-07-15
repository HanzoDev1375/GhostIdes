package ir.hanzodev1375.ghostide.refactor.renameclass;

import ir.hanzodev1375.ghostide.refactor.rename.CancellationToken;
import ir.hanzodev1375.ghostide.refactor.rename.ProgressReporter;
import ir.hanzodev1375.ghostide.refactor.rename.RollbackManager;
import ir.hanzodev1375.ghostide.refactor.rename.lang.ClassReferenceRewriter;
import ir.hanzodev1375.ghostide.refactor.rename.model.RenameProgress;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public final class ClassRenameEngine {

  public File apply(
      ClassScanResult scanResult,
      String newClassName,
      RollbackManager rollbackManager,
      ProgressReporter reporter,
      CancellationToken token)
      throws Exception {
    ClassReferenceRewriter rewriter = new ClassReferenceRewriter();
    int total = scanResult.getTargets().size();
    int index = 0;
    for (ClassFileTarget target : scanResult.getTargets()) {
      token.throwIfCancelled();
      index++;
      File file = target.getFile();
      reporter.report(
          new RenameProgress(RenameProgress.Phase.REWRITING_JAVA, index, total, file.getName()));
      boolean isKotlinFile = file.getName().endsWith(".kt");
      String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
      ClassReferenceRewriter.Result result =
          rewriter.rewrite(
              content,
              isKotlinFile,
              scanResult.getPackageName(),
              scanResult.getOldClassName(),
              newClassName,
              target.shouldRewriteUnqualified());
      if (result.getChangeCount() > 0) {
        rollbackManager.writeFile(file, result.getContent());
      }
    }

    token.throwIfCancelled();
    File targetFile = scanResult.getTargetFile();
    String extension = scanResult.isKotlin() ? ".kt" : ".java";
    File newFile = new File(targetFile.getParentFile(), newClassName + extension);
    reporter.report(new RenameProgress(RenameProgress.Phase.MOVING_FILES, 1, 1, newFile.getName()));
    rollbackManager.moveFile(targetFile, newFile);
    return newFile;
  }
}
