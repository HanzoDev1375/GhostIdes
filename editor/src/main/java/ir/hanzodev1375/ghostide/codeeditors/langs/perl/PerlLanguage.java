/**
 * Comment by ghost ide
 *
 * @author: Ninjacoder
 */
package ir.hanzodev1375.ghostide.codeeditors.langs.perl;

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

/**
 * Dedicated Perl language for GhostIde with sigil variable coloring ($var, @arr, %hash), string
 * interpolation coloring, heredoc and POD block support.
 */
public class PerlLanguage implements Language {

  private static final CodeSnippet SUB_SNIPPET =
      CodeSnippetParser.parse("sub ${1:name} {\n    my (${2}) = @_;\n    $0\n}");

  private static final CodeSnippet IF_ELSE_SNIPPET =
      CodeSnippetParser.parse("if (${1:condition}) {\n    $0\n} else {\n    \n}");

  private static final CodeSnippet FOREACH_SNIPPET =
      CodeSnippetParser.parse("foreach my $${1:item} (@${2:list}) {\n    $0\n}");

  private static final CodeSnippet WHILE_SNIPPET =
      CodeSnippetParser.parse("while (${1:condition}) {\n    $0\n}");

  private static final String[] KEYWORDS = {
    "my", "our", "local", "sub", "if", "elsif", "else", "unless", "while", "until",
    "for", "foreach", "do", "last", "next", "redo", "goto", "return", "wantarray",
    "use", "no", "require", "package", "bless", "ref", "defined", "undef", "exists",
    "delete", "each", "keys", "values", "and", "or", "not", "xor", "eq", "ne", "lt",
    "gt", "le", "ge", "cmp", "x", "__PACKAGE__", "__FILE__", "__LINE__", "__SUB__",
    "__DATA__", "__END__"
  };

  private IdentifierAutoComplete autoComplete;

  private final PerlIncrementalAnalyzeManager manager;

  private final PerlQuoteHandler quoteHandler = new PerlQuoteHandler();

  public PerlLanguage() {
    autoComplete = new IdentifierAutoComplete(KEYWORDS);
    manager = new PerlIncrementalAnalyzeManager();
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

  private void addSnippet(
      CompletionPublisher publisher,
      String prefix,
      String trigger,
      String label,
      CodeSnippet snippet) {
    if (!prefix.isEmpty() && trigger.startsWith(prefix)) {
      publisher.addItem(
          new SimpleSnippetCompletionItem(
              trigger, label, new SnippetDescription(prefix.length(), snippet, true)));
    }
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
    addSnippet(publisher, prefix, "sub", "Snippet - Subroutine", SUB_SNIPPET);
    addSnippet(publisher, prefix, "ife", "Snippet - If/Else", IF_ELSE_SNIPPET);
    addSnippet(publisher, prefix, "foreach", "Snippet - Foreach", FOREACH_SNIPPET);
    addSnippet(publisher, prefix, "while", "Snippet - While", WHILE_SNIPPET);
  }

  @Override
  public int getIndentAdvance(@NonNull ContentReference text, int line, int column) {
    var content = text.getLine(line).substring(0, column);
    return getIndentAdvance(content);
  }

  int getIndentAdvance(String content) {
    int advance = 0;
    PerlTextTokenizer t = new PerlTextTokenizer(content);
    PerlTokens token;
    while ((token = t.nextToken()) != PerlTokens.EOF) {
      if (token == PerlTokens.LBRACE) advance++;
      if (token == PerlTokens.RBRACE) advance--;
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
