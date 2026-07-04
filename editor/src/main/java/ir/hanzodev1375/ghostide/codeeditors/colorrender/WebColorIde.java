package ir.hanzodev1375.ghostide.codeeditors.colorrender;

import android.content.Context;
import android.graphics.Color;
import android.widget.Toast;
import androidx.annotation.NonNull;
import io.github.rosemoe.sora.event.ContentChangeEvent;
import io.github.rosemoe.sora.event.InlayHintClickEvent;
import io.github.rosemoe.sora.lang.styling.color.ConstColor;
import io.github.rosemoe.sora.lang.styling.inlayHint.ColorInlayHint;
import io.github.rosemoe.sora.lang.styling.inlayHint.InlayHintsContainer;
import io.github.rosemoe.sora.text.CharPosition;
import io.github.rosemoe.sora.widget.CodeEditor;
import ir.hanzodev1375.ghostide.codeeditors.colorrender.model.colorrepo.ColorNameRepository;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebColorIde {

  // الگوی قبلی برای HEX، rgb، rgba
  private static final String BASIC_PATTERN =
      "(?:(#(?:[0-9a-fA-F]{3}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8}))\\b"
          + "|rgb\\((\\d{1,3})\\s*,\\s*(\\d{1,3})\\s*,\\s*(\\d{1,3})\\s*\\)"
          + "|rgba\\((\\d{1,3})\\s*,\\s*(\\d{1,3})\\s*,\\s*(\\d{1,3})\\s*,\\s*([\\d.]+)\\s*\\))";

  private final CodeEditor editor;
  private final ColorNameRepository colorNameRepo;
  private final Pattern fullColorPattern;

  public WebColorIde(@NonNull CodeEditor editor) {
    this.editor = editor;
    Context ctx = editor.getContext();
    this.colorNameRepo = ColorNameRepository.getInstance(ctx);
    this.fullColorPattern = buildFullColorPattern();
  }

  private Pattern buildFullColorPattern() {
    String namePattern = colorNameRepo.getNamePattern().pattern();

    String combined = BASIC_PATTERN + "|\\b(" + namePattern + ")\\b";
    return Pattern.compile(combined, Pattern.CASE_INSENSITIVE);
  }

  public void attach() {
    editor.subscribeEvent(ContentChangeEvent.class, (event, unsubscribe) -> updateColorHints());
    editor.subscribeEvent(
        InlayHintClickEvent.class,
        (event, unsubscribe) -> {
          if (!(event.getInlayHint() instanceof ColorInlayHint)) return;
          ColorInlayHint hint = (ColorInlayHint) event.getInlayHint();

          String colorText = findColorAt(hint.getLine(), hint.getColumn());
          if (colorText == null) return;

          Context ctx = editor.getContext();
          Toast.makeText(ctx, colorText, Toast.LENGTH_SHORT).show();

          int initialColor;
          try {
            initialColor = Color.parseColor(colorText);
          } catch (Exception e) {
            initialColor = Color.WHITE;
          }

          ColorPickerBottomSheetDialog.show(
              ctx,
              initialColor,
              newColor -> replaceColorAt(hint.getLine(), hint.getColumn(), newColor));
        });
  }

  private String findColorAt(int line, int column) {
    try {
      String[] lines = editor.getText().toString().split("\n");
      if (line < 0 || line >= lines.length) return null;
      String text = lines[line];
      if (column < 0 || column >= text.length()) return null;
      Matcher m = fullColorPattern.matcher(text);
      while (m.find()) {
        if (m.start() <= column && column <= m.end()) {
          String matched = text.substring(m.start(), m.end());
          // اگر نام رنگ بود، معادل HEX آن را برگردان
          if (colorNameRepo.getNameToHex().containsKey(matched.toLowerCase())) {
            return colorNameRepo.getHexForName(matched);
          }
          return matched;
        }
      }
    } catch (Exception ignored) {
    }
    return null;
  }

  private void replaceColorAt(int line, int column, int newColorArgb) {
    try {
      if (!editor.isEditable()) return;

      int cursorOffset =
          editor.getCursor().getLeftLine() >= 0
              ? editor
                  .getText()
                  .getIndexer()
                  .getCharIndex(
                      editor.getCursor().getLeftLine(), editor.getCursor().getLeftColumn())
              : 0;

      String oldLine = editor.getText().getLineString(line);
      if (column < 0 || column >= oldLine.length()) return;

      Matcher m = fullColorPattern.matcher(oldLine);
      int matchStart = -1, matchEnd = -1;
      String originalColorText = null;
      while (m.find()) {
        if (m.start() <= column && column <= m.end()) {
          matchStart = m.start();
          matchEnd = m.end();
          originalColorText = oldLine.substring(matchStart, matchEnd);
          break;
        }
      }
      if (matchStart == -1 || originalColorText == null) return;

      String replacement = buildColorReplacement(originalColorText, newColorArgb);

      int startOffset = editor.getText().getIndexer().getCharIndex(line, matchStart);
      int endOffset = editor.getText().getIndexer().getCharIndex(line, matchEnd);

      editor.getText().beginBatchEdit();
      try {
        editor.getText().replace(startOffset, endOffset, replacement);
      } finally {
        editor.getText().endBatchEdit();
      }

      if (cursorOffset <= editor.getText().length()) {
        CharPosition newPos = editor.getText().getIndexer().getCharPosition(cursorOffset);
        editor.getCursor().set(newPos.line, newPos.column);
      }
      editor.invalidate();
    } catch (Exception e) {
      e.printStackTrace();
      Toast.makeText(editor.getContext(), "خطا: " + e.getMessage(), Toast.LENGTH_LONG).show();
    }
  }

  private String buildColorReplacement(String original, int colorArgb) {
    int a = Color.alpha(colorArgb);
    int r = Color.red(colorArgb);
    int g = Color.green(colorArgb);
    int b = Color.blue(colorArgb);

    // اگر متن اصلی یک نام رنگ است، با HEX جایگزین کن
    if (colorNameRepo.getNameToHex().containsKey(original.toLowerCase())) {
      if (a == 255) {
        return String.format("#%02X%02X%02X", r, g, b);
      } else {
        return String.format("#%02X%02X%02X%02X", a, r, g, b);
      }
    }

    // باقی موارد بر اساس فرمت اصلی
    if (original.startsWith("#")) {
      int len = original.length() - 1;
      boolean includeAlpha = (len == 4 || len == 8) || (len <= 6 && a != 255);
      if (includeAlpha) {
        return String.format("#%02X%02X%02X%02X", a, r, g, b);
      } else {
        return String.format("#%02X%02X%02X", r, g, b);
      }
    } else if (original.startsWith("0x") || original.startsWith("0X")) {
      boolean includeAlpha =
          (original.length() - 2 == 8) || (original.length() - 2 <= 6 && a != 255);
      if (includeAlpha) {
        return String.format("0x%02X%02X%02X%02X", a, r, g, b);
      } else {
        return String.format("0x%02X%02X%02X", r, g, b);
      }
    } else if (original.startsWith("rgb(")) {
      if (a == 255) {
        return String.format("rgb(%d, %d, %d)", r, g, b);
      } else {
        return String.format("rgba(%d, %d, %d, %.2f)", r, g, b, a / 255f);
      }
    } else if (original.startsWith("rgba(")) {
      return String.format("rgba(%d, %d, %d, %.2f)", r, g, b, a / 255f);
    }

    // Fallback
    return String.format("#%06X", (0xFFFFFF & colorArgb));
  }

  private void updateColorHints() {
    String text = editor.getText().toString();
    Matcher matcher = fullColorPattern.matcher(text);
    InlayHintsContainer container = new InlayHintsContainer();

    while (matcher.find()) {
      int startOffset = matcher.start();
      CharPosition pos = textPosition(startOffset);
      String matched = matcher.group();

      // بررسی نام رنگ
      if (colorNameRepo.getNameToHex().containsKey(matched.toLowerCase())) {
        String hex = colorNameRepo.getHexForName(matched);
        int color = Color.parseColor(hex);
        container.add(new ColorInlayHint(pos.line, pos.column, new ConstColor(color), null));
        continue;
      }

      // HEX
      if (matcher.group(1) != null) {
        String hex = matcher.group(1);
        if (hex.length() == 4) {
          hex = expandShortHex(hex);
        }
        container.add(new ColorInlayHint(pos.line, pos.column, new ConstColor(hex), null));
      }
      // rgb
      else if (matcher.group(2) != null) {
        int r = clamp(parseIntSafe(matcher.group(2)), 0, 255);
        int g = clamp(parseIntSafe(matcher.group(3)), 0, 255);
        int b = clamp(parseIntSafe(matcher.group(4)), 0, 255);
        int color = 0xFF000000 | (r << 16) | (g << 8) | b;
        container.add(new ColorInlayHint(pos.line, pos.column, new ConstColor(color), null));
      }
      // rgba
      else if (matcher.group(5) != null) {
        int r = clamp(parseIntSafe(matcher.group(5)), 0, 255);
        int g = clamp(parseIntSafe(matcher.group(6)), 0, 255);
        int b = clamp(parseIntSafe(matcher.group(7)), 0, 255);
        float aFloat = parseFloatSafe(matcher.group(8));
        int a = clamp(Math.round(aFloat * 255), 0, 255);
        int color = (a << 24) | (r << 16) | (g << 8) | b;

        container.add(new ColorInlayHint(pos.line, pos.column, new ConstColor(color), null));
      }
    }

    editor.setInlayHints(container);
    editor.registerInlayHintRenderer(
        io.github.rosemoe.sora.graphics.inlayHint.ColorInlayHintRenderer.Companion
            .getDefaultInstance());
  }

  private String expandShortHex(String hex) {
    StringBuilder sb = new StringBuilder("#");
    for (int i = 1; i < hex.length(); i++) {
      char c = hex.charAt(i);
      sb.append(c).append(c);
    }
    return sb.toString();
  }

  private int clamp(int value, int min, int max) {
    return Math.max(min, Math.min(max, value));
  }

  private int parseIntSafe(String s) {
    try {
      return Integer.parseInt(s);
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  private float parseFloatSafe(String s) {
    try {
      return Float.parseFloat(s);
    } catch (NumberFormatException e) {
      return 1f;
    }
  }

  private CharPosition textPosition(int offset) {
    return editor.getText().getIndexer().getCharPosition(offset);
  }
}
