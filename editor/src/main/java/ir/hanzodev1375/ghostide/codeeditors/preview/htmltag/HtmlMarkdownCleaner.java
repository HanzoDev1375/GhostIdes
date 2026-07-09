package ir.hanzodev1375.ghostide.codeeditors.preview.htmltag;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * mdn_html.json stores each tag's full raw MDN markdown page, including MDN-only macro syntax like
 * {{HTMLElement("base")}} that real markdown renderers (Markwon) don't understand. This keeps
 * genuine markdown (bold, links, inline code, fenced code blocks) intact and only resolves those
 * macros, so the result can be handed straight to Markwon.
 */
final class HtmlMarkdownCleaner {

  private static final int MAX_LENGTH = 600;

  private static final Set<String> CODE_LIKE_MACROS = new HashSet<>();

  static {
    CODE_LIKE_MACROS.add("cssxref");
    CODE_LIKE_MACROS.add("httpheader");
    CODE_LIKE_MACROS.add("domxref");
    CODE_LIKE_MACROS.add("jsxref");
    CODE_LIKE_MACROS.add("svgattr");
    CODE_LIKE_MACROS.add("svgelement");
  }

  private static final Pattern HEADING_PATTERN = Pattern.compile("(?m)^##\\s");
  private static final Pattern MACRO_WITH_ARGS_PATTERN =
      Pattern.compile("\\{\\{(\\w+)\\(([^)]*)\\)\\}\\}");
  private static final Pattern MACRO_BARE_PATTERN = Pattern.compile("\\{\\{[^}]*\\}\\}");
  private static final Pattern QUOTED_ARG_PATTERN = Pattern.compile("\"([^\"]*)\"");
  private static final Pattern BLANK_LINES_PATTERN = Pattern.compile("\\n{3,}");
  private static final Pattern HTML_CODE_BLOCK_PATTERN =
      Pattern.compile("```html[^\\n]*\\n(.*?)```", Pattern.DOTALL);

  private HtmlMarkdownCleaner() {}

  static String buildRenderableMarkdown(String rawMarkdown) {
    String body = stripFrontMatter(rawMarkdown);

    String summary = resolveMacros(extractSummarySection(body));
    summary = BLANK_LINES_PATTERN.matcher(summary).replaceAll("\n\n").trim();
    summary = truncate(summary, MAX_LENGTH);

    String example = extractExample(body);
    if (example == null) return summary;
    return summary + "\n\n```html\n" + example + "\n```";
  }

  private static String stripFrontMatter(String markdown) {
    if (markdown == null) return "";
    String trimmed = markdown.trim();
    if (trimmed.startsWith("---")) {
      int end = trimmed.indexOf("\n---", 3);
      if (end != -1) {
        int afterEnd = trimmed.indexOf('\n', end + 4);
        if (afterEnd != -1) return trimmed.substring(afterEnd + 1);
      }
    }
    return trimmed;
  }

  private static String extractSummarySection(String body) {
    int end = body.length();

    Matcher headingMatcher = HEADING_PATTERN.matcher(body);
    if (headingMatcher.find()) {
      end = Math.min(end, headingMatcher.start());
    }

    int interactiveIndex = body.indexOf("{{InteractiveExample");
    if (interactiveIndex != -1) {
      end = Math.min(end, interactiveIndex);
    }

    return body.substring(0, end);
  }

  private static String resolveMacros(String text) {
    Matcher matcher = MACRO_WITH_ARGS_PATTERN.matcher(text);
    StringBuffer buffer = new StringBuffer();
    while (matcher.find()) {
      String macroName = matcher.group(1).toLowerCase(Locale.ROOT);
      String lastArg = lastQuotedArg(matcher.group(2));
      String replacement;
      if (lastArg.isEmpty()) {
        replacement = "";
      } else if (macroName.equals("htmlelement")) {
        replacement = "`<" + lastArg + ">`";
      } else if (CODE_LIKE_MACROS.contains(macroName)) {
        replacement = "`" + lastArg + "`";
      } else {
        replacement = lastArg;
      }
      matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
    }
    matcher.appendTail(buffer);

    return MACRO_BARE_PATTERN.matcher(buffer.toString()).replaceAll("");
  }

  private static String lastQuotedArg(String args) {
    Matcher matcher = QUOTED_ARG_PATTERN.matcher(args);
    String last = "";
    while (matcher.find()) {
      last = matcher.group(1);
    }
    return last;
  }

  private static String extractExample(String body) {
    Matcher matcher = HTML_CODE_BLOCK_PATTERN.matcher(body);
    if (matcher.find()) {
      String code = matcher.group(1).trim();
      return code.isEmpty() ? null : code;
    }
    return null;
  }

  private static String truncate(String text, int maxLength) {
    if (text.length() <= maxLength) return text;
    int cut = text.lastIndexOf(' ', maxLength);
    if (cut <= 0) cut = maxLength;
    return text.substring(0, cut).trim() + "…";
  }
}
