/*
 * نمونه کد (فیک) — پیاده‌سازی ساده‌ی تم واکنشی به سبک تلگرام
 * ----------------------------------------------
 * ایده اینه که وقتی تِم عوض می‌شه، صفحه از نو ساخته نشه (recreate)؛
 * فقط رنگِ همون View های موجود عوض بشه + انیمیشن بین دو رنگ.
 *
 * برای درک:
 *   1) ThemeManager   → همه‌ی رنگ‌ها با کلید (اسم) اینجا ذخیره ان.
 *   2) ThemeBus       → بلندگو: وقتی تم عوض شد به همه خبر می‌ده.
 *   3) ThemeBind      → به هر ویو یه کلید رنگ وصل می‌کنه (برچسب).
 *   4) ThemeApply     → بلندگو که گفت "تم عوض شد" فقط رنگ ویوها رو عوض می‌کنه.
 *   5) ThemeAnimation → بین رنگِ قدیم و جدید پرش (lerp) می‌کنه.
 */

package sample;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.core.graphics.ColorUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/* ============ ۱) مخزن مرکزی رنگ‌ها (معادل Theme.getColor در تلگرام) ============ */
class ThemeManager {
  // کلیدها رو با مسیر JSON نگه می‌داریم: "activity.background" ، "widget.accent" و...
  private static final Map<String, Integer> colors = new HashMap<>();

  static {
    colors.put("window.background", Color.parseColor("#1e1e1e"));
    colors.put("widget.accent", Color.parseColor("#0e639c"));
    colors.put("widget.text", Color.parseColor("#cccccc"));
  }

  /** تنها راه خواندن رنگ — هیچ‌جا نباید Color.parseColor مستقیم باشه. */
  public static int color(String key) {
    Integer c = colors.get(key);
    return c != null ? c : Color.TRANSPARENT;
  }

  public static void applyNewTheme() {
    // این وقتیه که کاربر تِم جدید انتخاب می‌کنه؛ فقط مپ عوض می‌شه، ویوها دست نمی‌خورن.
    colors.put("window.background", Color.parseColor("#0d1117"));
    colors.put("widget.accent", Color.parseColor("#58a6ff"));
    colors.put("widget.text", Color.parseColor("#e6edf3"));

    // بعد از عوض شدن مپ، به بلندگو می‌گیم همه رو خبر کن.
    ThemeBus.notifyThemeChanged();
  }

  /** نسخه‌ی کپی از رنگ‌های فعلی — برای مقایسه‌ی قدیم/جدید موقع انیمیشن. */
  public static Map<String, Integer> snapshot() {
    return new HashMap<>(colors);
  }
}

/* ============ ۳) اتصال یک ویو به یک کلید رنگ (معادل ThemeDescription) ============ */
class ThemeBind {
  public static final int TEXT = 1;
  public static final int BACKGROUND = 2;
  public static final int HINT = 3;

  /** موقع ساخت ویو یک‌بار صدا می‌زنیم تا برچسب کلید رو روی ویو بنویسیم. */
  public static void bind(View view, String key, int what) {
    // در پروژه‌ی واقعی بهتره لیست bind ها توی Activity نگه داشته بشه،
    // اینجا از tag استفاده کردیم فقط برای سادگی.
    view.setTag(DATA_KEY, new String[] {key, String.valueOf(what)});
  }

  private static final int DATA_KEY = 0x7a00_0001;

  /** رنگِ مربوط به این کلید رو روی همین ویو (بدون ساختن دوباره‌اش) اعمال می‌کنه. */
  public static void applyColor(View view, int color) {
    Object tag = view.getTag(DATA_KEY);
    if (!(tag instanceof String[])) {
      return;
    }
    String[] data = (String[]) tag;
    int what = Integer.parseInt(data[1]);
    if (what == TEXT) {
      ((TextView) view).setTextColor(color);
    } else if (what == BACKGROUND) {
      view.setBackgroundColor(color);
    } else if (what == HINT) {
      ((TextView) view).setHintTextColor(color);
    }
    view.invalidate(); // فقط یه invalidate، نه بازسازی
  }
}

/* ============ ۲) بلندگو: منتشر کردن خبر «تم عوض شد» (معادل NotificationCenter) ============ */
class ThemeBus {
  interface ThemeChangeListener {
    void onThemeChanged(Map<String, Integer> newColors, boolean animated);
  }

  private static final List<ThemeChangeListener> listeners = new CopyOnWriteArrayList<>();

  public static void register(ThemeChangeListener l) {
    if (l != null && !listeners.contains(l)) {
      listeners.add(l);
    }
  }

  public static void unregister(ThemeChangeListener l) {
    listeners.remove(l);
  }

  private static void notifyThemeChanged() {
    Map<String, Integer> current = ThemeManager.snapshot();
    for (ThemeChangeListener l : listeners) {
      l.onThemeChanged(current, true);
    }
  }
}

/* ============ ۵) انیمیشن: پرش بین رنگِ قدیم و جدید، فریم به فریم ============ */
class ThemeAnimation {
  interface ProgressListener {
    void onFrame(Map<String, Integer> interpolatedColors);
  }

  private Map<String, Integer> startColors;

  public void start(Map<String, Integer> oldColors, Map<String, Integer> newColors, ProgressListener listener) {
    this.startColors = oldColors;
    ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
    animator.setDuration(250);
    animator.addUpdateListener(animation -> {
      float progress = (float) animation.getAnimatedValue();
      Map<String, Integer> frame = new HashMap<>();
      for (Map.Entry<String, Integer> entry : newColors.entrySet()) {
        int old = startColors.getOrDefault(entry.getKey(), entry.getValue());
        int now = entry.getValue();
        // lerp بین دو رنگ — دقیقاً همون ترفند setThemeAnimationValue تلگرام
        frame.put(entry.getKey(), ColorUtils.blendARGB(old, now, progress));
      }
      listener.onFrame(frame);
    });
    animator.start();
  }
}

/* ============ ۴) اکتیویتی پایه: گوش دادن به بلندگو + رنگ کردن ویوها ============ */
abstract class SampleBaseActivity extends Activity implements ThemeBus.ThemeChangeListener {

  private ThemeAnimation animation = new ThemeAnimation();
  private Map<String, Integer> lastColors = ThemeManager.snapshot();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // موقع ساخت، برای ویوها برچسب کلید می‌زنیم (یک‌بار).
    ThemeBind.bind(findViewById(android.R.id.title), "widget.text", ThemeBind.TEXT);
    ThemeBind.bind(findViewById(android.R.id.content), "window.background", ThemeBind.BACKGROUND);
    ThemeBus.register(this);
  }

  @Override
  protected void onDestroy() {
    ThemeBus.unregister(this);
    super.onDestroy();
  }

  @Override
  public void onThemeChanged(Map<String, Integer> newColors, boolean animated) {
    if (animated) {
      // انیمیشن: بین رنگ‌های قدیم که ذخیره کردیم و رنگ‌های جدید پرش می‌کنیم.
      animation.start(lastColors, newColors, frame -> applyToTree(getWindow().getDecorView(), frame));
    } else {
      applyToTree(getWindow().getDecorView(), newColors);
    }
    lastColors = newColors;
  }

  /** جلو رفتن در درخت ویوها و رنگ کردن هر ویوی برچسب‌دار — بدون هیچ recreate. */
  private void applyToTree(View root, Map<String, Integer> colors) {
    if (root == null) {
      return;
    }
    Object tag = root.getTag(ThemeBind.DATA_KEY);
    if (tag instanceof String[]) {
      String key = ((String[]) tag)[0];
      ThemeBind.applyColor(root, colors.getOrDefault(key, ThemeManager.color(key)));
    }
    if (root instanceof ViewGroup) {
      ViewGroup group = (ViewGroup) root;
      for (int i = 0; i < group.getChildCount(); i++) {
        applyToTree(group.getChildAt(i), colors);
      }
    }
  }
}