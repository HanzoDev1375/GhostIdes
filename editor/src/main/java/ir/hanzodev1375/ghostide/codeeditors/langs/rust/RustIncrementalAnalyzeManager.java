/**
 * Comment by ghost ide
 *
 * @author: Ninjacoder
 */
package ir.hanzodev1375.ghostide.codeeditors.langs.rust;

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

public class RustIncrementalAnalyzeManager
    extends AsyncIncrementalAnalyzeManager<
        RustState, RustIncrementalAnalyzeManager.HighlightToken> {

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

  private final ThreadLocal<RustTextTokenizer> tokenizerProvider = new ThreadLocal<>();

  protected IdentifierAutoComplete.SyncIdentifiers identifiers =
      new IdentifierAutoComplete.SyncIdentifiers();

  private synchronized RustTextTokenizer obtainTokenizer() {
    var res = tokenizerProvider.get();
    if (res == null) {
      res = new RustTextTokenizer("");
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
          if (token == RustTokens.LBRACE) {
            CodeBlock block = new CodeBlock();
            block.startLine = i;
            block.startColumn = offset;
            stack.push(block);
          } else if (token == RustTokens.RBRACE) {
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

  private static int getType(RustTokens token) {
    if (token == RustTokens.LBRACE || token == RustTokens.RBRACE) return 3;
    if (token == RustTokens.LBRACK || token == RustTokens.RBRACK) return 2;
    if (token == RustTokens.LPAREN || token == RustTokens.RPAREN) return 1;
    return 0;
  }

  private static boolean isStart(RustTokens token) {
    return token == RustTokens.LBRACE || token == RustTokens.LBRACK || token == RustTokens.LPAREN;
  }

  @NonNull
  @Override
  public RustState getInitialState() {
    return new RustState();
  }

  @Override
  public boolean stateEquals(@NonNull RustState state, @NonNull RustState another) {
    return state.equals(another);
  }

  @Override
  public void onAddState(RustState state) {
    if (state.identifiers != null) {
      for (String identifier : state.identifiers) {
        identifiers.identifierIncrease(identifier);
      }
    }
  }

  @Override
  public void onAbandonState(RustState state) {
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
  public LineTokenizeResult<RustState, HighlightToken> tokenizeLine(
      CharSequence line, RustState state, int lineIndex) {
    var tokens = new ArrayList<HighlightToken>();
    int newState = STATE_NORMAL;
    var stateObj = new RustState();
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
      tokens.add(new HighlightToken(RustTokens.UNKNOWN, 0));
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
            line.subSequence(0, offset), 0, RustTokens.BLOCK_COMMENT_COMPLETE, tokens);
      } else {
        tokens.add(new HighlightToken(RustTokens.BLOCK_COMMENT_COMPLETE, 0));
      }
      return IntPair.pack(STATE_NORMAL, offset);
    }
    if (offset < 1000) {
      detectHighlightUrls(
          line.subSequence(0, offset), 0, RustTokens.BLOCK_COMMENT_INCOMPLETE, tokens);
    } else {
      tokens.add(new HighlightToken(RustTokens.BLOCK_COMMENT_INCOMPLETE, 0));
    }
    return IntPair.pack(STATE_INCOMPLETE_BLOCK_COMMENT, offset);
  }

  private int tokenizeNormal(
      CharSequence text, int offset, List<HighlightToken> tokens, RustState st) {
    var tokenizer = obtainTokenizer();
    tokenizer.reset(text);
    tokenizer.offset = offset;
    RustTokens token;
    int state = STATE_NORMAL;
    while ((token = tokenizer.nextToken()) != RustTokens.EOF) {
      if (tokenizer.getTokenLength() < 1000
          && (token == RustTokens.STRING_LITERAL
              || token == RustTokens.BYTE_STRING_LITERAL
              || token == RustTokens.CHARACTER_LITERAL
              || token == RustTokens.BLOCK_COMMENT_COMPLETE
              || token == RustTokens.BLOCK_COMMENT_INCOMPLETE
              || token == RustTokens.LINE_COMMENT)) {
        detectHighlightUrls(tokenizer.getTokenText(), tokenizer.offset, token, tokens);
        if (token == RustTokens.BLOCK_COMMENT_INCOMPLETE) {
          state = STATE_INCOMPLETE_BLOCK_COMMENT;
          break;
        }
        continue;
      }
      HighlightToken ht = new HighlightToken(token, tokenizer.offset);
      if (token == RustTokens.IDENTIFIER
          || token == RustTokens.RAW_IDENTIFIER
          || token == RustTokens.LIFETIME
          || token == RustTokens.PRIMITIVE_TYPE
          || token == RustTokens.MACRO) {
        ht.tokenText = tokenizer.getTokenText().toString();
      }
      tokens.add(ht);
      if (token == RustTokens.LBRACE || token == RustTokens.RBRACE) {
        st.hasBraces = true;
      }
      if (token == RustTokens.LPAREN || token == RustTokens.LBRACE || token == RustTokens.LBRACK) {
        st.bracketDepth++;
      } else if (token == RustTokens.RPAREN
          || token == RustTokens.RBRACE
          || token == RustTokens.RBRACK) {
        st.bracketDepth = Math.max(0, st.bracketDepth - 1);
      }
      if (token == RustTokens.IDENTIFIER) {
        st.addIdentifier(tokenizer.getTokenText());
      }
      if (token == RustTokens.BLOCK_COMMENT_INCOMPLETE) {
        state = STATE_INCOMPLETE_BLOCK_COMMENT;
        break;
      }
    }
    return state;
  }

  private void detectHighlightUrls(
      CharSequence tokenText, int offset, RustTokens token, List<HighlightToken> tokens) {
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
  public List<Span> generateSpansForLine(LineTokenizeResult<RustState, HighlightToken> lineResult) {
    var spans = new ArrayList<Span>();
    var tokens = lineResult.tokens;
    RustTokens previous = RustTokens.UNKNOWN;
    int depth = lineResult.state.startBracketDepth;
    boolean expectType = false;
    boolean expectFnName = false;
    boolean expectVarName = false;
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
        case KEYWORD_AS:
        case KEYWORD_BREAK:
        case KEYWORD_CONST:
        case KEYWORD_CONTINUE:
        case KEYWORD_CRATE:
        case KEYWORD_ELSE:
        case KEYWORD_ENUM:
        case KEYWORD_EXTERN:
        case KEYWORD_FALSE:
        case KEYWORD_FN:
        case KEYWORD_FOR:
        case KEYWORD_IF:
        case KEYWORD_IMPL:
        case KEYWORD_IN:
        case KEYWORD_LET:
        case KEYWORD_LOOP:
        case KEYWORD_MATCH:
        case KEYWORD_MOD:
        case KEYWORD_MOVE:
        case KEYWORD_MUT:
        case KEYWORD_PUB:
        case KEYWORD_RETURN:
        case KEYWORD_SELF:
        case KEYWORD_SELF_TYPE:
        case KEYWORD_STATIC:
        case KEYWORD_STRUCT:
        case KEYWORD_SUPER:
        case KEYWORD_TRAIT:
        case KEYWORD_TRUE:
        case KEYWORD_TYPE:
        case KEYWORD_UNSAFE:
        case KEYWORD_USE:
        case KEYWORD_WHERE:
        case KEYWORD_WHILE:
        case KEYWORD_ASYNC:
        case KEYWORD_AWAIT:
        case KEYWORD_DYN:
        case KEYWORD_REF:
        case KEYWORD_BOX:
        case KEYWORD_UNION:
        case KEYWORD_MACRO_RULES:
        case KEYWORD_DEFAULT:
        case KEYWORD_FINAL:
        case KEYWORD_OVERRIDE:
        case KEYWORD_PRIV:
        case KEYWORD_PUB_CRATE:
        case KEYWORD_PUB_SUPER:
        case KEYWORD_PUB_SELF:
          span =
              SpanFactory.obtain(
                  offset, TextStyle.makeStyle(GhostColorScheme.KEYWORD, 0, true, false, false));
          if (token == RustTokens.KEYWORD_FN) {
            expectFnName = true;
          } else if (token == RustTokens.KEYWORD_LET
              || token == RustTokens.KEYWORD_MUT
              || token == RustTokens.KEYWORD_CONST
              || token == RustTokens.KEYWORD_STATIC) {
            expectVarName = true;
          } else if (token == RustTokens.KEYWORD_STRUCT
              || token == RustTokens.KEYWORD_ENUM
              || token == RustTokens.KEYWORD_TRAIT
              || token == RustTokens.KEYWORD_TYPE
              || token == RustTokens.KEYWORD_IMPL
              || token == RustTokens.KEYWORD_DYN) {
            expectType = true;
          }
          break;
        case PRIMITIVE_TYPE:
          span =
              SpanFactory.obtain(
                  offset,
                  TextStyle.makeStyle(GhostColorScheme.COLORNEXTCHAR, 0, true, false, false));
          break;
        case MACRO:
          span =
              SpanFactory.obtain(
                  offset,
                  TextStyle.makeStyle(GhostColorScheme.FUNCTION_NAME, 0, true, false, false));
          break;
        case LIFETIME:
          span =
              SpanFactory.obtain(
                  offset,
                  TextStyle.makeStyle(GhostColorScheme.ATTRIBUTE_NAME, 0, true, false, false));
          break;
        case RAW_POINTER_MUT:
        case RAW_POINTER_CONST:
        case REFERENCE:
        case MUT_REF:
          span =
              SpanFactory.obtain(
                  offset, TextStyle.makeStyle(GhostColorScheme.KEYWORD, 0, true, false, false));
          break;
        case INTEGER_LITERAL:
        case FLOATING_LITERAL:
        case CHARACTER_LITERAL:
        case STRING_LITERAL:
        case BYTE_STRING_LITERAL:
        case BOOLEAN_LITERAL:
          span = SpanFactory.obtain(offset, TextStyle.makeStyle(GhostColorScheme.LITERAL, true));
          break;
        case IDENTIFIER:
        case RAW_IDENTIFIER:
          int color = GhostColorScheme.TEXT_NORMAL;
          String text = tokenRecord.tokenText != null ? tokenRecord.tokenText : "";
          // تشخیص ماکرو (با !)
          if (token == RustTokens.MACRO) {
            color = GhostColorScheme.FUNCTION_NAME;
          } else if (expectFnName) {
            color = GhostColorScheme.FUNCTION_NAME;
            expectFnName = false;
          } else if (expectVarName) {
            color = GhostColorScheme.IDENTIFIER_VAR;
            expectVarName = false;
          } else if (expectType) {
            color = GhostColorScheme.COLORNEXTCHAR;
            expectType = false;
          } else if (previous == RustTokens.PATH_SEP) {
            color = GhostColorScheme.COLORNEXTCHAR;
          } else if (previous == RustTokens.DOT) {
            color = GhostColorScheme.COLORNEXTDOT;
          } else if (previous == RustTokens.COLON) {
            color = GhostColorScheme.IDENTIFIER_NAME;
          } else if (previous == RustTokens.ASSIGN || previous == RustTokens.EQ) {
            color = GhostColorScheme.IDENTIFIER_VAR;
          } else {
            // نگاه به جلو
            int j = i + 1;
            RustTokens next = RustTokens.UNKNOWN;
            while (j < tokens.size()) {
              RustTokens n = tokens.get(j).token;
              if (n != RustTokens.WHITESPACE
                  && n != RustTokens.NEWLINE
                  && n != RustTokens.BLOCK_COMMENT_COMPLETE
                  && n != RustTokens.BLOCK_COMMENT_INCOMPLETE
                  && n != RustTokens.LINE_COMMENT) {
                next = n;
                break;
              }
              j++;
            }
            if (next == RustTokens.LPAREN) {
              color = GhostColorScheme.FUNCTION_NAME;
            } else if (next == RustTokens.DOT) {
              color = GhostColorScheme.COLORNEXTDOT;
            } else if (next == RustTokens.ASSIGN || next == RustTokens.EQ) {
              color = GhostColorScheme.IDENTIFIER_VAR;
            } else if (next == RustTokens.COLON) {
              color = GhostColorScheme.IDENTIFIER_NAME;
            } else if (next == RustTokens.PATH_SEP) {
              color = GhostColorScheme.COLORNEXTCHAR;
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
        case PATH_SEP:
        case RANGE:
        case RANGE_INCLUSIVE:
        case ELLIPSIS:
        case SEMICOLON:
        case COLON:
        case COMMA:
        case DOT:
        case AT:
        case NOT:
        case UNDERSCORE:
        case DOLLAR:
        case HASH:
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

    public RustTokens token;

    public int offset;

    public String url;

    public String tokenText;

    public HighlightToken(RustTokens token, int offset) {
      this.token = token;
      this.offset = offset;
    }

    public HighlightToken(RustTokens token, int offset, String url) {
      this.token = token;
      this.offset = offset;
      this.url = url;
    }
  }
}
