package ir.theme;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.sidesheet.SideSheetDialog;
import com.google.android.material.tabs.TabLayout;
import com.skydoves.powermenu.PowerMenu;
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme;
import ir.hanzodev1375.components.WebViewBottomSheetFragment;
import ir.hanzodev1375.filetreelib.widget.FileTreeView;
import ir.hanzodev1375.ghostide.codeeditors.IdeEditor;
import ir.hanzodev1375.ghostide.codeeditors.colorscheme.GhostColorScheme;
import ir.hanzodev1375.ghostide.customui.EditorStatusBar;
import ir.hanzodev1375.ghostide.customui.GhostIdeEditorSearch;
import ir.hanzodev1375.ghostide.customui.LayoutSymbolbar;
import ir.hanzodev1375.ghostide.utils.BlurTransformation;

public class ThemeUtils {

  private final ThemeManager manager;

  public ThemeUtils(ThemeManager manager) {
    this.manager = manager;
  }

  public GhostTheme getTheme() {
    return manager.getTheme();
  }

  public void applyImageBackground(ImageView v) {
    GhostTheme theme = getTheme();
    if (theme == null) {
      return;
    }
    WidgetTheme w = theme.getWidget();
    if (w == null) {
      return;
    }
    if (w.getImagepath() != null && !w.getImagepath().isEmpty()) {
      v.setVisibility(View.VISIBLE);
      if (w.getBlursize() == 0) {
        Glide.with(v.getContext())
            .load(w.getImagepath()) // noblur
            .into(v);
      } else {
        Glide.with(v.getContext())
            .load(w.getImagepath())
            .transform(new BlurTransformation((int) w.getBlursize()))
            .into(v);
      }
    } else v.setVisibility(View.INVISIBLE);
  }

  public void applyView(View v) {
    GhostTheme theme = getTheme();
    if (theme == null) {
      return;
    }
    if (theme.getActivity() == null) {
      return;
    }
    ActivityTheme colors = theme.getActivity();
    v.setBackgroundColor(Color.parseColor(colors.getBackground()));
  }

  public void applyWebViewBottomSheetFragment(WebViewBottomSheetFragment sheet) {
    var theme = getTheme();
    var weget = theme.getWidget();
    if (weget == null) return;

    sheet.setSheetBackgroundColor(Color.parseColor(weget.getBackground()));
    sheet.setHeaderTextColor(Color.parseColor(weget.getAccent()));
    sheet.setProgressBarColor(Color.parseColor(weget.getAccent()));
    sheet.setIconColorFilter(Color.parseColor(weget.getImageTint()));
    sheet.setCloseIconTint(Color.parseColor(weget.getImageTint()));
    sheet.setDropdownIconTint(Color.parseColor(weget.getImageTint()));
    sheet.setMenuIconTint(Color.parseColor(weget.getImageTint()));
  }

  public void applyViewPagePanel(ImageView v, ImageView v2, TextView tv, View rootview) {
    GhostTheme theme = getTheme();
    if (theme == null) {
      return;
    }
    var wiget = theme.getWidget();
    if (wiget == null) {
      return;
    }
    GradientDrawable gd = (GradientDrawable) rootview.getBackground().mutate();
    gd.setColor(Color.parseColor(wiget.getMenubackground()));
    v.setColorFilter(Color.parseColor(wiget.getMenutextcolor()));
    v2.setColorFilter(Color.parseColor(wiget.getMenutextcolor()));
    tv.setTextColor(Color.parseColor(wiget.getMenutextcolor()));
  }

  public void setFileManagerBack(View headTop, View headBottom, ImageView ic) {
    headBottom.setBackgroundColor(Color.TRANSPARENT);
    headTop.setBackgroundColor(Color.TRANSPARENT);
    GhostTheme theme = getTheme();
    if (theme == null) {
      return;
    }
    var w = theme.getWidget();
    if (w == null) {
      return;
    }
    if (w.getImagepath() != null && !w.getImagepath().isEmpty()) {
      ic.setVisibility(View.VISIBLE);
      if (w.getBlursize() == 0) {
        Glide.with(ic.getContext())
            .load(w.getImagepath()) // not blur mod inject
            .into(ic);
      } else {
        Glide.with(ic.getContext())
            .load(w.getImagepath())
            .transform(new BlurTransformation((int) w.getBlursize()))
            .into(ic);
      }
    } else ic.setVisibility(View.INVISIBLE);
  }

  public int getMenuColor() {
    GhostTheme theme = getTheme();
    if (theme == null) {
      return 0;
    }
    if (theme.getWidget() == null) {
      return 0;
    }

    return Color.parseColor(theme.getWidget().getMenubackground());
  }

  public void applySymbolBarLayout(LayoutSymbolbar bar) {
    GhostTheme theme = getTheme();
    if (theme == null) {
      return;
    }
    if (theme.getEditor() == null) {
      return;
    }
    var colors = theme.getEditor();
    var gd = new GradientDrawable();
    gd.setCornerRadius(40f);
    gd.setStroke(3, Color.parseColor(colors.getCompletionWndCorner()));
    gd.setColor(Color.parseColor(colors.getCompletionWndBackground()));
    bar.setBackground(gd);
  }

  public void applySymbolBarText(TextView bar) {
    GhostTheme theme = getTheme();
    if (theme == null) {
      return;
    }
    if (theme.getEditor() == null) {
      return;
    }
    var colors = theme.getEditor();

    bar.setTextColor(Color.parseColor(colors.getCompletionWndTextPrimary()));
  }

  public void applyActivity(AppCompatActivity activity) {

    GhostTheme theme = getTheme();
    if (theme == null) {
      return;
    }

    if (theme.getActivity() == null) {
      return;
    }

    ActivityTheme colors = theme.getActivity();
    Window window = activity.getWindow();

    if (colors.getStatusBar() != null) {
      window.setStatusBarColor(parseColor(colors.getStatusBar()));
    }

    if (colors.getNavigationBar() != null) {
      window.setNavigationBarColor(parseColor(colors.getNavigationBar()));
    }
  }

  public PowerMenu apply(Context c) {
    var menu = new PowerMenu.Builder(c).build();
    GhostTheme theme = getTheme();
    if (theme == null) {
      Log.e("PowerMenu", "theme has null");
    }
    if (theme.getWidget() == null) {
      Log.e("PowerMenu", "theme has null");
    }
    WidgetTheme widget = theme.getWidget();
    menu.setTextColor(Color.parseColor(widget.getMenutextcolor()));
    menu.setMenuColor(Color.parseColor(widget.getMenubackground()));
    menu.setIconColor(Color.parseColor(widget.getMenutextcolor()));
    menu.setSelectedMenuColor(Color.parseColor(widget.getSelectedmenucolor()));
    menu.setShowBackground(false);
    menu.setMenuRadius(10f);
    menu.setMenuShadow(3f);
    menu.setAutoDismiss(true);

    return menu;
  }

  public void applyEditor(IdeEditor editor) {
    GhostTheme theme = getTheme();
    if (theme == null || theme.getEditor() == null) return;
    EditorTheme t = theme.getEditor();
    var scheme = editor.getColorScheme();

    scheme.setColor(GhostColorScheme.LINE_DIVIDER, parseColor(t.getLineDivider()));
    scheme.setColor(GhostColorScheme.LINE_NUMBER, parseColor(t.getLineNumber()));
    scheme.setColor(
        GhostColorScheme.LINE_NUMBER_BACKGROUND, parseColor(t.getLineNumberBackground()));

    //  scheme.setColor(GhostColorScheme.WHOLE_BACKGROUND, parseColor(t.getWholeBackground()));
    editor.getColorScheme().setColor(EditorColorScheme.WHOLE_BACKGROUND, 0);
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
    scheme.setColor(GhostColorScheme.BRACKET1, parseColor(t.getBracketlevelmatch1()));
    scheme.setColor(GhostColorScheme.BRACKET2, parseColor(t.getBracketlevelmatch2()));
    scheme.setColor(GhostColorScheme.BRACKET3, parseColor(t.getBracketlevelmatch3()));
    scheme.setColor(GhostColorScheme.BRACKET4, parseColor(t.getBracketlevelmatch4()));
    scheme.setColor(GhostColorScheme.BRACKET5, parseColor(t.getBracketlevelmatch5()));
    scheme.setColor(GhostColorScheme.BRACKET6, parseColor(t.getBracketlevelmatch6()));
  }

  public void applyTextView(TextView textView) {

    GhostTheme theme = getTheme();

    if (theme == null) {
      return;
    }
    if (theme.getWidget() == null) {
      return;
    }
    WidgetTheme widget = theme.getWidget();
    if (widget.getText() != null) {
      textView.setTextColor(parseColor(widget.getText()));
    }
    if (widget.getHint() != null) {
      textView.setHintTextColor(parseColor(widget.getHint()));
    }
  }

  public void applyImageView(ImageView imageView) {

    GhostTheme theme = getTheme();

    if (theme == null) {
      return;
    }

    if (theme.getWidget() == null) {
      return;
    }

    WidgetTheme widget = theme.getWidget();

    if (widget.getImageTint() == null) {
      return;
    }

    imageView.setColorFilter(parseColor(widget.getImageTint()));
  }

  public void applyFab(FloatingActionButton fab) {

    GhostTheme theme = getTheme();

    if (theme == null) {
      return;
    }

    if (theme.getWidget() == null) {
      return;
    }

    WidgetTheme widget = theme.getWidget();

    if (widget.getFabBackground() != null) {

      fab.setBackgroundTintList(ColorStateList.valueOf(parseColor(widget.getFabBackground())));
    }

    if (widget.getFabIcon() != null) {

      fab.setColorFilter(parseColor(widget.getFabIcon()));
    }
  }

  public void applyTabLayout(TabLayout layout) {

    GhostTheme theme = getTheme();

    if (theme == null) {
      return;
    }

    if (theme.getWidget() == null) {
      return;
    }

    WidgetTheme widget = theme.getWidget();

    if (widget.getBackground() != null) {
      layout.setBackgroundColor(parseColor(widget.getBackground()));
    }

    if (widget.getAccent() != null) {
      layout.setSelectedTabIndicatorColor(parseColor(widget.getAccent()));
    }

    if (widget.getTabSelected() != null && widget.getTabUnselected() != null) {

      layout.setTabTextColors(
          parseColor(widget.getTabUnselected()), parseColor(widget.getTabSelected()));
    }
  }

  public void applyEditorStatusBar(EditorStatusBar status) {
    var editor = getTheme().getEditor();
    if (editor != null) {
      status.setStrokeColor(parseColor(editor.getCompletionWndCorner()));
      status.setBackgroundColorValue(parseColor(editor.getCompletionWndBackground()));
      status.setTextColor(parseColor(editor.getCompletionWndTextPrimary()));
      status.setIconTint(parseColor(editor.getCompletionWndTextSecondary()));
    }
  }

  public void applyGhostIdeEditorSearch(GhostIdeEditorSearch search) {
    var editor = getTheme().getEditor();
    if (editor != null) {
      search.setBackgroundColorValue(parseColor(editor.getCompletionWndBackground()));
      search.setStrokeColor(parseColor(editor.getCompletionWndCorner()));
      search.setTextColor(parseColor(editor.getCompletionWndTextPrimary()));
      search.setColorFilter(parseColor(editor.getCompletionWndTextSecondary()));
      search.setTextSearchColor(parseColor(editor.getCompletionWndTextMatched()));
    }
  }

  public void applySideSheetAndFileTree(SideSheetDialog dialog, FileTreeView v) {
    var editor = getTheme().getEditor();
    if (editor != null) {
      dialog
          .getWindow()
          .getDecorView()
          .setBackgroundTintList(
              ColorStateList.valueOf(parseColor(editor.getCompletionWndBackground())));
      v.setShowSearchBar(false);
      v.setZoomMod(true);
      v.setRainbowIndentGuides(true);
      int[] level = {
        parseColor(editor.getKeyword()),
        parseColor(editor.getAnnotation()),
        parseColor(editor.getAttributeName()),
        parseColor(editor.getAttributeValue()),
        parseColor(editor.getColornextless()),
        parseColor(editor.getCompletionWndCorner()),
        parseColor(editor.getCompletionWndTextPrimary())
      };
      v.setRainbowIndentGuideColors(level);
    }
  }

  /** Themes a generic side sheet (plugin panels, etc.) with the current editor background. */
  public void applySideSheet(SideSheetDialog dialog) {
    var editor = getTheme().getEditor();
    if (editor != null) {
      dialog
          .getWindow()
          .getDecorView()
          .setBackgroundTintList(
              ColorStateList.valueOf(parseColor(editor.getCompletionWndBackground())));
    }
  }

  private int parseColor(String color) {
    try {
      return Color.parseColor(color);
    } catch (Exception e) {
      return Color.WHITE;
    }
  }
}
