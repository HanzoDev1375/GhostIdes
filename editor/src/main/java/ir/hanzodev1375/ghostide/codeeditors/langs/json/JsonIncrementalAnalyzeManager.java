package ir.hanzodev1375.ghostide.codeeditors.langs.json;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PictureDrawable;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.caverock.androidsvg.SVG;
import io.github.rosemoe.sora.lang.analysis.AsyncIncrementalAnalyzeManager;
import io.github.rosemoe.sora.lang.styling.CodeBlock;
import io.github.rosemoe.sora.lang.styling.Span;
import io.github.rosemoe.sora.lang.styling.SpanFactory;
import io.github.rosemoe.sora.lang.styling.TextStyle;
import io.github.rosemoe.sora.lang.styling.line.LineSideIcon;
import io.github.rosemoe.sora.text.Content;
import io.github.rosemoe.sora.text.ContentReference;
import ir.hanzodev1375.ghostide.codeeditors.colorscheme.GhostColorScheme;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class JsonIncrementalAnalyzeManager
    extends AsyncIncrementalAnalyzeManager<
        JsonState, JsonIncrementalAnalyzeManager.HighlightToken> {

  private final ThreadLocal<JsonTextTokenizer> tokenizerProvider = new ThreadLocal<>();

  private Context context;
  private String jsonFilePath;

  public void init(Context context, String jsonFilePath) {
    this.context = context.getApplicationContext();
    this.jsonFilePath = jsonFilePath;
  }

  private synchronized JsonTextTokenizer obtainTokenizer() {
    var res = tokenizerProvider.get();
    if (res == null) {
      res = new JsonTextTokenizer("");
      tokenizerProvider.set(res);
    }
    return res;
  }

  @Override
  public List<CodeBlock> computeBlocks(Content text, CodeBlockAnalyzeDelegate delegate) {
    var stack = new Stack<CodeBlock>();
    var blocks = new ArrayList<CodeBlock>();
    for (int i = 0; i < text.getLineCount() && delegate.isNotCancelled(); i++) {
      var state = getState(i);
      for (var tokenRecord : state.tokens) {
        if (tokenRecord.token == JsonTokens.LBRACE) {
          CodeBlock block = new CodeBlock();
          block.startLine = i;
          block.startColumn = tokenRecord.offset;
          stack.push(block);
        } else if (tokenRecord.token == JsonTokens.RBRACE) {
          if (!stack.isEmpty()) {
            CodeBlock block = stack.pop();
            block.endLine = i;
            block.endColumn = tokenRecord.offset;
            if (block.startLine != block.endLine) {
              blocks.add(block);
            }
          }
        }
      }
    }
    return blocks;
  }

  @NonNull
  @Override
  public JsonState getInitialState() {
    return new JsonState();
  }

  @Override
  public boolean stateEquals(@NonNull JsonState state, @NonNull JsonState another) {
    return state.equals(another);
  }

  @Override
  public void onAddState(JsonState state) {}

  @Override
  public void onAbandonState(JsonState state) {}

  @Override
  public void reset(@NonNull ContentReference content, @NonNull Bundle extraArguments) {
    super.reset(content, extraArguments);
  }

  @Override
  public LineTokenizeResult<JsonState, HighlightToken> tokenizeLine(
      CharSequence line, JsonState state, int lineIndex) {
    var tokens = new ArrayList<HighlightToken>();
    var tokenizer = obtainTokenizer();
    tokenizer.reset(line);

    JsonTokens previous = JsonTokens.UNKNOWN;
    JsonTokens token;
    while ((token = tokenizer.nextToken()) != JsonTokens.EOF) {
      tokens.add(new HighlightToken(token, tokenizer.offset));
      if (token == JsonTokens.STRING && previous == JsonTokens.COLON) {
        String text = tokenizer.getTokenText().toString();
        if (text.length() >= 2) {
          loadImageToLine(text.substring(1, text.length() - 1), lineIndex);
        }
      }
      if (token != JsonTokens.WHITESPACE && token != JsonTokens.NEWLINE) {
        previous = token;
      }
    }
    if (tokens.isEmpty()) {
      tokens.add(new HighlightToken(JsonTokens.UNKNOWN, 0));
    }
    return new LineTokenizeResult<>(new JsonState(), tokens);
  }

  @Override
  public List<Span> generateSpansForLine(LineTokenizeResult<JsonState, HighlightToken> lineResult) {
    var spans = new ArrayList<Span>();
    var tokens = lineResult.tokens;
    JsonTokens previous = JsonTokens.UNKNOWN;

    for (var tokenRecord : tokens) {
      var token = tokenRecord.token;
      int offset = tokenRecord.offset;
      Span span;
      switch (token) {
        case COLON:
        case COMMA:
          span = SpanFactory.obtain(offset, GhostColorScheme.OPERATOR);
          break;
        case LBRACE:
        case LBRACKET:
        case RBRACE:
        case RBRACKET:
          span = SpanFactory.obtain(offset, TextStyle.makeStyle(GhostColorScheme.ATTRIBUTE_NAME));
          break;
        case STRING:
          if (previous == JsonTokens.COLON) {
            span = SpanFactory.obtain(offset, TextStyle.makeStyle(GhostColorScheme.LITERAL, true));
          } else {
            span =
                SpanFactory.obtain(offset, TextStyle.makeStyle(GhostColorScheme.ATTRIBUTE_VALUE));
          }
          break;
        case NUMBER:
          span = SpanFactory.obtain(offset, TextStyle.makeStyle(GhostColorScheme.LITERAL, true));
          break;
        case TRUE:
        case FALSE:
        case NULL:
          span =
              SpanFactory.obtain(
                  offset, TextStyle.makeStyle(GhostColorScheme.KEYWORD, 0, true, false, false));
          break;
        default:
          span = SpanFactory.obtain(offset, TextStyle.makeStyle(GhostColorScheme.TEXT_NORMAL));
      }
      spans.add(span);
      if (token != JsonTokens.WHITESPACE && token != JsonTokens.NEWLINE) {
        previous = token;
      }
    }
    return spans;
  }

  private boolean isImagePath(String path) {
    String lower = path.toLowerCase();
    return lower.endsWith(".png")
        || lower.endsWith(".jpg")
        || lower.endsWith(".jpeg")
        || lower.endsWith(".webp")
        || lower.endsWith(".gif")
        || lower.endsWith(".svg");
  }

  private File resolvePath(String value) {
    if (value.startsWith("./")) {
      value = value.substring(2);
    }
    File file = new File(value);
    if (!file.isAbsolute()) {
      File parent = new File(jsonFilePath).getParentFile();
      if (parent != null) {
        file = new File(parent, value);
      }
    }
    try {
      return file.getCanonicalFile();
    } catch (IOException e) {
      return null;
    }
  }

  private void loadSvgToLine(String value, int currentLine) {
    File file = resolvePath(value);
    if (file == null || !file.exists()) return;
    try (FileInputStream fis = new FileInputStream(file)) {
      SVG svg = SVG.getFromInputStream(fis);
      svg.setDocumentWidth(48);
      svg.setDocumentHeight(48);
      PictureDrawable drawable = new PictureDrawable(svg.renderToPicture());
      if (getManagedStyles() != null) {
        getManagedStyles().eraseLineStyle(currentLine, LineSideIcon.class);
        getManagedStyles().addLineStyle(new LineSideIcon(currentLine, drawable));
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void loadImageToLine(String value, int currentLine) {
    if (context == null || jsonFilePath == null) return;
    if (value.toLowerCase().endsWith(".svg")) {
      loadSvgToLine(value, currentLine);
      return;
    }
    if (!isImagePath(value)) {
      if (getManagedStyles() != null) {
        getManagedStyles().eraseLineStyle(currentLine, LineSideIcon.class);
      }
      return;
    }
    File file = resolvePath(value);
    if (file == null || !file.exists()) {
      if (getManagedStyles() != null) {
        getManagedStyles().eraseLineStyle(currentLine, LineSideIcon.class);
      }
      return;
    }
    Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
    if (bitmap == null) {
      if (getManagedStyles() != null) {
        getManagedStyles().eraseLineStyle(currentLine, LineSideIcon.class);
      }
      return;
    }
    Drawable drawable = new BitmapDrawable(context.getResources(), bitmap);
    if (getManagedStyles() != null) {
      getManagedStyles().eraseLineStyle(currentLine, LineSideIcon.class);
      getManagedStyles().addLineStyle(new LineSideIcon(currentLine, drawable));
    }
  }

  public static class HighlightToken {
    public JsonTokens token;
    public int offset;

    public HighlightToken(JsonTokens token, int offset) {
      this.token = token;
      this.offset = offset;
    }
  }
}
