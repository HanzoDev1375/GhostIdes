package ir.hanzodev1375.components.sheet.customitemsheet.ui;

import android.content.Context;
import com.example.liquidglass.LiquidGlassView;
import android.util.AttributeSet;
import ir.hanzodev1375.components.utils.ComponentsPrefs;
import ir.theme.M3Theme;

public class GlassCompat extends LiquidGlassView {
  private ComponentsPrefs setting;

  public GlassCompat(Context c) {
    super(c);
    init();
  }

  public GlassCompat(Context c, AttributeSet s) {
    super(c, s);
    init();
  }

  public GlassCompat(Context c, AttributeSet s, int defStyle) {
    super(c, s, defStyle);
    init();
  }

  void init() {
    setting = new ComponentsPrefs(getContext());
    if (setting.isGlassMaterialColor()) {
      setGlassTint(M3Theme.surface(), setting.getGlassTint());
    }
  }
}
