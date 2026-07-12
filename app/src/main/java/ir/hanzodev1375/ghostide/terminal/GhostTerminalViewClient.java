package ir.hanzodev1375.ghostide.terminal;

import android.content.Context;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.inputmethod.InputMethodManager;
import android.util.Log;
import com.blankj.utilcode.util.KeyboardUtils;
import com.termux.terminal.TerminalSession;
import com.termux.view.TerminalView;
import com.termux.view.TerminalViewClient;

/**
 * پیاده‌سازی {@link TerminalViewClient}. مسئول: باز کردن کیبورد با تپ، و خوندن وضعیت
 * دکمه‌های CTRL/ALT ردیف extra-keys (از طریق {@link KeyModifierState}) هر بار که کاراکتری
 * تایپ میشه.
 *
 * توجه: مثل GhostTerminalSessionClient، این هم مستقیماً یه اینترفیس خارجیه؛ اگه ورژن
 * ترموکس شما متد اضافه‌ای داشت که اینجا نیومده (مثلاً isTerminalViewScalingDisabled در
 * نسخه‌های خیلی جدید)، با Alt+Enter در اندروید استودیو استاب خالی‌ش رو اضافه کن.
 */
public class GhostTerminalViewClient implements TerminalViewClient {

  /** وضعیت toggle دکمه‌های CTRL/ALT توی ردیف extra-keys پایین ترمینال. */
  public interface KeyModifierState {
    boolean isCtrlToggled();

    boolean isAltToggled();

    /** بعد از مصرف یه کاراکتر با CTRL روشن، دوباره خاموشش کن (رفتار sticky). */
    void consumeCtrlToggle();

    void consumeAltToggle();
  }

  private static final String LOG_TAG = "GhostTerminal";

  // چون TerminalView خودش متدی برای "بزرگ/کوچیک کردن فونت نسبت به قبل" نداره (فقط
  // setTextSize(int pixels) خام رو میگیره)، این منطق (پله‌ای زیاد/کم کردن + محدودکردن به
  // بازه‌ی min/max) رو خودمون اینجا نگه میداریم.
  private static final int MIN_FONT_SIZE_SP = 8;
  private static final int MAX_FONT_SIZE_SP = 30;
  private static final int DEFAULT_FONT_SIZE_SP = 14;
  private static final int FONT_SIZE_STEP_SP = 1;

  private final TerminalView terminalView;
  private final KeyModifierState modifierState;
  private int currentFontSizeSp = DEFAULT_FONT_SIZE_SP;

  public GhostTerminalViewClient(TerminalView terminalView, KeyModifierState modifierState) {
    this.terminalView = terminalView;
    this.modifierState = modifierState;
    applyFontSize();
  }

  private void applyFontSize() {
    float scaledDensity = terminalView.getResources().getDisplayMetrics().scaledDensity;
    // اگه setTextSize(int) روی این ورژن پیدا نشد، جایگزین کن با هر متد مشابهی که
    // اندروید استودیو موقع تایپ "terminalView.set" پیشنهاد میده (مثلاً setTextSize(float)).
    terminalView.setTextSize(Math.round(currentFontSizeSp * scaledDensity));
  }

  @Override
  public float onScale(float scale) {
    if (scale < 0.9f || scale > 1.1f) {
      boolean increase = scale > 1.1f;
      currentFontSizeSp =
          increase
              ? Math.min(MAX_FONT_SIZE_SP, currentFontSizeSp + FONT_SIZE_STEP_SP)
              : Math.max(MIN_FONT_SIZE_SP, currentFontSizeSp - FONT_SIZE_STEP_SP);
      applyFontSize();
      return 1.0f;
    }
    return scale;
  }

  @Override
  public void onSingleTapUp(MotionEvent e) {
    terminalView.setFocusable(true);
    terminalView.setFocusableInTouchMode(true);
    terminalView.requestFocus();
    KeyboardUtils.showSoftInput(terminalView);
  }

  @Override
  public boolean shouldBackButtonBeMappedToEscape() {
    return false;
  }

  @Override
  public boolean shouldEnforceCharBasedInput() {
    return true;
  }

  @Override
  public boolean shouldUseCtrlSpaceWorkaround() {
    return false;
  }

  @Override
  public boolean isTerminalViewSelected() {
    return true;
  }

  @Override
  public void copyModeChanged(boolean copyMode) {}

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent e, TerminalSession session) {
    return false;
  }

  @Override
  public boolean onKeyUp(int keyCode, KeyEvent e) {
    return false;
  }

  @Override
  public boolean onLongPress(MotionEvent event) {
    return false;
  }

  @Override
  public boolean readControlKey() {
    return modifierState.isCtrlToggled();
  }

  @Override
  public boolean readAltKey() {
    return modifierState.isAltToggled();
  }

  @Override
  public boolean readShiftKey() {
    return false;
  }

  @Override
  public boolean readFnKey() {
    return false;
  }

  @Override
  public boolean onCodePoint(int codePoint, boolean ctrlDown, TerminalSession session) {
    // بعد از مصرفِ یه کاراکتر، اگه CTRL/ALT موقتاً از extra-keys روشن بودن، خاموششون کن
    if (modifierState.isCtrlToggled()) modifierState.consumeCtrlToggle();
    if (modifierState.isAltToggled()) modifierState.consumeAltToggle();
    return false;
  }

  @Override
  public void onEmulatorSet() {}

  @Override
  public void logError(String tag, String message) {
    Log.e(tag, message);
  }

  @Override
  public void logWarn(String tag, String message) {
    Log.w(tag, message);
  }

  @Override
  public void logInfo(String tag, String message) {
    Log.i(tag, message);
  }

  @Override
  public void logDebug(String tag, String message) {
    Log.d(tag, message);
  }

  @Override
  public void logVerbose(String tag, String message) {
    Log.v(tag, message);
  }

  @Override
  public void logStackTraceWithMessage(String tag, String message, Exception e) {
    Log.e(tag, message, e);
  }

  @Override
  public void logStackTrace(String tag, Exception e) {
    Log.e(tag, "uncaught exception", e);
  }
}