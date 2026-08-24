package ir.theme.themeeditor;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.gson.Gson;

import io.github.rosemoe.sora.widget.schemes.EditorColorScheme;
import ir.hanzodev1375.ghostide.R;
import ir.hanzodev1375.ghostide.codeeditors.IdeEditor;
import ir.hanzodev1375.ghostide.codeeditors.colorscheme.GhostColorScheme;
import ir.hanzodev1375.ghostide.codeeditors.langs.cpp.CppLanguage;
import ir.hanzodev1375.ghostide.codeeditors.langs.html.HtmlLanguage;
import ir.hanzodev1375.ghostide.codeeditors.langs.java.JavaLanguage;
import ir.hanzodev1375.ghostide.codeeditors.langs.js.JsLanguage;
import ir.hanzodev1375.components.childern.ViewChilder;
import ir.theme.EditorTheme;
import ir.theme.GhostTheme;

public class ThemePreviewBottomSheet extends BottomSheetDialogFragment {

  private static final String ARG_THEME_JSON = "theme_json";

  private IdeEditor editorPreview;
  private TabLayout tabLayout;
  private ViewChilder backgroundMedia;
  private GhostTheme currentTheme;
  private FloatingActionButton fabClose;

  public static ThemePreviewBottomSheet newInstance(GhostTheme theme) {
    ThemePreviewBottomSheet fragment = new ThemePreviewBottomSheet();
    Bundle args = new Bundle();
    args.putString(ARG_THEME_JSON, new Gson().toJson(theme));
    fragment.setArguments(args);
    return fragment;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getArguments() != null) {
      String json = getArguments().getString(ARG_THEME_JSON);
      currentTheme = new Gson().fromJson(json, GhostTheme.class);
    }
    if (currentTheme == null) {
      dismiss();
    }
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.bottom_sheet_preview_theme, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    editorPreview = view.findViewById(R.id.editorPreview);
    tabLayout = view.findViewById(R.id.tabLayout);
    backgroundMedia = view.findViewById(R.id.ivBackgroundImage);
    fabClose = view.findViewById(R.id.fabClose);

    applyThemeToEditor();
    applyBackgroundImage();
    setupTabs();

    fabClose.setOnClickListener(v -> dismiss());
  }

  @Override
  public void onDestroyView() {
    // Release the media child (video player / webview) when the sheet goes away.
    if (backgroundMedia != null) {
      backgroundMedia.clear();
    }
    super.onDestroyView();
  }

  private void applyThemeToEditor() {
    if (currentTheme == null || currentTheme.getEditor() == null) return;

    EditorTheme t = currentTheme.getEditor();
    var scheme = editorPreview.getColorScheme();
    var widget = currentTheme.getWidget();
    backgroundMedia.setBackgroundColor(parseColor(widget.getBackground()));
    if (widget.getBackground() != null) {
      tabLayout.setBackgroundColor(parseColor(widget.getBackground()));
    }

    if (widget.getAccent() != null) {
      tabLayout.setSelectedTabIndicatorColor(parseColor(widget.getAccent()));
    }

    if (widget.getTabSelected() != null && widget.getTabUnselected() != null) {

      tabLayout.setTabTextColors(
          parseColor(widget.getTabUnselected()), parseColor(widget.getTabSelected()));
    }

    if (widget.getFabBackground() != null) {
      fabClose.setBackgroundTintList(ColorStateList.valueOf(parseColor(widget.getFabBackground())));
    }

    if (widget.getFabIcon() != null) {

      fabClose.setColorFilter(parseColor(widget.getFabIcon()));
    }
    scheme.setColor(GhostColorScheme.LINE_DIVIDER, parseColor(t.getLineDivider()));
    scheme.setColor(GhostColorScheme.LINE_NUMBER, parseColor(t.getLineNumber()));
    scheme.setColor(
        GhostColorScheme.LINE_NUMBER_BACKGROUND, parseColor(t.getLineNumberBackground()));

    //  scheme.setColor(GhostColorScheme.WHOLE_BACKGROUND, parseColor(t.getWholeBackground()));
    editorPreview.getColorScheme().setColor(EditorColorScheme.WHOLE_BACKGROUND, 0);
    scheme.setColor(GhostColorScheme.TEXT_NORMAL, parseColor(t.getTextNormal()));
    scheme.setColor(
        GhostColorScheme.SELECTED_TEXT_BACKGROUND, parseColor(t.getSelectedTextBackground()));
    scheme.setColor(GhostColorScheme.SELECTION_INSERT, parseColor(t.getSelectionInsert()));
    scheme.setColor(GhostColorScheme.SELECTION_HANDLE, parseColor(t.getSelectionHandle()));
    scheme.setColor(GhostColorScheme.CURRENT_LINE, parseColor(t.getCurrentLine()));
    scheme.setColor(GhostColorScheme.UNDERLINE, parseColor(t.getUnderline()));
    scheme.setColor(GhostColorScheme.SCROLL_BAR_THUMB, parseColor(t.getScrollBarThumb()));
    scheme.setColor(
        GhostColorScheme.SCROLL_BAR_THUMB_PRESSED, parseColor(t.getScrollBarThumbPressed()));
    scheme.setColor(GhostColorScheme.SCROLL_BAR_TRACK, parseColor(t.getScrollBarTrack()));
    scheme.setColor(GhostColorScheme.BLOCK_LINE, parseColor(t.getBlockLine()));
    scheme.setColor(GhostColorScheme.BLOCK_LINE_CURRENT, parseColor(t.getBlockLineCurrent()));
    scheme.setColor(GhostColorScheme.LINE_NUMBER_PANEL, parseColor(t.getLineNumberPanel()));
    scheme.setColor(
        GhostColorScheme.LINE_NUMBER_PANEL_TEXT, parseColor(t.getLineNumberPanelText()));
    scheme.setColor(
        GhostColorScheme.COMPLETION_WND_BACKGROUND, parseColor(t.getCompletionWndBackground()));
    scheme.setColor(GhostColorScheme.COMPLETION_WND_CORNER, parseColor(t.getCompletionWndCorner()));
    scheme.setColor(GhostColorScheme.KEYWORD, parseColor(t.getKeyword()));
    scheme.setColor(GhostColorScheme.COMMENT, parseColor(t.getComment()));
    scheme.setColor(GhostColorScheme.OPERATOR, parseColor(t.getOperator()));
    scheme.setColor(GhostColorScheme.LITERAL, parseColor(t.getLiteral()));
    scheme.setColor(GhostColorScheme.IDENTIFIER_VAR, parseColor(t.getIdentifierVar()));
    scheme.setColor(GhostColorScheme.IDENTIFIER_NAME, parseColor(t.getIdentifierName()));
    scheme.setColor(GhostColorScheme.FUNCTION_NAME, parseColor(t.getFunctionName()));
    scheme.setColor(GhostColorScheme.ANNOTATION, parseColor(t.getAnnotation()));
    scheme.setColor(
        GhostColorScheme.MATCHED_TEXT_BACKGROUND, parseColor(t.getMatchedTextBackground()));
    scheme.setColor(GhostColorScheme.MATCHED_TEXT_BORDER, parseColor(t.getMatchedTextBorder()));
    scheme.setColor(GhostColorScheme.TEXT_SELECTED, parseColor(t.getTextSelected()));
    scheme.setColor(GhostColorScheme.NON_PRINTABLE_CHAR, parseColor(t.getNonPrintableChar()));
    scheme.setColor(GhostColorScheme.HTML_TAG, parseColor(t.getHtmlTag()));
    scheme.setColor(GhostColorScheme.ATTRIBUTE_NAME, parseColor(t.getAttributeName()));
    scheme.setColor(GhostColorScheme.ATTRIBUTE_VALUE, parseColor(t.getAttributeValue()));
    scheme.setColor(GhostColorScheme.PROBLEM_ERROR, parseColor(t.getProblemError()));
    scheme.setColor(GhostColorScheme.PROBLEM_WARNING, parseColor(t.getProblemWarning()));
    scheme.setColor(GhostColorScheme.PROBLEM_TYPO, parseColor(t.getProblemTypo()));
    scheme.setColor(GhostColorScheme.COLORNEXTDOT, parseColor(t.getColornextdot()));
    scheme.setColor(GhostColorScheme.COLORNEXTBRAK, parseColor(t.getColornextbrak()));
    scheme.setColor(GhostColorScheme.COLORNEXTCHAR, parseColor(t.getColornextchar()));
    scheme.setColor(GhostColorScheme.COLORUPPERCASE, parseColor(t.getColoruppercase()));
    scheme.setColor(GhostColorScheme.COLORNEXTLESS, parseColor(t.getColornextless()));

    // رنگ‌های جدید
    scheme.setColor(GhostColorScheme.LINE_NUMBER_CURRENT, parseColor(t.getLineNumberCurrent()));
    scheme.setColor(GhostColorScheme.SELECTED_TEXT_BORDER, parseColor(t.getSelectedTextBorder()));
    scheme.setColor(GhostColorScheme.CURRENT_ROW_BORDER, parseColor(t.getCurrentRowBorder()));
    scheme.setColor(
        GhostColorScheme.HIGHLIGHTED_DELIMITERS_BACKGROUND,
        parseColor(t.getHighlightedDelimitersBackground()));
    scheme.setColor(
        GhostColorScheme.HIGHLIGHTED_DELIMITERS_UNDERLINE,
        parseColor(t.getHighlightedDelimitersUnderline()));
    scheme.setColor(
        GhostColorScheme.HIGHLIGHTED_DELIMITERS_FOREGROUND,
        parseColor(t.getHighlightedDelimitersForeground()));
    scheme.setColor(
        GhostColorScheme.HIGHLIGHTED_DELIMITERS_BORDER,
        parseColor(t.getHighlightedDelimitersBorder()));
    scheme.setColor(
        GhostColorScheme.TEXT_HIGHLIGHT_BACKGROUND, parseColor(t.getTextHighlightBackground()));
    scheme.setColor(GhostColorScheme.TEXT_HIGHLIGHT_BORDER, parseColor(t.getTextHighlightBorder()));
    scheme.setColor(
        GhostColorScheme.TEXT_HIGHLIGHT_STRONG_BACKGROUND,
        parseColor(t.getTextHighlightStrongBackground()));
    scheme.setColor(
        GhostColorScheme.TEXT_HIGHLIGHT_STRONG_BORDER,
        parseColor(t.getTextHighlightStrongBorder()));
    scheme.setColor(
        GhostColorScheme.STATIC_SPAN_BACKGROUND, parseColor(t.getStaticSpanBackground()));
    scheme.setColor(
        GhostColorScheme.STATIC_SPAN_FOREGROUND, parseColor(t.getStaticSpanForeground()));
    scheme.setColor(
        GhostColorScheme.TEXT_INLAY_HINT_BACKGROUND, parseColor(t.getTextInlayHintBackground()));
    scheme.setColor(
        GhostColorScheme.TEXT_INLAY_HINT_FOREGROUND, parseColor(t.getTextInlayHintForeground()));
    scheme.setColor(
        GhostColorScheme.SNIPPET_BACKGROUND_EDITING, parseColor(t.getSnippetBackgroundEditing()));
    scheme.setColor(
        GhostColorScheme.SNIPPET_BACKGROUND_RELATED, parseColor(t.getSnippetBackgroundRelated()));
    scheme.setColor(
        GhostColorScheme.SNIPPET_BACKGROUND_INACTIVE, parseColor(t.getSnippetBackgroundInactive()));
    scheme.setColor(GhostColorScheme.HARD_WRAP_MARKER, parseColor(t.getHardWrapMarker()));
    scheme.setColor(
        GhostColorScheme.FUNCTION_CHAR_BACKGROUND_STROKE,
        parseColor(t.getFunctionCharBackgroundStroke()));
    scheme.setColor(
        GhostColorScheme.DIAGNOSTIC_TOOLTIP_BACKGROUND,
        parseColor(t.getDiagnosticTooltipBackground()));
    scheme.setColor(
        GhostColorScheme.DIAGNOSTIC_TOOLTIP_BRIEF_MSG,
        parseColor(t.getDiagnosticTooltipBriefMsg()));
    scheme.setColor(
        GhostColorScheme.DIAGNOSTIC_TOOLTIP_DETAILED_MSG,
        parseColor(t.getDiagnosticTooltipDetailedMsg()));
    scheme.setColor(
        GhostColorScheme.DIAGNOSTIC_TOOLTIP_ACTION, parseColor(t.getDiagnosticTooltipAction()));
    scheme.setColor(GhostColorScheme.STICKY_SCROLL_DIVIDER, parseColor(t.getStickyScrollDivider()));
    scheme.setColor(GhostColorScheme.STRIKETHROUGH, parseColor(t.getStrikeThrough()));
    scheme.setColor(GhostColorScheme.SIDE_BLOCK_LINE, parseColor(t.getSideBlockLine()));
    scheme.setColor(
        GhostColorScheme.COMPLETION_WND_TEXT_PRIMARY, parseColor(t.getCompletionWndTextPrimary()));
    scheme.setColor(
        GhostColorScheme.COMPLETION_WND_TEXT_SECONDARY,
        parseColor(t.getCompletionWndTextSecondary()));
    scheme.setColor(
        GhostColorScheme.COMPLETION_WND_ITEM_CURRENT, parseColor(t.getCompletionWndItemCurrent()));
    scheme.setColor(
        GhostColorScheme.COMPLETION_WND_TEXT_MATCHED, parseColor(t.getCompletionWndTextMatched()));
    scheme.setColor(GhostColorScheme.SIGNATURE_BACKGROUND, parseColor(t.getSignatureBackground()));
    scheme.setColor(GhostColorScheme.SIGNATURE_BORDER, parseColor(t.getSignatureBorder()));
    scheme.setColor(GhostColorScheme.SIGNATURE_TEXT_NORMAL, parseColor(t.getSignatureTextNormal()));
    scheme.setColor(
        GhostColorScheme.SIGNATURE_TEXT_HIGHLIGHTED_PARAMETER,
        parseColor(t.getSignatureTextHighlightedParameter()));
    scheme.setColor(GhostColorScheme.HOVER_BACKGROUND, parseColor(t.getHoverBackground()));
    scheme.setColor(GhostColorScheme.HOVER_BORDER, parseColor(t.getHoverBorder()));
    scheme.setColor(GhostColorScheme.HOVER_TEXT_NORMAL, parseColor(t.getHoverTextNormal()));
    scheme.setColor(
        GhostColorScheme.HOVER_TEXT_HIGHLIGHTED, parseColor(t.getHoverTextHighlighted()));
    scheme.setColor(
        GhostColorScheme.TEXT_ACTION_WINDOW_BACKGROUND,
        parseColor(t.getTextActionWindowBackground()));
    scheme.setColor(
        GhostColorScheme.TEXT_ACTION_WINDOW_ICON_COLOR,
        parseColor(t.getTextActionWindowIconColor()));
    scheme.setColor(GhostColorScheme.MINIMAP_BACKGROUND, parseColor(t.getMinimapBackground()));
    scheme.setColor(GhostColorScheme.MINIMAP_VIEWPORT, parseColor(t.getMinimapViewport()));
    scheme.setColor(
        GhostColorScheme.MINIMAP_VIEWPORT_BORDER, parseColor(t.getMinimapViewportBorder()));


  }

  private void applyBackgroundImage() {
    String imagePath =
        (currentTheme.getWidget() != null) ? currentTheme.getWidget().getImagepath() : null;
    getView().setBackgroundColor(Color.parseColor(currentTheme.getActivity().getBackground()));
    if (imagePath != null && !imagePath.isEmpty()) {
      backgroundMedia.setVisibility(View.VISIBLE);
      backgroundMedia.load(imagePath, currentTheme.getWidget().getBlursize());
    } else {
      backgroundMedia.clear();
      backgroundMedia.setVisibility(View.GONE);
    }
  }

  private void setupTabs() {
    tabLayout.addTab(tabLayout.newTab().setText("item.java"));
    tabLayout.addTab(tabLayout.newTab().setText("index.html"));
    tabLayout.addTab(tabLayout.newTab().setText("test.js"));
    tabLayout.addTab(tabLayout.newTab().setText("model.cpp"));

    tabLayout.addOnTabSelectedListener(
        new TabLayout.OnTabSelectedListener() {
          @Override
          public void onTabSelected(TabLayout.Tab tab) {
            setSampleCodeForTab(tab.getPosition());
          }

          @Override
          public void onTabUnselected(TabLayout.Tab tab) {}

          @Override
          public void onTabReselected(TabLayout.Tab tab) {}
        });

    setSampleCodeForTab(0);
  }

  private void setSampleCodeForTab(int position) {
    String code;
    switch (position) {
      case 0:
        code =
            "public class Item {\n"
                + "    private String name;\n"
                + "    private int value;\n"
                + "\n"
                + "    public Item(String name, int value) {\n"
                + "        this.name = name;\n"
                + "        this.value = value;\n"
                + "    }\n"
                + "\n"
                + "    public void print() {\n"
                + "        System.out.println(name + \": \" + value);\n"
                + "    }\n"
                + "}";
        editorPreview.setEditorLanguage(new JavaLanguage(getContext()));
        break;
      case 1:
        code =
            "<!DOCTYPE html>\n"
                + "<html>\n"
                + "<head>\n"
                + "    <title>Sample Page</title>\n"
                + "</head>\n"
                + "<body>\n"
                + "    <h1>Hello, World!</h1>\n"
                + "    <p>This is a sample HTML file.</p>\n"
                + "</body>\n"
                + "</html>";
        editorPreview.setEditorLanguage(new HtmlLanguage(getContext(), ""));
        break;
      case 2:
        code =
            "function greet(name) {\n"
                + "    return `Hello, ${name}!`;\n"
                + "}\n"
                + "\n"
                + "const result = greet('User');\n"
                + "console.log(result);";
        editorPreview.setEditorLanguage(new JsLanguage(getContext(), ""));
        break;
      case 3:
        code =
            "#include <iostream>\n"
                + "using namespace std;\n"
                + "\n"
                + "class Model {\n"
                + "public:\n"
                + "    void display() {\n"
                + "        cout << \"Model C++ class\" << endl;\n"
                + "    }\n"
                + "};\n"
                + "\n"
                + "int main() {\n"
                + "    Model m;\n"
                + "    m.display();\n"
                + "    return 0;\n"
                + "}";
        editorPreview.setEditorLanguage(new CppLanguage(getContext()));
        break;
      default:
        return;
    }
    editorPreview.setText(code);
  }

  private int parseColor(String color) {
    try {
      return Color.parseColor(color);
    } catch (Exception e) {
      return Color.TRANSPARENT;
    }
  }
}
