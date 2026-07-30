package ir.hanzodev1375.ghostide.codeeditors.setting;

import android.content.SharedPreferences;
import android.util.Log;
import androidx.annotation.NonNull;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.HashSet;
import android.preference.PreferenceManager;
import android.content.Context;
import ir.hanzodev1375.ghostide.codeeditors.R;

public class PreferencesUtils {

  private final String TAG = PreferencesUtils.class.getSimpleName();
  private Context c;

  public PreferencesUtils(Context c) {
    this.c = c;
  }

  public SharedPreferences getDefaultPreferences() {
    return PreferenceManager.getDefaultSharedPreferences(c);
  }

  // ========== Getter ==========

  public boolean autoSaveFiles() {
    return getDefaultPreferences()
        .getBoolean(Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_AUTO_SAVE, false);
  }

  public boolean canCloseRelativeToFirstDepth() {
    var depth =
        getDefaultPreferences()
            .getString(
                Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_RELATIVE_CLOSE_DEPTH, "First");
    if (depth.equalsIgnoreCase("All")) return false;
    return true;
  }

  public boolean isTerminalFragment() {
    return getDefaultPreferences().getBoolean(Constants.SharedPreferenceKeys.KRY_TERM, false);
  }

  public void setTerminalFragment(boolean b) {
    getDefaultPreferences().edit().putBoolean(Constants.SharedPreferenceKeys.KRY_TERM, b).apply();
  }

  public void setBlurMod(boolean mod) {
    getDefaultPreferences().edit().putBoolean(Constants.SharedPreferenceKeys.KEY_BLUR, mod).apply();
  }

  public boolean isBlurMod() {
    return getDefaultPreferences().getBoolean(Constants.SharedPreferenceKeys.KEY_BLUR, true);
  }

  public void setGitLocalUserName(String name) {
    getDefaultPreferences()
        .edit()
        .putString(Constants.SharedPreferenceKeys.KEY_GIT_LOCAL_USER_NAME, name)
        .apply();
  }

  public String getGitLocalUserName() {
    return getDefaultPreferences()
        .getString(Constants.SharedPreferenceKeys.KEY_GIT_LOCAL_USER_NAME, "");
  }

  public void setGitLocalUserEmail(String email) {
    getDefaultPreferences()
        .edit()
        .putString(Constants.SharedPreferenceKeys.KEY_GIT_LOCAL_USER_EMAIL, email)
        .apply();
  }

  public String getGitLocalUserEmail() {
    return getDefaultPreferences()
        .getString(Constants.SharedPreferenceKeys.KEY_GIT_LOCAL_USER_EMAIL, "");
  }

  public boolean hasGitLocalUserConfig() {
    return !getGitLocalUserEmail().isEmpty() && !getGitLocalUserEmail().isEmpty();
  }

  public void clearGitLocalUserConfig() {
    getDefaultPreferences()
        .edit()
        .remove(Constants.SharedPreferenceKeys.KEY_GIT_LOCAL_USER_NAME)
        .remove(Constants.SharedPreferenceKeys.KEY_GIT_LOCAL_USER_EMAIL)
        .apply();
  }

  public int getCursorBlinkPeriod() {
    return getDefaultPreferences()
        .getInt(Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_CURSOR_BLINK_PERIOD, 500);
  }

  public String getCurrentEditorFontName() {
    return getDefaultPreferences()
        .getString(Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_FONT, "jetbrains_mono_regular");
  }

  public boolean enableAutoComplete() {
    return getDefaultPreferences()
        .getBoolean(Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_AUTO_COMPLETE, false);
  }

  public boolean enableAutoCompleteWindowAnimation() {
    return getDefaultPreferences()
        .getBoolean(Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_ANIMATE_AUTO_COMP_WINDOW, false);
  }

  public boolean enableBracketAutoClosing() {
    return getDefaultPreferences()
        .getBoolean(Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_AUTO_CLOSE_BRACKET, false);
  }

  public boolean enableBracketHighlight() {
    return getDefaultPreferences()
        .getBoolean(Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_HIGHLIGHT_BRACKET, true);
  }

  public boolean enableDeleteEmptyLine() {
    return getDefaultPreferences()
        .getBoolean(Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_DELETE_EMPTY_LINE, false);
  }

  public boolean enableDeleteTab() {
    return getDefaultPreferences()
        .getBoolean(Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_DELETE_TAB, false);
  }

  public boolean enableHardWareAcceleration() {
    return getDefaultPreferences()
        .getBoolean(Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_HARDWARE_ACCELERATION, false);
  }

  public String getTranslateTargetLang() {
    return getDefaultPreferences()
        .getString(Constants.SharedPreferenceKeys.KEY_TRANSLATE_TARGET_LANG, "en");
  }

  public void setTranslateTargetLang(String langCode) {
    getDefaultPreferences()
        .edit()
        .putString(Constants.SharedPreferenceKeys.KEY_TRANSLATE_TARGET_LANG, langCode)
        .apply();
  }

  public boolean enableLineNumbers() {
    return getDefaultPreferences()
        .getBoolean(Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_LINE_NUMBERS, true);
  }

  public boolean getShowIconTab() {
    return getDefaultPreferences()
        .getBoolean(Constants.SharedPreferenceKeys.KEY_SHOWTAB_ICON, false);
  }

  public void setShowIconTab(boolean is) {
    getDefaultPreferences()
        .edit()
        .putBoolean(Constants.SharedPreferenceKeys.KEY_SHOWTAB_ICON, is)
        .apply();
  }

  public boolean enableMagnifier() {
    return getDefaultPreferences()
        .getBoolean(Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_MAGNIFIER, true);
  }

  public boolean enableScrollBar() {
    return getDefaultPreferences()
        .getBoolean(Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_SCROLL_BAR, false);
  }

  public boolean enableStickyScroll() {
    return getDefaultPreferences()
        .getBoolean(Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_STICKY_SCROLL, false);
  }

  public boolean enableMiniMap() {
    return getDefaultPreferences()
        .getBoolean(Constants.SharedPreferenceKeys.KEY_CODE_EDITORMINIMAP, false);
  }

  public boolean flagEmptyLine() {
    Set<String> values =
        getDefaultPreferences()
            .getStringSet(
                Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_NP_PAINT_FLAGS, new HashSet<>());
    return values.contains("4");
  }

  public boolean flagInner() {
    Set<String> values =
        getDefaultPreferences()
            .getStringSet(
                Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_NP_PAINT_FLAGS, new HashSet<>());
    return values.contains("1");
  }

  public boolean flagLeading() {
    Set<String> values =
        getDefaultPreferences()
            .getStringSet(
                Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_NP_PAINT_FLAGS, new HashSet<>());
    return values.contains("2");
  }

  public boolean flagLineBreaks() {
    Set<String> values =
        getDefaultPreferences()
            .getStringSet(
                Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_NP_PAINT_FLAGS, new HashSet<>());
    return values.contains("5");
  }

  public boolean flagTrailing() {
    Set<String> values =
        getDefaultPreferences()
            .getStringSet(
                Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_NP_PAINT_FLAGS, new HashSet<>());
    return values.contains("3");
  }

  public int getCodeEditorTabSize() {
    return getCodeEditorTabSize(2);
  }

  public int getCodeEditorTabSize(int size) {
    return getDefaultPreferences()
        .getInt(Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_TAB_SIZE, size);
  }

  public int getCurrentBufferSize() {
    var selected =
        getDefaultPreferences().getString(Constants.SharedPreferenceKeys.KEY_BUFFER_SIZE, "5");
    return Integer.parseInt(selected) * 1024;
  }

  public float getCurrentEditorLineHeight() {
    var selected =
        getDefaultPreferences()
            .getString(Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_LINE_HEIGHT, "2");
    return switch (selected) {
      case "1" -> 1;
      case "3" -> 3;
      case "4" -> 4;
      default -> 2;
    };
  }

  public int getCurrentEditorFont() {
    var selectedFont =
        getDefaultPreferences()
            .getString(
                Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_FONT, "jetbrains_mono_regular");
    return getEditorFont(selectedFont);
  }

  public void setCurrentEditorFont(String fontName) {
    getDefaultPreferences()
        .edit()
        .putString(Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_FONT, fontName)
        .apply();
  }

  private int getEditorFont(@NonNull String selectedFont) {
    switch (selectedFont) {
      case "inconsolata_regular":
        return R.font.inconsolata_regular;
      case "sourcecodepro_regular":
        return R.font.sourcecodepro_regular;
      case "firacode_regular":
        return R.font.firacode_regular;
      case "notosans_regular":
        return R.font.notosans_regular;
      case "notosans_italic":
        return R.font.notosans_italic;
      case "jetbrains_mono_regular":
      default:
        return R.font.jetbrains_mono_regular;
    }
  }

  public int getCursorBlinkPeriod(int defaultBlinkPeriod) {
    return getDefaultPreferences()
        .getInt(
            Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_CURSOR_BLINK_PERIOD, defaultBlinkPeriod);
  }

  public boolean pinLineNumber() {
    return getDefaultPreferences()
        .getBoolean(Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_PIN_LINE_NUM, false);
  }

  public boolean useFontLigatures() {
    return getDefaultPreferences()
        .getBoolean(Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_FONT_LIAGTURES, false);
  }

  public boolean useICULibrary() {
    return getDefaultPreferences()
        .getBoolean(Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_ICU, false);
  }

  public boolean useTabIndentation() {
    return getDefaultPreferences()
        .getBoolean(Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_TAB_INDENT, false);
  }

  public boolean useWordWrap() {
    return getDefaultPreferences()
        .getBoolean(Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_WORD_WRAP, false);
  }

  // ========== Setter ==========

  public void setAutoSave(boolean enabled) {
    getDefaultPreferences()
        .edit()
        .putBoolean(Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_AUTO_SAVE, enabled)
        .apply();
  }

  public void setRelativeCloseDepth(String depth) {
    getDefaultPreferences()
        .edit()
        .putString(Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_RELATIVE_CLOSE_DEPTH, depth)
        .apply();
  }

  public void setCursorBlinkPeriod(int periodMs) {
    getDefaultPreferences()
        .edit()
        .putInt(Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_CURSOR_BLINK_PERIOD, periodMs)
        .apply();
  }

  public void setAutoComplete(boolean enabled) {
    getDefaultPreferences()
        .edit()
        .putBoolean(Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_AUTO_COMPLETE, enabled)
        .apply();
  }

  public void setMiniMap(boolean enabled) {
    getDefaultPreferences()
        .edit()
        .putBoolean(Constants.SharedPreferenceKeys.KEY_CODE_EDITORMINIMAP, enabled)
        .apply();
  }

  public void setAutoCompleteWindowAnimation(boolean enabled) {
    getDefaultPreferences()
        .edit()
        .putBoolean(
            Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_ANIMATE_AUTO_COMP_WINDOW, enabled)
        .apply();
  }

  public void setBracketAutoClosing(boolean enabled) {
    getDefaultPreferences()
        .edit()
        .putBoolean(Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_AUTO_CLOSE_BRACKET, enabled)
        .apply();
  }

  public void setBracketHighlight(boolean enabled) {
    getDefaultPreferences()
        .edit()
        .putBoolean(Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_HIGHLIGHT_BRACKET, enabled)
        .apply();
  }

  public void setDeleteEmptyLine(boolean enabled) {
    getDefaultPreferences()
        .edit()
        .putBoolean(Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_DELETE_EMPTY_LINE, enabled)
        .apply();
  }

  public void setDeleteTab(boolean enabled) {
    getDefaultPreferences()
        .edit()
        .putBoolean(Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_DELETE_TAB, enabled)
        .apply();
  }

  public void setHardwareAcceleration(boolean enabled) {
    getDefaultPreferences()
        .edit()
        .putBoolean(Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_HARDWARE_ACCELERATION, enabled)
        .apply();
  }

  public void setLineNumbers(boolean enabled) {
    getDefaultPreferences()
        .edit()
        .putBoolean(Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_LINE_NUMBERS, enabled)
        .apply();
  }

  public void setMagnifier(boolean enabled) {
    getDefaultPreferences()
        .edit()
        .putBoolean(Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_MAGNIFIER, enabled)
        .apply();
  }

  public void setScrollBar(boolean enabled) {
    getDefaultPreferences()
        .edit()
        .putBoolean(Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_SCROLL_BAR, enabled)
        .apply();
  }

  public void setStickyScroll(boolean enabled) {
    getDefaultPreferences()
        .edit()
        .putBoolean(Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_STICKY_SCROLL, enabled)
        .apply();
  }

  public void setNonPrintableFlags(Set<String> flags) {
    getDefaultPreferences()
        .edit()
        .putStringSet(Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_NP_PAINT_FLAGS, flags)
        .apply();
  }

  public void setCodeEditorTabSize(int tabSize) {
    getDefaultPreferences()
        .edit()
        .putInt(Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_TAB_SIZE, tabSize)
        .apply();
  }

  public void setBufferSize(String kb) {
    getDefaultPreferences()
        .edit()
        .putString(Constants.SharedPreferenceKeys.KEY_BUFFER_SIZE, kb)
        .apply();
  }

  public void setLineHeight(String lineHeightEntry) {
    getDefaultPreferences()
        .edit()
        .putString(Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_LINE_HEIGHT, lineHeightEntry)
        .apply();
  }

  public void setPinLineNumber(boolean pin) {
    getDefaultPreferences()
        .edit()
        .putBoolean(Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_PIN_LINE_NUM, pin)
        .apply();
  }

  public void setFontLigatures(boolean enabled) {
    getDefaultPreferences()
        .edit()
        .putBoolean(Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_FONT_LIAGTURES, enabled)
        .apply();
  }

  public void setICULibrary(boolean enabled) {
    getDefaultPreferences()
        .edit()
        .putBoolean(Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_ICU, enabled)
        .apply();
  }

  public void setTabIndentation(boolean useTabs) {
    getDefaultPreferences()
        .edit()
        .putBoolean(Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_TAB_INDENT, useTabs)
        .apply();
  }

  public void setWordWrap(boolean enabled) {
    getDefaultPreferences()
        .edit()
        .putBoolean(Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_WORD_WRAP, enabled)
        .apply();
  }

  public void clearPreference(@NonNull SharedPreferences pref, String key) {
    Map<String, ?> cues = pref.getAll();
    cues.forEach(
        (k, v) -> {
          if (Objects.equals(key, k)) {
            emptyEditorValue(pref.edit(), k, v);
          }
        });
  }

  private void emptyEditorValue(
      SharedPreferences.Editor editor, @NonNull String key, @NonNull Object value) {
    try {
      if (value instanceof Boolean) editor.putBoolean(key, false);
      else if (value instanceof Float) editor.putFloat(key, 0.0f);
      else if (value instanceof String) editor.putString(key, "");
      else if (value instanceof Integer) editor.putInt(key, 0);
      else if (value instanceof Long) editor.putLong(key, 0L);
      else if (value instanceof Set<?>) editor.putStringSet(key, new HashSet<>());
      Log.d(TAG, String.format("Key-Value: %s -> %s", key, value));
      editor.apply();
    } catch (Exception e) {
      Log.e(TAG, "Failed to clear preference value", e);
    }
  }

  public int getAppTheme() {
    return getDefaultPreferences().getInt(Constants.SharedPreferenceKeys.KEY_APP_THEME, 0);
  }

  public void setAppTheme(int themeIndex) {
    getDefaultPreferences()
        .edit()
        .putInt(Constants.SharedPreferenceKeys.KEY_APP_THEME, themeIndex)
        .apply();
  }

  public String getAppThemeFile() {
    return getDefaultPreferences().getString(Constants.SharedPreferenceKeys.KEY_APP_THEME_FILE, "");
  }

  public void setAppThemeFile(String filePath) {
    getDefaultPreferences()
        .edit()
        .putString(Constants.SharedPreferenceKeys.KEY_APP_THEME_FILE, filePath)
        .apply();
  }

  // ========== GitHub Account ==========

  public String getGitHubToken() {
    return getDefaultPreferences().getString(Constants.SharedPreferenceKeys.KEY_GITHUB_TOKEN, "");
  }

  public void setGitHubToken(String token) {
    getDefaultPreferences()
        .edit()
        .putString(Constants.SharedPreferenceKeys.KEY_GITHUB_TOKEN, token)
        .apply();
  }

  public String getGitHubUsername() {
    return getDefaultPreferences()
        .getString(Constants.SharedPreferenceKeys.KEY_GITHUB_USERNAME, "");
  }

  public void setGitHubUsername(String username) {
    getDefaultPreferences()
        .edit()
        .putString(Constants.SharedPreferenceKeys.KEY_GITHUB_USERNAME, username)
        .apply();
  }

  public String getGitHubName() {
    return getDefaultPreferences().getString(Constants.SharedPreferenceKeys.KEY_GITHUB_NAME, "");
  }

  public void setGitHubName(String name) {
    getDefaultPreferences()
        .edit()
        .putString(Constants.SharedPreferenceKeys.KEY_GITHUB_NAME, name)
        .apply();
  }

  public String getGitHubAvatarUrl() {
    return getDefaultPreferences()
        .getString(Constants.SharedPreferenceKeys.KEY_GITHUB_AVATAR_URL, "");
  }

  public void setGitHubAvatarUrl(String avatarUrl) {
    getDefaultPreferences()
        .edit()
        .putString(Constants.SharedPreferenceKeys.KEY_GITHUB_AVATAR_URL, avatarUrl)
        .apply();
  }

  public boolean isGitHubLoggedIn() {
    return !getGitHubToken().isEmpty();
  }

  public String getGitCommitName() {
    return getDefaultPreferences().getString(Constants.SharedPreferenceKeys.KEY_DEVNAMEGIT, "");
  }

  public void setGitCommitName(String data) {
    getDefaultPreferences()
        .edit()
        .putString(Constants.SharedPreferenceKeys.KEY_DEVNAMEGIT, data)
        .apply();
  }

  public String getGitCommitEmail() {
    return getDefaultPreferences().getString(Constants.SharedPreferenceKeys.KEY_EMAILGIT, "");
  }

  public void setGitCommitEmail(String data) {
    getDefaultPreferences()
        .edit()
        .putString(Constants.SharedPreferenceKeys.KEY_EMAILGIT, data)
        .apply();
  }

  public boolean getShowLineColPanel() {
    return getDefaultPreferences().getBoolean(Constants.SharedPreferenceKeys.KEY_CODE_SHOWLC, true);
  }

  public void setShowLineColPanel(boolean mod) {
    getDefaultPreferences()
        .edit()
        .putBoolean(Constants.SharedPreferenceKeys.KEY_CODE_SHOWLC, mod)
        .apply();
  }

  public void setRemovedDataCommit() {
    getDefaultPreferences()
        .edit()
        .remove(Constants.SharedPreferenceKeys.KEY_DEVNAMEGIT)
        .remove(Constants.SharedPreferenceKeys.KEY_EMAILGIT)
        .apply();
  }

  public void clearGitHubAccount() {
    getDefaultPreferences()
        .edit()
        .remove(Constants.SharedPreferenceKeys.KEY_GITHUB_TOKEN)
        .remove(Constants.SharedPreferenceKeys.KEY_GITHUB_USERNAME)
        .remove(Constants.SharedPreferenceKeys.KEY_GITHUB_NAME)
        .remove(Constants.SharedPreferenceKeys.KEY_GITHUB_AVATAR_URL)
        .apply();
  }

  public boolean getGridMod() {
    return getDefaultPreferences().getBoolean(Constants.SharedPreferenceKeys.KEY_GRIDMOD, false);
  }

  public void setGridMod(boolean mod) {
    getDefaultPreferences()
        .edit()
        .putBoolean(Constants.SharedPreferenceKeys.KEY_GRIDMOD, mod)
        .apply();
  }

  public boolean isShowBackground() {
    return getDefaultPreferences().getBoolean(Constants.SharedPreferenceKeys.KEY_FILRMB, false);
  }

  public void setShowBackground(boolean mod) {
    getDefaultPreferences()
        .edit()
        .putBoolean(Constants.SharedPreferenceKeys.KEY_FILRMB, mod)
        .apply();
  }

  public int getAnimationBatteryThreshold() {
    return getDefaultPreferences()
        .getInt(
            Constants.SharedPreferenceKeys.KEY_ANIMATION_BATTERY_THRESHOLD,
            20); // باطری اگر 20 درصد بود خاموشش کنن عه
  }

  public void setAnimationBatteryThreshold(int percent) {
    getDefaultPreferences()
        .edit()
        .putInt(Constants.SharedPreferenceKeys.KEY_ANIMATION_BATTERY_THRESHOLD, percent)
        .apply();
  }

  public int getGridSpanCount() {
    return getDefaultPreferences().getInt(Constants.SharedPreferenceKeys.KEY_GRIDCOUNT, 2);
  }

  public void setGridSpanCount(int count) {
    getDefaultPreferences()
        .edit()
        .putInt(Constants.SharedPreferenceKeys.KEY_GRIDCOUNT, count)
        .apply();
  }

  public boolean isShowHiddenFiles() {
    return getDefaultPreferences()
        .getBoolean(Constants.SharedPreferenceKeys.KEY_SHOW_HIDDEN_FILES, false);
  }

  public void setShowHiddenFiles(boolean show) {
    getDefaultPreferences()
        .edit()
        .putBoolean(Constants.SharedPreferenceKeys.KEY_SHOW_HIDDEN_FILES, show)
        .apply();
  }

  public boolean enablePowerMode() {
    return getDefaultPreferences()
        .getBoolean(Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_POWER_MODE, false);
  }

  public void setPowerMode(boolean enabled) {
    getDefaultPreferences()
        .edit()
        .putBoolean(Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_POWER_MODE, enabled)
        .apply();
  }

  public String getPowerModeEffectType() {
    return getDefaultPreferences()
        .getString(Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_POWER_MODE_EFFECT, "none");
  }

  public void setPowerModeEffectType(String effectType) {
    getDefaultPreferences()
        .edit()
        .putString(Constants.SharedPreferenceKeys.KEY_CODE_EDITOR_POWER_MODE_EFFECT, effectType)
        .apply();
  }
}
