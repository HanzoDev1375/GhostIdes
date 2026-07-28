/**
 * Comment by ghost ide
 *
 * @author: Ninjacoder
 */
package ir.hanzodev1375.ghostide.codeeditors.langs.dart;

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

public class DartIncrementalAnalyzeManager
    extends AsyncIncrementalAnalyzeManager<
        DartState, DartIncrementalAnalyzeManager.HighlightToken> {

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

  private final ThreadLocal<DartTextTokenizer> tokenizerProvider = new ThreadLocal<>();

  protected IdentifierAutoComplete.SyncIdentifiers identifiers =
      new IdentifierAutoComplete.SyncIdentifiers();

  private synchronized DartTextTokenizer obtainTokenizer() {
    var res = tokenizerProvider.get();
    if (res == null) {
      res = new DartTextTokenizer("");
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
        for (var tokenRecord : state.tokens) {
          var token = tokenRecord.token;
          int offset = tokenRecord.offset;
          if (token == DartTokens.LBRACE) {
            if (stack.isEmpty()) {
              if (currSwitch > maxSwitch) maxSwitch = currSwitch;
              currSwitch = 0;
            }
            currSwitch++;
            CodeBlock block = new CodeBlock();
            block.startLine = i;
            block.startColumn = offset;
            stack.push(block);
          } else if (token == DartTokens.RBRACE) {
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

  private static int getType(DartTokens token) {
    if (token == DartTokens.LBRACE || token == DartTokens.RBRACE) return 3;
    if (token == DartTokens.LBRACK || token == DartTokens.RBRACK) return 2;
    if (token == DartTokens.LPAREN || token == DartTokens.RPAREN) return 1;
    return 0;
  }

  private static boolean isStart(DartTokens token) {
    return token == DartTokens.LBRACE || token == DartTokens.LBRACK || token == DartTokens.LPAREN;
  }

  @NonNull
  @Override
  public DartState getInitialState() {
    return new DartState();
  }

  @Override
  public boolean stateEquals(@NonNull DartState state, @NonNull DartState another) {
    return state.equals(another);
  }

  @Override
  public void onAddState(DartState state) {
    if (state.identifiers != null) {
      for (String identifier : state.identifiers) {
        identifiers.identifierIncrease(identifier);
      }
    }
  }

  @Override
  public void onAbandonState(DartState state) {
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
  public LineTokenizeResult<DartState, HighlightToken> tokenizeLine(
      CharSequence line, DartState state, int lineIndex) {
    var tokens = new ArrayList<HighlightToken>();
    int newState = STATE_NORMAL;
    var stateObj = new DartState();
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
      tokens.add(new HighlightToken(DartTokens.UNKNOWN, 0));
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
            line.subSequence(0, offset), 0, DartTokens.BLOCK_COMMENT_COMPLETE, tokens);
      } else {
        tokens.add(new HighlightToken(DartTokens.BLOCK_COMMENT_COMPLETE, 0));
      }
      return IntPair.pack(STATE_NORMAL, offset);
    }
    if (offset < 1000) {
      detectHighlightUrls(
          line.subSequence(0, offset), 0, DartTokens.BLOCK_COMMENT_INCOMPLETE, tokens);
    } else {
      tokens.add(new HighlightToken(DartTokens.BLOCK_COMMENT_INCOMPLETE, 0));
    }
    return IntPair.pack(STATE_INCOMPLETE_BLOCK_COMMENT, offset);
  }

  private int tokenizeNormal(
      CharSequence text, int offset, List<HighlightToken> tokens, DartState st) {
    var tokenizer = obtainTokenizer();
    tokenizer.reset(text);
    tokenizer.offset = offset;
    DartTokens token;
    int state = STATE_NORMAL;
    while ((token = tokenizer.nextToken()) != DartTokens.EOF) {
      if (tokenizer.getTokenLength() < 1000
          && (token == DartTokens.STRING_LITERAL
              || token == DartTokens.BLOCK_COMMENT_COMPLETE
              || token == DartTokens.BLOCK_COMMENT_INCOMPLETE
              || token == DartTokens.LINE_COMMENT
              || token == DartTokens.LINE_COMMENT_DOC)) {
        detectHighlightUrls(tokenizer.getTokenText(), tokenizer.offset, token, tokens);
        if (token == DartTokens.BLOCK_COMMENT_INCOMPLETE) {
          state = STATE_INCOMPLETE_BLOCK_COMMENT;
          break;
        }
        continue;
      }
      tokens.add(new HighlightToken(token, tokenizer.offset));
      if (token == DartTokens.LBRACE || token == DartTokens.RBRACE) {
        st.hasBraces = true;
      }
      if (token == DartTokens.LPAREN
          || token == DartTokens.LBRACE
          || token == DartTokens.LBRACK) {
        st.bracketDepth++;
      } else if (token == DartTokens.RPAREN
          || token == DartTokens.RBRACE
          || token == DartTokens.RBRACK) {
        st.bracketDepth = Math.max(0, st.bracketDepth - 1);
      }
      if (token == DartTokens.IDENTIFIER) {
        st.addIdentifier(tokenizer.getTokenText());
      }
      if (token == DartTokens.BLOCK_COMMENT_INCOMPLETE) {
        state = STATE_INCOMPLETE_BLOCK_COMMENT;
        break;
      }
    }
    return state;
  }

  private void detectHighlightUrls(
      CharSequence tokenText, int offset, DartTokens token, List<HighlightToken> tokens) {
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
  public List<Span> generateSpansForLine(LineTokenizeResult<DartState, HighlightToken> lineResult) {
    var spans = new ArrayList<Span>();
    var tokens = lineResult.tokens;
    DartTokens previous = DartTokens.UNKNOWN;
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
        case LINE_COMMENT_DOC:
        case BLOCK_COMMENT_COMPLETE:
        case BLOCK_COMMENT_INCOMPLETE:
          span =
              SpanFactory.obtain(
                  offset,
                  TextStyle.makeStyle(GhostColorScheme.COMMENT, 0, false, true, false, true));
          break;

        case ABSTRACT:
        case AS:
        case ASSERT:
        case ASYNC:
        case AWAIT:
        case BREAK:
        case CASE:
        case CATCH:
        case CLASS:
        case CONST:
        case CONTINUE:
        case COVARIANT:
        case DEFAULT:
        case DEFERRED:
        case DO:
        case DYNAMIC:
        case ELSE:
        case ENUM:
        case EXPORT:
        case EXTENDS:
        case EXTENSION:
        case EXTERNAL:
        case FACTORY:
        case FINAL:
        case FINALLY:
        case FOR:
        case FUNCTION:
        case GET:
        case HIDE:
        case IF:
        case IMPLEMENTS:
        case IMPORT:
        case IN:
        case INTERFACE:
        case IS:
        case LIBRARY:
        case MIXIN:
        case NEW:
        case ON:
        case OPERATOR:
        case PART:
        case REQUIRED:
        case RETHROW:
        case RETURN:
        case SET:
        case SHOW:
        case STATIC:
        case SUPER:
        case SWITCH:
        case SYNC:
        case THIS:
        case THROW:
        case TRY:
        case TYPEDEF:
        case VAR:
        case VOID:
        case WHILE:
        case WITH:
        case YIELD:
        case NULL:
          span =
              SpanFactory.obtain(
                  offset, TextStyle.makeStyle(GhostColorScheme.KEYWORD, 0, true, false, false));
          break;
        case BOOLEAN_LITERAL:
        case NULL_LITERAL:
        case INTEGER_LITERAL:
        case DOUBLE_LITERAL:
        case STRING_LITERAL:
          span = SpanFactory.obtain(offset, TextStyle.makeStyle(GhostColorScheme.LITERAL, true));
          break;
        case IDENTIFIER:
          int color = GhostColorScheme.TEXT_NORMAL;
          if (previous == DartTokens.CLASS
              || previous == DartTokens.EXTENDS
              || previous == DartTokens.IMPLEMENTS
              || previous == DartTokens.WITH
              || previous == DartTokens.NEW) {

            color = GhostColorScheme.COLORNEXTCHAR;
          } else if (previous == DartTokens.FUNCTION
              || previous == DartTokens.GET
              || previous == DartTokens.SET
              || previous == DartTokens.OPERATOR) {
            color = GhostColorScheme.FUNCTION_NAME;
          } else if (previous == DartTokens.DOT) {
            color = GhostColorScheme.ATTRIBUTE_NAME;
          } else if (previous == DartTokens.AT) {
            color = GhostColorScheme.COLORNEXTBRAK;
          } else if (previous == DartTokens.VAR || previous == DartTokens.IN) {
            color = GhostColorScheme.COLORNEXTCHAR;
          } else if (previous == DartTokens.LPAREN) {
            color = GhostColorScheme.FUNCTION_NAME;
          } else {
            int j = i + 1;
            var next = DartTokens.UNKNOWN;
            while (j < tokens.size()) {
              var n = tokens.get(j).token;
              if (n != DartTokens.WHITESPACE
                  && n != DartTokens.NEWLINE
                  && n != DartTokens.BLOCK_COMMENT_COMPLETE
                  && n != DartTokens.BLOCK_COMMENT_INCOMPLETE
                  && n != DartTokens.LINE_COMMENT
                  && n != DartTokens.LINE_COMMENT_DOC) {
                next = n;
                break;
              }
              j++;
            }
            if (next == DartTokens.LPAREN) {
              color = GhostColorScheme.FUNCTION_NAME;
            } else if (next == DartTokens.LT || next == DartTokens.GT) {
              color = GhostColorScheme.FUNCTION_NAME;
            } else if (next == DartTokens.QUESTION || next == DartTokens.EXCLAMATION || previous == DartTokens.QUESTION_DOT) {
              color = GhostColorScheme.ATTRIBUTE_NAME;
            }
          }
          span = SpanFactory.obtain(offset, TextStyle.makeStyle(color));
          break;

        case PLUS:
        case MINUS:
        case STAR:
        case SLASH:
        case PERCENT:
        case CARET:
        case AMPERSAND:
        case PIPE:
        case TILDE:
        case LT:
        case GT:
        case ASSIGN:
        case PLUS_ASSIGN:
        case MINUS_ASSIGN:
        case STAR_ASSIGN:
        case SLASH_ASSIGN:
        case PERCENT_ASSIGN:
        case CARET_ASSIGN:
        case AMPERSAND_ASSIGN:
        case PIPE_ASSIGN:
        case EQ:
        case NOT_EQ:
        case LT_EQ:
        case GT_EQ:
        case INC:
        case DEC:
        case LOGICAL_AND:
        case LOGICAL_OR:
        case NULL_AWARE:
        case NULL_AWARE_ASSIGN:
        case QUESTION:
        case EXCLAMATION:
        case QUESTION_DOT:
        case EXCLAMATION_DOT:
        case ARROW:
        case SPREAD:
        case CASCADE:
        case AT:
        case SEMICOLON:
        case COLON:
        case COMMA:
        case DOT:
        case ELLIPSIS:
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
        case LINE_COMMENT_DOC:
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

    public DartTokens token;
    public int offset;
    public String url;

    public HighlightToken(DartTokens token, int offset) {
      this.token = token;
      this.offset = offset;
    }

    public HighlightToken(DartTokens token, int offset, String url) {
      this.token = token;
      this.offset = offset;
      this.url = url;
    }
  }
}
