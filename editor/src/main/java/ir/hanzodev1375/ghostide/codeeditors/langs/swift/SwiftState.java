/**
 * Comment by ghost ide
 *
 * @author: Ninjacoder
 */
package ir.hanzodev1375.ghostide.codeeditors.langs.swift;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Incremental analysis state for Swift. Carries the cross-line mode (normal, inside a nested block
 * comment with its pending depth, or inside a triple-quoted string) plus bracket depths and user
 * identifiers collected on the way.
 */
public class SwiftState {

  public static final int STATE_NORMAL = 0;

  public static final int STATE_INCOMPLETE_BLOCK_COMMENT = 1;

  public static final int STATE_IN_TRIPLE_STRING = 2;

  /** One of {@code STATE_*} constants. */
  public int state = STATE_NORMAL;

  /** Remaining nesting depth while inside a multi-line block comment. */
  public int commentDepth = 0;

  public boolean hasBraces = false;

  public int startBracketDepth = 0;

  public int bracketDepth = 0;

  public List<String> identifiers = null;

  public void addIdentifier(CharSequence idt) {
    if (identifiers == null) identifiers = new ArrayList<>();
    identifiers.add(idt.toString());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SwiftState that = (SwiftState) o;
    return state == that.state
        && commentDepth == that.commentDepth
        && hasBraces == that.hasBraces
        && bracketDepth == that.bracketDepth;
  }

  @Override
  public int hashCode() {
    return Objects.hash(state, commentDepth, hasBraces, bracketDepth);
  }
}
