package ir.hanzodev1375.ghostide.terminal;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import com.termux.terminal.TerminalColors;
import com.termux.terminal.TerminalEmulator;
import com.termux.terminal.TerminalSession;
import ir.hanzodev1375.ghostide.GhostIdeAppLoader;
import ir.theme.M3Theme;
import java.util.Properties;

/**
 * فقط رنگِ متن ترمینال رو آپدیت می‌کنه. پس‌زمینه دست نمی‌خوره: ترمینال شفافه و از ریشه/والپیپر رنگ
 * می‌گیره تا با بک‌گراند فعالِ کاربر مچ بشه.
 */
public final class TerminalColorsUtil {

  private TerminalColorsUtil() {}

  private static int parsedForeground = 0xffffffff;

  /** رنگ متن رو از متریال (colorOnSurface) می‌سازه و روی اسکیم سراسری ترمینال اعمال می‌کنه. */
  public static void apply(Activity activity, Context context) {
    if (activity == null || context == null) return;
    var theme = GhostIdeAppLoader.getInstance().getThemeUtils();
    var setting = GhostIdeAppLoader.getInstance().getSetting();

    var colorPrimaryFixed =
        setting.isShowBackground()
            ? toHex(Color.parseColor(theme.getTheme().getWidget().getText()))
            : toHex(fallback(M3Theme.primary(), Color.WHITE));

    Properties props = new Properties();
    props.setProperty("foreground", colorPrimaryFixed);
    props.setProperty("cursor", colorPrimaryFixed);
    try {
      TerminalColors.COLOR_SCHEME.updateWith(props);
    } catch (Exception ignored) {
    }
  }

  /** رنگ متن اعمال‌شده در {@link #apply}. */
  public static int foregroundColor() {
    return parsedForeground;
  }

  /** اگه سشن از قبل ساخته شده، رنگ‌های فعلیش رو با اسکیم جدید هماهنگ کن. */
  public static void refreshSession(TerminalSession session) {
    if (session == null) return;
    TerminalEmulator emulator = session.getEmulator();
    if (emulator != null) emulator.mColors.reset();
  }

  private static String toHex(int color) {
    return String.format("#%06x", color & 0x00ffffff);
  }

  private static int fallback(Integer value, int def) {
    return value != null ? value : def;
  }
}
