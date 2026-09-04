package ir.hanzodev1375.ghostide.activity;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import ir.hanzodev1375.components.views.GhostToast;
import ir.hanzodev1375.components.utils.ParticleItemAnimator;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import ir.hanzodev1375.ghostide.R;
import ir.hanzodev1375.ghostide.adapters.InstalledPluginAdapter;
import ir.hanzodev1375.ghostide.models.InstalledPluginInfo;
import ir.hanzodev1375.ghostide.databinding.ActivityPluginManagerBinding;
import ir.hanzodev1375.ghostide.plugin.api.PluginSetupAction;
import ir.hanzodev1375.ghostide.plugin.gpl.GplInstalledPlugins;
import ir.hanzodev1375.ghostide.plugin.gpl.GplManifest;
import ir.hanzodev1375.ghostide.plugin.gpl.GplManifestReader;
import ir.hanzodev1375.ghostide.plugin.gpl.GplPluginLoader;
import ir.hanzodev1375.ghostide.plugin.gpl.LoadedGplPlugin;
import ir.hanzodev1375.ghostide.terminal.activity.TerminalActivity;
import ir.hanzodev1375.components.sheet.customitemsheet.ui.DialogCompat;
import ir.theme.M3Theme;
/**
 * Lets the user browse to a {@code .gpl} file, install it, search installed plugins, and
 * uninstall them. A plugin here may contribute a {@code PluginScreen}, an {@code
 * LspServerProvider}, or both; this screen only manages which ones are present, never their
 * contributed UI.
 */
public class PluginManagerActivity extends BaseCompat {

  private static final String GPL_EXTENSION = ".gpl";
  private static final String TAG = "PluginManagerActivity";

  private ActivityPluginManagerBinding bind;
  private GplPluginLoader loader;
  private File installDir;
  private InstalledPluginAdapter adapter;

  private final ActivityResultLauncher<String[]> openDocumentLauncher =
      registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::onDocumentPicked);

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    bind = ActivityPluginManagerBinding.inflate(getLayoutInflater());
    setContentView(bind.getRoot());
    setupBackgroundBlur();

    loader = GplPluginLoader.getInstance(this);
    installDir = GplInstalledPlugins.installDir(this);

    adapter = new InstalledPluginAdapter(this::onUninstall);
    bind.rvPlugins.setLayoutManager(new LinearLayoutManager(this));
    bind.rvPlugins.setAdapter(adapter);

    bind.btnBack.setOnClickListener(v -> finish());
    bind.fab.setOnClickListener(v -> openDocumentLauncher.launch(new String[] {"*/*"}));

    bind.searchLayout.setOnTextChangedListener(this::onSearchTextChanged);
    bind.searchLayout.show();
    bind.rvPlugins.setItemAnimator(new ParticleItemAnimator(this));

    setupInsets();
    refreshList();

    M3Theme.applyTopLevel(bind.getRoot());
  }

  private void setupBackgroundBlur() {
    setupBackgroundBlur(bind.backgroundIconPlugins, bind.headtop, bind.pluginContent);
  }

  @Override
  protected void onDestroy() {
    bind = null;
    super.onDestroy();
  }

  private void setupInsets() {
    ViewCompat.setOnApplyWindowInsetsListener(
        bind.coordinator,
        (view, insets) -> {
          Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
          bind.headtop.setPadding(
              bind.headtop.getPaddingLeft(),
              systemBars.top,
              bind.headtop.getPaddingRight(),
              bind.headtop.getPaddingBottom());
          bind.rvPlugins.setPadding(
              bind.rvPlugins.getPaddingLeft(),
              bind.rvPlugins.getPaddingTop(),
              bind.rvPlugins.getPaddingRight(),
              systemBars.bottom + dp(88));
          bind.fab.post(
              () -> {
                ViewGroup.MarginLayoutParams params =
                    (ViewGroup.MarginLayoutParams) bind.fab.getLayoutParams();
                params.bottomMargin = dp(16) + systemBars.bottom;
                bind.fab.setLayoutParams(params);
              });
          return insets;
        });
  }

  private int dp(int value) {
    return Math.round(value * getResources().getDisplayMetrics().density);
  }

  private void onSearchTextChanged(String text) {
    adapter.filter(text);
    bind.emptyText.setVisibility(adapter.isEmpty() ? View.VISIBLE : View.GONE);
  }

  private void refreshList() {
    File[] files = installDir.listFiles((dir, name) -> name.endsWith(GPL_EXTENSION));
    List<InstalledPluginInfo> plugins = new ArrayList<>();
    if (files != null) {
      for (File file : files) {
        try {
          GplManifest manifest = GplManifestReader.read(file);
          if (manifest == null) {
            Log.w(TAG, "Skipping plugin with unreadable manifest: " + file);
            continue;
          }
          if (!loader.isLoaded(manifest.id())) {
            loader.load(file);
          }
          plugins.add(new InstalledPluginInfo(file, manifest, loader.isLoaded(manifest.id())));
        } catch (IOException | ReflectiveOperationException | RuntimeException e) {
          Log.e(TAG, "Failed to load " + file, e);
          GhostToast.makeText(
                  this,
                  getString(R.string.plugin_manager_install_error, file.getName()),
                  GhostToast.LENGTH_SHORT)
              .show();
        }
      }
    }
    adapter.submit(plugins);
    bind.emptyText.setVisibility(adapter.isEmpty() ? View.VISIBLE : View.GONE);
  }

  private void onUninstall(InstalledPluginInfo plugin) {
    loader.unload(plugin.manifest().id());
    if (plugin.file().delete()) {
      GhostToast.makeText(
              this,
              getString(R.string.plugin_manager_uninstalled_toast, plugin.manifest().name()),
              GhostToast.LENGTH_SHORT)
          .show();
    }
    refreshList();
  }

  private void onDocumentPicked(Uri uri) {
    if (uri == null) {
      return;
    }
    String displayName = queryDisplayName(uri);
    if (displayName != null && !displayName.toLowerCase(Locale.ROOT).endsWith(GPL_EXTENSION)) {
      GhostToast.makeText(this, R.string.plugin_manager_not_gpl_file, GhostToast.LENGTH_SHORT).show();
      return;
    }

    File tempFile = new File(getCacheDir(), "incoming.gpl");
    File cleanupOnFailure = tempFile;
    try {
      copyUriToFile(uri, tempFile);
      GplManifest manifest = GplManifestReader.read(tempFile);
      if (manifest == null) {
        throw new IOException("Could not read plugin manifest from " + uri);
      }
      File installedFile = new File(installDir, manifest.id() + GPL_EXTENSION);
      if (!tempFile.renameTo(installedFile)) {
        throw new IOException("Could not move installed plugin into place");
      }
      cleanupOnFailure = installedFile;
      LoadedGplPlugin loadedPlugin = loader.load(installedFile);
      GhostToast.makeText(
              this,
              getString(R.string.plugin_manager_installed_toast, manifest.name()),
              GhostToast.LENGTH_SHORT)
          .show();
      refreshList();
      showSetupActionsIfAny(loadedPlugin);
    } catch (IOException | ReflectiveOperationException | RuntimeException e) {
      Log.e(TAG, "Failed to install " + uri, e);
      cleanupOnFailure.delete();
      GhostToast.makeText(
              this,
              getString(
                  R.string.plugin_manager_install_error,
                  e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()),
              GhostToast.LENGTH_LONG)
          .show();
    }
  }

  private void showSetupActionsIfAny(LoadedGplPlugin loadedPlugin) {
    List<PluginSetupAction> actions = loadedPlugin.getPlugin().getSetupActions();
    if (actions.isEmpty()) {
      return;
    }
    StringBuilder message = new StringBuilder();
    StringBuilder combinedCommand = new StringBuilder();
    for (int i = 0; i < actions.size(); i++) {
      PluginSetupAction action = actions.get(i);
      message.append(action.label()).append(": ").append(action.command()).append('\n');
      if (i > 0) {
        combinedCommand.append(" && ");
      }
      combinedCommand.append(action.command());
    }
    new DialogCompat(this)
        .setTitle(loadedPlugin.getDescriptor().getName())
        .setMessage(message.toString().trim())
        .setPositiveButton(
            R.string.plugin_manager_run_setup,
            (dialog, which) -> {
              Intent intent = new Intent(this, TerminalActivity.class);
              intent.putExtra(TerminalActivity.EXTRA_COMMAND, combinedCommand.toString());
              startActivity(intent);
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private String queryDisplayName(Uri uri) {
    try (Cursor cursor =
        getContentResolver().query(uri, new String[] {OpenableColumns.DISPLAY_NAME}, null, null, null)) {
      if (cursor != null && cursor.moveToFirst()) {
        int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        if (index >= 0) {
          return cursor.getString(index);
        }
      }
    } catch (RuntimeException ignored) {
    }
    return null;
  }

  private void copyUriToFile(Uri uri, File destination) throws IOException {
    try (var input = getContentResolver().openInputStream(uri);
        var output = new FileOutputStream(destination)) {
      if (input == null) {
        throw new IOException("Could not open " + uri);
      }
      byte[] buffer = new byte[8192];
      int read;
      while ((read = input.read(buffer)) != -1) {
        output.write(buffer, 0, read);
      }
    }
  }
}
