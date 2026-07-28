/**
 * Comment by ghost ide
 *
 * @author: Ninjacoder
 */
package ir.hanzodev1375.ghostide.codeeditors.langs.html;

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
import io.github.rosemoe.sora.lang.completion.IdentifierAutoComplete;
import io.github.rosemoe.sora.lang.styling.CodeBlock;
import io.github.rosemoe.sora.lang.styling.Span;
import io.github.rosemoe.sora.lang.styling.SpanFactory;
import io.github.rosemoe.sora.lang.styling.TextStyle;
import io.github.rosemoe.sora.lang.styling.line.LineSideIcon;
import io.github.rosemoe.sora.text.Content;
import io.github.rosemoe.sora.text.ContentReference;
import ir.hanzodev1375.ghostide.codeeditors.colorscheme.GhostColorScheme;
import ir.hanzodev1375.ghostide.codeeditors.langs.tsx.TsxTextTokenizer;
import ir.hanzodev1375.ghostide.codeeditors.langs.tsx.TsxTokens;
import ir.hanzodev1375.ghostide.codeeditors.langs.sass.SassTextTokenizer;
import ir.hanzodev1375.ghostide.codeeditors.langs.sass.SassTokens;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumSet;
import java.util.List;

public class HtmlIncrementalAnalyzeManager
    extends AsyncIncrementalAnalyzeManager<
        HtmlState, HtmlIncrementalAnalyzeManager.HighlightToken> {

  private static final int JS_STATE_NORMAL = 0;
  private static final int JS_STATE_INCOMPLETE_COMMENT = 1;

  private static final int CSS_STATE_NORMAL = 0;
  private static final int CSS_STATE_INCOMPLETE_COMMENT = 1;

  private static final int[] BRACKET_COLORS = {
    GhostColorScheme.BRACKET1,
    GhostColorScheme.BRACKET2,
    GhostColorScheme.BRACKET3,
    GhostColorScheme.BRACKET4,
    GhostColorScheme.BRACKET5,
    GhostColorScheme.BRACKET6
  };

  private static long bracketStyle(int depth) {
    return TextStyle.makeStyle(BRACKET_COLORS[depth % BRACKET_COLORS.length]);
  }

  private static final EnumSet<TsxTokens> JS_KEYWORDS =
      EnumSet.of(
          TsxTokens.ABSTRACT,
          TsxTokens.AS,
          TsxTokens.ASSERTS,
          TsxTokens.ANY,
          TsxTokens.BOOLEAN,
          TsxTokens.BREAK,
          TsxTokens.CASE,
          TsxTokens.CATCH,
          TsxTokens.CLASS,
          TsxTokens.CONST,
          TsxTokens.CONTINUE,
          TsxTokens.DEBUGGER,
          TsxTokens.DECLARE,
          TsxTokens.DEFAULT,
          TsxTokens.DELETE,
          TsxTokens.DO,
          TsxTokens.ELSE,
          TsxTokens.ENUM,
          TsxTokens.EXPORT,
          TsxTokens.EXTENDS,
          TsxTokens.FALSE,
          TsxTokens.FINALLY,
          TsxTokens.FOR,
          TsxTokens.FROM,
          TsxTokens.FUNCTION,
          TsxTokens.GET,
          TsxTokens.IF,
          TsxTokens.IMPLEMENTS,
          TsxTokens.IMPORT,
          TsxTokens.IN,
          TsxTokens.INFER,
          TsxTokens.INSTANCEOF,
          TsxTokens.INTERFACE,
          TsxTokens.IS,
          TsxTokens.KEYOF,
          TsxTokens.LET,
          TsxTokens.MODULE,
          TsxTokens.NAMESPACE,
          TsxTokens.NEVER,
          TsxTokens.NEW,
          TsxTokens.NULL,
          TsxTokens.NUMBER,
          TsxTokens.OBJECT,
          TsxTokens.PACKAGE,
          TsxTokens.PRIVATE,
          TsxTokens.PROTECTED,
          TsxTokens.PUBLIC,
          TsxTokens.READONLY,
          TsxTokens.REQUIRE,
          TsxTokens.RETURN,
          TsxTokens.SET,
          TsxTokens.STATIC,
          TsxTokens.STRING,
          TsxTokens.SUPER,
          TsxTokens.SWITCH,
          TsxTokens.SYMBOL,
          TsxTokens.THIS,
          TsxTokens.THROW,
          TsxTokens.TRUE,
          TsxTokens.TRY,
          TsxTokens.TYPE,
          TsxTokens.TYPEOF,
          TsxTokens.UNDEFINED,
          TsxTokens.UNKNOWN,
          TsxTokens.VAR,
          TsxTokens.VOID,
          TsxTokens.WHILE,
          TsxTokens.WITH,
          TsxTokens.YIELD);

  private static final EnumSet<TsxTokens> JS_LITERALS =
      EnumSet.of(
          TsxTokens.STRING_LITERAL,
          TsxTokens.INTEGER_LITERAL,
          TsxTokens.FLOATING_LITERAL,
          TsxTokens.CHARACTER_LITERAL,
          TsxTokens.BOOLEAN_LITERAL,
          TsxTokens.NULL_LITERAL);

  private final ThreadLocal<HtmlTextTokenizer> tokenizerProvider = new ThreadLocal<>();
  private final ThreadLocal<TsxTextTokenizer> jsTokenizerProvider = new ThreadLocal<>();
  private final ThreadLocal<SassTextTokenizer> cssTokenizerProvider = new ThreadLocal<>();

  protected IdentifierAutoComplete.SyncIdentifiers identifiers =
      new IdentifierAutoComplete.SyncIdentifiers();

  private Context context;
  private String jsonFilePath;

  public void init(Context context, String jsonFilePath) {
    this.context = context;
    this.jsonFilePath = jsonFilePath;
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

  private void loadSvgToLine(String value, int currentLine) {
    if (value.startsWith("./")) {
      value = value.substring(2);
    }
    File file = new File(value);
    if (!file.isAbsolute() && jsonFilePath != null) {
      File parent = new File(jsonFilePath).getParentFile();
      if (parent != null) {
        file = new File(parent, value);
      }
    }
    try {
      file = file.getCanonicalFile();
    } catch (IOException e) {
      return;
    }
    if (!file.exists()) return;
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
      // ignore malformed svg
    }
  }

  private void loadImageToLine(String value, int currentLine) {
    if (value == null || value.isEmpty()) return;
    if (value.toLowerCase().endsWith(".svg")) {
      loadSvgToLine(value, currentLine);
      return;
    }
    if (!isImagePath(value) || context == null) {
      return;
    }
    if (value.startsWith("./")) {
      value = value.substring(2);
    }
    File file = new File(value);
    if (!file.isAbsolute() && jsonFilePath != null) {
      File parent = new File(jsonFilePath).getParentFile();
      if (parent != null) {
        file = new File(parent, value);
      }
    }
    try {
      file = file.getCanonicalFile();
    } catch (IOException e) {
      return;
    }
    if (!file.exists()) {
      return;
    }
    Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
    if (bitmap == null) {
      return;
    }
    Drawable drawable = new BitmapDrawable(context.getResources(), bitmap);
    if (getManagedStyles() != null) {
      getManagedStyles().eraseLineStyle(currentLine, LineSideIcon.class);
      getManagedStyles().addLineStyle(new LineSideIcon(currentLine, drawable));
    }
  }

  private synchronized HtmlTextTokenizer obtainTokenizer() {
    var res = tokenizerProvider.get();
    if (res == null) {
      res = new HtmlTextTokenizer("");
      tokenizerProvider.set(res);
    }
    return res;
  }

  private synchronized TsxTextTokenizer obtainJsTokenizer() {
    var res = jsTokenizerProvider.get();
    if (res == null) {
      res = new TsxTextTokenizer("");
      jsTokenizerProvider.set(res);
    }
    return res;
  }

  private synchronized SassTextTokenizer obtainCssTokenizer() {
    var res = cssTokenizerProvider.get();
    if (res == null) {
      res = new SassTextTokenizer("");
      cssTokenizerProvider.set(res);
    }
    return res;
  }


  private boolean isVoidElement(String tagName) {
    if (tagName == null) return false;
    switch (tagName.toLowerCase()) {
      case "area":
      case "base":
      case "br":
      case "col":
      case "embed":
      case "hr":
      case "img":
      case "input":
      case "link":
      case "meta":
      case "param":
      case "source":
      case "track":
      case "wbr":
        return true;
      default:
        return false;
    }
  }

  @Override
  public List<CodeBlock> computeBlocks(Content text, CodeBlockAnalyzeDelegate delegate) {
    var blocks = new ArrayList<CodeBlock>();
    Deque<int[]> tagStack = new ArrayDeque<>();
    Deque<int[]> jsStack = new ArrayDeque<>();
    Deque<int[]> cssStack = new ArrayDeque<>();

    boolean pendingOpen = false;
    boolean pendingClose = false;
    int pendingLine = -1;
    int pendingColumn = -1;
    String pendingTagName = null;

    for (int i = 0; i < text.getLineCount() && delegate.isNotCancelled(); i++) {
      var state = getState(i);
      for (var tokenRecord : state.tokens) {
        int offset = tokenRecord.offset;

        if (tokenRecord.jsToken != null) {
          var jt = tokenRecord.jsToken;
          if (jt == TsxTokens.LBRACE) {
            jsStack.push(new int[] {i, offset});
          } else if (jt == TsxTokens.RBRACE) {
            if (!jsStack.isEmpty()) {
              var start = jsStack.pop();
              if (start[0] != i) {
                CodeBlock b = new CodeBlock();
                b.startLine = start[0];
                b.startColumn = start[1];
                b.endLine = i;
                b.endColumn = offset;
                blocks.add(b);
              }
            }
          }
          continue;
        }

        if (tokenRecord.cssToken != null) {
          var ct = tokenRecord.cssToken;
          if (ct == SassTokens.LBRACE) {
            cssStack.push(new int[] {i, offset});
          } else if (ct == SassTokens.RBRACE) {
            if (!cssStack.isEmpty()) {
              var start = cssStack.pop();
              if (start[0] != i) {
                CodeBlock b = new CodeBlock();
                b.startLine = start[0];
                b.startColumn = start[1];
                b.endLine = i;
                b.endColumn = offset;
                blocks.add(b);
              }
            }
          }
          continue;
        }

        var token = tokenRecord.token;
        if (token == HtmlTokens.LT) {
          pendingOpen = true;
          pendingClose = false;
          pendingLine = i;
          pendingColumn = offset;
          pendingTagName = null;
        } else if (token == HtmlTokens.LT_SLASH) {
          pendingOpen = false;
          pendingClose = true;
          pendingLine = i;
          pendingColumn = offset;
          pendingTagName = null;
        } else if (token == HtmlTokens.TAG_NAME) {
          pendingTagName = tokenRecord.text;
        } else if (token == HtmlTokens.GT) {
          if (pendingOpen && !isVoidElement(pendingTagName)) {
            tagStack.push(new int[] {pendingLine, pendingColumn});
          } else if (pendingClose) {
            if (!tagStack.isEmpty()) {
              var start = tagStack.pop();
              if (start[0] != i) {
                CodeBlock b = new CodeBlock();
                b.startLine = start[0];
                b.startColumn = start[1];
                b.endLine = i;
                b.endColumn = offset;
                blocks.add(b);
              }
            }
          }
          pendingOpen = false;
          pendingClose = false;
        } else if (token == HtmlTokens.SLASH_GT) {
          pendingOpen = false;
          pendingClose = false;
        }
      }
    }
    return blocks;
  }

  @NonNull
  @Override
  public HtmlState getInitialState() {
    return new HtmlState();
  }

  @Override
  public boolean stateEquals(@NonNull HtmlState state, @NonNull HtmlState another) {
    return state.equals(another);
  }

  @Override
  public void onAddState(HtmlState state) {
    if (state.identifiers != null) {
      for (String identifier : state.identifiers) {
        identifiers.identifierIncrease(identifier);
      }
    }
  }

  @Override
  public void onAbandonState(HtmlState state) {
    if (state.identifiers != null) {
      for (String identifier : state.identifiers) {
        identifiers.identifierDecrease(identifier);
      }
    }
  }

  @Override
  public void reset(@NonNull ContentReference content, @NonNull Bundle extraArguments) {
    super.reset(content, extraArguments);
    identifiers.clear();
  }

  @Override
  public LineTokenizeResult<HtmlState, HighlightToken> tokenizeLine(
      CharSequence line, HtmlState state, int lineIndex) {
    var tokens = new ArrayList<HighlightToken>();
    var stateObj = new HtmlState();
    stateObj.startBracketDepth = state.bracketDepth;
    stateObj.bracketDepth = state.bracketDepth;
    stateObj.jsState = state.jsState;
    stateObj.jsStartBracketDepth = state.jsBracketDepth;
    stateObj.jsBracketDepth = state.jsBracketDepth;
    stateObj.cssState = state.cssState;
    stateObj.cssStartBracketDepth = state.cssBracketDepth;
    stateObj.cssBracketDepth = state.cssBracketDepth;
    var tokenizer = obtainTokenizer();
    if (state.state == HtmlTextTokenizer.STATE_TEXT) {
      tokenizer.reset(line);
    } else {
      tokenizer.resume(line, state.state, state.pendingTagName);
    }
    scanLine(tokenizer, tokens, stateObj, lineIndex);
    if (tokens.isEmpty()) {
      tokens.add(new HighlightToken(HtmlTokens.UNKNOWN, 0));
    }
    int mode = tokenizer.getMode();
    stateObj.state = mode;
    stateObj.pendingTagName =
        mode == HtmlTextTokenizer.STATE_TEXT ? "" : tokenizer.getCurrentTagName();
    if (mode != HtmlTextTokenizer.STATE_RAW_SCRIPT) {
      stateObj.jsState = JS_STATE_NORMAL;
      stateObj.jsBracketDepth = 0;
    }
    if (mode != HtmlTextTokenizer.STATE_RAW_STYLE) {
      stateObj.cssState = CSS_STATE_NORMAL;
      stateObj.cssBracketDepth = 0;
    }
    return new LineTokenizeResult<>(stateObj, tokens);
  }

  private void scanLine(
      HtmlTextTokenizer tokenizer, List<HighlightToken> tokens, HtmlState st, int lineIndex) {
    HtmlTokens token;
    boolean sawImagePreviewCandidate = false;
    while ((token = tokenizer.nextToken()) != HtmlTokens.EOF) {
      int mode = tokenizer.getMode();
      boolean wasScript = mode == HtmlTextTokenizer.STATE_RAW_SCRIPT;
      boolean wasStyle = mode == HtmlTextTokenizer.STATE_RAW_STYLE;
      if (wasScript && (token == HtmlTokens.RAW_TEXT || token == HtmlTokens.RAW_TEXT_INCOMPLETE)) {
        scanEmbeddedJs(tokenizer.getTokenText(), tokenizer.offset, tokens, st);
        continue;
      }
      if (wasStyle && (token == HtmlTokens.RAW_TEXT || token == HtmlTokens.RAW_TEXT_INCOMPLETE)) {
        scanEmbeddedCss(tokenizer.getTokenText(), tokenizer.offset, tokens, st);
        continue;
      }
      if (token == HtmlTokens.TAG_NAME || token == HtmlTokens.ATTR_NAME) {
        var ht = new HighlightToken(token, tokenizer.offset);
        ht.text = tokenizer.getTokenText().toString();
        tokens.add(ht);
        st.addIdentifier(tokenizer.getTokenText());
        continue;
      }
      if (token == HtmlTokens.ATTR_VALUE) {
        var ht = new HighlightToken(token, tokenizer.offset);
        ht.text = tokenizer.getTokenText().toString();
        tokens.add(ht);
        sawImagePreviewCandidate = true;
        String raw = ht.text;
        if (raw.length() >= 2
            && (raw.charAt(0) == '"' || raw.charAt(0) == '\'')
            && raw.charAt(raw.length() - 1) == raw.charAt(0)) {
          raw = raw.substring(1, raw.length() - 1);
        }
        loadImageToLine(raw, lineIndex);
        continue;
      }
      tokens.add(new HighlightToken(token, tokenizer.offset));
    }
    if (!sawImagePreviewCandidate && getManagedStyles() != null) {
      getManagedStyles().eraseLineStyle(lineIndex, LineSideIcon.class);
    }
  }

  private void scanEmbeddedJs(
      CharSequence chunk, int baseOffset, List<HighlightToken> tokens, HtmlState st) {
    var js = obtainJsTokenizer();
    int startFrom = 0;
    if (st.jsState == JS_STATE_INCOMPLETE_COMMENT) {
      int end = indexOfCommentEnd(chunk, 0);
      int commentEnd = end >= 0 ? end + 2 : chunk.length() - 1;
      tokens.add(
          new HighlightToken(HtmlTokens.RAW_TEXT, baseOffset, TsxTokens.BLOCK_COMMENT_COMPLETE));
      if (end < 0) {
        st.jsState = JS_STATE_INCOMPLETE_COMMENT;
        return;
      }
      st.jsState = JS_STATE_NORMAL;
      startFrom = commentEnd + 1;
      if (startFrom >= chunk.length()) return;
    }
    js.reset(chunk.subSequence(startFrom, chunk.length()));
    TsxTokens token;
    while ((token = js.nextToken()) != TsxTokens.EOF) {
      int tokOffset = baseOffset + startFrom + js.offset;
      if (token == TsxTokens.BLOCK_COMMENT_INCOMPLETE) {
        tokens.add(new HighlightToken(HtmlTokens.RAW_TEXT, tokOffset, token));
        st.jsState = JS_STATE_INCOMPLETE_COMMENT;
        return;
      }
      if (token == TsxTokens.LPAREN || token == TsxTokens.LBRACE || token == TsxTokens.LBRACK) {
        st.jsBracketDepth++;
      } else if (token == TsxTokens.RPAREN
          || token == TsxTokens.RBRACE
          || token == TsxTokens.RBRACK) {
        st.jsBracketDepth = Math.max(0, st.jsBracketDepth - 1);
      }
      tokens.add(new HighlightToken(HtmlTokens.RAW_TEXT, tokOffset, token));
    }
    st.jsState = JS_STATE_NORMAL;
  }

  private int indexOfCommentEnd(CharSequence chunk, int from) {
    for (int i = from; i + 1 < chunk.length(); i++) {
      if (chunk.charAt(i) == '*' && chunk.charAt(i + 1) == '/') {
        return i;
      }
    }
    return -1;
  }

  private void scanEmbeddedCss(
      CharSequence chunk, int baseOffset, List<HighlightToken> tokens, HtmlState st) {
    var css = obtainCssTokenizer();
    int startFrom = 0;
    if (st.cssState == CSS_STATE_INCOMPLETE_COMMENT) {
      int end = indexOfCommentEnd(chunk, 0);
      tokens.add(new HighlightToken(HtmlTokens.RAW_TEXT, baseOffset, SassTokens.BLOCK_COMMENT));
      if (end < 0) {
        st.cssState = CSS_STATE_INCOMPLETE_COMMENT;
        return;
      }
      st.cssState = CSS_STATE_NORMAL;
      startFrom = end + 2;
      if (startFrom >= chunk.length()) return;
    }
    css.reset(chunk.subSequence(startFrom, chunk.length()));
    SassTokens token;
    while ((token = css.nextToken()) != SassTokens.EOF) {
      int tokOffset = baseOffset + startFrom + css.offset;
      if (token == SassTokens.BLOCK_COMMENT) {
        String text =
            chunk
                .subSequence(startFrom + css.offset, startFrom + css.offset + css.length)
                .toString();
        if (!text.endsWith("*/")) {
          tokens.add(new HighlightToken(HtmlTokens.RAW_TEXT, tokOffset, token));
          st.cssState = CSS_STATE_INCOMPLETE_COMMENT;
          return;
        }
      }
      if (token == SassTokens.LPAREN || token == SassTokens.LBRACE || token == SassTokens.LBRACK) {
        st.cssBracketDepth++;
      } else if (token == SassTokens.RPAREN
          || token == SassTokens.RBRACE
          || token == SassTokens.RBRACK) {
        st.cssBracketDepth = Math.max(0, st.cssBracketDepth - 1);
      }
      tokens.add(new HighlightToken(HtmlTokens.RAW_TEXT, tokOffset, token));
    }
    st.cssState = CSS_STATE_NORMAL;
  }

  @Override
  public List<Span> generateSpansForLine(LineTokenizeResult<HtmlState, HighlightToken> lineResult) {
    var spans = new ArrayList<Span>();
    var tokens = lineResult.tokens;
    HtmlTokens previous = HtmlTokens.UNKNOWN;
    int jsDepth = lineResult.state.jsStartBracketDepth;
    int cssDepth = lineResult.state.cssStartBracketDepth;
    for (int i = 0; i < tokens.size(); i++) {
      var tokenRecord = tokens.get(i);
      int offset = tokenRecord.offset;
      Span span;
      if (tokenRecord.jsToken != null) {
        var jt = tokenRecord.jsToken;
        if (jt == TsxTokens.WHITESPACE || jt == TsxTokens.NEWLINE) {
          span = SpanFactory.obtain(offset, TextStyle.makeStyle(GhostColorScheme.TEXT_NORMAL));
        } else if (jt == TsxTokens.LINE_COMMENT
            || jt == TsxTokens.BLOCK_COMMENT_COMPLETE
            || jt == TsxTokens.BLOCK_COMMENT_INCOMPLETE) {
          span =
              SpanFactory.obtain(
                  offset,
                  TextStyle.makeStyle(GhostColorScheme.COMMENT, 0, false, true, false, true));
        } else if (JS_LITERALS.contains(jt)) {
          span = SpanFactory.obtain(offset, TextStyle.makeStyle(GhostColorScheme.LITERAL, true));
        } else if (JS_KEYWORDS.contains(jt)) {
          span =
              SpanFactory.obtain(
                  offset, TextStyle.makeStyle(GhostColorScheme.KEYWORD, 0, true, false, false));
        } else if (jt == TsxTokens.IDENTIFIER) {
          span = SpanFactory.obtain(offset, TextStyle.makeStyle(GhostColorScheme.TEXT_NORMAL));
        } else if (jt == TsxTokens.LPAREN || jt == TsxTokens.LBRACE || jt == TsxTokens.LBRACK) {
          span = SpanFactory.obtain(offset, bracketStyle(jsDepth));
          jsDepth++;
        } else if (jt == TsxTokens.RPAREN || jt == TsxTokens.RBRACE || jt == TsxTokens.RBRACK) {
          jsDepth = Math.max(0, jsDepth - 1);
          span = SpanFactory.obtain(offset, bracketStyle(jsDepth));
        } else {
          span = SpanFactory.obtain(offset, GhostColorScheme.OPERATOR);
        }
        spans.add(span);
        continue;
      }
      if (tokenRecord.cssToken != null) {
        var ct = tokenRecord.cssToken;
        SassTokens prevCss = i > 0 ? tokens.get(i - 1).cssToken : null;
        SassTokens nextCss = null;
        for (int j = i + 1; j < tokens.size(); j++) {
          if (tokens.get(j).cssToken == null) break;
          if (tokens.get(j).cssToken == SassTokens.WHITESPACE) continue;
          nextCss = tokens.get(j).cssToken;
          break;
        }
        if (ct == SassTokens.WHITESPACE || ct == SassTokens.NEWLINE) {
          span = SpanFactory.obtain(offset, TextStyle.makeStyle(GhostColorScheme.TEXT_NORMAL));
        } else if (ct == SassTokens.LINE_COMMENT || ct == SassTokens.BLOCK_COMMENT) {
          span =
              SpanFactory.obtain(
                  offset,
                  TextStyle.makeStyle(GhostColorScheme.COMMENT, 0, false, true, false, true));
        } else if (ct == SassTokens.AT_KEYWORD) {
          span =
              SpanFactory.obtain(
                  offset, TextStyle.makeStyle(GhostColorScheme.KEYWORD, 0, true, false, false));
        } else if (ct == SassTokens.VARIABLE) {
          span =
              SpanFactory.obtain(
                  offset,
                  TextStyle.makeStyle(GhostColorScheme.IDENTIFIER_VAR, 0, true, false, false));
        } else if (ct == SassTokens.PLACEHOLDER) {
          span =
              SpanFactory.obtain(
                  offset,
                  TextStyle.makeStyle(GhostColorScheme.IDENTIFIER_NAME, 0, true, false, false));
        } else if (ct == SassTokens.PARENT_SELECTOR || ct == SassTokens.INTERPOLATION_START) {
          span =
              SpanFactory.obtain(
                  offset, TextStyle.makeStyle(GhostColorScheme.KEYWORD, 0, true, false, false));
        } else if (ct == SassTokens.IDENT) {
          int color = GhostColorScheme.TEXT_NORMAL;
          if (prevCss == SassTokens.AT_KEYWORD) {
            color = GhostColorScheme.FUNCTION_NAME;
          } else if (prevCss == SassTokens.DOT) {
            color = GhostColorScheme.COLORNEXTDOT;
          } else if (prevCss == SassTokens.COLON) {
            color = GhostColorScheme.ATTRIBUTE_NAME;
          } else if (prevCss == SassTokens.PERCENT) {
            color = GhostColorScheme.COLORNEXTBRAK;
          } else if (prevCss == SassTokens.PARENT_SELECTOR) {
            color = GhostColorScheme.FUNCTION_NAME;
          } else if (prevCss == SassTokens.VARIABLE) {
            color = GhostColorScheme.ATTRIBUTE_NAME;
          } else if (nextCss == SassTokens.LPAREN) {
            color = GhostColorScheme.FUNCTION_NAME;
          } else if (nextCss == SassTokens.COLON) {
            color = GhostColorScheme.ATTRIBUTE_NAME;
          } else if (nextCss == SassTokens.LBRACE) {
            color = GhostColorScheme.LITERAL;
          }
          span = SpanFactory.obtain(offset, TextStyle.makeStyle(color));
        } else if (ct == SassTokens.STRING_LITERAL
            || ct == SassTokens.NUMBER
            || ct == SassTokens.UNIT
            || ct == SassTokens.COLOR_HEX) {
          span = SpanFactory.obtain(offset, TextStyle.makeStyle(GhostColorScheme.LITERAL, true));
        } else if (ct == SassTokens.LPAREN || ct == SassTokens.LBRACE || ct == SassTokens.LBRACK) {
          span = SpanFactory.obtain(offset, bracketStyle(cssDepth));
          cssDepth++;
        } else if (ct == SassTokens.RPAREN || ct == SassTokens.RBRACE || ct == SassTokens.RBRACK) {
          cssDepth = Math.max(0, cssDepth - 1);
          span = SpanFactory.obtain(offset, bracketStyle(cssDepth));
        } else {
          span = SpanFactory.obtain(offset, GhostColorScheme.OPERATOR);
        }
        spans.add(span);
        continue;
      }
      var token = tokenRecord.token;
      switch (token) {
        case WHITESPACE:
        case NEWLINE:
          span = SpanFactory.obtain(offset, TextStyle.makeStyle(GhostColorScheme.TEXT_NORMAL));
          break;
        case TEXT:
        case RAW_TEXT:
        case RAW_TEXT_INCOMPLETE:
          span = SpanFactory.obtain(offset, TextStyle.makeStyle(GhostColorScheme.TEXT_NORMAL));
          break;
        case ENTITY:
          span =
              SpanFactory.obtain(
                  offset, TextStyle.makeStyle(GhostColorScheme.LITERAL, 0, true, false, false));
          break;
        case COMMENT_COMPLETE:
        case COMMENT_INCOMPLETE:
          span =
              SpanFactory.obtain(
                  offset,
                  TextStyle.makeStyle(GhostColorScheme.COMMENT, 0, false, true, false, true));
          break;
        case DOCTYPE:
          span =
              SpanFactory.obtain(
                  offset, TextStyle.makeStyle(GhostColorScheme.KEYWORD, 0, true, false, false));
          break;
        case CDATA_COMPLETE:
        case CDATA_INCOMPLETE:
          span = SpanFactory.obtain(offset, TextStyle.makeStyle(GhostColorScheme.LITERAL, true));
          break;
        case LT:
        case LT_SLASH:
        case GT:
        case SLASH_GT:
          span =
              SpanFactory.obtain(
                  offset, TextStyle.makeStyle(GhostColorScheme.KEYWORD, 0, true, false, false));
          break;
        case TAG_NAME:
          span =
              SpanFactory.obtain(
                  offset,
                  TextStyle.makeStyle(GhostColorScheme.HTML_TAG, 0, true, false, false));
          break;
        case ATTR_NAME:
          span = SpanFactory.obtain(offset, TextStyle.makeStyle(GhostColorScheme.ATTRIBUTE_NAME));
          break;
        case ASSIGN:
          span = SpanFactory.obtain(offset, GhostColorScheme.OPERATOR);
          break;
        case ATTR_VALUE:
          span = SpanFactory.obtain(offset, TextStyle.makeStyle(GhostColorScheme.LITERAL, true));
          break;
        default:
          span = SpanFactory.obtain(offset, TextStyle.makeStyle(GhostColorScheme.TEXT_NORMAL));
      }
      previous = token;
      spans.add(span);
    }
    return spans;
  }

  public static class HighlightToken {

    public HtmlTokens token;

    public int offset;

    public TsxTokens jsToken;

    public SassTokens cssToken;

    public String text;

    public HighlightToken(HtmlTokens token, int offset) {
      this.token = token;
      this.offset = offset;
    }

    public HighlightToken(HtmlTokens token, int offset, TsxTokens jsToken) {
      this.token = token;
      this.offset = offset;
      this.jsToken = jsToken;
    }

    public HighlightToken(HtmlTokens token, int offset, SassTokens cssToken) {
      this.token = token;
      this.offset = offset;
      this.cssToken = cssToken;
    }
  }
}
