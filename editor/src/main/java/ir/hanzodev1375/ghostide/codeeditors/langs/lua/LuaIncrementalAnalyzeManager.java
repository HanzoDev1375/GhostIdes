/**
 * Comment by ghost ide
 *
 * @author: Ninjacoder
 */
package ir.hanzodev1375.ghostide.codeeditors.langs.lua;

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

public class LuaIncrementalAnalyzeManager
    extends AsyncIncrementalAnalyzeManager<LuaState, LuaIncrementalAnalyzeManager.HighlightToken> {

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

  private final ThreadLocal<LuaTextTokenizer> tokenizerProvider = new ThreadLocal<>();

  protected IdentifierAutoComplete.SyncIdentifiers identifiers =
      new IdentifierAutoComplete.SyncIdentifiers();

  private synchronized LuaTextTokenizer obtainTokenizer() {
    var res = tokenizerProvider.get();
    if (res == null) {
      res = new LuaTextTokenizer("");
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
          if (token == LuaTokens.LBRACE) {
            if (stack.isEmpty()) {
              if (currSwitch > maxSwitch) maxSwitch = currSwitch;
              currSwitch = 0;
            }
            currSwitch++;
            CodeBlock block = new CodeBlock();
            block.startLine = i;
            block.startColumn = offset;
            stack.push(block);
          } else if (token == LuaTokens.RBRACE) {
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

  private static int getType(LuaTokens token) {
    if (token == LuaTokens.LBRACE || token == LuaTokens.RBRACE) return 3;
    if (token == LuaTokens.LBRACK || token == LuaTokens.RBRACK) return 2;
    if (token == LuaTokens.LPAREN || token == LuaTokens.RPAREN) return 1;
    return 0;
  }

  private static boolean isStart(LuaTokens token) {
    return token == LuaTokens.LBRACE || token == LuaTokens.LBRACK || token == LuaTokens.LPAREN;
  }

  @NonNull
  @Override
  public LuaState getInitialState() {
    return new LuaState();
  }

  @Override
  public boolean stateEquals(@NonNull LuaState state, @NonNull LuaState another) {
    return state.equals(another);
  }

  @Override
  public void onAddState(LuaState state) {
    if (state.identifiers != null) {
      for (String identifier : state.identifiers) {
        identifiers.identifierIncrease(identifier);
      }
    }
  }

  @Override
  public void onAbandonState(LuaState state) {
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
  public LineTokenizeResult<LuaState, HighlightToken> tokenizeLine(
      CharSequence line, LuaState state, int lineIndex) {
    var tokens = new ArrayList<HighlightToken>();
    int newState = STATE_NORMAL;
    var stateObj = new LuaState();
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
      tokens.add(new HighlightToken(LuaTokens.UNKNOWN, 0));
    }
    stateObj.state = newState;
    return new LineTokenizeResult<>(stateObj, tokens);
  }

  private long tryFillIncompleteComment(CharSequence line, List<HighlightToken> tokens) {
    int offset = 0;
    char pre = '\0', cur = '\0';
    while (offset < line.length()) {
      pre = cur;
      cur = line.charAt(offset);
      offset++;
      if (pre == ']' && cur == ']') break;
    }
    if (pre == ']' && cur == ']') {
      if (offset < 1000) {
        detectHighlightUrls(
            line.subSequence(0, offset), 0, LuaTokens.BLOCK_COMMENT_COMPLETE, tokens);
      } else {
        tokens.add(new HighlightToken(LuaTokens.BLOCK_COMMENT_COMPLETE, 0));
      }
      return IntPair.pack(STATE_NORMAL, offset);
    }
    if (offset < 1000) {
      detectHighlightUrls(
          line.subSequence(0, offset), 0, LuaTokens.BLOCK_COMMENT_INCOMPLETE, tokens);
    } else {
      tokens.add(new HighlightToken(LuaTokens.BLOCK_COMMENT_INCOMPLETE, 0));
    }
    return IntPair.pack(STATE_INCOMPLETE_BLOCK_COMMENT, offset);
  }

  private int tokenizeNormal(
      CharSequence text, int offset, List<HighlightToken> tokens, LuaState st) {
    var tokenizer = obtainTokenizer();
    tokenizer.reset(text);
    tokenizer.offset = offset;
    LuaTokens token;
    int state = STATE_NORMAL;
    while ((token = tokenizer.nextToken()) != LuaTokens.EOF) {
      if (tokenizer.getTokenLength() < 1000
          && (token == LuaTokens.STRING_LITERAL
              || token == LuaTokens.BLOCK_COMMENT_COMPLETE
              || token == LuaTokens.BLOCK_COMMENT_INCOMPLETE
              || token == LuaTokens.LINE_COMMENT)) {
        detectHighlightUrls(tokenizer.getTokenText(), tokenizer.offset, token, tokens);
        if (token == LuaTokens.BLOCK_COMMENT_INCOMPLETE) {
          state = STATE_INCOMPLETE_BLOCK_COMMENT;
          break;
        }
        continue;
      }
      tokens.add(new HighlightToken(token, tokenizer.offset));
      if (token == LuaTokens.LBRACE || token == LuaTokens.RBRACE) {
        st.hasBraces = true;
      }
      if (token == LuaTokens.LPAREN || token == LuaTokens.LBRACE || token == LuaTokens.LBRACK) {
        st.bracketDepth++;
      } else if (token == LuaTokens.RPAREN || token == LuaTokens.RBRACE || token == LuaTokens.RBRACK) {
        st.bracketDepth = Math.max(0, st.bracketDepth - 1);
      }
      if (token == LuaTokens.IDENTIFIER) {
        st.addIdentifier(tokenizer.getTokenText());
      }
      if (token == LuaTokens.BLOCK_COMMENT_INCOMPLETE) {
        state = STATE_INCOMPLETE_BLOCK_COMMENT;
        break;
      }
    }
    return state;
  }

  private void detectHighlightUrls(
      CharSequence tokenText, int offset, LuaTokens token, List<HighlightToken> tokens) {
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
  public List<Span> generateSpansForLine(LineTokenizeResult<LuaState, HighlightToken> lineResult) {
    var spans = new ArrayList<Span>();
    var tokens = lineResult.tokens;
    LuaTokens previous = LuaTokens.UNKNOWN;
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
        case AND_KW:
        case BREAK:
        case DO:
        case ELSE:
        case ELSEIF:
        case END:
        case FALSE:
        case FOR:
        case FUNCTION:
        case GOTO:
        case IF:
        case IN:
        case LOCAL:
        case NIL:
        case NOT_KW:
        case OR_KW:
        case REPEAT:
        case RETURN:
        case THEN:
        case TRUE:
        case UNTIL:
        case WHILE:
          span =
              SpanFactory.obtain(
                  offset, TextStyle.makeStyle(GhostColorScheme.KEYWORD, 0, true, false, false));
          break;
        case INTEGER_LITERAL:
        case FLOATING_LITERAL:
        case STRING_LITERAL:
          span = SpanFactory.obtain(offset, TextStyle.makeStyle(GhostColorScheme.LITERAL, true));
          break;
        case IDENTIFIER:
          int color = GhostColorScheme.TEXT_NORMAL;
          if (previous == LuaTokens.DOT || previous == LuaTokens.COLON) {
            color = GhostColorScheme.COLORNEXTDOT;
          } else if (previous == LuaTokens.LOCAL || previous == LuaTokens.AND) {
            color = GhostColorScheme.COLORNEXTCHAR;
          } else {
            int j = i + 1;
            var next = LuaTokens.UNKNOWN;
            while (j < tokens.size()) {
              var n = tokens.get(j).token;
              if (n != LuaTokens.WHITESPACE
                  && n != LuaTokens.NEWLINE
                  && n != LuaTokens.BLOCK_COMMENT_COMPLETE
                  && n != LuaTokens.BLOCK_COMMENT_INCOMPLETE
                  && n != LuaTokens.LINE_COMMENT) {
                next = n;
                break;
              }
              j++;
            }
            if (next == LuaTokens.LPAREN) {
              color = GhostColorScheme.FUNCTION_NAME;
            }else if(next == LuaTokens.ASSIGN) {
            	color = GhostColorScheme.ATTRIBUTE_VALUE;
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

    public LuaTokens token;

    public int offset;

    public String url;

    public HighlightToken(LuaTokens token, int offset) {
      this.token = token;
      this.offset = offset;
    }

    public HighlightToken(LuaTokens token, int offset, String url) {
      this.token = token;
      this.offset = offset;
      this.url = url;
    }
  }
}
