package ir.hanzodev1375.ghostide.codeeditors.langs.ruby;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RubyState {

  public boolean insideBlockComment = false;

  public List<String> identifiers = null;

  public void addIdentifier(CharSequence idt) {
    if (identifiers == null) identifiers = new ArrayList<>();
    identifiers.add(idt.toString());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    RubyState that = (RubyState) o;
    return insideBlockComment == that.insideBlockComment;
  }

  @Override
  public int hashCode() {
    return Objects.hash(insideBlockComment);
  }
}
