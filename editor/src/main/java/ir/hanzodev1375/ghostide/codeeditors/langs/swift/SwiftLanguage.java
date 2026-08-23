/**
 * Comment by ghost ide
 *
 * @author: Ninjacoder
 */
package ir.hanzodev1375.ghostide.codeeditors.langs.swift;

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
 * Dedicated Swift language for GhostIde with \\(expr) interpolation coloring, nested block
 * comments and triple-quoted strings.
 */
public class SwiftLanguage implements Language {

  private static final CodeSnippet FUNC_SNIPPET =
      CodeSnippetParser.parse("func ${1:name}(${2:params}) -> ${3:Void} {\n    $0\n}");

  private static final CodeSnippet CLASS_SNIPPET =
      CodeSnippetParser.parse("class ${1:Name} {\n    $0\n}");

  private static final CodeSnippet STRUCT_SNIPPET =
      CodeSnippetParser.parse("struct ${1:Name} {\n    $0\n}");

  private static final CodeSnippet IF_ELSE_SNIPPET =
      CodeSnippetParser.parse("if ${1:condition} {\n    $0\n} else {\n    \n}");

  private static final CodeSnippet GUARD_SNIPPET =
      CodeSnippetParser.parse("guard let ${1:value} = ${2:optional} else { return }");

  private static final String[] KEYWORDS = {
    "associatedtype", "class", "deinit", "enum", "extension", "fileprivate", "func",
    "import", "init", "inout", "internal", "let", "open", "operator", "private",
    "precedencegroup", "protocol", "public", "rethrows", "static", "struct", "subscript",
    "typealias", "var", "break", "case", "catch", "continue", "default", "defer", "do",
    "else", "fallthrough", "for", "guard", "if", "in", "repeat", "return", "throw",
    "switch", "where", "while", "as", "is", "super", "self", "Self", "throws", "try",
    "await", "async", "actor", "some", "any", "weak", "unowned", "lazy", "willSet",
    "didSet", "get", "set", "mutating", "nonmutating", "required", "convenience",
    "override", "final", "indirect", "infix", "postfix", "prefix"
  };

  private IdentifierAutoComplete autoComplete;

  private final SwiftIncrementalAnalyzeManager manager;

  private final SwiftQuoteHandler quoteHandler = new SwiftQuoteHandler();

  public SwiftLanguage() {
    autoComplete = new IdentifierAutoComplete(KEYWORDS);
    manager = new SwiftIncrementalAnalyzeManager();
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
    addSnippet(publisher, prefix, "func", "Snippet - Function", FUNC_SNIPPET);
    addSnippet(publisher, prefix, "class", "Snippet - Class", CLASS_SNIPPET);
    addSnippet(publisher, prefix, "struct", "Snippet - Struct", STRUCT_SNIPPET);
    addSnippet(publisher, prefix, "ife", "Snippet - If/Else", IF_ELSE_SNIPPET);
    addSnippet(publisher, prefix, "guard", "Snippet - Guard Let", GUARD_SNIPPET);
  }

  @Override
  public int getIndentAdvance(@NonNull ContentReference text, int line, int column) {
    var content = text.getLine(line).substring(0, column);
    return getIndentAdvance(content);
  }

  int getIndentAdvance(String content) {
    int advance = 0;
    SwiftTextTokenizer t = new SwiftTextTokenizer(content);
    SwiftTokens token;
    while ((token = t.nextToken()) != SwiftTokens.EOF) {
      if (token == SwiftTokens.LBRACE) advance++;
      if (token == SwiftTokens.RBRACE) advance--;
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
