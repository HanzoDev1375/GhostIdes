package ir.hanzodev1375.ghostide.codeeditors.langs.kotlin;

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

public class KotlinIncrementalAnalyzeManager
    extends AsyncIncrementalAnalyzeManager<State, KotlinIncrementalAnalyzeManager.HighlightToken> {

  private static final int STATE_NORMAL = 0;
  private static final int STATE_INCOMPLETE_COMMENT = 1;
  private static final int STATE_INCOMPLETE_STRING = 2;

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

  private final ThreadLocal<KotlinTextTokenizer> tokenizerProvider = new ThreadLocal<>();
  protected IdentifierAutoComplete.SyncIdentifiers identifiers =
      new IdentifierAutoComplete.SyncIdentifiers();

  private synchronized KotlinTextTokenizer obtainTokenizer() {
    var res = tokenizerProvider.get();
    if (res == null) {
      res = new KotlinTextTokenizer("");
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
              || (state.state.state == STATE_INCOMPLETE_COMMENT && state.tokens.size() > 1);
      if (state.state.hasBraces || checkForIdentifiers) {
        for (int i1 = 0; i1 < state.tokens.size(); i1++) {
          var tokenRecord = state.tokens.get(i1);
          var token = tokenRecord.token;
          if (token == Tokens.LBRACE && state.state.hasBraces) {
            var offset = tokenRecord.offset;
            if (stack.isEmpty()) {
              if (currSwitch > maxSwitch) {
                maxSwitch = currSwitch;
              }
              currSwitch = 0;
            }
            currSwitch++;
            CodeBlock block = new CodeBlock();
            block.startLine = i;
            block.startColumn = offset;
            stack.push(block);
          } else if (token == Tokens.RBRACE && state.state.hasBraces) {
            var offset = tokenRecord.offset;
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
              bracketsStack.push(IntPair.pack(type, text.getCharIndex(i, tokenRecord.offset)));
            } else {
              if (!bracketsStack.isEmpty()) {
                var record = bracketsStack.pop();
                var typeRecord = IntPair.getFirst(record);
                if (typeRecord == type) {
                  brackets.add(IntPair.getSecond(record), text.getCharIndex(i, tokenRecord.offset));
                } else if (type == 3) {
                  while (!bracketsStack.isEmpty()) {
                    record = bracketsStack.pop();
                    if (IntPair.getFirst(record) == 3) {
                      brackets.add(
                          IntPair.getSecond(record), text.getCharIndex(i, tokenRecord.offset));
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

  private static int getType(Tokens token) {
    if (token == Tokens.LBRACE || token == Tokens.RBRACE) return 3;
    if (token == Tokens.LBRACK || token == Tokens.RBRACK) return 2;
    if (token == Tokens.LPAREN || token == Tokens.RPAREN) return 1;
    return 0;
  }

  private static boolean isStart(Tokens token) {
    return token == Tokens.LBRACE || token == Tokens.LBRACK || token == Tokens.LPAREN;
  }

  @Override
  @NonNull
  public State getInitialState() {
    return new State();
  }

  @Override
  public boolean stateEquals(@NonNull State state, @NonNull State another) {
    return state.equals(another);
  }

  @Override
  public void onAddState(State state) {
    if (state.identifiers != null) {
      for (String identifier : state.identifiers) {
        identifiers.identifierIncrease(identifier);
      }
    }
  }

  @Override
  public void onAbandonState(State state) {
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
  public LineTokenizeResult<State, HighlightToken> tokenizeLine(
      CharSequence line, State state, int lineIndex) {
    var tokens = new ArrayList<HighlightToken>();
    int newState = 0;
    var stateObj = new State();
    stateObj.startBracketDepth = state.bracketDepth;
    stateObj.bracketDepth = state.bracketDepth;
    if (state.state == STATE_NORMAL) {
      newState = tokenizeNormal(line, 0, tokens, stateObj);
    } else if (state.state == STATE_INCOMPLETE_COMMENT) {
      var res = tryFillIncompleteComment(line, tokens);
      newState = IntPair.getFirst(res);
      if (newState == STATE_NORMAL) {
        newState = tokenizeNormal(line, IntPair.getSecond(res), tokens, stateObj);
      } else {
        newState = STATE_INCOMPLETE_COMMENT;
      }
    } else if (state.state == STATE_INCOMPLETE_STRING) {
      newState = tokenizeStringContinuation(line, tokens, stateObj);
    }
    if (tokens.isEmpty()) {
      tokens.add(new HighlightToken(Tokens.UNKNOWN, 0));
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
        detectHighlightUrls(line.subSequence(0, offset), 0, Tokens.LONG_COMMENT_COMPLETE, tokens);
      } else {
        tokens.add(new HighlightToken(Tokens.LONG_COMMENT_COMPLETE, 0));
      }
      return IntPair.pack(STATE_NORMAL, offset);
    }
    if (offset < 1000) {
      detectHighlightUrls(line.subSequence(0, offset), 0, Tokens.LONG_COMMENT_INCOMPLETE, tokens);
    } else {
      tokens.add(new HighlightToken(Tokens.LONG_COMMENT_INCOMPLETE, 0));
    }
    return IntPair.pack(STATE_INCOMPLETE_COMMENT, offset);
  }

  private int tokenizeNormal(CharSequence text, int offset, List<HighlightToken> tokens, State st) {
    var tokenizer = obtainTokenizer();
    tokenizer.reset(text);
    tokenizer.offset = offset;
    return consumeTokens(tokenizer, tokens, st);
  }

  private int tokenizeStringContinuation(CharSequence text, List<HighlightToken> tokens, State st) {
    var tokenizer = obtainTokenizer();
    tokenizer.resumeStringContinuation(text, true);
    return consumeTokens(tokenizer, tokens, st);
  }

  private int consumeTokens(KotlinTextTokenizer tokenizer, List<HighlightToken> tokens, State st) {
    Tokens token;
    int state = STATE_NORMAL;
    while ((token = tokenizer.nextToken()) != Tokens.EOF) {
      if (tokenizer.getTokenLength() < 1000
          && (token == Tokens.STRING_LITERAL
              || token == Tokens.STRING_INCOMPLETE
              || token == Tokens.LONG_COMMENT_COMPLETE
              || token == Tokens.LONG_COMMENT_INCOMPLETE
              || token == Tokens.LINE_COMMENT)) {
        detectHighlightUrls(tokenizer.getTokenText(), tokenizer.offset, token, tokens);
        if (token == Tokens.LONG_COMMENT_INCOMPLETE) {
          state = STATE_INCOMPLETE_COMMENT;
          break;
        }
        continue;
      }
      tokens.add(new HighlightToken(token, tokenizer.offset));
      if (token == Tokens.LBRACE || token == Tokens.RBRACE) {
        st.hasBraces = true;
      }
      if (token == Tokens.LPAREN
          || token == Tokens.LBRACE
          || token == Tokens.LBRACK
          || token == Tokens.STRING_TEMPLATE_EXPR_START) {
        st.bracketDepth++;
      } else if (token == Tokens.RPAREN
          || token == Tokens.RBRACE
          || token == Tokens.RBRACK
          || token == Tokens.STRING_TEMPLATE_EXPR_END) {
        st.bracketDepth = Math.max(0, st.bracketDepth - 1);
      }
      if (token == Tokens.IDENTIFIER) {
        st.addIdentifier(tokenizer.getTokenText());
      }
      if (token == Tokens.LONG_COMMENT_INCOMPLETE) {
        state = STATE_INCOMPLETE_COMMENT;
        break;
      }
    }
    if (state == STATE_NORMAL && tokenizer.getMode() == 1) {
      state = STATE_INCOMPLETE_STRING;
    }
    return state;
  }

  private void detectHighlightUrls(
      CharSequence tokenText, int offset, Tokens token, List<HighlightToken> tokens) {
    var matcher = URL_PATTERN.matcher(tokenText);
    var index = 0;
    while (index < tokenText.length() && matcher.find(index)) {
      var start = matcher.start();
      var end = matcher.end();
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
  public List<Span> generateSpansForLine(LineTokenizeResult<State, HighlightToken> lineResult) {
    var spans = new ArrayList<Span>();
    var tokens = lineResult.tokens;
    Tokens previous = Tokens.UNKNOWN;
    boolean classNamePrevious = false;
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
        case CHARACTER_LITERAL:
        case FLOATING_POINT_LITERAL:
        case INTEGER_LITERAL:
        case STRING_LITERAL:
        case STRING_INCOMPLETE:
        case NULL_LITERAL:
          classNamePrevious = false;
          span = SpanFactory.obtain(offset, TextStyle.makeStyle(GhostColorScheme.LITERAL, true));
          break;
        case VAL:
        case VAR:
        case FUN:
        case CLASS:
        case INTERFACE:
        case OBJECT:
        case ENUM:
        case SEALED:
        case TYPEALIAS:
        case AS:
          classNamePrevious = true;
          span =
              SpanFactory.obtain(
                  offset, TextStyle.makeStyle(GhostColorScheme.KEYWORD, 0, true, false, false));
          break;
        case BREAK:
        case CONTINUE:
        case DO:
        case ELSE:
        case FALSE:
        case FINAL:
        case FINALLY:
        case FOR:
        case IF:
        case IMPORT:
        case IN:
        case IS:
        case PACKAGE:
        case PRIVATE:
        case PROTECTED:
        case PUBLIC:
        case RETURN:
        case SUPER:
        case THIS:
        case THROW:
        case THROWS:
        case TRUE:
        case TRY:
        case WHEN:
        case WHILE:
        case OPEN:
        case INLINE:
        case DATA:
          classNamePrevious = false;
          span =
              SpanFactory.obtain(
                  offset, TextStyle.makeStyle(GhostColorScheme.KEYWORD, 0, true, false, false));
          break;
        case LINE_COMMENT:
        case LONG_COMMENT_COMPLETE:
        case LONG_COMMENT_INCOMPLETE:
          span =
              SpanFactory.obtain(
                  offset,
                  TextStyle.makeStyle(GhostColorScheme.COMMENT, 0, false, true, false, true));
          break;
        case IDENTIFIER:
          int type = GhostColorScheme.TEXT_NORMAL;
          if (previous == Tokens.AT) {
            type = GhostColorScheme.ANNOTATION;
          } else if (previous == Tokens.DOT) {
            type = GhostColorScheme.COLORNEXTDOT;
          } else if (previous == Tokens.VAL || previous == Tokens.VAR) {
            type = GhostColorScheme.FUNCTION_NAME;
          } else if (previous == Tokens.CLASS) {
            type = GhostColorScheme.COLORUPPERCASE;
          } else if (previous == Tokens.COLON) {
            type = GhostColorScheme.ATTRIBUTE_NAME;
          } else if (previous == Tokens.FUN || previous == Tokens.AS) {
            type = GhostColorScheme.COLORNEXTCHAR;
          } else {
            int j = i + 1;
            var next = Tokens.UNKNOWN;
            label:
            while (j < tokens.size()) {
              next = tokens.get(j).token;
              switch (next) {
                case WHITESPACE:
                case NEWLINE:
                case LONG_COMMENT_INCOMPLETE:
                case LONG_COMMENT_COMPLETE:
                case LINE_COMMENT:
                  break;
                default:
                  break label;
              }
              j++;
            }
            if (next == Tokens.LPAREN) {
              type = GhostColorScheme.FUNCTION_NAME;
            } else {
              classNamePrevious = true;
            }
            if (next == Tokens.DOT) {
              type = GhostColorScheme.COLORNEXTDOT;
            }
            if (next == Tokens.VAL || next == Tokens.VAR) type = GhostColorScheme.FUNCTION_NAME;
          }

          span = SpanFactory.obtain(offset, TextStyle.makeStyle(type));
          break;
        case LPAREN:
        case LBRACE:
        case LBRACK:
        case STRING_TEMPLATE_EXPR_START:
          classNamePrevious = false;
          span = SpanFactory.obtain(offset, bracketStyle(depth));
          depth++;
          break;
        case RPAREN:
        case RBRACE:
        case RBRACK:
        case STRING_TEMPLATE_EXPR_END:
          classNamePrevious = false;
          depth = Math.max(0, depth - 1);
          span = SpanFactory.obtain(offset, bracketStyle(depth));
          break;
        case STRING_TEMPLATE_DOLLAR:
          classNamePrevious = false;
          span = SpanFactory.obtain(offset, TextStyle.makeStyle(GhostColorScheme.LITERAL, true));
          break;
        default:
          classNamePrevious = false;
          span = SpanFactory.obtain(offset, GhostColorScheme.OPERATOR);
      }
      switch (token) {
        case LINE_COMMENT:
        case LONG_COMMENT_COMPLETE:
        case LONG_COMMENT_INCOMPLETE:
        case WHITESPACE:
        case NEWLINE:
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
    public Tokens token;
    public int offset;
    public String url;

    public HighlightToken(Tokens token, int offset) {
      this.token = token;
      this.offset = offset;
    }

    public HighlightToken(Tokens token, int offset, String url) {
      this.token = token;
      this.offset = offset;
      this.url = url;
    }
  }
}
