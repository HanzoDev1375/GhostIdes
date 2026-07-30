/**
 * Comment by ghost ide
 *
 * @author: Ninjacoder
 */
package ir.hanzodev1375.ghostide.codeeditors.langs.go;

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
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandleResult;
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandler;
import io.github.rosemoe.sora.lang.styling.Styles;
import io.github.rosemoe.sora.lang.styling.StylesUtils;
import io.github.rosemoe.sora.text.CharPosition;
import io.github.rosemoe.sora.text.Content;
import io.github.rosemoe.sora.text.ContentReference;
import io.github.rosemoe.sora.text.TextRange;
import io.github.rosemoe.sora.text.TextUtils;
import io.github.rosemoe.sora.util.MyCharacter;
import io.github.rosemoe.sora.widget.SymbolPairMatch;

public class GoLanguage implements Language {

  private static final CodeSnippet FOR_SNIPPET =
      CodeSnippetParser.parse("for ${1:i} := ${2:0}; ${1:i} < ${3:10}; ${1:i}++ {\n    $0\n}");

  private static final CodeSnippet FOR_RANGE_SNIPPET =
      CodeSnippetParser.parse("for ${1:index}, ${2:value} := range ${3:slice} {\n    $0\n}");

  private static final CodeSnippet FUNC_SNIPPET =
      CodeSnippetParser.parse("func ${1:name}(${2:params}) ${3:returnType} {\n    $0\n}");

  private static final CodeSnippet IF_SNIPPET =
      CodeSnippetParser.parse("if ${1:condition} {\n    $0\n}");

  private static final CodeSnippet SWITCH_SNIPPET =
      CodeSnippetParser.parse("switch ${1:value} {\ncase ${2:case}:\n    $0\n}");

  private static final CodeSnippet STRUCT_SNIPPET =
      CodeSnippetParser.parse("type ${1:name} struct {\n    ${2:fields}\n}\n$0");

  private static final CodeSnippet DEFER_SNIPPET =
      CodeSnippetParser.parse("defer ${1:func}(${2:args})$0");
  private Context editor;
  private static final String[] KEYWORDS = {
    "break",
    "case",
    "chan",
    "const",
    "continue",
    "default",
    "defer",
    "else",
    "fallthrough",
    "for",
    "func",
    "go",
    "goto",
    "if",
    "import",
    "interface",
    "map",
    "package",
    "range",
    "return",
    "select",
    "struct",
    "switch",
    "type",
    "var",
    "nil",
    "true",
    "false"
  };

  private IdentifierAutoComplete autoComplete;

  private final GoIncrementalAnalyzeManager manager;

  private final GoQuoteHandler quoteHandler = new GoQuoteHandler();

  public GoLanguage(Context editor) {
    this.editor = editor;
    autoComplete = new IdentifierAutoComplete(KEYWORDS);
    manager = new GoIncrementalAnalyzeManager();
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

  @Override
  public int getInterruptionLevel() {
    return INTERRUPTION_LEVEL_STRONG;
  }

  @Override
  public boolean useTab() {
    return true;
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
    if ("for".startsWith(prefix) && !prefix.isEmpty())
      publisher.addItem(
          new SimpleSnippetCompletionItem(
              "for", "Snippet - For", new SnippetDescription(prefix.length(), FOR_SNIPPET, true)));
    if ("forr".startsWith(prefix) && !prefix.isEmpty())
      publisher.addItem(
          new SimpleSnippetCompletionItem(
              "forr",
              "Snippet - For Range",
              new SnippetDescription(prefix.length(), FOR_RANGE_SNIPPET, true)));
    if ("func".startsWith(prefix) && !prefix.isEmpty())
      publisher.addItem(
          new SimpleSnippetCompletionItem(
              "func",
              "Snippet - Function",
              new SnippetDescription(prefix.length(), FUNC_SNIPPET, true)));
    if ("if".startsWith(prefix) && !prefix.isEmpty())
      publisher.addItem(
          new SimpleSnippetCompletionItem(
              "if", "Snippet - If", new SnippetDescription(prefix.length(), IF_SNIPPET, true)));
    if ("switch".startsWith(prefix) && !prefix.isEmpty())
      publisher.addItem(
          new SimpleSnippetCompletionItem(
              "switch",
              "Snippet - Switch",
              new SnippetDescription(prefix.length(), SWITCH_SNIPPET, true)));
    if ("struct".startsWith(prefix) && !prefix.isEmpty())
      publisher.addItem(
          new SimpleSnippetCompletionItem(
              "struct",
              "Snippet - Struct",
              new SnippetDescription(prefix.length(), STRUCT_SNIPPET, true)));
    if ("defer".startsWith(prefix) && !prefix.isEmpty())
      publisher.addItem(
          new SimpleSnippetCompletionItem(
              "defer",
              "Snippet - Defer",
              new SnippetDescription(prefix.length(), DEFER_SNIPPET, true)));
  }

  @Override
  public int getIndentAdvance(@NonNull ContentReference text, int line, int column) {
    var content = text.getLine(line).substring(0, column);
    return getIndentAdvance(content);
  }

  private int getIndentAdvance(String content) {
    GoTextTokenizer t = new GoTextTokenizer(content);
    GoTokens token;
    int advance = 0;
    while ((token = t.nextToken()) != GoTokens.EOF) {
      if (token == GoTokens.LBRACE) advance++;
    }
    return Math.max(0, advance) * 4;
  }

  private final NewlineHandler[] newlineHandlers = new NewlineHandler[] {new BraceHandler()};

  @Override
  public NewlineHandler[] getNewlineHandlers() {
    return newlineHandlers;
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
