package ir.hanzodev1375.ghostide.codeeditors.langs.cpp;

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
import io.github.rosemoe.sora.widget.CodeEditor;
import io.github.rosemoe.sora.widget.SymbolPairMatch;
import static java.lang.Character.isWhitespace;

/**
 * C++ language. Hand-written implementation, matching the same architecture used by the C, Rust, Go
 * and other native-tokenizer languages.
 *
 * @author Rosemoe
 */
public class CppLanguage implements Language {

  private static final CodeSnippet FOR_SNIPPET =
      CodeSnippetParser.parse("for(int ${1:i} = 0;$1 < ${2:count};$1++) {\n    $0\n}");
  private static final CodeSnippet STATIC_CONST_SNIPPET =
      CodeSnippetParser.parse(
          "private final static ${1:type} ${2/(.*)/${1:/upcase}/} = ${3:value};");
  private static final CodeSnippet CLIPBOARD_SNIPPET = CodeSnippetParser.parse("${1:${CLIPBOARD}}");

  private Context editor;
  private CodeEditor codeEditorRef;
  private IdentifierAutoComplete autoComplete;
  private final CppIncrementalAnalyzeManager manager;
  private final CppQuoteHandler quoteHandler = new CppQuoteHandler();

  public CppLanguage(Context editor) {
    this(editor, null);
  }

  public CppLanguage(Context editor, CodeEditor codeEditor) {
    this.editor = editor;
    this.codeEditorRef = codeEditor;
    String[] cppkeyword = {"const", "if", "for"};
    autoComplete = new IdentifierAutoComplete(cppkeyword);
    manager = new CppIncrementalAnalyzeManager();
  }

  private final Formatter formatter =
      new AsyncFormatter() {
        @Nullable
        @Override
        public TextRange formatAsync(@NonNull Content text, @NonNull TextRange cursorRange) {
          CppFormatter formatted = new CppFormatter();
          String formatResult = formatted.formatCpp(editor, text.toString(), "google");

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

  private static boolean isIdentifierChar(char c) {
    return MyCharacter.isJavaIdentifierPart(c) || c == '.';
  }

  @Override
  public void requireAutoComplete(
      @NonNull ContentReference content,
      @NonNull CharPosition position,
      @NonNull CompletionPublisher publisher,
      @NonNull Bundle extraArguments) {
    var prefix = CompletionHelper.computePrefix(content, position, CppLanguage::isIdentifierChar);
    final var idt = manager.identifiers;
    if (idt != null && autoComplete != null) {
      autoComplete.requireAutoComplete(content, position, prefix, publisher, idt);
    }
    if ("fori".startsWith(prefix) && prefix.length() > 0) {
      publisher.addItem(
          new SimpleSnippetCompletionItem(
              "fori",
              "Snippet - For loop on index",
              new SnippetDescription(prefix.length(), FOR_SNIPPET, true)));
    }
    if ("sconst".startsWith(prefix) && prefix.length() > 0) {
      publisher.addItem(
          new SimpleSnippetCompletionItem(
              "sconst",
              "Snippet - Static Constant",
              new SnippetDescription(prefix.length(), STATIC_CONST_SNIPPET, true)));
    }
    if ("clip".startsWith(prefix) && prefix.length() > 0) {
      publisher.addItem(
          new SimpleSnippetCompletionItem(
              "clip",
              "Snippet - Clipboard contents",
              new SnippetDescription(prefix.length(), CLIPBOARD_SNIPPET, true)));
    }
  }

  @Override
  public int getIndentAdvance(@NonNull ContentReference text, int line, int column) {
    var content = text.getLine(line).substring(0, column);
    return getIndentAdvance(content);
  }

  private int getIndentAdvance(String content) {
    CppTextTokenizer t = new CppTextTokenizer(content);
    CppTokens token;
    int advance = 0;
    while ((token = t.nextToken()) != CppTokens.EOF) {
      if (token == CppTokens.LBRACE) {
        advance++;
      } else if (token == CppTokens.RBRACE) {
        advance--;
      }
    }
    advance = Math.max(0, advance);
    return advance * 2;
  }

  private final NewlineHandler[] newlineHandlers = new NewlineHandler[] {new BraceHandler()};

  @Override
  public boolean useTab() {
    return false;
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
