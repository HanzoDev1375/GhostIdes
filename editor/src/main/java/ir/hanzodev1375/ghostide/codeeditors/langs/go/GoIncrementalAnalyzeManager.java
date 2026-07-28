/**
 * Comment by ghost ide
 *
 * @author: Ninjacoder
 */
package ir.hanzodev1375.ghostide.codeeditors.langs.go;

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

public class GoIncrementalAnalyzeManager
    extends AsyncIncrementalAnalyzeManager<GoState, GoIncrementalAnalyzeManager.HighlightToken> {

  private static final int STATE_NORMAL = 0;

  private static final int STATE_INCOMPLETE_BLOCK_COMMENT = 1;

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

  private final ThreadLocal<GoTextTokenizer> tokenizerProvider = new ThreadLocal<>();

  protected IdentifierAutoComplete.SyncIdentifiers identifiers =
      new IdentifierAutoComplete.SyncIdentifiers();

  private synchronized GoTextTokenizer obtainTokenizer() {
    var res = tokenizerProvider.get();
    if (res == null) {
      res = new GoTextTokenizer("");
      tokenizerProvider.set(res);
    }
    return res;
  }

  @Override
  public List<CodeBlock> computeBlocks(Content text, CodeBlockAnalyzeDelegate delegate) {
    var stack = new Stack<CodeBlock>();
    var blocks = new ArrayList<CodeBlock>();
    var maxSwitch = 0;
    var currSwitch = 0;
    var brackets = new SimpleBracketsCollector();
    var bracketsStack = new Stack<Long>();
    for (int i = 0; i < text.getLineCount() && delegate.isNotCancelled(); i++) {
      var state = getState(i);
      boolean checkForIdentifiers =
          state.state.state == STATE_NORMAL
              || (state.state.state == STATE_INCOMPLETE_BLOCK_COMMENT && state.tokens.size() > 1);
      if (state.state.hasBraces || checkForIdentifiers) {
        for (int i1 = 0; i1 < state.tokens.size(); i1++) {
          var tokenRecord = state.tokens.get(i1);
          var token = tokenRecord.token;
          int offset = tokenRecord.offset;
          if (token == GoTokens.LBRACE) {
            if (stack.isEmpty()) {
              if (currSwitch > maxSwitch) maxSwitch = currSwitch;
              currSwitch = 0;
            }
            currSwitch++;
            CodeBlock block = new CodeBlock();
            block.startLine = i;
            block.startColumn = offset;
            stack.push(block);
          } else if (token == GoTokens.RBRACE) {
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

  private static int getType(GoTokens token) {
    if (token == GoTokens.LBRACE || token == GoTokens.RBRACE) return 3;
    if (token == GoTokens.LBRACK || token == GoTokens.RBRACK) return 2;
    if (token == GoTokens.LPAREN || token == GoTokens.RPAREN) return 1;
    return 0;
  }

  private static boolean isStart(GoTokens token) {
    return token == GoTokens.LBRACE || token == GoTokens.LBRACK || token == GoTokens.LPAREN;
  }

  @NonNull
  @Override
  public GoState getInitialState() {
    return new GoState();
  }

  @Override
  public boolean stateEquals(@NonNull GoState state, @NonNull GoState another) {
    return state.equals(another);
  }

  @Override
  public void onAddState(GoState state) {
    if (state.identifiers != null) {
      for (String identifier : state.identifiers) {
        identifiers.identifierIncrease(identifier);
      }
    }
  }

  @Override
  public void onAbandonState(GoState state) {
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
  public LineTokenizeResult<GoState, HighlightToken> tokenizeLine(
      CharSequence line, GoState state, int lineIndex) {
    var tokens = new ArrayList<HighlightToken>();
    int newState = STATE_NORMAL;
    var stateObj = new GoState();
    stateObj.startBracketDepth = state.bracketDepth;
    stateObj.bracketDepth = state.bracketDepth;
    if (state.state == STATE_NORMAL) {
      newState = tokenizeNormal(line, 0, tokens, stateObj);
    } else if (state.state == STATE_INCOMPLETE_BLOCK_COMMENT) {
      var res = tryFillIncompleteComment(line, tokens);
      newState = IntPair.getFirst(res);
      if (newState == STATE_NORMAL) {
        newState = tokenizeNormal(line, IntPair.getSecond(res), tokens, stateObj);
      } else {
        newState = STATE_INCOMPLETE_BLOCK_COMMENT;
      }
    }
    if (tokens.isEmpty()) {
      tokens.add(new HighlightToken(GoTokens.UNKNOWN, 0));
    }
    stateObj.state = newState;
    return new LineTokenizeResult<>(stateObj, tokens);
  }

  private long tryFillIncompleteComment(CharSequence line, List<HighlightToken> tokens) {
    char pre = '\0', cur = '\0';
    int offset = 0;
    while ((pre != '*' || cur != '/') && offset < line.length()) {
      pre = cur;
      cur = line.charAt(offset);
      offset++;
    }
    if (pre == '*' && cur == '/') {
      if (offset < 1000) {
        detectHighlightUrls(
            line.subSequence(0, offset), 0, GoTokens.BLOCK_COMMENT_COMPLETE, tokens);
      } else {
        tokens.add(new HighlightToken(GoTokens.BLOCK_COMMENT_COMPLETE, 0));
      }
      return IntPair.pack(STATE_NORMAL, offset);
    }
    if (offset < 1000) {
      detectHighlightUrls(
          line.subSequence(0, offset), 0, GoTokens.BLOCK_COMMENT_INCOMPLETE, tokens);
    } else {
      tokens.add(new HighlightToken(GoTokens.BLOCK_COMMENT_INCOMPLETE, 0));
    }
    return IntPair.pack(STATE_INCOMPLETE_BLOCK_COMMENT, offset);
  }

  private int tokenizeNormal(
      CharSequence text, int offset, List<HighlightToken> tokens, GoState st) {
    var tokenizer = obtainTokenizer();
    tokenizer.reset(text);
    tokenizer.offset = offset;
    GoTokens token;
    int state = STATE_NORMAL;
    while ((token = tokenizer.nextToken()) != GoTokens.EOF) {
      if (tokenizer.getTokenLength() < 1000
          && (token == GoTokens.STRING_LITERAL
              || token == GoTokens.CHAR_LITERAL
              || token == GoTokens.BLOCK_COMMENT_COMPLETE
              || token == GoTokens.BLOCK_COMMENT_INCOMPLETE
              || token == GoTokens.LINE_COMMENT)) {
        detectHighlightUrls(tokenizer.getTokenText(), tokenizer.offset, token, tokens);
        if (token == GoTokens.BLOCK_COMMENT_INCOMPLETE) {
          state = STATE_INCOMPLETE_BLOCK_COMMENT;
          break;
        }
        continue;
      }
      tokens.add(new HighlightToken(token, tokenizer.offset));
      if (token == GoTokens.LBRACE || token == GoTokens.RBRACE) {
        st.hasBraces = true;
      }
      if (token == GoTokens.LPAREN || token == GoTokens.LBRACE || token == GoTokens.LBRACK) {
        st.bracketDepth++;
      } else if (token == GoTokens.RPAREN || token == GoTokens.RBRACE || token == GoTokens.RBRACK) {
        st.bracketDepth = Math.max(0, st.bracketDepth - 1);
      }
      if (token == GoTokens.IDENTIFIER) {
        st.addIdentifier(tokenizer.getTokenText());
      }
      if (token == GoTokens.BLOCK_COMMENT_INCOMPLETE) {
        state = STATE_INCOMPLETE_BLOCK_COMMENT;
        break;
      }
    }
    return state;
  }

  private void detectHighlightUrls(
      CharSequence tokenText, int offset, GoTokens token, List<HighlightToken> tokens) {
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
  public List<Span> generateSpansForLine(LineTokenizeResult<GoState, HighlightToken> lineResult) {
    var spans = new ArrayList<Span>();
    var tokens = lineResult.tokens;
    GoTokens previous = GoTokens.UNKNOWN;
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
        case BLOCK_COMMENT_COMPLETE:
        case BLOCK_COMMENT_INCOMPLETE:
          span =
              SpanFactory.obtain(
                  offset,
                  TextStyle.makeStyle(GhostColorScheme.COMMENT, 0, false, true, false, true));
          break;
        case BREAK:
        case CASE:
        case CHAN:
        case CONST:
        case CONTINUE:
        case DEFAULT:
        case DEFER:
        case ELSE:
        case FALLTHROUGH:
        case FOR:
        case FUNC:
        case GO:
        case GOTO:
        case IF:
        case IMPORT:
        case INTERFACE:
        case MAP:
        case PACKAGE:
        case RANGE:
        case RETURN:
        case SELECT:
        case STRUCT:
        case SWITCH:
        case TYPE:
        case VAR:
          span =
              SpanFactory.obtain(
                  offset, TextStyle.makeStyle(GhostColorScheme.KEYWORD, 0, true, false, false));
          break;
        case NIL:
        case TRUE:
        case FALSE:
        case IOTA:
          span =
              SpanFactory.obtain(
                  offset, TextStyle.makeStyle(GhostColorScheme.KEYWORD, 0, true, false, false));
          break;
        case INTEGER_LITERAL:
        case FLOATING_LITERAL:
        case STRING_LITERAL:
        case CHAR_LITERAL:
          span = SpanFactory.obtain(offset, TextStyle.makeStyle(GhostColorScheme.LITERAL, true));
          break;
        case IDENTIFIER:
          int color = GhostColorScheme.TEXT_NORMAL;
          if (previous == GoTokens.DOT) {
            color = GhostColorScheme.COLORNEXTDOT;
          } else if (previous == GoTokens.VAR || previous == GoTokens.CONST) {
            color = GhostColorScheme.COLORNEXTBRAK;
          } else if (previous == GoTokens.FUNC) {
            color = GhostColorScheme.ATTRIBUTE_NAME;
          } else if (previous == GoTokens.RETURN || previous == GoTokens.CASE) {
            color = GhostColorScheme.FUNCTION_NAME;
          } else {
            int j = i + 1;
            var next = GoTokens.UNKNOWN;
            while (j < tokens.size()) {
              var n = tokens.get(j).token;
              if (n != GoTokens.WHITESPACE
                  && n != GoTokens.NEWLINE
                  && n != GoTokens.BLOCK_COMMENT_COMPLETE
                  && n != GoTokens.BLOCK_COMMENT_INCOMPLETE
                  && n != GoTokens.LINE_COMMENT) {
                next = n;
                break;
              }
              j++;
            }
            if (next == GoTokens.LPAREN) {
              color = GhostColorScheme.FUNCTION_NAME;
            } else if (next == GoTokens.LT) {
              color = GhostColorScheme.ANNOTATION;
            } else if (next == GoTokens.ASSIGN) {
              color = GhostColorScheme.ATTRIBUTE_NAME;
            }
          }
          span = SpanFactory.obtain(offset, TextStyle.makeStyle(color));
          break;
        case LPAREN:
        case LBRACE:
        case LBRACK:
          span = SpanFactory.obtain(offset, bracketStyle(depth));
          depth++;
          break;
        case RPAREN:
        case RBRACE:
        case RBRACK:
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
        case BLOCK_COMMENT_COMPLETE:
        case BLOCK_COMMENT_INCOMPLETE:
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

    public GoTokens token;

    public int offset;

    public String url;

    public HighlightToken(GoTokens token, int offset) {
      this.token = token;
      this.offset = offset;
    }

    public HighlightToken(GoTokens token, int offset, String url) {
      this.token = token;
      this.offset = offset;
      this.url = url;
    }
  }
}
