/**
 * Comment by ghost ide
 *
 * @author: Ninjacoder
 */
package ir.hanzodev1375.ghostide.codeeditors.langs.tsx;

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

public class TsxIncrementalAnalyzeManager
    extends AsyncIncrementalAnalyzeManager<TsxState, TsxIncrementalAnalyzeManager.HighlightToken> {

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

  private final ThreadLocal<TsxTextTokenizer> tokenizerProvider = new ThreadLocal<>();

  protected IdentifierAutoComplete.SyncIdentifiers identifiers =
      new IdentifierAutoComplete.SyncIdentifiers();

  private synchronized TsxTextTokenizer obtainTokenizer() {
    var res = tokenizerProvider.get();
    if (res == null) {
      res = new TsxTextTokenizer("");
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
          if (token == TsxTokens.LBRACE || token == TsxTokens.JSX_TAG_OPEN) {
            CodeBlock block = new CodeBlock();
            block.startLine = i;
            block.startColumn = offset;
            stack.push(block);
          } else if (token == TsxTokens.RBRACE || token == TsxTokens.JSX_TAG_OPEN_SLASH) {
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

  private static int getType(TsxTokens token) {
    if (token == TsxTokens.LBRACE || token == TsxTokens.RBRACE) return 3;
    if (token == TsxTokens.LBRACK || token == TsxTokens.RBRACK) return 2;
    if (token == TsxTokens.LPAREN || token == TsxTokens.RPAREN) return 1;
    return 0;
  }

  private static boolean isStart(TsxTokens token) {
    return token == TsxTokens.LBRACE || token == TsxTokens.LBRACK || token == TsxTokens.LPAREN;
  }

  @NonNull
  @Override
  public TsxState getInitialState() {
    return new TsxState();
  }

  @Override
  public boolean stateEquals(@NonNull TsxState state, @NonNull TsxState another) {
    return state.equals(another);
  }

  @Override
  public void onAddState(TsxState state) {
    if (state.identifiers != null) {
      for (String identifier : state.identifiers) {
        identifiers.identifierIncrease(identifier);
      }
    }
  }

  @Override
  public void onAbandonState(TsxState state) {
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
  public LineTokenizeResult<TsxState, HighlightToken> tokenizeLine(
      CharSequence line, TsxState state, int lineIndex) {
    var tokens = new ArrayList<HighlightToken>();
    int newState = STATE_NORMAL;
    var stateObj = new TsxState();
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
      tokens.add(new HighlightToken(TsxTokens.UNKNOWN, 0));
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
            line.subSequence(0, offset), 0, TsxTokens.BLOCK_COMMENT_COMPLETE, tokens);
      } else {
        tokens.add(new HighlightToken(TsxTokens.BLOCK_COMMENT_COMPLETE, 0));
      }
      return IntPair.pack(STATE_NORMAL, offset);
    }
    if (offset < 1000) {
      detectHighlightUrls(
          line.subSequence(0, offset), 0, TsxTokens.BLOCK_COMMENT_INCOMPLETE, tokens);
    } else {
      tokens.add(new HighlightToken(TsxTokens.BLOCK_COMMENT_INCOMPLETE, 0));
    }
    return IntPair.pack(STATE_INCOMPLETE_BLOCK_COMMENT, offset);
  }

  private int tokenizeNormal(
      CharSequence text, int offset, List<HighlightToken> tokens, TsxState st) {
    var tokenizer = obtainTokenizer();
    tokenizer.reset(text);
    tokenizer.offset = offset;
    TsxTokens token;
    int state = STATE_NORMAL;
    while ((token = tokenizer.nextToken()) != TsxTokens.EOF) {
      if (tokenizer.getTokenLength() < 1000
          && (token == TsxTokens.STRING_LITERAL
              || token == TsxTokens.BLOCK_COMMENT_COMPLETE
              || token == TsxTokens.BLOCK_COMMENT_INCOMPLETE
              || token == TsxTokens.LINE_COMMENT)) {
        detectHighlightUrls(tokenizer.getTokenText(), tokenizer.offset, token, tokens);
        if (token == TsxTokens.BLOCK_COMMENT_INCOMPLETE) {
          state = STATE_INCOMPLETE_BLOCK_COMMENT;
          break;
        }
        continue;
      }
      tokens.add(new HighlightToken(token, tokenizer.offset));
      if (token == TsxTokens.LBRACE || token == TsxTokens.RBRACE) {
        st.hasBraces = true;
      }
      if (token == TsxTokens.LPAREN || token == TsxTokens.LBRACE || token == TsxTokens.LBRACK) {
        st.bracketDepth++;
      } else if (token == TsxTokens.RPAREN
          || token == TsxTokens.RBRACE
          || token == TsxTokens.RBRACK) {
        st.bracketDepth = Math.max(0, st.bracketDepth - 1);
      }
      if (token == TsxTokens.IDENTIFIER || token == TsxTokens.JSX_IDENTIFIER) {
        st.addIdentifier(tokenizer.getTokenText());
      }
      if (token == TsxTokens.BLOCK_COMMENT_INCOMPLETE) {
        state = STATE_INCOMPLETE_BLOCK_COMMENT;
        break;
      }
    }
    return state;
  }

  private void detectHighlightUrls(
      CharSequence tokenText, int offset, TsxTokens token, List<HighlightToken> tokens) {
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
  public List<Span> generateSpansForLine(LineTokenizeResult<TsxState, HighlightToken> lineResult) {
    var spans = new ArrayList<Span>();
    var tokens = lineResult.tokens;
    TsxTokens previous = TsxTokens.UNKNOWN;
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

        case ABSTRACT:
        case ANY:
        case BOOLEAN:
        case BREAK:
        case CASE:
        case CATCH:
        case CLASS:
        case CONST:
        case CONTINUE:
        case DEBUGGER:
        case DECLARE:
        case DEFAULT:
        case DELETE:
        case DO:
        case ELSE:
        case ENUM:
        case EXPORT:
        case EXTENDS:
        case FINALLY:
        case FOR:
        case FROM:
        case FUNCTION:
        case GET:
        case IF:
        case IMPLEMENTS:
        case IMPORT:
        case IN:
        case INFER:
        case INSTANCEOF:
        case INTERFACE:
        case IS:
        case KEYOF:
        case LET:
        case MODULE:
        case NAMESPACE:
        case NEVER:
        case NEW:
        case NULL:
        case NUMBER:
        case OBJECT:
        case PACKAGE:
        case PRIVATE:
        case PROTECTED:
        case PUBLIC:
        case READONLY:
        case REQUIRE:
        case RETURN:
        case SET:
        case STATIC:
        case STRING:
        case SUPER:
        case SWITCH:
        case SYMBOL:
        case THIS:
        case THROW:
        case TRY:
        case TYPE:
        case TYPEOF:
        case UNDEFINED:
        case UNKNOWN:
        case VAR:
        case VOID:
        case WHILE:
        case WITH:
        case YIELD:
        case AS:
        case ASSERTS:
          span =
              SpanFactory.obtain(
                  offset, TextStyle.makeStyle(GhostColorScheme.KEYWORD, 0, true, false, false));
          break;

        case JSX_TAG_OPEN:
        case JSX_TAG_CLOSE:
        case JSX_TAG_SELF_CLOSE:
        case JSX_TAG_OPEN_SLASH:
        case LT:
        case GT:
          span =
              SpanFactory.obtain(
                  offset, TextStyle.makeStyle(GhostColorScheme.HTML_TAG, 0, true, false, false));
          break;
        case JSX_IDENTIFIER:
          String text = tokenRecord.tokenText != null ? tokenRecord.tokenText : "";
          int jsxColor = GhostColorScheme.TEXT_NORMAL;
          if (!text.isEmpty() && Character.isUpperCase(text.charAt(0))) {
            jsxColor = GhostColorScheme.IDENTIFIER_VAR;
          } else {
            jsxColor = GhostColorScheme.IDENTIFIER_NAME;
          }
          span = SpanFactory.obtain(offset, TextStyle.makeStyle(jsxColor, 0, true, false, false));
          break;

        case INTEGER_LITERAL:
        case FLOATING_LITERAL:
        case CHARACTER_LITERAL:
        case STRING_LITERAL:
        case BOOLEAN_LITERAL:
        case NULL_LITERAL:
        case BACKTIK_LIERAL:
          span = SpanFactory.obtain(offset, TextStyle.makeStyle(GhostColorScheme.LITERAL, true));
          break;

        case IDENTIFIER:
          int color = GhostColorScheme.TEXT_NORMAL;
          if (previous == TsxTokens.CLASS
              || previous == TsxTokens.INTERFACE
              || previous == TsxTokens.EXTENDS
              || previous == TsxTokens.IMPLEMENTS
              || previous == TsxTokens.NEW) {
            color = GhostColorScheme.COLORNEXTCHAR;
          } else if (previous == TsxTokens.FUNCTION
              || previous == TsxTokens.GET
              || previous == TsxTokens.SET
              || previous == TsxTokens.PUBLIC
              || previous == TsxTokens.PRIVATE
              || previous == TsxTokens.PROTECTED) {
            color = GhostColorScheme.FUNCTION_NAME;
          } else if (previous == TsxTokens.DEFAULT) {
            color = GhostColorScheme.ATTRIBUTE_NAME;
          } else if (previous == TsxTokens.NULL_COALESCING_ASSIGN
              || previous == TsxTokens.NULL_COALESCING
              || previous == TsxTokens.OPTIONAL_CHAINING) {
            color = GhostColorScheme.IDENTIFIER_NAME;
          } else {
            int j = i + 1;
            TsxTokens next = TsxTokens.UNKNOWN;
            while (j < tokens.size()) {
              TsxTokens n = tokens.get(j).token;
              if (n != TsxTokens.WHITESPACE
                  && n != TsxTokens.NEWLINE
                  && n != TsxTokens.BLOCK_COMMENT_COMPLETE
                  && n != TsxTokens.BLOCK_COMMENT_INCOMPLETE
                  && n != TsxTokens.LINE_COMMENT) {
                next = n;
                break;
              }
              j++;
            }
            if (next == TsxTokens.LPAREN) {
              color = GhostColorScheme.FUNCTION_NAME;
            } else if (next == TsxTokens.DOT) {
              color = GhostColorScheme.COLORNEXTDOT;
            } else if (next == TsxTokens.ASSIGN || next == TsxTokens.EQ) {
              color = GhostColorScheme.IDENTIFIER_VAR;
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
        case UNSIGNED_SHIFT_RIGHT:
        case QUESTION:
        case COLON:
        case ARROW:
        case DOUBLE_COLON:
        case ELLIPSIS:
        case SEMICOLON:
        case COMMA:
        case DOT:
        case NON_NULL_ASSERT:
        case OPTIONAL_CHAINING:
        case NULL_COALESCING:
        case NULL_COALESCING_ASSIGN:
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

    public TsxTokens token;

    public int offset;

    public String url;

    public String tokenText;

    public HighlightToken(TsxTokens token, int offset) {
      this.token = token;
      this.offset = offset;
    }

    public HighlightToken(TsxTokens token, int offset, String url) {
      this.token = token;
      this.offset = offset;
      this.url = url;
    }
  }
}
