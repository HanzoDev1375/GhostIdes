package ir.hanzodev1375.ghostide.codeeditors.langs.python3;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.github.rosemoe.sora.lang.format.AsyncFormatter;
import io.github.rosemoe.sora.text.TextRange;
import java.io.StringReader;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;

import io.github.rosemoe.sora.lang.EmptyLanguage;
import io.github.rosemoe.sora.lang.Language;
import io.github.rosemoe.sora.lang.QuickQuoteHandler;
import io.github.rosemoe.sora.lang.analysis.AnalyzeManager;
import io.github.rosemoe.sora.lang.completion.CompletionHelper;
import io.github.rosemoe.sora.lang.completion.CompletionPublisher;
import io.github.rosemoe.sora.lang.completion.IdentifierAutoComplete;
import io.github.rosemoe.sora.lang.completion.snippet.CodeSnippet;
import io.github.rosemoe.sora.lang.completion.snippet.parser.CodeSnippetParser;
import io.github.rosemoe.sora.lang.completion.SnippetDescription;
import io.github.rosemoe.sora.lang.format.Formatter;
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandler;
import io.github.rosemoe.sora.text.CharPosition;
import io.github.rosemoe.sora.text.ContentReference;
import io.github.rosemoe.sora.widget.SymbolPairMatch;
import ir.hanzodev1375.ghostide.codeeditors.langs.antlr4base.SnippetCompletionItem;
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandleResult;
import io.github.rosemoe.sora.lang.styling.Styles;
import io.github.rosemoe.sora.text.Content;
import io.github.rosemoe.sora.text.TextUtils;

public class Python3Language implements Language {

  private final Python3Analyzer analyzer;
  private final IdentifierAutoComplete autoComplete;

  private static final CodeSnippet SNIPPET_MAIN_GUARD =
      CodeSnippetParser.parse("if __name__ == '__main__':\n    $0");

  private static final CodeSnippet SNIPPET_FOR =
      CodeSnippetParser.parse("for ${1:item} in ${2:iterable}:\n    $0");

  private static final CodeSnippet SNIPPET_WHILE =
      CodeSnippetParser.parse("while ${1:condition}:\n    $0");

  private static final CodeSnippet SNIPPET_IF =
      CodeSnippetParser.parse("if ${1:condition}:\n    $0");

  private static final CodeSnippet SNIPPET_DEF =
      CodeSnippetParser.parse(
          "def ${1:func_name}(${2:params}):\n    \"\"\"${3:docstring}\"\"\"\n    $0");

  private static final CodeSnippet SNIPPET_CLASS =
      CodeSnippetParser.parse(
          "class ${1:ClassName}:\n    \"\"\"${2:docstring}\"\"\"\n    def __init__(self, ${3:params}):\n        $0");

  private static final CodeSnippet SNIPPET_TRY_EXCEPT =
      CodeSnippetParser.parse("try:\n    $0\nexcept ${1:Exception} as e:\n    ${2:pass}");

  private static final CodeSnippet SNIPPET_WITH =
      CodeSnippetParser.parse("with ${1:open('file.txt', 'r')} as ${2:f}:\n    $0");
  private Context context;

  public Python3Language(Context context) {
    this.context = context;
    String[] pythonKeywords = {
      "False",
      "None",
      "True",
      "and",
      "as",
      "assert",
      "async",
      "await",
      "break",
      "class",
      "continue",
      "def",
      "del",
      "elif",
      "else",
      "except",
      "finally",
      "for",
      "from",
      "global",
      "if",
      "import",
      "in",
      "is",
      "lambda",
      "nonlocal",
      "not",
      "or",
      "pass",
      "raise",
      "return",
      "try",
      "while",
      "with",
      "yield",
      "match",
      "case",
      "type"
    };
    autoComplete = new IdentifierAutoComplete(pythonKeywords);
    analyzer = new Python3Analyzer();
  }

  private final Formatter pythonFormatter =
      new AsyncFormatter() {
        @Nullable
        @Override
        public TextRange formatAsync(@NonNull Content text, @NonNull TextRange cursorRange) {
          PythonFormatter formatter = new PythonFormatter();
          String formatted = formatter.formatPython(context, text.toString());

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

    String prefix = CompletionHelper.computePrefix(content, position, this::isIdentifierChar);
    autoComplete.requireAutoComplete(
        content, position, prefix, publisher, analyzer.getSyncIdentifiers());
    if (prefix.length() > 0) {
      if ("main".startsWith(prefix)) {
        publisher.addItem(
            createSnippetItem(
                "main", "Snippet - if __name__ == '__main__'", SNIPPET_MAIN_GUARD, prefix));
      }
      if ("for".startsWith(prefix)) {
        publisher.addItem(createSnippetItem("for", "Snippet - for loop", SNIPPET_FOR, prefix));
      }
      if ("while".startsWith(prefix)) {
        publisher.addItem(
            createSnippetItem("while", "Snippet - while loop", SNIPPET_WHILE, prefix));
      }
      if ("if".startsWith(prefix) && !"elif".startsWith(prefix)) {
        publisher.addItem(createSnippetItem("if", "Snippet - if statement", SNIPPET_IF, prefix));
      }
      if ("def".startsWith(prefix)) {
        publisher.addItem(
            createSnippetItem("def", "Snippet - function definition", SNIPPET_DEF, prefix));
      }
      if ("class".startsWith(prefix)) {
        publisher.addItem(
            createSnippetItem("class", "Snippet - class definition", SNIPPET_CLASS, prefix));
      }
      if ("try".startsWith(prefix)) {
        publisher.addItem(
            createSnippetItem("try", "Snippet - try/except block", SNIPPET_TRY_EXCEPT, prefix));
      }
      if ("with".startsWith(prefix)) {
        publisher.addItem(
            createSnippetItem("with", "Snippet - with statement", SNIPPET_WITH, prefix));
      }
    }
  }

  private SnippetCompletionItem createSnippetItem(
      String name, String desc, CodeSnippet snippet, String prefix) {
    return new SnippetCompletionItem(
        name, desc, new SnippetDescription(prefix.length(), snippet, true));
  }

  /**
   * تشخیص می‌کند که آیا کاراکتر داده شده بخشی از یک شناسه پایتون است یا خیر. برای محاسبه پیشوند در
   * CompletionHelper استفاده می‌شود.
   */
  private boolean isIdentifierChar(char c) {
    return Character.isLetterOrDigit(c) || c == '_';
  }

  @Override
  public int getIndentAdvance(@NonNull ContentReference text, int line, int column) {

    Token token;
    int advance = 0;
    boolean openBlock = false;
    try {
      var lexer =
          new PythonLexerCompat(CharStreams.fromReader(new StringReader(text.getLine(line))));
      while (((token = lexer.nextToken()) != null && token.getType() != token.EOF)) {
        switch (token.getType()) {
          case PythonLexerCompat.CLASS:
          case PythonLexerCompat.DEF:
          case PythonLexerCompat.IF:
          case PythonLexerCompat.ELIF:
          case PythonLexerCompat.FOR:
          case PythonLexerCompat.WHILE:
          case PythonLexerCompat.ELSE:
          case PythonLexerCompat.TRY:
          case PythonLexerCompat.EXCEPT:
            openBlock = !openBlock;
            break;
          case PythonLexerCompat.COLON:
            advance++;
            break;
          case PythonLexerCompat.CONTINUE:
          case PythonLexerCompat.BREAK:
            openBlock = !openBlock;
            advance--;
            break;
        }
      }
      advance = Math.max(0, advance);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return openBlock ? advance * 2 : 0;
  }

  @Override
  public boolean useTab() {
    return false;
  }

  @NonNull
  @Override
  public Formatter getFormatter() {
    return pythonFormatter;
  }

  @Override
  public SymbolPairMatch getSymbolPairs() {

    return new SymbolPairMatch.DefaultSymbolPairs();
  }

  @Override
  public NewlineHandler[] getNewlineHandlers() {
    return new NewlineHandler[] {new PythonIndentHandler()};
  }

  private class PythonIndentHandler implements NewlineHandler {

    @Override
    public boolean matchesRequirement(
        @NonNull Content text, @NonNull CharPosition position, @Nullable Styles style) {

      int line = position.line;

      if (line < 0 || line >= text.getLineCount()) {
        return false;
      }

      String before = text.getLine(line).subSequence(0, position.column).toString().trim();

      return before.endsWith(":");
    }

    @NonNull
    @Override
    public NewlineHandleResult handleNewline(
        @NonNull Content text,
        @NonNull CharPosition position,
        @Nullable Styles style,
        int tabSize) {

      String lineText = text.getLine(position.line).toString();

      int indent = TextUtils.countLeadingSpaceCount(lineText, tabSize);

      String indentStr = TextUtils.createIndent(indent + tabSize, tabSize, false);

      return new NewlineHandleResult(new StringBuilder("\n").append(indentStr), 0);
    }
  }
}
