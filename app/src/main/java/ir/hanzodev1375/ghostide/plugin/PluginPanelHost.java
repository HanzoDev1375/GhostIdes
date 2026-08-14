package ir.hanzodev1375.ghostide.plugin;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.sidesheet.SideSheetDialog;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ir.hanzodev1375.ghostide.ide.ui.api.EditorPanel;
import ir.hanzodev1375.ghostide.ide.ui.api.PluginUiExtensionPoints;
import ir.hanzodev1375.ghostide.plugin.api.GlobalRegistry;
import ir.theme.ThemeManager;
import ir.theme.ThemeUtils;
import ir.theme.WidgetTheme;

/**
 * Host for {@link EditorPanel} contributions on any screen. Reads everything registered at {@link
 * PluginUiExtensionPoints#EDITOR_PANEL}, exposes it as {@link #getPanels()}, and opens one panel
 * inside a themed side sheet when {@link #showPanel(EditorPanel)} is called. The returned {@link
 * View} is created lazily and cached for the lifetime of this host so a panel keeps its state
 * between opens.
 */
public final class PluginPanelHost {

  private final Activity activity;
  private final ThemeUtils theme;
  private final List<EditorPanel> panels =
      GlobalRegistry.extensions().extensions(PluginUiExtensionPoints.EDITOR_PANEL);
  private final Map<String, View> views = new HashMap<>();

  public PluginPanelHost(Activity activity) {
    this.activity = activity;
    this.theme = new ThemeUtils(new ThemeManager(activity));
  }

  public List<EditorPanel> getPanels() {
    return panels;
  }

  public boolean isEmpty() {
    return panels.isEmpty();
  }

  public void showPanel(EditorPanel panel) {
    if (panel == null || activity.isFinishing()) {
      return;
    }
    View content = views.get(panel.getId());
    if (content == null) {
      content = createPanelView(panel);
      views.put(panel.getId(), content);
    } else if (content.getParent() instanceof ViewGroup) {
      ((ViewGroup) content.getParent()).removeView(content);
    }

    LinearLayout wrapper = new LinearLayout(activity);
    wrapper.setOrientation(LinearLayout.VERTICAL);

    TextView header = new TextView(activity);
    header.setText(panel.getTitle());
    header.setTextSize(15);
    header.setGravity(Gravity.CENTER_VERTICAL);
    int pad = dp(16);
    header.setPadding(pad, dp(12), pad, dp(12));
    header.setTypeface(header.getTypeface(), android.graphics.Typeface.BOLD);
    styleHeader(header);
    wrapper.addView(header, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    View divider = new View(activity);
    divider.setBackgroundColor(0x223E4452);
    wrapper.addView(divider, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));

    wrapper.addView(content, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

    SideSheetDialog sheet = new SideSheetDialog(activity);
    sheet.setContentView(wrapper);
    sheet.getWindow().setNavigationBarColor(Color.TRANSPARENT);
    theme.applySideSheet(sheet);
    sheet.show();
  }

  private View createPanelView(EditorPanel panel) {
    try {
      return panel.createView();
    } catch (Exception e) {
      TextView error = new TextView(activity);
      error.setPadding(dp(16), dp(16), dp(16), dp(16));
      error.setText("Plugin panel '" + panel.getId() + "' failed: " + e);
      return error;
    }
  }

  private void styleHeader(TextView header) {
    WidgetTheme widget = theme.getTheme() == null ? null : theme.getTheme().getWidget();
    if (widget == null) {
      return;
    }
    String background = widget.getMenubackground();
    if (background != null) {
      header.setBackground(new ColorDrawable(Color.parseColor(background)));
    }
    String text = widget.getMenutextcolor();
    if (text != null) {
      header.setTextColor(Color.parseColor(text));
    }
  }

  private int dp(int value) {
    float density = activity.getResources().getDisplayMetrics().density;
    return Math.round(value * density);
  }
}
