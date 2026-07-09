package ir.hanzodev1375.ghostide.codeeditors.preview.htmltag;

import ir.hanzodev1375.ghostide.codeeditors.preview.Match;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class HtmlTagRefUtils {

  private HtmlTagRefUtils() {}

  // Matches the tag name in an opening (<div) or closing (</div) HTML tag.
  private static final Pattern TAG_PATTERN = Pattern.compile("</?([a-zA-Z][a-zA-Z0-9]*)");

  static Match findTagAtPosition(String lineText, int cursorColumn) {
    if (lineText == null || lineText.isEmpty()) return null;

    Matcher matcher = TAG_PATTERN.matcher(lineText);
    while (matcher.find()) {
      int start = matcher.start(1);
      int end = matcher.end(1);

      if (cursorColumn >= start && cursorColumn <= end) {
        return new Match(matcher.group(1), start, end);
      }
    }
    return null;
  }
}
