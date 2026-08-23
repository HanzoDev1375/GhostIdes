/**
 * Comment by ghost ide
 *
 * @author: Ninjacoder
 */
package ir.hanzodev1375.ghostide.codeeditors.langs.simple;

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

public class SimpleIncrementalAnalyzeManager
    extends AsyncIncrementalAnalyzeManager<
        SimpleState, SimpleIncrementalAnalyzeManager.HighlightToken> {

  private static final int STATE_NORMAL = 0;

  private static final int STATE_INCOMPLETE_BLOCK_COMMENT = 1;

  private static final int STATE_IN_STRING = 2;

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

  private final SimpleLangConfig cfg;

  private final ThreadLocal<SimpleTextTokenizer> tokenizerProvider = new ThreadLocal<>();

  protected IdentifierAutoComplete.SyncIdentifiers identifiers =
      new IdentifierAutoComplete.SyncIdentifiers();

  public SimpleIncrementalAnalyzeManager(SimpleLangConfig cfg) {
    this.cfg = cfg;
  }

  private synchronized SimpleTextTokenizer obtainTokenizer() {
    var res = tokenizerProvider.get();
    if (res == null) {
      res = new SimpleTextTokenizer(cfg, "");
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
          if (token == SimpleTokens.LBRACE) {
            CodeBlock block = new CodeBlock();
            block.startLine = i;
            block.startColumn = offset;
            stack.push(block);
          } else if (token == SimpleTokens.RBRACE) {
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

  private static int getType(SimpleTokens token) {
    if (token == SimpleTokens.LBRACE || token == SimpleTokens.RBRACE) return 3;
    if (token == SimpleTokens.LBRACK || token == SimpleTokens.RBRACK) return 2;
    if (token == SimpleTokens.LPAREN || token == SimpleTokens.RPAREN) return 1;
    return 0;
  }

  private static boolean isStart(SimpleTokens token) {
    return token == SimpleTokens.LBRACE
        || token == SimpleTokens.LBRACK
        || token == SimpleTokens.LPAREN;
  }

  @NonNull
  @Override
  public SimpleState getInitialState() {
    return new SimpleState();
  }

  @Override
  public boolean stateEquals(@NonNull SimpleState state, @NonNull SimpleState another) {
    return state.equals(another);
  }

  @Override
  public void onAddState(SimpleState state) {
    if (state.identifiers != null) {
      for (String identifier : state.identifiers) {
        identifiers.identifierIncrease(identifier);
      }
    }
  }

  @Override
  public void onAbandonState(SimpleState state) {
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
  public LineTokenizeResult<SimpleState, HighlightToken> tokenizeLine(
      CharSequence line, SimpleState state, int lineIndex) {
    var tokens = new ArrayList<HighlightToken>();
    int newState = STATE_NORMAL;
    var stateObj = new SimpleState();
    stateObj.startBracketDepth = state.bracketDepth;
    stateObj.bracketDepth = state.bracketDepth;
    switch (state.state) {
      case STATE_NORMAL:
        newState = tokenizeNormal(line, 0, tokens, stateObj);
        break;
      case STATE_INCOMPLETE_BLOCK_COMMENT:
        var res = tryFillIncompleteComment(line, tokens);
        newState = IntPair.getFirst(res);
        if (newState == STATE_NORMAL) {
          newState = tokenizeNormal(line, IntPair.getSecond(res), tokens, stateObj);
        }
        break;
      case STATE_IN_STRING:
        var resStr = tryFillIncompleteString(line, tokens);
        newState = IntPair.getFirst(resStr);
        if (newState == STATE_NORMAL) {
          newState = tokenizeNormal(line, IntPair.getSecond(resStr), tokens, stateObj);
        }
        break;
    }
    if (tokens.isEmpty()) {
      tokens.add(new HighlightToken(SimpleTokens.UNKNOWN, 0));
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
      tokens.add(new HighlightToken(SimpleTokens.BLOCK_COMMENT_COMPLETE, 0));
      return IntPair.pack(STATE_NORMAL, offset);
    }
    tokens.add(new HighlightToken(SimpleTokens.BLOCK_COMMENT_INCOMPLETE, 0));
    return IntPair.pack(STATE_INCOMPLETE_BLOCK_COMMENT, offset);
  }

  private long tryFillIncompleteString(CharSequence line, List<HighlightToken> tokens) {
    for (int i = 0; i + 2 < line.length(); i++) {
      if (line.charAt(i) == '"' && line.charAt(i + 1) == '"' && line.charAt(i + 2) == '"') {
        tokens.add(new HighlightToken(SimpleTokens.STRING_BLOCK_COMPLETE, 0));
        return IntPair.pack(STATE_NORMAL, i + 3);
      }
    }
    tokens.add(new HighlightToken(SimpleTokens.STRING_BLOCK_INCOMPLETE, 0));
    return IntPair.pack(STATE_IN_STRING, line.length());
  }

  private int tokenizeNormal(
      CharSequence text, int offset, List<HighlightToken> tokens, SimpleState st) {
    var tokenizer = obtainTokenizer();
    tokenizer.reset(text);
    tokenizer.offset = offset;
    SimpleTokens token;
    int state = STATE_NORMAL;
    while ((token = tokenizer.nextToken()) != SimpleTokens.EOF) {
      if (tokenizer.getTokenLength() < 1000
          && (token == SimpleTokens.LINE_COMMENT || token == SimpleTokens.BLOCK_COMMENT_COMPLETE)) {
        detectHighlightUrls(tokenizer.getTokenText(), tokenizer.offset, token, tokens);
        continue;
      }
      HighlightToken ht = new HighlightToken(token, tokenizer.offset);
      if (token == SimpleTokens.IDENTIFIER
          || token == SimpleTokens.KEYWORD
          || token == SimpleTokens.TYPE_KEYWORD
          || token == SimpleTokens.BUILTIN
          || token == SimpleTokens.ANNOTATION) {
        ht.tokenText = tokenizer.getTokenText().toString();
      }
      tokens.add(ht);
      if (token == SimpleTokens.STRING_BLOCK_INCOMPLETE) {
        state = STATE_IN_STRING;
        break;
      }
      if (token == SimpleTokens.BLOCK_COMMENT_INCOMPLETE) {
        state = STATE_INCOMPLETE_BLOCK_COMMENT;
        break;
      }
      if (token == SimpleTokens.LBRACE || token == SimpleTokens.RBRACE) {
        st.hasBraces = true;
      }
      if (token == SimpleTokens.LPAREN
          || token == SimpleTokens.LBRACE
          || token == SimpleTokens.LBRACK) {
        st.bracketDepth++;
      } else if (token == SimpleTokens.RPAREN
          || token == SimpleTokens.RBRACE
          || token == SimpleTokens.RBRACK) {
        st.bracketDepth = Math.max(0, st.bracketDepth - 1);
      }
      if (token == SimpleTokens.IDENTIFIER) {
        st.addIdentifier(tokenizer.getTokenText());
      }
    }
    return state;
  }

  private void detectHighlightUrls(
      CharSequence tokenText, int offset, SimpleTokens token, List<HighlightToken> tokens) {
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
      LineTokenizeResult<SimpleState, HighlightToken> lineResult) {
    var spans = new ArrayList<Span>();
    var tokens = lineResult.tokens;
    SimpleTokens previous = SimpleTokens.UNKNOWN;
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
        case KEYWORD:
          span =
              SpanFactory.obtain(
                  offset, TextStyle.makeStyle(GhostColorScheme.KEYWORD, 0, true, false, false));
          break;
        case TYPE_KEYWORD:
        case ANNOTATION:
          span =
              SpanFactory.obtain(
                  offset,
                  TextStyle.makeStyle(GhostColorScheme.COLORNEXTCHAR, 0, true, false, false));
          break;
        case BUILTIN:
          span =
              SpanFactory.obtain(
                  offset,
                  TextStyle.makeStyle(GhostColorScheme.FUNCTION_NAME, 0, true, false, false));
          break;
        case INTEGER_LITERAL:
        case FLOATING_LITERAL:
        case CHARACTER_LITERAL:
        case STRING_LITERAL:
        case STRING_BLOCK_COMPLETE:
        case STRING_BLOCK_INCOMPLETE:
          span = SpanFactory.obtain(offset, TextStyle.makeStyle(GhostColorScheme.LITERAL, true));
          break;
        case IDENTIFIER:
          int color = GhostColorScheme.TEXT_NORMAL;
          if (previous == SimpleTokens.DOT) {
            color = GhostColorScheme.COLORNEXTDOT;
          } else if (previous == SimpleTokens.COLON || previous == SimpleTokens.DOUBLE_COLON) {
            color = GhostColorScheme.IDENTIFIER_NAME;
          } else if (previous == SimpleTokens.ASSIGN || previous == SimpleTokens.EQ) {
            color = GhostColorScheme.IDENTIFIER_VAR;
          } else {
            int j = i + 1;
            SimpleTokens next = SimpleTokens.UNKNOWN;
            while (j < tokens.size()) {
              SimpleTokens n = tokens.get(j).token;
              if (n != SimpleTokens.WHITESPACE && n != SimpleTokens.NEWLINE) {
                next = n;
                break;
              }
              j++;
            }
            if (next == SimpleTokens.LPAREN) {
              color = GhostColorScheme.FUNCTION_NAME;
            } else if (next == SimpleTokens.ASSIGN || next == SimpleTokens.EQ) {
              color = GhostColorScheme.IDENTIFIER_VAR;
            } else if (next == SimpleTokens.COLON) {
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
        case LT_EQ:
        case GT_EQ:
        case ASSIGN:
        case EQ:
        case NOT_EQ:
        case NOT:
        case INC:
        case DEC:
        case SHIFT_LEFT:
        case SHIFT_RIGHT:
        case ARROW:
        case FAT_ARROW:
        case DOUBLE_COLON:
        case QUESTION:
        case AT:
        case HASH:
        case DOLLAR:
        case BACKTICK:
        case PLUS_ASSIGN:
        case MINUS_ASSIGN:
        case STAR_ASSIGN:
        case SLASH_ASSIGN:
        case PERCENT_ASSIGN:
        case AND_ASSIGN:
        case OR_ASSIGN:
        case XOR_ASSIGN:
        case LOGICAL_AND:
        case LOGICAL_OR:
        case SEMICOLON:
        case COLON:
        case COMMA:
        case DOT:
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

    public SimpleTokens token;

    public int offset;

    public String url;

    public String tokenText;

    public HighlightToken(SimpleTokens token, int offset) {
      this.token = token;
      this.offset = offset;
    }

    public HighlightToken(SimpleTokens token, int offset, String url) {
      this.token = token;
      this.offset = offset;
      this.url = url;
    }
  }
}
