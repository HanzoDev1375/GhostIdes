/**
 * Comment by ghost ide
 *
 * @author: Ninjacoder
 */
package ir.hanzodev1375.ghostide.codeeditors.langs.vue;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class VueState {

  public static final int BLOCK_OUTSIDE = 0;
  public static final int BLOCK_TEMPLATE = 1;
  public static final int BLOCK_SCRIPT = 2;
  public static final int BLOCK_STYLE = 3;

  public int block = BLOCK_OUTSIDE;

  public int commentState = 0;

  public int startBracketDepth = 0;

  public int bracketDepth = 0;

  public boolean inTag = false;

  public boolean tagNamePending = false;

  public boolean inMustache = false;

  public List<String> identifiers = null;

  public void addIdentifier(CharSequence idt) {
    if (identifiers == null) identifiers = new ArrayList<>();
    identifiers.add(idt.toString());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    VueState that = (VueState) o;
    return block == that.block
        && commentState == that.commentState
        && bracketDepth == that.bracketDepth
        && inTag == that.inTag
        && tagNamePending == that.tagNamePending
        && inMustache == that.inMustache;
  }

  @Override
  public int hashCode() {
    return Objects.hash(block, commentState, bracketDepth, inTag, tagNamePending, inMustache);
  }
}
