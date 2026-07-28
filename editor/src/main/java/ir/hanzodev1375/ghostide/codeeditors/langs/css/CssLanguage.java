package ir.hanzodev1375.ghostide.codeeditors.langs.css;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.github.rosemoe.sora.lang.Language;
import io.github.rosemoe.sora.lang.QuickQuoteHandler;
import io.github.rosemoe.sora.lang.analysis.AnalyzeManager;
import io.github.rosemoe.sora.lang.completion.CompletionHelper;
import io.github.rosemoe.sora.lang.completion.CompletionPublisher;
import io.github.rosemoe.sora.lang.completion.IdentifierAutoComplete;
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
import ir.hanzodev1375.ghostide.codeeditors.langs.formatHelp.PrettierFormatter;
import ir.hanzodev1375.ghostide.codeeditors.langs.html.HtmlIncrementalAnalyzeManager;
import ir.hanzodev1375.ghostide.codeeditors.langs.sass.SassIncrementalAnalyzeManager;
import ir.hanzodev1375.ghostide.codeeditors.langs.sass.SassTextTokenizer;
import ir.hanzodev1375.ghostide.codeeditors.langs.sass.SassTokens;
import java.io.IOException;
import java.io.StringReader;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandleResult;
import io.github.rosemoe.sora.lang.styling.Styles;

public class CssLanguage implements Language {

  private final SassIncrementalAnalyzeManager analyzer;
  private final IdentifierAutoComplete autoComplete;
  private Context context;
  private String path;

  public CssLanguage(Context context, String path) {
    String[] htmlKeywords = {"!", "DOCTYPE"};
    autoComplete = new IdentifierAutoComplete(htmlKeywords);
    analyzer = new SassIncrementalAnalyzeManager();
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
  }

  @Override
  public int getIndentAdvance(@NonNull ContentReference text, int line, int column) {
    var tokenizer = new SassTextTokenizer(text.getLine(line));
    SassTokens token;
    int advance = 0;
    while ((token = tokenizer.nextToken()) != SassTokens.EOF) {
      switch (token) {
        case LBRACE:
          advance++;
          break;
        case RBRACK:
          advance--;
          break;
        default:
          break;
      }
    }
    advance = Math.max(0, advance);
    return advance * 2;
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
