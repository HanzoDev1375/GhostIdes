package ir.hanzodev1375.ghostide.codeeditors.preview.url;

import android.util.Log;
import ir.hanzodev1375.ghostide.codeeditors.preview.Match;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class UrlRefUtils {

  private UrlRefUtils() {}

  private static final Pattern URL_PATTERN =
      Pattern.compile("https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+", Pattern.CASE_INSENSITIVE);

  static Match findUrlAtPosition(String lineText, int cursorColumn) {
    if (lineText == null || lineText.isEmpty()) return null;

    Matcher matcher = URL_PATTERN.matcher(lineText);
    while (matcher.find()) {
      int start = matcher.start();
      int end = matcher.end();

      if (cursorColumn >= start && cursorColumn <= end) {
        String cleaned = clean(lineText.substring(start, end));
        if (isValidUrl(cleaned)) {
          return new Match(cleaned, start, end);
        }
      }
    }
    return null;
  }

  private static String clean(String raw) {
    String cleaned = raw.trim();

    if ((cleaned.startsWith("\"") && cleaned.endsWith("\""))
        || (cleaned.startsWith("'") && cleaned.endsWith("'"))) {
      cleaned = cleaned.substring(1, cleaned.length() - 1);
    }

    while (cleaned.endsWith(",")
        || cleaned.endsWith(";")
        || cleaned.endsWith(".")
        || cleaned.endsWith("\"")
        || cleaned.endsWith("'")) {
      cleaned = cleaned.substring(0, cleaned.length() - 1);
    }

    cleaned = stripUnbalancedTrailingParen(cleaned);
    return cleaned.trim();
  }

  // Handles URLs that legitimately end with ")" (e.g. wikipedia links) while
  // still trimming a stray closing paren picked up from surrounding prose.
  private static String stripUnbalancedTrailingParen(String s) {
    int open = 0;
    int close = 0;
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c == '(') open++;
      else if (c == ')') close++;
    }
    while (close > open && s.endsWith(")")) {
      s = s.substring(0, s.length() - 1);
      close--;
    }
    return s;
  }

  static boolean isValidUrl(String url) {
    if (url == null || url.isEmpty()) return false;
    try {
      new URL(url);
      return true;
    } catch (Exception e) {
      Log.e("UrlRefUtils", e.getMessage());
      return false;
    }
  }
}
