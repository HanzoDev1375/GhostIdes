/**
 * Comment by ghost ide
 *
 * @author: Ninjacoder
 */
package ir.hanzodev1375.ghostide.codeeditors.langs.php;

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

public class PhpLanguage implements Language {

  private static final CodeSnippet FOR_SNIPPET =
      CodeSnippetParser.parse("for ($${1:i} = 0; $${1:i} < ${2:10}; $${1:i}++) {\n    $0\n}");

  private static final CodeSnippet FOREACH_SNIPPET =
      CodeSnippetParser.parse("foreach ($${1:array} as $${2:value}) {\n    $0\n}");

  private static final CodeSnippet FOREACH_KEY_SNIPPET =
      CodeSnippetParser.parse("foreach ($${1:array} as $${2:key} => $${3:value}) {\n    $0\n}");

  private static final CodeSnippet FUNCTION_SNIPPET =
      CodeSnippetParser.parse("function ${1:name}(${2:params}) {\n    $0\n}");

  private static final CodeSnippet IF_SNIPPET =
      CodeSnippetParser.parse("if (${1:condition}) {\n    $0\n}");

  private static final CodeSnippet ELSE_SNIPPET = CodeSnippetParser.parse("else {\n    $0\n}");

  private static final CodeSnippet ELSEIF_SNIPPET =
      CodeSnippetParser.parse("elseif (${1:condition}) {\n    $0\n}");

  private static final CodeSnippet SWITCH_SNIPPET =
      CodeSnippetParser.parse(
          "switch (${1:value}) {\n    case ${2:case}:\n        $0\n        break;\n}");

  private static final CodeSnippet CLASS_SNIPPET =
      CodeSnippetParser.parse("class ${1:name} {\n    $0\n}");

  private static final CodeSnippet NAMESPACE_SNIPPET =
      CodeSnippetParser.parse("namespace ${1:name};\n\n$0");
  private Context context;

  private static final String[] KEYWORDS = {
    "abstract",
    "and",
    "array",
    "as",
    "break",
    "callable",
    "case",
    "catch",
    "class",
    "clone",
    "const",
    "continue",
    "declare",
    "default",
    "die",
    "do",
    "echo",
    "else",
    "elseif",
    "empty",
    "enddeclare",
    "endfor",
    "endforeach",
    "endif",
    "endswitch",
    "endwhile",
    "eval",
    "exit",
    "extends",
    "final",
    "finally",
    "fn",
    "for",
    "foreach",
    "function",
    "global",
    "goto",
    "if",
    "implements",
    "include",
    "include_once",
    "instanceof",
    "insteadof",
    "interface",
    "isset",
    "list",
    "match",
    "namespace",
    "new",
    "or",
    "print",
    "private",
    "protected",
    "public",
    "readonly",
    "require",
    "require_once",
    "return",
    "static",
    "switch",
    "throw",
    "trait",
    "try",
    "unset",
    "use",
    "var",
    "while",
    "xor",
    "yield",
    "true",
    "false",
    "null",
    "parent",
    "self"
  };

  private IdentifierAutoComplete autoComplete;

  private final PhpIncrementalAnalyzeManager manager;

  private final PhpQuoteHandler quoteHandler = new PhpQuoteHandler();

  public PhpLanguage(Context context) {
    this.context = context;
    autoComplete = new IdentifierAutoComplete(KEYWORDS);
    manager = new PhpIncrementalAnalyzeManager();
  }

  private final Formatter phpFormatter =
      new AsyncFormatter() {
        @Nullable
        @Override
        public TextRange formatAsync(@NonNull Content text, @NonNull TextRange cursorRange) {
          PhpFormatter formatter = new PhpFormatter();
          String formatted = formatter.formatPhp(context, text.toString());

          if (formatted == null) {
            return cursorRange;
          }

          if (!text.toString().equals(formatted)) {
            int oldCursor = cursorRange.getStartIndex();
            text.delete(0, text.length());
            text.insert(0, 0, formatted);
            int newCursor = Math.min(oldCursor, formatted.length());
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
    final var idt = manager.identifiers;
    if (idt != null && autoComplete != null) {
      autoComplete.requireAutoComplete(content, position, prefix, publisher, idt);
    }
    if ("for".startsWith(prefix) && !prefix.isEmpty())
      publisher.addItem(
          new SimpleSnippetCompletionItem(
              "for", "Snippet - For", new SnippetDescription(prefix.length(), FOR_SNIPPET, true)));
    if ("foreach".startsWith(prefix) && !prefix.isEmpty())
      publisher.addItem(
          new SimpleSnippetCompletionItem(
              "foreach",
              "Snippet - Foreach",
              new SnippetDescription(prefix.length(), FOREACH_SNIPPET, true)));
    if ("foreachk".startsWith(prefix) && !prefix.isEmpty())
      publisher.addItem(
          new SimpleSnippetCompletionItem(
              "foreachk",
              "Snippet - Foreach with key",
              new SnippetDescription(prefix.length(), FOREACH_KEY_SNIPPET, true)));
    if ("func".startsWith(prefix) && !prefix.isEmpty())
      publisher.addItem(
          new SimpleSnippetCompletionItem(
              "func",
              "Snippet - Function",
              new SnippetDescription(prefix.length(), FUNCTION_SNIPPET, true)));
    if ("if".startsWith(prefix) && !prefix.isEmpty())
      publisher.addItem(
          new SimpleSnippetCompletionItem(
              "if", "Snippet - If", new SnippetDescription(prefix.length(), IF_SNIPPET, true)));
    if ("else".startsWith(prefix) && !prefix.isEmpty())
      publisher.addItem(
          new SimpleSnippetCompletionItem(
              "else",
              "Snippet - Else",
              new SnippetDescription(prefix.length(), ELSE_SNIPPET, true)));
    if ("elseif".startsWith(prefix) && !prefix.isEmpty())
      publisher.addItem(
          new SimpleSnippetCompletionItem(
              "elseif",
              "Snippet - Elseif",
              new SnippetDescription(prefix.length(), ELSEIF_SNIPPET, true)));
    if ("switch".startsWith(prefix) && !prefix.isEmpty())
      publisher.addItem(
          new SimpleSnippetCompletionItem(
              "switch",
              "Snippet - Switch",
              new SnippetDescription(prefix.length(), SWITCH_SNIPPET, true)));
    if ("class".startsWith(prefix) && !prefix.isEmpty())
      publisher.addItem(
          new SimpleSnippetCompletionItem(
              "class",
              "Snippet - Class",
              new SnippetDescription(prefix.length(), CLASS_SNIPPET, true)));
    if ("namespace".startsWith(prefix) && !prefix.isEmpty())
      publisher.addItem(
          new SimpleSnippetCompletionItem(
              "namespace",
              "Snippet - Namespace",
              new SnippetDescription(prefix.length(), NAMESPACE_SNIPPET, true)));
  }

  @Override
  public int getIndentAdvance(@NonNull ContentReference text, int line, int column) {
    var content = text.getLine(line).substring(0, column);
    return getIndentAdvance(content);
  }

  private int getIndentAdvance(String content) {
    PhpTextTokenizer t = new PhpTextTokenizer(content);
    PhpTokens token;
    int advance = 0;
    while ((token = t.nextToken()) != PhpTokens.EOF) {
      if (token == PhpTokens.LBRACE) advance++;
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
    return phpFormatter;
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
