package ir.hanzodev1375.ghostide.codeeditors.setting;

public class Constants {

  public static class SharedPreferenceKeys {
    // Editor Preferences
    public static final String KEY_CODE_EDITOR_FONT_LIAGTURES = "pref_code_editor_font_liagtures";
    public static final String KEY_CODE_EDITOR_WORD_WRAP = "pref_code_editor_word_wrap";
    public static final String KEY_CODE_EDITOR_TAB_INDENT = "pref_code_editor_tab_indent";
    public static final String KEY_CODE_EDITOR_ICU = "pref_code_editor_icu";
    public static final String KEY_CODE_EDITOR_AUTO_SAVE = "pref_code_editor_auto_save";
    public static final String KEY_CODE_EDITOR_RELATIVE_CLOSE_DEPTH =
        "pref_code_editor_relative_close_depth";
    public static final String KEY_CODE_EDITOR_PIN_LINE_NUM = "pref_code_editor_pin_line_numbers";
    public static final String KEY_CODE_EDITOR_MAGNIFIER = "pref_code_editor_use_magnifier";
    public static final String KEY_CODE_EDITOR_STICKY_SCROLL = "pref_code_editor_sticky_scroll";
    public static final String KEY_CODE_EDITOR_AUTO_CLOSE_BRACKET =
        "pref_code_editor_auto_close_bracket";
    public static final String KEY_CODE_EDITOR_SCROLL_BAR = "pref_code_editor_scroll_bar";
    public static final String KEY_CODE_EDITOR_HARDWARE_ACCELERATION =
        "pref_code_editor_hardware_acceleration";
    public static final String KEY_CODE_EDITOR_LINE_NUMBERS = "pref_code_editor_line_numbers";
    public static final String KEY_CODE_EDITOR_DELETE_EMPTY_LINE =
        "pref_code_editor_delete_empty_line_bck_key_event";
    public static final String KEY_CODE_EDITOR_DELETE_TAB =
        "pref_code_editor_delete_tab_bck_key_event";
    public static final String KEY_CODE_EDITOR_ANIMATE_AUTO_COMP_WINDOW =
        "pref_code_editor_animate_auto_complt_window";
    public static final String KEY_CODE_EDITOR_HIGHLIGHT_BRACKET =
        "pref_code_editor_highlight_brckt";
    public static final String KEY_CODE_EDITOR_AUTO_COMPLETE = "pref_code_editor_auto_complete";
    public static final String KEY_CODE_EDITOR_GHOST_TEXT =
        "pref_code_editor_ghost_text_completion";
    public static final String KEY_CODE_EDITOR_CURSOR_BLINK_PERIOD =
        "pref_code_editor_cursor_blnk_period";
    public static final String KEY_CODE_EDITOR_TAB_SIZE = "pref_code_editor_tab_size";
    public static final String KEY_CODE_EDITOR_LINE_HEIGHT = "pref_code_editor_line_height";
    public static final String KEY_CODE_EDITOR_NP_PAINT_FLAGS = "pref_code_editor_npc";
    public static final String KEY_CODE_EDITORMINIMAP = "pref_code_editor_minimaprender";
    public static final String KEY_CODE_EDITOR_FONT = "pref_code_editor_fontselect";
    public static final String KEY_CODE_SHOWLC = "pref_code_editorshowlc";
    // General Configuration Preferences
    public static final String KEY_BUFFER_SIZE = "pref_general_config_buffer_size";
    public static final String KEY_APP_THEME = "pref_app_theme";
    public static final String KEY_APP_THEME_FILE = "pref_app_theme_file";
    // GitHub Account Preferences
    public static final String KEY_GITHUB_TOKEN = "pref_github_token";
    public static final String KEY_GITHUB_USERNAME = "pref_github_username";
    public static final String KEY_GITHUB_NAME = "pref_github_name";
    public static final String KEY_GITHUB_AVATAR_URL = "pref_github_avatar_url";
    public static final String KEY_SHOWTAB_ICON = "pref_showicon_tab";
    public static final String KEY_TRANSLATE_TARGET_LANG = "pref_translate_target_lang";
    public static final String KEY_GIT_LOCAL_USER_NAME = "git_local_user_name";
    public static final String KEY_GIT_LOCAL_USER_EMAIL = "git_local_user_email";
    public static final String KEY_DEVNAMEGIT = "git_pref_name";
    public static final String KEY_EMAILGIT = "git_pref_gitname";
    public static final String KEY_GRIDMOD = "filemanager_grid_mod";
    public static final String KEY_GRIDCOUNT = "filemanager_grid_count";
    public static final String KEY_FILRMB = "filemanager_showbackgroundtheme";
    public static final String KEY_ANIMATION_BATTERY_THRESHOLD = "pref_animation_battery_threshold";
    public static final String KEY_SHOW_HIDDEN_FILES = "filemanager_show_hidden_files";
    public static final String KEY_CODE_EDITOR_POWER_MODE = "pref_code_editor_power_mode";
    public static final String KEY_CODE_EDITOR_POWER_MODE_EFFECT =
        "pref_code_editor_power_mode_effect";
    public static final String KRY_TERM = "terminal_fragment";
    public static final String KEY_BLUR = "app_blurmod_allstate";
    public static final String KEY_CONSTZOOM = "app_const_zoommod";
    public static final String Key_langColor = "editor_tabcolor";

    private SharedPreferenceKeys() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
  }
}
