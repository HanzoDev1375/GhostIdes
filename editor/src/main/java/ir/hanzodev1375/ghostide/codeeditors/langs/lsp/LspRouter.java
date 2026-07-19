package ir.hanzodev1375.ghostide.codeeditors.langs.lsp;

import android.content.Context;
import android.util.Log;

import java.util.Locale;

import io.github.rosemoe.sora.lsp.editor.LspEditor;
import io.github.rosemoe.sora.widget.CodeEditor;

/**
 * دروازه ی یکپارچه روی همه ی سرورهای LSP هر زبان (PylspServer، ClangdServer، GoServer، CssServer،
 * HtmlServer، PhpServer، SassServer، TsServer، JavaServer، RubyServer، CsharpServer). بر اساس پسوند
 * فایل تشخیص می ده کدوم سرور مسئوله، چک می کنه که آیا نصب هست یا نه، و وصلش می کنه.
 *
 * <p>هیچ پروسه یا LspProject جدیدی اینجا ساخته نمی شه؛ همه چیز فقط dispatch به همون کلاس های
 * *Server موجود هست که خودشون project/executable/language wrapper رو مدیریت می کنن. این کلاس فقط یک
 * نقطه ی ورودی مشترک برای IdeEditor و CustomEditorTextActionWindow فراهم می کنه تا مجبور نباشن
 * پسوند فایل رو دستی به کلاس درست map کنن.
 */
public final class LspRouter {

  private static final String TAG = "LspRouter";

  private LspRouter() {}

  private enum Lang {
    NONE,
    PYTHON,
    JAVA,
    CPP,
    GO,
    CSS,
    HTML,
    PHP,
    SASS,
    JS,
    RUBY,
    CSHARP
  }

 
  private static Lang langOf(String filePath) {
    if (filePath == null) return Lang.NONE;
    if (filePath.toLowerCase(Locale.ROOT).endsWith(".py")) return Lang.PYTHON;
    if (JavaServer.isJavaFile(filePath)) return Lang.JAVA;
    if (ClangdServer.isCppFile(filePath)) return Lang.CPP;
    if (TsServer.isJsFile(filePath)) return Lang.JS;
    if (PhpServer.isPhpFile(filePath)) return Lang.PHP;
    if (HtmlServer.isHtmlFile(filePath)) return Lang.HTML;
    if (CssServer.isCssFile(filePath)) return Lang.CSS;
    if (GoServer.isGoFile(filePath)) return Lang.GO;
    if (SassServer.isSassFile(filePath)) return Lang.SASS;
    if (RubyServer.isRubyFile(filePath)) return Lang.RUBY;
    if (CsharpServer.isCsharpFile(filePath)) return Lang.CSHARP;
    return Lang.NONE;
  }

  /** آیا اصلا برای این نوع فایل یک سرور LSP در پروژه تعریف شده (چه نصب باشه چه نه). */
  public static boolean isSupportedFile(String filePath) {
    return langOf(filePath) != Lang.NONE;
  }

  /**
   * چک سریع و بدون I/O سنگین (فقط وجود فایل باینری سرور رو داخل rootfs نگاه می کنه). صدا زدنش روی
   * UI thread مشکلی نداره؛ برای تصمیم نشون دادن/قایم کردن دکمه های LSP استفاده کن.
   */
  public static boolean isInstalled(Context context, String filePath) {
    if (context == null || filePath == null) return false;
    switch (langOf(filePath)) {
      case PYTHON:
        return PylspServer.isInstalled(context);
      case JAVA:
        return JavaServer.isInstalled(context);
      case CPP:
        return ClangdServer.isInstalled(context);
      case GO:
        return GoServer.findExecutable(context) != null;
      case CSS:
        return CssServer.isInstalled(context);
      case HTML:
        return HtmlServer.isInstalled(context);
      case PHP:
        return PhpServer.isInstalled(context);
      case SASS:
        return SassServer.isInstalled(context);
      case JS:
        return TsServer.isInstalled(context);
      case RUBY:
        return RubyServer.isInstalled(context);
      case CSHARP:
        return CsharpServer.isInstalled(context);
      default:
        return false;
    }
  }

  /**
   * فایل رو به سرور LSP مناسبِ پسوندش وصل می کنه. عملیات I/O سنگینه (اجرای proot + هندشیک LSP)،
   * هرگز روی UI thread صدا نزن؛ از یک ترد پس زمینه صداش بزن.
   *
   * @return LspEditor وصل شده، یا null اگه پسوند پشتیبانی نشه/سرور نصب نباشه/اتصال شکست بخوره
   */
  public static LspEditor connectFile(
      Context context, String projectRoot, String filePath, CodeEditor editor) {
    if (context == null || filePath == null || editor == null) return null;
    try {
      switch (langOf(filePath)) {
        case PYTHON:
          return PylspServer.connectFile(context, projectRoot, filePath, editor);
        case JAVA:
          return JavaServer.connectFile(context, projectRoot, filePath, editor);
        case CPP:
          return ClangdServer.connectFile(context, projectRoot, filePath, editor);
        case GO:
          return GoServer.connectFile(context, projectRoot, filePath, editor);
        case CSS:
          return CssServer.connectFile(context, projectRoot, filePath, editor);
        case HTML:
          return HtmlServer.connectFile(context, projectRoot, filePath, editor);
        case PHP:
          return PhpServer.connectFile(context, projectRoot, filePath, editor);
        case SASS:
          return SassServer.connectFile(context, projectRoot, filePath, editor);
        case JS:
          return TsServer.connectFile(context, projectRoot, filePath, editor);
        case RUBY:
          return RubyServer.connectFile(context, projectRoot, filePath, editor);
        case CSHARP:
          return CsharpServer.connectFile(context, projectRoot, filePath, editor);
        default:
          return null;
      }
    } catch (Exception e) {
      Log.e(TAG, "اتصال LSP برای فایل ناموفق بود: " + filePath, e);
      return null;
    }
  }

  /**
   * موقع بستن فایل/تب صدا بزن. اگه صدا زده نشه فقط سرور تا بسته شدن پروسه ی برنامه زنده می مونه.
   */
  public static void disconnectFile(LspEditor lspEditor) {
    if (lspEditor == null) return;
    try {
      lspEditor.dispose();
    } catch (Exception e) {
      Log.e(TAG, "بستن اتصال LSP با خطا مواجه شد", e);
    }
  }
}
