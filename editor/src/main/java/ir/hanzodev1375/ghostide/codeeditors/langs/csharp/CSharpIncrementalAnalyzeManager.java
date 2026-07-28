/**
 * Comment by ghost ide
 *
 * @author: Ninjacoder
 */
package ir.hanzodev1375.ghostide.codeeditors.langs.csharp;

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

public class CSharpIncrementalAnalyzeManager
    extends AsyncIncrementalAnalyzeManager<
        CSharpState, CSharpIncrementalAnalyzeManager.HighlightToken> {

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

  private final ThreadLocal<CSharpTextTokenizer> tokenizerProvider = new ThreadLocal<>();

  protected IdentifierAutoComplete.SyncIdentifiers identifiers =
      new IdentifierAutoComplete.SyncIdentifiers();

  private synchronized CSharpTextTokenizer obtainTokenizer() {
    var res = tokenizerProvider.get();
    if (res == null) {
      res = new CSharpTextTokenizer("");
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
          if (token == CSharpTokens.LBRACE) {
            if (stack.isEmpty()) {
              if (currSwitch > maxSwitch) maxSwitch = currSwitch;
              currSwitch = 0;
            }
            currSwitch++;
            CodeBlock block = new CodeBlock();
            block.startLine = i;
            block.startColumn = offset;
            stack.push(block);
          } else if (token == CSharpTokens.RBRACE) {
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

  private static int getType(CSharpTokens token) {
    if (token == CSharpTokens.LBRACE || token == CSharpTokens.RBRACE) return 3;
    if (token == CSharpTokens.LBRACK || token == CSharpTokens.RBRACK) return 2;
    if (token == CSharpTokens.LPAREN || token == CSharpTokens.RPAREN) return 1;
    return 0;
  }

  private static boolean isStart(CSharpTokens token) {
    return token == CSharpTokens.LBRACE
        || token == CSharpTokens.LBRACK
        || token == CSharpTokens.LPAREN;
  }

  @NonNull
  @Override
  public CSharpState getInitialState() {
    return new CSharpState();
  }

  @Override
  public boolean stateEquals(@NonNull CSharpState state, @NonNull CSharpState another) {
    return state.equals(another);
  }

  @Override
  public void onAddState(CSharpState state) {
    if (state.identifiers != null) {
      for (String identifier : state.identifiers) {
        identifiers.identifierIncrease(identifier);
      }
    }
  }

  @Override
  public void onAbandonState(CSharpState state) {
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
  public LineTokenizeResult<CSharpState, HighlightToken> tokenizeLine(
      CharSequence line, CSharpState state, int lineIndex) {
    var tokens = new ArrayList<HighlightToken>();
    int newState = STATE_NORMAL;
    var stateObj = new CSharpState();
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
      tokens.add(new HighlightToken(CSharpTokens.UNKNOWN, 0));
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
            line.subSequence(0, offset), 0, CSharpTokens.BLOCK_COMMENT_COMPLETE, tokens);
      } else {
        tokens.add(new HighlightToken(CSharpTokens.BLOCK_COMMENT_COMPLETE, 0));
      }
      return IntPair.pack(STATE_NORMAL, offset);
    }
    if (offset < 1000) {
      detectHighlightUrls(
          line.subSequence(0, offset), 0, CSharpTokens.BLOCK_COMMENT_INCOMPLETE, tokens);
    } else {
      tokens.add(new HighlightToken(CSharpTokens.BLOCK_COMMENT_INCOMPLETE, 0));
    }
    return IntPair.pack(STATE_INCOMPLETE_BLOCK_COMMENT, offset);
  }

  private int tokenizeNormal(
      CharSequence text, int offset, List<HighlightToken> tokens, CSharpState st) {
    var tokenizer = obtainTokenizer();
    tokenizer.reset(text);
    tokenizer.offset = offset;
    CSharpTokens token;
    int state = STATE_NORMAL;
    while ((token = tokenizer.nextToken()) != CSharpTokens.EOF) {
      if (tokenizer.getTokenLength() < 1000
          && (token == CSharpTokens.STRING_LITERAL
              || token == CSharpTokens.CHAR_LITERAL
              || token == CSharpTokens.BLOCK_COMMENT_COMPLETE
              || token == CSharpTokens.BLOCK_COMMENT_INCOMPLETE
              || token == CSharpTokens.LINE_COMMENT
              || token == CSharpTokens.LINE_COMMENT_DOC)) {
        detectHighlightUrls(tokenizer.getTokenText(), tokenizer.offset, token, tokens);
        if (token == CSharpTokens.BLOCK_COMMENT_INCOMPLETE) {
          state = STATE_INCOMPLETE_BLOCK_COMMENT;
          break;
        }
        continue;
      }
      tokens.add(new HighlightToken(token, tokenizer.offset));
      if (token == CSharpTokens.LBRACE || token == CSharpTokens.RBRACE) {
        st.hasBraces = true;
      }
      if (token == CSharpTokens.LPAREN
          || token == CSharpTokens.LBRACE
          || token == CSharpTokens.LBRACK) {
        st.bracketDepth++;
      } else if (token == CSharpTokens.RPAREN
          || token == CSharpTokens.RBRACE
          || token == CSharpTokens.RBRACK) {
        st.bracketDepth = Math.max(0, st.bracketDepth - 1);
      }
      if (token == CSharpTokens.IDENTIFIER) {
        st.addIdentifier(tokenizer.getTokenText());
      }
      if (token == CSharpTokens.BLOCK_COMMENT_INCOMPLETE) {
        state = STATE_INCOMPLETE_BLOCK_COMMENT;
        break;
      }
    }
    return state;
  }

  private void detectHighlightUrls(
      CharSequence tokenText, int offset, CSharpTokens token, List<HighlightToken> tokens) {
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
      LineTokenizeResult<CSharpState, HighlightToken> lineResult) {
    var spans = new ArrayList<Span>();
    var tokens = lineResult.tokens;
    CSharpTokens previous = CSharpTokens.UNKNOWN;
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
        case BASE:
        case BOOL:
        case BREAK:
        case BYTE:
        case CASE:
        case CATCH:
        case CHAR:
        case CHECKED:
        case CLASS:
        case CONST:
        case CONTINUE:
        case DECIMAL:
        case DEFAULT:
        case DELEGATE:
        case DO:
        case DOUBLE:
        case ELSE:
        case ENUM:
        case EVENT:
        case EXPLICIT:
        case EXTERN:
        case FALSE:
        case FINALLY:
        case FIXED:
        case FLOAT:
        case FOR:
        case FOREACH:
        case GOTO:
        case IF:
        case IMPLICIT:
        case IN:
        case INT:
        case INTERFACE:
        case INTERNAL:
        case IS:
        case LOCK:
        case LONG:
        case NAMESPACE:
        case NEW:
        case NULL:
        case OBJECT:
        case OPERATOR:
        case OUT:
        case OVERRIDE:
        case PARAMS:
        case PRIVATE:
        case PROTECTED:
        case PUBLIC:
        case READONLY:
        case REF:
        case RETURN:
        case SBYTE:
        case SEALED:
        case SHORT:
        case SIZEOF:
        case STACKALLOC:
        case STATIC:
        case STRING:
        case STRUCT:
        case SWITCH:
        case THIS:
        case THROW:
        case TRUE:
        case TRY:
        case TYPEOF:
        case UINT:
        case ULONG:
        case UNCHECKED:
        case UNSAFE:
        case USHORT:
        case USING:
        case VIRTUAL:
        case VOID:
        case VOLATILE:
        case WHILE:
        case ADD:
        case ALIAS:
        case ASCENDING:
        case ASYNC:
        case AWAIT:
        case BY:
        case DESCENDING:
        case DYNAMIC:
        case EQUALS:
        case FROM:
        case GET:
        case GLOBAL:
        case GROUP:
        case INTO:
        case JOIN:
        case LET:
        case NAMEOF:
        case NOTNULL:
        case ON:
        case ORDERBY:
        case PARTIAL:
        case REMOVE:
        case SELECT:
        case SET:
        case UNMANAGED:
        case VALUE:
        case VAR:
        case WHEN:
        case WHERE:
        case WITH:
        case YIELD:
          span =
              SpanFactory.obtain(
                  offset, TextStyle.makeStyle(GhostColorScheme.KEYWORD, 0, true, false, false));
          break;
        case BOOLEAN_LITERAL:
        case NULL_LITERAL:
        case INTEGER_LITERAL:
        case REAL_LITERAL:
        case STRING_LITERAL:
        case CHAR_LITERAL:
          span = SpanFactory.obtain(offset, TextStyle.makeStyle(GhostColorScheme.LITERAL, true));
          break;
        case IDENTIFIER:
          int color = GhostColorScheme.TEXT_NORMAL;
          if (previous == CSharpTokens.CLASS
              || previous == CSharpTokens.STRUCT
              || previous == CSharpTokens.INTERFACE
              || previous == CSharpTokens.ENUM
              || previous == CSharpTokens.NAMESPACE
              || previous == CSharpTokens.NEW) {
            color = GhostColorScheme.COLORNEXTCHAR;
          } else if (previous == CSharpTokens.VOID
              || previous == CSharpTokens.STATIC
              || previous == CSharpTokens.PUBLIC
              || previous == CSharpTokens.PRIVATE
              || previous == CSharpTokens.PROTECTED
              || previous == CSharpTokens.INTERNAL) {
            color = GhostColorScheme.FUNCTION_NAME;
          } else if (previous == CSharpTokens.USING) {
            color = GhostColorScheme.ATTRIBUTE_NAME;
          } else if (previous == CSharpTokens.DYNAMIC
              || previous == CSharpTokens.STRING
              || previous == CSharpTokens.DOUBLE
              || previous == CSharpTokens.INT
              || previous == CSharpTokens.WHERE) {
            color = GhostColorScheme.COLORNEXTCHAR;
          } else if (previous == CSharpTokens.LAMBDA_ARROW) {
            color = GhostColorScheme.COLORUPPERCASE;
          } else if (previous == CSharpTokens.EQ) {
            color = GhostColorScheme.IDENTIFIER_NAME;
          } else if (previous == CSharpTokens.LT || previous == CSharpTokens.QUESTION) {
            color = GhostColorScheme.IDENTIFIER_VAR;
          } else {
            int j = i + 1;
            var next = CSharpTokens.UNKNOWN;
            while (j < tokens.size()) {
              var n = tokens.get(j).token;
              if (n != CSharpTokens.WHITESPACE
                  && n != CSharpTokens.NEWLINE
                  && n != CSharpTokens.BLOCK_COMMENT_COMPLETE
                  && n != CSharpTokens.BLOCK_COMMENT_INCOMPLETE
                  && n != CSharpTokens.LINE_COMMENT
                  && n != CSharpTokens.LINE_COMMENT_DOC) {
                next = n;
                break;
              }
              j++;
            }
            if (next == CSharpTokens.LPAREN) {
              color = GhostColorScheme.FUNCTION_NAME;
            } else if (next == CSharpTokens.DOT) {
              color = GhostColorScheme.COLORNEXTLESS;
            } else if (next == CSharpTokens.LT || next == CSharpTokens.GT) {
              color = GhostColorScheme.HTML_TAG;
            } else if (next == CSharpTokens.EQ || next == CSharpTokens.QUESTION) {
              color = GhostColorScheme.HTML_TAG;
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
        case NULL_COALESCING:
        case NULL_COALESCING_ASSIGN:
        case ARROW:
        case LAMBDA_ARROW:
        case NAMESPACE_SEP:
        case MEMBER_ACCESS:
        case POINTER_ACCESS:
        case INDEX_ACCESS:
        case RANGE:
        case SEMICOLON:
        case COLON:
        case COMMA:
        case DOT:
        case ELLIPSIS:
        case QUESTION:
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

    public CSharpTokens token;

    public int offset;

    public String url;

    public HighlightToken(CSharpTokens token, int offset) {
      this.token = token;
      this.offset = offset;
    }

    public HighlightToken(CSharpTokens token, int offset, String url) {
      this.token = token;
      this.offset = offset;
      this.url = url;
    }
  }
}
