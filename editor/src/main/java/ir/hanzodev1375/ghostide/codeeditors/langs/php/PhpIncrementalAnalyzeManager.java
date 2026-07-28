/**
 * Comment by ghost ide
 *
 * @author: Ninjacoder
 */
package ir.hanzodev1375.ghostide.codeeditors.langs.php;

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

public class PhpIncrementalAnalyzeManager
    extends AsyncIncrementalAnalyzeManager<PhpState, PhpIncrementalAnalyzeManager.HighlightToken> {

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

  private final ThreadLocal<PhpTextTokenizer> tokenizerProvider = new ThreadLocal<>();

  protected IdentifierAutoComplete.SyncIdentifiers identifiers =
      new IdentifierAutoComplete.SyncIdentifiers();

  private synchronized PhpTextTokenizer obtainTokenizer() {
    var res = tokenizerProvider.get();
    if (res == null) {
      res = new PhpTextTokenizer("");
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
          if (token == PhpTokens.LBRACE) {
            if (stack.isEmpty()) {
              if (currSwitch > maxSwitch) maxSwitch = currSwitch;
              currSwitch = 0;
            }
            currSwitch++;
            CodeBlock block = new CodeBlock();
            block.startLine = i;
            block.startColumn = offset;
            stack.push(block);
          } else if (token == PhpTokens.RBRACE) {
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

  private static int getType(PhpTokens token) {
    if (token == PhpTokens.LBRACE || token == PhpTokens.RBRACE) return 3;
    if (token == PhpTokens.LBRACK || token == PhpTokens.RBRACK) return 2;
    if (token == PhpTokens.LPAREN || token == PhpTokens.RPAREN) return 1;
    return 0;
  }

  private static boolean isStart(PhpTokens token) {
    return token == PhpTokens.LBRACE || token == PhpTokens.LBRACK || token == PhpTokens.LPAREN;
  }

  @NonNull
  @Override
  public PhpState getInitialState() {
    return new PhpState();
  }

  @Override
  public boolean stateEquals(@NonNull PhpState state, @NonNull PhpState another) {
    return state.equals(another);
  }

  @Override
  public void onAddState(PhpState state) {
    if (state.identifiers != null) {
      for (String identifier : state.identifiers) {
        identifiers.identifierIncrease(identifier);
      }
    }
  }

  @Override
  public void onAbandonState(PhpState state) {
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
  public LineTokenizeResult<PhpState, HighlightToken> tokenizeLine(
      CharSequence line, PhpState state, int lineIndex) {
    var tokens = new ArrayList<HighlightToken>();
    int newState = STATE_NORMAL;
    var stateObj = new PhpState();
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
      tokens.add(new HighlightToken(PhpTokens.UNKNOWN, 0));
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
            line.subSequence(0, offset), 0, PhpTokens.BLOCK_COMMENT_COMPLETE, tokens);
      } else {
        tokens.add(new HighlightToken(PhpTokens.BLOCK_COMMENT_COMPLETE, 0));
      }
      return IntPair.pack(STATE_NORMAL, offset);
    }
    if (offset < 1000) {
      detectHighlightUrls(
          line.subSequence(0, offset), 0, PhpTokens.BLOCK_COMMENT_INCOMPLETE, tokens);
    } else {
      tokens.add(new HighlightToken(PhpTokens.BLOCK_COMMENT_INCOMPLETE, 0));
    }
    return IntPair.pack(STATE_INCOMPLETE_BLOCK_COMMENT, offset);
  }

  private int tokenizeNormal(
      CharSequence text, int offset, List<HighlightToken> tokens, PhpState st) {
    var tokenizer = obtainTokenizer();
    tokenizer.reset(text);
    tokenizer.offset = offset;
    PhpTokens token;
    int state = STATE_NORMAL;
    while ((token = tokenizer.nextToken()) != PhpTokens.EOF) {
      if (tokenizer.getTokenLength() < 1000
          && (token == PhpTokens.STRING_LITERAL
              || token == PhpTokens.CHAR_LITERAL
              || token == PhpTokens.BLOCK_COMMENT_COMPLETE
              || token == PhpTokens.BLOCK_COMMENT_INCOMPLETE
              || token == PhpTokens.LINE_COMMENT
              || token == PhpTokens.LINE_COMMENT_HASH)) {
        detectHighlightUrls(tokenizer.getTokenText(), tokenizer.offset, token, tokens);
        if (token == PhpTokens.BLOCK_COMMENT_INCOMPLETE) {
          state = STATE_INCOMPLETE_BLOCK_COMMENT;
          break;
        }
        continue;
      }
      tokens.add(new HighlightToken(token, tokenizer.offset));
      if (token == PhpTokens.LBRACE || token == PhpTokens.RBRACE) {
        st.hasBraces = true;
      }
      if (token == PhpTokens.LPAREN
          || token == PhpTokens.LBRACE
          || token == PhpTokens.LBRACK
          || token == PhpTokens.STRING_TEMPLATE_EXPR_START) {
        st.bracketDepth++;
      } else if (token == PhpTokens.RPAREN
          || token == PhpTokens.RBRACE
          || token == PhpTokens.RBRACK
          || token == PhpTokens.STRING_TEMPLATE_EXPR_END) {
        st.bracketDepth = Math.max(0, st.bracketDepth - 1);
      }
      if (token == PhpTokens.IDENTIFIER) {
        st.addIdentifier(tokenizer.getTokenText());
      }
      if (token == PhpTokens.BLOCK_COMMENT_INCOMPLETE) {
        state = STATE_INCOMPLETE_BLOCK_COMMENT;
        break;
      }
    }
    return state;
  }

  private void detectHighlightUrls(
      CharSequence tokenText, int offset, PhpTokens token, List<HighlightToken> tokens) {
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
  public List<Span> generateSpansForLine(LineTokenizeResult<PhpState, HighlightToken> lineResult) {
    var spans = new ArrayList<Span>();
    var tokens = lineResult.tokens;
    PhpTokens previous = PhpTokens.UNKNOWN;
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

        case PHP_OPEN_TAG:
        case PHP_CLOSE_TAG:
        case SHORT_ECHO:
          span =
              SpanFactory.obtain(
                  offset, TextStyle.makeStyle(GhostColorScheme.KEYWORD, 0, true, false, false));
          break;

        case LINE_COMMENT:
        case LINE_COMMENT_HASH:
        case BLOCK_COMMENT_COMPLETE:
        case BLOCK_COMMENT_INCOMPLETE:
          span =
              SpanFactory.obtain(
                  offset,
                  TextStyle.makeStyle(GhostColorScheme.COMMENT, 0, false, true, false, true));
          break;

        case ABSTRACT:
        case AND_KW:
        case ARRAY:
        case AS:
        case BREAK:
        case CALLABLE:
        case CASE:
        case CATCH:
        case CLASS:
        case CLONE:
        case CONST:
        case CONTINUE:
        case DECLARE:
        case DEFAULT:
        case DIE:
        case DO:
        case ECHO:
        case ELSE:
        case ELSEIF:
        case EMPTY:
        case ENDDECLARE:
        case ENDFOR:
        case ENDFOREACH:
        case ENDIF:
        case ENDSWITCH:
        case ENDWHILE:
        case EVAL:
        case EXIT:
        case EXTENDS:
        case FINAL:
        case FINALLY:
        case FN:
        case FOR:
        case FOREACH:
        case FUNCTION:
        case GLOBAL:
        case GOTO:
        case IF:
        case IMPLEMENTS:
        case INCLUDE:
        case INCLUDE_ONCE:
        case INSTANCEOF:
        case INSTEADOF:
        case INTERFACE:
        case ISSET:
        case LIST:
        case MATCH:
        case NAMESPACE:
        case NEW:
        case OR_KW:
        case PRINT:
        case PRIVATE:
        case PROTECTED:
        case PUBLIC:
        case READONLY:
        case REQUIRE:
        case REQUIRE_ONCE:
        case RETURN:
        case STATIC:
        case SWITCH:
        case THROW:
        case TRAIT:
        case TRY:
        case UNSET:
        case USE:
        case VAR:
        case WHILE:
        case XOR_KW:
        case YIELD:
        case TRUE:
        case FALSE:
        case NULL:
        case PARENT:
        case SELF:
          span =
              SpanFactory.obtain(
                  offset, TextStyle.makeStyle(GhostColorScheme.KEYWORD, 0, true, false, false));
          break;

        case INTEGER_LITERAL:
        case FLOATING_LITERAL:
          span = SpanFactory.obtain(offset, TextStyle.makeStyle(GhostColorScheme.LITERAL, true));
          break;

        case STRING_LITERAL:
        case CHAR_LITERAL:
          span = SpanFactory.obtain(offset, TextStyle.makeStyle(GhostColorScheme.LITERAL, true));
          break;

        case DOLLAR:
          span =
              SpanFactory.obtain(
                  offset,
                  TextStyle.makeStyle(GhostColorScheme.ATTRIBUTE_NAME, 0, true, false, false));
          break;

        case IDENTIFIER:
          int color = GhostColorScheme.TEXT_NORMAL;
          if (previous == PhpTokens.DOLLAR) {
            color = GhostColorScheme.IDENTIFIER_VAR;
          } else if (previous == PhpTokens.FUNCTION
              || previous == PhpTokens.PROTECTED
              || previous == PhpTokens.PRIVATE
              || previous == PhpTokens.PUBLIC) {
            color = GhostColorScheme.FUNCTION_NAME;
          } else if (previous == PhpTokens.CLASS || previous == PhpTokens.ABSTRACT) {
            color = GhostColorScheme.COLORNEXTCHAR;
          } else if (previous == PhpTokens.DOT) {
            color = GhostColorScheme.COLORNEXTDOT;
          } else if (previous == PhpTokens.NEW) {
            color = GhostColorScheme.COLORNEXTCHAR;
          } else if (previous == PhpTokens.BACKSLASH) {
            color = GhostColorScheme.COLORNEXTBRAK;
          } else if (previous == PhpTokens.COLON) {
            color = GhostColorScheme.COLORUPPERCASE;
          } else if (previous == PhpTokens.OBJECT_OPERATOR) {
            color = GhostColorScheme.HTML_TAG;
          } else {
            int j = i + 1;
            var next = PhpTokens.UNKNOWN;
            while (j < tokens.size()) {
              var n = tokens.get(j).token;
              if (n != PhpTokens.WHITESPACE
                  && n != PhpTokens.NEWLINE
                  && n != PhpTokens.BLOCK_COMMENT_COMPLETE
                  && n != PhpTokens.BLOCK_COMMENT_INCOMPLETE
                  && n != PhpTokens.LINE_COMMENT
                  && n != PhpTokens.LINE_COMMENT_HASH) {
                next = n;
                break;
              }
              j++;
            }
            if (next == PhpTokens.LPAREN) {
              color = GhostColorScheme.FUNCTION_NAME;
            } else if (next == PhpTokens.ASSIGN) {
              color = GhostColorScheme.IDENTIFIER_VAR;
            } else if (next == PhpTokens.BACKSLASH) {
              color = GhostColorScheme.COLORNEXTBRAK;
            } else if (next == PhpTokens.DOLLAR) {
              color = GhostColorScheme.COLORNEXTLESS;
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
        case EQ:
        case LT:
        case GT:
        case NOT:
        case AND:
        case OR:
        case XOR:
        case ASSIGN:
        case PLUS_ASSIGN:
        case MINUS_ASSIGN:
        case STAR_ASSIGN:
        case SLASH_ASSIGN:
        case PERCENT_ASSIGN:
        case XOR_ASSIGN:
        case AND_ASSIGN:
        case OR_ASSIGN:
        case INC:
        case DEC:
        case CONCAT:
        case CONCAT_ASSIGN:
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
        case STRING_TEMPLATE_EXPR_START:
          span = SpanFactory.obtain(offset, bracketStyle(depth));
          depth++;
          break;
        case RPAREN:
        case RBRACE:
        case RBRACK:
        case STRING_TEMPLATE_EXPR_END:
          depth = Math.max(0, depth - 1);
          span = SpanFactory.obtain(offset, bracketStyle(depth));
          break;
        case OBJECT_OPERATOR:
          span = SpanFactory.obtain(offset, GhostColorScheme.OPERATOR);
          break;
        default:
          span = SpanFactory.obtain(offset, GhostColorScheme.OPERATOR);
      }

      switch (token) {
        case WHITESPACE:
        case NEWLINE:
        case LINE_COMMENT:
        case LINE_COMMENT_HASH:
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

    public PhpTokens token;

    public int offset;

    public String url;

    public HighlightToken(PhpTokens token, int offset) {
      this.token = token;
      this.offset = offset;
    }

    public HighlightToken(PhpTokens token, int offset, String url) {
      this.token = token;
      this.offset = offset;
      this.url = url;
    }
  }
}
