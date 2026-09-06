package ir.hanzodev1375.components.colors;

import android.graphics.Color;

public final class AccentPalette {

  private AccentPalette() {}

  public static int changeAccent(int baseAccent, int accent, int color, boolean isDark) {
    return changeAccent(baseAccent, accent, color, isDark, color);
  }

  public static int changeAccent(
      int baseAccent, int accent, int color, boolean isDark, int fallback) {
    if (baseAccent == 0 || accent == 0 || baseAccent == accent) {
      return color;
    }
    return changeAccent(hsv(baseAccent), hsv(accent), color, isDark, fallback);
  }

  public static int changeAccent(
      float[] baseHsv, float[] accentHsv, int color, boolean isDark, int fallback) {
    float[] colorHsv = hsv(color);
    float diffH =
        Math.min(Math.abs(colorHsv[0] - baseHsv[0]), Math.abs(colorHsv[0] - baseHsv[0] - 360f));
    if (diffH > 30f) {
      return fallback;
    }
    if (baseHsv[1] <= 0.001f || baseHsv[2] <= 0.001f) {
      return fallback;
    }

    float dist = Math.min(1.5f * colorHsv[1] / baseHsv[1], 1f);

    colorHsv[0] = colorHsv[0] + accentHsv[0] - baseHsv[0];
    colorHsv[1] = colorHsv[1] * accentHsv[1] / baseHsv[1];
    colorHsv[2] = colorHsv[2] * (1f - dist + dist * accentHsv[2] / baseHsv[2]);

    int newColor = Color.HSVToColor(Color.alpha(color), colorHsv);

    float origBrightness = perceivedBrightness(color);
    float newBrightness = perceivedBrightness(newColor);

    // تم تیره باید رنگ ها را روشن نگه دارد و تم روشن تیره — اگه برعکس شد تصحیح کن.
    boolean needRevertBrightness =
        isDark ? origBrightness > newBrightness : origBrightness < newBrightness;

    if (needRevertBrightness) {
      float amountOfNew = 0.6f;
      float fallbackAmount = (1f - amountOfNew) * origBrightness / newBrightness + amountOfNew;
      newColor = changeBrightness(newColor, fallbackAmount);
    }
    return newColor;
  }

  /** شیفت دادن یک مجموعه رنگ کامل (مثلاً آرایه ی role های تم) به اکسنت جدید. */
  public static int[] applyAccent(int baseAccent, int accent, int[] colors, boolean isDark) {
    int[] result = new int[colors.length];
    for (int i = 0; i < colors.length; i++) {
      result[i] = changeAccent(baseAccent, accent, colors[i], isDark, colors[i]);
    }
    return result;
  }

  /**
   * اکسنت «مشتق شده» برای یک عنصر جدا (مثل رنگ bubbles در تلگرام): با استفاده از فاصله ی رنگی، رنگِ
   * مجاور با رنگ پایه را به سمت اکسنت جدید منتقل می کنه.
   */
  public static int deriveAccent(int baseAccent, int baseColor, int elementColor) {
    float[] baseHsv = hsv(baseAccent);
    float[] elementHsv = hsv(elementColor);

    float[] out = hsv(baseColor);

    float dist = Math.min(1.5f * elementHsv[1] / baseHsv[1], 1f);
    if (baseHsv[1] <= 0.001f) {
      return elementColor;
    }

    out[0] = elementHsv[0] - baseHsv[0] + baseHsv[0];
    out[1] = elementHsv[1] * baseHsv[1] / baseHsv[1];
    out[2] = (elementHsv[2] / baseHsv[2] + dist - 1f) * baseHsv[2] / dist;
    if (out[2] < 0.3f) {
      return elementColor;
    }
    return Color.HSVToColor(255, out);
  }

  /**
   * انتقال hue یک رنگ به hue اکسنت داده شده و حفظ saturation/value خودِ رنگ.
   *
   * <p>برای گرادیان های چند استاپ (مثلاً حلقه یا ستاره) عالیه: عمق و پروفایل روشنایی رنگ پایه حفظ
   * می مونه ولی سایه رنگ به hue آواتار/اکسنت منتقل می شه. رنگ های بی رنگ (سدره) دست نمی خورن.
   */
  public static int recolor(int accent, int color) {
    float[] a = hsv(accent);
    if (a[1] <= 0.05f) {
      return color;
    }
    float[] c = hsv(color);
    float[] out = {a[0], c[1], c[2]};
    return Color.HSVToColor(Color.alpha(color), out);
  }

  /** ضرب RGB در {@code amount} (محدود به 0..255) بدون تغییر آلفا — همون changeBrightness تلگرام. */
  public static int changeBrightness(int color, float amount) {
    int r = (int) (Color.red(color) * amount);
    int g = (int) (Color.green(color) * amount);
    int b = (int) (Color.blue(color) * amount);
    r = r < 0 ? 0 : Math.min(r, 255);
    g = g < 0 ? 0 : Math.min(g, 255);
    b = b < 0 ? 0 : Math.min(b, 255);
    return Color.argb(Color.alpha(color), r, g, b);
  }

  /** روشنایی درک شده (rec.709 luma)، ۰ = سیاه، ۱ = سفید. */
  public static float perceivedBrightness(int color) {
    return (Color.red(color) * 0.2126f + Color.green(color) * 0.7152f + Color.blue(color) * 0.0722f)
        / 255f;
  }

  public static boolean isDarkColor(int color) {
    return perceivedBrightness(color) < 0.5f;
  }

  /** فاصله ی رنگی رد مین (weighted) تلگرام؛ برای مقایسه ی نزدیکی دو رنگ. */
  public static int colorDistance(int color1, int color2) {
    int r1 = Color.red(color1);
    int g1 = Color.green(color1);
    int b1 = Color.blue(color1);

    int r2 = Color.red(color2);
    int g2 = Color.green(color2);
    int b2 = Color.blue(color2);

    int rMean = (r1 + r2) / 2;
    int r = r1 - r2;
    int g = g1 - g2;
    int b = b1 - b2;
    return (((512 + rMean) * r * r) >> 8) + (4 * g * g) + (((767 - rMean) * b * b) >> 8);
  }

  public static int averageColor(int color1, int color2) {
    int r1 = Color.red(color1);
    int r2 = Color.red(color2);
    int g1 = Color.green(color1);
    int g2 = Color.green(color2);
    int b1 = Color.blue(color1);
    int b2 = Color.blue(color2);
    return Color.argb(255, (r1 / 2 + r2 / 2), (g1 / 2 + g2 / 2), (b1 / 2 + b2 / 2));
  }

  public static int withAlpha(int color, int alpha) {
    return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
  }

  private static float[] hsv(int color) {
    float[] hsv = new float[3];
    Color.colorToHSV(color, hsv);
    return hsv;
  }
}
