package ir.hanzodev1375.ghostide.codeeditors.colorscheme;

import android.graphics.Color;
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme;

public class GhostColorScheme extends EditorColorScheme {
  private static int endColor = END_COLOR_ID;
  public static final int COLORNEXTDOT = ++endColor;
  public static final int COLORNEXTBRAK = ++endColor;
  public static final int COLORNEXTCHAR = ++endColor;
  public static final int COLORUPPERCASE = ++endColor;
  public static final int COLORNEXTLESS = ++endColor;
  public static final int BRACKET1 = ++endColor;
  public static final int BRACKET2 = ++endColor;
  public static final int BRACKET3 = ++endColor;
  public static final int BRACKET4 = ++endColor;
  public static final int BRACKET5 = ++endColor;
  public static final int BRACKET6 = ++endColor;

  @Override
  public void applyDefault() {
    super.applyDefault();
    setColor(COLORNEXTDOT, Color.parseColor("#ff3208"));
    setColor(COLORNEXTBRAK, Color.parseColor("#ff10ba"));
    setColor(COLORNEXTCHAR, Color.parseColor("#6ba108"));
    setColor(COLORUPPERCASE, Color.parseColor("#ff2c11"));
    setColor(WHOLE_BACKGROUND, Color.TRANSPARENT);
    setColor(COLORNEXTLESS, Color.parseColor("#ffc190"));
    setColor(BRACKET1, Color.parseColor("#FFDD00"));
    setColor(BRACKET2, Color.parseColor("#00D9FF"));
    setColor(BRACKET3, Color.parseColor("#00FF55"));
    setColor(BRACKET4, Color.parseColor("#FF6200"));
    setColor(BRACKET5, Color.parseColor("#FF64F5"));
    setColor(BRACKET6, Color.parseColor("#64FFD0"));
  }

  @Override
  public boolean isDark() {
    return true;
  }
}
