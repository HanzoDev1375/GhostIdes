package ir.theme.themeeditor;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.text.Spanned;
import android.view.Menu;
import android.view.MenuItem;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.Slider;
import com.google.android.material.tabs.TabLayout;
import com.blankj.utilcode.util.FileIOUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ir.hanzodev1375.components.childern.ViewChilder;
import ir.hanzodev1375.ghostide.R;
import ir.hanzodev1375.ghostide.activity.BaseCompat;
import ir.hanzodev1375.ghostide.codeeditors.colorrender.ColorPickerBottomSheetDialog;
import ir.theme.ActivityTheme;
import ir.theme.EditorTheme;
import ir.theme.GhostTheme;
import ir.theme.ThemeManager;
import ir.theme.WidgetTheme;

public class ThemeEditorActivity extends BaseCompat {

  public static final String EXTRA_THEME_PATH = "theme_path";

  private TabLayout tabLayout;
  private RecyclerView recyclerView;
  private ThemeDetailAdapter adapter;
  private GhostTheme currentTheme;
  private Gson gson = new GsonBuilder().setPrettyPrinting().create();
  private String currentThemePath;
  private SearchView searchView;
  private String currentQuery = "";
  private boolean isSearching = false;
  private List<ThemeRow> activityItems = new ArrayList<>();
  private List<ThemeRow> editorItems = new ArrayList<>();
  private List<ThemeRow> widgetItems = new ArrayList<>();
  private ImageItem pendingImageItem;
  private ActivityResultLauncher<String[]> pickImageLauncher;
  private Map<String, String> titleToKeyMap = new HashMap<>();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    pickImageLauncher =
        registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
              if (uri == null || pendingImageItem == null) return;
              try {
                getContentResolver()
                    .takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
              } catch (Exception ignored) {
              }
              pendingImageItem.updater.update(currentTheme, uri.toString());
              saveThemeToFile();
              buildColorItems();
              refreshCurrentTab();
            });

    setContentView(R.layout.activity_theme_editor);
    setupBackgroundBlur();

    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    currentThemePath = getIntent().getStringExtra(EXTRA_THEME_PATH);
    if (currentThemePath == null || currentThemePath.isEmpty()) {
      Toast.makeText(this, "No theme file path", Toast.LENGTH_SHORT).show();
      finish();
      return;
    }

    File file = new File(currentThemePath);
    if (!file.exists()) {
      Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show();
      finish();
      return;
    }

    String json = readFileToString(file);
    if (json == null || json.isEmpty()) {
      Toast.makeText(this, "Failed to read file", Toast.LENGTH_SHORT).show();
      finish();
      return;
    }

    try {
      currentTheme = gson.fromJson(json, GhostTheme.class);
      if (currentTheme == null) throw new Exception();
      if (currentTheme.getActivity() == null) currentTheme.setActivity(new ActivityTheme());
      if (currentTheme.getEditor() == null) currentTheme.setEditor(new EditorTheme());
      if (currentTheme.getWidget() == null) currentTheme.setWidget(new WidgetTheme());
    } catch (Exception e) {
      Toast.makeText(this, "Invalid theme", Toast.LENGTH_SHORT).show();
      finish();
      return;
    }

    initTitleToKeyMap();
    tabLayout = findViewById(R.id.tabLayout);
    recyclerView = findViewById(R.id.recyclerView);
    recyclerView.setLayoutManager(new LinearLayoutManager(this));

    buildColorItems();

    tabLayout.addTab(tabLayout.newTab().setText("Activity"));
    tabLayout.addTab(tabLayout.newTab().setText("Editor"));
    tabLayout.addTab(tabLayout.newTab().setText("Widget"));

    tabLayout.addOnTabSelectedListener(
        new TabLayout.OnTabSelectedListener() {
          @Override
          public void onTabSelected(TabLayout.Tab tab) {
            switch (tab.getPosition()) {
              case 0:
                adapter = new ThemeDetailAdapter(activityItems);
                break;
              case 1:
                adapter = new ThemeDetailAdapter(editorItems);
                break;
              case 2:
                adapter = new ThemeDetailAdapter(widgetItems);
                break;
            }
            clearSearch();
            recyclerView.setAdapter(adapter);
          }

          @Override
          public void onTabUnselected(TabLayout.Tab tab) {}

          @Override
          public void onTabReselected(TabLayout.Tab tab) {}
        });

    adapter = new ThemeDetailAdapter(activityItems);
    recyclerView.setAdapter(adapter);
  }

  private void setupBackgroundBlur() {
    ViewChilder background = findViewById(R.id.backgroundIconThemeEditor);
    View appbar = findViewById(R.id.appbar);
    if (background == null || appbar == null) return;
    setupBackgroundBlur(background, appbar);
  }

  private String readFileToString(File file) {
    try (FileInputStream fis = new FileInputStream(file)) {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      byte[] buffer = new byte[1024];
      int len;
      while ((len = fis.read(buffer)) != -1) {
        baos.write(buffer, 0, len);
      }
      return baos.toString(StandardCharsets.UTF_8.name());
    } catch (IOException e) {
      return null;
    }
  }

  private void initTitleToKeyMap() {
    // Activity
    titleToKeyMap.put("Background", "background");
    titleToKeyMap.put("Status Bar", "statusBar");
    titleToKeyMap.put("Navigation Bar", "navigationBar");

    // Editor
    titleToKeyMap.put("Line Divider", "lineDivider");
    titleToKeyMap.put("Text Normal", "textNormal");
    titleToKeyMap.put("Keyword", "keyword");
    titleToKeyMap.put("Comment", "comment");
    titleToKeyMap.put("Operator", "operator");
    titleToKeyMap.put("Literal", "literal");
    titleToKeyMap.put("Identifier Var", "identifierVar");
    titleToKeyMap.put("Identifier Name", "identifierName");
    titleToKeyMap.put("Function Name", "functionName");
    titleToKeyMap.put("Annotation", "annotation");
    titleToKeyMap.put("Current Line", "currentLine");
    titleToKeyMap.put("Line Number", "lineNumber");
    titleToKeyMap.put("Line Number Background", "lineNumberBackground");
    titleToKeyMap.put("Selected Text Background", "selectedTextBackground");
    titleToKeyMap.put("Selection Insert", "selectionInsert");
    titleToKeyMap.put("Selection Handle", "selectionHandle");
    titleToKeyMap.put("Underline", "underline");
    titleToKeyMap.put("Scroll Bar Thumb", "scrollBarThumb");
    titleToKeyMap.put("Scroll Bar Thumb Pressed", "scrollBarThumbPressed");
    titleToKeyMap.put("Scroll Bar Track", "scrollBarTrack");
    titleToKeyMap.put("Block Line", "blockLine");
    titleToKeyMap.put("Block Line Current", "blockLineCurrent");
    titleToKeyMap.put("Line Number Panel", "lineNumberPanel");
    titleToKeyMap.put("Line Number Panel Text", "lineNumberPanelText");
    titleToKeyMap.put("Completion Wnd Background", "completionWndBackground");
    titleToKeyMap.put("Completion Wnd Corner", "completionWndCorner");
    titleToKeyMap.put("Matched Text Background", "matchedTextBackground");
    titleToKeyMap.put("Matched Text Border", "matchedTextBorder");
    titleToKeyMap.put("Text Selected", "textSelected");
    titleToKeyMap.put("Non Printable Char", "nonPrintableChar");
    titleToKeyMap.put("HTML Tag", "htmlTag");
    titleToKeyMap.put("Attribute Name", "attributeName");
    titleToKeyMap.put("Attribute Value", "attributeValue");
    titleToKeyMap.put("Problem Error", "problemError");
    titleToKeyMap.put("Problem Warning", "problemWarning");
    titleToKeyMap.put("Problem Typo", "problemTypo");
    titleToKeyMap.put("Color Next Dot", "colornextdot");
    titleToKeyMap.put("Color Next Brak", "colornextbrak");
    titleToKeyMap.put("Color Next Char", "colornextchar");
    titleToKeyMap.put("Color Uppercase", "coloruppercase");
    titleToKeyMap.put("Color Next Less", "colornextless");
    titleToKeyMap.put("Line Number Current", "lineNumberCurrent");
    titleToKeyMap.put("Selected Text Border", "selectedTextBorder");
    titleToKeyMap.put("Current Row Border", "currentRowBorder");
    titleToKeyMap.put("Highlighted Delimiters Background", "highlightedDelimitersBackground");
    titleToKeyMap.put("Highlighted Delimiters Underline", "highlightedDelimitersUnderline");
    titleToKeyMap.put("Highlighted Delimiters Foreground", "highlightedDelimitersForeground");
    titleToKeyMap.put("Highlighted Delimiters Border", "highlightedDelimitersBorder");
    titleToKeyMap.put("Text Highlight Background", "textHighlightBackground");
    titleToKeyMap.put("Text Highlight Border", "textHighlightBorder");
    titleToKeyMap.put("Text Highlight Strong Background", "textHighlightStrongBackground");
    titleToKeyMap.put("Text Highlight Strong Border", "textHighlightStrongBorder");
    titleToKeyMap.put("Static Span Background", "staticSpanBackground");
    titleToKeyMap.put("Static Span Foreground", "staticSpanForeground");
    titleToKeyMap.put("Text Inlay Hint Background", "textInlayHintBackground");
    titleToKeyMap.put("Text Inlay Hint Foreground", "textInlayHintForeground");
    titleToKeyMap.put("Snippet Background Editing", "snippetBackgroundEditing");
    titleToKeyMap.put("Snippet Background Related", "snippetBackgroundRelated");
    titleToKeyMap.put("Snippet Background Inactive", "snippetBackgroundInactive");
    titleToKeyMap.put("Hard Wrap Marker", "hardWrapMarker");
    titleToKeyMap.put("Function Char Background Stroke", "functionCharBackgroundStroke");
    titleToKeyMap.put("Diagnostic Tooltip Background", "diagnosticTooltipBackground");
    titleToKeyMap.put("Diagnostic Tooltip Brief Msg", "diagnosticTooltipBriefMsg");
    titleToKeyMap.put("Diagnostic Tooltip Detailed Msg", "diagnosticTooltipDetailedMsg");
    titleToKeyMap.put("Diagnostic Tooltip Action", "diagnosticTooltipAction");
    titleToKeyMap.put("Sticky Scroll Divider", "stickyScrollDivider");
    titleToKeyMap.put("Strike Through", "strikeThrough");
    titleToKeyMap.put("Side Block Line", "sideBlockLine");
    titleToKeyMap.put("Completion Wnd Text Primary", "completionWndTextPrimary");
    titleToKeyMap.put("Completion Wnd Text Secondary", "completionWndTextSecondary");
    titleToKeyMap.put("Completion Wnd Item Current", "completionWndItemCurrent");
    titleToKeyMap.put("Completion Wnd Text Matched", "completionWndTextMatched");
    titleToKeyMap.put("Signature Background", "signatureBackground");
    titleToKeyMap.put("Signature Border", "signatureBorder");
    titleToKeyMap.put("Signature Text Normal", "signatureTextNormal");
    titleToKeyMap.put("Signature Text Highlighted Parameter", "signatureTextHighlightedParameter");
    titleToKeyMap.put("Hover Background", "hoverBackground");
    titleToKeyMap.put("Hover Border", "hoverBorder");
    titleToKeyMap.put("Hover Text Normal", "hoverTextNormal");
    titleToKeyMap.put("Hover Text Highlighted", "hoverTextHighlighted");
    titleToKeyMap.put("Text Action Window Background", "textActionWindowBackground");
    titleToKeyMap.put("Text Action Window Icon Color", "textActionWindowIconColor");
    titleToKeyMap.put("Minimap Background", "minimapBackground");
    titleToKeyMap.put("Minimap Viewport", "minimapViewport");
    titleToKeyMap.put("Minimap Viewport Border", "minimapViewportBorder");
    titleToKeyMap.put("Bracket Level Match 1", "bracketlevelmatch1");
    titleToKeyMap.put("Bracket Level Match 2", "bracketlevelmatch2");
    titleToKeyMap.put("Bracket Level Match 3", "bracketlevelmatch3");
    titleToKeyMap.put("Bracket Level Match 4", "bracketlevelmatch4");
    titleToKeyMap.put("Bracket Level Match 5", "bracketlevelmatch5");
    titleToKeyMap.put("Bracket Level Match 6", "bracketlevelmatch6");

    // Widget
    titleToKeyMap.put("Text", "text");
    titleToKeyMap.put("Hint", "hint");
    titleToKeyMap.put("Accent", "accent");
    titleToKeyMap.put("Surface", "surface");
    titleToKeyMap.put("Stroke", "stroke");
    titleToKeyMap.put("FAB Background", "fabBackground");
    titleToKeyMap.put("FAB Icon", "fabIcon");
    titleToKeyMap.put("Tab Selected", "tabSelected");
    titleToKeyMap.put("Tab Unselected", "tabUnselected");
    titleToKeyMap.put("Image Tint", "imageTint");
    titleToKeyMap.put("Menu Background", "menubackground");
    titleToKeyMap.put("Menu Text Color", "menutextcolor");
    titleToKeyMap.put("Selected Menu Color", "selectedmenucolor");
    // Background قبلاً اضافه شد
  }

  private String getDefaultColorForTitle(String title) {
    String key = titleToKeyMap.get(title);
    if (key == null) return null;
    try {
      JsonObject defaultObj =
          JsonParser.parseString(new ThemeManager(this).getDefaultThemeJson()).getAsJsonObject();
      JsonObject activity = defaultObj.getAsJsonObject("activity");
      if (activity.has(key)) return activity.get(key).getAsString();
      JsonObject editor = defaultObj.getAsJsonObject("editor");
      if (editor.has(key)) return editor.get(key).getAsString();
      JsonObject widget = defaultObj.getAsJsonObject("widget");
      if (widget.has(key)) return widget.get(key).getAsString();
    } catch (Exception ignored) {
    }
    return null;
  }

  private void resetToDefault() {
    String oldImagePath = currentTheme.getWidget().getImagepath();
    float oldBlurSize = currentTheme.getWidget().getBlursize();

    ThemeManager tmp = new ThemeManager(this);
    String defaultJson = tmp.getDefaultThemeJson();
    currentTheme = gson.fromJson(defaultJson, GhostTheme.class);
    currentTheme.getWidget().setImagepath(oldImagePath);
    currentTheme.getWidget().setBlursize(oldBlurSize);

    saveThemeToFile();
    buildColorItems();
    refreshCurrentTab();
    clearSearch();
    Toast.makeText(this, "Reset to default", Toast.LENGTH_SHORT).show();
  }

  private void filter(String query) {
    currentQuery = query;
    if (query == null || query.trim().isEmpty()) {
      clearSearch();
      return;
    }
    isSearching = true;
    List<ThemeRow> fullList = getCurrentFullList();
    List<ThemeRow> filtered = new ArrayList<>();
    String lowerQuery = query.toLowerCase();
    for (ThemeRow item : fullList) {
      if (item.title.toLowerCase().contains(lowerQuery)) {
        filtered.add(item);
      }
    }
    adapter.updateList(filtered);
    adapter.setHighlightQuery(query);
  }

  private void clearSearch() {
    if (!isSearching && currentQuery.isEmpty()) return;
    isSearching = false;
    currentQuery = "";
    if (searchView != null) {
      searchView.setQuery("", false);
    }
    adapter.updateList(getCurrentFullList());
    adapter.setHighlightQuery(null);
  }

  private List<ThemeRow> getCurrentFullList() {
    int pos = tabLayout.getSelectedTabPosition();
    switch (pos) {
      case 0:
        return activityItems;
      case 1:
        return editorItems;
      case 2:
        return widgetItems;
      default:
        return activityItems;
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.theme_editor_menu, menu);
    MenuItem searchItem = menu.findItem(R.id.action_search);
    searchView = (SearchView) searchItem.getActionView();
    searchView.setQueryHint("Search Color");
    searchView.setOnQueryTextListener(
        new SearchView.OnQueryTextListener() {
          @Override
          public boolean onQueryTextSubmit(String query) {
            filter(query);
            return true;
          }

          @Override
          public boolean onQueryTextChange(String newText) {
            filter(newText);
            return true;
          }
        });
    searchView.setOnCloseListener(
        () -> {
          clearSearch();
          return false;
        });
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.action_reset) {
      resetToDefault();
      return true;
    } else if (item.getItemId() == R.id.action_view) {
      GhostTheme themeCopy =
          new GsonBuilder()
              .create()
              .fromJson(
                  new GsonBuilder().setPrettyPrinting().create().toJson(currentTheme),
                  GhostTheme.class);
      ThemePreviewBottomSheet bottomSheet = ThemePreviewBottomSheet.newInstance(themeCopy);
      bottomSheet.show(getSupportFragmentManager(), "preview_theme");
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  private void buildColorItems() {
    activityItems.clear();
    editorItems.clear();
    widgetItems.clear();

    ActivityTheme a = currentTheme.getActivity();
    activityItems.add(
        new ColorItem("Background", a.getBackground(), (t, c) -> t.getActivity().setBackground(c)));
    activityItems.add(
        new ColorItem("Status Bar", a.getStatusBar(), (t, c) -> t.getActivity().setStatusBar(c)));
    activityItems.add(
        new ColorItem(
            "Navigation Bar", a.getNavigationBar(), (t, c) -> t.getActivity().setNavigationBar(c)));

    EditorTheme e = currentTheme.getEditor();
    editorItems.add(
        new ColorItem("Line Divider", e.getLineDivider(), (t, c) -> t.getEditor().setLineDivider(c)));
    editorItems.add(
        new ColorItem("Text Normal", e.getTextNormal(), (t, c) -> t.getEditor().setTextNormal(c)));
    editorItems.add(
        new ColorItem("Keyword", e.getKeyword(), (t, c) -> t.getEditor().setKeyword(c)));
    editorItems.add(
        new ColorItem("Comment", e.getComment(), (t, c) -> t.getEditor().setComment(c)));
    editorItems.add(
        new ColorItem("Operator", e.getOperator(), (t, c) -> t.getEditor().setOperator(c)));
    editorItems.add(
        new ColorItem("Literal", e.getLiteral(), (t, c) -> t.getEditor().setLiteral(c)));
    editorItems.add(
        new ColorItem(
            "Identifier Var", e.getIdentifierVar(), (t, c) -> t.getEditor().setIdentifierVar(c)));
    editorItems.add(
        new ColorItem(
            "Identifier Name",
            e.getIdentifierName(),
            (t, c) -> t.getEditor().setIdentifierName(c)));
    editorItems.add(
        new ColorItem(
            "Function Name", e.getFunctionName(), (t, c) -> t.getEditor().setFunctionName(c)));
    editorItems.add(
        new ColorItem("Annotation", e.getAnnotation(), (t, c) -> t.getEditor().setAnnotation(c)));
    editorItems.add(
        new ColorItem(
            "Current Line", e.getCurrentLine(), (t, c) -> t.getEditor().setCurrentLine(c)));
    editorItems.add(
        new ColorItem("Line Number", e.getLineNumber(), (t, c) -> t.getEditor().setLineNumber(c)));
    editorItems.add(
        new ColorItem(
            "Line Number Background",
            e.getLineNumberBackground(),
            (t, c) -> t.getEditor().setLineNumberBackground(c)));
    editorItems.add(
        new ColorItem(
            "Selected Text Background",
            e.getSelectedTextBackground(),
            (t, c) -> t.getEditor().setSelectedTextBackground(c)));
    editorItems.add(
        new ColorItem(
            "Selection Insert",
            e.getSelectionInsert(),
            (t, c) -> t.getEditor().setSelectionInsert(c)));
    editorItems.add(
        new ColorItem(
            "Selection Handle",
            e.getSelectionHandle(),
            (t, c) -> t.getEditor().setSelectionHandle(c)));
    editorItems.add(
        new ColorItem("Underline", e.getUnderline(), (t, c) -> t.getEditor().setUnderline(c)));
    editorItems.add(
        new ColorItem(
            "Scroll Bar Thumb",
            e.getScrollBarThumb(),
            (t, c) -> t.getEditor().setScrollBarThumb(c)));
    editorItems.add(
        new ColorItem(
            "Scroll Bar Thumb Pressed",
            e.getScrollBarThumbPressed(),
            (t, c) -> t.getEditor().setScrollBarThumbPressed(c)));
    editorItems.add(
        new ColorItem(
            "Scroll Bar Track",
            e.getScrollBarTrack(),
            (t, c) -> t.getEditor().setScrollBarTrack(c)));
    editorItems.add(
        new ColorItem("Block Line", e.getBlockLine(), (t, c) -> t.getEditor().setBlockLine(c)));
    editorItems.add(
        new ColorItem(
            "Block Line Current",
            e.getBlockLineCurrent(),
            (t, c) -> t.getEditor().setBlockLineCurrent(c)));
    editorItems.add(
        new ColorItem(
            "Line Number Panel",
            e.getLineNumberPanel(),
            (t, c) -> t.getEditor().setLineNumberPanel(c)));
    editorItems.add(
        new ColorItem(
            "Line Number Panel Text",
            e.getLineNumberPanelText(),
            (t, c) -> t.getEditor().setLineNumberPanelText(c)));
    editorItems.add(
        new ColorItem(
            "Completion Wnd Background",
            e.getCompletionWndBackground(),
            (t, c) -> t.getEditor().setCompletionWndBackground(c)));
    editorItems.add(
        new ColorItem(
            "Completion Wnd Corner",
            e.getCompletionWndCorner(),
            (t, c) -> t.getEditor().setCompletionWndCorner(c)));
    editorItems.add(
        new ColorItem(
            "Matched Text Background",
            e.getMatchedTextBackground(),
            (t, c) -> t.getEditor().setMatchedTextBackground(c)));
    editorItems.add(
        new ColorItem(
            "Matched Text Border",
            e.getMatchedTextBorder(),
            (t, c) -> t.getEditor().setMatchedTextBorder(c)));
    editorItems.add(
        new ColorItem(
            "Text Selected", e.getTextSelected(), (t, c) -> t.getEditor().setTextSelected(c)));
    editorItems.add(
        new ColorItem(
            "Non Printable Char",
            e.getNonPrintableChar(),
            (t, c) -> t.getEditor().setNonPrintableChar(c)));
    editorItems.add(
        new ColorItem("HTML Tag", e.getHtmlTag(), (t, c) -> t.getEditor().setHtmlTag(c)));
    editorItems.add(
        new ColorItem(
            "Attribute Name", e.getAttributeName(), (t, c) -> t.getEditor().setAttributeName(c)));
    editorItems.add(
        new ColorItem(
            "Attribute Value",
            e.getAttributeValue(),
            (t, c) -> t.getEditor().setAttributeValue(c)));
    editorItems.add(
        new ColorItem(
            "Problem Error", e.getProblemError(), (t, c) -> t.getEditor().setProblemError(c)));
    editorItems.add(
        new ColorItem(
            "Problem Warning",
            e.getProblemWarning(),
            (t, c) -> t.getEditor().setProblemWarning(c)));
    editorItems.add(
        new ColorItem(
            "Problem Typo", e.getProblemTypo(), (t, c) -> t.getEditor().setProblemTypo(c)));
    editorItems.add(
        new ColorItem(
            "Color Next Dot", e.getColornextdot(), (t, c) -> t.getEditor().setColornextdot(c)));
    editorItems.add(
        new ColorItem(
            "Color Next Brak", e.getColornextbrak(), (t, c) -> t.getEditor().setColornextbrak(c)));
    editorItems.add(
        new ColorItem(
            "Color Next Char", e.getColornextchar(), (t, c) -> t.getEditor().setColornextchar(c)));
    editorItems.add(
        new ColorItem(
            "Color Uppercase",
            e.getColoruppercase(),
            (t, c) -> t.getEditor().setColoruppercase(c)));
    editorItems.add(
        new ColorItem(
            "Color Next Less", e.getColornextless(), (t, c) -> t.getEditor().setColornextless(c)));
    editorItems.add(
        new ColorItem(
            "Line Number Current",
            e.getLineNumberCurrent(),
            (t, c) -> t.getEditor().setLineNumberCurrent(c)));
    editorItems.add(
        new ColorItem(
            "Selected Text Border",
            e.getSelectedTextBorder(),
            (t, c) -> t.getEditor().setSelectedTextBorder(c)));
    editorItems.add(
        new ColorItem(
            "Current Row Border",
            e.getCurrentRowBorder(),
            (t, c) -> t.getEditor().setCurrentRowBorder(c)));
    editorItems.add(
        new ColorItem(
            "Highlighted Delimiters Background",
            e.getHighlightedDelimitersBackground(),
            (t, c) -> t.getEditor().setHighlightedDelimitersBackground(c)));
    editorItems.add(
        new ColorItem(
            "Highlighted Delimiters Underline",
            e.getHighlightedDelimitersUnderline(),
            (t, c) -> t.getEditor().setHighlightedDelimitersUnderline(c)));
    editorItems.add(
        new ColorItem(
            "Highlighted Delimiters Foreground",
            e.getHighlightedDelimitersForeground(),
            (t, c) -> t.getEditor().setHighlightedDelimitersForeground(c)));
    editorItems.add(
        new ColorItem(
            "Highlighted Delimiters Border",
            e.getHighlightedDelimitersBorder(),
            (t, c) -> t.getEditor().setHighlightedDelimitersBorder(c)));
    editorItems.add(
        new ColorItem(
            "Text Highlight Background",
            e.getTextHighlightBackground(),
            (t, c) -> t.getEditor().setTextHighlightBackground(c)));
    editorItems.add(
        new ColorItem(
            "Text Highlight Border",
            e.getTextHighlightBorder(),
            (t, c) -> t.getEditor().setTextHighlightBorder(c)));
    editorItems.add(
        new ColorItem(
            "Text Highlight Strong Background",
            e.getTextHighlightStrongBackground(),
            (t, c) -> t.getEditor().setTextHighlightStrongBackground(c)));
    editorItems.add(
        new ColorItem(
            "Text Highlight Strong Border",
            e.getTextHighlightStrongBorder(),
            (t, c) -> t.getEditor().setTextHighlightStrongBorder(c)));
    editorItems.add(
        new ColorItem(
            "Static Span Background",
            e.getStaticSpanBackground(),
            (t, c) -> t.getEditor().setStaticSpanBackground(c)));
    editorItems.add(
        new ColorItem(
            "Static Span Foreground",
            e.getStaticSpanForeground(),
            (t, c) -> t.getEditor().setStaticSpanForeground(c)));
    editorItems.add(
        new ColorItem(
            "Text Inlay Hint Background",
            e.getTextInlayHintBackground(),
            (t, c) -> t.getEditor().setTextInlayHintBackground(c)));
    editorItems.add(
        new ColorItem(
            "Text Inlay Hint Foreground",
            e.getTextInlayHintForeground(),
            (t, c) -> t.getEditor().setTextInlayHintForeground(c)));
    editorItems.add(
        new ColorItem(
            "Snippet Background Editing",
            e.getSnippetBackgroundEditing(),
            (t, c) -> t.getEditor().setSnippetBackgroundEditing(c)));
    editorItems.add(
        new ColorItem(
            "Snippet Background Related",
            e.getSnippetBackgroundRelated(),
            (t, c) -> t.getEditor().setSnippetBackgroundRelated(c)));
    editorItems.add(
        new ColorItem(
            "Snippet Background Inactive",
            e.getSnippetBackgroundInactive(),
            (t, c) -> t.getEditor().setSnippetBackgroundInactive(c)));
    editorItems.add(
        new ColorItem(
            "Hard Wrap Marker",
            e.getHardWrapMarker(),
            (t, c) -> t.getEditor().setHardWrapMarker(c)));
    editorItems.add(
        new ColorItem(
            "Function Char Background Stroke",
            e.getFunctionCharBackgroundStroke(),
            (t, c) -> t.getEditor().setFunctionCharBackgroundStroke(c)));
    editorItems.add(
        new ColorItem(
            "Diagnostic Tooltip Background",
            e.getDiagnosticTooltipBackground(),
            (t, c) -> t.getEditor().setDiagnosticTooltipBackground(c)));
    editorItems.add(
        new ColorItem(
            "Diagnostic Tooltip Brief Msg",
            e.getDiagnosticTooltipBriefMsg(),
            (t, c) -> t.getEditor().setDiagnosticTooltipBriefMsg(c)));
    editorItems.add(
        new ColorItem(
            "Diagnostic Tooltip Detailed Msg",
            e.getDiagnosticTooltipDetailedMsg(),
            (t, c) -> t.getEditor().setDiagnosticTooltipDetailedMsg(c)));
    editorItems.add(
        new ColorItem(
            "Diagnostic Tooltip Action",
            e.getDiagnosticTooltipAction(),
            (t, c) -> t.getEditor().setDiagnosticTooltipAction(c)));
    editorItems.add(
        new ColorItem(
            "Sticky Scroll Divider",
            e.getStickyScrollDivider(),
            (t, c) -> t.getEditor().setStickyScrollDivider(c)));
    editorItems.add(
        new ColorItem(
            "Strike Through", e.getStrikeThrough(), (t, c) -> t.getEditor().setStrikeThrough(c)));
    editorItems.add(
        new ColorItem(
            "Side Block Line", e.getSideBlockLine(), (t, c) -> t.getEditor().setSideBlockLine(c)));
    editorItems.add(
        new ColorItem(
            "Completion Wnd Text Primary",
            e.getCompletionWndTextPrimary(),
            (t, c) -> t.getEditor().setCompletionWndTextPrimary(c)));
    editorItems.add(
        new ColorItem(
            "Completion Wnd Text Secondary",
            e.getCompletionWndTextSecondary(),
            (t, c) -> t.getEditor().setCompletionWndTextSecondary(c)));
    editorItems.add(
        new ColorItem(
            "Completion Wnd Item Current",
            e.getCompletionWndItemCurrent(),
            (t, c) -> t.getEditor().setCompletionWndItemCurrent(c)));
    editorItems.add(
        new ColorItem(
            "Completion Wnd Text Matched",
            e.getCompletionWndTextMatched(),
            (t, c) -> t.getEditor().setCompletionWndTextMatched(c)));
    editorItems.add(
        new ColorItem(
            "Signature Background",
            e.getSignatureBackground(),
            (t, c) -> t.getEditor().setSignatureBackground(c)));
    editorItems.add(
        new ColorItem(
            "Signature Border",
            e.getSignatureBorder(),
            (t, c) -> t.getEditor().setSignatureBorder(c)));
    editorItems.add(
        new ColorItem(
            "Signature Text Normal",
            e.getSignatureTextNormal(),
            (t, c) -> t.getEditor().setSignatureTextNormal(c)));
    editorItems.add(
        new ColorItem(
            "Signature Text Highlighted Parameter",
            e.getSignatureTextHighlightedParameter(),
            (t, c) -> t.getEditor().setSignatureTextHighlightedParameter(c)));
    editorItems.add(
        new ColorItem(
            "Hover Background",
            e.getHoverBackground(),
            (t, c) -> t.getEditor().setHoverBackground(c)));
    editorItems.add(
        new ColorItem(
            "Hover Border", e.getHoverBorder(), (t, c) -> t.getEditor().setHoverBorder(c)));
    editorItems.add(
        new ColorItem(
            "Hover Text Normal",
            e.getHoverTextNormal(),
            (t, c) -> t.getEditor().setHoverTextNormal(c)));
    editorItems.add(
        new ColorItem(
            "Hover Text Highlighted",
            e.getHoverTextHighlighted(),
            (t, c) -> t.getEditor().setHoverTextHighlighted(c)));
    editorItems.add(
        new ColorItem(
            "Text Action Window Background",
            e.getTextActionWindowBackground(),
            (t, c) -> t.getEditor().setTextActionWindowBackground(c)));
    editorItems.add(
        new ColorItem(
            "Text Action Window Icon Color",
            e.getTextActionWindowIconColor(),
            (t, c) -> t.getEditor().setTextActionWindowIconColor(c)));
    editorItems.add(
        new ColorItem(
            "Minimap Background",
            e.getMinimapBackground(),
            (t, c) -> t.getEditor().setMinimapBackground(c)));
    editorItems.add(
        new ColorItem(
            "Minimap Viewport",
            e.getMinimapViewport(),
            (t, c) -> t.getEditor().setMinimapViewport(c)));
    editorItems.add(
        new ColorItem(
            "Minimap Viewport Border",
            e.getMinimapViewportBorder(),
            (t, c) -> t.getEditor().setMinimapViewportBorder(c)));
    editorItems.add(
        new ColorItem(
            "Bracket Level Match 1",
            e.getBracketlevelmatch1(),
            (t, c) -> t.getEditor().setBracketlevelmatch1(c)));
    editorItems.add(
        new ColorItem(
            "Bracket Level Match 2",
            e.getBracketlevelmatch2(),
            (t, c) -> t.getEditor().setBracketlevelmatch2(c)));
    editorItems.add(
        new ColorItem(
            "Bracket Level Match 3",
            e.getBracketlevelmatch3(),
            (t, c) -> t.getEditor().setBracketlevelmatch3(c)));
    editorItems.add(
        new ColorItem(
            "Bracket Level Match 4",
            e.getBracketlevelmatch4(),
            (t, c) -> t.getEditor().setBracketlevelmatch4(c)));
    editorItems.add(
        new ColorItem(
            "Bracket Level Match 5",
            e.getBracketlevelmatch5(),
            (t, c) -> t.getEditor().setBracketlevelmatch5(c)));
    editorItems.add(
        new ColorItem(
            "Bracket Level Match 6",
            e.getBracketlevelmatch6(),
            (t, c) -> t.getEditor().setBracketlevelmatch6(c)));

    WidgetTheme w = currentTheme.getWidget();
    widgetItems.add(
        new ColorItem("Background", w.getBackground(), (t, c) -> t.getWidget().setBackground(c)));
    widgetItems.add(new ColorItem("Text", w.getText(), (t, c) -> t.getWidget().setText(c)));
    widgetItems.add(new ColorItem("Hint", w.getHint(), (t, c) -> t.getWidget().setHint(c)));
    widgetItems.add(new ColorItem("Accent", w.getAccent(), (t, c) -> t.getWidget().setAccent(c)));
    widgetItems.add(
        new ColorItem("Surface", w.getSurface(), (t, c) -> t.getWidget().setSurface(c)));
    widgetItems.add(new ColorItem("Stroke", w.getStroke(), (t, c) -> t.getWidget().setStroke(c)));
    widgetItems.add(
        new ColorItem(
            "FAB Background", w.getFabBackground(), (t, c) -> t.getWidget().setFabBackground(c)));
    widgetItems.add(
        new ColorItem("FAB Icon", w.getFabIcon(), (t, c) -> t.getWidget().setFabIcon(c)));
    widgetItems.add(
        new ColorItem(
            "Tab Selected", w.getTabSelected(), (t, c) -> t.getWidget().setTabSelected(c)));
    widgetItems.add(
        new ColorItem(
            "Tab Unselected", w.getTabUnselected(), (t, c) -> t.getWidget().setTabUnselected(c)));
    widgetItems.add(
        new ColorItem("Image Tint", w.getImageTint(), (t, c) -> t.getWidget().setImageTint(c)));
    widgetItems.add(
        new ColorItem(
            "Menu Background",
            w.getMenubackground(),
            (t, c) -> t.getWidget().setMenubackground(c)));
    widgetItems.add(
        new ColorItem(
            "Menu Text Color", w.getMenutextcolor(), (t, c) -> t.getWidget().setMenutextcolor(c)));
    widgetItems.add(
        new ColorItem(
            "Selected Menu Color",
            w.getSelectedmenucolor(),
            (t, c) -> t.getWidget().setSelectedmenucolor(c)));

    widgetItems.add(
        new ImageItem(
            "Background Image",
            w.getImagepath() == null ? "" : w.getImagepath(),
            (t, p) -> t.getWidget().setImagepath(p)));
    widgetItems.add(
        new SliderItem(
            "Blur Size", w.getBlursize(), 0f, 25f, 1f, (t, v) -> t.getWidget().setBlursize(v)));
  }

  private void saveThemeToFile() {
    if (currentTheme.getWidget().getImagepath() == null) {
      currentTheme.getWidget().setImagepath("");
    }
    String json = gson.toJson(currentTheme);
    FileIOUtils.writeFileFromString(currentThemePath, json);
  }

  private void refreshCurrentTab() {
    if (isSearching && !currentQuery.isEmpty()) {
      filter(currentQuery);
    } else {
      int pos = tabLayout.getSelectedTabPosition();
      if (pos == 0) adapter.updateList(activityItems);
      else if (pos == 1) adapter.updateList(editorItems);
      else adapter.updateList(widgetItems);
    }
    adapter.setHighlightQuery(null);
  }

  private class ThemeDetailAdapter extends RecyclerView.Adapter<RootHolder> {
    static final int TYPE_COLOR = 0;
    static final int TYPE_IMAGE = 1;
    static final int TYPE_SLIDER = 2;

    private List<ThemeRow> items;
    private String highlightQuery = null;

    ThemeDetailAdapter(List<ThemeRow> items) {
      this.items = items;
    }

    void updateList(List<ThemeRow> newItems) {
      this.items = newItems;
      notifyDataSetChanged();
    }

    void setHighlightQuery(String query) {
      this.highlightQuery = query;
      notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
      ThemeRow item = items.get(position);
      if (item instanceof ImageItem) return TYPE_IMAGE;
      if (item instanceof SliderItem) return TYPE_SLIDER;
      return TYPE_COLOR;
    }

    @NonNull
    @Override
    public RootHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
      LayoutInflater inflater = LayoutInflater.from(parent.getContext());
      if (viewType == TYPE_IMAGE) {
        return new ImageViewHolder(inflater.inflate(R.layout.item_image_row, parent, false));
      }
      if (viewType == TYPE_SLIDER) {
        return new SliderViewHolder(inflater.inflate(R.layout.item_slider_row, parent, false));
      }
      return new ColorViewHolder(inflater.inflate(R.layout.item_color_row, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RootHolder holder, int position) {
      ThemeRow item = items.get(position);
      if (holder instanceof ColorViewHolder) {
        bindColor((ColorViewHolder) holder, (ColorItem) item);
      } else if (holder instanceof ImageViewHolder) {
        bindImage((ImageViewHolder) holder, (ImageItem) item);
      } else if (holder instanceof SliderViewHolder) {
        bindSlider((SliderViewHolder) holder, (SliderItem) item);
      }
    }

    @Override
    public void onViewRecycled(@NonNull RootHolder holder) {
      super.onViewRecycled(holder);
      if (holder instanceof ImageViewHolder) {
        ((ImageViewHolder) holder).mediaPreview.clear();
      }
    }

    private void bindTitle(TextView titleView, String title) {
      if (highlightQuery != null && !highlightQuery.isEmpty()) {
        SpannableString spannable = new SpannableString(title);
        String lowerTitle = title.toLowerCase();
        String lowerQuery = highlightQuery.toLowerCase();
        int start = lowerTitle.indexOf(lowerQuery);
        if (start >= 0) {
          spannable.setSpan(
              new BackgroundColorSpan(Color.YELLOW),
              start,
              start + highlightQuery.length(),
              Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        titleView.setText(spannable);
      } else {
        titleView.setText(title);
      }
    }

    private void bindColor(ColorViewHolder holder, ColorItem item) {
      bindTitle(holder.title, item.title);
      String colorToShow = item.currentColor;
      if (colorToShow == null || colorToShow.isEmpty()) {
        String def = getDefaultColorForTitle(item.title);
        if (def != null) colorToShow = def;
      }
      if (colorToShow == null || colorToShow.isEmpty()) {
        colorToShow = "#000000";
      }
      final String initialHex = colorToShow;
      try {
        shape(holder.colorPreview, Color.parseColor(initialHex));
      } catch (Exception e) {
        holder.colorPreview.setBackgroundColor(Color.BLACK);
      }
      holder.editIcon.setOnClickListener(
          v -> {
            int initialColor;
            try {
              initialColor = Color.parseColor(initialHex);
            } catch (Exception e) {
              initialColor = Color.BLACK;
            }
            ColorPickerBottomSheetDialog.show(
                ThemeEditorActivity.this,
                initialColor,
                newColor -> {
                  String newHex = String.format("#%08X", newColor);
                  item.updater.update(currentTheme, newHex);
                  item.currentColor = newHex;
                  saveThemeToFile();
                  notifyItemChanged(holder.getBindingAdapterPosition());
                });
          });
    }

    private void bindImage(ImageViewHolder holder, ImageItem item) {
      bindTitle(holder.title, item.title);
      boolean hasImage = item.currentPath != null && !item.currentPath.isEmpty();
      holder.clearIcon.setVisibility(hasImage ? View.VISIBLE : View.GONE);
      // ViewChilder previews every supported media type (image/gif/video/html),
      // not only static images.
      if (hasImage) {
        holder.mediaPreview.setVisibility(View.VISIBLE);
        holder.mediaPreview.load(item.currentPath);
      } else {
        holder.mediaPreview.clear();
        holder.mediaPreview.setVisibility(View.GONE);
      }
      holder.browseIcon.setOnClickListener(
          v -> {
            pendingImageItem = item;
            pickImageLauncher.launch(new String[] {"image/*", "video/*", "text/html"});
          });
      holder.clearIcon.setOnClickListener(
          v -> {
            item.updater.update(currentTheme, "");
            item.currentPath = "";
            saveThemeToFile();
            notifyItemChanged(holder.getBindingAdapterPosition());
          });
    }

    private void bindSlider(SliderViewHolder holder, SliderItem item) {
      bindTitle(holder.title, item.title);
      holder.valueText.setText(String.valueOf((int) item.currentValue));
      holder.editIcon.setOnClickListener(
          v -> {
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_slider, null);
            Slider slider = dialogView.findViewById(R.id.slider);
            TextView valueText = dialogView.findViewById(R.id.slider_value);
            slider.setValueFrom(item.minValue);
            slider.setValueTo(item.maxValue);
            slider.setStepSize(item.step);
            slider.setValue(item.currentValue);
            valueText.setText(String.valueOf((int) slider.getValue()));
            slider.addOnChangeListener(
                (s, val, fromUser) -> valueText.setText(String.valueOf((int) val)));
            new MaterialAlertDialogBuilder(ThemeEditorActivity.this)
                .setTitle(item.title)
                .setView(dialogView)
                .setPositiveButton(
                    R.string.ok,
                    (d, w) -> {
                      item.updater.update(currentTheme, slider.getValue());
                      item.currentValue = slider.getValue();
                      saveThemeToFile();
                      notifyItemChanged(holder.getBindingAdapterPosition());
                    })
                .setNegativeButton(R.string.cancel, null)
                .show();
          });
    }

    void shape(View v, int color) {
      var gd = new GradientDrawable();
      gd.setStroke(1, Color.WHITE);
      gd.setCornerRadius(20f);
      gd.setColor(color);
      v.setBackground(gd);
    }

    @Override
    public int getItemCount() {
      return items.size();
    }

    class ColorViewHolder extends RootHolder {
      TextView title;
      View colorPreview;
      ImageView editIcon;

      ColorViewHolder(@NonNull View itemView) {
        super(itemView);
        title = itemView.findViewById(R.id.title);
        colorPreview = itemView.findViewById(R.id.colorPreview);
        editIcon = itemView.findViewById(R.id.editIcon);
      }
    }

    class ImageViewHolder extends RootHolder {
      TextView title;
      ViewChilder mediaPreview;
      ImageView clearIcon;
      ImageView browseIcon;

      ImageViewHolder(@NonNull View itemView) {
        super(itemView);
        title = itemView.findViewById(R.id.title);
        mediaPreview = itemView.findViewById(R.id.mediaPreview);
        clearIcon = itemView.findViewById(R.id.clearIcon);
        browseIcon = itemView.findViewById(R.id.browseIcon);
      }
    }

    class SliderViewHolder extends RootHolder {
      TextView title;
      TextView valueText;
      ImageView editIcon;

      SliderViewHolder(@NonNull View itemView) {
        super(itemView);
        title = itemView.findViewById(R.id.title);
        valueText = itemView.findViewById(R.id.valueText);
        editIcon = itemView.findViewById(R.id.editIcon);
      }
    }
  }

  private abstract static class ThemeRow {
    String title;

    ThemeRow(String title) {
      this.title = title;
    }
  }

  private static class ColorItem extends ThemeRow {
    String currentColor;
    ColorUpdater updater;

    ColorItem(String title, String currentColor, ColorUpdater updater) {
      super(title);
      this.currentColor = currentColor;
      this.updater = updater;
    }
  }

  private static class ImageItem extends ThemeRow {
    String currentPath;
    ImageUpdater updater;

    ImageItem(String title, String currentPath, ImageUpdater updater) {
      super(title);
      this.currentPath = currentPath;
      this.updater = updater;
    }
  }

  private static class SliderItem extends ThemeRow {
    float currentValue;
    float minValue;
    float maxValue;
    float step;
    SliderUpdater updater;

    SliderItem(
        String title,
        float currentValue,
        float minValue,
        float maxValue,
        float step,
        SliderUpdater updater) {
      super(title);
      this.currentValue = currentValue;
      this.minValue = minValue;
      this.maxValue = maxValue;
      this.step = step;
      this.updater = updater;
    }
  }

  private interface ColorUpdater {
    void update(GhostTheme theme, String newColor);
  }

  private interface ImageUpdater {
    void update(GhostTheme theme, String newPath);
  }

  private interface SliderUpdater {
    void update(GhostTheme theme, float newValue);
  }
}
