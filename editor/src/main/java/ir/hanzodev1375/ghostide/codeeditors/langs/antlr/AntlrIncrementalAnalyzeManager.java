/**
 * Comment by ghost ide
 *
 * @author: Ninjacoder
 */
package ir.hanzodev1375.ghostide.codeeditors.langs.antlr;

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

public class AntlrIncrementalAnalyzeManager
    extends AsyncIncrementalAnalyzeManager<
        AntlrState, AntlrIncrementalAnalyzeManager.HighlightToken> {

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

  private final ThreadLocal<AntlrTextTokenizer> tokenizerProvider = new ThreadLocal<>();

  protected IdentifierAutoComplete.SyncIdentifiers identifiers =
      new IdentifierAutoComplete.SyncIdentifiers();

  private synchronized AntlrTextTokenizer obtainTokenizer() {
    var res = tokenizerProvider.get();
    if (res == null) {
      res = new AntlrTextTokenizer("");
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
          if (token == AntlrTokens.LBRACE) {
            CodeBlock block = new CodeBlock();
            block.startLine = i;
            block.startColumn = offset;
            stack.push(block);
          } else if (token == AntlrTokens.RBRACE) {
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

  private static int getType(AntlrTokens token) {
    if (token == AntlrTokens.LBRACE || token == AntlrTokens.RBRACE) return 3;
    if (token == AntlrTokens.LBRACK || token == AntlrTokens.RBRACK) return 2;
    if (token == AntlrTokens.LPAREN || token == AntlrTokens.RPAREN) return 1;
    return 0;
  }

  private static boolean isStart(AntlrTokens token) {
    return token == AntlrTokens.LBRACE
        || token == AntlrTokens.LBRACK
        || token == AntlrTokens.LPAREN;
  }

  @NonNull
  @Override
  public AntlrState getInitialState() {
    return new AntlrState();
  }

  @Override
  public boolean stateEquals(@NonNull AntlrState state, @NonNull AntlrState another) {
    return state.equals(another);
  }

  @Override
  public void onAddState(AntlrState state) {
    if (state.identifiers != null) {
      for (String identifier : state.identifiers) {
        identifiers.identifierIncrease(identifier);
      }
    }
  }

  @Override
  public void onAbandonState(AntlrState state) {
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
  public LineTokenizeResult<AntlrState, HighlightToken> tokenizeLine(
      CharSequence line, AntlrState state, int lineIndex) {
    var tokens = new ArrayList<HighlightToken>();
    int newState = STATE_NORMAL;
    var stateObj = new AntlrState();
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
      tokens.add(new HighlightToken(AntlrTokens.UNKNOWN, 0));
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
            line.subSequence(0, offset), 0, AntlrTokens.BLOCK_COMMENT_COMPLETE, tokens);
      } else {
        tokens.add(new HighlightToken(AntlrTokens.BLOCK_COMMENT_COMPLETE, 0));
      }
      return IntPair.pack(STATE_NORMAL, offset);
    }
    if (offset < 1000) {
      detectHighlightUrls(
          line.subSequence(0, offset), 0, AntlrTokens.BLOCK_COMMENT_INCOMPLETE, tokens);
    } else {
      tokens.add(new HighlightToken(AntlrTokens.BLOCK_COMMENT_INCOMPLETE, 0));
    }
    return IntPair.pack(STATE_INCOMPLETE_BLOCK_COMMENT, offset);
  }

  private int tokenizeNormal(
      CharSequence text, int offset, List<HighlightToken> tokens, AntlrState st) {
    var tokenizer = obtainTokenizer();
    tokenizer.reset(text);
    tokenizer.offset = offset;
    AntlrTokens token;
    int state = STATE_NORMAL;
    while ((token = tokenizer.nextToken()) != AntlrTokens.EOF) {
      if (tokenizer.getTokenLength() < 1000
          && (token == AntlrTokens.STRING_LITERAL
              || token == AntlrTokens.BLOCK_COMMENT_COMPLETE
              || token == AntlrTokens.BLOCK_COMMENT_INCOMPLETE
              || token == AntlrTokens.LINE_COMMENT
              || token == AntlrTokens.ACTION)) {
        detectHighlightUrls(tokenizer.getTokenText(), tokenizer.offset, token, tokens);
        if (token == AntlrTokens.BLOCK_COMMENT_INCOMPLETE) {
          state = STATE_INCOMPLETE_BLOCK_COMMENT;
          break;
        }
        continue;
      }
      HighlightToken ht = new HighlightToken(token, tokenizer.offset);
      if (token == AntlrTokens.RULE_REF
          || token == AntlrTokens.TOKEN_REF
          || token == AntlrTokens.IDENTIFIER) {
        ht.tokenText = tokenizer.getTokenText().toString();
      }
      tokens.add(ht);
      if (token == AntlrTokens.LBRACE || token == AntlrTokens.RBRACE) {
        st.hasBraces = true;
      }
      if (token == AntlrTokens.LPAREN
          || token == AntlrTokens.LBRACE
          || token == AntlrTokens.LBRACK) {
        st.bracketDepth++;
      } else if (token == AntlrTokens.RPAREN
          || token == AntlrTokens.RBRACE
          || token == AntlrTokens.RBRACK) {
        st.bracketDepth = Math.max(0, st.bracketDepth - 1);
      }
      if (token == AntlrTokens.IDENTIFIER) {
        st.addIdentifier(tokenizer.getTokenText());
      }
      if (token == AntlrTokens.BLOCK_COMMENT_INCOMPLETE) {
        state = STATE_INCOMPLETE_BLOCK_COMMENT;
        break;
      }
    }
    return state;
  }

  private void detectHighlightUrls(
      CharSequence tokenText, int offset, AntlrTokens token, List<HighlightToken> tokens) {
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
      LineTokenizeResult<AntlrState, HighlightToken> lineResult) {
    var spans = new ArrayList<Span>();
    var tokens = lineResult.tokens;
    AntlrTokens previous = AntlrTokens.UNKNOWN;
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
        case GRAMMAR:
        case LEXER:
        case PARSER:
        case FRAGMENT:
        case OPTIONS:
        case TOKENS:
        case CHANNELS:
        case IMPORT:
        case MODE:
        case PUSH_MODE:
        case POP_MODE:
        case MORE:
        case SKIP:
        case TYPE:
        case RETURNS:
        case THROWS:
        case CATCH:
        case FINALLY:
        case LOCAL:
          span =
              SpanFactory.obtain(
                  offset, TextStyle.makeStyle(GhostColorScheme.KEYWORD, 0, true, false, false));
          break;
        case ACTION:
          span =
              SpanFactory.obtain(
                  offset,
                  TextStyle.makeStyle(GhostColorScheme.ATTRIBUTE_VALUE, 0, true, false, false));
          break;
        case RULE_REF:
          span =
              SpanFactory.obtain(
                  offset,
                  TextStyle.makeStyle(GhostColorScheme.FUNCTION_NAME, 0, true, false, false));
          break;
        case TOKEN_REF:
          span =
              SpanFactory.obtain(
                  offset,
                  TextStyle.makeStyle(GhostColorScheme.COLORNEXTCHAR, 0, true, false, false));
          break;
        case STRING_LITERAL:
          span = SpanFactory.obtain(offset, TextStyle.makeStyle(GhostColorScheme.LITERAL, true));
          break;
        case INTEGER_LITERAL:
          span = SpanFactory.obtain(offset, TextStyle.makeStyle(GhostColorScheme.LITERAL, true));
          break;
        case IDENTIFIER:
          int color = GhostColorScheme.TEXT_NORMAL;
          if (previous == AntlrTokens.DOT) {
            color = GhostColorScheme.COLORNEXTDOT;
          } else if (previous == AntlrTokens.COLON) {
            color = GhostColorScheme.IDENTIFIER_NAME;
          } else {
            int j = i + 1;
            AntlrTokens next = AntlrTokens.UNKNOWN;
            while (j < tokens.size()) {
              AntlrTokens n = tokens.get(j).token;
              if (n != AntlrTokens.WHITESPACE
                  && n != AntlrTokens.NEWLINE
                  && n != AntlrTokens.LINE_COMMENT
                  && n != AntlrTokens.BLOCK_COMMENT_COMPLETE
                  && n != AntlrTokens.BLOCK_COMMENT_INCOMPLETE) {
                next = n;
                break;
              }
              j++;
            }
            if (next == AntlrTokens.ASSIGN || next == AntlrTokens.EQ) {
              color = GhostColorScheme.IDENTIFIER_VAR;
            } else if (next == AntlrTokens.LPAREN) {
              color = GhostColorScheme.FUNCTION_NAME;
            } else if (next == AntlrTokens.COLON) {
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
        case PIPE:
        case AMPERSAND:
        case CARET:
        case TILDE:
        case LT:
        case GT:
        case LT_EQ:
        case GT_EQ:
        case EQ:
        case NOT_EQ:
        case ASSIGN:
        case QUESTION:
        case SEMICOLON:
        case COLON:
        case COMMA:
        case DOT:
        case NOT:
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

    public AntlrTokens token;

    public int offset;

    public String url;

    public String tokenText;

    public HighlightToken(AntlrTokens token, int offset) {
      this.token = token;
      this.offset = offset;
    }

    public HighlightToken(AntlrTokens token, int offset, String url) {
      this.token = token;
      this.offset = offset;
      this.url = url;
    }
  }
}
