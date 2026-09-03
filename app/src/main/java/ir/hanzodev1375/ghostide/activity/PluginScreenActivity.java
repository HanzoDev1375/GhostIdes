package ir.hanzodev1375.ghostide.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import ir.hanzodev1375.components.views.GhostToast;

import java.util.List;

import androidx.fragment.app.Fragment;

import ir.hanzodev1375.components.childern.ViewChilder;
import ir.hanzodev1375.ghostide.R;
import ir.hanzodev1375.ghostide.ide.ui.api.PluginScreen;
import ir.hanzodev1375.ghostide.ide.ui.api.PluginUiExtensionPoints;
import ir.hanzodev1375.ghostide.plugin.api.GlobalRegistry;

/**
 * Hosts one {@link PluginScreen} at a time, chosen by {@link #EXTRA_SCREEN_ID}. This is the only
 * Activity a plugin's screen ever runs inside; the plugin itself never declares one.
 */
public class PluginScreenActivity extends BaseCompat {

  public static final String EXTRA_SCREEN_ID = "ir.hanzodev1375.ghostide.extra.PLUGIN_SCREEN_ID";

  public static Intent createIntent(Context context, String screenId) {
    Intent intent = new Intent(context, PluginScreenActivity.class);
    intent.putExtra(EXTRA_SCREEN_ID, screenId);
    return intent;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_plugin_screen);
    setupBackgroundBlur();

    String screenId = getIntent().getStringExtra(EXTRA_SCREEN_ID);
    PluginScreen screen = findScreen(screenId);
    if (screen == null) {
      GhostToast.makeText(this, "Plugin screen not found: " + screenId, GhostToast.LENGTH_SHORT).show();
      finish();
      return;
    }

    setTitle(screen.getTitle());
    if (savedInstanceState == null) {
      Fragment fragment = screen.createFragment();
      getSupportFragmentManager()
          .beginTransaction()
          .replace(R.id.plugin_screen_container, fragment)
          .commit();
    }
  }

  private void setupBackgroundBlur() {
    ViewChilder background = findViewById(R.id.backgroundIconPluginScreen);
    View container = findViewById(R.id.plugin_screen_container);
    if (background == null || container == null) return;
    setupBackgroundBlur(background, container);
  }

  private static PluginScreen findScreen(String screenId) {
    if (screenId == null) {
      return null;
    }
    List<PluginScreen> screens =
        GlobalRegistry.extensions().extensions(PluginUiExtensionPoints.PLUGIN_SCREEN);
    for (PluginScreen screen : screens) {
      if (screenId.equals(screen.getId())) {
        return screen;
      }
    }
    return null;
  }
}
