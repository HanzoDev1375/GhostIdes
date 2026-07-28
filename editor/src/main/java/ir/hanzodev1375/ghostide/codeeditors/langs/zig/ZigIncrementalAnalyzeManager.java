/**
 * Comment by ghost ide
 *
 * @author: Ninjacoder
 */
package ir.hanzodev1375.ghostide.codeeditors.langs.zig;

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

public class ZigIncrementalAnalyzeManager
    extends AsyncIncrementalAnalyzeManager<ZigState, ZigIncrementalAnalyzeManager.HighlightToken> {

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

  private final ThreadLocal<ZigTextTokenizer> tokenizerProvider = new ThreadLocal<>();

  protected IdentifierAutoComplete.SyncIdentifiers identifiers =
      new IdentifierAutoComplete.SyncIdentifiers();

  private synchronized ZigTextTokenizer obtainTokenizer() {
    var res = tokenizerProvider.get();
    if (res == null) {
      res = new ZigTextTokenizer("");
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
      boolean checkForIdentifiers =
          state.state.state == STATE_NORMAL
              || (state.state.state == STATE_INCOMPLETE_BLOCK_COMMENT && state.tokens.size() > 1);
      if (state.state.hasBraces || checkForIdentifiers) {
        for (var tokenRecord : state.tokens) {
          var token = tokenRecord.token;
          int offset = tokenRecord.offset;
          if (token == ZigTokens.LBRACE) {
            CodeBlock block = new CodeBlock();
            block.startLine = i;
            block.startColumn = offset;
            stack.push(block);
          } else if (token == ZigTokens.RBRACE) {
            if (!stack.isEmpty()) {
              CodeBlock block = stack.pop();
              block.endLine = i;
              block.endColumn = offset;
              if (block.startLine != block.endLine) {
                blocks.add(block);
              }
            }
          }
          int type = getType(token);
          if (type > 0) {
            if (isStart(token)) {
              bracketsStack.push(IntPair.pack(type, text.getCharIndex(i, offset)));
            } else {
              if (!bracketsStack.isEmpty()) {
                var record = bracketsStack.pop();
                int typeRecord = IntPair.getFirst(record);
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

  private static int getType(ZigTokens token) {
    if (token == ZigTokens.LBRACE || token == ZigTokens.RBRACE) return 3;
    if (token == ZigTokens.LBRACK || token == ZigTokens.RBRACK) return 2;
    if (token == ZigTokens.LPAREN || token == ZigTokens.RPAREN) return 1;
    return 0;
  }

  private static boolean isStart(ZigTokens token) {
    return token == ZigTokens.LBRACE || token == ZigTokens.LBRACK || token == ZigTokens.LPAREN;
  }

  @NonNull
  @Override
  public ZigState getInitialState() {
    return new ZigState();
  }

  @Override
  public boolean stateEquals(@NonNull ZigState state, @NonNull ZigState another) {
    return state.equals(another);
  }

  @Override
  public void onAddState(ZigState state) {
    if (state.identifiers != null) {
      for (String identifier : state.identifiers) {
        identifiers.identifierIncrease(identifier);
      }
    }
  }

  @Override
  public void onAbandonState(ZigState state) {
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
  public LineTokenizeResult<ZigState, HighlightToken> tokenizeLine(
      CharSequence line, ZigState state, int lineIndex) {
    var tokens = new ArrayList<HighlightToken>();
    int newState = STATE_NORMAL;
    var stateObj = new ZigState();
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
      tokens.add(new HighlightToken(ZigTokens.UNKNOWN, 0));
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
            line.subSequence(0, offset), 0, ZigTokens.BLOCK_COMMENT_COMPLETE, tokens);
      } else {
        tokens.add(new HighlightToken(ZigTokens.BLOCK_COMMENT_COMPLETE, 0));
      }
      return IntPair.pack(STATE_NORMAL, offset);
    }
    if (offset < 1000) {
      detectHighlightUrls(
          line.subSequence(0, offset), 0, ZigTokens.BLOCK_COMMENT_INCOMPLETE, tokens);
    } else {
      tokens.add(new HighlightToken(ZigTokens.BLOCK_COMMENT_INCOMPLETE, 0));
    }
    return IntPair.pack(STATE_INCOMPLETE_BLOCK_COMMENT, offset);
  }

  private int tokenizeNormal(
      CharSequence text, int offset, List<HighlightToken> tokens, ZigState st) {
    var tokenizer = obtainTokenizer();
    tokenizer.reset(text);
    tokenizer.offset = offset;
    ZigTokens token;
    int state = STATE_NORMAL;
    while ((token = tokenizer.nextToken()) != ZigTokens.EOF) {
      if (tokenizer.getTokenLength() < 1000
          && (token == ZigTokens.STRING_LITERAL
              || token == ZigTokens.CHARACTER_LITERAL
              || token == ZigTokens.BLOCK_COMMENT_COMPLETE
              || token == ZigTokens.BLOCK_COMMENT_INCOMPLETE
              || token == ZigTokens.LINE_COMMENT)) {
        detectHighlightUrls(tokenizer.getTokenText(), tokenizer.offset, token, tokens);
        if (token == ZigTokens.BLOCK_COMMENT_INCOMPLETE) {
          state = STATE_INCOMPLETE_BLOCK_COMMENT;
          break;
        }
        continue;
      }
      HighlightToken ht = new HighlightToken(token, tokenizer.offset);
      if (token == ZigTokens.IDENTIFIER
          || token == ZigTokens.PRIMITIVE_TYPE
          || token == ZigTokens.BUILTIN_FUNCTION) {
        ht.tokenText = tokenizer.getTokenText().toString();
      }
      tokens.add(ht);
      if (token == ZigTokens.LBRACE || token == ZigTokens.RBRACE) {
        st.hasBraces = true;
      }
      if (token == ZigTokens.LPAREN || token == ZigTokens.LBRACE || token == ZigTokens.LBRACK) {
        st.bracketDepth++;
      } else if (token == ZigTokens.RPAREN
          || token == ZigTokens.RBRACE
          || token == ZigTokens.RBRACK) {
        st.bracketDepth = Math.max(0, st.bracketDepth - 1);
      }
      if (token == ZigTokens.IDENTIFIER) {
        st.addIdentifier(tokenizer.getTokenText());
      }
      if (token == ZigTokens.BLOCK_COMMENT_INCOMPLETE) {
        state = STATE_INCOMPLETE_BLOCK_COMMENT;
        break;
      }
    }
    return state;
  }

  private void detectHighlightUrls(
      CharSequence tokenText, int offset, ZigTokens token, List<HighlightToken> tokens) {
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
  public List<Span> generateSpansForLine(LineTokenizeResult<ZigState, HighlightToken> lineResult) {
    var spans = new ArrayList<Span>();
    var tokens = lineResult.tokens;
    ZigTokens previous = ZigTokens.UNKNOWN;
    int depth = lineResult.state.startBracketDepth;
    boolean expectFnName = false;
    boolean expectType = false;
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
        case KEYWORD_ASM:
        case KEYWORD_BREAK:
        case KEYWORD_CALL:
        case KEYWORD_CATCH:
        case KEYWORD_COMPTIME:
        case KEYWORD_CONST:
        case KEYWORD_CONTINUE:
        case KEYWORD_DEFER:
        case KEYWORD_ELSE:
        case KEYWORD_ENUM:
        case KEYWORD_EXPORT:
        case KEYWORD_EXTERN:
        case KEYWORD_FN:
        case KEYWORD_FOR:
        case KEYWORD_IF:
        case KEYWORD_INLINE:
        case KEYWORD_NOINLINE:
        case KEYWORD_OPAQUE:
        case KEYWORD_OR:
        case KEYWORD_PACKED:
        case KEYWORD_PUB:
        case KEYWORD_RESUME:
        case KEYWORD_RETURN:
        case KEYWORD_STRUCT:
        case KEYWORD_SUSPEND:
        case KEYWORD_SWITCH:
        case KEYWORD_TEST:
        case KEYWORD_THREADLOCAL:
        case KEYWORD_TRY:
        case KEYWORD_UNION:
        case KEYWORD_UNREACHABLE:
        case KEYWORD_USING:
        case KEYWORD_VAR:
        case KEYWORD_VOLATILE:
        case KEYWORD_WHILE:
        case KEYWORD_ALLOWZERO:
        case KEYWORD_ANYTYPE:
        case KEYWORD_ANYFRAME:
        case KEYWORD_DIST:
        case KEYWORD_ERRDEFER:
        case KEYWORD_ERROR:
        case KEYWORD_EXTENSIBLE:
        case KEYWORD_INTERFACE:
        case KEYWORD_NOSUSPEND:
        case KEYWORD_PROTO:
        case KEYWORD_SECTION:
        case KEYWORD_TRUE:
        case KEYWORD_FALSE:
        case KEYWORD_NULL:
        case KEYWORD_UNDEFINED:
          span =
              SpanFactory.obtain(
                  offset, TextStyle.makeStyle(GhostColorScheme.KEYWORD, 0, true, false, false));
          if (token == ZigTokens.KEYWORD_FN) {
            expectFnName = true;
          } else if (token == ZigTokens.KEYWORD_STRUCT
              || token == ZigTokens.KEYWORD_ENUM
              || token == ZigTokens.KEYWORD_UNION
              || token == ZigTokens.KEYWORD_OPAQUE
              || token == ZigTokens.KEYWORD_PACKED) {
            expectType = true;
          }
          break;
        case PRIMITIVE_TYPE:
          span =
              SpanFactory.obtain(
                  offset,
                  TextStyle.makeStyle(GhostColorScheme.COLORNEXTCHAR, 0, true, false, false));
          break;
        case BUILTIN_FUNCTION:
          span =
              SpanFactory.obtain(
                  offset,
                  TextStyle.makeStyle(GhostColorScheme.FUNCTION_NAME, 0, true, false, false));
          break;
        case INTEGER_LITERAL:
        case FLOATING_LITERAL:
        case CHARACTER_LITERAL:
        case STRING_LITERAL:
        case BOOLEAN_LITERAL:
        case NULL_LITERAL:
          span = SpanFactory.obtain(offset, TextStyle.makeStyle(GhostColorScheme.LITERAL, true));
          break;
        case IDENTIFIER:
          int color = GhostColorScheme.TEXT_NORMAL;
          String text = tokenRecord.tokenText != null ? tokenRecord.tokenText : "";
          if (expectFnName) {
            color = GhostColorScheme.FUNCTION_NAME;
            expectFnName = false;
          } else if (expectType) {
            color = GhostColorScheme.COLORNEXTCHAR;
            expectType = false;
          } else if (previous == ZigTokens.DOT) {
            color = GhostColorScheme.COLORNEXTDOT;
          } else if (previous == ZigTokens.COLON) {
            color = GhostColorScheme.IDENTIFIER_NAME;
          } else if (previous == ZigTokens.ASSIGN || previous == ZigTokens.EQ) {
            color = GhostColorScheme.IDENTIFIER_VAR;
          } else {
            int j = i + 1;
            ZigTokens next = ZigTokens.UNKNOWN;
            while (j < tokens.size()) {
              ZigTokens n = tokens.get(j).token;
              if (n != ZigTokens.WHITESPACE
                  && n != ZigTokens.NEWLINE
                  && n != ZigTokens.BLOCK_COMMENT_COMPLETE
                  && n != ZigTokens.BLOCK_COMMENT_INCOMPLETE
                  && n != ZigTokens.LINE_COMMENT) {
                next = n;
                break;
              }
              j++;
            }
            if (next == ZigTokens.LPAREN) {
              color = GhostColorScheme.FUNCTION_NAME;
            } else if (next == ZigTokens.ASSIGN || next == ZigTokens.EQ) {
              color = GhostColorScheme.IDENTIFIER_VAR;
            } else if (next == ZigTokens.DOT) {
              color = GhostColorScheme.COLORNEXTDOT;
            } else if (next == ZigTokens.COLON) {
              color = GhostColorScheme.IDENTIFIER_NAME;
            }
          }
          span = SpanFactory.obtain(offset, TextStyle.makeStyle(color));
          break;
        case PLUS:
        case MINUS:
        case STAR:
        case SLASH:
        case PERCENT:
        case AMPERSAND:
        case PIPE:
        case CARET:
        case TILDE:
        case LT:
        case GT:
        case ASSIGN:
        case PLUS_ASSIGN:
        case MINUS_ASSIGN:
        case STAR_ASSIGN:
        case SLASH_ASSIGN:
        case PERCENT_ASSIGN:
        case AND_ASSIGN:
        case OR_ASSIGN:
        case XOR_ASSIGN:
        case EQ:
        case NOT_EQ:
        case LT_EQ:
        case GT_EQ:
        case INC:
        case DEC:
        case LOGICAL_AND:
        case LOGICAL_OR:
        case SHIFT_LEFT:
        case SHIFT_RIGHT:
        case ARROW:
        case FAT_ARROW:
        case DOUBLE_DOT:
        case ELLIPSIS:
        case SEMICOLON:
        case NOT:
        case COLON:
        case COMMA:
        case DOT:
        case AT:
          span = SpanFactory.obtain(offset, GhostColorScheme.OPERATOR);
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
          span = SpanFactory.obtain(offset, GhostColorScheme.TEXT_NORMAL);
      }
      if (tokenRecord.url != null) {
        span = SpanFactory.obtain(span.getColumn(), span.getStyle());
        span.setSpanExt(SpanExtAttrs.EXT_INTERACTION_INFO, new SpanClickableUrl(tokenRecord.url));
        span.setUnderlineColor(new EditorColor(span.getForegroundColorId()));
      }
      spans.add(span);
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
    }
    return spans;
  }

  public static class HighlightToken {

    public ZigTokens token;

    public int offset;

    public String url;

    public String tokenText;

    public HighlightToken(ZigTokens token, int offset) {
      this.token = token;
      this.offset = offset;
    }

    public HighlightToken(ZigTokens token, int offset, String url) {
      this.token = token;
      this.offset = offset;
      this.url = url;
    }
  }
}
