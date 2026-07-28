/**
 * Comment by ghost ide
 *
 * @author: Ninjacoder
 */
package ir.hanzodev1375.ghostide.codeeditors.langs.shell;

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

public class ShellIncrementalAnalyzeManager
    extends AsyncIncrementalAnalyzeManager<
        ShellState, ShellIncrementalAnalyzeManager.HighlightToken> {

  private static final int STATE_NORMAL = 0;

  private static final int STATE_INCOMPLETE_COMMENT = 1;

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

  private final ThreadLocal<ShellTextTokenizer> tokenizerProvider = new ThreadLocal<>();

  protected IdentifierAutoComplete.SyncIdentifiers identifiers =
      new IdentifierAutoComplete.SyncIdentifiers();

  private synchronized ShellTextTokenizer obtainTokenizer() {
    var res = tokenizerProvider.get();
    if (res == null) {
      res = new ShellTextTokenizer("");
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
              || (state.state.state == STATE_INCOMPLETE_COMMENT && state.tokens.size() > 1);
      if (state.state.hasBraces || checkForIdentifiers) {
        for (var tokenRecord : state.tokens) {
          var token = tokenRecord.token;
          int offset = tokenRecord.offset;
          if (token == ShellTokens.LBRACE
              || token == ShellTokens.KEYWORD_DO
              || token == ShellTokens.KEYWORD_THEN
              || token == ShellTokens.KEYWORD_ELSE
              || token == ShellTokens.KEYWORD_ELIF
              || token == ShellTokens.KEYWORD_CASE
              || token == ShellTokens.KEYWORD_SELECT) {
            CodeBlock block = new CodeBlock();
            block.startLine = i;
            block.startColumn = offset;
            stack.push(block);
          } else if (token == ShellTokens.RBRACE
              || token == ShellTokens.KEYWORD_DONE
              || token == ShellTokens.KEYWORD_FI
              || token == ShellTokens.KEYWORD_ESAC) {
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

  private static int getType(ShellTokens token) {
    if (token == ShellTokens.LBRACE || token == ShellTokens.RBRACE) return 3;
    if (token == ShellTokens.LBRACK || token == ShellTokens.RBRACK) return 2;
    if (token == ShellTokens.LPAREN || token == ShellTokens.RPAREN) return 1;
    return 0;
  }

  private static boolean isStart(ShellTokens token) {
    return token == ShellTokens.LBRACE
        || token == ShellTokens.LBRACK
        || token == ShellTokens.LPAREN;
  }

  @NonNull
  @Override
  public ShellState getInitialState() {
    return new ShellState();
  }

  @Override
  public boolean stateEquals(@NonNull ShellState state, @NonNull ShellState another) {
    return state.equals(another);
  }

  @Override
  public void onAddState(ShellState state) {
    if (state.identifiers != null) {
      for (String identifier : state.identifiers) {
        identifiers.identifierIncrease(identifier);
      }
    }
  }

  @Override
  public void onAbandonState(ShellState state) {
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
  public LineTokenizeResult<ShellState, HighlightToken> tokenizeLine(
      CharSequence line, ShellState state, int lineIndex) {
    var tokens = new ArrayList<HighlightToken>();
    int newState = STATE_NORMAL;
    var stateObj = new ShellState();
    stateObj.startBracketDepth = state.bracketDepth;
    stateObj.bracketDepth = state.bracketDepth;
    if (state.state == STATE_NORMAL) {
      newState = tokenizeNormal(line, 0, tokens, stateObj);
    } else if (state.state == STATE_INCOMPLETE_COMMENT) {
      // shell فقط کامنت خطی داره (#) پس اینجا دیگه نیازی نیست
      newState = STATE_NORMAL;
    }
    if (tokens.isEmpty()) {
      tokens.add(new HighlightToken(ShellTokens.UNKNOWN, 0));
    }
    stateObj.state = newState;
    return new LineTokenizeResult<>(stateObj, tokens);
  }

  private int tokenizeNormal(
      CharSequence text, int offset, List<HighlightToken> tokens, ShellState st) {
    var tokenizer = obtainTokenizer();
    tokenizer.reset(text);
    tokenizer.offset = offset;
    ShellTokens token;
    int state = STATE_NORMAL;
    while ((token = tokenizer.nextToken()) != ShellTokens.EOF) {
      if (tokenizer.getTokenLength() < 1000
          && (token == ShellTokens.STRING_LITERAL || token == ShellTokens.LINE_COMMENT)) {
        detectHighlightUrls(tokenizer.getTokenText(), tokenizer.offset, token, tokens);
        continue;
      }
      HighlightToken ht = new HighlightToken(token, tokenizer.offset);
      if (token == ShellTokens.IDENTIFIER) {
        ht.tokenText = tokenizer.getTokenText().toString();
      }
      tokens.add(ht);
      if (token == ShellTokens.LBRACE || token == ShellTokens.RBRACE) {
        st.hasBraces = true;
      }
      if (token == ShellTokens.LPAREN
          || token == ShellTokens.LBRACE
          || token == ShellTokens.LBRACK) {
        st.bracketDepth++;
      } else if (token == ShellTokens.RPAREN
          || token == ShellTokens.RBRACE
          || token == ShellTokens.RBRACK) {
        st.bracketDepth = Math.max(0, st.bracketDepth - 1);
      }
      if (token == ShellTokens.IDENTIFIER) {
        st.addIdentifier(tokenizer.getTokenText());
      }
    }
    return state;
  }

  private void detectHighlightUrls(
      CharSequence tokenText, int offset, ShellTokens token, List<HighlightToken> tokens) {
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
      LineTokenizeResult<ShellState, HighlightToken> lineResult) {
    var spans = new ArrayList<Span>();
    var tokens = lineResult.tokens;
    ShellTokens previous = ShellTokens.UNKNOWN;
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
        case KEYWORD_IF:
        case KEYWORD_THEN:
        case KEYWORD_ELSE:
        case KEYWORD_ELIF:
        case KEYWORD_FI:
        case KEYWORD_FOR:
        case KEYWORD_WHILE:
        case KEYWORD_UNTIL:
        case KEYWORD_DO:
        case KEYWORD_DONE:
        case KEYWORD_CASE:
        case KEYWORD_ESAC:
        case KEYWORD_IN:
        case KEYWORD_FUNCTION:
        case KEYWORD_RETURN:
        case KEYWORD_EXIT:
        case KEYWORD_SOURCE:
        case KEYWORD_EXPORT:
        case KEYWORD_READONLY:
        case KEYWORD_LOCAL:
        case KEYWORD_DECLARE:
        case KEYWORD_TYPESET:
        case KEYWORD_UNSET:
        case KEYWORD_SHIFT:
        case KEYWORD_GETOPTS:
        case KEYWORD_SELECT:
        case KEYWORD_TIME:
        case KEYWORD_EVAL:
        case KEYWORD_EXEC:
        case KEYWORD_TRAP:
        case KEYWORD_WAIT:
        case KEYWORD_SUSPEND:
        case ECHO:
          span =
              SpanFactory.obtain(
                  offset, TextStyle.makeStyle(GhostColorScheme.KEYWORD, 0, true, false, false));
          break;
        case BOOLEAN_LITERAL:
          span = SpanFactory.obtain(offset, TextStyle.makeStyle(GhostColorScheme.LITERAL, true));
          break;
        case INTEGER_LITERAL:
        case STRING_LITERAL:
          span = SpanFactory.obtain(offset, TextStyle.makeStyle(GhostColorScheme.LITERAL, true));
          break;
        case IDENTIFIER:
          int color = GhostColorScheme.TEXT_NORMAL;
          if (previous == ShellTokens.DOLLAR) {
            color = GhostColorScheme.IDENTIFIER_VAR;
          } else if (previous == ShellTokens.ASSIGN) {
            color = GhostColorScheme.IDENTIFIER_VAR;
          } else if (previous == ShellTokens.KEYWORD_EXPORT
              || previous == ShellTokens.KEYWORD_READONLY
              || previous == ShellTokens.KEYWORD_LOCAL
              || previous == ShellTokens.KEYWORD_DECLARE) {
            color = GhostColorScheme.IDENTIFIER_VAR;
          } else if (previous == ShellTokens.ECHO) {
            color = GhostColorScheme.IDENTIFIER_NAME;
          } else {
            int j = i + 1;
            ShellTokens next = ShellTokens.UNKNOWN;
            while (j < tokens.size()) {
              ShellTokens n = tokens.get(j).token;
              if (n != ShellTokens.WHITESPACE && n != ShellTokens.NEWLINE) {
                next = n;
                break;
              }
              j++;
            }
            if (next == ShellTokens.LPAREN) {
              color = GhostColorScheme.FUNCTION_NAME;
            } else if (next == ShellTokens.ASSIGN) {
              color = GhostColorScheme.IDENTIFIER_VAR;
            } else if (previous == ShellTokens.KEYWORD_FUNCTION) {
              color = GhostColorScheme.FUNCTION_NAME;
            }
          }
          span = SpanFactory.obtain(offset, TextStyle.makeStyle(color));
          break;
        case DOLLAR:
          span =
              SpanFactory.obtain(
                  offset,
                  TextStyle.makeStyle(GhostColorScheme.ATTRIBUTE_NAME, 0, true, false, false));
          break;
        case PARAM_EXPANSION:
        case COMMAND_SUBSTITUTION:
        case ARITHMETIC:
          span =
              SpanFactory.obtain(
                  offset,
                  TextStyle.makeStyle(GhostColorScheme.ATTRIBUTE_NAME, 0, true, false, false));
          break;
        case BRACE_EXPANSION:
          span =
              SpanFactory.obtain(
                  offset, TextStyle.makeStyle(GhostColorScheme.OPERATOR, 0, true, false, false));
          break;
        case TEST_OPERATOR:
          span =
              SpanFactory.obtain(
                  offset, TextStyle.makeStyle(GhostColorScheme.KEYWORD, 0, true, false, false));
          break;
        case PIPE:
        case AMPERSAND:
        case REDIRECT_IN:
        case REDIRECT_OUT:
        case REDIRECT_APPEND:
        case REDIRECT_ERR:
        case REDIRECT_BOTH:
        case HERE_DOC:
        case HERESTRING:
        case PLUS:
        case MINUS:
        case STAR:
        case SLASH:
        case PERCENT:
        case EQ:
        case NOT_EQ:
        case LT:
        case GT:
        case LT_EQ:
        case GT_EQ:
        case ASSIGN:
        case AND:
        case OR:
        case NOT:
        case INC:
        case DEC:
        case SEMICOLON:
        case COLON:
        case COMMA:
        case DOT:
        case BACKTICK:
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
          break;
        default:
          previous = token;
      }
    }
    return spans;
  }

  public static class HighlightToken {

    public ShellTokens token;

    public int offset;

    public String url;

    public String tokenText;

    public HighlightToken(ShellTokens token, int offset) {
      this.token = token;
      this.offset = offset;
    }

    public HighlightToken(ShellTokens token, int offset, String url) {
      this.token = token;
      this.offset = offset;
      this.url = url;
    }
  }
}
