package ir.hanzodev1375.ghostide.codeeditors.langs.lsp;

import android.content.Context;

import io.github.rosemoe.sora.widget.CodeEditor;
import java.util.Locale;

import io.github.rosemoe.sora.lang.Language;
import io.github.rosemoe.sora.lsp.editor.text.EditorMarkdownCodeHighlighterProvider;
import io.github.rosemoe.sora.lsp.editor.text.MarkdownCodeHighlighterRegistry;
import ir.hanzodev1375.ghostide.codeeditors.langs.c.CLanguage;
import ir.hanzodev1375.ghostide.codeeditors.langs.cpp.CppLanguage;
import ir.hanzodev1375.ghostide.codeeditors.langs.css.CssLanguage;
import ir.hanzodev1375.ghostide.codeeditors.langs.csharp.CSharpLanguage;
import ir.hanzodev1375.ghostide.codeeditors.langs.dart.DartLanguage;
import ir.hanzodev1375.ghostide.codeeditors.langs.go.GoLanguage;
import ir.hanzodev1375.ghostide.codeeditors.langs.gradle.GradleLanguage;
import ir.hanzodev1375.ghostide.codeeditors.langs.html.HtmlLanguage;
import ir.hanzodev1375.ghostide.codeeditors.langs.ini.IniLanguage;
import ir.hanzodev1375.ghostide.codeeditors.langs.java.JavaLanguage;
import ir.hanzodev1375.ghostide.codeeditors.langs.js.JsLanguage;
import ir.hanzodev1375.ghostide.codeeditors.langs.json.JsonLanguage;
import ir.hanzodev1375.ghostide.codeeditors.langs.kotlin.KotlinLanguage;
import ir.hanzodev1375.ghostide.codeeditors.langs.lua.LuaLanguage;
import ir.hanzodev1375.ghostide.codeeditors.langs.markdown.MarkdownLanguage;
import ir.hanzodev1375.ghostide.codeeditors.langs.php.PhpLanguage;
import ir.hanzodev1375.ghostide.codeeditors.langs.python3.Python3Language;
import ir.hanzodev1375.ghostide.codeeditors.langs.ruby.RubyLanguage;
import ir.hanzodev1375.ghostide.codeeditors.langs.rust.RustLanguage;
import ir.hanzodev1375.ghostide.codeeditors.langs.sass.SassLanguage;
import ir.hanzodev1375.ghostide.codeeditors.langs.shell.ShellLanguage;
import ir.hanzodev1375.ghostide.codeeditors.langs.sql.SqlLanguage;
import ir.hanzodev1375.ghostide.codeeditors.langs.toml.TomlLanguage;
import ir.hanzodev1375.ghostide.codeeditors.langs.vue.VueLanguage;
import ir.hanzodev1375.ghostide.codeeditors.langs.xml.XmlLanguage;
import ir.hanzodev1375.ghostide.codeeditors.langs.yaml.YamlLanguage;
import ir.hanzodev1375.ghostide.codeeditors.langs.zig.ZigLanguage;

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
                case "jsx":
                  language = new JsLanguage(appContext, null);
                  break;
                case "typescript":
                case "ts":
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
                case "kt":
                case "kts":
                  language = new KotlinLanguage(appContext);
                  break;
                case "dart":
                  language = new DartLanguage();
                  break;
                case "c":
                  language = new CLanguage();
                  break;
                case "cpp":
                case "cc":
                case "cxx":
                case "hpp":
                  language = new CppLanguage(appContext);
                  break;
                case "lua":
                  language = new LuaLanguage();
                  break;
                case "rust":
                case "rs":
                  language = new RustLanguage();
                  break;
                case "shell":
                case "sh":
                case "bash":
                  language = new ShellLanguage();
                  break;
                case "sql":
                  language = new SqlLanguage();
                  break;
                case "toml":
                  language = new TomlLanguage();
                  break;
                case "vue":
                  language = new VueLanguage(appContext);
                  break;
                case "xml":
                  language = new XmlLanguage();
                  break;
                case "yaml":
                case "yml":
                  language = new YamlLanguage();
                  break;
                case "zig":
                  language = new ZigLanguage();
                  break;
                case "gradle":
                  language = new GradleLanguage();
                  break;
                case "ini":
                case "cfg":
                  language = new IniLanguage();
                  break;

                default:
                  return null;
              }
              return new Pair<>(language, scheme);
            }));
  }
}
