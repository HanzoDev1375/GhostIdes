package ir.hanzodev1375.ghostide.codeeditors.dependencychecker;

import java.util.List;

import io.github.rosemoe.sora.event.SelectionChangeEvent;
import io.github.rosemoe.sora.widget.CodeEditor;
import ir.hanzodev1375.ghostide.codeeditors.langs.gradle.GradleLanguage;

/** Checks Gradle build files for newer versions of declared dependencies. */
public final class GradleDependencyCheckerIde extends DependencyCheckerIde {

  public GradleDependencyCheckerIde(CodeEditor editor) {
    super(editor);
  }

  @Override
  protected boolean isMyLanguage() {
    return editor.getEditorLanguage() instanceof GradleLanguage;
  }

  @Override
  protected DependencyMatch findUnderCursor(SelectionChangeEvent event) {
    int line = event.getLeft().getLine();
    int column = event.getLeft().getColumn();
    String lineText = editor.getText().getLineString(line);
    return DependencyRefParser.findInGradle(lineText, column);
  }

  @Override
  protected void collectLineHighlights(int line, String lineText, List<DependencyMatch> into) {
    into.addAll(DependencyRefParser.findAllInGradle(lineText));
  }
}
