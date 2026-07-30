package ir.hanzodev1375.ghostide.codeeditors.langs.json;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import io.github.rosemoe.sora.lang.Language;
import io.github.rosemoe.sora.lang.QuickQuoteHandler;
import io.github.rosemoe.sora.lang.analysis.AnalyzeManager;
import io.github.rosemoe.sora.lang.completion.CompletionHelper;
import io.github.rosemoe.sora.lang.completion.CompletionPublisher;
import io.github.rosemoe.sora.lang.completion.IdentifierAutoComplete;
import io.github.rosemoe.sora.lang.format.Formatter;
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandler;
import io.github.rosemoe.sora.text.CharPosition;
import io.github.rosemoe.sora.text.ContentReference;
import io.github.rosemoe.sora.widget.SymbolPairMatch;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import ir.hanzodev1375.ghostide.codeeditors.langs.antlr4base.CharParser;
import ir.hanzodev1375.ghostide.codeeditors.util.CustomFormatter;

public class JsonLanguage implements Language {

  private final JsonIncrementalAnalyzeManager analyzer;
  private final IdentifierAutoComplete autoComplete;
  private final CustomFormatter formatter = new CustomFormatter();

  public JsonLanguage(Context ctx, String path) {

    String[] keywords = {"true", "false", "null"};

    autoComplete = new IdentifierAutoComplete(keywords);

    analyzer = new JsonIncrementalAnalyzeManager();
    analyzer.init(ctx, path);

    formatter.setFormatAction(this::formatJson);
  }

  @Override
  public AnalyzeManager getAnalyzeManager() {
    return analyzer;
  }

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
      @NonNull Bundle extra) {

    String prefix = CompletionHelper.computePrefix(content, position, CharParser::parserJava);
    autoComplete.requireAutoComplete(content, position, prefix, publisher, null);
  }

  @Override
  public int getIndentAdvance(@NonNull ContentReference text, int line, int column) {
    var tokenizer = new JsonTextTokenizer(text.getLine(line));
    JsonTokens token;
    int advance = 0;

    while ((token = tokenizer.nextToken()) != JsonTokens.EOF) {
      if (token == JsonTokens.LBRACE) advance++;
      if (token == JsonTokens.RBRACE) advance--;
    }

    return Math.max(0, advance) * 2;
  }

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
    return new NewlineHandler[0];
  }

  private String formatJson(String code) {
    try {
      Gson gson = new GsonBuilder().setPrettyPrinting().create();
      JsonElement element = JsonParser.parseString(code);
      return gson.toJson(element);
    } catch (Exception e) {
      return code;
    }
  }
}
