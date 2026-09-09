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
import ir.theme.internal.ThemeRefResolver;
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
    ThemeRefResolver.resolveJson(loadedObj);
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
    "background": "#1e1e1e",
    "statusBar": "#1e1e1e",
    "navigationBar": "#1e1e1e"
  },

  "editor": {
    "lineDivider": "#333333",
    "wholeBackground": "#1e1e1e",
    "lineNumber": "#858585",
    "lineNumberBackground": "#1e1e1e",
    "textNormal": "#d4d4d4",
    "keyword": "#569cd6",
    "comment": "#6a9955",
    "operator": "#d4d4d4",
    "literal": "#ce9178",
    "identifierVar": "#9cdcfe",
    "identifierName": "#4ec9b0",
    "functionName": "#dcdcaa",
    "annotation": "#d0d0d0",
    "htmlTag": "#569cd6",
    "attributeName": "#9cdcfe",
    "attributeValue": "#ce9178",
    "nonPrintableChar": "#858585",
    "colornextdot": "#9cdcfe",
    "colornextbrak": "#569cd6",
    "colornextchar": "#ce9178",
    "coloruppercase": "#4ec9b0",
    "colornextless": "#6a9955",
    "currentLine": "#282828",
    "currentRowBorder": "#333333",
    "blockLine": "#333333",
    "blockLineCurrent": "#569cd6",
    "sideBlockLine": "#333333",
    "hardWrapMarker": "#333333",
    "strikeThrough": "#f48771",
    "lineNumberCurrent": "#c6c6c6",
    "selectedTextBackground": "#264f78",
    "selectedTextBorder": "#569cd6",
    "textSelected": "#ffffff",
    "selectionInsert": "#aeafad",
    "selectionHandle": "#569cd6",
    "underline": "#9cdcfe",
    "scrollBarThumb": "#424242",
    "scrollBarThumbPressed": "#569cd6",
    "scrollBarTrack": "#1e1e1e",
    "completionWndBackground": "#252526",
    "completionWndCorner": "#252526",
    "completionWndTextPrimary": "#d4d4d4",
    "completionWndTextSecondary": "#858585",
    "completionWndItemCurrent": "#04395e",
    "completionWndTextMatched": "#569cd6",
    "matchedTextBackground": "#613214",
    "matchedTextBorder": "#569cd6",
    "highlightedDelimitersBackground": "#282828",
    "highlightedDelimitersUnderline": "#569cd6",
    "highlightedDelimitersForeground": "#d4d4d4",
    "highlightedDelimitersBorder": "#569cd6",
    "textHighlightBackground": "#343b41",
    "textHighlightBorder": "#569cd6",
    "textHighlightStrongBackground": "#282828",
    "textHighlightStrongBorder": "#dcdcaa",
    "staticSpanBackground": "#1e1e1e",
    "staticSpanForeground": "#d4d4d4",
    "problemError": "#f48771",
    "problemWarning": "#cca700",
    "problemTypo": "#73c991",
    "signatureBackground": "#252526",
    "signatureBorder": "#333333",
    "signatureTextNormal": "#d4d4d4",
    "signatureTextHighlightedParameter": "#569cd6",
    "hoverBackground": "#252526",
    "hoverBorder": "#454545",
    "hoverTextNormal": "#d4d4d4",
    "hoverTextHighlighted": "#9cdcfe",
    "diagnosticTooltipBackground": "#252526",
    "diagnosticTooltipBriefMsg": "#d4d4d4",
    "diagnosticTooltipDetailedMsg": "#858585",
    "diagnosticTooltipAction": "#4fc1ff",
    "textActionWindowBackground": "#252526",
    "textActionWindowIconColor": "#569cd6",
    "textInlayHintBackground": "#1e1e1e",
    "textInlayHintForeground": "#858585",
    "snippetBackgroundEditing": "#282828",
    "snippetBackgroundRelated": "#333333",
    "snippetBackgroundInactive": "#1e1e1e",
    "functionCharBackgroundStroke": "#dcdcaa",
    "minimapBackground": "#191919",
    "minimapViewport": "#30ffffff",
    "minimapViewportBorder": "#b0ffffff",
    "bracketlevelmatch1": "#569cd6",
    "bracketlevelmatch2": "#4ec9b0",
    "bracketlevelmatch3": "#ce9178",
    "bracketlevelmatch4": "#dcdcaa",
    "bracketlevelmatch5": "#c586c0",
    "bracketlevelmatch6": "#6a9955",
    "stickyScrollDivider": "#333333"
  },

  "widget": {
    "text": "#cccccc",
    "hint": "#858585",
    "accent": "#0e639c",
    "background": "#1e1e1e",
    "surface": "#252526",
    "stroke": "#3c3c3c",
    "fabBackground": "#0e639c",
    "fabIcon": "#ffffff",
    "tabSelected": "#0e639c",
    "tabUnselected": "#858585",
    "imageTint": "#cccccc",
    "menubackground": "#252526",
    "menutextcolor": "#cccccc",
    "selectedmenucolor": "#04395e",
    "imagepath": "",
    "blursize": 1
  },

  "material3": {
    "primary": "#4fc1ff",
    "surfaceTint": "#4fc1ff",
    "onPrimary": "#00324c",
    "primaryContainer": "#004969",
    "onPrimaryContainer": "#c6e6ff",
    "secondary": "#94cff2",
    "onSecondary": "#003549",
    "secondaryContainer": "#004d66",
    "onSecondaryContainer": "#cce7ff",
    "tertiary": "#dc88c9",
    "onTertiary": "#4c223f",
    "tertiaryContainer": "#693853",
    "onTertiaryContainer": "#ffd8ef",
    "error": "#f48771",
    "onError": "#690000",
    "errorContainer": "#93000a",
    "onErrorContainer": "#ffdad7",
    "background": "#111111",
    "onBackground": "#e2e2e2",
    "surface": "#111111",
    "onSurface": "#e2e2e2",
    "surfaceVariant": "#434343",
    "onSurfaceVariant": "#c4c4c4",
    "outline": "#8e8e8e",
    "outlineVariant": "#434343",
    "shadow": "#000000",
    "scrim": "#000000",
    "inverseSurface": "#e2e2e2",
    "inverseOnSurface": "#303030",
    "inversePrimary": "#006491",
    "primaryFixed": "#c6e6ff",
    "onPrimaryFixed": "#003149",
    "primaryFixedDim": "#8dcdff",
    "onPrimaryFixedVariant": "#004969",
    "secondaryFixed": "#cce7ff",
    "onSecondaryFixed": "#003549",
    "secondaryFixedDim": "#7fb9dc",
    "onSecondaryFixedVariant": "#004d66",
    "tertiaryFixed": "#ffd8ef",
    "onTertiaryFixed": "#390f30",
    "tertiaryFixedDim": "#e0a7ce",
    "onTertiaryFixedVariant": "#693853",
    "surfaceDim": "#111111",
    "surfaceBright": "#383838",
    "surfaceContainerLowest": "#0c0c0c",
    "surfaceContainerLow": "#191919",
    "surfaceContainer": "#1d1d1d",
    "surfaceContainerHigh": "#282828",
    "surfaceContainerHighest": "#323232"
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
