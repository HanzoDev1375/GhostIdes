/**
 * Comment by ghost ide
 *
 * @author: Ninjacoder
 */
package ir.hanzodev1375.ghostide.codeeditors.langs.perl;

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
 * Incremental analyze manager for Perl. Handles heredocs, POD blocks, sigil variable coloring,
 * interpolation coloring and rotating bracket colors.
 */
public class PerlIncrementalAnalyzeManager
    extends AsyncIncrementalAnalyzeManager<
        PerlState, PerlIncrementalAnalyzeManager.HighlightToken> {

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

  private final ThreadLocal<PerlTextTokenizer> tokenizerProvider = new ThreadLocal<>();

  protected IdentifierAutoComplete.SyncIdentifiers identifiers =
      new IdentifierAutoComplete.SyncIdentifiers();

  private synchronized PerlTextTokenizer obtainTokenizer() {
    var res = tokenizerProvider.get();
    if (res == null) {
      res = new PerlTextTokenizer("");
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
          state.state.state == PerlState.STATE_NORMAL
              || (state.state.state == PerlState.STATE_IN_HEREDOC && state.tokens.size() > 1)
              || (state.state.state == PerlState.STATE_IN_POD && state.tokens.size() > 1);
      if (state.state.hasBraces || checkForIdentifiers) {
        for (var tokenRecord : state.tokens) {
          var token = tokenRecord.token;
          int offset = tokenRecord.offset;
          if (token == PerlTokens.LBRACE) {
            CodeBlock block = new CodeBlock();
            block.startLine = i;
            block.startColumn = offset;
            stack.push(block);
          } else if (token == PerlTokens.RBRACE) {
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

  private static int getType(PerlTokens token) {
    if (token == PerlTokens.LBRACE || token == PerlTokens.RBRACE) return 3;
    if (token == PerlTokens.LBRACK || token == PerlTokens.RBRACK) return 2;
    if (token == PerlTokens.LPAREN || token == PerlTokens.RPAREN) return 1;
    return 0;
  }

  private static boolean isStart(PerlTokens token) {
    return token == PerlTokens.LBRACE
        || token == PerlTokens.LBRACK
        || token == PerlTokens.LPAREN;
  }

  @NonNull
  @Override
  public PerlState getInitialState() {
    return new PerlState();
  }

  @Override
  public boolean stateEquals(@NonNull PerlState state, @NonNull PerlState another) {
    return state.equals(another);
  }

  @Override
  public void onAddState(PerlState state) {
    if (state.identifiers != null) {
      for (String identifier : state.identifiers) {
        identifiers.identifierIncrease(identifier);
      }
    }
  }

  @Override
  public void onAbandonState(PerlState state) {
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
  public LineTokenizeResult<PerlState, HighlightToken> tokenizeLine(
      CharSequence line, PerlState state, int lineIndex) {
    var tokens = new ArrayList<HighlightToken>();
    int newState = PerlState.STATE_NORMAL;
    var stateObj = new PerlState();
    stateObj.delimiter = state.delimiter;
    stateObj.startBracketDepth = state.bracketDepth;
    stateObj.bracketDepth = state.bracketDepth;
    switch (state.state) {
      case PerlState.STATE_NORMAL:
        newState = tokenizeNormal(line, 0, tokens, stateObj);
        break;
      case PerlState.STATE_IN_HEREDOC:
        String trimmed = line.toString().trim();
        if (trimmed.equals(state.delimiter)) {
          tokens.add(new HighlightToken(PerlTokens.HEREDOC_END, 0));
          stateObj.delimiter = null;
          newState = PerlState.STATE_NORMAL;
        } else {
          tokens.add(new HighlightToken(PerlTokens.HEREDOC_BODY, 0));
          newState = PerlState.STATE_IN_HEREDOC;
        }
        break;
      case PerlState.STATE_IN_POD:
        String podTrimmed = line.toString().trim();
        if (podTrimmed.startsWith("=cut")) {
          tokens.add(new HighlightToken(PerlTokens.POD_END, 0));
          newState = PerlState.STATE_NORMAL;
        } else {
          tokens.add(new HighlightToken(PerlTokens.POD_BODY, 0));
          newState = PerlState.STATE_IN_POD;
        }
        break;
    }
    if (tokens.isEmpty()) {
      tokens.add(new HighlightToken(PerlTokens.UNKNOWN, 0));
    }
    stateObj.state = newState;
    return new LineTokenizeResult<>(stateObj, tokens);
  }

  private int tokenizeNormal(
      CharSequence text, int offset, List<HighlightToken> tokens, PerlState st) {
    var tokenizer = obtainTokenizer();
    tokenizer.reset(text);
    tokenizer.offset = offset;
    PerlTokens token;
    int state = PerlState.STATE_NORMAL;
    while ((token = tokenizer.nextToken()) != PerlTokens.EOF) {
      if (tokenizer.getTokenLength() < 1000
          && (token == PerlTokens.LINE_COMMENT || token == PerlTokens.STRING_CHUNK)) {
        detectHighlightUrls(tokenizer.getTokenText(), tokenizer.offset, token, tokens);
        continue;
      }
      HighlightToken ht = new HighlightToken(token, tokenizer.offset);
      if (token == PerlTokens.IDENTIFIER
          || token == PerlTokens.KEYWORD
          || token == PerlTokens.SIGIL_VAR) {
        ht.tokenText = tokenizer.getTokenText().toString();
      }
      tokens.add(ht);
      if (token == PerlTokens.HEREDOC_START) {
        st.delimiter = tokenizer.heredocDelimiter;
        st.state = PerlState.STATE_IN_HEREDOC;
        state = PerlState.STATE_IN_HEREDOC;
      }
      if (token == PerlTokens.POD_LINE) {
        st.state = PerlState.STATE_IN_POD;
        state = PerlState.STATE_IN_POD;
      }
      if (token == PerlTokens.LBRACE || token == PerlTokens.RBRACE) {
        st.hasBraces = true;
      }
      if (token == PerlTokens.LPAREN || token == PerlTokens.LBRACE || token == PerlTokens.LBRACK) {
        st.bracketDepth++;
      } else if (token == PerlTokens.RPAREN
          || token == PerlTokens.RBRACE
          || token == PerlTokens.RBRACK) {
        st.bracketDepth = Math.max(0, st.bracketDepth - 1);
      }
      if (token == PerlTokens.IDENTIFIER) {
        st.addIdentifier(tokenizer.getTokenText());
      }
    }
    return state;
  }

  private void detectHighlightUrls(
      CharSequence tokenText, int offset, PerlTokens token, List<HighlightToken> tokens) {
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
  public List<Span> generateSpansForLine(LineTokenizeResult<PerlState, HighlightToken> lineResult) {
    var spans = new ArrayList<Span>();
    var tokens = lineResult.tokens;
    PerlTokens previous = PerlTokens.UNKNOWN;
    int depth = lineResult.state.startBracketDepth;
    boolean expectSubName = false;
    boolean expectTypeName = false;
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
        case POD_LINE:
        case POD_BODY:
        case POD_END:
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
          if ("sub".equals(t)) expectSubName = true;
          if ("package".equals(t) || "use".equals(t) || "require".equals(t) || "no".equals(t))
            expectTypeName = true;
          break;
        case BUILTIN:
          span =
              SpanFactory.obtain(
                  offset,
                  TextStyle.makeStyle(GhostColorScheme.FUNCTION_NAME, 0, true, false, false));
          break;
        case SIGIL_VAR:
          span = SpanFactory.obtain(offset, TextStyle.makeStyle(GhostColorScheme.COLORNEXTLESS));
          break;
        case INTERPOLATION:
          span =
              SpanFactory.obtain(
                  offset,
                  TextStyle.makeStyle(GhostColorScheme.COLORNEXTBRAK, 0, true, false, false));
          break;
        case INTEGER_LITERAL:
        case FLOATING_LITERAL:
        case STRING_LITERAL:
        case STRING_CHUNK:
        case HEREDOC_START:
        case HEREDOC_BODY:
        case HEREDOC_END:
          span = SpanFactory.obtain(offset, TextStyle.makeStyle(GhostColorScheme.LITERAL, true));
          break;
        case IDENTIFIER:
          int color = GhostColorScheme.TEXT_NORMAL;
          if (expectSubName) {
            color = GhostColorScheme.FUNCTION_NAME;
            expectSubName = false;
          } else if (expectTypeName) {
            color = GhostColorScheme.COLORNEXTCHAR;
            expectTypeName = false;
          } else if (previous == PerlTokens.DOT
              || previous == PerlTokens.CONCAT
              || previous == PerlTokens.PACKAGE_SEP) {
            color = GhostColorScheme.COLORNEXTDOT;
          } else if (previous == PerlTokens.COLON) {
            color = GhostColorScheme.IDENTIFIER_NAME;
          } else if (previous == PerlTokens.ASSIGN) {
            color = GhostColorScheme.IDENTIFIER_VAR;
          } else {
            int j = i + 1;
            PerlTokens next = PerlTokens.UNKNOWN;
            while (j < tokens.size()) {
              PerlTokens n = tokens.get(j).token;
              if (n != PerlTokens.WHITESPACE
                  && n != PerlTokens.NEWLINE
                  && n != PerlTokens.LINE_COMMENT
                  && n != PerlTokens.POD_LINE
                  && n != PerlTokens.POD_BODY
                  && n != PerlTokens.POD_END) {
                next = n;
                break;
              }
              j++;
            }
            if (next == PerlTokens.LPAREN) {
              color = GhostColorScheme.FUNCTION_NAME;
            } else if (next == PerlTokens.ASSIGN || next == PerlTokens.FAT_COMMA) {
              color = GhostColorScheme.IDENTIFIER_VAR;
            } else if (next == PerlTokens.DOT || next == PerlTokens.CONCAT) {
              color = GhostColorScheme.COLORNEXTDOT;
            }
          }
          span = SpanFactory.obtain(offset, TextStyle.makeStyle(color));
          break;
        case PLUS:
        case MINUS:
        case STAR:
        case POWER:
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
        case EQ:
        case NOT_EQ:
        case SPACESHIP:
        case ASSIGN:
        case PLUS_ASSIGN:
        case MINUS_ASSIGN:
        case STAR_ASSIGN:
        case SLASH_ASSIGN:
        case PERCENT_ASSIGN:
        case POWER_ASSIGN:
        case CONCAT_ASSIGN:
        case AND_ASSIGN:
        case OR_ASSIGN:
        case XOR_ASSIGN:
        case MATCH:
        case NOT_MATCH:
        case ARROW:
        case FAT_COMMA:
        case DEFINED_OR:
        case DEFINED_OR_ASSIGN:
        case INC:
        case DEC:
        case LOGICAL_AND:
        case LOGICAL_OR:
        case NOT:
        case SHIFT_LEFT:
        case SHIFT_RIGHT:
        case SHIFT_LEFT_ASSIGN:
        case SHIFT_RIGHT_ASSIGN:
        case QUESTION:
        case DOLLAR:
        case AT:
        case HASH:
        case BACKTICK:
        case PACKAGE_SEP:
        case RANGE:
        case ELLIPSIS:
        case SEMICOLON:
        case COLON:
        case COMMA:
        case DOT:
        case CONCAT:
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
        case POD_LINE:
        case POD_BODY:
        case POD_END:
          break;
        default:
          previous = token;
      }
    }
    return spans;
  }

  public static class HighlightToken {

    public PerlTokens token;

    public int offset;

    public String url;

    public String tokenText;

    public HighlightToken(PerlTokens token, int offset) {
      this.token = token;
      this.offset = offset;
    }

    public HighlightToken(PerlTokens token, int offset, String url) {
      this.token = token;
      this.offset = offset;
      this.url = url;
    }
  }
}
