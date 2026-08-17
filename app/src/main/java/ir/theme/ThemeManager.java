package ir.theme;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import com.blankj.utilcode.util.FileIOUtils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ir.hanzodev1375.ghostide.codeeditors.setting.PreferencesUtils;
import ir.hanzodev1375.ghostide.utils.ConstKeys;
import ir.hanzodev1375.ghostide.utils.FileUtil;
import java.io.File;
import java.nio.charset.StandardCharsets;

public class ThemeManager {

  private final SharedPreferences preferences;
  private final Gson gson;
  private final Context context;

  public ThemeManager(Context context) {
    this.context = context;
    this.preferences = context.getSharedPreferences(ConstKeys.PREFS_NAME, Context.MODE_PRIVATE);
    this.gson = new Gson();
  }

  public void saveTheme(GhostTheme theme) {
    String json = gson.toJson(theme);
    preferences.edit().putString(ConstKeys.THEME, json).apply();
  }

  public GhostTheme getTheme() {
    PreferencesUtils prefsUtils = new PreferencesUtils(context);
    String themeFile = prefsUtils.getAppThemeFile();

    if (!TextUtils.isEmpty(themeFile)) {
      File file = new File(themeFile);
      if (file.exists()) {
        try {
          String json =
              new String(FileUtil.readBytesCompat(file), StandardCharsets.UTF_8);
          String merged = mergeWithDefault(json);
          GhostTheme theme = gson.fromJson(merged, GhostTheme.class);
          if (theme != null) {
            return theme;
          }
        } catch (Exception ignored) {
        }
      }
      prefsUtils.setAppThemeFile("");
    }

    String json = preferences.getString(ConstKeys.THEME, null);
    if (json == null || json.isEmpty()) {
      json = getDefaultThemeJson();
    }

    try {
      String merged = mergeWithDefault(json);
      GhostTheme theme = gson.fromJson(merged, GhostTheme.class);
      if (theme == null) {
        return gson.fromJson(getDefaultThemeJson(), GhostTheme.class);
      }
      return theme;
    } catch (Exception e) {
      return gson.fromJson(getDefaultThemeJson(), GhostTheme.class);
    }
  }

  public void setThemeFromFile(String filePath) {
    PreferencesUtils prefsUtils = new PreferencesUtils(context);
    if (filePath == null || filePath.trim().isEmpty()) {
      prefsUtils.setAppThemeFile("");
      preferences.edit().putString(ConstKeys.THEME, getDefaultThemeJson()).apply();
      return;
    }

    File file = new File(filePath);
    if (file.exists()) {
      try {
        String json = FileIOUtils.readFile2String(file);
        String merged = mergeWithDefault(json);
        GhostTheme theme = gson.fromJson(merged, GhostTheme.class);
        if (theme != null) {
          prefsUtils.setAppThemeFile(filePath);
          preferences.edit().putString(ConstKeys.THEME, merged).apply();
          return;
        }
      } catch (Exception ignored) {
      }
    }
    prefsUtils.setAppThemeFile("");
    preferences.edit().putString(ConstKeys.THEME, getDefaultThemeJson()).apply();
  }

  private String mergeWithDefault(String loadedJson) {
    JsonObject defaultObj = JsonParser.parseString(getDefaultThemeJson()).getAsJsonObject();
    JsonObject loadedObj = JsonParser.parseString(loadedJson).getAsJsonObject();
    deepMerge(defaultObj, loadedObj);
    return loadedObj.toString();
  }

  private void deepMerge(JsonObject defaultObj, JsonObject loadedObj) {
    for (String key : defaultObj.keySet()) {
      if (!loadedObj.has(key) || loadedObj.get(key).isJsonNull()) {
        loadedObj.add(key, defaultObj.get(key));
      } else if (defaultObj.get(key).isJsonObject() && loadedObj.get(key).isJsonObject()) {
        deepMerge(defaultObj.getAsJsonObject(key), loadedObj.getAsJsonObject(key));
      }
    }
  }

  public String getDefaultThemeJson() {
    return """
        {
          "activity": {
            "background": "#282c34",
            "statusBar": "#282c34",
            "navigationBar": "#282c34"
          },
          "editor": {
            "lineDivider": "#3e4452",
            "lineNumber": "#5c6370",
            "lineNumberBackground": "#282c34",
            "wholeBackground": "#282c34",
            "textNormal": "#abb2bf",
            "selectedTextBackground": "#3e4452",
            "selectionInsert": "#528bff",
            "selectionHandle": "#528bff",
            "currentLine": "#2c313a",
            "underline": "#abb2bf",
            "scrollBarThumb": "#3e4452",
            "scrollBarThumbPressed": "#528bff",
            "scrollBarTrack": "#21252b",
            "blockLine": "#3e4452",
            "blockLineCurrent": "#528bff",
            "lineNumberPanel": "#21252b",
            "lineNumberPanelText": "#abb2bf",
            "completionWndBackground": "#282c34",
            "completionWndCorner": "#282c34",
            "keyword": "#c678dd",
            "comment": "#5c6370",
            "operator": "#56b6c2",
            "literal": "#d19a66",
            "identifierVar": "#e06c75",
            "identifierName": "#61afef",
            "functionName": "#61afef",
            "annotation": "#e5c07b",
            "matchedTextBackground": "#3e4452",
            "matchedTextBorder": "#528bff",
            "textSelected": "#ffffff",
            "nonPrintableChar": "#3e4452",
            "htmlTag": "#e06c75",
            "attributeName": "#d19a66",
            "attributeValue": "#98c379",
            "problemError": "#e06c75",
            "problemWarning": "#e5c07b",
            "problemTypo": "#98c379",
            "colornextdot": "#c678dd",
            "colornextbrak": "#56b6c2",
            "colornextchar": "#d19a66",
            "coloruppercase": "#61afef",
            "colornextless": "#98c379",
            "lineNumberCurrent": "#528bff",
            "selectedTextBorder": "#528bff",
            "currentRowBorder": "#3e4452",
            "highlightedDelimitersBackground": "#2c313a",
            "highlightedDelimitersUnderline": "#528bff",
            "highlightedDelimitersForeground": "#abb2bf",
            "highlightedDelimitersBorder": "#528bff",
            "textHighlightBackground": "#3e4452",
            "textHighlightBorder": "#528bff",
            "textHighlightStrongBackground": "#2c313a",
            "textHighlightStrongBorder": "#c678dd",
            "staticSpanBackground": "#282c34",
            "staticSpanForeground": "#abb2bf",
            "textInlayHintBackground": "#2c313a",
            "textInlayHintForeground": "#5c6370",
            "snippetBackgroundEditing": "#2c313a",
            "snippetBackgroundRelated": "#3e4452",
            "snippetBackgroundInactive": "#21252b",
            "hardWrapMarker": "#3e4452",
            "functionCharBackgroundStroke": "#3e4452",
            "diagnosticTooltipBackground": "#2c313a",
            "diagnosticTooltipBriefMsg": "#abb2bf",
            "diagnosticTooltipDetailedMsg": "#5c6370",
            "diagnosticTooltipAction": "#61afef",
            "stickyScrollDivider": "#3e4452",
            "strikeThrough": "#00000000",
            "sideBlockLine": "#3e4452",
            "completionWndTextPrimary": "#abb2bf",
            "completionWndTextSecondary": "#5c6370",
            "completionWndItemCurrent": "#2c313a",
            "completionWndTextMatched": "#61afef",
            "signatureBackground": "#282c34",
            "signatureBorder": "#3e4452",
            "signatureTextNormal": "#abb2bf",
            "signatureTextHighlightedParameter": "#e06c75",
            "hoverBackground": "#2c313a",
            "hoverBorder": "#528bff",
            "hoverTextNormal": "#abb2bf",
            "hoverTextHighlighted": "#61afef",
            "textActionWindowBackground": "#282c34",
            "textActionWindowIconColor": "#abb2bf",
            "minimapBackground": "#a0282c34",
            "minimapViewport": "#30ffffff",
            "minimapViewportBorder": "#b0ffffff",
            "bracketlevelmatch1": "#FFDD00",
            "bracketlevelmatch2": "#00D9FF",
            "bracketlevelmatch3": "#00FF55",
            "bracketlevelmatch4": "#FF6200",
            "bracketlevelmatch5": "#FF64F5",
            "bracketlevelmatch6": "#64FFD0"
          },
          "widget": {
            "text": "#abb2bf",
            "hint": "#5c6370",
            "accent": "#61afef",
            "background": "#282c34",
            "surface": "#2c313a",
            "stroke": "#3e4452",
            "fabBackground": "#61afef",
            "fabIcon": "#ffffff",
            "tabSelected": "#61afef",
            "tabUnselected": "#5c6370",
            "imageTint": "#abb2bf",
            "menubackground": "#282c34",
            "menutextcolor": "#abb2bf",
            "selectedmenucolor": "#3e4452",
            "imagepath": "",
            "blursize": 1
          }
        }
        """;
  }

  public void resetToDefault() {
    preferences.edit().remove(ConstKeys.THEME).apply();
    new PreferencesUtils(context).setAppThemeFile("");
  }
}
