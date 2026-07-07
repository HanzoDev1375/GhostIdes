package ir.hanzodev1375.ghostide.codeeditors.stringres;

import android.widget.Toast;

import androidx.annotation.NonNull;

import io.github.rosemoe.sora.event.InterceptTarget;
import io.github.rosemoe.sora.event.LongPressEvent;
import io.github.rosemoe.sora.text.Cursor;
import io.github.rosemoe.sora.widget.CodeEditor;

import ir.hanzodev1375.ghostide.codeeditors.R;
import ir.hanzodev1375.ghostide.codeeditors.langs.java.JavaLanguage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * وقتی روی متن سلکت‌شده در یک فایل جاوا لانگ‌کلیک بشه، دیالوگ «استخراج رشته به strings.xml» رو نشون
 * میده. بعد از تایید، یک <string name="..."> به strings.xml ماژول فایل فعلی اضافه میشه و متن
 * انتخاب‌شده داخل ادیتور با R.string.NAME جایگزین میشه.
 *
 * <p>معماری این کلاس دقیقاً از الگوی WebColorIde / ImagePreviewIde پیروی می‌کنه.
 *
 * @author Ghost
 */
public final class StringResourceExtractorIde {

  private final CodeEditor editor;
  private volatile String currentFilePath;

  public StringResourceExtractorIde(@NonNull CodeEditor editor) {
    this.editor = editor;
  }

  public void setCurrentFilePath(String filePath) {
    this.currentFilePath = filePath;
  }

  public void attach() {
    editor.subscribeEvent(
        LongPressEvent.class,
        (event, unsubscribe) -> {
          if (!isJavaLanguage()) return;

          Cursor cursor = editor.getCursor();
          if (!cursor.isSelected()) return;

          int idx = event.getIndex();
          if (idx < cursor.getLeft() || idx > cursor.getRight()) return;

          event.intercept(InterceptTarget.TARGET_EDITOR);
          editor.post(this::showExtractDialog);
        });
  }

  /** حالا که کلاس واقعی رو می‌دونیم (langs.java.JavaLanguage توی همین ماژول)، مستقیم instanceof. */
  private boolean isJavaLanguage() {
    return editor.getEditorLanguage() instanceof JavaLanguage;
  }

  // ── نمایش دیالوگ ──────────────────────────────────────────────────────────

  private void showExtractDialog() {
    try {
      Cursor cursor = editor.getCursor();
      int leftLine = cursor.getLeftLine();
      int rightLine = cursor.getRightLine();

      if (leftLine != rightLine) {
        Toast.makeText(
                editor.getContext(),
                editor.getContext().getString(R.string.extract_string_multiline_error),
                Toast.LENGTH_SHORT)
            .show();
        return;
      }

      int line = leftLine;
      int leftCol = cursor.getLeftColumn();
      int rightCol = cursor.getRightColumn();
      String lineText = editor.getText().getLineString(line);
      String selected = editor.getText().subContent(line, leftCol, line, rightCol).toString();

      int extractStartCol = leftCol;
      int extractEndCol = rightCol;
      String value;

      boolean selectionHasQuotes =
          selected.length() >= 2 && selected.startsWith("\"") && selected.endsWith("\"");
      if (selectionHasQuotes) {
        // خود کوتیشن‌ها هم انتخاب شدن؛ محتوای داخلشون رو می‌گیریم
        value = selected.substring(1, selected.length() - 1);
      } else {
        // اگه دقیقاً محتوای بین دو کوتیشن انتخاب شده (بدون خود "")، کوتیشن‌های اطراف رو هم
        // به محدوده‌ی جایگزینی اضافه می‌کنیم تا بعد از استخراج کد شکسته نشه
        boolean quoteBefore = leftCol > 0 && lineText.charAt(leftCol - 1) == '"';
        boolean quoteAfter = rightCol < lineText.length() && lineText.charAt(rightCol) == '"';
        if (quoteBefore && quoteAfter) {
          extractStartCol = leftCol - 1;
          extractEndCol = rightCol + 1;
        }
        value = selected;
      }

      if (value.trim().isEmpty()) {
        Toast.makeText(
                editor.getContext(),
                editor.getContext().getString(R.string.extract_string_empty_selection),
                Toast.LENGTH_SHORT)
            .show();
        return;
      }

      String suggestedName = suggestResourceName(value);
      int fLine = line;
      int fStartCol = extractStartCol;
      int fEndCol = extractEndCol;

      ExtractStringResourceDialog.show(
          editor.getContext(),
          suggestedName,
          value,
          (name, finalValue) ->
              extractToStringResource(fLine, fStartCol, fEndCol, name, finalValue));
    } catch (Exception e) {
      Toast.makeText(
              editor.getContext(),
              editor.getContext().getString(R.string.extract_string_error, e.getMessage()),
              Toast.LENGTH_LONG)
          .show();
    }
  }

  private void extractToStringResource(
      int line, int startCol, int endCol, String rawName, String value) {
    try {
      if (!editor.isEditable()) return;

      if (currentFilePath == null) {
        Toast.makeText(
                editor.getContext(),
                editor.getContext().getString(R.string.extract_string_error),
                Toast.LENGTH_LONG)
            .show();
        return;
      }

      File stringsXml = resolveStringsXmlFile();
      if (stringsXml == null) {
        Toast.makeText(
                editor.getContext(),
                editor
                    .getContext()
                    .getString(R.string.extract_string_invalid_path, currentFilePath),
                Toast.LENGTH_LONG)
            .show();
        return;
      }

      String resourceName = writeStringResource(stringsXml, sanitizeResourceName(rawName), value);

      int startOffset = editor.getText().getIndexer().getCharIndex(line, startCol);
      int endOffset = editor.getText().getIndexer().getCharIndex(line, endCol);
      String replacement = "R.string." + resourceName;

      editor.getText().beginBatchEdit();
      try {
        editor.getText().replace(startOffset, endOffset, replacement);
      } finally {
        editor.getText().endBatchEdit();
      }

      editor.setSelection(line, startCol + replacement.length());
      editor.invalidate();

      Toast.makeText(
              editor.getContext(),
              editor.getContext().getString(R.string.extract_string_success, resourceName),
              Toast.LENGTH_SHORT)
          .show();
    } catch (Exception e) {
      Toast.makeText(
              editor.getContext(),
              editor.getContext().getString(R.string.extract_string_error, e.getMessage()),
              Toast.LENGTH_LONG)
          .show();
    }
  }

  /**
   * ماژول‌روت رو از روی مسیر فایل جاوای فعلی پیدا می‌کنه (همه‌چیز قبل از اولین «/src/»)، بعد دقیقاً
   * با همون الگوریتم StringsTranslatorSheet.findStringsXml (جستجوی بازگشتی برای یه strings.xml داخل
   * پوشه‌ی values، با رد کردن build/.git/پوشه‌های نقطه‌دار) دنبال strings.xml موجود می‌گرده. اگه
   * چیزی پیدا نشد، مسیر پیش‌فرض src/main/res/values/strings.xml رو برمی‌گردونه تا از نو ساخته بشه.
   */
  private File resolveStringsXmlFile() {
    if (currentFilePath == null) return null;
    String normalized = currentFilePath.replace('\\', '/');
    int srcIdx = normalized.indexOf("/src/");
    if (srcIdx < 0) return null;
    File moduleRoot = new File(normalized.substring(0, srcIdx));
    File found = findValuesStringsXml(moduleRoot);
    if (found != null) return found;
    return new File(moduleRoot, "src/main/res/values/strings.xml");
  }

  private File findValuesStringsXml(File dir) {
    if (dir == null || !dir.exists()) return null;
    if (dir.isFile()) {
      if (dir.getName().equals("strings.xml")
          && dir.getParentFile() != null
          && dir.getParentFile().getName().equals("values")) {
        return dir;
      }
      return null;
    }
    String name = dir.getName();
    if (name.equals("build") || name.equals(".git") || name.startsWith(".")) return null;
    File[] children = dir.listFiles();
    if (children == null) return null;
    for (File child : children) {
      File found = findValuesStringsXml(child);
      if (found != null) return found;
    }
    return null;
  }

  private String writeStringResource(File file, String baseName, String value) throws IOException {
    String content;
    if (file.exists()) {
      content = readFile(file);
    } else {
      File parent = file.getParentFile();
      if (parent != null && !parent.exists()) {
        parent.mkdirs();
      }
      content = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n</resources>\n";
    }

    String uniqueName = baseName;
    int suffix = 2;
    while (containsResourceName(content, uniqueName)) {
      uniqueName = baseName + "_" + suffix;
      suffix++;
    }

    String newEntry =
        "    <string name=\"" + uniqueName + "\">" + escapeXmlValue(value) + "</string>\n";

    int closeTagIndex = content.lastIndexOf("</resources>");
    String updated =
        closeTagIndex >= 0
            ? content.substring(0, closeTagIndex) + newEntry + content.substring(closeTagIndex)
            : content + newEntry;

    writeFile(file, updated);
    return uniqueName;
  }

  private boolean containsResourceName(String xmlContent, String name) {
    Pattern p = Pattern.compile("<string\\s+name=\"" + Pattern.quote(name) + "\"");
    return p.matcher(xmlContent).find();
  }

  private String sanitizeResourceName(String raw) {
    String s = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    s = s.replaceAll("[^a-z0-9_]+", "_");
    s = s.replaceAll("_+", "_");
    s = s.replaceAll("^_+|_+$", "");
    if (s.isEmpty() || Character.isDigit(s.charAt(0))) {
      s = "str_" + s;
    }
    return s;
  }

  private String suggestResourceName(String value) {
    String base = sanitizeResourceName(value);
    if (base.length() > 40) {
      base = base.substring(0, 40).replaceAll("_+$", "");
    }
    return base.isEmpty() ? "extracted_string" : base;
  }

  private String escapeXmlValue(String raw) {
    if (raw == null) return "";
    return raw.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;");
  }

  private String readFile(File file) throws IOException {
    StringBuilder sb = new StringBuilder();
    try (InputStreamReader reader =
        new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
      char[] buffer = new char[4096];
      int read;
      while ((read = reader.read(buffer)) != -1) {
        sb.append(buffer, 0, read);
      }
    }
    return sb.toString();
  }

  private void writeFile(File file, String content) throws IOException {
    try (OutputStreamWriter writer =
        new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
      writer.write(content);
    }
  }
}
