/**
 * Comment by ghost ide
 *
 * @author: Ninjacoder
 */
package ir.hanzodev1375.ghostide.codeeditors.langs.asm;

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
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandler;
import io.github.rosemoe.sora.text.CharPosition;
import io.github.rosemoe.sora.text.ContentReference;
import io.github.rosemoe.sora.util.MyCharacter;
import io.github.rosemoe.sora.widget.SymbolPairMatch;

public class AsmLanguage implements Language {

  private static final CodeSnippet MAIN_SNIPPET =
      CodeSnippetParser.parse(
          ".global ${1:_start}\n${1:_start}:\n    $0\n    mov\tx8, #93\n    mov\tx0, #0\n    svc\t#0");

  private static final CodeSnippet PROC_SNIPPET =
      CodeSnippetParser.parse("${1:name}:\n    $0\n    ret");

  private static final CodeSnippet DATA_SNIPPET =
      CodeSnippetParser.parse(".section .data\n${1:name}: .word ${2:0}");

  private static final String[] KEYWORDS = {
    "mov", "movz", "movk", "movn", "ldr", "ldrb", "ldrh", "ldrsb", "ldrsh", "ldrsw", "str",
    "strb", "strh", "add", "adds", "sub", "subs", "mul", "smull", "umull", "sdiv", "udiv",
    "and", "ands", "orr", "eor", "eon", "bic", "mvn", "lsl", "lsr", "asr", "ror", "cmp",
    "cmn", "tst", "teq", "b", "bl", "br", "blr", "bx", "ret", "beq", "bne", "blt", "ble",
    "bgt", "bge", "cbz", "cbnz", "tbz", "tbnz", "push", "pop", "stp", "ldp", "svc", "nop",
    "call", "jmp", "je", "jne", "jg", "jge", "jl", "jle", "lea", "int", "syscall", "xchg",
    "test", "neg", "inc", "dec", "global", "extern", "section", "equ"
  };

  private IdentifierAutoComplete autoComplete;

  private final AsmIncrementalAnalyzeManager manager;

  private final AsmQuoteHandler quoteHandler = new AsmQuoteHandler();

  public AsmLanguage() {
    autoComplete = new IdentifierAutoComplete(KEYWORDS);
    manager = new AsmIncrementalAnalyzeManager();
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
    if ("main".startsWith(prefix) && !prefix.isEmpty())
      publisher.addItem(
          new SimpleSnippetCompletionItem(
              "main",
              "Snippet - Main",
              new SnippetDescription(prefix.length(), MAIN_SNIPPET, true)));
    if ("proc".startsWith(prefix) && !prefix.isEmpty())
      publisher.addItem(
          new SimpleSnippetCompletionItem(
              "proc",
              "Snippet - Procedure",
              new SnippetDescription(prefix.length(), PROC_SNIPPET, true)));
    if ("data".startsWith(prefix) && !prefix.isEmpty())
      publisher.addItem(
          new SimpleSnippetCompletionItem(
              "data",
              "Snippet - Data Section",
              new SnippetDescription(prefix.length(), DATA_SNIPPET, true)));
  }

  @Override
  public int getIndentAdvance(@NonNull ContentReference text, int line, int column) {
    return 0;
  }

  @Override
  public NewlineHandler[] getNewlineHandlers() {
    return new NewlineHandler[0];
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
}
