package ir.hanzodev1375.ghostide.jgit.diff;

import android.content.Context;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.graphics.*;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.*;
import android.view.*;
import android.widget.*;
import ir.hanzodev1375.components.views.GhostToast;
import java.util.*;
import java.util.concurrent.*;
import android.text.*;

public class GitDiffViewer extends View {
  private List<DiffLine> diffLines = new ArrayList<>();
  private DiffTheme theme = new DiffTheme();
  private MultiLanguageHighlighter highlighter;
  private SparseArray<List<SyntaxHighlighter.HighlightSpan>> highlightCache;
  private ExecutorService executor;
  private Handler mainHandler;
  private SparseArray<String> lineLanguageMap = new SparseArray<>();

  private TextPaint textPaint;
  private TextPaint lineNumberPaint;
  private Paint bgPaint, lineNumberBgPaint, selectionPaint;
  private float lineHeight = 60f, textSize = 32f, lineNumberWidth = 80f, scaleFactor = 1f;
  private float scrollX = 0f, scrollY = 0f;
  private float[] baseLineWidths;
  private float baseMaxWidth = 0f;
  private float baseTextSize = 32f;

  private ScaleGestureDetector scaleDetector;
  private OverScroller scroller;
  private VelocityTracker velocityTracker;
  private float lastX, lastY;
  private int ptrId = -1;

  private boolean isSelecting = false;
  private int selectionStartLine = -1, selectionEndLine = -1;
  private int selectionStartChar = -1, selectionEndChar = -1;
  private RectF selectionRect = new RectF();
  private Handler longPressHandler;
  private Runnable longPressRunnable;
  private boolean isLongPress = false;
  private float downX, downY;
  private PopupWindow popupWindow;
  private LinearLayout popupLayout;

  public GitDiffViewer(Context context) {
    super(context);
    init(context);
  }

  public GitDiffViewer(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }

  private void init(Context context) {
    textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    textPaint.setTypeface(Typeface.MONOSPACE);
    textPaint.setTextSize(textSize);
    baseTextSize = textSize;

    lineNumberPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    lineNumberPaint.setTypeface(Typeface.MONOSPACE);
    lineNumberPaint.setTextAlign(Paint.Align.CENTER);

    bgPaint = new Paint();
    lineNumberBgPaint = new Paint();
    selectionPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    selectionPaint.setColor(Color.argb(100, 50, 150, 255));

    scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
    scroller = new OverScroller(context);
    setFocusable(true);
    setLayerType(LAYER_TYPE_HARDWARE, null);

    highlighter = new MultiLanguageHighlighter();
    highlightCache = new SparseArray<>();
    executor = Executors.newSingleThreadExecutor();
    mainHandler = new Handler(Looper.getMainLooper());

    longPressHandler = new Handler();
    longPressRunnable =
        () -> {
          isLongPress = true;
          startSelection(downX, downY);
          showPopupWindow();
        };
  }

  private String extractFileNameFromHeader(String line) {
    int idx = line.indexOf(" a/");
    if (idx == -1) idx = line.indexOf(" b/");
    if (idx == -1) return null;
    String path = line.substring(idx + 3);
    int space = path.indexOf(' ');
    if (space > 0) path = path.substring(0, space);
    int lastSlash = path.lastIndexOf('/');
    if (lastSlash >= 0) path = path.substring(lastSlash + 1);
    return path;
  }

  public void parseDiffOutput(String diff) {
    List<DiffLine> lines = new ArrayList<>();
    String[] split = diff.split("\n");
    int oldNum = 0, newNum = 0;
    lineLanguageMap.clear();
    String currentLanguageForFile = "java";

    for (String line : split) {
      if (line.trim().isEmpty()) continue;
      if (line.startsWith("diff --git") || line.startsWith("--- ") || line.startsWith("+++ ")) {
        String fileName = extractFileNameFromHeader(line);
        if (fileName != null) {
          String newLang = MultiLanguageHighlighter.detectLanguageFromFileName(fileName);
          if (newLang != null) {
            currentLanguageForFile = newLang;
          }
        }
        lines.add(new DiffLine(line, DiffLine.LineType.HEADER, 0));
        lineLanguageMap.put(lines.size() - 1, currentLanguageForFile);
      } else if (line.startsWith("@@")) {
        lines.add(new DiffLine(line, DiffLine.LineType.HEADER, 0));
        lineLanguageMap.put(lines.size() - 1, currentLanguageForFile);
        int[] nums = extractNums(line);
        oldNum = nums[0] - 1;
        newNum = nums[1] - 1;
      } else if (line.startsWith("+")) {
        lines.add(new DiffLine(line, DiffLine.LineType.ADDED, ++newNum));
        lineLanguageMap.put(lines.size() - 1, currentLanguageForFile);
      } else if (line.startsWith("-")) {
        lines.add(new DiffLine(line, DiffLine.LineType.REMOVED, ++oldNum));
        lineLanguageMap.put(lines.size() - 1, currentLanguageForFile);
      } else if (line.startsWith(" ")) {
        lines.add(new DiffLine(line, DiffLine.LineType.NORMAL, ++newNum));
        lineLanguageMap.put(lines.size() - 1, currentLanguageForFile);
        oldNum++;
      }
    }

    setDiffLines(lines);
  }

  private int[] extractNums(String h) {
    try {
      String[] p = h.split(" ");
      return new int[] {
        Integer.parseInt(p[1].substring(1).split(",")[0]),
        Integer.parseInt(p[2].substring(1).split(",")[0])
      };
    } catch (Exception e) {
      return new int[] {1, 1};
    }
  }

  public void setDiffLines(List<DiffLine> lines) {
    this.diffLines = lines != null ? lines : new ArrayList<>();
    baseLineWidths = null;
    highlightCache.clear();
    scrollX = 0;
    scrollY = 0;
    ensureBaseWidths();
    invalidate();
  }

  private void ensureBaseWidths() {
    if (diffLines.isEmpty()) return;
    if (baseLineWidths != null && baseLineWidths.length == diffLines.size()) return;
    baseLineWidths = new float[diffLines.size()];
    textPaint.setTextSize(baseTextSize);
    float max = 0f;
    for (int i = 0; i < diffLines.size(); i++) {
      float w = textPaint.measureText(diffLines.get(i).getText());
      baseLineWidths[i] = w;
      if (w > max) max = w;
    }
    baseMaxWidth = max;
    textPaint.setTextSize(textSize * scaleFactor);
  }

  private float getScaledMaxTextWidth() {
    return baseMaxWidth * scaleFactor;
  }

  private void requestHighlight(final int lineIndex, String text, DiffLine.LineType type) {
    if (highlightCache.get(lineIndex) != null) return;
    final String lang = lineLanguageMap.get(lineIndex, "java");
    executor.execute(
        () -> {
          highlighter.highlight(
              text,
              type,
              lang,
              spans -> {
                mainHandler.post(
                    () -> {
                      highlightCache.put(lineIndex, spans);
                      float lh = lineHeight * scaleFactor;
                      float top = lineIndex * lh - scrollY;
                      if (top + lh > 0 && top < getHeight()) invalidate();
                    });
              });
        });
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    if (diffLines.isEmpty()) return;

    float lh = lineHeight * scaleFactor;
    float ts = textSize * scaleFactor;
    float lnw = lineNumberWidth * scaleFactor;

    textPaint.setTextSize(ts);
    lineNumberPaint.setTextSize(ts * 0.85f);

    int start = (int) (scrollY / lh);
    if (start < 0) start = 0;
    int end = start + (int) (getHeight() / lh) + 2;
    if (end > diffLines.size()) end = diffLines.size();

    canvas.drawColor(theme.getNormalLineBg());

    for (int i = start; i < end; i++) {
      DiffLine line = diffLines.get(i);
      float top = i * lh - scrollY;
      float bottom = top + lh;

      bgPaint.setColor(getBgColor(line));
      canvas.drawRect(0, top, getWidth(), bottom, bgPaint);

      float numRight = lnw - scrollX;
      if (numRight > 0) {
        lineNumberBgPaint.setColor(theme.getLineNumberBg());
        canvas.drawRect(0, top, numRight, bottom, lineNumberBgPaint);
        if (line.getLineNumber() > 0 && line.getType() != DiffLine.LineType.HEADER) {
          lineNumberPaint.setColor(theme.getLineNumberTextColor());
          canvas.drawText(
              String.valueOf(line.getLineNumber()),
              numRight / 2,
              bottom - lh * 0.2f,
              lineNumberPaint);
        }
      }

      float textX = lnw + 20 * scaleFactor - scrollX;
      float textY = bottom - lh * 0.2f;
      String text = line.getText();

      List<SyntaxHighlighter.HighlightSpan> spans = highlightCache.get(i);
      if (spans == null) {
        textPaint.setColor(getEffectiveTextColor(line));
        canvas.drawText(text, textX, textY, textPaint);
        requestHighlight(i, text, line.getType());
      } else {
        float x = textX;
        int last = 0;
        for (SyntaxHighlighter.HighlightSpan span : spans) {
          if (span.start > last) {
            x =
                drawTextChunk(
                    canvas, text, last, span.start, getEffectiveTextColor(line), x, textY);
          }
          x = drawTextChunk(canvas, text, span.start, span.end, span.color, x, textY);
          last = span.end;
        }
        if (last < text.length()) {
          drawTextChunk(canvas, text, last, text.length(), getEffectiveTextColor(line), x, textY);
        }
      }

      if (isSelecting && i >= selectionStartLine && i <= selectionEndLine) {
        drawSelection(canvas, i, top, bottom, lnw);
      }
    }
  }

  private float drawTextChunk(
      Canvas canvas, String text, int start, int end, int color, float x, float y) {
    textPaint.setColor(color);
    canvas.drawText(text, start, end, x, y, textPaint);
    return x + textPaint.measureText(text, start, end);
  }

  private void drawSelection(Canvas canvas, int lineIndex, float top, float bottom, float lnw) {
    float textStartX = lnw + 20 * scaleFactor - scrollX;
    String text = diffLines.get(lineIndex).getText();
    int startChar = (lineIndex == selectionStartLine) ? selectionStartChar : 0;
    int endChar = (lineIndex == selectionEndLine) ? selectionEndChar : text.length();
    if (startChar >= text.length() || endChar < 0) return;
    float selStartX = textStartX + measureTextWidth(text.substring(0, startChar));
    float selEndX = textStartX + measureTextWidth(text.substring(0, endChar));
    selectionRect.set(selStartX, top, selEndX, bottom);
    canvas.drawRect(selectionRect, selectionPaint);
  }

  private float measureTextWidth(String s) {
    if (s.isEmpty()) return 0;
    return textPaint.measureText(s);
  }

  private int getBgColor(DiffLine line) {
    switch (line.getType()) {
      case ADDED:
        return theme.getAddedLineBg();
      case REMOVED:
        return theme.getRemovedLineBg();
      case HEADER:
        return theme.getHeaderLineBg();
      default:
        return theme.getNormalLineBg();
    }
  }

  private int getTextColor(DiffLine line) {
    switch (line.getType()) {
      case ADDED:
        return theme.getAddedTextColor();
      case REMOVED:
        return theme.getRemovedTextColor();
      case HEADER:
        return theme.getHeaderTextColor();
      default:
        return theme.getNormalTextColor();
    }
  }

  private int getEffectiveTextColor(DiffLine line) {
    DiffLine.LineType type = line.getType();
    if (type == DiffLine.LineType.ADDED || type == DiffLine.LineType.REMOVED) {
      return theme.getNormalTextColor();
    }
    return getTextColor(line);
  }

  private void startSelection(float x, float y) {
    float lh = lineHeight * scaleFactor;
    int lineIndex = (int) ((y + scrollY) / lh);
    if (lineIndex >= 0 && lineIndex < diffLines.size()) {
      isSelecting = true;
      selectionStartLine = selectionEndLine = lineIndex;
      float lnw = lineNumberWidth * scaleFactor;
      float textStartX = lnw + 20 * scaleFactor;
      String text = diffLines.get(lineIndex).getText();
      int charIndex = getCharIndexAtPosition(text, x + scrollX - textStartX);
      selectionStartChar = selectionEndChar = Math.min(charIndex, text.length());
      invalidate();
    }
  }

  private void updateSelection(float x, float y) {
    if (!isSelecting) return;
    float lh = lineHeight * scaleFactor;
    int lineIndex = (int) ((y + scrollY) / lh);
    if (lineIndex < 0 || lineIndex >= diffLines.size()) return;
    float lnw = lineNumberWidth * scaleFactor;
    float textStartX = lnw + 20 * scaleFactor;
    String text = diffLines.get(lineIndex).getText();
    int charIndex = getCharIndexAtPosition(text, x + scrollX - textStartX);
    charIndex = Math.min(charIndex, text.length());
    if (selectionStartLine < lineIndex
        || (selectionStartLine == lineIndex && selectionStartChar <= charIndex)) {
      selectionEndLine = lineIndex;
      selectionEndChar = charIndex;
    } else {
      selectionEndLine = selectionStartLine;
      selectionEndChar = selectionStartChar;
      selectionStartLine = lineIndex;
      selectionStartChar = charIndex;
    }
    invalidate();
  }

  private int getCharIndexAtPosition(String text, float x) {
    if (x <= 0 || text.isEmpty()) return 0;
    float currentX = 0;
    for (int i = 0; i < text.length(); i++) {
      float w = textPaint.measureText(String.valueOf(text.charAt(i)));
      if (currentX + w / 2 > x) return i;
      currentX += w;
    }
    return text.length();
  }

  private void clearSelection() {
    isSelecting = false;
    selectionStartLine = selectionEndLine = -1;
    selectionStartChar = selectionEndChar = -1;
    dismissPopupWindow();
    invalidate();
  }

  private void selectAll() {
    if (diffLines.isEmpty()) return;
    isSelecting = true;
    selectionStartLine = 0;
    selectionStartChar = 0;
    selectionEndLine = diffLines.size() - 1;
    selectionEndChar = diffLines.get(selectionEndLine).getText().length();
    invalidate();
    GhostToast.makeText(getContext(), "همه متن انتخاب شد", GhostToast.LENGTH_SHORT).show();
  }

  private void copySelection() {
    String selectedText = getSelectedText();
    if (!selectedText.isEmpty()) {
      ClipboardManager clipboard =
          (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
      clipboard.setPrimaryClip(ClipData.newPlainText("diff_text", selectedText));
      GhostToast.makeText(getContext(), "کپی شد", GhostToast.LENGTH_SHORT).show();
    }
    clearSelection();
  }

  private String getSelectedText() {
    if (!isSelecting || diffLines.isEmpty()) return "";
    StringBuilder sb = new StringBuilder();
    for (int i = selectionStartLine; i <= selectionEndLine; i++) {
      String text = diffLines.get(i).getText();
      int start = (i == selectionStartLine) ? selectionStartChar : 0;
      int end = (i == selectionEndLine) ? selectionEndChar : text.length();
      if (start < end && start < text.length()) {
        sb.append(text.substring(start, Math.min(end, text.length())));
      }
      if (i < selectionEndLine) sb.append("\n");
    }
    return sb.toString();
  }

  private void showPopupWindow() {
    dismissPopupWindow();
    popupLayout = new LinearLayout(getContext());
    popupLayout.setOrientation(LinearLayout.HORIZONTAL);
    popupLayout.setBackgroundColor(Color.rgb(50, 50, 50));
    popupLayout.setPadding(20, 20, 20, 20);

    Button selectAllBtn = new Button(getContext());
    selectAllBtn.setText("انتخاب همه");
    selectAllBtn.setOnClickListener(v -> selectAll());

    Button copyBtn = new Button(getContext());
    copyBtn.setText("کپی");
    copyBtn.setOnClickListener(v -> copySelection());

    Button cancelBtn = new Button(getContext());
    cancelBtn.setText("بستن");
    cancelBtn.setOnClickListener(v -> clearSelection());

    LinearLayout.LayoutParams params =
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    params.setMargins(10, 0, 10, 0);
    popupLayout.addView(selectAllBtn, params);
    popupLayout.addView(copyBtn, params);
    popupLayout.addView(cancelBtn, params);

    popupWindow =
        new PopupWindow(
            popupLayout,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true);
    popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    popupWindow.setOutsideTouchable(true);
    popupWindow.setOnDismissListener(
        () -> {
          if (!isSelecting) clearSelection();
        });

    float lh = lineHeight * scaleFactor;
    int yOffset = (int) (selectionStartLine * lh - scrollY) - 150;
    popupWindow.showAtLocation(
        this, Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, Math.max(0, yOffset));
  }

  private void dismissPopupWindow() {
    if (popupWindow != null && popupWindow.isShowing()) popupWindow.dismiss();
    popupWindow = null;
    popupLayout = null;
  }

  public void applyDarkTheme() {
    theme.setDarkTheme();
    invalidate();
  }

  public void applyLightTheme() {
    theme.setLightTheme();
    invalidate();
  }

  public void applyMaterial3() {
    theme.applyMaterial3(getContext());
    invalidate();
  }

  public void setLanguage(String lang) {
    /* دیگر نیازی به این متد نیست، ولی برای سازگاری نگه می‌داریم */
  }

  private float maxScrollX() {
    float contentWidth = lineNumberWidth * scaleFactor + getScaledMaxTextWidth() + 40 * scaleFactor;
    return Math.max(0, contentWidth - getWidth());
  }

  private float maxScrollY() {
    return Math.max(0, diffLines.size() * lineHeight * scaleFactor - getHeight());
  }

  private void clampScroll() {
    if (scrollX < 0) scrollX = 0;
    if (scrollY < 0) scrollY = 0;
    float maxX = maxScrollX(), maxY = maxScrollY();
    if (scrollX > maxX) scrollX = maxX;
    if (scrollY > maxY) scrollY = maxY;
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    scaleDetector.onTouchEvent(event);
    if (velocityTracker == null) velocityTracker = VelocityTracker.obtain();
    velocityTracker.addMovement(event);

    switch (event.getActionMasked()) {
      case MotionEvent.ACTION_DOWN:
        downX = event.getX();
        downY = event.getY();
        ptrId = event.getPointerId(0);
        lastX = event.getX();
        lastY = event.getY();
        scroller.forceFinished(true);
        isLongPress = false;
        longPressHandler.postDelayed(longPressRunnable, 500);
        getParent().requestDisallowInterceptTouchEvent(true);
        return true;
      case MotionEvent.ACTION_MOVE:
        if (Math.hypot(event.getX() - downX, event.getY() - downY) > 20) {
          longPressHandler.removeCallbacks(longPressRunnable);
        }
        if (scaleDetector.isInProgress()) return true;
        if (isLongPress && isSelecting) {
          updateSelection(event.getX(), event.getY());
          return true;
        }
        int idx = event.findPointerIndex(ptrId);
        if (idx < 0) return true;
        float dx = lastX - event.getX(idx);
        float dy = lastY - event.getY(idx);
        lastX = event.getX(idx);
        lastY = event.getY(idx);
        scrollX += dx;
        scrollY += dy;
        clampScroll();
        invalidate();
        return true;
      case MotionEvent.ACTION_UP:
      case MotionEvent.ACTION_CANCEL:
        longPressHandler.removeCallbacks(longPressRunnable);
        if (isLongPress && isSelecting) return true;
        velocityTracker.computeCurrentVelocity(1000, 8000);
        float vx = velocityTracker.getXVelocity(ptrId);
        float vy = velocityTracker.getYVelocity(ptrId);
        scroller.fling(
            (int) scrollX,
            (int) scrollY,
            (int) -vx,
            (int) -vy,
            0,
            (int) maxScrollX(),
            0,
            (int) maxScrollY());
        velocityTracker.clear();
        ptrId = -1;
        getParent().requestDisallowInterceptTouchEvent(false);
        postInvalidateOnAnimation();
        return true;
    }
    return super.onTouchEvent(event);
  }

  @Override
  public void computeScroll() {
    if (scroller.computeScrollOffset()) {
      scrollX = scroller.getCurrX();
      scrollY = scroller.getCurrY();
      clampScroll();
      postInvalidateOnAnimation();
    }
    super.computeScroll();
  }

  private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
    private float focusX, focusY;

    @Override
    public boolean onScaleBegin(ScaleGestureDetector d) {
      focusX = d.getFocusX() + scrollX;
      focusY = d.getFocusY() + scrollY;
      scroller.forceFinished(true);
      return true;
    }

    @Override
    public boolean onScale(ScaleGestureDetector d) {
      float oldScale = scaleFactor;
      scaleFactor *= d.getScaleFactor();
      scaleFactor = Math.max(0.5f, Math.min(3f, scaleFactor));
      scrollX = focusX * (scaleFactor / oldScale) - d.getFocusX();
      scrollY = focusY * (scaleFactor / oldScale) - d.getFocusY();
      clampScroll();
      invalidate();
      return true;
    }
  }
}
