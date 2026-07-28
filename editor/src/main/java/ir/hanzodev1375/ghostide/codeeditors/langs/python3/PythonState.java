/**
 * Comment by ghost ide
 *
 * @author: Ninjacoder
 */
package ir.hanzodev1375.ghostide.codeeditors.langs.python3;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PythonState {

  // 0 = normal, 1 = inside an unterminated triple-quoted string
  public int state = 0;
  public char pendingQuoteChar = 0;
  public boolean pendingIsFString = false;
  public boolean pendingIsRaw = false;
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
    PythonState that = (PythonState) o;
    return state == that.state
        && pendingQuoteChar == that.pendingQuoteChar
        && pendingIsFString == that.pendingIsFString
        && pendingIsRaw == that.pendingIsRaw
        && hasBraces == that.hasBraces
        && bracketDepth == that.bracketDepth;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        state, pendingQuoteChar, pendingIsFString, pendingIsRaw, hasBraces, bracketDepth);
  }
}
