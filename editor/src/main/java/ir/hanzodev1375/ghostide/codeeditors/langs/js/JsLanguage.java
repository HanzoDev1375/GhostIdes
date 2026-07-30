package ir.hanzodev1375.ghostide.codeeditors.langs.js;

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
import io.github.rosemoe.sora.lang.format.Formatter;
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandler;
import io.github.rosemoe.sora.text.CharPosition;
import io.github.rosemoe.sora.text.ContentReference;
import io.github.rosemoe.sora.widget.SymbolPairMatch;
import ir.hanzodev1375.ghostide.codeeditors.langs.antlr4base.CharParser;
import ir.hanzodev1375.ghostide.codeeditors.langs.html.HtmlIncrementalAnalyzeManager;
import ir.hanzodev1375.ghostide.codeeditors.langs.html.HtmlTextTokenizer;
import ir.hanzodev1375.ghostide.codeeditors.langs.html.HtmlTokens;
import ir.hanzodev1375.ghostide.codeeditors.langs.sass.SassTextTokenizer;
import ir.hanzodev1375.ghostide.codeeditors.langs.sass.SassTokens;
import ir.hanzodev1375.ghostide.codeeditors.langs.tsx.TsxIncrementalAnalyzeManager;
import java.io.IOException;
import java.io.StringReader;
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandleResult;
import io.github.rosemoe.sora.lang.styling.Styles;
import io.github.rosemoe.sora.text.Content;
import io.github.rosemoe.sora.text.TextUtils;

public class JsLanguage implements Language {

  private final TsxIncrementalAnalyzeManager analyzer;
  private final IdentifierAutoComplete autoComplete;
  private Context context;
  private String path;

  public JsLanguage(Context context, String path) {
    String[] htmlKeywords = {"!", "DOCTYPE"};
    autoComplete = new IdentifierAutoComplete(htmlKeywords);
    analyzer = new TsxIncrementalAnalyzeManager();
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
    String prefix = CompletionHelper.computePrefix(content, position, CharParser::parserJava);
    return;
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
    return EmptyLanguage.EmptyFormatter.INSTANCE;
  }

  @Override
  public SymbolPairMatch getSymbolPairs() {
    return new SymbolPairMatch.DefaultSymbolPairs();
  }

  private class JsBraceHandler implements NewlineHandler {

    @Override
    public boolean matchesRequirement(
        @NonNull Content text, @NonNull CharPosition position, @Nullable Styles style) {

      int line = position.line;

      if (line < 0 || line >= text.getLineCount()) {
        return false;
      }

      CharSequence lineText = text.getLine(line);

      String before = lineText.subSequence(0, position.column).toString();

      String after = lineText.subSequence(position.column, lineText.length()).toString();

      return before.trim().endsWith("{") && after.trim().startsWith("}");
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

      String innerIndent = TextUtils.createIndent(indent + tabSize, tabSize, false);

      String closeIndent = TextUtils.createIndent(indent, tabSize, false);

      StringBuilder sb =
          new StringBuilder().append('\n').append(innerIndent).append('\n').append(closeIndent);

      int shiftLeft = closeIndent.length() + 1;

      return new NewlineHandleResult(sb, shiftLeft);
    }
  }

  @Override
  public NewlineHandler[] getNewlineHandlers() {
    return new NewlineHandler[] {new JsBraceHandler()};
  }
}
