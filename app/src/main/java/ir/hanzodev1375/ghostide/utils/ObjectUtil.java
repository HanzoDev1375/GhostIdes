package ir.hanzodev1375.ghostide.utils;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import com.google.android.material.R;
import com.google.android.material.color.MaterialColors;
import com.skydoves.powermenu.MenuAnimation;
import com.skydoves.powermenu.PowerMenu;
import ir.hanzodev1375.ghostide.GhostIdeAppLoader;
import ir.theme.ThemeManager;
import ir.theme.ThemeUtils;

public class ObjectUtil {
  public static void showFixPos(PowerMenu menu, View view) {
    int[] location = new int[2];
    view.getLocationOnScreen(location);
    int x = location[0];
    int y = location[1];
    var dm = view.getResources().getDisplayMetrics();
    int screenHeight = dm.heightPixels;
    int menuHeight = menu.getContentViewHeight();
    if (menuHeight <= 0) menuHeight = 200;
    int spaceBelow = screenHeight - (y + view.getHeight());
    int spaceAbove = y;
    if (spaceBelow < menuHeight && spaceAbove > spaceBelow) y -= menuHeight;
    else y += view.getHeight();
    menu.showAtLocation(view, Gravity.TOP | Gravity.START, x, y);
  }

  public static PowerMenu stepMenu(Context context, View view) {
    var menu = new PowerMenu.Builder(context).setIsMaterial(true).build();
    var setting = GhostIdeAppLoader.getInstance().getSetting();
    var themeManager = new ThemeManager(context);
    var themeUtil = new ThemeUtils(themeManager);
    var widgetImpl = themeUtil.getTheme().getWidget();
    menu.setAutoDismiss(true);
    menu.setMenuColor(
        setting.isShowBackground()
            ? Color.parseColor(widgetImpl.getMenubackground())
            : MaterialColors.getColor(context, R.attr.colorSurface, 0));
    menu.setTextColor(
        setting.isShowBackground()
            ? Color.parseColor(widgetImpl.getMenutextcolor())
            : MaterialColors.getColor(context, R.attr.colorOnSurface, 0));
    menu.setShowBackground(false);
    menu.setMenuRadius(25f);
    menu.setMenuShadow(3f);
    menu.setAnimation(MenuAnimation.FADE);
    menu.setIconColor(
        setting.isShowBackground()
            ? Color.parseColor(widgetImpl.getMenutextcolor())
            : MaterialColors.getColor(context, R.attr.colorOnSurface, 0));
    //showFixPos(menu, view);
    return menu;
  }
}
