package ir.hanzodev1375.ghostide.codeeditors.langs.lsp.gth;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Schema of GhostIDE theme files ({@code .gth}), a port of the original JS
 * {@code theme-schema.js}.
 *
 * <p>A theme is one JSON object with exactly four sections:
 *
 * <pre>activity | editor | widget | material3</pre>
 *
 * Every color value is a hex string (#RRGGBB or #RRGGBBAA) OR a relative reference
 * {@code "@section.key"} pointing at another color. References may chain.
 * {@code widget.blursize} is a number; {@code widget.imagepath} is a string (file path).
 *
 * <p>{@link #DEFAULTS} mirror {@code ThemeManager.buildDefaultThemeJson()} so references to a
 * section that is missing from the file still resolve (as at runtime, where missing keys are
 * filled from the default theme before applying).
 */
public final class ThemeSchema {

  public static final List<String> BLOCKS =
      Collections.unmodifiableList(Arrays.asList("activity", "editor", "widget", "material3"));

  private static final Set<String> BLOCK_SET = new HashSet<>(BLOCKS);

  private static final Map<String, Map<String, String>> DEFAULTS = new LinkedHashMap<>();
  private static final Map<String, Map<String, String>> DESC = new LinkedHashMap<>();

  static {
    putAll(
        DEFAULTS,
        "activity",
        "background", "#282c34",
        "statusBar", "#282c34",
        "navigationBar", "#282c34");

    putAll(
        DEFAULTS,
        "editor",
        "lineDivider", "#3e4452",
        "lineNumber", "#5c6370",
        "lineNumberBackground", "#282c34",
        "wholeBackground", "#282c34",
        "textNormal", "#abb2bf",
        "selectedTextBackground", "#3e4452",
        "selectionInsert", "#528bff",
        "selectionHandle", "#528bff",
        "currentLine", "#2c313a",
        "underline", "#abb2bf",
        "scrollBarThumb", "#3e4452",
        "scrollBarThumbPressed", "#528bff",
        "scrollBarTrack", "#21252b",
        "blockLine", "#3e4452",
        "blockLineCurrent", "#528bff",
        "lineNumberPanel", "#21252b",
        "lineNumberPanelText", "#abb2bf",
        "completionWndBackground", "#282c34",
        "completionWndCorner", "#282c34",
        "keyword", "#c678dd",
        "comment", "#5c6370",
        "operator", "#56b6c2",
        "literal", "#d19a66",
        "identifierVar", "#e06c75",
        "identifierName", "#61afef",
        "functionName", "#61afef",
        "annotation", "#e5c07b",
        "matchedTextBackground", "#3e4452",
        "matchedTextBorder", "#528bff",
        "textSelected", "#ffffff",
        "nonPrintableChar", "#3e4452",
        "htmlTag", "#e06c75",
        "attributeName", "#d19a66",
        "attributeValue", "#98c379",
        "problemError", "#e06c75",
        "problemWarning", "#e5c07b",
        "problemTypo", "#98c379",
        "colornextdot", "#c678dd",
        "colornextbrak", "#56b6c2",
        "colornextchar", "#d19a66",
        "coloruppercase", "#61afef",
        "colornextless", "#98c379",
        "lineNumberCurrent", "#528bff",
        "selectedTextBorder", "#528bff",
        "currentRowBorder", "#3e4452",
        "highlightedDelimitersBackground", "#2c313a",
        "highlightedDelimitersUnderline", "#528bff",
        "highlightedDelimitersForeground", "#abb2bf",
        "highlightedDelimitersBorder", "#528bff",
        "textHighlightBackground", "#3e4452",
        "textHighlightBorder", "#528bff",
        "textHighlightStrongBackground", "#2c313a",
        "textHighlightStrongBorder", "#c678dd",
        "staticSpanBackground", "#282c34",
        "staticSpanForeground", "#abb2bf",
        "textInlayHintBackground", "#2c313a",
        "textInlayHintForeground", "#5c6370",
        "snippetBackgroundEditing", "#2c313a",
        "snippetBackgroundRelated", "#3e4452",
        "snippetBackgroundInactive", "#21252b",
        "hardWrapMarker", "#3e4452",
        "functionCharBackgroundStroke", "#3e4452",
        "diagnosticTooltipBackground", "#2c313a",
        "diagnosticTooltipBriefMsg", "#abb2bf",
        "diagnosticTooltipDetailedMsg", "#5c6370",
        "diagnosticTooltipAction", "#61afef",
        "stickyScrollDivider", "#3e4452",
        "strikeThrough", "#00000000",
        "sideBlockLine", "#3e4452",
        "completionWndTextPrimary", "#abb2bf",
        "completionWndTextSecondary", "#5c6370",
        "completionWndItemCurrent", "#2c313a",
        "completionWndTextMatched", "#61afef",
        "signatureBackground", "#282c34",
        "signatureBorder", "#3e4452",
        "signatureTextNormal", "#abb2bf",
        "signatureTextHighlightedParameter", "#e06c75",
        "hoverBackground", "#2c313a",
        "hoverBorder", "#528bff",
        "hoverTextNormal", "#abb2bf",
        "hoverTextHighlighted", "#61afef",
        "textActionWindowBackground", "#282c34",
        "textActionWindowIconColor", "#abb2bf",
        "minimapBackground", "#a0282c34",
        "minimapViewport", "#30ffffff",
        "minimapViewportBorder", "#b0ffffff",
        "bracketlevelmatch1", "#FFDD00",
        "bracketlevelmatch2", "#00D9FF",
        "bracketlevelmatch3", "#00FF55",
        "bracketlevelmatch4", "#FF6200",
        "bracketlevelmatch5", "#FF64F5",
        "bracketlevelmatch6", "#64FFD0");

    putAll(
        DEFAULTS,
        "widget",
        "text", "#abb2bf",
        "hint", "#5c6370",
        "accent", "#61afef",
        "background", "#282c34",
        "surface", "#2c313a",
        "stroke", "#3e4452",
        "fabBackground", "#61afef",
        "fabIcon", "#ffffff",
        "tabSelected", "#61afef",
        "tabUnselected", "#5c6370",
        "imageTint", "#abb2bf",
        "menubackground", "#282c34",
        "menutextcolor", "#abb2bf",
        "selectedmenucolor", "#3e4452",
        "imagepath", "",
        "blursize", "1");

    putAll(
        DEFAULTS,
        "material3",
        "primary", "#B9C3FF",
        "surfaceTint", "#B9C3FF",
        "onPrimary", "#212C61",
        "primaryContainer", "#384379",
        "onPrimaryContainer", "#DDE1FF",
        "secondary", "#C3C5DD",
        "onSecondary", "#2C2F42",
        "secondaryContainer", "#424659",
        "onSecondaryContainer", "#DFE1F9",
        "tertiary", "#E5BAD8",
        "onTertiary", "#44263E",
        "tertiaryContainer", "#5C3C55",
        "onTertiaryContainer", "#FFD7F3",
        "error", "#FFB4AB",
        "onError", "#690005",
        "errorContainer", "#93000A",
        "onErrorContainer", "#FFDAD6",
        "background", "#121318",
        "onBackground", "#E3E1E9",
        "surface", "#121318",
        "onSurface", "#E3E1E9",
        "surfaceVariant", "#45464F",
        "onSurfaceVariant", "#C6C5D0",
        "outline", "#90909A",
        "outlineVariant", "#45464F",
        "shadow", "#000000",
        "scrim", "#000000",
        "inverseSurface", "#E3E1E9",
        "inverseOnSurface", "#303036",
        "inversePrimary", "#505B92",
        "primaryFixed", "#DDE1FF",
        "onPrimaryFixed", "#08164B",
        "primaryFixedDim", "#B9C3FF",
        "onPrimaryFixedVariant", "#384379",
        "secondaryFixed", "#DFE1F9",
        "onSecondaryFixed", "#171B2C",
        "secondaryFixedDim", "#C3C5DD",
        "onSecondaryFixedVariant", "#424659",
        "tertiaryFixed", "#FFD7F3",
        "onTertiaryFixed", "#2D1228",
        "tertiaryFixedDim", "#E5BAD8",
        "onTertiaryFixedVariant", "#5C3C55",
        "surfaceDim", "#121318",
        "surfaceBright", "#38393F",
        "surfaceContainerLowest", "#0D0E13",
        "surfaceContainerLow", "#1B1B21",
        "surfaceContainer", "#1F1F25",
        "surfaceContainerHigh", "#292A2F",
        "surfaceContainerHighest", "#34343A");

    putAll(
        DESC,
        "activity",
        "background", "Overall window background color",
        "statusBar", "Status bar color (top area with clock/notifications)",
        "navigationBar", "Navigation bar color at the bottom of the screen");

    putAll(
        DESC,
        "editor",
        "textNormal", "Default text color",
        "keyword", "Keywords (if, for, return, ...)",
        "comment", "Comments",
        "operator", "Operators (+, =, &&, ...)",
        "literal", "Literals (numbers, strings)",
        "identifierVar", "Variable identifier",
        "identifierName", "Identifier name",
        "functionName", "Function names",
        "annotation", "Annotations (@Override, ...)",
        "htmlTag", "HTML tags",
        "attributeName", "Attribute names",
        "attributeValue", "Attribute values",
        "nonPrintableChar", "Invisible / non-printable characters",
        "colornextdot", "Token color after a dot",
        "colornextbrak", "Token color after a bracket",
        "colornextchar", "Next character highlight color",
        "coloruppercase", "Uppercase token color",
        "colornextless", "Token color related to less-than",
        "lineDivider", "Divider between editor sections",
        "currentLine", "Highlight of the line the cursor is on",
        "lineNumber", "Line number text color",
        "lineNumberCurrent", "Current (active) line number",
        "lineNumberBackground", "Background behind line numbers",
        "lineNumberPanel", "Line number panel background",
        "lineNumberPanelText", "Line number panel text color",
        "currentRowBorder", "Current row border",
        "blockLine", "Code block guide line",
        "blockLineCurrent", "Code block guide line in the current block",
        "sideBlockLine", "Side block guide line",
        "hardWrapMarker", "Hard wrap marker color",
        "strikeThrough", "Strikethrough text color (deleted lines)",
        "selectedTextBackground", "Background behind selected text",
        "selectedTextBorder", "Border around selected text",
        "textSelected", "Selected text color",
        "selectionInsert", "Cursor line color",
        "selectionHandle", "Selection handle color",
        "underline", "Underline color",
        "scrollBarThumb", "Scroll bar thumb color",
        "scrollBarThumbPressed", "Thumb color when pressed/touched",
        "scrollBarTrack", "Scroll bar track color",
        "completionWndBackground", "Autocomplete popup background",
        "completionWndCorner", "Autocomplete popup corner color",
        "completionWndTextPrimary", "Primary text in autocomplete",
        "completionWndTextSecondary", "Secondary text in autocomplete",
        "completionWndItemCurrent", "Current item highlight in autocomplete",
        "completionWndTextMatched", "Matched text color in completion item",
        "matchedTextBackground", "Matched text background in search",
        "matchedTextBorder", "Matched text border",
        "highlightedDelimitersBackground", "Highlighted bracket pair background",
        "highlightedDelimitersUnderline", "Highlighted bracket pair underline",
        "highlightedDelimitersForeground", "Highlighted bracket pair foreground",
        "highlightedDelimitersBorder", "Highlighted bracket pair border",
        "textHighlightBackground", "General text highlight background",
        "textHighlightBorder", "General text highlight border",
        "textHighlightStrongBackground", "Strong text highlight background",
        "textHighlightStrongBorder", "Strong text highlight border",
        "staticSpanBackground", "Static span background",
        "staticSpanForeground", "Static span foreground",
        "problemError", "Error underline/color",
        "problemWarning", "Warning underline/color",
        "problemTypo", "Spelling error underline/color",
        "signatureBackground", "Function signature popup background",
        "signatureBorder", "Function signature popup border",
        "signatureTextNormal", "Normal text in signature popup",
        "signatureTextHighlightedParameter", "Highlighted parameter in signature popup",
        "hoverBackground", "Hover tooltip background",
        "hoverBorder", "Hover tooltip border",
        "hoverTextNormal", "Normal text in hover tooltip",
        "hoverTextHighlighted", "Highlighted text in hover tooltip",
        "diagnosticTooltipBackground", "Diagnostic tooltip background",
        "diagnosticTooltipBriefMsg", "Brief diagnostic message",
        "diagnosticTooltipDetailedMsg", "Detailed diagnostic message",
        "diagnosticTooltipAction", "Action (clickable) text for diagnostics",
        "textActionWindowBackground", "Text action window background",
        "textActionWindowIconColor", "Text action window icon color",
        "textInlayHintBackground", "Inlay hint background",
        "textInlayHintForeground", "Inlay hint foreground",
        "snippetBackgroundEditing", "Snippet being edited background",
        "snippetBackgroundRelated", "Related snippet area background",
        "snippetBackgroundInactive", "Inactive snippet background",
        "functionCharBackgroundStroke", "Function character background/stroke",
        "minimapBackground", "Minimap background",
        "minimapViewport", "Minimap viewport rectangle",
        "minimapViewportBorder", "Minimap viewport border",
        "bracketlevelmatch1", "Bracket nesting level 1 color",
        "bracketlevelmatch2", "Bracket nesting level 2 color",
        "bracketlevelmatch3", "Bracket nesting level 3 color",
        "bracketlevelmatch4", "Bracket nesting level 4 color",
        "bracketlevelmatch5", "Bracket nesting level 5 color",
        "bracketlevelmatch6", "Bracket nesting level 6 color",
        "stickyScrollDivider", "Divider under the sticky header",
        "wholeBackground", "Overall editor background");

    putAll(
        DESC,
        "widget",
        "text", "General widget text color",
        "hint", "Hint/placeholder text color (e.g. in inputs)",
        "accent", "Accent color used across widgets",
        "background", "General background color",
        "surface", "Surface color (cards/panels)",
        "stroke", "Border/outline color",
        "fabBackground", "Floating action button background",
        "fabIcon", "Floating action button icon color",
        "tabSelected", "Selected tab color",
        "tabUnselected", "Unselected tab color",
        "imageTint", "Tint applied to icons/images",
        "menubackground", "Dropdown menu background",
        "menutextcolor", "Menu text color",
        "selectedmenucolor", "Selected menu item highlight color",
        "imagepath", "Path to the blurred background image/GIF/video",
        "blursize", "Background image blur amount (0-25)");

    putAll(
        DESC,
        "material3",
        "primary", "Primary brand color (buttons, active indicators)",
        "onPrimary", "Text/icon on primary",
        "primaryContainer", "Softer (container) version of primary",
        "onPrimaryContainer", "Text/icon on primaryContainer",
        "primaryFixed", "Light fixed tone of primary",
        "onPrimaryFixed", "Text on primaryFixed",
        "primaryFixedDim", "Dim fixed tone of primary",
        "onPrimaryFixedVariant", "Text on primaryFixedDim",
        "inversePrimary", "Primary for inverse surfaces",
        "secondary", "Secondary brand color",
        "onSecondary", "Text/icon on secondary",
        "secondaryContainer", "Softer container of secondary",
        "onSecondaryContainer", "Text/icon on secondaryContainer",
        "secondaryFixed", "Light fixed tone of secondary",
        "onSecondaryFixed", "Text on secondaryFixed",
        "secondaryFixedDim", "Dim fixed tone of secondary",
        "onSecondaryFixedVariant", "Text on secondaryFixedDim",
        "tertiary", "Third accent color",
        "onTertiary", "Text/icon on tertiary",
        "tertiaryContainer", "Softer container of tertiary",
        "onTertiaryContainer", "Text/icon on tertiaryContainer",
        "tertiaryFixed", "Light fixed tone of tertiary",
        "onTertiaryFixed", "Text on tertiaryFixed",
        "tertiaryFixedDim", "Dim fixed tone of tertiary",
        "onTertiaryFixedVariant", "Text on tertiaryFixedDim",
        "error", "Error color (validation, issues)",
        "onError", "Text/icon on error",
        "errorContainer", "Softer container of error",
        "onErrorContainer", "Text/icon on errorContainer",
        "background", "App background",
        "onBackground", "Text/icon on background",
        "surface", "App surface (cards, sheets)",
        "onSurface", "Text/icon on surface",
        "surfaceVariant", "Surface variant (for adjacent surfaces)",
        "onSurfaceVariant", "Text/icon on surfaceVariant",
        "surfaceTint", "Tint color on surfaces",
        "outline", "Border/outline color",
        "outlineVariant", "Lighter outline variant",
        "shadow", "Shadow color",
        "scrim", "Dark layer behind dialogs/sheets",
        "inverseSurface", "Inverse surface color",
        "inverseOnSurface", "Text/icon on inverseSurface",
        "surfaceDim", "Dim surface tone (darkest)",
        "surfaceBright", "Bright surface tone",
        "surfaceContainerLowest", "Lowest surface container (darkest)",
        "surfaceContainerLow", "Low surface container",
        "surfaceContainer", "Default surface container",
        "surfaceContainerHigh", "High surface container",
        "surfaceContainerHighest", "Highest surface container (lightest)");
  }

  private static void putAll(Map<String, Map<String, String>> map, String block, String... kv) {
    Map<String, String> section = new LinkedHashMap<>();
    for (int i = 0; i + 1 < kv.length; i += 2) {
      section.put(kv[i], kv[i + 1]);
    }
    map.put(block, section);
  }

  private ThemeSchema() {}

  /** Key type of a property. */
  public static String typeOf(String block, String key) {
    if ("widget".equals(block) && "blursize".equals(key)) return "number";
    if ("widget".equals(block) && "imagepath".equals(key)) return "path";
    return "color";
  }

  public static boolean isBlock(String name) {
    return name != null && BLOCK_SET.contains(name);
  }

  public static List<String> keysOf(String block) {
    Map<String, String> d = DEFAULTS.get(block);
    return d != null ? new java.util.ArrayList<>(d.keySet()) : Collections.emptyList();
  }

  public static boolean isValidKey(String block, String key) {
    Map<String, String> d = DEFAULTS.get(block);
    return d != null && d.containsKey(key);
  }

  public static String description(String block, String key) {
    Map<String, String> d = DESC.get(block);
    return d != null && d.get(key) != null ? d.get(key) : "";
  }

  public static Map<String, String> defaultsOf(String block) {
    Map<String, String> d = DEFAULTS.get(block);
    return d != null ? d : Collections.emptyMap();
  }

  /**
   * Resolves "@section.key" (chained) against an effective map, cycle-safe. If the value is not
   * a reference it is returned unchanged.
   */
  public static String resolveRef(Map<String, Map<String, String>> effective, String refText) {
    if (refText == null || !refText.startsWith("@")) return refText;
    Set<String> seen = new HashSet<>();
    String current = refText;
    int hops = 0;
    while (current.startsWith("@") && hops < 24) {
      if (seen.contains(current)) break;
      seen.add(current);
      String[] parts = current.substring(1).split("\\.", 2);
      if (parts.length != 2) return current;
      String block = parts[0];
      String key = parts[1];
      Map<String, String> box = effective.get(block);
      if (box == null || !box.containsKey(key)) return current;
      current = String.valueOf(box.get(key));
      hops++;
    }
    return current;
  }
}