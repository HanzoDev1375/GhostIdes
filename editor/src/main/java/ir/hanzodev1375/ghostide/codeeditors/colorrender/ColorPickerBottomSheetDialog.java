package ir.hanzodev1375.ghostide.codeeditors.colorrender;

import android.content.Context;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import androidx.annotation.NonNull;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;
import ir.hanzodev1375.components.sheet.BaseSheet;
import ir.hanzodev1375.ghostide.codeeditors.R;

public class ColorPickerBottomSheetDialog {

  public interface OnColorPickedListener {
    void onColorPicked(int colorArgb);
  }

  // پرچم برای جلوگیری از حلقه بی‌نهایت هنگام تنظیم متن از سوی کد
  private static boolean settingText = false;

  public static void show(
      @NonNull Context context, int initialColor, @NonNull OnColorPickedListener listener) {

    var dialog = new BaseSheet(context);
    View view = LayoutInflater.from(context).inflate(R.layout.color_picker_bottom_sheet, null);
    ColorSpectrumView spectrum = view.findViewById(R.id.colorSpectrum);
    HueSliderView hueBar = view.findViewById(R.id.hueSliderView);
    Slider alphaSlider = view.findViewById(R.id.alphaSlider);
    ShapeableImageView preview = view.findViewById(R.id.colorPreview);
    TextInputEditText hexInput = view.findViewById(R.id.hexValue);
    TextInputEditText rgbInput = view.findViewById(R.id.rgbValue);
    TextInputEditText rgbaInput = view.findViewById(R.id.rgbaValue);
    TextInputEditText hslInput = view.findViewById(R.id.hslValue);
    TextInputEditText hslaInput = view.findViewById(R.id.hslaValue);

    Button confirmBtn = view.findViewById(R.id.confirmButton);

    final int[] currentColor = new int[] {initialColor};

    // مقداردهی اولیه
    int r = Color.red(initialColor);
    int g = Color.green(initialColor);
    int b = Color.blue(initialColor);
    int a = Color.alpha(initialColor);
    float[] hsl = rgbToHsl(r, g, b);

    spectrum.setHue(hsl[0]);
    spectrum.setColor(initialColor);
    hueBar.setHue(hsl[0]);
    alphaSlider.setValue(a);

    // نمایش اولیه
    updatePreview(
        spectrum,
        alphaSlider,
        preview,
        hexInput,
        rgbInput,
        rgbaInput,
        hslInput,
        hslaInput,
        currentColor);

    // لیسنر طیف رنگ
    spectrum.setOnColorChangedListener(
        newColor -> {
          int alpha = (int) alphaSlider.getValue();
          currentColor[0] =
              Color.argb(alpha, Color.red(newColor), Color.green(newColor), Color.blue(newColor));
          updatePreview(
              spectrum,
              alphaSlider,
              preview,
              hexInput,
              rgbInput,
              rgbaInput,
              hslInput,
              hslaInput,
              currentColor);
        });

    // لیسنر نوار hue
    hueBar.setOnHueChangedListener(
        hue -> {
          spectrum.setHue(hue); // خود spectrum دوباره رنگ را به‌روز می‌کند
        });

    // لیسنر اسلایدر alpha
    alphaSlider.addOnChangeListener(
        (slider, value, fromUser) -> {
          if (fromUser) {
            int colorWithoutAlpha = currentColor[0] & 0x00FFFFFF; // حذف alpha قبلی
            currentColor[0] =
                Color.argb(
                    (int) value,
                    Color.red(colorWithoutAlpha),
                    Color.green(colorWithoutAlpha),
                    Color.blue(colorWithoutAlpha));
            updatePreview(
                spectrum,
                alphaSlider,
                preview,
                hexInput,
                rgbInput,
                rgbaInput,
                hslInput,
                hslaInput,
                currentColor);
          }
        });

    // لیسنرهای ورودی متن
    TextWatcher textWatcher =
        new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {}

          @Override
          public void afterTextChanged(Editable s) {
            parseInput(
                hexInput,
                rgbInput,
                rgbaInput,
                hslInput,
                hslaInput,
                spectrum,
                hueBar,
                alphaSlider,
                preview,
                currentColor);
          }
        };

    hexInput.addTextChangedListener(textWatcher);
    rgbInput.addTextChangedListener(textWatcher);
    rgbaInput.addTextChangedListener(textWatcher);
    hslInput.addTextChangedListener(textWatcher);
    hslaInput.addTextChangedListener(textWatcher);

    confirmBtn.setOnClickListener(
        v -> {
          listener.onColorPicked(currentColor[0]);
          dialog.dismiss();
        });

    dialog.setContentView(view);
    dialog.show();
  }

  // ==================== متدهای به‌روزرسانی ====================

  private static void updatePreview(
      ColorSpectrumView spectrum,
      Slider alpha,
      ShapeableImageView preview,
      TextInputEditText hex,
      TextInputEditText rgb,
      TextInputEditText rgba,
      TextInputEditText hsl,
      TextInputEditText hsla,
      int[] currentColor) {

    int color = currentColor[0];
    int r = Color.red(color);
    int g = Color.green(color);
    int b = Color.blue(color);
    int a = Color.alpha(color);

    preview.setBackgroundColor(color);

    float[] hslArr = rgbToHsl(r, g, b);

    setTextWithoutTrigger(hex, rgbaToHex(r, g, b, a));
    setTextWithoutTrigger(rgb, toRgbString(r, g, b));
    setTextWithoutTrigger(rgba, toRgbaString(r, g, b, a));
    setTextWithoutTrigger(hsl, toHslString(hslArr[0], hslArr[1], hslArr[2]));
    setTextWithoutTrigger(hsla, toHslaString(hslArr[0], hslArr[1], hslArr[2], a));
  }

  private static void setTextWithoutTrigger(TextInputEditText editText, String text) {
    settingText = true;
    if (!text.equals(String.valueOf(editText.getText()))) {
      editText.setText(text);
    }
    settingText = false;
  }

  private static boolean isSettingText() {
    return settingText;
  }

  // ==================== تجزیه ورودی‌های متنی ====================

  private static void parseInput(
      TextInputEditText hex,
      TextInputEditText rgb,
      TextInputEditText rgba,
      TextInputEditText hsl,
      TextInputEditText hsla,
      ColorSpectrumView spectrum,
      HueSliderView hueBar,
      Slider alpha,
      ShapeableImageView preview,
      int[] currentColor) {

    if (isSettingText()) return; // جلوگیری از حلقه

    String hexStr = hex.getText().toString().trim();
    if (!hexStr.isEmpty()) {
      try {
        int[] rgbaArr = hexToRgba(hexStr);
        float[] hslArr = rgbToHsl(rgbaArr[0], rgbaArr[1], rgbaArr[2]);
        hueBar.setHue(hslArr[0]);
        spectrum.setHue(hslArr[0]);
        spectrum.setColor(Color.argb(255, rgbaArr[0], rgbaArr[1], rgbaArr[2]));
        alpha.setValue(rgbaArr[3]);
        currentColor[0] = Color.argb(rgbaArr[3], rgbaArr[0], rgbaArr[1], rgbaArr[2]);
        updatePreview(spectrum, alpha, preview, hex, rgb, rgba, hsl, hsla, currentColor);
        return;
      } catch (Exception ignored) {
      }
    }

    String rgbStr = rgb.getText().toString().trim();
    if (rgbStr.startsWith("rgb(") && rgbStr.endsWith(")")) {
      try {
        String[] parts = rgbStr.substring(4, rgbStr.length() - 1).split(",");
        int r = Integer.parseInt(parts[0].trim());
        int g = Integer.parseInt(parts[1].trim());
        int b = Integer.parseInt(parts[2].trim());
        float[] hslArr = rgbToHsl(r, g, b);
        hueBar.setHue(hslArr[0]);
        spectrum.setHue(hslArr[0]);
        spectrum.setColor(Color.rgb(r, g, b));
        currentColor[0] = Color.argb((int) alpha.getValue(), r, g, b);
        updatePreview(spectrum, alpha, preview, hex, rgb, rgba, hsl, hsla, currentColor);
        return;
      } catch (Exception ignored) {
      }
    }

    String rgbaStr = rgba.getText().toString().trim();
    if (rgbaStr.startsWith("rgba(") && rgbaStr.endsWith(")")) {
      try {
        String[] parts = rgbaStr.substring(5, rgbaStr.length() - 1).split(",");
        int r = Integer.parseInt(parts[0].trim());
        int g = Integer.parseInt(parts[1].trim());
        int b = Integer.parseInt(parts[2].trim());
        float aFloat = Float.parseFloat(parts[3].trim());
        int a = (int) (aFloat * 255);
        float[] hslArr = rgbToHsl(r, g, b);
        hueBar.setHue(hslArr[0]);
        spectrum.setHue(hslArr[0]);
        spectrum.setColor(Color.argb(255, r, g, b));
        alpha.setValue(a);
        currentColor[0] = Color.argb(a, r, g, b);
        updatePreview(spectrum, alpha, preview, hex, rgb, rgba, hsl, hsla, currentColor);
        return;
      } catch (Exception ignored) {
      }
    }

    String hslStr = hsl.getText().toString().trim();
    if (hslStr.startsWith("hsl(") && hslStr.endsWith(")")) {
      try {
        String[] parts = hslStr.substring(4, hslStr.length() - 1).split(",");
        float h = Float.parseFloat(parts[0].trim());
        float s = Float.parseFloat(parts[1].trim().replace("%", ""));
        float l = Float.parseFloat(parts[2].trim().replace("%", ""));
        hueBar.setHue(h);
        spectrum.setHue(h);
        int[] rgbArr = hslToRgb(h, s, l);
        spectrum.setColor(Color.rgb(rgbArr[0], rgbArr[1], rgbArr[2]));
        currentColor[0] = Color.argb((int) alpha.getValue(), rgbArr[0], rgbArr[1], rgbArr[2]);
        updatePreview(spectrum, alpha, preview, hex, rgb, rgba, hsl, hsla, currentColor);
        return;
      } catch (Exception ignored) {
      }
    }

    String hslaStr = hsla.getText().toString().trim();
    if (hslaStr.startsWith("hsla(") && hslaStr.endsWith(")")) {
      try {
        String[] parts = hslaStr.substring(5, hslaStr.length() - 1).split(",");
        float h = Float.parseFloat(parts[0].trim());
        float s = Float.parseFloat(parts[1].trim().replace("%", ""));
        float l = Float.parseFloat(parts[2].trim().replace("%", ""));
        float aFloat = Float.parseFloat(parts[3].trim());
        int a = (int) (aFloat * 255);
        hueBar.setHue(h);
        spectrum.setHue(h);
        int[] rgbArr = hslToRgb(h, s, l);
        spectrum.setColor(Color.argb(255, rgbArr[0], rgbArr[1], rgbArr[2]));
        alpha.setValue(a);
        currentColor[0] = Color.argb(a, rgbArr[0], rgbArr[1], rgbArr[2]);
        updatePreview(spectrum, alpha, preview, hex, rgb, rgba, hsl, hsla, currentColor);
      } catch (Exception ignored) {
      }
    }
  }

  // ==================== تبدیل‌های رنگی (دست‌نخورده) ====================

  private static float[] rgbToHsl(int r, int g, int b) {
    float rf = r / 255f;
    float gf = g / 255f;
    float bf = b / 255f;
    float max = Math.max(rf, Math.max(gf, bf));
    float min = Math.min(rf, Math.min(gf, bf));
    float delta = max - min;
    float hue = 0;
    float saturation = 0;
    float lightness = (max + min) / 2;
    if (delta != 0) {
      saturation = delta / (1 - Math.abs(2 * lightness - 1));
      if (max == rf) {
        hue = (gf - bf) / delta;
        if (gf < bf) hue += 6;
      } else if (max == gf) {
        hue = (bf - rf) / delta + 2;
      } else {
        hue = (rf - gf) / delta + 4;
      }
      hue *= 60;
    }
    return new float[] {hue, saturation * 100, lightness * 100};
  }

  private static int[] hslToRgb(float h, float s, float l) {
    float sat = s / 100f;
    float light = l / 100f;
    float c = (1 - Math.abs(2 * light - 1)) * sat;
    float hp = h / 60f;
    float x = c * (1 - Math.abs(hp % 2 - 1));
    float r1, g1, b1;
    if (hp < 1) {
      r1 = c;
      g1 = x;
      b1 = 0;
    } else if (hp < 2) {
      r1 = x;
      g1 = c;
      b1 = 0;
    } else if (hp < 3) {
      r1 = 0;
      g1 = c;
      b1 = x;
    } else if (hp < 4) {
      r1 = 0;
      g1 = x;
      b1 = c;
    } else if (hp < 5) {
      r1 = x;
      g1 = 0;
      b1 = c;
    } else {
      r1 = c;
      g1 = 0;
      b1 = x;
    }
    float m = light - c / 2;
    int r = Math.round((r1 + m) * 255);
    int g = Math.round((g1 + m) * 255);
    int b = Math.round((b1 + m) * 255);
    return new int[] {r, g, b};
  }

  private static String rgbaToHex(int r, int g, int b, int a) {
    return String.format("#%02X%02X%02X%02X", a, r, g, b);
  }

  private static int[] hexToRgba(String hex) {
    if (hex.startsWith("#")) hex = hex.substring(1);
    if (hex.length() == 6) {
      int r = Integer.parseInt(hex.substring(0, 2), 16);
      int g = Integer.parseInt(hex.substring(2, 4), 16);
      int b = Integer.parseInt(hex.substring(4, 6), 16);
      return new int[] {r, g, b, 255};
    } else {
      int a = Integer.parseInt(hex.substring(0, 2), 16);
      int r = Integer.parseInt(hex.substring(2, 4), 16);
      int g = Integer.parseInt(hex.substring(4, 6), 16);
      int b = Integer.parseInt(hex.substring(6, 8), 16);
      return new int[] {r, g, b, a};
    }
  }

  private static String toRgbString(int r, int g, int b) {
    return "rgb(" + r + ", " + g + ", " + b + ")";
  }

  private static String toRgbaString(int r, int g, int b, int a) {
    float alpha = a / 255f;
    return "rgba(" + r + ", " + g + ", " + b + ", " + String.format("%.2f", alpha) + ")";
  }

  private static String toHslString(float h, float s, float l) {
    return "hsl(" + Math.round(h) + ", " + Math.round(s) + "%, " + Math.round(l) + "%)";
  }

  private static String toHslaString(float h, float s, float l, int a) {
    float alpha = a / 255f;
    return "hsla("
        + Math.round(h)
        + ", "
        + Math.round(s)
        + "%, "
        + Math.round(l)
        + "%, "
        + String.format("%.2f", alpha)
        + ")";
  }
}
