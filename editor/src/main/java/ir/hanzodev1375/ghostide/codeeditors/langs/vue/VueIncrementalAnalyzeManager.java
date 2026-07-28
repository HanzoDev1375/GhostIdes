/**
 * Comment by ghost ide
 *
 * @author: Ninjacoder
 */
package ir.hanzodev1375.ghostide.codeeditors.langs.vue;

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

public class VueIncrementalAnalyzeManager
    extends AsyncIncrementalAnalyzeManager<VueState, VueIncrementalAnalyzeManager.HighlightToken> {

  private static final int COMMENT_NORMAL = 0;

  private static final int COMMENT_INCOMPLETE = 1;

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

  private final ThreadLocal<VueTextTokenizer> tokenizerProvider = new ThreadLocal<>();

  protected IdentifierAutoComplete.SyncIdentifiers identifiers =
      new IdentifierAutoComplete.SyncIdentifiers();

  private synchronized VueTextTokenizer obtainTokenizer() {
    var res = tokenizerProvider.get();
    if (res == null) {
      res = new VueTextTokenizer("");
      tokenizerProvider.set(res);
    }
    return res;
  }

  public VueState getLineState(int line) {
    return getState(line).state;
  }

  private static int modeForBlock(int block) {
    if (block == VueState.BLOCK_SCRIPT) return VueTextTokenizer.MODE_SCRIPT;
    if (block == VueState.BLOCK_STYLE) return VueTextTokenizer.MODE_STYLE;
    return VueTextTokenizer.MODE_MARKUP;
  }

  private static int findTerminator(CharSequence line, String terminator) {
    int n = line.length(), m = terminator.length();
    for (int i = 0; i <= n - m; i++) {
      boolean match = true;
      for (int j = 0; j < m; j++) {
        if (line.charAt(i + j) != terminator.charAt(j)) {
          match = false;
          break;
        }
      }
      if (match) return i + m;
    }
    return -1;
  }

  @Override
  public List<CodeBlock> computeBlocks(Content text, CodeBlockAnalyzeDelegate delegate) {
    var braceStack = new Stack<CodeBlock>();
    var tagStack = new Stack<CodeBlock>();
    var blocks = new ArrayList<CodeBlock>();
    var brackets = new SimpleBracketsCollector();
    var bracketsStack = new Stack<Long>();
    for (int i = 0; i < text.getLineCount() && delegate.isNotCancelled(); i++) {
      var state = getState(i);
      for (int i1 = 0; i1 < state.tokens.size(); i1++) {
        var tokenRecord = state.tokens.get(i1);
        var token = tokenRecord.token;
        int offset = tokenRecord.offset;
        if (token == VueTokens.LBRACE) {
          CodeBlock block = new CodeBlock();
          block.startLine = i;
          block.startColumn = offset;
          braceStack.push(block);
        } else if (token == VueTokens.RBRACE) {
          if (!braceStack.isEmpty()) {
            CodeBlock block = braceStack.pop();
            block.endLine = i;
            block.endColumn = offset;
            if (block.startLine != block.endLine) blocks.add(block);
          }
        } else if (token == VueTokens.TAG_OPEN_START) {
          CodeBlock block = new CodeBlock();
          block.startLine = i;
          block.startColumn = offset;
          tagStack.push(block);
        } else if (token == VueTokens.TAG_SELF_CLOSE) {
          if (!tagStack.isEmpty()) tagStack.pop();
        } else if (token == VueTokens.TAG_CLOSE_START) {
          if (!tagStack.isEmpty()) {
            CodeBlock block = tagStack.pop();
            block.endLine = i;
            block.endColumn = offset;
            if (block.startLine != block.endLine) blocks.add(block);
          }
        }
        var type = getType(token);
        if (type > 0) {
          if (isStart(token)) {
            bracketsStack.push(IntPair.pack(type, text.getCharIndex(i, offset)));
          } else if (!bracketsStack.isEmpty()) {
            var record = bracketsStack.pop();
            var typeRecord = IntPair.getFirst(record);
            if (typeRecord == type) {
              brackets.add(IntPair.getSecond(record), text.getCharIndex(i, offset));
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

  private static int getType(VueTokens token) {
    if (token == VueTokens.LBRACE || token == VueTokens.RBRACE) return 3;
    if (token == VueTokens.LBRACK || token == VueTokens.RBRACK) return 2;
    if (token == VueTokens.LPAREN || token == VueTokens.RPAREN) return 1;
    return 0;
  }

  private static boolean isStart(VueTokens token) {
    return token == VueTokens.LBRACE || token == VueTokens.LBRACK || token == VueTokens.LPAREN;
  }

  @NonNull
  @Override
  public VueState getInitialState() {
    return new VueState();
  }

  @Override
  public boolean stateEquals(@NonNull VueState state, @NonNull VueState another) {
    return state.equals(another);
  }

  @Override
  public void onAddState(VueState state) {
    if (state.identifiers != null) {
      for (String identifier : state.identifiers) {
        identifiers.identifierIncrease(identifier);
      }
    }
  }

  @Override
  public void onAbandonState(VueState state) {
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
  public LineTokenizeResult<VueState, HighlightToken> tokenizeLine(
      CharSequence line, VueState state, int lineIndex) {
    var tokens = new ArrayList<HighlightToken>();
    var stateObj = new VueState();
    stateObj.block = state.block;
    stateObj.startBracketDepth = state.bracketDepth;
    stateObj.bracketDepth = state.bracketDepth;
    stateObj.inTag = state.inTag;
    stateObj.tagNamePending = state.tagNamePending;
    stateObj.inMustache = state.inMustache;
    if (state.commentState == COMMENT_INCOMPLETE) {
      boolean htmlComment =
          state.block == VueState.BLOCK_TEMPLATE || state.block == VueState.BLOCK_OUTSIDE;
      String terminator = htmlComment ? "-->" : "*/";
      VueTokens completeTok =
          htmlComment ? VueTokens.HTML_COMMENT_COMPLETE : VueTokens.BLOCK_COMMENT_COMPLETE;
      VueTokens incompleteTok =
          htmlComment ? VueTokens.HTML_COMMENT_INCOMPLETE : VueTokens.BLOCK_COMMENT_INCOMPLETE;
      int end = findTerminator(line, terminator);
      if (end >= 0) {
        detectHighlightUrls(line.subSequence(0, end), 0, completeTok, tokens);
        stateObj.commentState = COMMENT_NORMAL;
        tokenizeBlockContent(line, end, tokens, stateObj);
      } else {
        detectHighlightUrls(line, 0, incompleteTok, tokens);
        stateObj.commentState = COMMENT_INCOMPLETE;
      }
    } else {
      stateObj.commentState = COMMENT_NORMAL;
      tokenizeBlockContent(line, 0, tokens, stateObj);
    }
    if (tokens.isEmpty()) {
      tokens.add(new HighlightToken(VueTokens.UNKNOWN, 0));
    }
    return new LineTokenizeResult<>(stateObj, tokens);
  }

  private void tokenizeBlockContent(
      CharSequence line, int startOffset, List<HighlightToken> tokens, VueState st) {
    var tokenizer = obtainTokenizer();
    tokenizer.reset(line);
    tokenizer.offset = startOffset;
    tokenizer.inTag = st.inTag;
    tokenizer.tagNamePending = st.tagNamePending;
    tokenizer.inMustache = st.inMustache;
    tokenizer.setMode(modeForBlock(st.block));
    VueTokens token;
    boolean sawOpenTagStart = false, sawCloseTagStart = false;
    String pendingTagName = null;
    while ((token = tokenizer.nextToken()) != VueTokens.EOF) {
      if (tokenizer.getTokenLength() < 1000
          && (token == VueTokens.STRING_LITERAL
              || token == VueTokens.TEMPLATE_LITERAL
              || token == VueTokens.ATTR_VALUE
              || token == VueTokens.LINE_COMMENT
              || token == VueTokens.BLOCK_COMMENT_COMPLETE
              || token == VueTokens.HTML_COMMENT_COMPLETE)) {
        detectHighlightUrls(tokenizer.getTokenText(), tokenizer.offset, token, tokens);
        continue;
      }
      if (token == VueTokens.BLOCK_COMMENT_INCOMPLETE
          || token == VueTokens.HTML_COMMENT_INCOMPLETE) {
        detectHighlightUrls(tokenizer.getTokenText(), tokenizer.offset, token, tokens);
        st.commentState = COMMENT_INCOMPLETE;
        break;
      }
      tokens.add(new HighlightToken(token, tokenizer.offset));
      if (token == VueTokens.LBRACE || token == VueTokens.LPAREN || token == VueTokens.LBRACK) {
        st.bracketDepth++;
      } else if (token == VueTokens.RBRACE
          || token == VueTokens.RPAREN
          || token == VueTokens.RBRACK) {
        st.bracketDepth = Math.max(0, st.bracketDepth - 1);
      }
      if (token == VueTokens.IDENTIFIER && st.block == VueState.BLOCK_SCRIPT) {
        st.addIdentifier(tokenizer.getTokenText());
      }
      if (token == VueTokens.TAG_OPEN_START) {
        sawOpenTagStart = true;
        sawCloseTagStart = false;
        pendingTagName = null;
      } else if (token == VueTokens.TAG_CLOSE_START) {
        sawCloseTagStart = true;
        sawOpenTagStart = false;
        pendingTagName = null;
      } else if (token == VueTokens.TAG_NAME) {
        pendingTagName = tokenizer.getTokenText().toString().toLowerCase();
      } else if (token == VueTokens.TAG_END) {
        if (sawOpenTagStart && pendingTagName != null && st.block == VueState.BLOCK_OUTSIDE) {
          if (pendingTagName.equals("template")) {
            st.block = VueState.BLOCK_TEMPLATE;
            st.bracketDepth = 0;
          } else if (pendingTagName.equals("script")) {
            st.block = VueState.BLOCK_SCRIPT;
            st.bracketDepth = 0;
          } else if (pendingTagName.equals("style")) {
            st.block = VueState.BLOCK_STYLE;
            st.bracketDepth = 0;
          }
          tokenizer.setMode(modeForBlock(st.block));
        } else if (sawCloseTagStart && pendingTagName != null) {
          boolean matches =
              (st.block == VueState.BLOCK_TEMPLATE && pendingTagName.equals("template"))
                  || (st.block == VueState.BLOCK_SCRIPT && pendingTagName.equals("script"))
                  || (st.block == VueState.BLOCK_STYLE && pendingTagName.equals("style"));
          if (matches) {
            st.block = VueState.BLOCK_OUTSIDE;
            st.bracketDepth = 0;
            tokenizer.setMode(modeForBlock(st.block));
          }
        }
        sawOpenTagStart = false;
        sawCloseTagStart = false;
        pendingTagName = null;
      } else if (token == VueTokens.TAG_SELF_CLOSE) {
        sawOpenTagStart = false;
        sawCloseTagStart = false;
        pendingTagName = null;
      }
    }
    st.inTag = tokenizer.inTag;
    st.tagNamePending = tokenizer.tagNamePending;
    st.inMustache = tokenizer.inMustache;
  }

  private void detectHighlightUrls(
      CharSequence tokenText, int offset, VueTokens token, List<HighlightToken> tokens) {
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
  public List<Span> generateSpansForLine(LineTokenizeResult<VueState, HighlightToken> lineResult) {
    var spans = new ArrayList<Span>();
    var tokens = lineResult.tokens;
    int blockCtx = lineResult.state.block;
    VueTokens previous = VueTokens.UNKNOWN;
    int depth = lineResult.state.startBracketDepth;
    boolean inValue = false;
    for (int i = 0; i < tokens.size(); i++) {
      var tokenRecord = tokens.get(i);
      var token = tokenRecord.token;
      int offset = tokenRecord.offset;
      Span span;
      switch (token) {
        case WHITESPACE:
        case NEWLINE:
        case TEXT_CONTENT:
          span = SpanFactory.obtain(offset, TextStyle.makeStyle(GhostColorScheme.TEXT_NORMAL));
          break;
        case LINE_COMMENT:
        case BLOCK_COMMENT_COMPLETE:
        case BLOCK_COMMENT_INCOMPLETE:
        case HTML_COMMENT_COMPLETE:
        case HTML_COMMENT_INCOMPLETE:
          span =
              SpanFactory.obtain(
                  offset,
                  TextStyle.makeStyle(GhostColorScheme.COMMENT, 0, false, true, false, true));
          break;
        case KEYWORD:
        case KW_FUNCTION:
        case KW_CONST:
        case KW_LET:
        case KW_VAR:
        case KW_CLASS:
        case KW_NEW:
        case KW_RETURN:
        case KW_IMPORT:
        case KW_EXPORT:
        case KW_FROM:
        case KW_DEFAULT:
          span =
              SpanFactory.obtain(
                  offset, TextStyle.makeStyle(GhostColorScheme.KEYWORD, 0, true, false, false));
          break;
        case NUMBER_LITERAL:
        case STRING_LITERAL:
        case TEMPLATE_LITERAL:
        case ATTR_VALUE:
          span = SpanFactory.obtain(offset, TextStyle.makeStyle(GhostColorScheme.LITERAL, true));
          break;
        case TAG_NAME:
          span = SpanFactory.obtain(offset, TextStyle.makeStyle(GhostColorScheme.FUNCTION_NAME));
          break;
        case ATTR_NAME:
          span = SpanFactory.obtain(offset, TextStyle.makeStyle(GhostColorScheme.ATTRIBUTE_NAME));
          break;
        case DIRECTIVE_NAME:
        case CSS_AT_RULE:
          span = SpanFactory.obtain(offset, TextStyle.makeStyle(GhostColorScheme.ANNOTATION));
          break;
        case IDENTIFIER:
          int color;
          if (blockCtx == VueState.BLOCK_STYLE) {
            if (depth % 2 == 1) {
              color = inValue ? GhostColorScheme.LITERAL : GhostColorScheme.ATTRIBUTE_NAME;
            } else {
              color = GhostColorScheme.FUNCTION_NAME;
            }
          } else if (previous == VueTokens.DOT) {
            color = GhostColorScheme.COLORNEXTDOT;
          } else if (previous == VueTokens.KW_CONST
              || previous == VueTokens.KW_LET
              || previous == VueTokens.KW_VAR) {
            color = GhostColorScheme.COLORNEXTBRAK;
          } else if (previous == VueTokens.KW_FUNCTION
              || previous == VueTokens.KW_CLASS
              || previous == VueTokens.KW_NEW) {
            color = GhostColorScheme.ATTRIBUTE_NAME;
          } else if (previous == VueTokens.KW_RETURN) {
            color = GhostColorScheme.FUNCTION_NAME;
          } else {
            int j = i + 1;
            var next = VueTokens.UNKNOWN;
            while (j < tokens.size()) {
              var n = tokens.get(j).token;
              if (n != VueTokens.WHITESPACE
                  && n != VueTokens.NEWLINE
                  && n != VueTokens.LINE_COMMENT
                  && n != VueTokens.BLOCK_COMMENT_COMPLETE
                  && n != VueTokens.BLOCK_COMMENT_INCOMPLETE) {
                next = n;
                break;
              }
              j++;
            }
            if (next == VueTokens.LPAREN) color = GhostColorScheme.FUNCTION_NAME;
            else if (next == VueTokens.ASSIGN) color = GhostColorScheme.ATTRIBUTE_NAME;
            else color = GhostColorScheme.TEXT_NORMAL;
          }
          span = SpanFactory.obtain(offset, TextStyle.makeStyle(color));
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
        case COLON:
          if (blockCtx == VueState.BLOCK_STYLE && depth % 2 == 1) inValue = true;
          span = SpanFactory.obtain(offset, GhostColorScheme.OPERATOR);
          break;
        case SEMICOLON:
          inValue = false;
          span = SpanFactory.obtain(offset, GhostColorScheme.OPERATOR);
          break;
        default:
          span = SpanFactory.obtain(offset, GhostColorScheme.OPERATOR);
      }
      switch (token) {
        case WHITESPACE:
        case NEWLINE:
        case TEXT_CONTENT:
        case LINE_COMMENT:
        case BLOCK_COMMENT_COMPLETE:
        case BLOCK_COMMENT_INCOMPLETE:
        case HTML_COMMENT_COMPLETE:
        case HTML_COMMENT_INCOMPLETE:
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

    public VueTokens token;
    public int offset;
    public String url;

    public HighlightToken(VueTokens token, int offset) {
      this.token = token;
      this.offset = offset;
    }

    public HighlightToken(VueTokens token, int offset, String url) {
      this.token = token;
      this.offset = offset;
      this.url = url;
    }
  }
}
