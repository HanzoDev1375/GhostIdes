package ir.hanzodev1375.ghostide.codeeditors.langs.lsp;

import android.content.Context;

import io.github.rosemoe.sora.widget.CodeEditor;
import java.util.Locale;

import io.github.rosemoe.sora.lang.Language;
import io.github.rosemoe.sora.lsp.editor.text.EditorMarkdownCodeHighlighterProvider;
import io.github.rosemoe.sora.lsp.editor.text.MarkdownCodeHighlighterRegistry;
import ir.hanzodev1375.ghostide.codeeditors.langs.css.CssLanguage;
import ir.hanzodev1375.ghostide.codeeditors.langs.csharp.CSharpLanguage;
import ir.hanzodev1375.ghostide.codeeditors.langs.go.GoLanguage;
import ir.hanzodev1375.ghostide.codeeditors.langs.html.HtmlLanguage;
import ir.hanzodev1375.ghostide.codeeditors.langs.java.JavaLanguage;
import ir.hanzodev1375.ghostide.codeeditors.langs.js.JsLanguage;
import ir.hanzodev1375.ghostide.codeeditors.langs.json.JsonLanguage;
import ir.hanzodev1375.ghostide.codeeditors.langs.markdown.MarkdownLanguage;
import ir.hanzodev1375.ghostide.codeeditors.langs.php.PhpLanguage;
import ir.hanzodev1375.ghostide.codeeditors.langs.python3.Python3Language;
import ir.hanzodev1375.ghostide.codeeditors.langs.ruby.RubyLanguage;
import ir.hanzodev1375.ghostide.codeeditors.langs.sass.SassLanguage;

import kotlin.Pair;

public final class LspHoverHighlighter {

  private static volatile boolean installed = false;

  private LspHoverHighlighter() {}

  public static synchronized void install(Context context, CodeEditor editor) {
    if (installed) return;
    installed = true;

    Context appContext = context.getApplicationContext();
    var scheme = editor.getColorScheme();
    var registry = MarkdownCodeHighlighterRegistry.Companion.getGlobal();

    registry.withProvider(
        new EditorMarkdownCodeHighlighterProvider(
            languageName -> {
              if (languageName == null) return null;
              Language language;
              switch (languageName.toLowerCase(Locale.ROOT)) {
                case "html":
                  language = new HtmlLanguage(appContext, null);
                  break;
                case "javascript":
                case "js":
                case "typescript":
                case "ts":
                case "jsx":
                case "tsx":
                  language = new JsLanguage(appContext, null);
                  break;
                case "css":
                  language = new CssLanguage(appContext, null);
                  break;
                case "scss":
                case "less":
                case "sass":
                  language = new SassLanguage(appContext);
                  break;
                case "json":
                  language = new JsonLanguage(appContext, null);
                  break;
                case "python":
                case "py":
                  language = new Python3Language(appContext);
                  break;
                case "java":
                  language = new JavaLanguage(appContext);
                  break;
                case "go":
                case "golang":
                  language = new GoLanguage(appContext);
                  break;
                case "php":
                  language = new PhpLanguage(appContext);
                  break;
                case "ruby":
                  language = new RubyLanguage(appContext);
                  break;
                case "csharp":
                case "cs":
                  language = new CSharpLanguage();
                  break;
                case "markdown":
                case "md":
                  language = new MarkdownLanguage();
                  break;
                default:
                  return null;
              }
              return new Pair<>(language, scheme);
            }));
  }
}
