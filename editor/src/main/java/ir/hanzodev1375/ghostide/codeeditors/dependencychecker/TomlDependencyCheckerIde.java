package ir.hanzodev1375.ghostide.codeeditors.dependencychecker;

import java.util.List;
import java.util.Locale;

import io.github.rosemoe.sora.event.SelectionChangeEvent;
import io.github.rosemoe.sora.widget.CodeEditor;
import ir.hanzodev1375.ghostide.codeeditors.langs.toml.TomlLanguage;

/**
 * Checks Gradle version catalog files (gradle/libs.versions.toml) for newer library versions.
 *
 * <p>Unlike arbitrary TOML files, only files whose path ends with {@code libs.versions.toml} under a
 * {@code gradle} directory are treated as version catalogs.
 */
public final class TomlDependencyCheckerIde extends DependencyCheckerIde {

  public TomlDependencyCheckerIde(CodeEditor editor) {
    super(editor);
  }

  @Override
  protected boolean isMyLanguage() {
    if (!(editor.getEditorLanguage() instanceof TomlLanguage)) return false;
    String path = getFilePath();
    if (path == null) return false;
    String normalized = path.replace('\\', '/').toLowerCase(Locale.ROOT);
    return normalized.contains("/gradle/") && normalized.endsWith("libs.versions.toml");
  }

  @Override
  protected DependencyMatch findUnderCursor(SelectionChangeEvent event) {
    int line = event.getLeft().getLine();
    int column = event.getLeft().getColumn();
    String lineText = editor.getText().getLineString(line);
    return DependencyRefParser.findInToml(lineText, column);
  }

  @Override
  protected void collectLineHighlights(int line, String lineText, List<DependencyMatch> into) {
    DependencyMatch d = DependencyRefParser.findInToml(lineText);
    if (d != null) into.add(d);
  }
}
