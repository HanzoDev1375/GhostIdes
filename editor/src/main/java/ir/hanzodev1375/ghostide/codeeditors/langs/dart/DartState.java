/**
 * Comment by ghost ide
 *
 * @author: Ninjacoder
 */
package ir.hanzodev1375.ghostide.codeeditors.langs.dart;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DartState {

  // 0 = normal, 1 = incomplete block comment 
  public int state = 0;

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
    DartState that = (DartState) o;
    return state == that.state && hasBraces == that.hasBraces && bracketDepth == that.bracketDepth;
  }

  @Override
  public int hashCode() {
    return Objects.hash(state, hasBraces, bracketDepth);
  }
}
