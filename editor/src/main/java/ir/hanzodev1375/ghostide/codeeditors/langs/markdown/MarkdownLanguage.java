package ir.hanzodev1375.ghostide.codeeditors.langs.markdown;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.github.rosemoe.sora.lang.EmptyLanguage;
import io.github.rosemoe.sora.lang.Language;
import io.github.rosemoe.sora.lang.QuickQuoteHandler;
import io.github.rosemoe.sora.lang.analysis.AnalyzeManager;
import io.github.rosemoe.sora.lang.completion.CompletionPublisher;
import io.github.rosemoe.sora.lang.completion.SimpleSnippetCompletionItem;
import io.github.rosemoe.sora.lang.completion.SnippetDescription;
import io.github.rosemoe.sora.lang.completion.snippet.CodeSnippet;
import io.github.rosemoe.sora.lang.completion.snippet.parser.CodeSnippetParser;
import io.github.rosemoe.sora.lang.format.Formatter;
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandleResult;
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandler;
import io.github.rosemoe.sora.lang.styling.Styles;
import io.github.rosemoe.sora.text.CharPosition;
import io.github.rosemoe.sora.text.Content;
import io.github.rosemoe.sora.text.ContentReference;
import io.github.rosemoe.sora.text.TextUtils;
import io.github.rosemoe.sora.widget.SymbolPairMatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownLanguage implements Language {

  private static final CodeSnippet SNIPPET_FENCED_CODE =
      CodeSnippetParser.parse("```${1:language}\n$0\n```");

  private static final CodeSnippet SNIPPET_LINK = CodeSnippetParser.parse("[${1:text}](${2:url})");

  private static final CodeSnippet SNIPPET_IMAGE = CodeSnippetParser.parse("![${1:alt}](${2:url})");

  private static final CodeSnippet SNIPPET_TABLE =
      CodeSnippetParser.parse(
          "| ${1:Col1} | ${2:Col2} | ${3:Col3} |\n" + "| --- | --- | --- |\n" + "| $0 |  |  |");

  private static final CodeSnippet SNIPPET_BOLD = CodeSnippetParser.parse("**${1:text}**");

  private static final CodeSnippet SNIPPET_ITALIC = CodeSnippetParser.parse("*${1:text}*");

  private static final CodeSnippet SNIPPET_STRIKETHROUGH = CodeSnippetParser.parse("~~${1:text}~~");

  private static final CodeSnippet SNIPPET_BLOCKQUOTE = CodeSnippetParser.parse("> ${1:quote}");

  private static final CodeSnippet SNIPPET_H1 = CodeSnippetParser.parse("# ${1:Heading}");

  private static final CodeSnippet SNIPPET_H2 = CodeSnippetParser.parse("## ${1:Heading}");

  private static final CodeSnippet SNIPPET_H3 = CodeSnippetParser.parse("### ${1:Heading}");

  private final MarkdownIncrementalAnalyzeManager manager;
  private final MarkdownQuoteHandler quoteHandler = new MarkdownQuoteHandler();

  public MarkdownLanguage() {
    manager = new MarkdownIncrementalAnalyzeManager();
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
    /* nothing to release */
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

    CharSequence line = content.getLine(position.line);
    int col = position.column;
    int start = col;
    while (start > 0 && !Character.isWhitespace(line.charAt(start - 1))) start--;
    String prefix = line.subSequence(start, col).toString();

    if (prefix.isEmpty()) return;

    offerSnippet(publisher, prefix, "```", "Snippet - Fenced code block", SNIPPET_FENCED_CODE);
    offerSnippet(publisher, prefix, "[", "Snippet - Link", SNIPPET_LINK);
    offerSnippet(publisher, prefix, "![", "Snippet - Image", SNIPPET_IMAGE);
    offerSnippet(publisher, prefix, "table", "Snippet - Table", SNIPPET_TABLE);
    offerSnippet(publisher, prefix, "**", "Snippet - Bold", SNIPPET_BOLD);
    offerSnippet(publisher, prefix, "*", "Snippet - Italic", SNIPPET_ITALIC);
    offerSnippet(publisher, prefix, "~~", "Snippet - Strikethrough", SNIPPET_STRIKETHROUGH);
    offerSnippet(publisher, prefix, ">", "Snippet - Blockquote", SNIPPET_BLOCKQUOTE);
    offerSnippet(publisher, prefix, "#", "Snippet - H1", SNIPPET_H1);
    offerSnippet(publisher, prefix, "##", "Snippet - H2", SNIPPET_H2);
    offerSnippet(publisher, prefix, "###", "Snippet - H3", SNIPPET_H3);
  }

  private static void offerSnippet(
      CompletionPublisher publisher,
      String prefix,
      String trigger,
      String desc,
      CodeSnippet snippet) {
    if (trigger.startsWith(prefix)) {
      publisher.addItem(
          new SimpleSnippetCompletionItem(
              trigger, desc, new SnippetDescription(prefix.length(), snippet, true)));
    }
  }

  /**
   * Markdown indentation rules: - Inside a list item → continue with the same indent. - Inside a
   * blockquote → continue with "> ". - Otherwise → 0.
   */
  @Override
  public int getIndentAdvance(@NonNull ContentReference text, int line, int column) {
    CharSequence lineText = text.getLine(line);
    String sub = lineText.subSequence(0, Math.min(column, lineText.length())).toString();
    return computeIndent(sub);
  }

  private int computeIndent(String lineContent) {
    String trimmed = lineContent.stripLeading();

    if (trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("+ ")) {
      return 2;
    }

    if (trimmed.matches("\\d+[.)].+")) return 3;

    return 0;
  }

  private final NewlineHandler[] newlineHandlers =
      new NewlineHandler[] {new BlockquoteHandler(), new ListItemHandler()};

  @Override
  public NewlineHandler[] getNewlineHandlers() {
    return newlineHandlers;
  }

  @Override
  public boolean useTab() {
    return false;
  }

  @NonNull
  @Override
  public Formatter getFormatter() {
    return EmptyLanguage.EmptyFormatter.INSTANCE;
  }

  @Override
  public SymbolPairMatch getSymbolPairs() {
    SymbolPairMatch pairs = new SymbolPairMatch();
    pairs.putPair('(', new SymbolPairMatch.SymbolPair("(", ")"));
    pairs.putPair('[', new SymbolPairMatch.SymbolPair("[", "]"));
    pairs.putPair('`', new SymbolPairMatch.SymbolPair("`", "`"));
    pairs.putPair('*', new SymbolPairMatch.SymbolPair("*", "*"));
    pairs.putPair('_', new SymbolPairMatch.SymbolPair("_", "_"));
    pairs.putPair('"', new SymbolPairMatch.SymbolPair("\"", "\""));
    return pairs;
  }

  /**
   * When the user presses Enter inside a blockquote line ("> …"), the new line automatically starts
   * with "> ".
   */
  class BlockquoteHandler implements NewlineHandler {

    @Override
    public boolean matchesRequirement(
        @NonNull Content text, @NonNull CharPosition position, @Nullable Styles style) {
      CharSequence line = text.getLine(position.line);
      String trimmed = line.toString().stripLeading();
      return trimmed.startsWith("> ") || trimmed.equals(">");
    }

    @NonNull
    @Override
    public NewlineHandleResult handleNewline(
        @NonNull Content text,
        @NonNull CharPosition position,
        @Nullable Styles style,
        int tabSize) {

      CharSequence line = text.getLine(position.line);
      String lineStr = line.toString();
      int leadSpaces = 0;
      while (leadSpaces < lineStr.length() && lineStr.charAt(leadSpaces) == ' ') leadSpaces++;

      String indent = TextUtils.createIndent(leadSpaces, tabSize, useTab());

      String insert = "\n" + indent + "> ";
      return new NewlineHandleResult(insert, 0);
    }
  }

  /**
   * When Enter is pressed at the end of a list item line, the next line automatically gets the same
   * list marker prefix.
   */
  class ListItemHandler implements NewlineHandler {

    private String detectedPrefix = "";

    @Override
    public boolean matchesRequirement(
        @NonNull Content text, @NonNull CharPosition position, @Nullable Styles style) {
      CharSequence line = text.getLine(position.line);
      String lineStr = line.toString();
      String trimmed = lineStr.stripLeading();
      int leading = lineStr.length() - trimmed.length();
      String spaces = lineStr.substring(0, leading);

      if (trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("+ ")) {
        detectedPrefix = spaces + trimmed.charAt(0) + " ";
        return true;
      }

      Matcher m = Pattern.compile("^(\\d+)([.)])(\\s)").matcher(trimmed);
      if (m.find()) {
        int num = Integer.parseInt(m.group(1)) + 1;
        String delim = m.group(2);
        detectedPrefix = spaces + num + delim + " ";
        return true;
      }
      return false;
    }

    @NonNull
    @Override
    public NewlineHandleResult handleNewline(
        @NonNull Content text,
        @NonNull CharPosition position,
        @Nullable Styles style,
        int tabSize) {
      String insert = "\n" + detectedPrefix;
      return new NewlineHandleResult(insert, 0);
    }
  }
}
