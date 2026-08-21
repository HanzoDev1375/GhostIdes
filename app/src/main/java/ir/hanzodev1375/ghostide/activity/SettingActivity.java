package ir.hanzodev1375.ghostide.activity;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.os.LocaleListCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.blankj.utilcode.util.ThreadUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import ir.hanzodev1375.components.SearchLayout;
import ir.hanzodev1375.components.TextInputDialogFragment;
import ir.hanzodev1375.components.sheet.SliderSheet;
import ir.hanzodev1375.ghostide.R;
import ir.hanzodev1375.ghostide.adapters.SettingsAdapter;
import ir.hanzodev1375.ghostide.ai.utils.AiConstants;
import ir.hanzodev1375.ghostide.ai.utils.AiPreferencesUtils;
import ir.hanzodev1375.ghostide.appicon.AppIconChooserDialogBuilder;
import ir.hanzodev1375.ghostide.appicon.AppIconManager;
import ir.hanzodev1375.ghostide.codeeditors.setting.PreferencesUtils;
import ir.hanzodev1375.ghostide.codeeditors.ui.power.PowerModeEffectManager;
import ir.hanzodev1375.ghostide.codeeditors.util.TranslateLanguages;
import ir.hanzodev1375.ghostide.customui.ExpandableLayout;
import ir.hanzodev1375.ghostide.jgit.GitHubClient;
import ir.hanzodev1375.ghostide.models.SettingItem;
import ir.hanzodev1375.ghostide.themeengine.ThemeChooserDialogBuilder;
import ir.hanzodev1375.ghostide.themeengine.ThemeEngine;
import ir.hanzodev1375.ghostide.utils.FileUtil;
import ir.hanzodev1375.ghostide.utils.LocaleHelper;
import ir.theme.GhostTheme;
import ir.theme.ThemeManager;
import ir.theme.ThemeUtils;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class SettingActivity extends BaseCompat {

  private PreferencesUtils prefs;
  protected ExpandableLayout expandEditor, expandApp, LspView;
  private RecyclerView rvEditor, rvApp, rvLsp;
  private SettingsAdapter editorAdapter, appAdapter, lspAdapter;
  private ThemeEngine themeEngine;
  private AiPreferencesUtils aiPrefs;
  private SearchLayout ser;
  private boolean isSearchActive = false;
  private Runnable filterRunnable;
  private Runnable expandRunnable;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_setting);
    aiPrefs = new AiPreferencesUtils(this);
    prefs = new PreferencesUtils(this);
    setupBackgroundBlur();
    themeEngine = ThemeEngine.getInstance(this);
    MaterialToolbar toolbar = findViewById(R.id.toolbar);
    ser = findViewById(R.id.searchitem);

    setSupportActionBar(toolbar);
    if (getSupportActionBar() != null) {
      getSupportActionBar().setDisplayHomeAsUpEnabled(true);
      getSupportActionBar().setTitle(R.string.settings_title);
    }

    expandEditor = findViewById(R.id.expandEditor);
    expandApp = findViewById(R.id.expandApp);

    expandEditor.setTitle(getString(R.string.section_editor));
    expandApp.setTitle(getString(R.string.section_app));
    LspView = findViewById(R.id.lspsetting);
    LspView.setTitle(getString(R.string.lsptitles));

    rvEditor = expandEditor.getRecyclerView();
    rvApp = expandApp.getRecyclerView();
    rvLsp = LspView.getRecyclerView();
    rvEditor.setLayoutManager(new LinearLayoutManager(this));
    rvApp.setLayoutManager(new LinearLayoutManager(this));
    rvLsp.setLayoutManager(new LinearLayoutManager(this));
    editorAdapter = new SettingsAdapter(getEditorItems());
    appAdapter = new SettingsAdapter(getAppItems());
    lspAdapter = new SettingsAdapter(getLspSetting());
    ser.show();
    View root = findViewById(R.id.settingRoot);

    ViewCompat.setOnApplyWindowInsetsListener(
        root,
        (v, insets) -> {
          Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
          Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
          findViewById(R.id.appbar).setPadding(0, systemBars.top, 0, 0);
          int bottomInset = Math.max(systemBars.bottom, ime.bottom);
          ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) ser.getLayoutParams();
          lp.bottomMargin = bottomInset + (int) (4 * getResources().getDisplayMetrics().density);
          ser.setLayoutParams(lp);
          // adjust padding for insets
          NestedScrollView scrollView = findViewById(R.id.scrollView);
          scrollView.setPadding(0, 0, 0, bottomInset + (int) (80 * getResources().getDisplayMetrics().density));
          return insets;
        });

    rvEditor.setAdapter(editorAdapter);
    rvApp.setAdapter(appAdapter);
    rvLsp.setAdapter(lspAdapter);

    ExpandableLayout expandAi = findViewById(R.id.expandAi);
    expandAi.setTitle(getString(R.string.ai_section_title));
    RecyclerView rvAi = expandAi.getRecyclerView();
    rvAi.setLayoutManager(new LinearLayoutManager(this));
    SettingsAdapter aiAdapter = new SettingsAdapter(getAiItems());
    rvAi.setAdapter(aiAdapter);
    ser.setIconClose(R.drawable.ic_close);
    ser.setIconSearch(R.drawable.outline_search);

    // Prevents crash when DiffUtil updates lists while animations are running
    rvEditor.setItemAnimator(null);
    rvApp.setItemAnimator(null);
    rvLsp.setItemAnimator(null);
    rvAi.setItemAnimator(null);

    ser.setOnTextChangedListener(
        (item) -> {
          boolean hasText = item.length() > 0;

          if (hasText && !isSearchActive) {
            isSearchActive = true;

            ser.removeCallbacks(expandRunnable);
            expandRunnable =
                () -> {
                  if (!expandAi.isExpanded()) expandAi.expand();
                  if (!expandApp.isExpanded()) expandApp.expand();
                  if (!expandEditor.isExpanded()) expandEditor.expand();
                  if (!LspView.isExpanded()) LspView.expand();
                };

            ser.postDelayed(expandRunnable, 50);
          }

          ser.removeCallbacks(filterRunnable);
          filterRunnable =
              () -> {
                if (hasText) {
                  editorAdapter.filter(item);
                  appAdapter.filter(item);
                  lspAdapter.filter(item);
                  aiAdapter.filter(item);
                } else {
                  editorAdapter.resetToFull();
                  appAdapter.resetToFull();
                  lspAdapter.resetToFull();
                  aiAdapter.resetToFull();

                  if (expandAi.isExpanded()) expandAi.collapse();
                  if (expandApp.isExpanded()) expandApp.collapse();
                  if (expandEditor.isExpanded()) expandEditor.collapse();
                  if (LspView.isExpanded()) LspView.collapse();
                  isSearchActive = false;
                  ser.removeCallbacks(expandRunnable);
                }
              };
          ser.postDelayed(filterRunnable, 50);
        });

    editorAdapter.setOnItemClickListener(
        position -> {
          if (position == 17) showTabSizeDialog();
          else if (position == 18) showLineHeightDialog();
          else if (position == 19) showCursorBlinkDialog();
          else if (position == 21) showFontDialog();
          else if (position == 22) showTranslateLanguageDialog();
          else if (position == 25) showPowerModeEffectDialog();
        });

    appAdapter.setOnItemClickListener(
        position -> {
          if (position == 0) showBufferSizeDialog();
          else if (position == 1) showThemeDialog();
          else if (position == 2) showAppIconDialog();
          else if (position == 3) showLoadThemeDialog();
          else if (position == 4) showGitHubAccountDialog();
          else if (position == 5) showLanguageDialog();
          else if (position == 9) showAnimationThresholdDialog();
          else if (position == 10) showGridConunt();
        });

    aiAdapter.setOnItemClickListener(
        position -> {
          if (position == 0) {
            showProviderDialog(aiAdapter);
          } else if (position == 1) {
            showApiKeyDialog("claude", aiAdapter);
          } else if (position == 2) {
            showApiKeyDialog("chatgpt", aiAdapter);
          } else if (position == 3) {
            showApiKeyDialog("deepseek", aiAdapter);
          } else if (position == 4) {
            showApiKeyDialog("gemini", aiAdapter);
          } else if (position == 5) {
            showApiKeyDialog("openrouter", aiAdapter);
          }
        });

    if ("githublogin".equals(getIntent().getStringExtra("open_section"))) {
      ThreadUtils.runOnUiThreadDelayed(
          () -> {
            expandApp.expand();
            showGitHubAccountDialog();
          },
          500);
    }
  }

  /**
   * دقیقاً همون مکانیزمِ FileManagerActivity: اگه "نمایش بک‌گراند" از تنظیمات روشن باشه و تم یه عکس
   * بک‌گراند ست کرده باشه، همون عکسِ بلورشده رو پشتِ تولبار و لیست تنظیمات نشون میدیم.
   */
  private void setupBackgroundBlur() {
    View appbar = findViewById(R.id.appbar);
    View settingsContainer = findViewById(R.id.settings_container);
    ImageView backgroundIcon = findViewById(R.id.backgroundIconSetting);
    if (appbar == null || settingsContainer == null || backgroundIcon == null) return;
    setupBackgroundBlur(backgroundIcon, appbar, settingsContainer);
  }

  private List<SettingItem> getAiItems() {
    List<SettingItem> items = new ArrayList<>();
    String currentProvider = aiPrefs.getSelectedProvider();
    String providerDisplay = "";
    switch (currentProvider) {
      case AiConstants.AiProvider.CLAUDE:
        providerDisplay = "Claude";
        break;
      case AiConstants.AiProvider.CHATGPT:
        providerDisplay = "ChatGPT";
        break;
      case AiConstants.AiProvider.DEEPSEEK:
        providerDisplay = "DeepSeek";
        break;
      case AiConstants.AiProvider.GEMINI:
        providerDisplay = "Gemini";
        break;
      case AiConstants.AiProvider.OPENROUTER:
        providerDisplay = "OpenRouter";
        break;
    }
    items.add(
        new SettingItem(
            getString(R.string.ai_provider),
            getString(R.string.ai_provider_desc, providerDisplay),
            false,
            0,
            null));
    items.add(
        new SettingItem(
            getString(R.string.claude_api_key),
            aiPrefs.hasClaudeApiKey() ? "*********" : getString(R.string.not_set),
            false,
            0,
            null));
    items.add(
        new SettingItem(
            getString(R.string.chatgpt_api_key),
            aiPrefs.hasChatGptApiKey() ? "*********" : getString(R.string.not_set),
            false,
            0,
            null));
    items.add(
        new SettingItem(
            getString(R.string.deepseek_api_key),
            aiPrefs.hasDeepSeekApiKey() ? "*********" : getString(R.string.not_set),
            false,
            0,
            null));
    items.add(
        new SettingItem(
            getString(R.string.gemini_api_key),
            aiPrefs.hasGeminiApiKey() ? "*********" : getString(R.string.not_set),
            false,
            0,
            null));
    // OpenRouter
    items.add(
        new SettingItem(
            getString(R.string.pref_openrouter_api_key),
            aiPrefs.hasOpenRouterApiKey() ? "*********" : getString(R.string.not_set),
            false,
            0,
            null));
    return items;
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

    items.add(
        new SettingItem(
            getString(R.string.pref_font),
            getString(R.string.pref_font_desc)
                + "\n"
                + getFontDisplayName(prefs.getCurrentEditorFontName()),
            false,
            0,
            null));

    String currentLangName = TranslateLanguages.getNameByCode(prefs.getTranslateTargetLang());
    items.add(
        new SettingItem(
            getString(R.string.pref_translate_target_lang),
            getString(R.string.pref_translate_target_lang_desc) + "\n" + currentLangName,
            false,
            0,
            null));
    items.add(
        new SettingItem(
            getString(R.string.setting_lineinfopaneltitle),
            getString(R.string.setting_lineinfopanelsubtitle),
            prefs.getShowLineColPanel(),
            0,
            prefs::setShowLineColPanel));
    items.add(
        new SettingItem(
            getString(R.string.pref_power_mode),
            getString(R.string.pref_power_mode_desc),
            prefs.enablePowerMode(),
            0,
            prefs::setPowerMode));
    items.add(
        new SettingItem(
            getString(R.string.pref_power_mode_effect),
            getString(R.string.pref_power_mode_effect_desc) + "\n" + prefs.getPowerModeEffectType(),
            false,
            0,
            null));
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
            getString(R.string.pref_app_icon),
            getString(R.string.pref_app_icon_desc)
                + "\n"
                + getString(AppIconManager.getCurrentIcon(this).labelRes),
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
    items.add(
        new SettingItem(
            getString(R.string.github_account),
            prefs.isGitHubLoggedIn()
                ? getString(R.string.github_account_logged_in, prefs.getGitHubUsername())
                : getString(R.string.github_account_not_logged_in),
            false,
            0,
            null));

    String currentLang = LocaleHelper.LANGUAGE_NAMES[LocaleHelper.getSavedLanguageIndex(this)];
    items.add(
        new SettingItem(
            getString(R.string.pref_language),
            getString(R.string.pref_language_desc) + "\n" + currentLang,
            false,
            0,
            null));
    items.add(
        new SettingItem(
            getString(R.string.pref_show_tab_icon_title),
            getString(R.string.pref_show_tab_icon_summary),
            prefs.getShowIconTab(),
            0,
            prefs::setShowIconTab));
    items.add(
        new SettingItem(
            getString(R.string.grid_title),
            getString(R.string.grid_subtitle),
            prefs.getGridMod(),
            0,
            prefs::setGridMod));
    items.add(
        new SettingItem(
            getString(R.string.pref_show_background),
            getString(R.string.pref_show_background_desc),
            prefs.isShowBackground(),
            0,
            isChecked -> {
              prefs.setShowBackground(isChecked);
              recreate();
            }));
    items.add(
        new SettingItem(
            getString(R.string.pref_animation_battery_threshold),
            getString(R.string.pref_animation_battery_threshold_desc)
                + "\n"
                + String.format(
                    getString(R.string.current_value), prefs.getAnimationBatteryThreshold() + "%"),
            false,
            0,
            null));
    items.add(
        new SettingItem(
            getString(R.string.pref_grid_title),
            getString(R.string.pref_grid_subtitle),
            false,
            0,
            null));
    items.add(
        new SettingItem(
            getString(R.string.pref_show_hidden_files),
            getString(R.string.pref_show_hidden_files_desc),
            prefs.isShowHiddenFiles(),
            0,
            prefs::setShowHiddenFiles));
    items.add(
        new SettingItem(
            getString(R.string.terfrtitle),
            getString(R.string.terfrsubtitle),
            prefs.isTerminalFragment(),
            0,
            prefs::setTerminalFragment));
    items.add(
        new SettingItem(
            getString(R.string.all_blur),
            getString(R.string.all_blur_subtitle),
            prefs.isBlurMod(),
            0,
            prefs::setBlurMod));

    return items;
  }

  private List<SettingItem> getLspSetting() {
    List<SettingItem> items = new ArrayList<>();
    items.add(
        new SettingItem(
            getString(R.string.lsp_inlaytitle),
            getString(R.string.lsp_inlaysubtitle),
            prefs.isInlayHint(),
            0,
            prefs::setInlayHint));
    items.add(
        new SettingItem(
            getString(R.string.lsp_hovertitle),
            getString(R.string.lsp_hoversubtitle),
            prefs.isHover(),
            0,
            prefs::setHover));
    items.add(
        new SettingItem(
            getString(R.string.lsp_signaturehelptitle),
            getString(R.string.lsp_signaturehelpsubtitle),
            prefs.isSignatureHelp(),
            0,
            prefs::setSignatureHelp));
    items.add(
        new SettingItem(
            getString(R.string.lsp_diagnosticstitle),
            getString(R.string.lsp_diagnosticssubtitle),
            prefs.isDiagnostics(),
            0,
            prefs::setDiagnostics));
    return items;
  }

  private void showLanguageDialog() {
    int checkedIndex = LocaleHelper.getSavedLanguageIndex(this);
    new MaterialAlertDialogBuilder(this)
        .setTitle(R.string.pref_language)
        .setSingleChoiceItems(
            LocaleHelper.LANGUAGE_NAMES,
            checkedIndex,
            (dialog, which) -> {
              String selectedCode = LocaleHelper.LANGUAGE_CODES[which];
              LocaleHelper.saveLanguage(this, selectedCode);
              dialog.dismiss();
              LocaleListCompat localeList =
                  "default".equals(selectedCode)
                      ? LocaleListCompat.getEmptyLocaleList()
                      : LocaleListCompat.forLanguageTags(selectedCode);
              AppCompatDelegate.setApplicationLocales(localeList);
            })
        .setNegativeButton(R.string.cancel, null)
        .show();
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

  private void showGridConunt() {
    int id = prefs.getGridSpanCount();
    int[] values = {2, 4, 6, 8};
    String[] label = {
      getString(R.string.pref_grid, "2"),
      getString(R.string.pref_grid, "4"),
      getString(R.string.pref_grid, "6"),
      getString(R.string.pref_grid, "8")
    };

    int checked = 0;
    for (int i = 0; i < values.length; i++) {
      if (values[i] == id) {
        checked = i;
        break;
      }
    }

    new MaterialAlertDialogBuilder(this)
        .setTitle(R.string.pref_grid_title)
        .setSingleChoiceItems(
            label,
            checked,
            (d, which) -> {
              prefs.setGridSpanCount(values[which]);
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
        .setNegativeButton(R.string.cancel, null)
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

  private void showTranslateLanguageDialog() {
    String[] names = TranslateLanguages.NAMES;
    String[] codes = TranslateLanguages.CODES;
    String saved = prefs.getTranslateTargetLang();
    int checked = 0;
    for (int i = 0; i < codes.length; i++) {
      if (codes[i].equals(saved)) {
        checked = i;
        break;
      }
    }
    new MaterialAlertDialogBuilder(this)
        .setTitle(R.string.pref_translate_target_lang)
        .setSingleChoiceItems(
            names,
            checked,
            (dialog, which) -> {
              prefs.setTranslateTargetLang(codes[which]);
              dialog.dismiss();
              SettingItem item = editorAdapter.getItemAtPosition(21);
              if (item != null) {
                item.setDescription(
                    getString(R.string.pref_translate_target_lang_desc) + "\n" + names[which]);
                editorAdapter.notifyItemChanged(21);
              }
            })
        .setNegativeButton(R.string.cancel, null)
        .show();
  }

  private void showBufferSizeDialog() {
    int current = prefs.getCurrentBufferSize() / 1024;
    String[] sizes = {"2", "4", "6", "8", "10", "12", "16", "20"};
    String[] labels = {
      getString(R.string.buffer_size_2), getString(R.string.buffer_size_4),
      getString(R.string.buffer_size_6), getString(R.string.buffer_size_8),
      getString(R.string.buffer_size_10), getString(R.string.buffer_size_12),
      getString(R.string.buffer_size_16), getString(R.string.buffer_size_20)
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
    new ThemeChooserDialogBuilder(this)
        .setPositiveButton(
            R.string.ok,
            (position, theme) -> {
              themeEngine.setStaticTheme(theme);
              recreateAllActivities();
            })
        .setNegativeButton(R.string.cancel)
        .setNeutralButton(
            "Default",
            (position, theme) -> {
              themeEngine.resetTheme();
              recreateAllActivities();
            })
        .create()
        .show();
  }

  private void showAppIconDialog() {
    new AppIconChooserDialogBuilder(this)
        .setTitle(R.string.pref_app_icon)
        .setPositiveButton(
            R.string.ok,
            icon -> {
              AppIconManager.applyIcon(this, icon);
              appAdapter
                  .getItemAtPosition(2)
                  .setDescription(
                      getString(R.string.pref_app_icon_desc) + "\n" + getString(icon.labelRes));
              appAdapter.notifyItemChanged(2);
            })
        .setNegativeButton(R.string.cancel)
        .create()
        .show();
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
                String json = new String(FileUtil.readBytesCompat(file), StandardCharsets.UTF_8);
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
            R.string.cancel, (dialog, which) -> new ThemeManager(this).resetToDefault())
        .show();
  }

  private void showGitHubAccountDialog() {
    if (prefs.isGitHubLoggedIn()) {
      new MaterialAlertDialogBuilder(this)
          .setTitle(getString(R.string.github_account))
          .setMessage(getString(R.string.github_account_logged_in, prefs.getGitHubUsername()))
          .setPositiveButton(getString(R.string.ok), null)
          .setNegativeButton(
              getString(R.string.github_logout),
              (d, w) -> {
                new GitHubClient(this).logout();
                Toast.makeText(this, getString(R.string.github_logout_success), Toast.LENGTH_SHORT)
                    .show();
                appAdapter.updateItem(
                    3,
                    new SettingItem(
                        getString(R.string.github_account),
                        getString(R.string.github_account_not_logged_in),
                        false,
                        0,
                        null));
              })
          .show();
    } else {
      TextInputDialogFragment.newInstance(
              getString(R.string.github_login_title), getString(R.string.github_login_hint), "")
          .setCallback(
              token ->
                  new GitHubClient(this)
                      .login(
                          token,
                          new GitHubClient.GitHubLoginCallback() {
                            @Override
                            public void onSuccess(String name, String username, String avatarUrl) {
                              runOnUiThread(
                                  () -> {
                                    Toast.makeText(
                                            SettingActivity.this,
                                            getString(R.string.github_welcome, name),
                                            Toast.LENGTH_SHORT)
                                        .show();
                                    appAdapter.updateItem(
                                        3,
                                        new SettingItem(
                                            getString(R.string.github_account),
                                            getString(R.string.github_account_logged_in, username),
                                            false,
                                            0,
                                            null));
                                  });
                            }

                            @Override
                            public void onFailure(String errorMessage) {
                              runOnUiThread(
                                  () ->
                                      Toast.makeText(
                                              SettingActivity.this,
                                              errorMessage,
                                              Toast.LENGTH_SHORT)
                                          .show());
                            }
                          }))
          .show(getSupportFragmentManager(), "github_token");
    }
  }

  private void showProviderDialog(SettingsAdapter adapter) {
    String[] providers = {"Claude", "ChatGPT", "DeepSeek", "Gemini", "OpenRouter"};
    String[] values = {
      AiConstants.AiProvider.CLAUDE,
      AiConstants.AiProvider.CHATGPT,
      AiConstants.AiProvider.DEEPSEEK,
      AiConstants.AiProvider.GEMINI,
      AiConstants.AiProvider.OPENROUTER
    };
    int checked = 0;
    String current = aiPrefs.getSelectedProvider();
    for (int i = 0; i < values.length; i++) {
      if (values[i].equals(current)) {
        checked = i;
        break;
      }
    }

    new MaterialAlertDialogBuilder(this)
        .setTitle(R.string.ai_provider)
        .setSingleChoiceItems(
            providers,
            checked,
            (dialog, which) -> {
              aiPrefs.setSelectedProvider(values[which]);
              dialog.dismiss();
              String newProvider = providers[which];
              SettingItem item = adapter.getItemAtPosition(0);
              if (item != null) {
                item.setDescription(getString(R.string.ai_provider_desc, newProvider));
                adapter.notifyItemChanged(0);
              }
            })
        .setNegativeButton(R.string.cancel, null)
        .show();
  }

  int position = 0;

  private void showApiKeyDialog(String provider, SettingsAdapter adapter) {
    String title = "";
    String currentKey = "";

    switch (provider) {
      case "claude":
        title = getString(R.string.claude_api_key);
        currentKey = aiPrefs.getClaudeApiKey();
        position = 1;
        break;
      case "chatgpt":
        title = getString(R.string.chatgpt_api_key);
        currentKey = aiPrefs.getChatGptApiKey();
        position = 2;
        break;
      case "deepseek":
        title = getString(R.string.deepseek_api_key);
        currentKey = aiPrefs.getDeepSeekApiKey();
        position = 3;
        break;
      case "gemini":
        title = getString(R.string.gemini_api_key);
        currentKey = aiPrefs.getGeminiApiKey();
        position = 4;
        break;
      case "openrouter":
        title = getString(R.string.pref_openrouter_api_key);
        currentKey = aiPrefs.getOpenRouterApiKey();
        position = 5;
        break;
    }

    TextInputDialogFragment.newInstance(
            title, "Enter API key", currentKey.isEmpty() ? null : currentKey)
        .setCallback(
            text -> {
              if (text.isEmpty()) {
                switch (provider) {
                  case "claude":
                    aiPrefs.setClaudeApiKey("");
                    break;
                  case "chatgpt":
                    aiPrefs.setChatGptApiKey("");
                    break;
                  case "deepseek":
                    aiPrefs.setDeepSeekApiKey("");
                    break;
                  case "gemini":
                    aiPrefs.setGeminiApiKey("");
                    break;
                  case "openrouter":
                    aiPrefs.setOpenRouterApiKey("");
                    break;
                }
                Toast.makeText(this, R.string.key_cleared, Toast.LENGTH_SHORT).show();
                SettingItem item = adapter.getItemAtPosition(position);
                if (item != null) {
                  item.setDescription(getString(R.string.not_set));
                  adapter.notifyItemChanged(position);
                }
              } else {
                switch (provider) {
                  case "claude":
                    aiPrefs.setClaudeApiKey(text);
                    break;
                  case "chatgpt":
                    aiPrefs.setChatGptApiKey(text);
                    break;
                  case "deepseek":
                    aiPrefs.setDeepSeekApiKey(text);
                    break;
                  case "gemini":
                    aiPrefs.setGeminiApiKey(text);
                    break;
                  case "openrouter":
                    aiPrefs.setOpenRouterApiKey(text);
                    break;
                }
                Toast.makeText(this, R.string.key_saved, Toast.LENGTH_SHORT).show();
                SettingItem item = adapter.getItemAtPosition(position);
                if (item != null) {
                  item.setDescription("*********");
                  adapter.notifyItemChanged(position);
                }
              }
            })
        .show(getSupportFragmentManager(), "api_key_dialog");
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home) finish();
    return super.onOptionsItemSelected(item);
  }

  private void showFontDialog() {
    final String[] fontKeys = {
      "jetbrains_mono_regular",
      "inconsolata_regular",
      "sourcecodepro_regular",
      "firacode_regular",
      "notosans_regular",
      "notosans_italic"
    };
    final String[] fontNames = {
      getString(R.string.font_jetbrains_mono),
      getString(R.string.font_inconsolata),
      getString(R.string.font_source_code_pro),
      getString(R.string.font_fira_code),
      getString(R.string.font_noto_sans),
      getString(R.string.font_noto_sans_italic)
    };
    String currentFont = prefs.getCurrentEditorFontName();
    int checked = 0;
    for (int i = 0; i < fontKeys.length; i++) {
      if (fontKeys[i].equals(currentFont)) {
        checked = i;
        break;
      }
    }
    new MaterialAlertDialogBuilder(this)
        .setTitle(R.string.pref_font)
        .setSingleChoiceItems(
            fontNames,
            checked,
            (dialog, which) -> {
              prefs.setCurrentEditorFont(fontKeys[which]);
              dialog.dismiss();
              SettingItem item = editorAdapter.getItemAtPosition(21);
              if (item != null) {
                item.setDescription(getString(R.string.pref_font_desc) + "\n" + fontNames[which]);
                editorAdapter.notifyItemChanged(21);
              }
            })
        .setNegativeButton(R.string.cancel, null)
        .show();
  }

  private String getFontDisplayName(String fontKey) {
    switch (fontKey) {
      case "inconsolata_regular":
        return getString(R.string.font_inconsolata);
      case "sourcecodepro_regular":
        return getString(R.string.font_source_code_pro);
      case "firacode_regular":
        return getString(R.string.font_fira_code);
      case "notosans_regular":
        return getString(R.string.font_noto_sans);
      case "notosans_italic":
        return "Noto Sans Italic";
      default:
        return getString(R.string.font_jetbrains_mono);
    }
  }

  private void showAnimationThresholdDialog() {
    var slidersheet = new SliderSheet(this);

    int current = prefs.getAnimationBatteryThreshold();
    var slider = slidersheet.getSlider();
    slider.setValueFrom(0f);
    slider.setValueTo(100f);
    slider.setStepSize(5f);
    slider.setValue(current);
    slidersheet.setLable(current + "%");
    slider.addOnChangeListener((s, value, fromUser) -> slidersheet.setLable((int) value + "%"));
    slidersheet.setButtonOk(
        v -> {
          prefs.setAnimationBatteryThreshold((int) slider.getValue());
          appAdapter.updateItem(
              8,
              new SettingItem(
                  getString(R.string.pref_animation_battery_threshold),
                  getString(R.string.pref_animation_battery_threshold_desc)
                      + "\n"
                      + String.format(
                          getString(R.string.current_value),
                          prefs.getAnimationBatteryThreshold() + "%"),
                  false,
                  0,
                  null));
          slidersheet.dismiss();
        },
        R.string.ok);
    slidersheet.setButtonNo(
        v -> {
          slidersheet.dismiss();
        },
        R.string.no);
    slidersheet.show();
  }

  private void showPowerModeEffectDialog() {
    String[] names = PowerModeEffectManager.EffectType.getAllName();
    String current = prefs.getPowerModeEffectType();
    int checked = 0;
    for (int i = 0; i < names.length; i++) {
      if (names[i].equalsIgnoreCase(current)) {
        checked = i;
        break;
      }
    }
    new MaterialAlertDialogBuilder(this)
        .setTitle(R.string.pref_power_mode_effect)
        .setSingleChoiceItems(
            names,
            checked,
            (dialog, which) -> {
              prefs.setPowerModeEffectType(names[which]);
              dialog.dismiss();
              SettingItem item = editorAdapter.getItemAtPosition(25);
              if (item != null) {
                item.setDescription(
                    getString(R.string.pref_power_mode_effect_desc) + "\n" + names[which]);
                editorAdapter.notifyItemChanged(25);
              }
            })
        .setNegativeButton(R.string.cancel, null)
        .show();
  }
}
