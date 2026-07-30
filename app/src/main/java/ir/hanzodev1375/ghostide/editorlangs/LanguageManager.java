package ir.hanzodev1375.ghostide.editorlangs;

import android.content.Context;
import io.github.rosemoe.sora.lang.EmptyLanguage;
import io.github.rosemoe.sora.lang.Language;
import ir.hanzodev1375.ghostide.codeeditors.langs.antlr.AntlrLanguage;
import ir.hanzodev1375.ghostide.codeeditors.langs.c.CLanguage;
import ir.hanzodev1375.ghostide.codeeditors.langs.cpp.CppLanguage;
import ir.hanzodev1375.ghostide.codeeditors.langs.csharp.CSharpLanguage;
import ir.hanzodev1375.ghostide.codeeditors.langs.css.CssLanguage;
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
import ir.hanzodev1375.ghostide.codeeditors.langs.tsx.TsxLanguage;
import ir.hanzodev1375.ghostide.codeeditors.langs.vue.VueLanguage;
import ir.hanzodev1375.ghostide.codeeditors.langs.xml.XmlLanguage;
import ir.hanzodev1375.ghostide.codeeditors.langs.yaml.YamlLanguage;
import ir.hanzodev1375.ghostide.codeeditors.langs.zig.ZigLanguage;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class LanguageManager {

  private interface LanguageFactory {
    Language create(Context context, String filePath);
  }

  private static final Map<String, LanguageFactory> factories = new HashMap<>();

  static {
    register((c, p) -> new JavaLanguage(c), "java");
    register((c, p) -> new CLanguage(), "c");
    register((c, p) -> new CSharpLanguage(), "cs");
    register((c, p) -> new CppLanguage(c), "cpp", "cxx", "hpp", "hxx", "cc", "h");
    register((c, p) -> new HtmlLanguage(c, p), "html");
    register((c, p) -> new CssLanguage(c, p), "css");
    register((c, p) -> new JsLanguage(c, p), "js","mjs","jsx");
    register((c, p) -> new Python3Language(c), "py");
    register((c, p) -> new JsonLanguage(c, p), "json");
    register((c, p) -> new XmlLanguage(), "xml");
    register((c, p) -> new KotlinLanguage(c), "kt", "kts");
    register((c, p) -> new TomlLanguage(), "toml");
    register((c, p) -> new GradleLanguage(), "gradle", "groovy");
    register((c, p) -> new SassLanguage(c), "sass", "scss");
    register((c, p) -> new MarkdownLanguage(), "md", "markdown");
    register((c, p) -> new YamlLanguage(), "yml", "yaml");
    register((c, p) -> new LuaLanguage(), "lua");
    register((c, p) -> new GoLanguage(c), "go");
    register((c, p) -> new PhpLanguage(c), "php");
    register((c, p) -> new DartLanguage(), "dart");
    register((c, p) -> new TsxLanguage(), "ts", "tsx");
    register((c, p) -> new SqlLanguage(), "sql");
    register((c, p) -> new ShellLanguage(), "sh", "rc", "bash", "bashrc", "ash", "zsh", "zshrc");
    register((c, p) -> new RustLanguage(), "rs");
    register((c, p) -> new RubyLanguage(c), "rb");
    register((c, p) -> new AntlrLanguage(), "g4");
    register((c, p) -> new IniLanguage(), "ini");
    register((c, p) -> new ZigLanguage(), "zig");
    register((c, p) -> new EmptyLanguage(), "txt", "log");
    register((c, p) -> new VueLanguage(c), "vue");
  }

  private static void register(LanguageFactory factory, String... extensions) {
    for (String ext : extensions) {
      factories.put(ext, factory);
    }
  }

  private LanguageManager() {}

  public static Language resolve(Context context, String filePath) {
    if (filePath == null) return null;
    var factory = factories.get(extensionOf(filePath));
    return factory != null ? factory.create(context, filePath) : null;
  }

  public static boolean isSupported(String filePath) {
    return filePath != null && factories.containsKey(extensionOf(filePath));
  }

  private static String extensionOf(String filePath) {
    int dot = filePath.lastIndexOf('.');
    if (dot < 0 || dot == filePath.length() - 1) return "";
    return filePath.substring(dot + 1).toLowerCase(Locale.ROOT);
  }
}
