package ir.hanzodev1375.ghostide.plugin;

import android.app.Activity;
import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.sidesheet.SideSheetDialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import ir.hanzodev1375.ghostide.ide.ui.api.EditorPanel;
import ir.hanzodev1375.ghostide.ide.ui.api.PluginStateMod;
import ir.hanzodev1375.ghostide.ide.ui.api.PluginUiExtensionPoints;
import ir.hanzodev1375.ghostide.plugin.api.GlobalRegistry;
import ir.theme.ThemeManager;
import ir.theme.ThemeUtils;
import ir.theme.WidgetTheme;

/**
 * Host for {@link EditorPanel} contributions on any screen. Reads everything registered at {@link
 * PluginUiExtensionPoints#EDITOR_PANEL}, exposes it as {@link #getPanels()}, and opens one panel
 * when {@link #showPanel(EditorPanel)} is called. The {@link PluginStateMod} returned by {@link
 * EditorPanel#getState()} decides how the panel is shown: side sheet (default), dialog, bottom sheet
 * or a (bottom sheet) dialog fragment. The returned {@link View} is created lazily and cached for
 * the lifetime of this host so a panel keeps its state between opens.
 */
public final class PluginPanelHost {

  private final Activity activity;
  private final ThemeUtils theme;
  private final Supplier<String> lastPathResolver;
  private final List<EditorPanel> panels;
  private final Map<String, View> views = new HashMap<>();

  public PluginPanelHost(Activity activity) {
    this(activity, null);
  }

  public PluginPanelHost(Activity activity, Supplier<String> lastPathResolver) {
    this.activity = activity;
    this.lastPathResolver = lastPathResolver;
    this.theme = new ThemeUtils(new ThemeManager(activity));
    this.panels =
        wrapLastPath(GlobalRegistry.extensions().extensions(PluginUiExtensionPoints.EDITOR_PANEL));
  }

  /**
   * First non-blank {@link EditorPanel#getLastPath()} among the registered panels, falling back
   * to {@code fallback} (e.g. the editor's current open file). Panels get the final say on the
   * path a {@code CodeRunnerHost.runCurrentFile()} should run.
   */
  public static String resolveLastPath(Supplier<String> fallback) {
    for (EditorPanel panel :
        GlobalRegistry.extensions().extensions(PluginUiExtensionPoints.EDITOR_PANEL)) {
      String path = panel.getLastPath();
      if (path != null && !path.isEmpty()) {
        return path;
      }
    }
    return fallback == null ? null : fallback.get();
  }

  /** Wraps panels so an unimplemented {@code getLastPath()} falls back to this host's path. */
  private List<EditorPanel> wrapLastPath(List<EditorPanel> originals) {
    List<EditorPanel> wrapped = new ArrayList<>(originals.size());
    for (EditorPanel panel : originals) {
      wrapped.add(new LastPathEditorPanel(panel));
    }
    return wrapped;
  }

  private final class LastPathEditorPanel implements EditorPanel {
    private final EditorPanel delegate;

    LastPathEditorPanel(EditorPanel delegate) {
      this.delegate = delegate;
    }

    @Override
    public String getId() {
      return delegate.getId();
    }

    @Override
    public String getTitle() {
      return delegate.getTitle();
    }

    @Override
    public View createView() {
      return delegate.createView();
    }

    @Override
    public String getLastPath() {
      String path = delegate.getLastPath();
      if ((path == null || path.isEmpty()) && lastPathResolver != null) {
        return lastPathResolver.get();
      }
      return path;
    }

    @Override
    public PluginStateMod getState() {
      return delegate.getState();
    }

    @Override
    public void setState(PluginStateMod state) {
      delegate.setState(state);
    }
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

    ViewGroup wrapper = buildWrapper(panel, content);

    switch (panel.getState()) {
      case DIALOG:
        showDialog(wrapper);
        break;
      case DIALOGFRAGMENT:
        showDialogFragment(panel, wrapper);
        break;
      case FRAGMENT:
        showFragment(panel, wrapper);
        break;
      case BOTTOMSHERTFRAGMENT:
        showBottomSheetFragment(panel, wrapper);
        break;
      case BOTTOMSHEETDIALOG:
        showBottomSheetDialog(wrapper);
        break;
      case SIDESHEET:
      default:
        showSideSheet(wrapper);
        break;
    }
  }

  private void showSideSheet(ViewGroup wrapper) {
    SideSheetDialog sheet = new SideSheetDialog(activity);
    sheet.setContentView(wrapper);
    sheet.getWindow().setNavigationBarColor(Color.TRANSPARENT);
    theme.applySideSheet(sheet);
    sheet.show();
  }

  private void showBottomSheetDialog(ViewGroup wrapper) {
    BottomSheetDialog sheet = new BottomSheetDialog(activity);
    sheet.setContentView(wrapper);
    sheet.getWindow().setNavigationBarColor(Color.TRANSPARENT);
    styleDialogWindow(sheet.getWindow());
    sheet.show();
  }

  private void showDialog(ViewGroup wrapper) {
    Dialog dialog = new Dialog(activity);
    dialog.setContentView(wrapper);
    dialog.getWindow().setNavigationBarColor(Color.TRANSPARENT);
    styleDialogWindow(dialog.getWindow());
    dialog.show();
  }

  private void showDialogFragment(EditorPanel panel, ViewGroup wrapper) {
    FragmentManager fm = fragmentManager();
    if (fm == null) {
      showDialog(wrapper);
      return;
    }
    String tag = "plugin_panel_dialog_" + panel.getId();
    Fragment previous = fm.findFragmentByTag(tag);
    if (previous != null) {
      fm.beginTransaction().remove(previous).commitNow();
    }
    new PanelDialogFragment(wrapper).show(fm, tag);
  }

  private void showBottomSheetFragment(EditorPanel panel, ViewGroup wrapper) {
    FragmentManager fm = fragmentManager();
    if (fm == null) {
      showBottomSheetDialog(wrapper);
      return;
    }
    String tag = "plugin_panel_sheet_" + panel.getId();
    Fragment previous = fm.findFragmentByTag(tag);
    if (previous != null) {
      fm.beginTransaction().remove(previous).commitNow();
    }
    new PanelBottomSheetFragment(wrapper).show(fm, tag);
  }

  private void showFragment(EditorPanel panel, ViewGroup wrapper) {
    FragmentManager fm = fragmentManager();
    if (fm == null) {
      showDialog(wrapper);
      return;
    }
    String tag = "plugin_panel_fragment_" + panel.getId();
    Fragment previous = fm.findFragmentByTag(tag);
    if (previous != null) {
      fm.beginTransaction().remove(previous).commitNow();
    }
    FragmentTransaction tx = fm.beginTransaction();
    tx.add(android.R.id.content, new PanelHostFragment(wrapper), tag);
    tx.commit();
  }

  private FragmentManager fragmentManager() {
    if (activity instanceof FragmentActivity) {
      return ((FragmentActivity) activity).getSupportFragmentManager();
    }
    return null;
  }

  private ViewGroup buildWrapper(EditorPanel panel, View content) {
    LinearLayout wrapper = new LinearLayout(activity);
    wrapper.setOrientation(LinearLayout.VERTICAL);

    LinearLayout headerBox = new LinearLayout(activity);
    headerBox.setOrientation(LinearLayout.VERTICAL);
    int pad = dp(16);
    headerBox.setPadding(pad, dp(12), pad, dp(12));

    TextView header = new TextView(activity);
    header.setText(panel.getTitle());
    header.setTextSize(15);
    header.setGravity(Gravity.CENTER_VERTICAL);
    header.setTypeface(header.getTypeface(), Typeface.BOLD);
    headerBox.addView(header, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    String lastPath = panel.getLastPath();
    if (lastPath != null && !lastPath.isEmpty()) {
      TextView path = new TextView(activity);
      path.setText(lastPath);
      path.setTextSize(12);
      path.setGravity(Gravity.CENTER_VERTICAL);
      headerBox.addView(path, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    styleHeader(headerBox);
    wrapper.addView(headerBox, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    View divider = new View(activity);
    divider.setBackgroundColor(0x223E4452);
    wrapper.addView(divider, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));

    wrapper.addView(content, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
    return wrapper;
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

  private void styleHeader(LinearLayout headerBox) {
    WidgetTheme widget = theme.getTheme() == null ? null : theme.getTheme().getWidget();
    if (widget == null) {
      return;
    }
    String background = widget.getMenubackground();
    if (background != null) {
      headerBox.setBackground(new ColorDrawable(Color.parseColor(background)));
    }
    String text = widget.getMenutextcolor();
    if (text != null) {
      int color = Color.parseColor(text);
      for (int i = 0; i < headerBox.getChildCount(); i++) {
        if (headerBox.getChildAt(i) instanceof TextView) {
          ((TextView) headerBox.getChildAt(i)).setTextColor(color);
        }
      }
    }
  }

  private void styleDialogWindow(Window window) {
    var editor = theme.getTheme() == null ? null : theme.getTheme().getEditor();
    if (editor == null || editor.getCompletionWndBackground() == null) {
      return;
    }
    try {
      window
          .getDecorView()
          .setBackgroundTintList(
              ColorStateList.valueOf(Color.parseColor(editor.getCompletionWndBackground())));
    } catch (Exception ignored) {
    }
  }

  /** Reusable fragment that just hosts a panel view. */
  private static final class PanelHostFragment extends Fragment {
    private View content;

    public PanelHostFragment() {
      // Required for FragmentManager restoration.
    }

    PanelHostFragment(View content) {
      this.content = content;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      return attach(content);
    }
  }

  /** Dialog-hosted panel. */
  private static final class PanelDialogFragment extends androidx.fragment.app.DialogFragment {
    private View content;

    public PanelDialogFragment() {
      // Required for FragmentManager restoration.
    }

    PanelDialogFragment(View content) {
      this.content = content;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      return attach(content);
    }
  }

  /** Bottom sheet hosted panel. */
  private static final class PanelBottomSheetFragment extends BottomSheetDialogFragment {
    private View content;

    public PanelBottomSheetFragment() {
      // Required for FragmentManager restoration.
    }

    PanelBottomSheetFragment(View content) {
      this.content = content;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      return attach(content);
    }
  }

  private static View attach(View content) {
    if (content == null) {
      return null;
    }
    if (content.getParent() instanceof ViewGroup) {
      ((ViewGroup) content.getParent()).removeView(content);
    }
    return content;
  }

  private int dp(int value) {
    float density = activity.getResources().getDisplayMetrics().density;
    return Math.round(value * density);
  }
}
