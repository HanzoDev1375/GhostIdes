package ir.hanzodev1375.ghostide.codeeditors;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import androidx.annotation.Nullable;
import com.eup.codeopsstudio.editor.langs.widget.component.CustomEditorTextActionWindow;
import io.github.rosemoe.sora.event.ContentChangeEvent;
import io.github.rosemoe.sora.event.ScrollEvent;
import io.github.rosemoe.sora.graphics.inlayHint.TextInlayHintRenderer;
import io.github.rosemoe.sora.lang.styling.inlayHint.InlayHintsContainer;
import io.github.rosemoe.sora.lang.styling.inlayHint.TextInlayHint;
import io.github.rosemoe.sora.text.Content;
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion;
import io.github.rosemoe.sora.widget.component.EditorTextActionWindow;
import io.github.rosemoe.sora.widget.component.Magnifier;
import io.github.rosemoe.sora.widget.CodeEditor;
import ir.hanzodev1375.ghostide.codeeditors.colorrender.WebColorIde;
import ir.hanzodev1375.ghostide.codeeditors.preview.ImagePreviewIde;
import ir.hanzodev1375.ghostide.codeeditors.preview.htmltag.HtmlTagPreviewIde;
import ir.hanzodev1375.ghostide.codeeditors.preview.url.OnLinkClickEventListener;
import ir.hanzodev1375.ghostide.codeeditors.preview.url.UrlPreviewIde;
import ir.hanzodev1375.ghostide.codeeditors.preview.xmlattr.XmlAttrPreviewIde;
import ir.hanzodev1375.ghostide.codeeditors.setting.Constants;
import ir.hanzodev1375.ghostide.codeeditors.setting.PreferencesUtils;
import ir.hanzodev1375.ghostide.codeeditors.stringres.StringResourceExtractorIde;
import ir.hanzodev1375.ghostide.codeeditors.ui.CustomEditorAutoCompletion;
import ir.hanzodev1375.ghostide.codeeditors.ui.CustomEditorCompletionAdapter;
import ir.hanzodev1375.ghostide.codeeditors.ui.power.PowerModeEffectManager;
import ir.hanzodev1375.ghostide.codeeditors.ui.power.custom.CustomEffect;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;

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
  private HtmlTagPreviewIde htmltagpreview;

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
    htmltagpreview = new HtmlTagPreviewIde(this);
    htmltagpreview.attach();
    editorAutoCompletion.setAdapter(new CustomEditorCompletionAdapter());
    replaceComponent(EditorAutoCompletion.class, editorAutoCompletion);
    replaceComponent(EditorTextActionWindow.class, new CustomEditorTextActionWindow(this));
    getComponent(EditorAutoCompletion.class)
        .setEnabledAnimation(setting.enableAutoCompleteWindowAnimation());

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
    updateEditorNonPrintablePaintingFlags();
    updateEditorFontLigatures();
    updateEditorPinLineNumber();
    updateEditorMiniMap();
    updateEditorTypeFace();
    editorBinder();
    updateEditorPowerMode();
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
  }

  @SuppressWarnings({"Deprecated", "all"})
  void editorBinder() {
    setLigatureEnabled(true);
    setHighlightCurrentLine(false);
    ensureSelectionVisible();
    setRenderFunctionCharacters(true);
    setDisableSoftKbdIfHardKbdAvailable(true);
  }

  public void setOnLinkClick(OnLinkClickEventListener call) {
    urlPreviewIde.setEvent(call);
  }

  public void setCutLine() {
    this.cutLine();
    // duplicateLine()
    // selectCurrentWord()

  }

  public void setCurrentFilePath(String htmlFilePath) {
    if (imagePreviewIde != null) {
      imagePreviewIde.setCurrentFilePath(htmlFilePath);
    }
    if (stringresourceextractoride != null) {
      stringresourceextractoride.setCurrentFilePath(htmlFilePath);
    }
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

  private void updateEditorNonPrintablePaintingFlags() {
    var flags =
        applyNonPrintableFlags(
            setting.flagLeading(),
            setting.flagInner(),
            setting.flagTrailing(),
            setting.flagEmptyLine(),
            setting.flagLineBreaks(),
            true,
            false);
    setNonPrintablePaintingFlags(flags);
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
        updateEditorNonPrintablePaintingFlags();
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
  protected void onDetachedFromWindow() {
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
