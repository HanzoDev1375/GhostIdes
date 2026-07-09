package ir.hanzodev1375.ghostide.codeeditors.preview.xmlattr;

import ir.hanzodev1375.ghostide.codeeditors.preview.Match;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class XmlAttrRefUtils {

  private XmlAttrRefUtils() {}

  // Matches an XML attribute name (optionally namespaced, e.g. android:layout_width
  // or app:layout_constraintTop_toTopOf) right before its "=" assignment.
  private static final Pattern ATTR_PATTERN =
      Pattern.compile("([a-zA-Z_][\\w.\\-]*:)?[a-zA-Z_][\\w.\\-]*(?=\\s*=\\s*[\"'])");

  static Match findAttrAtPosition(String lineText, int cursorColumn) {
    if (lineText == null || lineText.isEmpty()) return null;

    Matcher matcher = ATTR_PATTERN.matcher(lineText);
    while (matcher.find()) {
      int start = matcher.start();
      int end = matcher.end();

      if (cursorColumn >= start && cursorColumn <= end) {
        return new Match(matcher.group(), start, end);
      }
    }
    return null;
  }
}
