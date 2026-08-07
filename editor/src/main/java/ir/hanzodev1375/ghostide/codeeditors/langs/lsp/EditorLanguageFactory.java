package ir.hanzodev1375.ghostide.codeeditors.langs.lsp;

import android.content.Context;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
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

public final class EditorLanguageFactory {

  private interface Factory {
    Language create(Context context, String filePath);
  }

  private static final Map<String, Factory> FACTORIES = new HashMap<>();

  static {
    FACTORIES.put("java", (context, path) -> new JavaLanguage(context));
    FACTORIES.put("kt", (context, path) -> new KotlinLanguage(context));
    FACTORIES.put("kts", (context, path) -> new KotlinLanguage(context));
    FACTORIES.put("rs", (context, path) -> new RustLanguage());
    FACTORIES.put("go", (context, path) -> new GoLanguage(context));
    FACTORIES.put("py", (context, path) -> new Python3Language(context));
    FACTORIES.put("c", (context, path) -> new CLanguage());
    FACTORIES.put("h", (context, path) -> new CLanguage());
    FACTORIES.put("cpp", (context, path) -> new CppLanguage(context));
    FACTORIES.put("cc", (context, path) -> new CppLanguage(context));
    FACTORIES.put("cxx", (context, path) -> new CppLanguage(context));
    FACTORIES.put("hpp", (context, path) -> new CppLanguage(context));
    FACTORIES.put("cs", (context, path) -> new CSharpLanguage());
    FACTORIES.put("php", (context, path) -> new PhpLanguage(context));
    FACTORIES.put("rb", (context, path) -> new RubyLanguage(context));
    FACTORIES.put("css", CssLanguage::new);
    FACTORIES.put("json", JsonLanguage::new);
    FACTORIES.put("html", HtmlLanguage::new);
    FACTORIES.put("htm", HtmlLanguage::new);
    FACTORIES.put("js", JsLanguage::new);
    FACTORIES.put("mjs", JsLanguage::new);
    FACTORIES.put("cjs", JsLanguage::new);
    FACTORIES.put("ts", JsLanguage::new);
    FACTORIES.put("tsx", (context, path) -> new TsxLanguage());
    FACTORIES.put("dart", (context, path) -> new DartLanguage());
    FACTORIES.put("lua", (context, path) -> new LuaLanguage());
    FACTORIES.put("sql", (context, path) -> new SqlLanguage());
    FACTORIES.put("xml", (context, path) -> new XmlLanguage());
    FACTORIES.put("yaml", (context, path) -> new YamlLanguage());
    FACTORIES.put("yml", (context, path) -> new YamlLanguage());
    FACTORIES.put("toml", (context, path) -> new TomlLanguage());
    FACTORIES.put("ini", (context, path) -> new IniLanguage());
    FACTORIES.put("cfg", (context, path) -> new IniLanguage());
    FACTORIES.put("sh", (context, path) -> new ShellLanguage());
    FACTORIES.put("bash", (context, path) -> new ShellLanguage());
    FACTORIES.put("md", (context, path) -> new MarkdownLanguage());
    FACTORIES.put("markdown", (context, path) -> new MarkdownLanguage());
    FACTORIES.put("sass", (context, path) -> new SassLanguage(context));
    FACTORIES.put("scss", (context, path) -> new SassLanguage(context));
    FACTORIES.put("vue", (context, path) -> new VueLanguage(context));
    FACTORIES.put("gradle", (context, path) -> new GradleLanguage());
    FACTORIES.put("zig", (context, path) -> new ZigLanguage());
    FACTORIES.put("g4", (context, path) -> new AntlrLanguage());
  }

  private EditorLanguageFactory() {}

  public static Language create(Context context, String filePath) {
    String extension = extensionOf(filePath);
    Factory factory = FACTORIES.get(extension);
    if (factory == null) {
      return new EmptyLanguage();
    }
    try {
      return factory.create(context, filePath);
    } catch (RuntimeException e) {
      return new EmptyLanguage();
    }
  }

  private static String extensionOf(String filePath) {
    if (filePath == null) {
      return "";
    }
    int slash = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
    String name = slash >= 0 ? filePath.substring(slash + 1) : filePath;
    int dot = name.lastIndexOf('.');
    if (dot < 0 || dot == name.length() - 1) {
      return "";
    }
    return name.substring(dot + 1).toLowerCase(Locale.ROOT);
  }
}
