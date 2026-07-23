package ir.hanzodev1375.ghostide.postman.util;

import android.content.Context;
import android.util.TypedValue;

/** Resolves a theme attribute (e.g. R.attr.colorPrimary) to an actual ARGB color int. */
public class ColorUtils {
    public static int resolveAttrColor(Context context, int attrResId) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(attrResId, typedValue, true);
        return typedValue.data;
    }
}
