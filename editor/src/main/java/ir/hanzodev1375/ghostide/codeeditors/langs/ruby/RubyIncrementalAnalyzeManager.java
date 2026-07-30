package ir.hanzodev1375.ghostide.codeeditors.langs.ruby;

import android.os.Bundle;
import androidx.annotation.NonNull;
import io.github.rosemoe.sora.lang.analysis.AsyncIncrementalAnalyzeManager;
import io.github.rosemoe.sora.lang.completion.IdentifierAutoComplete;
import io.github.rosemoe.sora.lang.styling.CodeBlock;
import io.github.rosemoe.sora.lang.styling.Span;
import io.github.rosemoe.sora.lang.styling.SpanFactory;
import io.github.rosemoe.sora.lang.styling.TextStyle;
import io.github.rosemoe.sora.text.Content;
import io.github.rosemoe.sora.text.ContentReference;
import ir.hanzodev1375.ghostide.codeeditors.colorscheme.GhostColorScheme;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class RubyIncrementalAnalyzeManager
    extends AsyncIncrementalAnalyzeManager<
        RubyState, RubyIncrementalAnalyzeManager.HighlightToken> {

  private final ThreadLocal<RubyTextTokenizer> tokenizerProvider = new ThreadLocal<>();

  protected IdentifierAutoComplete.SyncIdentifiers identifiers =
      new IdentifierAutoComplete.SyncIdentifiers();

  public IdentifierAutoComplete.SyncIdentifiers getSyncIdentifiers() {
    return identifiers;
  }

  private synchronized RubyTextTokenizer obtainTokenizer() {
    var res = tokenizerProvider.get();
    if (res == null) {
      res = new RubyTextTokenizer("");
      tokenizerProvider.set(res);
    }
    return res;
  }

  private static boolean isBlockStart(RubyTokens token) {
    return token == RubyTokens.IF
        || token == RubyTokens.ELSIF
        || token == RubyTokens.ELSE
        || token == RubyTokens.UNLESS
        || token == RubyTokens.FOR
        || token == RubyTokens.DEF;
  }

  @Override
  public List<CodeBlock> computeBlocks(Content text, CodeBlockAnalyzeDelegate delegate) {
    var stack = new Stack<CodeBlock>();
    var blocks = new ArrayList<CodeBlock>();
    for (int i = 0; i < text.getLineCount() && delegate.isNotCancelled(); i++) {
      var state = getState(i);
      for (var tokenRecord : state.tokens) {
        var token = tokenRecord.token;
        int offset = tokenRecord.offset;
        if (isBlockStart(token)) {
          CodeBlock block = new CodeBlock();
          block.startLine = i;
          block.startColumn = offset;
          stack.push(block);
        } else if (token == RubyTokens.END) {
          if (!stack.isEmpty()) {
            CodeBlock block = stack.pop();
            block.endLine = i;
            block.endColumn = offset;
            if (block.startLine != block.endLine) {
              blocks.add(block);
            }
          }
        }
      }
    }
    return blocks;
  }

  @NonNull
  @Override
  public RubyState getInitialState() {
    return new RubyState();
  }

  @Override
  public boolean stateEquals(@NonNull RubyState state, @NonNull RubyState another) {
    return state.equals(another);
  }

  @Override
  public void onAddState(RubyState state) {
    if (state.identifiers != null) {
      for (String identifier : state.identifiers) {
        identifiers.identifierIncrease(identifier);
      }
    }
  }

  @Override
  public void onAbandonState(RubyState state) {
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
  public LineTokenizeResult<RubyState, HighlightToken> tokenizeLine(
      CharSequence line, RubyState state, int lineIndex) {
    var tokens = new ArrayList<HighlightToken>();
    var stateObj = new RubyState();
    var tokenizer = obtainTokenizer();

    if (state.insideBlockComment) {
      String text = line.toString();
      int idx = text.indexOf("=end");
      if (idx >= 0) {
        int end = idx + 4;
        tokens.add(new HighlightToken(RubyTokens.BLOCK_COMMENT, 0));
        if (end < line.length()) {
          tokenizer.reset(line.subSequence(end, line.length()));
          scanLine(tokenizer, tokens, stateObj, end);
        }
      } else {
        stateObj.insideBlockComment = true;
        tokens.add(new HighlightToken(RubyTokens.BLOCK_COMMENT, 0));
      }
      if (tokens.isEmpty()) tokens.add(new HighlightToken(RubyTokens.UNKNOWN, 0));
      return new LineTokenizeResult<>(stateObj, tokens);
    }

    tokenizer.reset(line);
    scanLine(tokenizer, tokens, stateObj, 0);
    if (tokens.isEmpty()) tokens.add(new HighlightToken(RubyTokens.UNKNOWN, 0));
    return new LineTokenizeResult<>(stateObj, tokens);
  }

  private void scanLine(
      RubyTextTokenizer tokenizer, List<HighlightToken> tokens, RubyState st, int baseOffset) {
    RubyTokens token;
    while ((token = tokenizer.nextToken()) != RubyTokens.EOF) {
      tokens.add(new HighlightToken(token, tokenizer.offset + baseOffset));
      if (token == RubyTokens.BLOCK_COMMENT) {
        String text = tokenizer.getTokenText().toString();
        if (!text.endsWith("=end")) {
          st.insideBlockComment = true;
        }
      }
      if (token == RubyTokens.ID) {
        st.addIdentifier(tokenizer.getTokenText());
      }
    }
  }

  @Override
  public List<Span> generateSpansForLine(
      LineTokenizeResult<RubyState, HighlightToken> lineResult) {
    var spans = new ArrayList<Span>();
    var tokens = lineResult.tokens;
    RubyTokens previous = RubyTokens.UNKNOWN;

    for (var tokenRecord : tokens) {
      var token = tokenRecord.token;
      int offset = tokenRecord.offset;
      Span span;
      switch (token) {
        case WHITESPACE:
        case NEWLINE:
          span = SpanFactory.obtain(offset, TextStyle.makeStyle(GhostColorScheme.TEXT_NORMAL));
          break;

        case LINE_COMMENT:
        case BLOCK_COMMENT:
          span =
              SpanFactory.obtain(
                  offset,
                  TextStyle.makeStyle(GhostColorScheme.COMMENT, 0, false, true, false, true));
          break;

        case REQUIRE:
        case END:
        case DEF:
        case RETURN:
        case PIR:
        case IF:
        case ELSE:
        case ELSIF:
        case UNLESS:
        case WHILE:
        case RETRY:
        case BREAK:
        case FOR:
        case AND:
        case OR:
        case NOT:
        case NIL:
        case TRUE:
        case FALSE:
        case LEFT_RBRACKET:
        case RIGHT_RBRACKET:
          span =
              SpanFactory.obtain(
                  offset, TextStyle.makeStyle(GhostColorScheme.KEYWORD, 0, true, false, false));
          break;

        case LITERAL:
        case INT:
        case FLOAT:
          span = SpanFactory.obtain(offset, TextStyle.makeStyle(GhostColorScheme.LITERAL, true));
          break;

        case PLUS:
        case MINUS:
        case MUL:
        case DIV:
        case MOD:
        case EXP:
        case EQUAL:
        case NOT_EQUAL:
        case GREATER:
        case LESS:
        case LESS_EQUAL:
        case GREATER_EQUAL:
        case ASSIGN:
        case PLUS_ASSIGN:
        case MINUS_ASSIGN:
        case MUL_ASSIGN:
        case DIV_ASSIGN:
        case MOD_ASSIGN:
        case EXP_ASSIGN:
        case BIT_AND:
        case BIT_OR:
        case BIT_XOR:
        case BIT_NOT:
        case BIT_SHL:
        case BIT_SHR:
          span = SpanFactory.obtain(offset, GhostColorScheme.OPERATOR);
          break;

        case ID:
          {
            int color = GhostColorScheme.TEXT_NORMAL;
            if (previous == RubyTokens.DEF) {
              color = GhostColorScheme.IDENTIFIER_VAR;
            } else if (previous == RubyTokens.UNLESS) {
              color = GhostColorScheme.ATTRIBUTE_NAME;
            } else if (previous == RubyTokens.IF || previous == RubyTokens.ELSIF) {
              color = GhostColorScheme.LITERAL;
            }
            span = SpanFactory.obtain(offset, TextStyle.makeStyle(color));
            break;
          }

        default:
          span = SpanFactory.obtain(offset, TextStyle.makeStyle(GhostColorScheme.TEXT_NORMAL));
          break;
      }

      spans.add(span);
      if (token != RubyTokens.WHITESPACE && token != RubyTokens.NEWLINE) {
        previous = token;
      }
    }
    return spans;
  }

  public static class HighlightToken {
    public RubyTokens token;
    public int offset;

    public HighlightToken(RubyTokens token, int offset) {
      this.token = token;
      this.offset = offset;
    }
  }
}
