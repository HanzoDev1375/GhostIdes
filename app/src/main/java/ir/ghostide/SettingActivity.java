package ir.hanzodev1375.ghostide.activity;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import ir.hanzodev1375.ghostide.R;
import ir.hanzodev1375.ghostide.adapters.SettingsAdapter;
import ir.hanzodev1375.ghostide.customui.ExpandableLayout;
import ir.hanzodev1375.ghostide.models.SettingItem;
import ir.hanzodev1375.ghostide.codeeditors.setting.PreferencesUtils;
import java.io.File;
import java.io.FileInputStream;
import ir.theme.GhostTheme;
import com.google.gson.Gson;
import ir.theme.ThemeManager;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class SettingActivity extends BaseCompat {

  private PreferencesUtils prefs;
  protected ExpandableLayout expandEditor, expandApp;
  private RecyclerView rvEditor, rvApp;
  private SettingsAdapter editorAdapter, appAdapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_setting);

    prefs = new PreferencesUtils(this);

    com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    if (getSupportActionBar() != null) {
      getSupportActionBar().setDisplayHomeAsUpEnabled(true);
      getSupportActionBar().setTitle(R.string.settings_title);
    }

    expandEditor = findViewById(R.id.expandEditor);
    expandApp = findViewById(R.id.expandApp);

    expandEditor.setTitle(getString(R.string.section_editor));
    expandApp.setTitle(getString(R.string.section_app));

    rvEditor = expandEditor.getRecyclerView();
    rvApp = expandApp.getRecyclerView();
    rvEditor.setLayoutManager(new LinearLayoutManager(this));
    rvApp.setLayoutManager(new LinearLayoutManager(this));
    editorAdapter = new SettingsAdapter(getEditorItems());
    appAdapter = new SettingsAdapter(getAppItems());

    rvEditor.setAdapter(editorAdapter);
    rvApp.setAdapter(appAdapter);
    editorAdapter.setOnItemClickListener(
        position -> {
          if (position == 17) showTabSizeDialog();
          else if (position == 18) showLineHeightDialog();
          else if (position == 19) showCursorBlinkDialog();
        });

    appAdapter.setOnItemClickListener(
        position -> {
          if (position == 0) showBufferSizeDialog();
          else if (position == 1) showThemeDialog();
          else if (position == 2) showLoadThemeDialog();
        });
  }

  private List<SettingItem> getEditorItems() {
    List<SettingItem> items = new ArrayList<>();
    items.add(
        new SettingItem(
            getString(R.string.pref_auto_save),
            getString(R.string.pref_auto_save_desc),
            prefs.autoSaveFiles(),
            0,
            prefs::setAutoSave));
    items.add(
        new SettingItem(
            getString(R.string.pref_auto_complete),
            getString(R.string.pref_auto_complete_desc),
            prefs.enableAutoComplete(),
            0,
            prefs::setAutoComplete));
    items.add(
        new SettingItem(
            getString(R.string.pref_auto_complete_animation),
            getString(R.string.pref_auto_complete_animation_desc),
            prefs.enableAutoCompleteWindowAnimation(),
            0,
            prefs::setAutoCompleteWindowAnimation));
    items.add(
        new SettingItem(
            getString(R.string.pref_auto_close_bracket),
            getString(R.string.pref_auto_close_bracket_desc),
            prefs.enableBracketAutoClosing(),
            0,
            prefs::setBracketAutoClosing));
    items.add(
        new SettingItem(
            getString(R.string.pref_bracket_highlight),
            getString(R.string.pref_bracket_highlight_desc),
            prefs.enableBracketHighlight(),
            0,
            prefs::setBracketHighlight));
    items.add(
        new SettingItem(
            getString(R.string.pref_line_numbers),
            getString(R.string.pref_line_numbers_desc),
            prefs.enableLineNumbers(),
            0,
            prefs::setLineNumbers));
    items.add(
        new SettingItem(
            getString(R.string.pref_pin_line_numbers),
            getString(R.string.pref_pin_line_numbers_desc),
            prefs.pinLineNumber(),
            0,
            prefs::setPinLineNumber));
    items.add(
        new SettingItem(
            getString(R.string.pref_word_wrap),
            getString(R.string.pref_word_wrap_desc),
            prefs.useWordWrap(),
            0,
            prefs::setWordWrap));
    items.add(
        new SettingItem(
            getString(R.string.pref_tab_indent),
            getString(R.string.pref_tab_indent_desc),
            prefs.useTabIndentation(),
            0,
            prefs::setTabIndentation));
    items.add(
        new SettingItem(
            getString(R.string.pref_font_ligatures),
            getString(R.string.pref_font_ligatures_desc),
            prefs.useFontLigatures(),
            0,
            prefs::setFontLigatures));
    items.add(
        new SettingItem(
            getString(R.string.pref_icu_library),
            getString(R.string.pref_icu_library_desc),
            prefs.useICULibrary(),
            0,
            prefs::setICULibrary));
    items.add(
        new SettingItem(
            getString(R.string.pref_magnifier),
            getString(R.string.pref_magnifier_desc),
            prefs.enableMagnifier(),
            0,
            prefs::setMagnifier));
    items.add(
        new SettingItem(
            getString(R.string.pref_sticky_scroll),
            getString(R.string.pref_sticky_scroll_desc),
            prefs.enableStickyScroll(),
            0,
            prefs::setStickyScroll));
    items.add(
        new SettingItem(
            getString(R.string.pref_scroll_bar),
            getString(R.string.pref_scroll_bar_desc),
            prefs.enableScrollBar(),
            0,
            prefs::setScrollBar));
    items.add(
        new SettingItem(
            getString(R.string.pref_hardware_acceleration),
            getString(R.string.pref_hardware_acceleration_desc),
            prefs.enableHardWareAcceleration(),
            0,
            prefs::setHardwareAcceleration));
    items.add(
        new SettingItem(
            getString(R.string.pref_delete_empty_line),
            getString(R.string.pref_delete_empty_line_desc),
            prefs.enableDeleteEmptyLine(),
            0,
            prefs::setDeleteEmptyLine));
    items.add(
        new SettingItem(
            getString(R.string.pref_delete_tab),
            getString(R.string.pref_delete_tab_desc),
            prefs.enableDeleteTab(),
            0,
            prefs::setDeleteTab));
    items.add(
        new SettingItem(
            getString(R.string.pref_tab_size),
            getString(R.string.pref_tab_size_desc),
            false,
            0,
            null));
    items.add(
        new SettingItem(
            getString(R.string.pref_line_height),
            getString(R.string.pref_line_height_desc),
            false,
            0,
            null));
    items.add(
        new SettingItem(
            getString(R.string.pref_cursor_blink_period),
            getString(R.string.pref_cursor_blink_period_desc),
            false,
            0,
            null));
    items.add(
        new SettingItem(
            getString(R.string.pref_minimap),
            getString(R.string.pref_minimap_dec),
            prefs.enableMiniMap(),
            0,
            prefs::setMiniMap));
    return items;
  }

  private List<SettingItem> getAppItems() {
    List<SettingItem> items = new ArrayList<>();
    String current =
        String.format(
            getString(R.string.current_value), (prefs.getCurrentBufferSize() / 1024) + " KB");
    items.add(
        new SettingItem(
            getString(R.string.pref_buffer_size),
            getString(R.string.pref_buffer_size_desc) + "\n" + current,
            false,
            0,
            null));

    items.add(
        new SettingItem(
            getString(R.string.pref_app_theme),
            getString(R.string.pref_app_theme_desc),
            false,
            0,
            null));
    items.add(
        new SettingItem(
            getString(R.string.pref_load_theme_file),
            getString(R.string.pref_load_theme_file_desc),
            false,
            0,
            null));
    return items;
  }

  private void showTabSizeDialog() {
    int[] sizes = {2, 4, 6, 8};
    String[] labels = {
      getString(R.string.tab_size_2),
      getString(R.string.tab_size_4),
      getString(R.string.tab_size_6),
      getString(R.string.tab_size_8)
    };
    int current = prefs.getCodeEditorTabSize();
    int checked = 0;
    for (int i = 0; i < sizes.length; i++) if (sizes[i] == current) checked = i;
    new MaterialAlertDialogBuilder(this)
        .setTitle(R.string.pref_tab_size)
        .setSingleChoiceItems(
            labels,
            checked,
            (d, which) -> {
              prefs.setCodeEditorTabSize(sizes[which]);
              d.dismiss();
            })
        .setNegativeButton(R.string.cancel, null)
        .show();
  }

  private void showLineHeightDialog() {
    float current = prefs.getCurrentEditorLineHeight();
    String[] values = {"1", "2", "3", "4"};
    String[] labels = {
      getString(R.string.line_height_1),
      getString(R.string.line_height_2),
      getString(R.string.line_height_3),
      getString(R.string.line_height_4)
    };
    int checked = ((int) current) - 1;
    if (checked < 0) checked = 1;
    new MaterialAlertDialogBuilder(this)
        .setTitle(R.string.pref_line_height)
        .setSingleChoiceItems(
            labels,
            checked,
            (d, which) -> {
              prefs.setLineHeight(values[which]);
              d.dismiss();
            })
        .show();
  }

  private void showCursorBlinkDialog() {
    View view = getLayoutInflater().inflate(R.layout.dialog_slider, null);
    Slider slider = view.findViewById(R.id.slider);
    TextView valueText = view.findViewById(R.id.slider_value);
    slider.setValueFrom(200);
    slider.setValueTo(1000);
    slider.setStepSize(50);
    slider.setValue(prefs.getCursorBlinkPeriod());
    valueText.setText(String.format(getString(R.string.cursor_blink_ms), (int) slider.getValue()));
    slider.addOnChangeListener(
        (s, val, fromUser) ->
            valueText.setText(String.format(getString(R.string.cursor_blink_ms), (int) val)));
    new MaterialAlertDialogBuilder(this)
        .setTitle(R.string.pref_cursor_blink_period)
        .setView(view)
        .setPositiveButton(
            R.string.ok, (d, w) -> prefs.setCursorBlinkPeriod((int) slider.getValue()))
        .setNegativeButton(R.string.cancel, null)
        .show();
  }

  private void showBufferSizeDialog() {
    int current = prefs.getCurrentBufferSize() / 1024;
    String[] sizes = {"2", "4", "6", "8", "10", "12", "16", "20"};
    String[] labels = {
      getString(R.string.buffer_size_2),
      getString(R.string.buffer_size_4),
      getString(R.string.buffer_size_6),
      getString(R.string.buffer_size_8),
      getString(R.string.buffer_size_10),
      getString(R.string.buffer_size_12),
      getString(R.string.buffer_size_16),
      getString(R.string.buffer_size_20)
    };
    int checked = 0;
    for (int i = 0; i < sizes.length; i++) if (Integer.parseInt(sizes[i]) == current) checked = i;
    new MaterialAlertDialogBuilder(this)
        .setTitle(R.string.pref_buffer_size)
        .setSingleChoiceItems(
            labels,
            checked,
            (d, which) -> {
              prefs.setBufferSize(sizes[which]);
              d.dismiss();
              new MaterialAlertDialogBuilder(this)
                  .setTitle(R.string.restart_required)
                  .setMessage(R.string.restart_message)
                  .setPositiveButton(
                      R.string.restart_now,
                      (d2, w) -> android.os.Process.killProcess(android.os.Process.myPid()))
                  .setNegativeButton(R.string.later, null)
                  .show();
            })
        .show();
  }

  private void showThemeDialog() {
    String[] themeNames = getResources().getStringArray(R.array.theme_names);
    int currentTheme = prefs.getAppTheme();

    new MaterialAlertDialogBuilder(this)
        .setTitle(R.string.pref_app_theme)
        .setSingleChoiceItems(
            themeNames,
            currentTheme,
            (dialog, which) -> {
              if (which != currentTheme) {
                prefs.setAppTheme(which);
              }
              dialog.dismiss();
            })
        .setNegativeButton(R.string.cancel, null)
        .show();
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home) finish();
    return super.onOptionsItemSelected(item);
  }

  private void showLoadThemeDialog() {
    View v = getLayoutInflater().inflate(R.layout.layout_inputlayout, null, false);
    TextInputLayout input = v.findViewById(R.id.editor);
    input.setHint("/sdcard/GhostIDE/themes/draks.gth");
    input.getEditText().setText(!prefs.getAppThemeFile().isEmpty() ? prefs.getAppThemeFile() : "");
    new MaterialAlertDialogBuilder(this)
        .setTitle(getString(R.string.theme_load_title))
        .setMessage(getString(R.string.theme_load_message))
        .setView(v)
        .setPositiveButton(
            R.string.ok,
            (dialog, which) -> {
              String path = input.getEditText().getText().toString().trim();
              if (path.isEmpty()) {
                Toast.makeText(this, getString(R.string.theme_load_empty_path), Toast.LENGTH_SHORT)
                    .show();
                return;
              }
              if (!path.endsWith(".gth")) {
                Toast.makeText(
                        this, getString(R.string.theme_load_invalid_extension), Toast.LENGTH_SHORT)
                    .show();
                return;
              }
              File file = new File(path);
              if (!file.exists()) {
                Toast.makeText(
                        this,
                        String.format(getString(R.string.theme_load_file_not_found), path),
                        Toast.LENGTH_SHORT)
                    .show();
                return;
              }
              try {
                String json =
                    new String(new FileInputStream(file).readAllBytes(), StandardCharsets.UTF_8);
                GhostTheme theme = new Gson().fromJson(json, GhostTheme.class);
                if (theme == null) throw new Exception("Invalid theme format");
                new ThemeManager(this).saveTheme(theme);
                prefs.setAppThemeFile(path);
                Toast.makeText(this, getString(R.string.theme_load_success), Toast.LENGTH_LONG)
                    .show();
              } catch (Exception e) {
                Toast.makeText(
                        this,
                        String.format(getString(R.string.theme_load_error), e.getMessage()),
                        Toast.LENGTH_SHORT)
                    .show();
              }
            })
        .setNegativeButton(
            R.string.cancel,
            (dialog, which) -> {
              new ThemeManager(this).resetToDefault();
              
            })
        .show();
  }
}
