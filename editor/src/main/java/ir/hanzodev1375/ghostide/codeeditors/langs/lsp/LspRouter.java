package ir.hanzodev1375.ghostide.codeeditors.langs.lsp;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import io.github.rosemoe.sora.lsp.client.languageserver.requestmanager.RequestManager;
import io.github.rosemoe.sora.lsp.editor.LspEditor;
import io.github.rosemoe.sora.widget.CodeEditor;
import ir.hanzodev1375.ghostide.codeeditors.langs.lsp.model.BreadcrumbItem;
import ir.hanzodev1375.ghostide.ide.api.LspServerProvider;

/**
 * @author Ghost
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
    VUE,
    JSON,
    MARKDOWN,
    PHP,
    SASS,
    JS,
    RUBY,
    CSHARP
  }

  private static Lang langOf(String filePath) {
    if (filePath == null) return Lang.NONE;
    String lower = filePath.toLowerCase(Locale.ROOT);
    if (lower.endsWith(".py")) return Lang.PYTHON;
    //   if (JavaServer.isSupportedFile(filePath)) return Lang.JAVA;
    if (ClangdServer.INSTANCE.isSupportedFile(filePath)) return Lang.CPP;
    if (TsServer.INSTANCE.isSupportedFile(filePath)) return Lang.JS;
    if (PhpServer.INSTANCE.isSupportedFile(filePath)) return Lang.PHP;
    if (HtmlServer.INSTANCE.isSupportedFile(filePath)) return Lang.HTML;
    if (VueServer.INSTANCE.isSupportedFile(filePath)) return Lang.VUE;
    if (CssServer.INSTANCE.isSupportedFile(filePath)) return Lang.CSS;
    if (JsonServer.INSTANCE.isSupportedFile(filePath)) return Lang.JSON;
    if (MarkdownServer.INSTANCE.isSupportedFile(filePath)) return Lang.MARKDOWN;
    if (GoServer.INSTANCE.isSupportedFile(filePath)) return Lang.GO;
    if (SassServer.INSTANCE.isSupportedFile(filePath)) return Lang.SASS;
    if (RubyServer.INSTANCE.isSupportedFile(filePath)) return Lang.RUBY;
    if (CsharpServer.INSTANCE.isSupportedFile(filePath)) return Lang.CSHARP;
    return Lang.NONE;
  }

  public static boolean isSupportedFile(String filePath) {
    if (LspExtensionBridge.findProviderForFile(filePath) != null) return true;
    return langOf(filePath) != Lang.NONE;
  }

  public static boolean isInstalled(Context context, String filePath) {
    if (context == null || filePath == null) return false;
    if (LspExtensionBridge.findProviderForFile(filePath) != null) return true;

    switch (langOf(filePath)) {
      case PYTHON:
        return PylspServer.INSTANCE.isInstalled(context);
        //      case JAVA:
        //        return JavaServer.INSTANCE.isInstalled(context);
      case CPP:
        return ClangdServer.INSTANCE.isInstalled(context);
      case GO:
        return GoServer.INSTANCE.isInstalled(context);
      case CSS:
        return CssServer.INSTANCE.isInstalled(context);
      case HTML:
        return HtmlServer.INSTANCE.isInstalled(context);
      case VUE:
        return VueServer.INSTANCE.isInstalled(context);
      case PHP:
        return PhpServer.INSTANCE.isInstalled(context);
      case SASS:
        return SassServer.INSTANCE.isInstalled(context);
      case JS:
        return TsServer.INSTANCE.isInstalled(context);
      case RUBY:
        return RubyServer.INSTANCE.isInstalled(context);
      case CSHARP:
        return CsharpServer.INSTANCE.isInstalled(context);
      case JSON:
        return JsonServer.INSTANCE.isInstalled(context);
      case MARKDOWN:
        return MarkdownServer.INSTANCE.isInstalled(context);
      default:
        return false;
    }
  }

  public static LspEditor connectFile(
      Context context, String projectRoot, String filePath, CodeEditor editor) {
    if (context == null || filePath == null || editor == null) return null;
    try {
      LspHoverHighlighter.install(context, editor);

      LspServerProvider provider = LspExtensionBridge.findProvider(projectRoot, filePath);
      if (provider != null) {
        return LspExtensionBridge.connectFile(context, provider, projectRoot, filePath, editor);
      }

      switch (langOf(filePath)) {
        case PYTHON:
          return PylspServer.INSTANCE.connectFile(context, projectRoot, filePath, editor);
          //        case JAVA:
          //          return JavaServer.INSTANCE.connectFile(context, projectRoot, filePath,
          // editor);
        case CPP:
          return ClangdServer.INSTANCE.connectFile(context, projectRoot, filePath, editor);
        case GO:
          return GoServer.INSTANCE.connectFile(context, projectRoot, filePath, editor);
        case CSS:
          return CssServer.INSTANCE.connectFile(context, projectRoot, filePath, editor);
        case HTML:
          return HtmlServer.INSTANCE.connectFile(context, projectRoot, filePath, editor);
        case VUE:
          return VueServer.INSTANCE.connectFile(context, projectRoot, filePath, editor);
        case PHP:
          return PhpServer.INSTANCE.connectFile(context, projectRoot, filePath, editor);
        case SASS:
          return SassServer.INSTANCE.connectFile(context, projectRoot, filePath, editor);
        case JS:
          return TsServer.INSTANCE.connectFile(context, projectRoot, filePath, editor);
        case RUBY:
          return RubyServer.INSTANCE.connectFile(context, projectRoot, filePath, editor);
        case CSHARP:
          return CsharpServer.INSTANCE.connectFile(context, projectRoot, filePath, editor);
        case JSON:
          return JsonServer.INSTANCE.connectFile(context, projectRoot, filePath, editor);
        case MARKDOWN:
          return MarkdownServer.INSTANCE.connectFile(context, projectRoot, filePath, editor);
        default:
          return null;
      }
    } catch (Exception e) {
      Log.e(TAG, "LSP connection failed for: " + filePath, e);
      return null;
    }
  }

  public static void disconnectFile(LspEditor lspEditor) {
    if (lspEditor == null) return;
    try {
      lspEditor.dispose();
    } catch (Exception e) {
      Log.e(TAG, "Failed to disconnect LSP", e);
    }
  }

  private static final long BREADCRUMB_TIMEOUT_MS = 2000;

  public static List<BreadcrumbItem> fetchBreadcrumbs(
      LspEditor lspEditor, String filePath, int line, int column) {
    if (lspEditor == null || !lspEditor.isConnected()) return Collections.emptyList();
    if (filePath == null || filePath.isEmpty()) return Collections.emptyList();
    RequestManager requestManager = lspEditor.getRequestManager();
    if (requestManager == null) return Collections.emptyList();
    String documentUri = new File(filePath).toURI().toString();
    Log.d(TAG, "documentSymbol request for " + documentUri);
    DocumentSymbolParams params = new DocumentSymbolParams(new TextDocumentIdentifier(documentUri));
    CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> future;
    try {
      future = requestManager.documentSymbol(params);
    } catch (Exception e) {
      Log.e(TAG, "documentSymbol request failed", e);
      return Collections.emptyList();
    }
    if (future == null) return Collections.emptyList();
    List<Either<SymbolInformation, DocumentSymbol>> symbols;
    try {
      symbols = future.get(BREADCRUMB_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    } catch (Exception e) {
      Log.e(TAG, "documentSymbol response timed out", e);
      return Collections.emptyList();
    }
    if (symbols == null || symbols.isEmpty()) {
      Log.d(TAG, "No symbols returned for " + documentUri);
      return Collections.emptyList();
    }
    List<BreadcrumbItem> path = new ArrayList<>();
    if (symbols.get(0).isRight()) {
      List<DocumentSymbol> roots = new ArrayList<>();
      for (Either<SymbolInformation, DocumentSymbol> item : symbols) {
        if (item.isRight()) roots.add(item.getRight());
      }
      collectDocumentSymbolPath(roots, line, column, path);
    } else {
      List<SymbolInformation> flat = new ArrayList<>();
      for (Either<SymbolInformation, DocumentSymbol> item : symbols) {
        if (item.isLeft()) flat.add(item.getLeft());
      }
      collectSymbolInformationPath(flat, line, column, path);
    }
    return path;
  }

  private static void collectDocumentSymbolPath(
      List<DocumentSymbol> symbols, int line, int column, List<BreadcrumbItem> out) {
    DocumentSymbol best = null;
    for (DocumentSymbol symbol : symbols) {
      if (rangeContains(symbol.getRange(), line, column)) {
        if (best == null || rangeSize(symbol.getRange()) <= rangeSize(best.getRange())) {
          best = symbol;
        }
      }
    }
    if (best == null) return;
    Range selection = best.getSelectionRange() != null ? best.getSelectionRange() : best.getRange();
    Position start = selection.getStart();
    out.add(
        new BreadcrumbItem(best.getName(), best.getKind(), start.getLine(), start.getCharacter()));
    if (best.getChildren() != null && !best.getChildren().isEmpty()) {
      collectDocumentSymbolPath(best.getChildren(), line, column, out);
    }
  }

  private static void collectSymbolInformationPath(
      List<SymbolInformation> symbols, int line, int column, List<BreadcrumbItem> out) {
    List<SymbolInformation> matches = new ArrayList<>();
    for (SymbolInformation symbol : symbols) {
      if (rangeContains(symbol.getLocation().getRange(), line, column)) matches.add(symbol);
    }
    Collections.sort(
        matches,
        (a, b) ->
            Long.compare(
                rangeSize(a.getLocation().getRange()), rangeSize(b.getLocation().getRange())));
    for (SymbolInformation symbol : matches) {
      Position start = symbol.getLocation().getRange().getStart();
      out.add(
          new BreadcrumbItem(
              symbol.getName(), symbol.getKind(), start.getLine(), start.getCharacter()));
    }
  }

  private static boolean rangeContains(Range range, int line, int column) {
    if (range == null) return false;
    Position start = range.getStart();
    Position end = range.getEnd();
    if (line < start.getLine() || line > end.getLine()) return false;
    if (line == start.getLine() && column < start.getCharacter()) return false;
    if (line == end.getLine() && column > end.getCharacter()) return false;
    return true;
  }

  private static long rangeSize(Range range) {
    Position start = range.getStart();
    Position end = range.getEnd();
    return (long) (end.getLine() - start.getLine()) * 1000000L
        + (end.getCharacter() - start.getCharacter());
  }
}
