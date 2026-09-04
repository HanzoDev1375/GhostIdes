# Theme Maker — Ghost IDE 👻

This guide explains **everything** you need to create your own Ghost IDE theme — even if you have **zero programming experience** and have never looked at a JSON file. An AI (or a friend) reading this page should be able to understand *exactly* which setting maps to which color, without you explaining anything.

> Read this whole page top-to-bottom. Every field is listed, explained in plain language, and mapped to its JSON key.

---

## 1. What is a theme file?

A Ghost IDE theme is a **single text file** with the extension **`.gth`**. Inside it is a **JSON** object — just a structured list of color names and their values.

- You can open it in the built-in **Theme Editor** (visual color pickers) — no coding needed.
- You can also open it as plain text and edit the hex colors by hand / ask an AI to edit it.

Every color is written as a hex value like `"#61afef"` (a `#` followed by 8 hexadecimal digits: `#RRGGBBAA` — Red, Green, Blue, Alpha).

---

## 2. Quick start (create your first theme in 1 minute)

1. Open **Ghost IDE** and go to the **File Manager**.
2. Find your current theme file (`.gth`) — or copy any existing `.gth`.
3. Copy it and rename the copy (for example `mytheme.gth`).
4. Tap the new file → a sheet opens → choose **Edit** (✎).
5. The **Theme Editor** opens with 4 tabs at the top: **Activity** · **Editor** · **Widget** · **M3Color**.
6. Tap any color swatch → pick a new color with the picker → tap OK. The file is saved automatically.
7. When you are happy, tap the file in the File Manager again → choose **Apply**.

> Everything in this document corresponds to the 4 tabs. Whatever you see in the editor is exactly what is described below.

---

## 3. The 4 sections of a theme

A theme JSON has **4 top-level blocks**. They match the 4 tabs in the editor:

| JSON block | Editor tab | What it controls |
|------------|------------|------------------|
| `activity` | **Activity** | Window: background, status bar, navigation bar |
| `editor`   | **Editor**   | The code editor: syntax colors, selection, scroll bar, brackets, … |
| `widget`   | **Widget**   | UI widgets: text, surfaces, FAB, tabs, menus, blur/background image |
| `material3`| **M3Color**  | The Material 3 color system used to color the whole app UI |

Below, each block is documented field-by-field.

---

## 4. `activity` — Window colors (tab: Activity)

| JSON key | Meaning (plain language) |
|----------|--------------------------|
| `background`    | The main window background color |
| `statusBar`     | The color of the top status bar (clock / notifications area) |
| `navigationBar` | The color of the bottom navigation bar |

---

## 5. `editor` — Code editor colors (tab: Editor)

These control how code looks while you type. Grouped by meaning:

### Text & syntax
| JSON key | Meaning |
|----------|---------|
| `textNormal`      | Default text color |
| `keyword`         | Keywords (`if`, `for`, `return`, …) |
| `comment`         | Comments |
| `operator`        | Operators (`+`, `=`, `&&`, …) |
| `literal`         | Literals (numbers, strings) |
| `identifierVar`   | Variable identifiers |
| `identifierName`  | Identifier names |
| `functionName`    | Function names |
| `annotation`      | Annotations (`@Override`, …) |
| `htmlTag`         | HTML tags |
| `attributeName`   | HTML/attribute names |
| `attributeValue`  | HTML/attribute values |
| `nonPrintableChar`| Non-printable / invisible characters |
| `colornextdot`    | Next-character token color |
| `colornextbrak`   | Next-bracket token color |
| `colornextchar`   | Next-character highlight color |
| `coloruppercase`  | Uppercase token color |
| `colornextless`   | "Less than" related token color |

### Lines & numbers
| JSON key | Meaning |
|----------|---------|
| `lineDivider`          | Divider line between editor areas |
| `currentLine`          | Highlight of the line the cursor is on |
| `lineNumber`           | Line number text color |
| `lineNumberCurrent`    | Line number of the current (active) line |
| `lineNumberBackground` | Background behind line numbers |
| `lineNumberPanel`      | Line-number panel background |
| `lineNumberPanelText`  | Line-number panel text color |
| `currentRowBorder`     | Border of the current row |
| `blockLine`            | Code block guide line |
| `blockLineCurrent`     | Code block guide line on current block |
| `sideBlockLine`        | Side block guide line |
| `hardWrapMarker`       | Hard-wrap marker color |
| `strikeThrough`        | Strike-through text color (e.g. removed lines) |

### Selection & cursor
| JSON key | Meaning |
|----------|---------|
| `selectedTextBackground` | Background behind selected text |
| `selectedTextBorder`     | Border around selected text |
| `textSelected`           | Color of selected text |
| `selectionInsert`        | Insertion (cursor) line color |
| `selectionHandle`        | Selection handle color |
| `underline`              | Underline color |

### Scroll bar
| JSON key | Meaning |
|----------|---------|
| `scrollBarThumb`        | Scroll bar handle (thumb) color |
| `scrollBarThumbPressed` | Scroll bar thumb while pressed |
| `scrollBarTrack`        | Scroll bar track (background) color |

### Autocomplete / completion window
| JSON key | Meaning |
|----------|---------|
| `completionWndBackground` | Autocomplete popup background |
| `completionWndCorner`     | Autocomplete popup corner color |
| `completionWndTextPrimary`   | Primary text in autocomplete |
| `completionWndTextSecondary` | Secondary text in autocomplete |
| `completionWndItemCurrent`   | Highlight of the current autocomplete item |
| `completionWndTextMatched`   | Color of the matched part of the item text |

### Search / matches
| JSON key | Meaning |
|----------|---------|
| `matchedTextBackground`       | Background of matched search text |
| `matchedTextBorder`           | Border of matched search text |
| `highlightedDelimitersBackground`  | Background of highlighted delimiter pairs |
| `highlightedDelimitersUnderline`   | Underline of highlighted delimiters |
| `highlightedDelimitersForeground`  | Foreground of highlighted delimiters |
| `highlightedDelimitersBorder`      | Border of highlighted delimiters |

### Text highlights
| JSON key | Meaning |
|----------|---------|
| `textHighlightBackground`        | Generic text-highlight background |
| `textHighlightBorder`            | Generic text-highlight border |
| `textHighlightStrongBackground`  | Strong text-highlight background |
| `textHighlightStrongBorder`      | Strong text-highlight border |
| `staticSpanBackground`           | Static span background |
| `staticSpanForeground`           | Static span foreground |

### Problems / diagnostics
| JSON key | Meaning |
|----------|---------|
| `problemError`   | Error underline/color |
| `problemWarning` | Warning underline/color |
| `problemTypo`    | Typo underline/color |

### Tooltips (hover, signature, diagnostic)
| JSON key | Meaning |
|----------|---------|
| `signatureBackground`                 | Signature help popup background |
| `signatureBorder`                     | Signature help popup border |
| `signatureTextNormal`                 | Signature help normal text |
| `signatureTextHighlightedParameter`   | Highlighted parameter in signature help |
| `hoverBackground`                     | Hover tooltip background |
| `hoverBorder`                         | Hover tooltip border |
| `hoverTextNormal`                     | Hover tooltip normal text |
| `hoverTextHighlighted`                | Hover tooltip highlighted text |
| `diagnosticTooltipBackground`         | Diagnostic tooltip background |
| `diagnosticTooltipBriefMsg`           | Diagnostic brief message |
| `diagnosticTooltipDetailedMsg`        | Diagnostic detailed message |
| `diagnosticTooltipAction`             | Diagnostic action (clickable) text |
| `textActionWindowBackground`          | Text action window background |
| `textActionWindowIconColor`           | Text action window icon color |

### Inlay hints & snippets
| JSON key | Meaning |
|----------|---------|
| `textInlayHintBackground`   | Inlay hint background |
| `textInlayHintForeground`   | Inlay hint foreground |
| `snippetBackgroundEditing`  | Background of a snippet currently being edited |
| `snippetBackgroundRelated`  | Background of related snippet region |
| `snippetBackgroundInactive` | Background of inactive snippet region |
| `functionCharBackgroundStroke` | Function-character background/stroke |

### Minimap
| JSON key | Meaning |
|----------|---------|
| `minimapBackground`        | Minimap background |
| `minimapViewport`          | Minimap viewport rectangle |
| `minimapViewportBorder`    | Minimap viewport border |

### Bracket level colors (nesting depth)
| JSON key | Meaning |
|----------|---------|
| `bracketlevelmatch1` | Color for bracket nesting level 1 |
| `bracketlevelmatch2` | Color for bracket nesting level 2 |
| `bracketlevelmatch3` | Color for bracket nesting level 3 |
| `bracketlevelmatch4` | Color for bracket nesting level 4 |
| `bracketlevelmatch5` | Color for bracket nesting level 5 |
| `bracketlevelmatch6` | Color for bracket nesting level 6 |

### Sticky scroll
| JSON key | Meaning |
|----------|---------|
| `stickyScrollDivider` | Divider shown under the sticky (pinned) scroll header |

> ⚠️ A few keys exist in the theme engine defaults but are **not** shown in the Editor tab yet: `wholeBackground` (the editor's overall background). You can still set it by editing the JSON directly.

---

## 6. `widget` — UI widget colors (tab: Widget)

| JSON key | Meaning |
|----------|---------|
| `text`          | General widget text color |
| `hint`          | Hint/placeholder text color (e.g. in inputs) |
| `accent`        | Accent color used across widgets |
| `background`    | General background color |
| `surface`       | Surface (card/panel) color |
| `stroke`        | Border/outline stroke color |
| `fabBackground` | Floating Action Button background |
| `fabIcon`       | Floating Action Button icon color |
| `tabSelected`   | Selected tab color |
| `tabUnselected` | Unselected tab color |
| `imageTint`     | Tint applied to icons/images |
| `menubackground`    | Menu popup background |
| `menutextcolor`     | Menu text color |
| `selectedmenucolor` | Highlight color of the selected menu item |

Plus two extra controls (not colors):
| JSON key | Meaning |
|----------|---------|
| `imagepath` | Path to a background image / GIF / video used as a blurred backdrop |
| `blursize`  | How strongly the background image is blurred (0–25) |

---

## 7. `material3` — Material 3 color system (tab: M3Color)

This is the **color system** that colors every Material widget in the app (buttons, switches, dialogs, tabs, surfaces, text inputs). Material 3 pairs every color with an **"on"** color (the text/icon color that sits on top of it) so contrast stays readable.

### Primary
| JSON key | Meaning |
|----------|---------|
| `primary`          | Main brand color (buttons, active indicator) |
| `onPrimary`        | Text/icons placed on `primary` |
| `primaryContainer` | Softer container version of primary |
| `onPrimaryContainer` | Text/icons placed on `primaryContainer` |
| `primaryFixed`        | Primary "fixed" tone (bright) |
| `onPrimaryFixed`      | Text on `primaryFixed` |
| `primaryFixedDim`     | Dimmer fixed primary tone |
| `onPrimaryFixedVariant` | Text on `primaryFixedDim` |
| `inversePrimary`      | Primary used on inverse surfaces |

### Secondary
| JSON key | Meaning |
|----------|---------|
| `secondary`          | Secondary brand color |
| `onSecondary`        | Text/icons on `secondary` |
| `secondaryContainer` | Softer secondary container |
| `onSecondaryContainer` | Text on `secondaryContainer` |
| `secondaryFixed`        | Bright fixed secondary |
| `onSecondaryFixed`      | Text on `secondaryFixed` |
| `secondaryFixedDim`     | Dim fixed secondary |
| `onSecondaryFixedVariant` | Text on `secondaryFixedDim` |

### Tertiary
| JSON key | Meaning |
|----------|---------|
| `tertiary`          | Tertiary accent color |
| `onTertiary`        | Text/icons on `tertiary` |
| `tertiaryContainer` | Softer tertiary container |
| `onTertiaryContainer` | Text on `tertiaryContainer` |
| `tertiaryFixed`        | Bright fixed tertiary |
| `onTertiaryFixed`      | Text on `tertiaryFixed` |
| `tertiaryFixedDim`     | Dim fixed tertiary |
| `onTertiaryFixedVariant` | Text on `tertiaryFixedDim` |

### Error
| JSON key | Meaning |
|----------|---------|
| `error`          | Error color (validation, problems) |
| `onError`        | Text/icons on `error` |
| `errorContainer` | Softer error container |
| `onErrorContainer` | Text on `errorContainer` |

### Neutral (background / surface)
| JSON key | Meaning |
|----------|---------|
| `background`    | App background |
| `onBackground`  | Text/icons on `background` |
| `surface`       | App surface (cards, sheets) |
| `onSurface`     | Text/icons on `surface` |
| `surfaceVariant`   | Surface variant (used for surfaces next to each other) |
| `onSurfaceVariant` | Text/icons on `surfaceVariant` |
| `surfaceTint`      | Tint over surfaces |

### Outline & misc
| JSON key | Meaning |
|----------|---------|
| `outline`         | Outline/border color |
| `outlineVariant`  | Lighter outline variant |
| `shadow`          | Shadow color |
| `scrim`           | Scrim (dim overlay behind dialogs/sheets) |

### Inverse
| JSON key | Meaning |
|----------|---------|
| `inverseSurface`   | Inverse surface color |
| `inverseOnSurface` | Text/icons on `inverseSurface` |

### Surface containers (layer tones)
| JSON key | Meaning |
|----------|---------|
| `surfaceDim`             | Dim (darkest) surface tone |
| `surfaceBright`          | Bright surface tone |
| `surfaceContainerLowest` | Lowest (darkest) surface container |
| `surfaceContainerLow`    | Low surface container |
| `surfaceContainer`       | Default surface container |
| `surfaceContainerHigh`   | High surface container |
| `surfaceContainerHighest`| Highest (lightest) surface container |

> **Tip:** the "rule of thumb" for Material 3 is: for every color there is an `on...` version. If you change `primary`, also change `onPrimary` so the text stays readable. The same applies to the `...Container` / `on...Container` pairs.

---

## 8. Full JSON template (copy-paste)

Save the following as `mytheme.gth` and edit the values. This is the exact structure Ghost IDE expects.

```json
{
  "activity": {
    "background": "#282c34",
    "statusBar": "#282c34",
    "navigationBar": "#282c34"
  },

  "editor": {
    "lineDivider": "#3e4452",
    "wholeBackground": "#282c34",
    "lineNumber": "#5c6370",
    "lineNumberBackground": "#282c34",
    "textNormal": "#abb2bf",
    "selectedTextBackground": "#3e4452",
    "selectionInsert": "#528bff",
    "selectionHandle": "#528bff",
    "currentLine": "#2c313a",
    "underline": "#abb2bf",
    "scrollBarThumb": "#3e4452",
    "scrollBarThumbPressed": "#528bff",
    "scrollBarTrack": "#21252b",
    "blockLine": "#3e4452",
    "blockLineCurrent": "#528bff",
    "lineNumberPanel": "#21252b",
    "lineNumberPanelText": "#abb2bf",
    "completionWndBackground": "#282c34",
    "completionWndCorner": "#282c34",
    "keyword": "#c678dd",
    "comment": "#5c6370",
    "operator": "#56b6c2",
    "literal": "#d19a66",
    "identifierVar": "#e06c75",
    "identifierName": "#61afef",
    "functionName": "#61afef",
    "annotation": "#e5c07b",
    "matchedTextBackground": "#3e4452",
    "matchedTextBorder": "#528bff",
    "textSelected": "#ffffff",
    "nonPrintableChar": "#3e4452",
    "htmlTag": "#e06c75",
    "attributeName": "#d19a66",
    "attributeValue": "#98c379",
    "problemError": "#e06c75",
    "problemWarning": "#e5c07b",
    "problemTypo": "#98c379",
    "colornextdot": "#c678dd",
    "colornextbrak": "#56b6c2",
    "colornextchar": "#d19a66",
    "coloruppercase": "#61afef",
    "colornextless": "#98c379",
    "lineNumberCurrent": "#528bff",
    "selectedTextBorder": "#528bff",
    "currentRowBorder": "#3e4452",
    "highlightedDelimitersBackground": "#2c313a",
    "highlightedDelimitersUnderline": "#528bff",
    "highlightedDelimitersForeground": "#abb2bf",
    "highlightedDelimitersBorder": "#528bff",
    "textHighlightBackground": "#3e4452",
    "textHighlightBorder": "#528bff",
    "textHighlightStrongBackground": "#2c313a",
    "textHighlightStrongBorder": "#c678dd",
    "staticSpanBackground": "#282c34",
    "staticSpanForeground": "#abb2bf",
    "textInlayHintBackground": "#2c313a",
    "textInlayHintForeground": "#5c6370",
    "snippetBackgroundEditing": "#2c313a",
    "snippetBackgroundRelated": "#3e4452",
    "snippetBackgroundInactive": "#21252b",
    "hardWrapMarker": "#3e4452",
    "functionCharBackgroundStroke": "#3e4452",
    "diagnosticTooltipBackground": "#2c313a",
    "diagnosticTooltipBriefMsg": "#abb2bf",
    "diagnosticTooltipDetailedMsg": "#5c6370",
    "diagnosticTooltipAction": "#61afef",
    "stickyScrollDivider": "#3e4452",
    "strikeThrough": "#00000000",
    "sideBlockLine": "#3e4452",
    "completionWndTextPrimary": "#abb2bf",
    "completionWndTextSecondary": "#5c6370",
    "completionWndItemCurrent": "#2c313a",
    "completionWndTextMatched": "#61afef",
    "signatureBackground": "#282c34",
    "signatureBorder": "#3e4452",
    "signatureTextNormal": "#abb2bf",
    "signatureTextHighlightedParameter": "#e06c75",
    "hoverBackground": "#2c313a",
    "hoverBorder": "#528bff",
    "hoverTextNormal": "#abb2bf",
    "hoverTextHighlighted": "#61afef",
    "textActionWindowBackground": "#282c34",
    "textActionWindowIconColor": "#abb2bf",
    "minimapBackground": "#a0282c34",
    "minimapViewport": "#30ffffff",
    "minimapViewportBorder": "#b0ffffff",
    "bracketlevelmatch1": "#FFDD00",
    "bracketlevelmatch2": "#00D9FF",
    "bracketlevelmatch3": "#00FF55",
    "bracketlevelmatch4": "#FF6200",
    "bracketlevelmatch5": "#FF64F5",
    "bracketlevelmatch6": "#64FFD0"
  },

  "widget": {
    "text": "#abb2bf",
    "hint": "#5c6370",
    "accent": "#61afef",
    "background": "#282c34",
    "surface": "#2c313a",
    "stroke": "#3e4452",
    "fabBackground": "#61afef",
    "fabIcon": "#ffffff",
    "tabSelected": "#61afef",
    "tabUnselected": "#5c6370",
    "imageTint": "#abb2bf",
    "menubackground": "#282c34",
    "menutextcolor": "#abb2bf",
    "selectedmenucolor": "#3e4452",
    "imagepath": "",
    "blursize": 1
  },

  "material3": {
    "primary": "#B9C3FF",
    "surfaceTint": "#B9C3FF",
    "onPrimary": "#212C61",
    "primaryContainer": "#384379",
    "onPrimaryContainer": "#DDE1FF",
    "secondary": "#C3C5DD",
    "onSecondary": "#2C2F42",
    "secondaryContainer": "#424659",
    "onSecondaryContainer": "#DFE1F9",
    "tertiary": "#E5BAD8",
    "onTertiary": "#44263E",
    "tertiaryContainer": "#5C3C55",
    "onTertiaryContainer": "#FFD7F3",
    "error": "#FFB4AB",
    "onError": "#690005",
    "errorContainer": "#93000A",
    "onErrorContainer": "#FFDAD6",
    "background": "#121318",
    "onBackground": "#E3E1E9",
    "surface": "#121318",
    "onSurface": "#E3E1E9",
    "surfaceVariant": "#45464F",
    "onSurfaceVariant": "#C6C5D0",
    "outline": "#90909A",
    "outlineVariant": "#45464F",
    "shadow": "#000000",
    "scrim": "#000000",
    "inverseSurface": "#E3E1E9",
    "inverseOnSurface": "#303036",
    "inversePrimary": "#505B92",
    "primaryFixed": "#DDE1FF",
    "onPrimaryFixed": "#08164B",
    "primaryFixedDim": "#B9C3FF",
    "onPrimaryFixedVariant": "#384379",
    "secondaryFixed": "#DFE1F9",
    "onSecondaryFixed": "#171B2C",
    "secondaryFixedDim": "#C3C5DD",
    "onSecondaryFixedVariant": "#424659",
    "tertiaryFixed": "#FFD7F3",
    "onTertiaryFixed": "#2D1228",
    "tertiaryFixedDim": "#E5BAD8",
    "onTertiaryFixedVariant": "#5C3C55",
    "surfaceDim": "#121318",
    "surfaceBright": "#38393F",
    "surfaceContainerLowest": "#0D0E13",
    "surfaceContainerLow": "#1B1B21",
    "surfaceContainer": "#1F1F25",
    "surfaceContainerHigh": "#292A2F",
    "surfaceContainerHighest": "#34343A"
  }
}
```

---

## 9. How to apply a theme

1. In the **File Manager**, tap your `.gth` file.
2. A sheet appears with two options:
   - **Edit** (✎) — open the visual theme editor.
   - **Apply** — make this theme the active one (changes the whole app instantly).
3. Tap **Apply**.

---

## 10. Handy rules

- **Color format:** always `#RRGGBBAA` (8 hex digits). `#RRGGBB` with 6 digits is usually tolerated.
- **`##00000000`** means fully transparent.
- **Pairs:** in `material3`, whenever you change a color also update its `on...` counterpart for readable contrast.
- **Missing keys are filled automatically** from the built-in default theme, so you never need to write every key — but writing them lets you control everything.
- You can **edit the `.gth` file as plain text** to set keys that the visual editor doesn't show yet (e.g. `wholeBackground`).

Now go make your own theme. If you got this far, you know everything there is to know. 🎨
