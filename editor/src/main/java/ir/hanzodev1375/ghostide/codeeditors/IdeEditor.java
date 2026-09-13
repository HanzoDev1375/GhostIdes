package ir.hanzodev1375.ghostide.codeeditors;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.util.AttributeSet;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import ir.hanzodev1375.components.sheet.customitemsheet.ui.DialogCompat;
import ir.hanzodev1375.ghostide.codeeditors.R;
import io.github.rosemoe.sora.widget.component.EditorDiagnosticTooltipWindow;
import ir.hanzodev1375.ghostide.codeeditors.langs.lsp.listener.GhostLspStatusListener;
import ir.hanzodev1375.ghostide.codeeditors.langs.lsp.LspInitParamsHook;
import ir.hanzodev1375.ghostide.codeeditors.ui.CustomEditorTextActionWindow;
import io.github.rosemoe.sora.event.ContentChangeEvent;
import io.github.rosemoe.sora.event.ScrollEvent;
import io.github.rosemoe.sora.graphics.inlayHint.GhostTextInlayHintRenderer;
import io.github.rosemoe.sora.lang.Language;
import io.github.rosemoe.sora.lang.styling.inlayHint.InlayHintsContainer;
import io.github.rosemoe.sora.lsp.editor.LspEditor;
import io.github.rosemoe.sora.lsp.editor.LspEditorStatus;
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion;
import io.github.rosemoe.sora.widget.component.EditorContextMenuCreator;
import io.github.rosemoe.sora.widget.component.EditorTextActionWindow;
import io.github.rosemoe.sora.widget.component.Magnifier;
import io.github.rosemoe.sora.widget.CodeEditor;
import ir.hanzodev1375.ghostide.codeeditors.ui.EditorContextMenu;
import ir.hanzodev1375.ghostide.codeeditors.colorrender.WebColorIde;
import ir.hanzodev1375.ghostide.codeeditors.langs.lsp.LspRouter;
import ir.hanzodev1375.ghostide.codeeditors.preview.ImagePreviewIde;
import ir.hanzodev1375.ghostide.codeeditors.preview.url.OnLinkClickEventListener;
import ir.hanzodev1375.ghostide.codeeditors.dependencychecker.GradleDependencyCheckerIde;
import ir.hanzodev1375.ghostide.codeeditors.dependencychecker.TomlDependencyCheckerIde;
import ir.hanzodev1375.ghostide.codeeditors.preview.url.UrlPreviewIde;
import ir.hanzodev1375.ghostide.codeeditors.preview.xmlattr.XmlAttrPreviewIde;
import ir.hanzodev1375.ghostide.codeeditors.setting.Constants;
import ir.hanzodev1375.ghostide.codeeditors.setting.PreferencesUtils;
import ir.hanzodev1375.ghostide.codeeditors.stringres.StringResourceExtractorIde;
import ir.hanzodev1375.ghostide.codeeditors.ui.CustomEditorAutoCompletion;
import ir.hanzodev1375.ghostide.codeeditors.ui.CustomEditorCompletionAdapter;
import ir.hanzodev1375.ghostide.codeeditors.ui.GhostDiagnosticTooltipLayout;
import ir.hanzodev1375.ghostide.codeeditors.ui.GhostTextCompletionManager;
import ir.hanzodev1375.ghostide.codeeditors.ui.power.PowerModeEffectManager;
import ir.hanzodev1375.ghostide.codeeditors.ui.power.custom.CustomEffect;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import android.graphics.Paint;
import android.view.KeyEvent;
import io.github.rosemoe.sora.event.DoubleClickEvent;
import io.github.rosemoe.sora.event.EditorKeyEvent;
import io.github.rosemoe.sora.event.InterceptTarget;
import io.github.rosemoe.sora.widget.style.LineNumberTipTextProvider;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class IdeEditor extends CodeEditor
    implements SharedPreferences.OnSharedPreferenceChangeListener {

  private PreferencesUtils setting;
  private WebColorIde webColorIde;
  private ImagePreviewIde imagePreviewIde;
  private PowerModeEffectManager mPowerModeEffectManager;
  private boolean powerModeEnabled = false;
  private UrlPreviewIde urlPreviewIde;
  private StringResourceExtractorIde stringresourceextractoride;
  private XmlAttrPreviewIde xmlAttrPreviewIde;
  private GradleDependencyCheckerIde gradleDependencyCheckerIde;
  private TomlDependencyCheckerIde tomlDependencyCheckerIde;
  private String currentFilePath;
  private volatile LspEditor lspEditor;
  @Nullable private GhostLspStatusListener lspStatusListener;
  private GhostTextCompletionManager ghostCompletionManager;
  private Runnable onSaveRequest;
  private Runnable onSearchRequest;
  private Runnable onGotoLineRequest;

  /** پیشوند کامنت تک خطی برای زبان ای شناخته شده، کلیدش پسوند فایل است (بدون نقطه). */
  private static final Map<String, String> COMMENT_PREFIX_BY_EXTENSION = new HashMap<>();

  static {
    COMMENT_PREFIX_BY_EXTENSION.put("java", "//");
    COMMENT_PREFIX_BY_EXTENSION.put("kt", "//");
    COMMENT_PREFIX_BY_EXTENSION.put("kts", "//");
    COMMENT_PREFIX_BY_EXTENSION.put("groovy", "//");
    COMMENT_PREFIX_BY_EXTENSION.put("gradle", "//");
    COMMENT_PREFIX_BY_EXTENSION.put("c", "//");
    COMMENT_PREFIX_BY_EXTENSION.put("h", "//");
    COMMENT_PREFIX_BY_EXTENSION.put("cpp", "//");
    COMMENT_PREFIX_BY_EXTENSION.put("hpp", "//");
    COMMENT_PREFIX_BY_EXTENSION.put("cc", "//");
    COMMENT_PREFIX_BY_EXTENSION.put("swift", "//");
    COMMENT_PREFIX_BY_EXTENSION.put("go", "//");
    COMMENT_PREFIX_BY_EXTENSION.put("rs", "//");
    COMMENT_PREFIX_BY_EXTENSION.put("js", "//");
    COMMENT_PREFIX_BY_EXTENSION.put("ts", "//");
    COMMENT_PREFIX_BY_EXTENSION.put("dart", "//");
    COMMENT_PREFIX_BY_EXTENSION.put("scala", "//");
    COMMENT_PREFIX_BY_EXTENSION.put("cs", "//");
    COMMENT_PREFIX_BY_EXTENSION.put("php", "//");
    COMMENT_PREFIX_BY_EXTENSION.put("sh", "#");
    COMMENT_PREFIX_BY_EXTENSION.put("bash", "#");
    COMMENT_PREFIX_BY_EXTENSION.put("py", "#");
    COMMENT_PREFIX_BY_EXTENSION.put("rb", "#");
    COMMENT_PREFIX_BY_EXTENSION.put("pl", "#");
    COMMENT_PREFIX_BY_EXTENSION.put("yaml", "#");
    COMMENT_PREFIX_BY_EXTENSION.put("yml", "#");
    COMMENT_PREFIX_BY_EXTENSION.put("ini", ";");
    COMMENT_PREFIX_BY_EXTENSION.put("cfg", ";");
    COMMENT_PREFIX_BY_EXTENSION.put("toml", "#");
    COMMENT_PREFIX_BY_EXTENSION.put("sql", "--");
    COMMENT_PREFIX_BY_EXTENSION.put("lua", "--");
    COMMENT_PREFIX_BY_EXTENSION.put("xml", "<!--");
    COMMENT_PREFIX_BY_EXTENSION.put("html", "<!--");
    COMMENT_PREFIX_BY_EXTENSION.put("htm", "<!--");
    COMMENT_PREFIX_BY_EXTENSION.put("css", "//");
    COMMENT_PREFIX_BY_EXTENSION.put("less", "//");
    COMMENT_PREFIX_BY_EXTENSION.put("scss", "//");
    COMMENT_PREFIX_BY_EXTENSION.put("json", "//");
    COMMENT_PREFIX_BY_EXTENSION.put("md", "#");
    COMMENT_PREFIX_BY_EXTENSION.put("properties", "#");
  }

  public void setOnSaveRequest(Runnable r) {
    onSaveRequest = r;
  }

  public void setOnSearchRequest(Runnable r) {
    onSearchRequest = r;
  }

  public void setOnGotoLineRequest(Runnable r) {
    onGotoLineRequest = r;
  }

  public IdeEditor(Context context) {
    super(context);
    init();
  }

  public IdeEditor(Context context, AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  private void init() {
    setting = new PreferencesUtils(getContext());
    ghostCompletionManager = new GhostTextCompletionManager(this);
    ghostCompletionManager.setEnabled(setting.enableGhostTextCompletion());
    registerInlayHintRenderer(GhostTextInlayHintRenderer.DefaultInstance);
    setWebIdeColor(true);
    imagePreviewIde = new ImagePreviewIde(this);
    imagePreviewIde.attach();
    mPowerModeEffectManager = new PowerModeEffectManager(this);
    var editorAutoCompletion = new CustomEditorAutoCompletion(this);
    urlPreviewIde = new UrlPreviewIde(this);
    urlPreviewIde.attach();
    stringresourceextractoride = new StringResourceExtractorIde(this);
    stringresourceextractoride.attach();
    xmlAttrPreviewIde = new XmlAttrPreviewIde(this);
    xmlAttrPreviewIde.attach();
    gradleDependencyCheckerIde = new GradleDependencyCheckerIde(this);
    gradleDependencyCheckerIde.attach();
    tomlDependencyCheckerIde = new TomlDependencyCheckerIde(this);
    tomlDependencyCheckerIde.attach();

    editorAutoCompletion.setAdapter(new CustomEditorCompletionAdapter());
    replaceComponent(EditorAutoCompletion.class, editorAutoCompletion);
    replaceComponent(EditorTextActionWindow.class, new CustomEditorTextActionWindow(this));
    replaceComponent(EditorContextMenuCreator.class, new EditorContextMenu(this));
    getComponent(EditorAutoCompletion.class)
        .setEnabledAnimation(setting.enableAutoCompleteWindowAnimation());
    getComponent(EditorDiagnosticTooltipWindow.class).setLayout(new GhostDiagnosticTooltipLayout());
    updateEditorTabSize();
    updateEditorStickyScroll();
    updateEditorHardWareAcceleration();
    updateEditorScrollBar();
    updateEditorMagnifier();
    updateEditorWordWrap();
    updateEditorLineNumber();
    updateEditorAutoCompletePanelAnimation();
    updateEditorDeleteEmptyLineFast();
    updateEditorDeleteTabs();
    updateEditorHighlightBracketPair();
    updateEditorLineSpacing();
    updateEditorCursorBlinkPeriod();
    applyNonPrintablePaintingFlags();
    updateEditorFontLigatures();
    updateEditorPinLineNumber();
    updateEditorMiniMap();
    updateEditorTypeFace();
    editorBinder();
    updateEditorPowerMode();
    updateEditorBlockLine();
    setCursorAnimationEnabled(true);
    setStickyTextSelection(true);
    setFirstLineNumberAlwaysVisible(true);
    setLineNumberAlign(Paint.Align.RIGHT);
    setLineNumberTipTextProvider(
        new LineNumberTipTextProvider() {
          @Override
          public String getCurrentText(CodeEditor editor) {
            return String.valueOf(editor.getCursor().getLeftLine() + 1);
          }
        });
    subscribeEvent(
        ContentChangeEvent.class,
        (ev, un) -> {
          if (isPowerModeEnabled() && getText().toString().length() > 0) {
            mPowerModeEffectManager.spawnEffectAtCursor();
          }
        });
    subscribeEvent(
        ScrollEvent.class,
        (ev, un) -> {
          if (mPowerModeEffectManager != null) {
            mPowerModeEffectManager.onEditorScrolled(
                ev.getStartX(), ev.getStartY(), ev.getEndX(), ev.getEndY());
          }
        });
    subscribeEvent(DoubleClickEvent.class, (ev, un) -> selectWord(ev.getLine(), ev.getColumn()));
    subscribeEvent(
        EditorKeyEvent.class,
        (ev, un) -> {
          if (ev.getEventType() != EditorKeyEvent.Type.DOWN || !ev.isCtrlPressed()) {
            return;
          }
          switch (ev.getKeyCode()) {
            case KeyEvent.KEYCODE_S:
              if (onSaveRequest != null) {
                onSaveRequest.run();
                ev.intercept(InterceptTarget.TARGET_EDITOR);
              }
              break;
            case KeyEvent.KEYCODE_F:
              if (onSearchRequest != null) {
                onSearchRequest.run();
                ev.intercept(InterceptTarget.TARGET_EDITOR);
              }
              break;
            case KeyEvent.KEYCODE_G:
              if (onGotoLineRequest != null) {
                onGotoLineRequest.run();
                ev.intercept(InterceptTarget.TARGET_EDITOR);
              }
              break;
            case KeyEvent.KEYCODE_SLASH:
              toggleCommentForCurrentLine();
              ev.intercept(InterceptTarget.TARGET_EDITOR);
              break;
          }
        });
  }

  @SuppressWarnings({"Deprecated", "all"})
  void editorBinder() {
    setHighlightCurrentLine(false);
    ensureSelectionVisible();
    setRenderFunctionCharacters(true);
    setDisableSoftKbdIfHardKbdAvailable(true);
  }

  @Override
  public void setEditorLanguage(@Nullable Language lang) {
    super.setEditorLanguage(lang);
    if (gradleDependencyCheckerIde != null) {
      gradleDependencyCheckerIde.refreshHighlights();
    }
    if (tomlDependencyCheckerIde != null) {
      tomlDependencyCheckerIde.refreshHighlights();
    }
  }

  public void setOnLinkClick(OnLinkClickEventListener call) {
    urlPreviewIde.setEvent(call);
  }

  public void setCutLine() {
    this.cutLine();
  }

  public void setDuplicateLine() {
    if (getCursor().isSelected()) {
      duplicateSelection();
    } else {
      duplicateLine();
    }
  }

  public void setSelectCurrentWord() {
    selectCurrentWord();
  }

  /** بر اساس تنظیم، هایلایت بلوک/خط فعلی و بلاکلاین کنار خط را روشن/خاموش میکند. */
  public void updateEditorBlockLine() {
    boolean enabled = setting.enableBlockLine();
    setHighlightCurrentBlock(enabled);
    setBlockLineEnabled(enabled);
    //setBlockLineWidth(3.0f);
  }

  /** رفتن به خط مشخص (شماره خط از ۱ شروع میشود ولی داخل سورا صفر-مبناست). */
  public void gotoLine(int lineNumber) {
    int target = Math.max(1, lineNumber);
    int lineCount = getText().getLineCount();
    if (target > lineCount) {
      target = lineCount;
    }
    jumpToLine(target - 1);
    requestFocus();
    ensureSelectionVisible();
  }

  /** پیشوند کامنت تک خطی بر اساس پسوند فایل فعلی؛ پیشفرض // */
  private String getCommentPrefix() {
    if (currentFilePath != null) {
      int dot = currentFilePath.lastIndexOf('.');
      if (dot >= 0 && dot + 1 < currentFilePath.length()) {
        String ext = currentFilePath.substring(dot + 1).toLowerCase(Locale.ROOT);
        String prefix = COMMENT_PREFIX_BY_EXTENSION.get(ext);
        if (prefix != null) {
          return prefix;
        }
      }
    }
    return "//";
  }

  /** کامنت/آنکامنت کردن خط فعلی یا همه خطوط انتخابشده. */
  public void toggleCommentForCurrentLine() {
    String prefix = getCommentPrefix();
    var cursor = getCursor();
    int startLine = cursor.getLeftLine();
    int endLine = cursor.getRightLine();
    var text = getText();
    boolean allCommented = true;
    for (int l = startLine; l <= endLine; l++) {
      String line = text.getLine(l).toString();
      if (!line.trim().isEmpty() && !line.trim().startsWith(prefix)) {
        allCommented = false;
        break;
      }
    }
    StringBuilder sb = new StringBuilder();
    for (int l = startLine; l <= endLine; l++) {
      String line = text.getLine(l).toString();
      if (allCommented) {
        int idx = line.indexOf(prefix);
        if (idx >= 0) {
          sb.append(line, 0, idx).append(line.substring(idx + prefix.length()));
        } else {
          sb.append(line);
        }
      } else if (!line.trim().isEmpty()) {
        int lead = 0;
        while (lead < line.length() && Character.isWhitespace(line.charAt(lead))) {
          lead++;
        }
        sb.append(line, 0, lead).append(prefix).append(line.substring(lead));
      } else {
        sb.append(line);
      }
      if (l != endLine) {
        sb.append('\n');
      }
    }
    text.beginBatchEdit();
    try {
      text.replace(startLine, 0, endLine, text.getLine(endLine).length(), sb);
    } finally {
      text.endBatchEdit();
    }
  }

  public void setCurrentFilePath(String htmlFilePath) {
    this.currentFilePath = htmlFilePath;
    if (imagePreviewIde != null) {
      imagePreviewIde.setCurrentFilePath(htmlFilePath);
    }
    if (stringresourceextractoride != null) {
      stringresourceextractoride.setCurrentFilePath(htmlFilePath);
    }
    if (gradleDependencyCheckerIde != null) {
      gradleDependencyCheckerIde.setFilePath(htmlFilePath);
    }
    if (tomlDependencyCheckerIde != null) {
      tomlDependencyCheckerIde.setFilePath(htmlFilePath);
    }
  }

  public String getCurrentFilePath() {
    return currentFilePath;
  }

  /**
   * چک سریع و بدون I/O سنگین که آیا برای فایل باز شده ی فعلی یک Language Server نصب شده یا نه (فقط
   * وجود باینری سرور رو داخل rootfs نگاه می کنه). صدا زدنش روی UI thread امنه؛ برای تصمیم نشون
   * دادن/قایم کردن دکمه های LSP توی CustomEditorTextActionWindow استفاده می شه.
   */
  public boolean isLspAvailableForCurrentFile() {
    return currentFilePath != null && LspRouter.isInstalled(getContext(), currentFilePath);
  }

  public LspEditor getLspEditor() {
    return lspEditor;
  }

  /**
   * فرگمنت (EditorFragment) از قبل موقع باز شدن فایل به سرور LSP وصل می شه؛ این متد فقط همون
   * LspEditor از قبل وصل شده رو به IdeEditor می ده تا CustomEditorTextActionWindow دوباره یک اتصال
   * جدید نسازه. برخلاف disconnectLsp()، اینجا چیزی dispose نمی شه - فقط رفرنس ست/پاک می شه؛ مسئولیت
   * dispose کردن اتصال هنوز دست خودِ فرگمنته (مثلا توی onDestroyView).
   */
  public void setLspEditor(LspEditor lspEditor) {
    this.lspEditor = lspEditor;
  }

  /**
   * اگه از قبل به سرور LSP وصل نشده، وصلش می کنه؛ اگه وصل بود همون اتصال قبلی رو برمی گردونه.
   *
   * <p>عملیات I/O سنگینه (اجرای proot + هندشیک LSP)، هرگز روی UI thread صداش نزن؛ از یک ترد پس
   * زمینه (Thread/Executor) صدا بزن.
   *
   * <p>نکته: چون فرگمنت فقط مسیر فایل رو از setCurrentFilePath می ده و ریشه ی پروژه رو نمی دونیم،
   * پوشه ی والدِ فایل به عنوان projectRoot استفاده می شه (برای clangd هم ایده آل ترین حالت نیست ولی
   * طبق کامنت خودِ ClangdServer، پوشه ی فایل هم کار می کنه).
   *
   * @return LspEditor وصل شده، یا null اگه فایلی باز نباشه/سرور نصب نباشه/اتصال شکست بخوره
   */
  @WorkerThread
  public LspEditor ensureLspConnected() {
    LspInitParamsHook.install();
    if (lspEditor != null && lspEditor.isConnected()) {
      return lspEditor;
    }
    if (currentFilePath == null) {
      return null;
    }
    File file = new File(currentFilePath);
    String projectRoot = file.getParent() != null ? file.getParent() : file.getAbsolutePath();
    LspEditor connected = LspRouter.connectFile(getContext(), projectRoot, currentFilePath, this);
    if (connected != null) {
      lspEditor = connected;
    }
    return lspEditor;
  }

  /**
   * موقع بسته شدن فایل/تب صدا بزن (اختیاری - چیزی اینجا خودکار صداش نمی زنه؛ اگه صدا زده نشه فقط
   * پروسه ی سرور LSP تا بسته شدن کامل برنامه زنده می مونه، مشکل عملکردی نداره).
   */
  public void disconnectLsp() {
    if (lspEditor != null) {
      LspRouter.disconnectFile(lspEditor);
      lspEditor = null;
    }
  }

  @Nullable
  public LspEditorStatus getLspStatus() {
    return lspEditor == null ? null : lspEditor.getStatus();
  }

  @Nullable
  public GhostLspStatusListener getLspStatusListener() {
    return lspStatusListener;
  }

  public void setLspStatusListener(@Nullable GhostLspStatusListener listener) {
    this.lspStatusListener = listener;
  }

  public GhostTextCompletionManager getGhostCompletionManager() {
    return ghostCompletionManager;
  }

  /**
   * Keep the ghost text completion preview alive no matter who replaces the inlay hints (color
   * previews, LSP, ...): the latest base container is remembered and the ghost hint is merged back
   * on top before it reaches the editor.
   */
  @Override
  public void setInlayHints(@Nullable InlayHintsContainer inlayHints) {
    if (ghostCompletionManager != null && ghostCompletionManager.isActive()) {
      ghostCompletionManager.setBaseHints(inlayHints);
      super.setInlayHints(ghostCompletionManager.mergeGhost(inlayHints));
    } else {
      super.setInlayHints(inlayHints);
    }
  }

  /**
   * Direct setter that bypasses the ghost text merge (used by {@link GhostTextCompletionManager}).
   */
  public void setInlayHintsRaw(@Nullable InlayHintsContainer inlayHints) {
    super.setInlayHints(inlayHints);
  }

  private void updateEditorPowerMode() {
    setPowerModeEnabled(setting.enablePowerMode());
    updateEditorPowerModeEffectType();
  }

  private void updateEditorPowerModeEffectType() {
    if (mPowerModeEffectManager != null) {
      mPowerModeEffectManager.setEffect(
          PowerModeEffectManager.EffectType.fromString(setting.getPowerModeEffectType()));
    }
  }

  private void updateEditorPinLineNumber() {
    setPinLineNumber(setting.pinLineNumber());
  }

  private void updateEditorMiniMap() {
    var enabled = setting.enableMiniMap();
    getProps().showMinimap = enabled;
  }

  public void setWebIdeColor(boolean mod) {
    if (mod) {
      webColorIde = new WebColorIde(this);
      webColorIde.attach();
    }
  }

  private void updateEditorFontLigatures() {
    setLigatureEnabled(setting.useFontLigatures());
  }

  private void updateEditorStickyScroll() {
    var enabled = setting.enableStickyScroll();
    getProps().stickyScroll = enabled;
    setStickyScroll(enabled);
    setStickyScrollMaxLines(4);
  }

  private void updateEditorTypeFace() {
    var typeface = getContext().getResources().getFont(setting.getCurrentEditorFont());
    setTypefaceText(typeface);
    setTypefaceLineNumber(typeface);
  }

  public void setStickyScroll(boolean enabled) {
    getProps().stickyScroll = enabled;
  }

  public void setStickyScrollMaxLines(int maxLines) {
    getProps().stickyScrollMaxLines = maxLines;
  }

  private void updateEditorHardWareAcceleration() {
    setHardwareAcceleratedDrawAllowed(setting.enableHardWareAcceleration());
  }

  private void updateEditorScrollBar() {
    setScrollBarEnabled(setting.enableScrollBar());
  }

  private void updateEditorTabSize() {
    setTabWidth(setting.getCodeEditorTabSize());
  }

  private void updateEditorMagnifier() {
    enableMagnifier(setting.enableMagnifier());
  }

  public void enableMagnifier(boolean enabled) {
    getComponent(Magnifier.class).setEnabled(enabled);
  }

  private void updateEditorWordWrap() {
    setWordwrap(setting.useWordWrap());
  }

  private void updateEditorLineNumber() {
    setLineNumberEnabled(setting.enableLineNumbers());
  }

  private void updateEditorAutoCompletePanelAnimation() {
    animateAutoCompletionPanel(setting.enableAutoCompleteWindowAnimation());
  }

  public void animateAutoCompletionPanel(boolean enabled) {
    getComponent(EditorAutoCompletion.class).setEnabledAnimation(enabled);
  }

  private void updateEditorDeleteEmptyLineFast() {
    deleteEmptyLineFast(setting.enableDeleteEmptyLine());
  }

  public void deleteEmptyLineFast(boolean deleteEmptyLinesFast) {
    getProps().deleteEmptyLineFast = deleteEmptyLinesFast;
  }

  private void updateEditorDeleteTabs() {
    deleteTabs(setting.enableDeleteTab());
  }

  public void deleteTabs(boolean deleteTabs) {
    getProps().deleteMultiSpaces = deleteTabs ? -1 : 1;
  }

  private void updateEditorHighlightBracketPair() {
    setHighlightBracketPair(setting.enableBracketHighlight());
  }

  private void updateEditorLineSpacing() {
    setLineSpacing(setting.getCurrentEditorLineHeight(), 1.1f);
  }

  private void updateEditorCursorBlinkPeriod() {
    setCursorBlinkPeriod(setting.getCursorBlinkPeriod());
  }

  public void useICULibrary(boolean enabled) {
    getProps().useICULibToSelectWords = enabled;
  }

  private void applyNonPrintablePaintingFlags() {
    var flags =
        applyNonPrintableFlags(
            setting.flagLeading(),
            setting.flagInner(),
            setting.flagTrailing(),
            setting.flagEmptyLine(),
            setting.flagLineBreaks(),
            setting.flagInSelection(),
            setting.flagTabSameAsSpace());
    setNonPrintablePaintingFlags(flags);
  }

  public void updateEditorNonPrintablePaintingFlags() {
    showNonPrintableFlagsDialog(getContext(), setting, this::applyNonPrintablePaintingFlags);
  }

  public static void showNonPrintableFlagsDialog(
      Context context, PreferencesUtils prefs, Runnable onApplied) {
    boolean[] checked = {
      prefs.flagLeading(),
      prefs.flagInner(),
      prefs.flagTrailing(),
      prefs.flagEmptyLine(),
      prefs.flagLineBreaks(),
      prefs.flagInSelection(),
      prefs.flagTabSameAsSpace()
    };
    boolean[] selection = checked.clone();
    String[] codes = {"2", "1", "3", "4", "5", "6", "7"};
    new DialogCompat(context)
        .setTitle(R.string.whitespace_dialog_title)
        .setMultiChoiceItems(
            context.getResources().getStringArray(R.array.whitespace_flag_labels),
            selection,
            (dialog, which, isChecked) -> selection[which] = isChecked)
        .setPositiveButton(
            R.string.whitespace_dialog_ok,
            (dialog, which) -> {
              Set<String> flags = new HashSet<>();
              for (int i = 0; i < selection.length; i++) {
                if (selection[i]) flags.add(codes[i]);
              }
              prefs.setNonPrintableFlags(flags);
              if (onApplied != null) onApplied.run();
            })
        .setNegativeButton(R.string.lsp_cancel, null)
        .show();
  }

  public int applyNonPrintableFlags(
      boolean leading,
      boolean inner,
      boolean trailing,
      boolean emptyLine,
      boolean lineSeparator,
      boolean inSelection,
      boolean tabSameAsSpace) {
    return (leading ? CodeEditor.FLAG_DRAW_WHITESPACE_LEADING : 0)
        | (inner ? CodeEditor.FLAG_DRAW_WHITESPACE_INNER : 0)
        | (trailing ? CodeEditor.FLAG_DRAW_WHITESPACE_TRAILING : 0)
        | (emptyLine ? CodeEditor.FLAG_DRAW_WHITESPACE_FOR_EMPTY_LINE : 0)
        | (lineSeparator ? CodeEditor.FLAG_DRAW_LINE_SEPARATOR : 0)
        | (inSelection ? CodeEditor.FLAG_DRAW_WHITESPACE_IN_SELECTION : 0)
        | (tabSameAsSpace ? CodeEditor.FLAG_DRAW_TAB_SAME_AS_SPACE : 0);
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences pref, @Nullable String key) {
    Objects.requireNonNull(key);
    switch (key) {
      case Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_TAB_SIZE:
        updateEditorTabSize();
        break;
      case Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_STICKY_SCROLL:
        updateEditorStickyScroll();
        break;
      case Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_HARDWARE_ACCELERATION:
        updateEditorHardWareAcceleration();
        break;
      case Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_SCROLL_BAR:
        updateEditorScrollBar();
        break;
      case Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_MAGNIFIER:
        updateEditorMagnifier();
        break;
      case Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_WORD_WRAP:
        updateEditorWordWrap();
        break;
      case Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_LINE_NUMBERS:
        updateEditorLineNumber();
        break;
      case Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_ANIMATE_AUTO_COMP_WINDOW:
        updateEditorAutoCompletePanelAnimation();
        break;
      case Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_DELETE_EMPTY_LINE:
        updateEditorDeleteEmptyLineFast();
        break;
      case Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_DELETE_TAB:
        updateEditorDeleteTabs();
        break;
      case Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_HIGHLIGHT_BRACKET:
        updateEditorHighlightBracketPair();
        break;
      case Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_LINE_HEIGHT:
        updateEditorLineSpacing();
        break;
      case Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_CURSOR_BLINK_PERIOD:
        updateEditorCursorBlinkPeriod();
        break;
      case Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_NP_PAINT_FLAGS:
        applyNonPrintablePaintingFlags();
        break;
      case Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_FONT_LIAGTURES:
        updateEditorFontLigatures();
        break;
      case Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_PIN_LINE_NUM:
        updateEditorPinLineNumber();
        break;
      case Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_ICU:
        useICULibrary(setting.useICULibrary());
        break;
      case Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_FONT:
        updateEditorTypeFace();
        break;
      case Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_POWER_MODE:
        setPowerModeEnabled(setting.enablePowerMode());
        break;
      case Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_POWER_MODE_EFFECT:
        updateEditorPowerModeEffectType();
        break;
      case Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_BLOCK_LINE:
        updateEditorBlockLine();
        break;
      case Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_GHOST_TEXT:
        if (ghostCompletionManager != null) {
          ghostCompletionManager.setEnabled(setting.enableGhostTextCompletion());
        }
        break;
      default:
    }
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    if (mPowerModeEffectManager != null) {
      mPowerModeEffectManager.drawEffects(canvas);
    }
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    if (setting != null) {
      setting.getDefaultPreferences().registerOnSharedPreferenceChangeListener(this);
    }
  }

  @Override
  protected void onDetachedFromWindow() {
    if (setting != null) {
      setting.getDefaultPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }
    super.onDetachedFromWindow();
    if (mPowerModeEffectManager != null) {
      mPowerModeEffectManager.clearEffects();
    }
  }

  public boolean registerCustomEffect(CustomEffect effect) {
    if (mPowerModeEffectManager != null) {
      return mPowerModeEffectManager.registerCustomEffect(effect);
    }
    return false;
  }

  public boolean unregisterCustomEffect(String effectName) {
    if (mPowerModeEffectManager != null) {
      return mPowerModeEffectManager.unregisterCustomEffect(effectName);
    }
    return false;
  }

  public List<CustomEffect> getCustomEffects() {
    if (mPowerModeEffectManager != null) {
      return mPowerModeEffectManager.getCustomEffects();
    }
    return new ArrayList<>();
  }

  public void spawnCustomEffect(String effectName, float x, float y) {
    if (mPowerModeEffectManager != null) {
      mPowerModeEffectManager.spawnCustomEffect(effectName, x, y);
      invalidate();
    }
  }

  /**
   * Get the PowerMode effect manager for this editor
   *
   * @return The PowerMode effect manager instance
   */
  public PowerModeEffectManager getPowerModeEffectManager() {
    return mPowerModeEffectManager;
  }

  public void setPowerModeEnabled(boolean enabled) {
    this.powerModeEnabled = enabled;
    if (enabled) {
      if (mPowerModeEffectManager == null) {
        mPowerModeEffectManager = new PowerModeEffectManager(this);
      }
    } else {
      if (mPowerModeEffectManager != null) {
        mPowerModeEffectManager.clearEffects();
      }
    }
    invalidate();
  }

  public boolean isPowerModeEnabled() {
    return powerModeEnabled && mPowerModeEffectManager != null;
  }
}
