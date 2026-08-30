package ir.hanzodev1375.components.sheet.customitemsheet.ui;

import android.content.Context;
import com.example.liquidglass.LiquidGlassView;
import android.util.AttributeSet;
import com.google.android.material.color.MaterialColors;
import ir.hanzodev1375.components.R;
import ir.hanzodev1375.ghostide.codeeditors.setting.PreferencesUtils;

public class GlassCompat extends LiquidGlassView {
  private PreferencesUtils setting;

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
    setting = new PreferencesUtils(getContext());
    if (setting.isGlassMaterialColor()) {
      setGlassTint(MaterialColors.getColor(this, R.attr.colorSurface), 0.6f);
    }
  }
}
