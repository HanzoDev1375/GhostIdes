package ir.hanzodev1375.ghostide.refactor.rename.lang;

import java.util.List;

public final class ClassReferenceRewriter {

  public static final class Result {

    private final String content;
    private final int changeCount;

    public Result(String content, int changeCount) {
      this.content = content;
      this.changeCount = changeCount;
    }

    public String getContent() {
      return content;
    }

    public int getChangeCount() {
      return changeCount;
    }
  }

  public Result rewrite(
      String content,
      boolean isKotlin,
      String packageName,
      String oldClassName,
      String newClassName,
      boolean rewriteUnqualified) {
    PackageTextRewriter qualifiedRewriter = new PackageTextRewriter();
    SimpleIdentifierRewriter simpleRewriter = new SimpleIdentifierRewriter();

    List<SourceRegion> regions = tokenize(content, isKotlin);
    String oldQualified = packageName.isEmpty() ? oldClassName : packageName + "." + oldClassName;
    String newQualified = packageName.isEmpty() ? newClassName : packageName + "." + newClassName;
    PackageTextRewriter.Result qualifiedResult =
        qualifiedRewriter.rewrite(content, regions, oldQualified, newQualified);

    int totalChanges = qualifiedResult.getChangeCount();
    String updatedContent = qualifiedResult.getContent();

    if (rewriteUnqualified) {
      List<SourceRegion> updatedRegions = tokenize(updatedContent, isKotlin);
      SimpleIdentifierRewriter.Result simpleResult =
          simpleRewriter.rewrite(updatedContent, updatedRegions, oldClassName, newClassName);
      totalChanges += simpleResult.getChangeCount();
      updatedContent = simpleResult.getContent();
    }

    return new Result(updatedContent, totalChanges);
  }

  private List<SourceRegion> tokenize(String content, boolean isKotlin) {
    if (isKotlin) {
      return new KotlinLexer().tokenize(content);
    }
    return new JavaLexer().tokenize(content);
  }
}
