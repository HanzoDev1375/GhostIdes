/**
 * Comment by ghost ide
 *
 * @author: Ninjacoder
 */
package ir.hanzodev1375.ghostide.codeeditors.langs.swift;

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

/**
 * Incremental analyze manager for Swift. Handles nested block comments, multi-line triple-quoted
 * strings, interpolation coloring and rotating bracket colors.
 */
public class SwiftIncrementalAnalyzeManager
    extends AsyncIncrementalAnalyzeManager<
        SwiftState, SwiftIncrementalAnalyzeManager.HighlightToken> {

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

  private final ThreadLocal<SwiftTextTokenizer> tokenizerProvider = new ThreadLocal<>();

  protected IdentifierAutoComplete.SyncIdentifiers identifiers =
      new IdentifierAutoComplete.SyncIdentifiers();

  /** Pending nesting depth while filling an incomplete block comment. */
  private int fillCommentDepth;

  private synchronized SwiftTextTokenizer obtainTokenizer() {
    var res = tokenizerProvider.get();
    if (res == null) {
      res = new SwiftTextTokenizer("");
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
          state.state.state == SwiftState.STATE_NORMAL
              || (state.state.state == SwiftState.STATE_INCOMPLETE_BLOCK_COMMENT
                  && state.tokens.size() > 1);
      if (state.state.hasBraces || checkForIdentifiers) {
        for (var tokenRecord : state.tokens) {
          var token = tokenRecord.token;
          int offset = tokenRecord.offset;
          if (token == SwiftTokens.LBRACE) {
            CodeBlock block = new CodeBlock();
            block.startLine = i;
            block.startColumn = offset;
            stack.push(block);
          } else if (token == SwiftTokens.RBRACE) {
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

  private static int getType(SwiftTokens token) {
    if (token == SwiftTokens.LBRACE || token == SwiftTokens.RBRACE) return 3;
    if (token == SwiftTokens.LBRACK || token == SwiftTokens.RBRACK) return 2;
    if (token == SwiftTokens.LPAREN || token == SwiftTokens.RPAREN) return 1;
    return 0;
  }

  private static boolean isStart(SwiftTokens token) {
    return token == SwiftTokens.LBRACE
        || token == SwiftTokens.LBRACK
        || token == SwiftTokens.LPAREN;
  }

  @NonNull
  @Override
  public SwiftState getInitialState() {
    return new SwiftState();
  }

  @Override
  public boolean stateEquals(@NonNull SwiftState state, @NonNull SwiftState another) {
    return state.equals(another);
  }

  @Override
  public void onAddState(SwiftState state) {
    if (state.identifiers != null) {
      for (String identifier : state.identifiers) {
        identifiers.identifierIncrease(identifier);
      }
    }
  }

  @Override
  public void onAbandonState(SwiftState state) {
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
  public LineTokenizeResult<SwiftState, HighlightToken> tokenizeLine(
      CharSequence line, SwiftState state, int lineIndex) {
    var tokens = new ArrayList<HighlightToken>();
    int newState = SwiftState.STATE_NORMAL;
    var stateObj = new SwiftState();
    stateObj.commentDepth = state.commentDepth;
    stateObj.startBracketDepth = state.bracketDepth;
    stateObj.bracketDepth = state.bracketDepth;
    switch (state.state) {
      case SwiftState.STATE_NORMAL:
        newState = tokenizeNormal(line, 0, tokens, stateObj);
        break;
      case SwiftState.STATE_INCOMPLETE_BLOCK_COMMENT:
        fillCommentDepth = state.commentDepth;
        var res = tryFillIncompleteComment(line, tokens);
        stateObj.commentDepth = fillCommentDepth;
        newState = IntPair.getFirst(res);
        if (newState == SwiftState.STATE_NORMAL) {
          newState = tokenizeNormal(line, IntPair.getSecond(res), tokens, stateObj);
        } else {
          newState = SwiftState.STATE_INCOMPLETE_BLOCK_COMMENT;
        }
        break;
      case SwiftState.STATE_IN_TRIPLE_STRING:
        var resStr = tryFillIncompleteTripleString(line, tokens);
        newState = IntPair.getFirst(resStr);
        if (newState == SwiftState.STATE_NORMAL) {
          newState = tokenizeNormal(line, IntPair.getSecond(resStr), tokens, stateObj);
        } else {
          newState = SwiftState.STATE_IN_TRIPLE_STRING;
        }
        break;
    }
    if (tokens.isEmpty()) {
      tokens.add(new HighlightToken(SwiftTokens.UNKNOWN, 0));
    }
    stateObj.state = newState;
    return new LineTokenizeResult<>(stateObj, tokens);
  }

  /**
   * Consumes a line continuing an unterminated nested block comment. On success the returned
   * offset points right after the closing marker.
   */
  private long tryFillIncompleteComment(CharSequence line, List<HighlightToken> tokens) {
    int depth = fillCommentDepth;
    int offset = 0;
    while (offset < line.length()) {
      char c = line.charAt(offset);
      if (c == '/' && offset + 1 < line.length() && line.charAt(offset + 1) == '*') {
        depth++;
        offset += 2;
        continue;
      }
      if (c == '*' && offset + 1 < line.length() && line.charAt(offset + 1) == '/') {
        depth--;
        offset += 2;
        if (depth == 0) {
          fillCommentDepth = 0;
          if (offset < 1000) {
            detectHighlightUrls(
                line.subSequence(0, offset), 0, SwiftTokens.BLOCK_COMMENT_COMPLETE, tokens);
          } else {
            tokens.add(new HighlightToken(SwiftTokens.BLOCK_COMMENT_COMPLETE, 0));
          }
          return IntPair.pack(SwiftState.STATE_NORMAL, offset);
        }
        continue;
      }
      offset++;
    }
    fillCommentDepth = depth;
    if (offset < 1000) {
      detectHighlightUrls(
          line.subSequence(0, offset), 0, SwiftTokens.BLOCK_COMMENT_INCOMPLETE, tokens);
    } else {
      tokens.add(new HighlightToken(SwiftTokens.BLOCK_COMMENT_INCOMPLETE, 0));
    }
    return IntPair.pack(SwiftState.STATE_INCOMPLETE_BLOCK_COMMENT, offset);
  }

  /** Consumes a line continuing an unterminated triple-quoted string. */
  private long tryFillIncompleteTripleString(CharSequence line, List<HighlightToken> tokens) {
    for (int i = 0; i + 2 < line.length(); i++) {
      char c = line.charAt(i);
      if (c == '\\') {
        i++;
        continue;
      }
      if (c == '"' && line.charAt(i + 1) == '"' && line.charAt(i + 2) == '"') {
        tokens.add(new HighlightToken(SwiftTokens.TRIPLE_STRING_COMPLETE, 0));
        return IntPair.pack(SwiftState.STATE_NORMAL, i + 3);
      }
    }
    tokens.add(new HighlightToken(SwiftTokens.TRIPLE_STRING_INCOMPLETE, 0));
    return IntPair.pack(SwiftState.STATE_IN_TRIPLE_STRING, line.length());
  }

  private int tokenizeNormal(
      CharSequence text, int offset, List<HighlightToken> tokens, SwiftState st) {
    var tokenizer = obtainTokenizer();
    tokenizer.reset(text);
    tokenizer.offset = offset;
    SwiftTokens token;
    int state = SwiftState.STATE_NORMAL;
    while ((token = tokenizer.nextToken()) != SwiftTokens.EOF) {
      if (tokenizer.getTokenLength() < 1000
          && (token == SwiftTokens.LINE_COMMENT
              || token == SwiftTokens.BLOCK_COMMENT_COMPLETE
              || token == SwiftTokens.STRING_CHUNK)) {
        detectHighlightUrls(tokenizer.getTokenText(), tokenizer.offset, token, tokens);
        continue;
      }
      HighlightToken ht = new HighlightToken(token, tokenizer.offset);
      if (token == SwiftTokens.IDENTIFIER
          || token == SwiftTokens.KEYWORD
          || token == SwiftTokens.TYPE_KEYWORD
          || token == SwiftTokens.BUILTIN
          || token == SwiftTokens.ANNOTATION) {
        ht.tokenText = tokenizer.getTokenText().toString();
      }
      tokens.add(ht);
      if (token == SwiftTokens.TRIPLE_STRING_INCOMPLETE) {
        st.state = SwiftState.STATE_IN_TRIPLE_STRING;
        state = SwiftState.STATE_IN_TRIPLE_STRING;
      }
      if (token == SwiftTokens.BLOCK_COMMENT_INCOMPLETE) {
        st.state = SwiftState.STATE_INCOMPLETE_BLOCK_COMMENT;
        st.commentDepth = Math.max(1, tokenizer.blockCommentDepth);
        state = SwiftState.STATE_INCOMPLETE_BLOCK_COMMENT;
        break;
      }
      if (token == SwiftTokens.LBRACE || token == SwiftTokens.RBRACE) {
        st.hasBraces = true;
      }
      if (token == SwiftTokens.LPAREN || token == SwiftTokens.LBRACE || token == SwiftTokens.LBRACK) {
        st.bracketDepth++;
      } else if (token == SwiftTokens.RPAREN
          || token == SwiftTokens.RBRACE
          || token == SwiftTokens.RBRACK) {
        st.bracketDepth = Math.max(0, st.bracketDepth - 1);
      }
      if (token == SwiftTokens.IDENTIFIER || token == SwiftTokens.ESCAPED_IDENTIFIER) {
        st.addIdentifier(tokenizer.getTokenText());
      }
    }
    return state;
  }

  private void detectHighlightUrls(
      CharSequence tokenText, int offset, SwiftTokens token, List<HighlightToken> tokens) {
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
  public List<Span> generateSpansForLine(LineTokenizeResult<SwiftState, HighlightToken> lineResult) {
    var spans = new ArrayList<Span>();
    var tokens = lineResult.tokens;
    SwiftTokens previous = SwiftTokens.UNKNOWN;
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
        case KEYWORD:
          span =
              SpanFactory.obtain(
                  offset, TextStyle.makeStyle(GhostColorScheme.KEYWORD, 0, true, false, false));
          String t = tokenRecord.tokenText != null ? tokenRecord.tokenText : "";
          if ("func".equals(t) || "init".equals(t)) expectFnName = true;
          if ("struct".equals(t)
              || "class".equals(t)
              || "enum".equals(t)
              || "protocol".equals(t)
              || "extension".equals(t)
              || "actor".equals(t)
              || "typealias".equals(t)) expectType = true;
          break;
        case TYPE_KEYWORD:
          span =
              SpanFactory.obtain(
                  offset,
                  TextStyle.makeStyle(GhostColorScheme.COLORNEXTCHAR, 0, true, false, false));
          break;
        case BUILTIN:
        case ANNOTATION:
          span =
              SpanFactory.obtain(
                  offset,
                  TextStyle.makeStyle(GhostColorScheme.FUNCTION_NAME, 0, true, false, false));
          break;
        case INTERPOLATION:
          span =
              SpanFactory.obtain(
                  offset,
                  TextStyle.makeStyle(GhostColorScheme.COLORNEXTBRAK, 0, true, false, false));
          break;
        case INTEGER_LITERAL:
        case FLOATING_LITERAL:
        case CHARACTER_LITERAL:
        case STRING_CHUNK:
        case TRIPLE_STRING_COMPLETE:
        case TRIPLE_STRING_INCOMPLETE:
        case BOOLEAN_LITERAL:
        case NIL_LITERAL:
          span = SpanFactory.obtain(offset, TextStyle.makeStyle(GhostColorScheme.LITERAL, true));
          break;
        case IDENTIFIER:
        case ESCAPED_IDENTIFIER:
          int color = GhostColorScheme.TEXT_NORMAL;
          if (expectFnName) {
            color = GhostColorScheme.FUNCTION_NAME;
            expectFnName = false;
          } else if (expectType) {
            color = GhostColorScheme.COLORNEXTCHAR;
            expectType = false;
          } else if (previous == SwiftTokens.DOT) {
            color = GhostColorScheme.COLORNEXTDOT;
          } else if (previous == SwiftTokens.COLON) {
            color = GhostColorScheme.IDENTIFIER_NAME;
          } else if (previous == SwiftTokens.ASSIGN || previous == SwiftTokens.EQ) {
            color = GhostColorScheme.IDENTIFIER_VAR;
          } else {
            int j = i + 1;
            SwiftTokens next = SwiftTokens.UNKNOWN;
            while (j < tokens.size()) {
              SwiftTokens n = tokens.get(j).token;
              if (n != SwiftTokens.WHITESPACE
                  && n != SwiftTokens.NEWLINE
                  && n != SwiftTokens.LINE_COMMENT
                  && n != SwiftTokens.BLOCK_COMMENT_COMPLETE
                  && n != SwiftTokens.BLOCK_COMMENT_INCOMPLETE) {
                next = n;
                break;
              }
              j++;
            }
            if (next == SwiftTokens.LPAREN) {
              color = GhostColorScheme.FUNCTION_NAME;
            } else if (next == SwiftTokens.ASSIGN || next == SwiftTokens.EQ) {
              color = GhostColorScheme.IDENTIFIER_VAR;
            } else if (next == SwiftTokens.COLON) {
              color = GhostColorScheme.IDENTIFIER_NAME;
            } else if (next == SwiftTokens.DOT) {
              color = GhostColorScheme.COLORNEXTDOT;
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
        case LOGICAL_AND:
        case LOGICAL_OR:
        case SHIFT_LEFT:
        case SHIFT_RIGHT:
        case SHIFT_LEFT_ASSIGN:
        case SHIFT_RIGHT_ASSIGN:
        case PLUS_ASSIGN:
        case MINUS_ASSIGN:
        case STAR_ASSIGN:
        case SLASH_ASSIGN:
        case PERCENT_ASSIGN:
        case AND_ASSIGN:
        case OR_ASSIGN:
        case XOR_ASSIGN:
        case ARROW:
        case NULL_COALESCE:
        case QUESTION:
        case DOUBLE_DOT:
        case CLOSED_RANGE:
        case HALF_OPEN_RANGE:
        case AT:
        case HASH:
        case BACKTICK:
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

    public SwiftTokens token;

    public int offset;

    public String url;

    public String tokenText;

    public HighlightToken(SwiftTokens token, int offset) {
      this.token = token;
      this.offset = offset;
    }

    public HighlightToken(SwiftTokens token, int offset, String url) {
      this.token = token;
      this.offset = offset;
      this.url = url;
    }
  }
}
