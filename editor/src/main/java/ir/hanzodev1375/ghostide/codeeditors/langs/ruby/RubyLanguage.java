/**
 * Comment by ghost ide
 *
 * @author: Ninjacoder
 */
package ir.hanzodev1375.ghostide.codeeditors.langs.ruby;

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
import java.io.StringReader;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;

public class RubyLanguage implements Language {

  private static final CodeSnippet DEF_SNIPPET =
      CodeSnippetParser.parse("def ${1:method_name}(${2:params})\n    $0\nend");

  private static final CodeSnippet CLASS_SNIPPET =
      CodeSnippetParser.parse("class ${1:ClassName}\n    $0\nend");

  private static final CodeSnippet MODULE_SNIPPET =
      CodeSnippetParser.parse("module ${1:ModuleName}\n    $0\nend");

  private static final CodeSnippet IF_SNIPPET =
      CodeSnippetParser.parse("if ${1:condition}\n    $0\nend");

  private static final CodeSnippet IF_ELSE_SNIPPET =
      CodeSnippetParser.parse("if ${1:condition}\n    $0\nelse\n    \nend");

  private static final CodeSnippet UNLESS_SNIPPET =
      CodeSnippetParser.parse("unless ${1:condition}\n    $0\nend");

  private static final CodeSnippet CASE_SNIPPET =
      CodeSnippetParser.parse("case ${1:value}\nwhen ${2:pattern}\n    $0\nend");

  private static final CodeSnippet FOR_SNIPPET =
      CodeSnippetParser.parse("for ${1:item} in ${2:collection}\n    $0\nend");

  private static final CodeSnippet WHILE_SNIPPET =
      CodeSnippetParser.parse("while ${1:condition}\n    $0\nend");

  private static final CodeSnippet UNTIL_SNIPPET =
      CodeSnippetParser.parse("until ${1:condition}\n    $0\nend");

  private static final CodeSnippet DO_SNIPPET = CodeSnippetParser.parse("do\n    $0\nend");

  private static final String[] KEYWORDS = {
    "alias",
    "and",
    "begin",
    "break",
    "case",
    "class",
    "def",
    "defined?",
    "do",
    "else",
    "elsif",
    "end",
    "ensure",
    "false",
    "for",
    "if",
    "in",
    "module",
    "next",
    "nil",
    "not",
    "or",
    "redo",
    "rescue",
    "retry",
    "return",
    "self",
    "super",
    "then",
    "true",
    "undef",
    "unless",
    "until",
    "when",
    "while",
    "yield",
    "__ENCODING__",
    "__LINE__",
    "__FILE__"
  };

  private IdentifierAutoComplete autoComplete;

  private final RubyAnalyzer manager;

  private final RubyQuoteHandler quoteHandler = new RubyQuoteHandler();
  private Context context;

  public RubyLanguage(Context context) {
    this.context = context;
    autoComplete = new IdentifierAutoComplete(KEYWORDS);
    manager = new RubyAnalyzer();
  }

  private final Formatter formatter =
      new AsyncFormatter() {
        @Nullable
        @Override
        public TextRange formatAsync(@NonNull Content text, @NonNull TextRange cursorRange) {
          var formatted = new RubyFormatter();
          String formatResult = formatted.formatRuby(context, text.toString());

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

    autoComplete.requireAutoComplete(
        content, position, prefix, publisher, manager.getSyncIdentifiers());

    if ("def".startsWith(prefix) && !prefix.isEmpty())
      publisher.addItem(
          new SimpleSnippetCompletionItem(
              "def",
              "Snippet - Define method",
              new SnippetDescription(prefix.length(), DEF_SNIPPET, true)));
    if ("class".startsWith(prefix) && !prefix.isEmpty())
      publisher.addItem(
          new SimpleSnippetCompletionItem(
              "class",
              "Snippet - Class",
              new SnippetDescription(prefix.length(), CLASS_SNIPPET, true)));
    if ("module".startsWith(prefix) && !prefix.isEmpty())
      publisher.addItem(
          new SimpleSnippetCompletionItem(
              "module",
              "Snippet - Module",
              new SnippetDescription(prefix.length(), MODULE_SNIPPET, true)));
    if ("if".startsWith(prefix) && !prefix.isEmpty())
      publisher.addItem(
          new SimpleSnippetCompletionItem(
              "if", "Snippet - If", new SnippetDescription(prefix.length(), IF_SNIPPET, true)));
    if ("ife".startsWith(prefix) && !prefix.isEmpty())
      publisher.addItem(
          new SimpleSnippetCompletionItem(
              "ife",
              "Snippet - If/Else",
              new SnippetDescription(prefix.length(), IF_ELSE_SNIPPET, true)));
    if ("unless".startsWith(prefix) && !prefix.isEmpty())
      publisher.addItem(
          new SimpleSnippetCompletionItem(
              "unless",
              "Snippet - Unless",
              new SnippetDescription(prefix.length(), UNLESS_SNIPPET, true)));
    if ("case".startsWith(prefix) && !prefix.isEmpty())
      publisher.addItem(
          new SimpleSnippetCompletionItem(
              "case",
              "Snippet - Case",
              new SnippetDescription(prefix.length(), CASE_SNIPPET, true)));
    if ("for".startsWith(prefix) && !prefix.isEmpty())
      publisher.addItem(
          new SimpleSnippetCompletionItem(
              "for", "Snippet - For", new SnippetDescription(prefix.length(), FOR_SNIPPET, true)));
    if ("while".startsWith(prefix) && !prefix.isEmpty())
      publisher.addItem(
          new SimpleSnippetCompletionItem(
              "while",
              "Snippet - While",
              new SnippetDescription(prefix.length(), WHILE_SNIPPET, true)));
    if ("until".startsWith(prefix) && !prefix.isEmpty())
      publisher.addItem(
          new SimpleSnippetCompletionItem(
              "until",
              "Snippet - Until",
              new SnippetDescription(prefix.length(), UNTIL_SNIPPET, true)));
    if ("do".startsWith(prefix) && !prefix.isEmpty())
      publisher.addItem(
          new SimpleSnippetCompletionItem(
              "do",
              "Snippet - Do block",
              new SnippetDescription(prefix.length(), DO_SNIPPET, true)));
  }

  @Override
  public int getIndentAdvance(@NonNull ContentReference text, int line, int column) {
    var content = text.getLine(line).substring(0, column);
    return getIndentAdvance(content);
  }

  int getIndentAdvance(String content) {
    try {
      var lexer = new Ruby(CharStreams.fromReader(new StringReader(content)));
      Token token;
      int advance = 0;

      while ((token = lexer.nextToken()) != null && token.getType() != Token.EOF) {
        if (token.getType() == Ruby.END) advance++;
      }
      return Math.max(0, advance) * 2;

    } catch (Exception e) {
      return 0;
    }
  }

  private final NewlineHandler[] newlineHandlers = new NewlineHandler[] {new BraceHandler()};

  @Override
  public NewlineHandler[] getNewlineHandlers() {
    return newlineHandlers;
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
