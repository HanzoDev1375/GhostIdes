/**
 * Comment by ghost ide
 *
 * @author: Ninjacoder
 */
package ir.hanzodev1375.ghostide.codeeditors.langs.sql;

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

public class SqlIncrementalAnalyzeManager
    extends AsyncIncrementalAnalyzeManager<SqlState, SqlIncrementalAnalyzeManager.HighlightToken> {

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

  private final ThreadLocal<SqlTextTokenizer> tokenizerProvider = new ThreadLocal<>();

  protected IdentifierAutoComplete.SyncIdentifiers identifiers =
      new IdentifierAutoComplete.SyncIdentifiers();

  private synchronized SqlTextTokenizer obtainTokenizer() {
    var res = tokenizerProvider.get();
    if (res == null) {
      res = new SqlTextTokenizer("");
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
          if (token == SqlTokens.LPAREN) {
            CodeBlock block = new CodeBlock();
            block.startLine = i;
            block.startColumn = offset;
            stack.push(block);
          } else if (token == SqlTokens.RPAREN) {
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

  private static int getType(SqlTokens token) {
    if (token == SqlTokens.LPAREN || token == SqlTokens.RPAREN) return 1;
    if (token == SqlTokens.LBRACK || token == SqlTokens.RBRACK) return 2;
    return 0;
  }

  private static boolean isStart(SqlTokens token) {
    return token == SqlTokens.LPAREN || token == SqlTokens.LBRACK;
  }

  @NonNull
  @Override
  public SqlState getInitialState() {
    return new SqlState();
  }

  @Override
  public boolean stateEquals(@NonNull SqlState state, @NonNull SqlState another) {
    return state.equals(another);
  }

  @Override
  public void onAddState(SqlState state) {
    if (state.identifiers != null) {
      for (String identifier : state.identifiers) {
        identifiers.identifierIncrease(identifier);
      }
    }
  }

  @Override
  public void onAbandonState(SqlState state) {
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
  public LineTokenizeResult<SqlState, HighlightToken> tokenizeLine(
      CharSequence line, SqlState state, int lineIndex) {
    var tokens = new ArrayList<HighlightToken>();
    int newState = STATE_NORMAL;
    var stateObj = new SqlState();
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
      tokens.add(new HighlightToken(SqlTokens.UNKNOWN, 0));
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
            line.subSequence(0, offset), 0, SqlTokens.BLOCK_COMMENT_COMPLETE, tokens);
      } else {
        tokens.add(new HighlightToken(SqlTokens.BLOCK_COMMENT_COMPLETE, 0));
      }
      return IntPair.pack(STATE_NORMAL, offset);
    }
    if (offset < 1000) {
      detectHighlightUrls(
          line.subSequence(0, offset), 0, SqlTokens.BLOCK_COMMENT_INCOMPLETE, tokens);
    } else {
      tokens.add(new HighlightToken(SqlTokens.BLOCK_COMMENT_INCOMPLETE, 0));
    }
    return IntPair.pack(STATE_INCOMPLETE_BLOCK_COMMENT, offset);
  }

  private int tokenizeNormal(
      CharSequence text, int offset, List<HighlightToken> tokens, SqlState st) {
    var tokenizer = obtainTokenizer();
    tokenizer.reset(text);
    tokenizer.offset = offset;
    SqlTokens token;
    int state = STATE_NORMAL;
    while ((token = tokenizer.nextToken()) != SqlTokens.EOF) {
      if (tokenizer.getTokenLength() < 1000
          && (token == SqlTokens.STRING_LITERAL
              || token == SqlTokens.BLOCK_COMMENT_COMPLETE
              || token == SqlTokens.BLOCK_COMMENT_INCOMPLETE
              || token == SqlTokens.LINE_COMMENT)) {
        detectHighlightUrls(tokenizer.getTokenText(), tokenizer.offset, token, tokens);
        if (token == SqlTokens.BLOCK_COMMENT_INCOMPLETE) {
          state = STATE_INCOMPLETE_BLOCK_COMMENT;
          break;
        }
        continue;
      }
      tokens.add(new HighlightToken(token, tokenizer.offset));
      if (token == SqlTokens.LPAREN || token == SqlTokens.RPAREN) {
        st.hasBraces = true;
      }
      if (token == SqlTokens.LPAREN || token == SqlTokens.LBRACK) {
        st.bracketDepth++;
      } else if (token == SqlTokens.RPAREN || token == SqlTokens.RBRACK) {
        st.bracketDepth = Math.max(0, st.bracketDepth - 1);
      }
      if (token == SqlTokens.IDENTIFIER) {
        st.addIdentifier(tokenizer.getTokenText());
      }
      if (token == SqlTokens.BLOCK_COMMENT_INCOMPLETE) {
        state = STATE_INCOMPLETE_BLOCK_COMMENT;
        break;
      }
    }
    return state;
  }

  private void detectHighlightUrls(
      CharSequence tokenText, int offset, SqlTokens token, List<HighlightToken> tokens) {
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
  public List<Span> generateSpansForLine(LineTokenizeResult<SqlState, HighlightToken> lineResult) {
    var spans = new ArrayList<Span>();
    var tokens = lineResult.tokens;
    SqlTokens previous = SqlTokens.UNKNOWN;
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
        case SELECT:
        case INSERT:
        case UPDATE:
        case DELETE:
        case CREATE:
        case DROP:
        case ALTER:
        case TABLE:
        case DATABASE:
        case INDEX:
        case VIEW:
        case PROCEDURE:
        case FUNCTION:
        case TRIGGER:
        case FROM:
        case WHERE:
        case GROUP_BY:
        case ORDER_BY:
        case HAVING:
        case JOIN:
        case INNER:
        case LEFT:
        case RIGHT:
        case FULL:
        case CROSS:
        case ON:
        case AS:
        case INTO:
        case VALUES:
        case SET:
        case DISTINCT:
        case ALL:
        case ANY:
        case EXISTS:
        case BETWEEN:
        case IN:
        case IS:
        case NOT:
        case AND:
        case OR:
        case CASE:
        case WHEN:
        case THEN:
        case ELSE:
        case END:
        case UNION:
        case INTERSECT:
        case EXCEPT:
        case PRIMARY_KEY:
        case FOREIGN_KEY:
        case REFERENCES:
        case CONSTRAINT:
        case DEFAULT:
        case UNIQUE:
        case CHECK:
        case AUTO_INCREMENT:
        case SERIAL:
        case LIMIT:
        case OFFSET:
        case FETCH:
        case NEXT:
        case ROWS:
        case ONLY:
        case WITH:
        case RECURSIVE:
        case OVER:
        case PARTITION_BY:
        case ROWS_BETWEEN:
        case UNBOUNDED:
        case PRECEDING:
        case FOLLOWING:
        case CURRENT_ROW:
        case BEGIN:
        case TRANSACTION:
        case COMMIT:
        case ROLLBACK:
        case SAVEPOINT:
        case CAST:
        case COALESCE:
        case NULLIF:
        case GREATEST:
        case LEAST:
        case EXTRACT:
        case DATE_PART:
        case DATE_TRUNC:
        case RANK:
        case DENSE_RANK:
        case ROW_NUMBER:
        case LAG:
        case LEAD:
        case FIRST_VALUE:
        case LAST_VALUE:
        case MATERIALIZED:
        case NOT_MATERIALIZED:
        case DO:
        case FOR:
        case EACH:
        case STATEMENT:
        case ROW:
        case BEFORE:
        case AFTER:
        case INSTEAD_OF:
        case CASCADE:
        case RESTRICT:
        case NO_ACTION:
        case SET_NULL:
        case SET_DEFAULT:
        case NULL:
          span =
              SpanFactory.obtain(
                  offset, TextStyle.makeStyle(GhostColorScheme.KEYWORD, 0, true, false, false));
          break;
        case COUNT:
        case SUM:
        case AVG:
        case MAX:
        case MIN:
        case GROUP_CONCAT:
        case STRING_AGG:
        case ARRAY_AGG:
          span =
              SpanFactory.obtain(
                  offset,
                  TextStyle.makeStyle(GhostColorScheme.FUNCTION_NAME, 0, true, false, false));
          break;
        case INT:
        case BIGINT:
        case SMALLINT:
        case TINYINT:
        case DECIMAL:
        case NUMERIC:
        case FLOAT:
        case DOUBLE:
        case CHAR:
        case VARCHAR:
        case TEXT:
        case NCHAR:
        case NVARCHAR:
        case DATE:
        case TIME:
        case DATETIME:
        case TIMESTAMP:
        case BOOLEAN:
        case BLOB:
          span =
              SpanFactory.obtain(
                  offset,
                  TextStyle.makeStyle(GhostColorScheme.COLORNEXTCHAR, 0, true, false, false));
          break;
        case NULL_LITERAL:
        case INTEGER_LITERAL:
        case REAL_LITERAL:
        case STRING_LITERAL:
          span = SpanFactory.obtain(offset, TextStyle.makeStyle(GhostColorScheme.LITERAL, true));
          break;
        case IDENTIFIER:
          int color = GhostColorScheme.TEXT_NORMAL;
          if (previous == SqlTokens.FROM
              || previous == SqlTokens.JOIN
              || previous == SqlTokens.INNER
              || previous == SqlTokens.LEFT
              || previous == SqlTokens.RIGHT
              || previous == SqlTokens.FULL
              || previous == SqlTokens.CROSS) {
            color = GhostColorScheme.COLORNEXTCHAR;
          } else if (previous == SqlTokens.AS
              || previous == SqlTokens.SELECT
              || previous == SqlTokens.ON
              || previous == SqlTokens.WHERE
              || previous == SqlTokens.ORDER_BY
              || previous == SqlTokens.GROUP_BY) {
            color = GhostColorScheme.IDENTIFIER_NAME;
          } else if (previous == SqlTokens.INTO || previous == SqlTokens.VALUES) {
            color = GhostColorScheme.COLORNEXTCHAR;
          } else if (previous == SqlTokens.TABLE
              || previous == SqlTokens.DATABASE
              || previous == SqlTokens.VIEW
              || previous == SqlTokens.INDEX) {
            color = GhostColorScheme.COLORNEXTCHAR;
          } else if (previous == SqlTokens.PRIMARY_KEY
              || previous == SqlTokens.FOREIGN_KEY
              || previous == SqlTokens.CONSTRAINT) {
            color = GhostColorScheme.COLORUPPERCASE;
          } else {
            int j = i + 1;
            SqlTokens next = SqlTokens.UNKNOWN;
            while (j < tokens.size()) {
              SqlTokens n = tokens.get(j).token;
              if (n != SqlTokens.WHITESPACE
                  && n != SqlTokens.NEWLINE
                  && n != SqlTokens.BLOCK_COMMENT_COMPLETE
                  && n != SqlTokens.BLOCK_COMMENT_INCOMPLETE
                  && n != SqlTokens.LINE_COMMENT) {
                next = n;
                break;
              }
              j++;
            }
            if (next == SqlTokens.LPAREN) {
              color = GhostColorScheme.FUNCTION_NAME;
            } else if (next == SqlTokens.DOT) {
              color = GhostColorScheme.COLORNEXTLESS;
            } else if (next == SqlTokens.ASSIGN || next == SqlTokens.EQ) {
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
        case EQ:
        case NOT_EQ:
        case LT:
        case GT:
        case LT_EQ:
        case GT_EQ:
        case ASSIGN:
        case CONCAT:
        case LIKE:
        case SEMICOLON:
        case COLON:
        case COMMA:
        case DOT:
          span = SpanFactory.obtain(offset, GhostColorScheme.OPERATOR);
          break;
        case LPAREN:
        case LBRACK:
          span = SpanFactory.obtain(offset, bracketStyle(depth));
          depth++;
          break;
        case RPAREN:
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

    public SqlTokens token;

    public int offset;

    public String url;

    public HighlightToken(SqlTokens token, int offset) {
      this.token = token;
      this.offset = offset;
    }

    public HighlightToken(SqlTokens token, int offset, String url) {
      this.token = token;
      this.offset = offset;
      this.url = url;
    }
  }
}
