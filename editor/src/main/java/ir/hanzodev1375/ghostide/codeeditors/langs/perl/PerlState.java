/**
 * Comment by ghost ide
 *
 * @author: Ninjacoder
 */
package ir.hanzodev1375.ghostide.codeeditors.langs.perl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Incremental analysis state for Perl. Carries the cross-line mode (normal, inside a heredoc with
 * its terminator label, or inside a POD block) plus bracket depths and user identifiers.
 */
public class PerlState {

  public static final int STATE_NORMAL = 0;

  public static final int STATE_IN_HEREDOC = 1;

  public static final int STATE_IN_POD = 2;

  /** One of {@code STATE_*} constants. */
  public int state = STATE_NORMAL;

  /** Terminator label while inside a heredoc. */
  public String delimiter = null;

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
    PerlState that = (PerlState) o;
    return state == that.state
        && hasBraces == that.hasBraces
        && bracketDepth == that.bracketDepth
        && Objects.equals(delimiter, that.delimiter);
  }

  @Override
  public int hashCode() {
    return Objects.hash(state, delimiter, hasBraces, bracketDepth);
  }
}
