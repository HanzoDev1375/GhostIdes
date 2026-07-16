package ir.hanzodev1375.ghostide.codeeditors.langs.css;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.github.rosemoe.sora.lang.EmptyLanguage;
import io.github.rosemoe.sora.lang.Language;
import io.github.rosemoe.sora.lang.QuickQuoteHandler;
import io.github.rosemoe.sora.lang.analysis.AnalyzeManager;
import io.github.rosemoe.sora.lang.completion.CompletionHelper;
import io.github.rosemoe.sora.lang.completion.CompletionPublisher;
import io.github.rosemoe.sora.lang.completion.IdentifierAutoComplete;
import io.github.rosemoe.sora.lang.completion.snippet.CodeSnippet;
import io.github.rosemoe.sora.lang.completion.snippet.parser.CodeSnippetParser;
import io.github.rosemoe.sora.lang.completion.SnippetDescription;
import io.github.rosemoe.sora.lang.format.AsyncFormatter;
import io.github.rosemoe.sora.lang.format.Formatter;
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandler;
import io.github.rosemoe.sora.lang.styling.Styles;
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandleResult;
import io.github.rosemoe.sora.text.CharPosition;
import io.github.rosemoe.sora.text.Content;
import io.github.rosemoe.sora.text.ContentReference;
import io.github.rosemoe.sora.text.TextRange;
import io.github.rosemoe.sora.text.TextUtils;
import io.github.rosemoe.sora.widget.SymbolPairMatch;
import ir.hanzodev1375.ghostide.codeeditors.langs.antlr4base.CharParser;
import ir.hanzodev1375.ghostide.codeeditors.langs.antlr4base.SnippetCompletionItem;
import ir.hanzodev1375.ghostide.codeeditors.langs.formatHelp.PrettierFormatter;
import ir.hanzodev1375.ghostide.codeeditors.langs.html.CssHelper;
import ir.hanzodev1375.ghostide.codeeditors.langs.html.CssCompletionItem;
import ir.hanzodev1375.ghostide.codeeditors.langs.html.HTMLAnalyzer;
import ir.hanzodev1375.ghostide.codeeditors.langs.html.HTMLLexer;
import ir.hanzodev1375.ghostide.codeeditors.langs.html.HtmlHelper;
import ir.hanzodev1375.ghostide.codeeditors.lspcustomhot.Css3Server;
import ir.hanzodev1375.ghostide.codeeditors.lspcustomhot.PathCompleter;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import ir.hanzodev1375.ghostide.codeeditors.lspcustomhot.CustomCompletionItem;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandleResult;
import io.github.rosemoe.sora.lang.styling.Styles;

public class CssLanguage implements Language {

  private final HTMLAnalyzer analyzer;
  private final IdentifierAutoComplete autoComplete;
  private Context context;
  private String path;

  public CssLanguage(Context context, String path) {
    String[] htmlKeywords = {"!", "DOCTYPE"};
    autoComplete = new IdentifierAutoComplete(htmlKeywords);
    analyzer = new HTMLAnalyzer();
    analyzer.init(context, path);
  }

  @NonNull
  @Override
  public AnalyzeManager getAnalyzeManager() {
    return analyzer;
  }

  @Nullable
  @Override
  public QuickQuoteHandler getQuickQuoteHandler() {
    return null;
  }

  private final Formatter format =
      new AsyncFormatter() {
        @Nullable
        @Override
        public TextRange formatAsync(@NonNull Content text, @NonNull TextRange cursorRange) {
          PrettierFormatter formatted = new PrettierFormatter();
          String formatResult = formatted.format(context, text.toString(), "css");

          if (!text.toString().equals(formatResult)) {
            int oldCursor = cursorRange.getStartIndex();
            text.delete(0, text.length());
            text.insert(0, 0, formatResult);
            int newCursor = Math.min(oldCursor, formatResult.length());
            CharPosition pos = text.getIndexer().getCharPosition(newCursor);
            return new TextRange(pos, pos);
          }

          return cursorRange;
        }

        @Nullable
        @Override
        public TextRange formatRegionAsync(
            @NonNull Content text,
            @NonNull TextRange rangeToFormat,
            @NonNull TextRange cursorRange) {
          return null;
        }
      };

  @Override
  public void destroy() {}

  @Override
  public int getInterruptionLevel() {
    return INTERRUPTION_LEVEL_STRONG;
  }

  @Override
  public void requireAutoComplete(
      @NonNull ContentReference content,
      @NonNull CharPosition position,
      @NonNull CompletionPublisher publisher,
      @NonNull Bundle es) {
    String prefix = CompletionHelper.computePrefix(content, position, CharParser::parserHtml);
    autoComplete.requireAutoComplete(
        content, position, prefix, publisher, analyzer.getSyncIdentifiers());
    for (CssCompletionItem item : CssHelper.getPropertyItemsByPrefix(prefix)) {
      publisher.addItem(item);
    }

    Css3Server cssServer = new Css3Server(context);
    List<CustomCompletionItem> cssItems = cssServer.getCompletions(prefix);
    for (CustomCompletionItem item : cssItems) {
      publisher.addItem(item);
    }

    for (CustomCompletionItem item : HtmlHelper.getNormalTag(prefix)) {
      publisher.addItem(item);
    }
    return;
  }

  @Override
  public int getIndentAdvance(@NonNull ContentReference text, int line, int column) {

    try {
      var lexer = new HTMLLexer(CharStreams.fromReader(new StringReader(text.getLine(line))));
      Token token;
      int advance = 0;
      while (((token = lexer.nextToken()) != null && token.getType() != token.EOF)) {
        switch (token.getType()) {
          case HTMLLexer.LBRACE:
            advance++;
            break;
          case HTMLLexer.RBRACE:
            advance--;
            break;
        }
      }
      advance = Math.max(0, advance);
      return advance * 2;
    } catch (IOException e) {
      e.printStackTrace();
    }
    return 0;
  }

  @Override
  public boolean useTab() {
    return true;
  }

  @NonNull
  @Override
  public Formatter getFormatter() {
    return format;
  }

  @Override
  public SymbolPairMatch getSymbolPairs() {
    return new SymbolPairMatch.DefaultSymbolPairs();
  }

  private class CssOpenBraceHandler implements NewlineHandler {

    @Override
    public boolean matchesRequirement(
        @NonNull Content text, @NonNull CharPosition position, @Nullable Styles style) {

      int line = position.line;
      if (line < 0 || line >= text.getLineCount()) {
        return false;
      }

      String before = text.getLine(line).subSequence(0, position.column).toString();

      int len = before.length();

      for (int i = len - 1; i >= 0; i--) {
        char c = before.charAt(i);

        if (c == '{') {
          return true;
        }

        if (!Character.isWhitespace(c)) {
          break;
        }
      }

      return false;
    }

    @NonNull
    @Override
    public NewlineHandleResult handleNewline(
        @NonNull Content text,
        @NonNull CharPosition position,
        @Nullable Styles style,
        int tabSize) {

      int line = position.line;

      String before = text.getLine(line).subSequence(0, position.column).toString();

      int indent = TextUtils.countLeadingSpaceCount(before, tabSize);

      String indentStr = TextUtils.createIndent(indent + tabSize, tabSize, false);

      return new NewlineHandleResult(new StringBuilder("\n").append(indentStr), 0);
    }
  }

  private class CssCloseBraceHandler implements NewlineHandler {

    @Override
    public boolean matchesRequirement(
        @NonNull Content text, @NonNull CharPosition position, @Nullable Styles style) {

      int line = position.line;

      if (line < 0 || line >= text.getLineCount()) {
        return false;
      }

      String before = text.getLine(line).subSequence(0, position.column).toString();

      return before.trim().endsWith("}");
    }

    @NonNull
    @Override
    public NewlineHandleResult handleNewline(
        @NonNull Content text,
        @NonNull CharPosition position,
        @Nullable Styles style,
        int tabSize) {

      int line = position.line;

      String before = text.getLine(line).subSequence(0, position.column).toString();

      int indent = TextUtils.countLeadingSpaceCount(before, tabSize);

      int newIndent = Math.max(0, indent - tabSize);

      String indentStr = TextUtils.createIndent(newIndent, tabSize, false);

      return new NewlineHandleResult(new StringBuilder("\n").append(indentStr), 0);
    }
  }

  @Override
  public NewlineHandler[] getNewlineHandlers() {
    return new NewlineHandler[] {new CssOpenBraceHandler(), new CssCloseBraceHandler()};
  }
}
