package ir.hanzodev1375.ghostide.refactor.renameclass;

import java.io.File;

public final class ClassFileTarget {

  private final File file;
  private final boolean rewriteUnqualified;

  public ClassFileTarget(File file, boolean rewriteUnqualified) {
    this.file = file;
    this.rewriteUnqualified = rewriteUnqualified;
  }

  public File getFile() {
    return file;
  }

  public boolean shouldRewriteUnqualified() {
    return rewriteUnqualified;
  }
}
