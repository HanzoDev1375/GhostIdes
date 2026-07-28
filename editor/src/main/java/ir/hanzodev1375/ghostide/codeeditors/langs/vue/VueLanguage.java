/**
 * Comment by ghost ide
 *
 * @author: Ninjacoder
 */
package ir.hanzodev1375.ghostide.codeeditors.langs.vue;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

public class VueLanguage implements Language {

  private static final CodeSnippet SFC_SNIPPET =
      CodeSnippetParser.parse(
          "<template>\n  $0\n</template>\n\n<script setup>\n</script>\n\n<style scoped>\n</style>\n");

  private static final CodeSnippet VIF_SNIPPET =
      CodeSnippetParser.parse("v-if=\"${1:condition}\"$0");

  private static final CodeSnippet VELSEIF_SNIPPET =
      CodeSnippetParser.parse("v-else-if=\"${1:condition}\"$0");

  private static final CodeSnippet VFOR_SNIPPET =
      CodeSnippetParser.parse("v-for=\"${1:item} in ${2:items}\" :key=\"${1:item}\"$0");

  private static final CodeSnippet VMODEL_SNIPPET =
      CodeSnippetParser.parse("v-model=\"${1:value}\"$0");

  private static final CodeSnippet VBIND_SNIPPET =
      CodeSnippetParser.parse(":${1:prop}=\"${2:value}\"$0");

  private static final CodeSnippet VON_SNIPPET =
      CodeSnippetParser.parse("@${1:click}=\"${2:handler}\"$0");

  private static final CodeSnippet REF_SNIPPET =
      CodeSnippetParser.parse("const ${1:state} = ref(${2:value})$0");

  private static final CodeSnippet REACTIVE_SNIPPET =
      CodeSnippetParser.parse("const ${1:state} = reactive({\n  $0\n})");

  private static final CodeSnippet COMPUTED_SNIPPET =
      CodeSnippetParser.parse("const ${1:name} = computed(() => {\n  $0\n})");

  private static final CodeSnippet WATCH_SNIPPET =
      CodeSnippetParser.parse("watch(${1:source}, (${2:value}) => {\n  $0\n})");

  private static final CodeSnippet PROPS_SNIPPET =
      CodeSnippetParser.parse("const props = defineProps({\n  ${1:name}: ${2:String}\n})$0");

  private static final CodeSnippet EMITS_SNIPPET =
      CodeSnippetParser.parse("const emit = defineEmits([${1:'update'}])$0");

  private static final CodeSnippet ONMOUNTED_SNIPPET =
      CodeSnippetParser.parse("onMounted(() => {\n  $0\n})");

  private Context editor;

  private static final String[] KEYWORDS = {
    "template",
    "script",
    "style",
    "setup",
    "scoped",
    "v-if",
    "v-else",
    "v-else-if",
    "v-for",
    "v-model",
    "v-bind",
    "v-on",
    "v-show",
    "v-slot",
    "v-pre",
    "v-once",
    "v-cloak",
    "ref",
    "reactive",
    "computed",
    "watch",
    "watchEffect",
    "onMounted",
    "onUnmounted",
    "onUpdated",
    "defineProps",
    "defineEmits",
    "defineExpose",
    "provide",
    "inject",
    "nextTick",
    "const",
    "let",
    "var",
    "function",
    "return",
    "import",
    "export",
    "from",
    "default",
    "class",
    "new",
    "if",
    "else",
    "for",
    "while",
    "true",
    "false",
    "null",
    "undefined"
  };

  private IdentifierAutoComplete autoComplete;

  private  VueIncrementalAnalyzeManager manager;

  private  VueQuoteHandler quoteHandler = new VueQuoteHandler();

  public VueLanguage(Context editor) {
    this.editor = editor;
    autoComplete = new IdentifierAutoComplete(KEYWORDS);
    manager = new VueIncrementalAnalyzeManager();
  }

  private final Formatter formatter =
      new AsyncFormatter() {
        @Nullable
        @Override
        public TextRange formatAsync(@NonNull Content text, @NonNull TextRange cursorRange) {
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
    if (prefix.isEmpty()) return;
    addSnippetIfMatches(publisher, prefix, "template", "Snippet - SFC Skeleton", SFC_SNIPPET);
    addSnippetIfMatches(publisher, prefix, "if", "Snippet - v-if", VIF_SNIPPET);
    addSnippetIfMatches(publisher, prefix, "elseif", "Snippet - v-else-if", VELSEIF_SNIPPET);
    addSnippetIfMatches(publisher, prefix, "for", "Snippet - v-for", VFOR_SNIPPET);
    addSnippetIfMatches(publisher, prefix, "model", "Snippet - v-model", VMODEL_SNIPPET);
    addSnippetIfMatches(publisher, prefix, "bind", "Snippet - v-bind shorthand", VBIND_SNIPPET);
    addSnippetIfMatches(publisher, prefix, "on", "Snippet - v-on shorthand", VON_SNIPPET);
    addSnippetIfMatches(publisher, prefix, "ref", "Snippet - ref", REF_SNIPPET);
    addSnippetIfMatches(publisher, prefix, "reactive", "Snippet - reactive", REACTIVE_SNIPPET);
    addSnippetIfMatches(publisher, prefix, "computed", "Snippet - computed", COMPUTED_SNIPPET);
    addSnippetIfMatches(publisher, prefix, "watch", "Snippet - watch", WATCH_SNIPPET);
    addSnippetIfMatches(publisher, prefix, "defineProps", "Snippet - defineProps", PROPS_SNIPPET);
    addSnippetIfMatches(publisher, prefix, "defineEmits", "Snippet - defineEmits", EMITS_SNIPPET);
    addSnippetIfMatches(publisher, prefix, "onMounted", "Snippet - onMounted", ONMOUNTED_SNIPPET);
  }

  private void addSnippetIfMatches(
      CompletionPublisher publisher,
      String prefix,
      String trigger,
      String label,
      CodeSnippet snippet) {
    if (trigger.startsWith(prefix)) {
      publisher.addItem(
          new SimpleSnippetCompletionItem(
              trigger, label, new SnippetDescription(prefix.length(), snippet, true)));
    }
  }

  @Override
  public int getIndentAdvance(@NonNull ContentReference text, int line, int column) {
    var content = text.getLine(line).substring(0, column);
    int block = manager.getLineState(line).block;
    return getIndentAdvance(content, block);
  }

  private int getIndentAdvance(String content, int block) {
    VueTextTokenizer t = new VueTextTokenizer(content);
    t.setMode(modeForBlock(block));
    VueTokens token;
    int advance = 0;
    boolean openTag = false;
    while ((token = t.nextToken()) != VueTokens.EOF) {
      if (token == VueTokens.LBRACE) advance++;
      else if (token == VueTokens.TAG_OPEN_START) openTag = true;
      else if (token == VueTokens.TAG_SELF_CLOSE) openTag = false;
      else if (token == VueTokens.TAG_END) {
        if (openTag) advance++;
        openTag = false;
      } else if (token == VueTokens.TAG_CLOSE_START) advance--;
    }
    return Math.max(0, advance) * 4;
  }

  private int modeForBlock(int block) {
    if (block == VueState.BLOCK_SCRIPT) return VueTextTokenizer.MODE_SCRIPT;
    if (block == VueState.BLOCK_STYLE) return VueTextTokenizer.MODE_STYLE;
    return VueTextTokenizer.MODE_MARKUP;
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
      if (StylesUtils.checkNoCompletion(style, position)) return false;
      var line = text.getLine(position.line);
      var before1 = getNonEmptyTextBefore(line, position.column, 1);
      var after1 = getNonEmptyTextAfter(line, position.column, 1);
      if (before1.equals("{") && after1.equals("}")) return true;
      var after2 = getNonEmptyTextAfter(line, position.column, 2);
      return before1.equals(">") && after2.equals("</");
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
      int block = manager.getLineState(position.line).block;
      return handleNewline(beforeText, afterText, tabSize, block);
    }

    @NonNull
    public NewlineHandleResult handleNewline(
        String beforeText, String afterText, int tabSize, int block) {
      int count = TextUtils.countLeadingSpaceCount(beforeText, tabSize);
      int advanceBefore = getIndentAdvance(beforeText, block);
      int advanceAfter = getIndentAdvance(afterText, block);
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
