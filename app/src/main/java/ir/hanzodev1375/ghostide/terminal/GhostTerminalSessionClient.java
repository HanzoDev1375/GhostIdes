package ir.hanzodev1375.ghostide.terminal;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.util.Log;
import com.termux.terminal.TerminalSession;
import com.termux.terminal.TerminalSessionClient; 

/**
 * پیاده‌سازی {@link TerminalSessionClient} کتابخونه‌ی terminal-emulator ترموکس (jitpack:
 * com.termux.termux-app:terminal-view). این کلاس رویدادهای هر {@link TerminalSession} (تغییر متن
 * صفحه، تغییر عنوان، تموم‌شدن پروسه‌ی شل، کپی/پیست کلیپ‌بورد و ...) رو میگیره و به {@link
 * #callback} (که TerminalActivity پیاده‌سازیش میکنه) گزارش میده.
 *
 * <p>توجه: این اینترفیس مستقیماً از کتابخونه‌ی خارجی میاد؛ اگه Android Studio بعد از اضافه‌کردن
 * dependency خطای "must implement inherited abstract method" داد (مثلاً یه متد جدید که اینجا نوشته
 * نشده)، فقط Alt+Enter بزن و بذار خودش استاب رو اضافه کنه — امضای دقیق متدها بین ورژن‌های ترموکس
 * ممکنه یکی-دو مورد فرق کنه.
 */
public class GhostTerminalSessionClient implements TerminalSessionClient {

  private static final String LOG_TAG = "GhostTerminal";

  /** رویدادهایی که TerminalActivity برای آپدیت UI (تب‌ها، عنوان و ...) بهشون نیاز داره. */
  public interface Callback {
    void onTextChanged(TerminalSession session);

    void onTitleChanged(TerminalSession session);

    void onSessionFinished(TerminalSession session);
  }

  private final Context appContext;
  private final Callback callback;

  public GhostTerminalSessionClient(Context context, Callback callback) {
    this.appContext = context.getApplicationContext();
    this.callback = callback;
  }

  @Override
  public void onTextChanged(TerminalSession changedSession) {
    callback.onTextChanged(changedSession);
  }

  @Override
  public void onTitleChanged(TerminalSession changedSession) {
    callback.onTitleChanged(changedSession);
  }

  @Override
  public void onSessionFinished(TerminalSession finishedSession) {
    callback.onSessionFinished(finishedSession);
  }

  @Override
  public void onCopyTextToClipboard(TerminalSession session, String text) {
    if (text == null) return;
    ClipboardManager clipboard =
        (ClipboardManager) appContext.getSystemService(Context.CLIPBOARD_SERVICE);
    if (clipboard != null) {
      clipboard.setPrimaryClip(ClipData.newPlainText("terminal", text));
    }
  }

  @Override
  public void onPasteTextFromClipboard(TerminalSession session) {
    ClipboardManager clipboard =
        (ClipboardManager) appContext.getSystemService(Context.CLIPBOARD_SERVICE);
    if (clipboard == null || !clipboard.hasPrimaryClip()) return;
    ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
    CharSequence pasted = item.getText();
    if (pasted == null || session.getEmulator() == null) return;
    // اگه paste(String) روی این ورژن کامپایل نشد، جایگزین کن با:
    // session.write(pasted.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    session.getEmulator().paste(pasted.toString());
  }

  @Override
  public void onBell(TerminalSession session) {
    // فعلاً نادیده میگیریم؛ در آینده میشه یه ویبره‌ی کوتاه اضافه کرد
  }

  @Override
  public void onColorsChanged(TerminalSession session) {}

  @Override
  public void onTerminalCursorStateChange(boolean state) {}

  @Override
  public Integer getTerminalCursorStyle() {
    return null; // پیش‌فرض خود کتابخونه استفاده بشه
  }

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
