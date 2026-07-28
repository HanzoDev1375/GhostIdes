/**
 * Comment by ghost ide
 *
 * @author: Ninjacoder
 */
package ir.hanzodev1375.ghostide.codeeditors.langs.tsx;

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

public class TsxLanguage implements Language {
  private static final CodeSnippet FUNCTION_COMPONENT =
      CodeSnippetParser.parse(
          "const ${1:ComponentName}: React.FC<${2:Props}> = (${3:props}) => {\n    $0\n};");

  private static final CodeSnippet USESTATE_SNIPPET =
      CodeSnippetParser.parse(
          "const [${1:state}, set${1/(.*)/${1:/capitalize}/}] = useState<${2:type}>(${3:initial});");

  private static final CodeSnippet USEEFFECT_SNIPPET =
      CodeSnippetParser.parse("useEffect(() => {\n    $0\n}, [${1:deps}]);");

  private static final CodeSnippet USE_CONTEXT_SNIPPET =
      CodeSnippetParser.parse("const ${1:context} = useContext(${2:Context});");

  private static final CodeSnippet USE_REDUCER_SNIPPET =
      CodeSnippetParser.parse(
          "const [${1:state}, ${1:dispatch}] = useReducer(${2:reducer}, ${3:initialState});");

  private static final CodeSnippet TS_INTERFACE_SNIPPET =
      CodeSnippetParser.parse("interface ${1:Name} {\n    ${2:prop}: ${3:type};\n}");

  private static final CodeSnippet TS_TYPE_SNIPPET =
      CodeSnippetParser.parse("type ${1:Name} = {\n    ${2:prop}: ${3:type};\n};");

  private static final CodeSnippet EXPORT_DEFAULT_SNIPPET =
      CodeSnippetParser.parse("export default ${1:ComponentName};");

  private static final String[] KEYWORDS = {
    "abstract",
    "as",
    "asserts",
    "any",
    "boolean",
    "break",
    "case",
    "catch",
    "class",
    "const",
    "continue",
    "debugger",
    "declare",
    "default",
    "delete",
    "do",
    "else",
    "enum",
    "export",
    "extends",
    "false",
    "finally",
    "for",
    "from",
    "function",
    "get",
    "if",
    "implements",
    "import",
    "in",
    "infer",
    "instanceof",
    "interface",
    "is",
    "keyof",
    "let",
    "module",
    "namespace",
    "never",
    "new",
    "null",
    "number",
    "object",
    "package",
    "private",
    "protected",
    "public",
    "readonly",
    "require",
    "return",
    "set",
    "static",
    "string",
    "super",
    "switch",
    "symbol",
    "this",
    "throw",
    "true",
    "try",
    "type",
    "typeof",
    "undefined",
    "unknown",
    "var",
    "void",
    "while",
    "with",
    "yield"
  };

  private IdentifierAutoComplete autoComplete;

  private final TsxIncrementalAnalyzeManager manager;

  private final TsxQuoteHandler quoteHandler = new TsxQuoteHandler();

  public TsxLanguage() {
    autoComplete = new IdentifierAutoComplete(KEYWORDS);
    manager = new TsxIncrementalAnalyzeManager();
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
   
    if ("fc".startsWith(prefix) && !prefix.isEmpty())
      publisher.addItem(
          new SimpleSnippetCompletionItem(
              "fc",
              "Snippet - Function Component",
              new SnippetDescription(prefix.length(), FUNCTION_COMPONENT, true)));
    if ("useState".startsWith(prefix) && !prefix.isEmpty())
      publisher.addItem(
          new SimpleSnippetCompletionItem(
              "useState",
              "Snippet - useState",
              new SnippetDescription(prefix.length(), USESTATE_SNIPPET, true)));
    if ("useEffect".startsWith(prefix) && !prefix.isEmpty())
      publisher.addItem(
          new SimpleSnippetCompletionItem(
              "useEffect",
              "Snippet - useEffect",
              new SnippetDescription(prefix.length(), USEEFFECT_SNIPPET, true)));
    if ("useContext".startsWith(prefix) && !prefix.isEmpty())
      publisher.addItem(
          new SimpleSnippetCompletionItem(
              "useContext",
              "Snippet - useContext",
              new SnippetDescription(prefix.length(), USE_CONTEXT_SNIPPET, true)));
    if ("useReducer".startsWith(prefix) && !prefix.isEmpty())
      publisher.addItem(
          new SimpleSnippetCompletionItem(
              "useReducer",
              "Snippet - useReducer",
              new SnippetDescription(prefix.length(), USE_REDUCER_SNIPPET, true)));
    if ("interface".startsWith(prefix) && !prefix.isEmpty())
      publisher.addItem(
          new SimpleSnippetCompletionItem(
              "interface",
              "Snippet - Interface",
              new SnippetDescription(prefix.length(), TS_INTERFACE_SNIPPET, true)));
    if ("type".startsWith(prefix) && !prefix.isEmpty())
      publisher.addItem(
          new SimpleSnippetCompletionItem(
              "type",
              "Snippet - Type",
              new SnippetDescription(prefix.length(), TS_TYPE_SNIPPET, true)));
    if ("exportd".startsWith(prefix) && !prefix.isEmpty())
      publisher.addItem(
          new SimpleSnippetCompletionItem(
              "exportd",
              "Snippet - Export Default",
              new SnippetDescription(prefix.length(), EXPORT_DEFAULT_SNIPPET, true)));
  }

  @Override
  public int getIndentAdvance(@NonNull ContentReference text, int line, int column) {
    var content = text.getLine(line).substring(0, column);
    return getIndentAdvance(content);
  }

  private int getIndentAdvance(String content) {
    TsxTextTokenizer t = new TsxTextTokenizer(content);
    TsxTokens token;
    int advance = 0;
    while ((token = t.nextToken()) != TsxTokens.EOF) {
      if (token == TsxTokens.LBRACE || token == TsxTokens.JSX_TAG_OPEN) advance++;
      if (token == TsxTokens.RBRACE || token == TsxTokens.JSX_TAG_CLOSE) advance--;
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
