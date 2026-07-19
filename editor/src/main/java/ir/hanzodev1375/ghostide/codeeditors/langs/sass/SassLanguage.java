/**
 * Comment by ghost ide
 *
 * @author: Ninjacoder
 */
package ir.hanzodev1375.ghostide.codeeditors.langs.sass;

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
import io.github.rosemoe.sora.lang.completion.SimpleSnippetCompletionItem;
import io.github.rosemoe.sora.lang.completion.SnippetDescription;
import io.github.rosemoe.sora.lang.completion.snippet.CodeSnippet;
import io.github.rosemoe.sora.lang.completion.snippet.parser.CodeSnippetParser;
import io.github.rosemoe.sora.lang.format.AsyncFormatter;
import io.github.rosemoe.sora.lang.format.Formatter;
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandler;
import io.github.rosemoe.sora.text.CharPosition;
import io.github.rosemoe.sora.text.Content;
import io.github.rosemoe.sora.lang.styling.Styles;
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandleResult;
import io.github.rosemoe.sora.lang.styling.StylesUtils;
import io.github.rosemoe.sora.text.TextRange;
import io.github.rosemoe.sora.text.TextUtils;
import io.github.rosemoe.sora.text.ContentReference;
import io.github.rosemoe.sora.util.MyCharacter;
import io.github.rosemoe.sora.widget.SymbolPairMatch;
import ir.hanzodev1375.ghostide.codeeditors.langs.formatHelp.PrettierFormatter;
import ir.hanzodev1375.ghostide.codeeditors.lspcustomhot.Css3Server;
import java.util.List;
import ir.hanzodev1375.ghostide.codeeditors.lspcustomhot.CustomCompletionItem;

public class SassLanguage implements Language {

  private static final CodeSnippet MIXIN_SNIPPET =
      CodeSnippetParser.parse("@mixin ${1:name}(${2:args}) {\n\t${3:styles}\n}\n$0");

  private static final CodeSnippet INCLUDE_SNIPPET =
      CodeSnippetParser.parse("@include ${1:name}(${2:args});$0");

  private static final CodeSnippet IF_SNIPPET =
      CodeSnippetParser.parse("@if ${1:condition} {\n\t${2:styles}\n}\n$0");

  private static final CodeSnippet EACH_SNIPPET =
      CodeSnippetParser.parse("@each $${1:item} in $${2:list} {\n\t${3:styles}\n}\n$0");

  private static final CodeSnippet FOR_SNIPPET =
      CodeSnippetParser.parse("@for $${1:i} from ${2:1} through ${3:10} {\n\t${4:styles}\n}\n$0");

  private static final CodeSnippet FUNCTION_SNIPPET =
      CodeSnippetParser.parse("@function ${1:name}(${2:args}) {\n\t@return ${3:value};\n}\n$0");

  private IdentifierAutoComplete autoComplete;

  private final SassIncrementalAnalyzeManager manager;
  private Context context;
  private final SassQuoteHandler quoteHandler = new SassQuoteHandler();

  private static final String[] KEYWORDS = {
    "@mixin",
    "@include",
    "@if",
    "@else",
    "@for",
    "@each",
    "@while",
    "@function",
    "@return",
    "@extend",
    "@import",
    "@use",
    "@forward",
    "true",
    "false",
    "null"
  };

  public SassLanguage(Context context) {
    this.context = context;
    autoComplete = new IdentifierAutoComplete(KEYWORDS);
    manager = new SassIncrementalAnalyzeManager();
  }

  @NonNull
  @Override
  public AnalyzeManager getAnalyzeManager() {
    return manager;
  }

  @Nullable
  @Override
  public QuickQuoteHandler getQuickQuoteHandler() {
    return quoteHandler;
  }

  @Override
  public void destroy() {
    autoComplete = null;
  }

  private final Formatter format =
      new AsyncFormatter() {
        @Nullable
        @Override
        public TextRange formatAsync(@NonNull Content text, @NonNull TextRange cursorRange) {
          var formatted = new PrettierFormatter();
          String formatResult = formatted.format(context, text.toString(), "sass");
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
  public int getInterruptionLevel() {
    return INTERRUPTION_LEVEL_STRONG;
  }

  @Override
  public void requireAutoComplete(
      @NonNull ContentReference content,
      @NonNull CharPosition position,
      @NonNull CompletionPublisher publisher,
      @NonNull Bundle extraArguments) {
    var prefix =
        CompletionHelper.computePrefix(content, position, MyCharacter::isJavaIdentifierPart);
    if (autoComplete != null) {}
    if ("mixin".startsWith(prefix) && !prefix.isEmpty())
      publisher.addItem(
          new SimpleSnippetCompletionItem(
              "mixin",
              "Snippet - Mixin",
              new SnippetDescription(prefix.length(), MIXIN_SNIPPET, true)));
    if ("include".startsWith(prefix) && !prefix.isEmpty())
      publisher.addItem(
          new SimpleSnippetCompletionItem(
              "include",
              "Snippet - Include",
              new SnippetDescription(prefix.length(), INCLUDE_SNIPPET, true)));
    if ("if".startsWith(prefix) && !prefix.isEmpty())
      publisher.addItem(
          new SimpleSnippetCompletionItem(
              "if", "Snippet - If", new SnippetDescription(prefix.length(), IF_SNIPPET, true)));
    if ("each".startsWith(prefix) && !prefix.isEmpty())
      publisher.addItem(
          new SimpleSnippetCompletionItem(
              "each",
              "Snippet - Each",
              new SnippetDescription(prefix.length(), EACH_SNIPPET, true)));
    if ("for".startsWith(prefix) && !prefix.isEmpty())
      publisher.addItem(
          new SimpleSnippetCompletionItem(
              "for", "Snippet - For", new SnippetDescription(prefix.length(), FOR_SNIPPET, true)));
    if ("function".startsWith(prefix) && !prefix.isEmpty())
      publisher.addItem(
          new SimpleSnippetCompletionItem(
              "function",
              "Snippet - Function",
              new SnippetDescription(prefix.length(), FUNCTION_SNIPPET, true)));
    Css3Server cssServer = new Css3Server(context);
    List<CustomCompletionItem> cssItems = cssServer.getCompletions(prefix);
    for (var item : cssItems) {
      publisher.addItem(item);
    }
  }

  @Override
  public int getIndentAdvance(@NonNull ContentReference text, int line, int column) {
    var content = text.getLine(line).substring(0, column);
    return getIndentAdvance(content);
  }

  private int getIndentAdvance(String content) {
    var t = new SassTextTokenizer(content);
    SassTokens token;
    int advance = 0;
    while ((token = t.nextToken()) != SassTokens.EOF) {
      if (token == SassTokens.LBRACE) {
        advance++;
      }
    }
    advance = Math.max(0, advance);
    return advance * 2;
  }

  private final NewlineHandler[] newlineHandlers = new NewlineHandler[] {new BraceHandler()};

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

  @Override
  public NewlineHandler[] getNewlineHandlers() {
    return newlineHandlers;
  }

  private static String getNonEmptyTextBefore(CharSequence text, int index, int length) {
    while (index > 0 && Character.isWhitespace(text.charAt(index - 1))) {
      index--;
    }
    return text.subSequence(Math.max(0, index - length), index).toString();
  }

  private static String getNonEmptyTextAfter(CharSequence text, int index, int length) {
    while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
      index++;
    }
    return text.subSequence(index, Math.min(index + length, text.length())).toString();
  }

  class BraceHandler implements NewlineHandler {

    @Override
    public boolean matchesRequirement(
        @NonNull Content text, @NonNull CharPosition position, @Nullable Styles style) {
      var line = text.getLine(position.line);
      return !StylesUtils.checkNoCompletion(style, position)
          && getNonEmptyTextBefore(line, position.column, 1).equals("{")
          && getNonEmptyTextAfter(line, position.column, 1).equals("}");
    }

    @NonNull
    @Override
    public NewlineHandleResult handleNewline(
        @NonNull Content text,
        @NonNull CharPosition position,
        @Nullable Styles style,
        int tabSize) {
      var line = text.getLine(position.line);
      int index = position.column;
      var beforeText = line.subSequence(0, index).toString();
      var afterText = line.subSequence(index, line.length()).toString();
      return handleNewline(beforeText, afterText, tabSize);
    }

    @NonNull
    public NewlineHandleResult handleNewline(String beforeText, String afterText, int tabSize) {
      int count = TextUtils.countLeadingSpaceCount(beforeText, tabSize);
      int advanceBefore = getIndentAdvance(beforeText);
      int advanceAfter = getIndentAdvance(afterText);
      String text;
      StringBuilder sb =
          new StringBuilder("\n")
              .append(TextUtils.createIndent(count + advanceBefore, tabSize, useTab()))
              .append('\n')
              .append(text = TextUtils.createIndent(count + advanceAfter, tabSize, useTab()));
      int shiftLeft = text.length() + 1;
      return new NewlineHandleResult(sb, shiftLeft);
    }
  }
}
