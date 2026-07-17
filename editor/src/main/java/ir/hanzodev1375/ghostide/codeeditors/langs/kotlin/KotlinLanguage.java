package ir.hanzodev1375.ghostide.codeeditors.langs.kotlin;

import android.content.Context;
import io.github.rosemoe.sora.lang.format.AsyncFormatter;
import io.github.rosemoe.sora.text.TextRange;
import static java.lang.Character.isWhitespace;

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
import io.github.rosemoe.sora.lang.format.Formatter;
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandleResult;
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandler;
import io.github.rosemoe.sora.lang.styling.Styles;
import io.github.rosemoe.sora.lang.styling.StylesUtils;
import io.github.rosemoe.sora.text.CharPosition;
import io.github.rosemoe.sora.text.Content;
import io.github.rosemoe.sora.text.ContentReference;
import io.github.rosemoe.sora.text.TextUtils;
import io.github.rosemoe.sora.util.MyCharacter;
import io.github.rosemoe.sora.widget.SymbolPairMatch;

import java.util.ArrayList;
import java.util.List;

public class KotlinLanguage implements Language {

  private static final CodeSnippet FOR_SNIPPET =
      CodeSnippetParser.parse("for (${1:i} in ${2:0} until ${3:10}) {\n    $0\n}");
  private static final CodeSnippet FUN_SNIPPET =
      CodeSnippetParser.parse("fun ${1:name}(${2:params}) {\n    $0\n}");
  private static final CodeSnippet IF_SNIPPET =
      CodeSnippetParser.parse("if (${1:condition}) {\n    $0\n}");

  private IdentifierAutoComplete autoComplete;
  private final KotlinIncrementalAnalyzeManager manager;
  private final KotlinQuoteHandler quoteHandler = new KotlinQuoteHandler();
  private Context context;
  private static final String[] KEYWORDS = {
    "as",
    "break",
    "class",
    "continue",
    "do",
    "else",
    "enum",
    "extends",
    "false",
    "final",
    "finally",
    "for",
    "fun",
    "if",
    "import",
    "in",
    "interface",
    "is",
    "null",
    "object",
    "package",
    "private",
    "protected",
    "public",
    "return",
    "sealed",
    "super",
    "this",
    "throw",
    "throws",
    "true",
    "try",
    "typealias",
    "val",
    "var",
    "when",
    "while",
    "data",
    "inline",
    "open"
  };

  public KotlinLanguage(Context context) {
    this.context = context;
    autoComplete = new IdentifierAutoComplete(KEYWORDS);
    manager = new KotlinIncrementalAnalyzeManager();
  }

  private final Formatter formatter =
      new AsyncFormatter() {
        @Nullable
        @Override
        public TextRange formatAsync(@NonNull Content text, @NonNull TextRange cursorRange) {
          var formatted = new KtFormater();
          String formatResult = formatted.formatKotlin(context, text.toString());

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
    final var idt = manager.identifiers;
    if (idt != null && autoComplete != null) {
      autoComplete.requireAutoComplete(content, position, prefix, publisher, idt);
    }
    if ("fori".startsWith(prefix) && !prefix.isEmpty()) {
      publisher.addItem(
          new SimpleSnippetCompletionItem(
              "fori",
              "Snippet - For loop",
              new SnippetDescription(prefix.length(), FOR_SNIPPET, true)));
    }
    if ("fun".startsWith(prefix) && !prefix.isEmpty()) {
      publisher.addItem(
          new SimpleSnippetCompletionItem(
              "fun",
              "Snippet - Function",
              new SnippetDescription(prefix.length(), FUN_SNIPPET, true)));
    }
    if ("if".startsWith(prefix) && !prefix.isEmpty()) {
      publisher.addItem(
          new SimpleSnippetCompletionItem(
              "if",
              "Snippet - If statement",
              new SnippetDescription(prefix.length(), IF_SNIPPET, true)));
    }
  }

  @Override
  public int getIndentAdvance(@NonNull ContentReference text, int line, int column) {
    var content = text.getLine(line).substring(0, column);
    return getIndentAdvance(content);
  }

  private int getIndentAdvance(String content) {
    KotlinTextTokenizer t = new KotlinTextTokenizer(content);
    Tokens token;
    int advance = 0;
    while ((token = t.nextToken()) != Tokens.EOF) {
      if (token == Tokens.LBRACE) {
        advance++;
      }
    }
    advance = Math.max(0, advance);
    return advance * 4;
  }

  private final NewlineHandler[] newlineHandlers = new NewlineHandler[] {new BraceHandler()};

  @Override
  public boolean useTab() {
    return true;
  }

  @NonNull
  @Override
  public Formatter getFormatter() {
    return formatter;
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
    while (index > 0 && isWhitespace(text.charAt(index - 1))) {
      index--;
    }
    return text.subSequence(Math.max(0, index - length), index).toString();
  }

  private static String getNonEmptyTextAfter(CharSequence text, int index, int length) {
    while (index < text.length() && isWhitespace(text.charAt(index))) {
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
