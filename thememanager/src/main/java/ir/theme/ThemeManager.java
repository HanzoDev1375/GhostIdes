package ir.theme;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import androidx.core.util.Supplier;
import com.blankj.utilcode.util.FileIOUtils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ir.theme.internal.ThemeConstKeys;
import ir.theme.internal.ThemeFileUtil;
import ir.theme.internal.ThemePrefsHelper;
import java.io.File;
import java.nio.charset.StandardCharsets;

public class ThemeManager {

  private final SharedPreferences preferences;
  private final Gson gson;
  private final Context context;
  private final ThemePrefsHelper prefsHelper;

  private static final Object CACHE_LOCK = new Object();
  private static String cacheKey;
  private static String cachedMergedJson;
  private static GhostTheme cachedThemeObj;

  private static volatile String cachedDefaultThemeJson;

  public ThemeManager(Context context) {
    this.context = context;
    this.preferences =
        context.getSharedPreferences(ThemeConstKeys.PREFS_NAME, Context.MODE_PRIVATE);
    this.gson = new Gson();
    this.prefsHelper = new ThemePrefsHelper(context);
  }

  private static volatile ThemeManager shared;

  /** Reuses a single ThemeManager (cheap: one Gson, one prefs helper). */
  public static ThemeManager getDefault(Context context) {
    ThemeManager s = shared;
    if (s == null) {
      synchronized (ThemeManager.class) {
        s = shared;
        if (s == null) {
          s = new ThemeManager(context.getApplicationContext());
          shared = s;
        }
      }
    }
    return s;
  }

  public String getThemeFilePath() {
    return prefsHelper.getAppThemeFile();
  }

  public void saveTheme(GhostTheme theme) {
    String json = gson.toJson(theme);
    prefsHelper.putThemeJson(json);
    invalidateCache();
  }

  public GhostTheme getTheme() {
    String themeFile = prefsHelper.getAppThemeFile();

    if (!TextUtils.isEmpty(themeFile)) {
      File file = new File(themeFile);
      if (file.exists()) {
        try {
          String key = "file:" + themeFile + "@" + file.lastModified() + ":" + file.length();
          GhostTheme theme =
              fromCacheOrParse(
                  key,
                  () -> {
                    try {
                      return new String(
                          ThemeFileUtil.readBytesCompat(new File(themeFile)), StandardCharsets.UTF_8);
                    } catch (Exception err) {
                      return "";
                    }
                  });

          if (theme != null) {
            return theme;
          }
        } catch (Exception ignored) {
        }
      }
      prefsHelper.setAppThemeFile("");
    }

    String json = prefsHelper.getThemeJson();
    if (json == null || json.isEmpty()) {
      json = getDefaultThemeJson();
    }

    try {
      String key = "prefs:" + json.length() + "@" + json.hashCode();
      final String json2 = json;
      return fromCacheOrParse(key, () -> json2);
    } catch (Exception e) {
      return gson.fromJson(getDefaultThemeJson(), GhostTheme.class);
    }
  }

  /** Returns the cached GhostTheme for key, or parses it once and caches the object. */
  private GhostTheme fromCacheOrParse(
      String key, Supplier<String> jsonSupplier) {
    synchronized (CACHE_LOCK) {
      if (key.equals(cacheKey)) {
        if (cachedThemeObj != null) {
          return cachedThemeObj;
        }
        if (cachedMergedJson != null) {
          cachedThemeObj = gson.fromJson(cachedMergedJson, GhostTheme.class);
          return cachedThemeObj;
        }
      }
      String merged = mergeWithDefault(jsonSupplier.get());
      cachedMergedJson = merged;
      cacheKey = key;
      cachedThemeObj = gson.fromJson(merged, GhostTheme.class);
      return cachedThemeObj;
    }
  }

  private static void invalidateCache() {
    synchronized (CACHE_LOCK) {
      cacheKey = null;
      cachedMergedJson = null;
      cachedThemeObj = null;
    }
  }

  public void setThemeFromFile(String filePath) {
    invalidateCache();
    if (filePath == null || filePath.trim().isEmpty()) {
      prefsHelper.setAppThemeFile("");
      prefsHelper.putThemeJson(getDefaultThemeJson());
      return;
    }

    File file = new File(filePath);
    if (file.exists()) {
      try {
        String json = FileIOUtils.readFile2String(file);
        String merged = mergeWithDefault(json);
        GhostTheme theme = gson.fromJson(merged, GhostTheme.class);
        if (theme != null) {
          prefsHelper.setAppThemeFile(filePath);
          prefsHelper.putThemeJson(merged);
          return;
        }
      } catch (Exception ignored) {
      }
    }
    prefsHelper.setAppThemeFile("");
    prefsHelper.putThemeJson(getDefaultThemeJson());
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
    String cached = cachedDefaultThemeJson;
    if (cached != null) {
      return cached;
    }
    String json = buildDefaultThemeJson();
    cachedDefaultThemeJson = json;
    return json;
  }

  private String buildDefaultThemeJson() {
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
    },
    "material3": {
      "primary": "#B9C3FF",
      "surfaceTint": "#B9C3FF",
      "onPrimary": "#212C61",
      "primaryContainer": "#384379",
      "onPrimaryContainer": "#DDE1FF",
      "secondary": "#C3C5DD",
      "onSecondary": "#2C2F42",
      "secondaryContainer": "#424659",
      "onSecondaryContainer": "#DFE1F9",
      "tertiary": "#E5BAD8",
      "onTertiary": "#44263E",
      "tertiaryContainer": "#5C3C55",
      "onTertiaryContainer": "#FFD7F3",
      "error": "#FFB4AB",
      "onError": "#690005",
      "errorContainer": "#93000A",
      "onErrorContainer": "#FFDAD6",
      "background": "#121318",
      "onBackground": "#E3E1E9",
      "surface": "#121318",
      "onSurface": "#E3E1E9",
      "surfaceVariant": "#45464F",
      "onSurfaceVariant": "#C6C5D0",
      "outline": "#90909A",
      "outlineVariant": "#45464F",
      "shadow": "#000000",
      "scrim": "#000000",
      "inverseSurface": "#E3E1E9",
      "inverseOnSurface": "#303036",
      "inversePrimary": "#505B92",
      "primaryFixed": "#DDE1FF",
      "onPrimaryFixed": "#08164B",
      "primaryFixedDim": "#B9C3FF",
      "onPrimaryFixedVariant": "#384379",
      "secondaryFixed": "#DFE1F9",
      "onSecondaryFixed": "#171B2C",
      "secondaryFixedDim": "#C3C5DD",
      "onSecondaryFixedVariant": "#424659",
      "tertiaryFixed": "#FFD7F3",
      "onTertiaryFixed": "#2D1228",
      "tertiaryFixedDim": "#E5BAD8",
      "onTertiaryFixedVariant": "#5C3C55",
      "surfaceDim": "#121318",
      "surfaceBright": "#38393F",
      "surfaceContainerLowest": "#0D0E13",
      "surfaceContainerLow": "#1B1B21",
      "surfaceContainer": "#1F1F25",
      "surfaceContainerHigh": "#292A2F",
      "surfaceContainerHighest": "#34343A"
    }
  }
        """;
  }

  public void resetToDefault() {
    invalidateCache();
    prefsHelper.removeThemeJson();
    prefsHelper.setAppThemeFile("");
  }
}
