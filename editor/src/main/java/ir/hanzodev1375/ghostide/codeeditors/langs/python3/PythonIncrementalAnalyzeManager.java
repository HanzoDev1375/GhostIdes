/**
 * Comment by ghost ide
 *
 * @author: Ninjacoder
 */
package ir.hanzodev1375.ghostide.codeeditors.langs.python3;

import android.os.Bundle;
import androidx.annotation.NonNull;
import io.github.rosemoe.sora.lang.analysis.AsyncIncrementalAnalyzeManager;
import io.github.rosemoe.sora.lang.brackets.SimpleBracketsCollector;
import io.github.rosemoe.sora.lang.completion.IdentifierAutoComplete;
import io.github.rosemoe.sora.lang.styling.CodeBlock;
import io.github.rosemoe.sora.lang.styling.Span;
import io.github.rosemoe.sora.lang.styling.SpanFactory;
import io.github.rosemoe.sora.lang.styling.TextStyle;
import io.github.rosemoe.sora.lang.styling.color.EditorColor;
import io.github.rosemoe.sora.lang.styling.span.SpanClickableUrl;
import io.github.rosemoe.sora.lang.styling.span.SpanExtAttrs;
import io.github.rosemoe.sora.text.Content;
import io.github.rosemoe.sora.text.ContentReference;
import io.github.rosemoe.sora.util.IntPair;
import ir.hanzodev1375.ghostide.codeeditors.colorscheme.GhostColorScheme;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Pattern;

public class PythonIncrementalAnalyzeManager
    extends AsyncIncrementalAnalyzeManager<
        PythonState, PythonIncrementalAnalyzeManager.HighlightToken> {

  private static final int STATE_NORMAL = 0;

  private static final int STATE_INCOMPLETE_STRING = 1;

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

  private static final Pattern URL_PATTERN =
      Pattern.compile(
          "https?:\\/\\/(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_\\+.~#?&/=]*)");

  private final ThreadLocal<PythonTextTokenizer> tokenizerProvider = new ThreadLocal<>();

  protected IdentifierAutoComplete.SyncIdentifiers identifiers =
      new IdentifierAutoComplete.SyncIdentifiers();

  private synchronized PythonTextTokenizer obtainTokenizer() {
    var res = tokenizerProvider.get();
    if (res == null) {
      res = new PythonTextTokenizer("");
      tokenizerProvider.set(res);
    }
    return res;
  }

  @Override
  public List<CodeBlock> computeBlocks(Content text, CodeBlockAnalyzeDelegate delegate) {
    var stack = new Stack<CodeBlock>();
    var blocks = new ArrayList<CodeBlock>();
    var brackets = new SimpleBracketsCollector();
    var bracketsStack = new Stack<Long>();
    for (int i = 0; i < text.getLineCount() && delegate.isNotCancelled(); i++) {
      var state = getState(i);
      if (state.state.hasBraces || state.state.state == STATE_NORMAL) {
        for (int i1 = 0; i1 < state.tokens.size(); i1++) {
          var tokenRecord = state.tokens.get(i1);
          var token = tokenRecord.token;
          int offset = tokenRecord.offset;
          if (token == PythonTokens.LBRACE || token == PythonTokens.FSTRING_EXPR_START) {
            CodeBlock block = new CodeBlock();
            block.startLine = i;
            block.startColumn = offset;
            stack.push(block);
          } else if (token == PythonTokens.RBRACE || token == PythonTokens.FSTRING_EXPR_END) {
            if (!stack.isEmpty()) {
              CodeBlock block = stack.pop();
              block.endLine = i;
              block.endColumn = offset;
              if (block.startLine != block.endLine) {
                blocks.add(block);
              }
            }
          }

          var type = getType(token);
          if (type > 0) {
            if (isStart(token)) {
              bracketsStack.push(IntPair.pack(type, text.getCharIndex(i, offset)));
            } else {
              if (!bracketsStack.isEmpty()) {
                var record = bracketsStack.pop();
                var typeRecord = IntPair.getFirst(record);
                if (typeRecord == type) {
                  brackets.add(IntPair.getSecond(record), text.getCharIndex(i, offset));
                } else if (type == 3) {
                  while (!bracketsStack.isEmpty()) {
                    record = bracketsStack.pop();
                    if (IntPair.getFirst(record) == 3) {
                      brackets.add(IntPair.getSecond(record), text.getCharIndex(i, offset));
                      break;
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
    if (delegate.isNotCancelled()) {
      withReceiver(r -> r.updateBracketProvider(this, brackets));
    }
    return blocks;
  }

  private static int getType(PythonTokens token) {
    if (token == PythonTokens.LBRACE
        || token == PythonTokens.RBRACE
        || token == PythonTokens.FSTRING_EXPR_START
        || token == PythonTokens.FSTRING_EXPR_END) return 3;
    if (token == PythonTokens.LBRACK || token == PythonTokens.RBRACK) return 2;
    if (token == PythonTokens.LPAREN || token == PythonTokens.RPAREN) return 1;
    return 0;
  }

  private static boolean isStart(PythonTokens token) {
    return token == PythonTokens.LBRACE
        || token == PythonTokens.FSTRING_EXPR_START
        || token == PythonTokens.LBRACK
        || token == PythonTokens.LPAREN;
  }

  @NonNull
  @Override
  public PythonState getInitialState() {
    return new PythonState();
  }

  @Override
  public boolean stateEquals(@NonNull PythonState state, @NonNull PythonState another) {
    return state.equals(another);
  }

  @Override
  public void onAddState(PythonState state) {
    if (state.identifiers != null) {
      for (String identifier : state.identifiers) {
        identifiers.identifierIncrease(identifier);
      }
    }
  }

  @Override
  public void onAbandonState(PythonState state) {
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
  public LineTokenizeResult<PythonState, HighlightToken> tokenizeLine(
      CharSequence line, PythonState state, int lineIndex) {
    var tokens = new ArrayList<HighlightToken>();
    var stateObj = new PythonState();
    stateObj.startBracketDepth = state.bracketDepth;
    stateObj.bracketDepth = state.bracketDepth;
    var tokenizer = obtainTokenizer();
    if (state.state == STATE_INCOMPLETE_STRING) {
      tokenizer.resumeTripleQuoteContinuation(
          line, state.pendingQuoteChar, state.pendingIsFString, state.pendingIsRaw);
    } else {
      tokenizer.reset(line);
    }
    scanLine(tokenizer, tokens, stateObj);
    if (tokens.isEmpty()) {
      tokens.add(new HighlightToken(PythonTokens.UNKNOWN, 0));
    }
    if (tokenizer.getMode() == 1) {
      stateObj.state = STATE_INCOMPLETE_STRING;
      stateObj.pendingQuoteChar = tokenizer.getPendingQuoteChar();
      stateObj.pendingIsFString = tokenizer.isPendingFString();
      stateObj.pendingIsRaw = tokenizer.isPendingRaw();
    } else {
      stateObj.state = STATE_NORMAL;
    }
    return new LineTokenizeResult<>(stateObj, tokens);
  }

  private void scanLine(
      PythonTextTokenizer tokenizer, List<HighlightToken> tokens, PythonState st) {
    PythonTokens token;
    while ((token = tokenizer.nextToken()) != PythonTokens.EOF) {
      if (tokenizer.getTokenLength() < 1000
          && (token == PythonTokens.STRING_LITERAL
              || token == PythonTokens.STRING_INCOMPLETE
              || token == PythonTokens.LINE_COMMENT)) {
        detectHighlightUrls(tokenizer.getTokenText(), tokenizer.offset, token, tokens);
        continue;
      }
      tokens.add(new HighlightToken(token, tokenizer.offset));
      if (token == PythonTokens.LBRACE || token == PythonTokens.RBRACE) {
        st.hasBraces = true;
      }
      if (token == PythonTokens.LPAREN
          || token == PythonTokens.LBRACE
          || token == PythonTokens.LBRACK
          || token == PythonTokens.FSTRING_EXPR_START) {
        st.bracketDepth++;
      } else if (token == PythonTokens.RPAREN
          || token == PythonTokens.RBRACE
          || token == PythonTokens.RBRACK
          || token == PythonTokens.FSTRING_EXPR_END) {
        st.bracketDepth = Math.max(0, st.bracketDepth - 1);
      }
      if (token == PythonTokens.IDENTIFIER) {
        st.addIdentifier(tokenizer.getTokenText());
      }
    }
  }

  private void detectHighlightUrls(
      CharSequence tokenText, int offset, PythonTokens token, List<HighlightToken> tokens) {
    var matcher = URL_PATTERN.matcher(tokenText);
    int index = 0;
    while (index < tokenText.length() && matcher.find(index)) {
      int start = matcher.start();
      int end = matcher.end();
      if (start > index) {
        tokens.add(new HighlightToken(token, offset + index));
      }
      tokens.add(new HighlightToken(token, offset + start, matcher.group()));
      index = end;
    }
    if (index != tokenText.length()) {
      tokens.add(new HighlightToken(token, offset + index));
    }
  }

  @Override
  public List<Span> generateSpansForLine(
      LineTokenizeResult<PythonState, HighlightToken> lineResult) {
    var spans = new ArrayList<Span>();
    var tokens = lineResult.tokens;
    PythonTokens previous = PythonTokens.UNKNOWN;
    int depth = lineResult.state.startBracketDepth;
    for (int i = 0; i < tokens.size(); i++) {
      var tokenRecord = tokens.get(i);
      var token = tokenRecord.token;
      int offset = tokenRecord.offset;
      Span span;
      switch (token) {
        case WHITESPACE:
        case NEWLINE:
          span = SpanFactory.obtain(offset, TextStyle.makeStyle(GhostColorScheme.TEXT_NORMAL));
          break;
        case LINE_COMMENT:
          span =
              SpanFactory.obtain(
                  offset,
                  TextStyle.makeStyle(GhostColorScheme.COMMENT, 0, false, true, false, true));
          break;
        case STRING_LITERAL:
        case STRING_INCOMPLETE:
        case INTEGER_LITERAL:
        case FLOATING_LITERAL:
        case IMAGINARY_LITERAL:
          span = SpanFactory.obtain(offset, TextStyle.makeStyle(GhostColorScheme.LITERAL, true));
          break;
        case FALSE_:
        case NONE_:
        case TRUE_:
        case AND:
        case AS:
        case ASSERT:
        case ASYNC:
        case AWAIT:
        case BREAK:
        case CLASS:
        case CONTINUE:
        case DEF:
        case DEL:
        case ELIF:
        case ELSE:
        case EXCEPT:
        case FINALLY:
        case FOR:
        case FROM:
        case GLOBAL:
        case IF:
        case IMPORT:
        case IN:
        case IS:
        case LAMBDA:
        case NONLOCAL:
        case NOT:
        case OR:
        case PASS:
        case RAISE:
        case RETURN:
        case TRY:
        case WHILE:
        case WITH:
        case YIELD:
          span =
              SpanFactory.obtain(
                  offset, TextStyle.makeStyle(GhostColorScheme.KEYWORD, 0, true, false, false));
          break;
        case IDENTIFIER:
          int color = GhostColorScheme.TEXT_NORMAL;
          if (previous == PythonTokens.CLASS) {
            color = GhostColorScheme.COLORUPPERCASE;
          } else if (previous == PythonTokens.DOT) {
            color = GhostColorScheme.COLORNEXTDOT;
          } else if (previous == PythonTokens.AT) {
            color = GhostColorScheme.ANNOTATION;
          } else {
            int j = i + 1;
            var next = PythonTokens.UNKNOWN;
            while (j < tokens.size()) {
              var n = tokens.get(j).token;
              if (n != PythonTokens.WHITESPACE
                  && n != PythonTokens.NEWLINE
                  && n != PythonTokens.LINE_COMMENT) {
                next = n;
                break;
              }
              j++;
            }
            if (next == PythonTokens.LPAREN) {
              color = GhostColorScheme.FUNCTION_NAME;
            }
          }
          span = SpanFactory.obtain(offset, TextStyle.makeStyle(color));
          break;
        case LPAREN:
        case LBRACE:
        case LBRACK:
        case FSTRING_EXPR_START:
          span = SpanFactory.obtain(offset, bracketStyle(depth));
          depth++;
          break;
        case RPAREN:
        case RBRACE:
        case RBRACK:
        case FSTRING_EXPR_END:
          depth = Math.max(0, depth - 1);
          span = SpanFactory.obtain(offset, bracketStyle(depth));
          break;
        default:
          span = SpanFactory.obtain(offset, GhostColorScheme.OPERATOR);
      }

      switch (token) {
        case WHITESPACE:
        case NEWLINE:
        case LINE_COMMENT:
          break;
        default:
          previous = token;
      }
      if (tokenRecord.url != null) {
        span = SpanFactory.obtain(span.getColumn(), span.getStyle());
        span.setSpanExt(SpanExtAttrs.EXT_INTERACTION_INFO, new SpanClickableUrl(tokenRecord.url));
        span.setUnderlineColor(new EditorColor(span.getForegroundColorId()));
      }
      spans.add(span);
    }
    return spans;
  }

  public static class HighlightToken {

    public PythonTokens token;

    public int offset;

    public String url;

    public HighlightToken(PythonTokens token, int offset) {
      this.token = token;
      this.offset = offset;
    }

    public HighlightToken(PythonTokens token, int offset, String url) {
      this.token = token;
      this.offset = offset;
      this.url = url;
    }
  }
}
