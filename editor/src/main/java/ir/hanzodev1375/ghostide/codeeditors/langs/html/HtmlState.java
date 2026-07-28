/**
 * Comment by ghost ide
 * @author: Ninjacoder
 */
package ir.hanzodev1375.ghostide.codeeditors.langs.html;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class HtmlState {

    // 0 = text, 1 = inside <!-- -->, 2 = inside <![CDATA[, 3 = inside <script>, 4 = inside <style>
    public int state = 0;

    public String pendingTagName = "";

    public int startBracketDepth = 0;

    public int bracketDepth = 0;

    // 0 = normal JS, 1 = inside an unterminated JS block comment
    public int jsState = 0;

    public int jsStartBracketDepth = 0;

    public int jsBracketDepth = 0;

    // 0 = normal CSS, 1 = inside an unterminated CSS block comment
    public int cssState = 0;

    public int cssStartBracketDepth = 0;

    public int cssBracketDepth = 0;

    public List<String> identifiers = null;

    public void addIdentifier(CharSequence idt) {
        if (identifiers == null)
            identifiers = new ArrayList<>();
        identifiers.add(idt.toString());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        HtmlState that = (HtmlState) o;
        return state == that.state
                && pendingTagName.equals(that.pendingTagName)
                && bracketDepth == that.bracketDepth
                && jsState == that.jsState
                && jsBracketDepth == that.jsBracketDepth
                && cssState == that.cssState
                && cssBracketDepth == that.cssBracketDepth;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                state, pendingTagName, bracketDepth, jsState, jsBracketDepth, cssState, cssBracketDepth);
    }
}
