package ir.hanzodev1375.components.sheet.customitemsheet.ui;

import android.content.Context;
import com.example.liquidglass.LiquidGlassFab;
import ir.hanzodev1375.components.R;
import ir.hanzodev1375.components.utils.ComponentsPrefs;
import android.util.AttributeSet;
import ir.theme.M3Theme;

public class FabGlass extends LiquidGlassFab {
  private ComponentsPrefs setting;

  public FabGlass(Context c) {
    super(c);
    init();
  }

  public FabGlass(Context c, AttributeSet s) {
    super(c, s);
    init();
  }

  public FabGlass(Context c, AttributeSet s, int defStyle) {
    super(c, s, defStyle);
    init();
  }

  void init() {
    setting = new ComponentsPrefs(getContext());
    if (setting.isGlassMaterialColor()) {
      setGlassTint(fallback(M3Theme.surface(), 0), setting.getGlassTint());
    }
  }

  private static int fallback(Integer value, int def) {
    return value != null ? value : def;
  }
}
