package ir.hanzodev1375.ghostide.store;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.Nullable;
import com.blankj.utilcode.util.FileIOUtils;
import com.blankj.utilcode.util.FileUtils;
import ir.hanzodev1375.components.sheet.BaseBlurBottomSheet;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ir.hanzodev1375.ghostide.R;
import ir.hanzodev1375.ghostide.codeeditors.IdeEditor;
import ir.hanzodev1375.ghostide.codeeditors.colorscheme.GhostColorScheme;
import ir.hanzodev1375.ghostide.codeeditors.langs.cpp.CppLanguage;
import ir.hanzodev1375.ghostide.codeeditors.langs.html.HtmlLanguage;
import ir.hanzodev1375.ghostide.codeeditors.langs.java.JavaLanguage;
import ir.hanzodev1375.ghostide.codeeditors.langs.js.JsLanguage;
import ir.hanzodev1375.ghostide.databinding.SheetThemeStorePreviewBinding;
import ir.theme.EditorTheme;
import ir.theme.GhostTheme;
import ir.theme.M3Theme;
import ir.theme.MaterialTheme;
import ir.theme.ThemeManager;
import ir.theme.ThemeMediaPath;
import ir.theme.WidgetTheme;
import ir.theme.ActivityTheme;
import ir.theme.internal.ThemeRefResolver;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme;
import ir.hanzodev1375.components.childern.ViewChilder;
import ir.hanzodev1375.components.store.api.ThemesApi;
import ir.hanzodev1375.components.store.event.ThemeInstalledEvent;
import ir.hanzodev1375.components.store.model.ThemeItem;
import ninja.coder.appuploader.main.appupdate.MarkwonHelper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.greenrobot.eventbus.EventBus;

public class ThemeStorePreviewSheet extends BaseBlurBottomSheet {

  private static final String ARG_THEME_JSON = "theme_json";

  private static final OkHttpClient client =
      new OkHttpClient.Builder()
          .connectTimeout(15, TimeUnit.SECONDS)
          .readTimeout(60, TimeUnit.SECONDS)
          .build();

  private SheetThemeStorePreviewBinding binding;
  private ThemeItem theme;
  private IdeEditor editorPreview;
  private View widgetRoot;
  private GhostTheme appliedTheme;
  private File appliedThemeFile;
  private boolean themeReady = false;

  public static ThemeStorePreviewSheet newInstance(ThemeItem theme) {
    ThemeStorePreviewSheet sheet = new ThemeStorePreviewSheet();
    Bundle args = new Bundle();
    args.putString(ARG_THEME_JSON, new Gson().toJson(theme));
    sheet.setArguments(args);
    return sheet;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getArguments() != null) {
      theme = new Gson().fromJson(getArguments().getString(ARG_THEME_JSON), ThemeItem.class);
    }
    if (theme == null) {
      dismiss();
    }
  }

  @Override
  protected void onContentReady(ViewGroup contentContainer) {
    View view =
        getLayoutInflater().inflate(R.layout.sheet_theme_store_preview, contentContainer, false);
    contentContainer.addView(
        view,
        new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

    binding = SheetThemeStorePreviewBinding.bind(view);

    if (theme == null) {
      dismiss();
      return;
    }
    binding.previewTitle.setText(theme.name());
    binding.previewSubtitle.setText(
        getString(
            R.string.themes_dev_version,
            theme.devname() != null ? theme.devname() : "",
            theme.version()));

    editorPreview = binding.editorPreview;
    setupTabs();
    buildWidgetSample(binding.widgetContent);

    binding.fabClose.setOnClickListener(v -> dismiss());
    binding.fabInstall.setEnabled(false);
    binding.fabInstall.setAlpha(0.55f);
    binding.fabInstall.setOnClickListener(v -> installTheme(view));

    M3Theme.applyTopLevel(view);
    loadDescription();
    startDownload();
  }

  private void loadDescription() {
    if (binding.previewDescriptionLabel != null) {
      binding.previewDescriptionLabel.setText("Description");
    }
    String url = theme.doc();
    if (binding.previewDescription == null || url == null || url.isEmpty()) return;
    ThemesApi.fetchText(
        getContext(),
        url,
        new ThemesApi.TextCallbacks() {
          @Override
          public void onSuccess(String content) {
            if (content == null || content.isEmpty()) return;
            binding.previewDescription.post(
                () -> MarkwonHelper.setMarkdown(binding.previewDescription, content));
          }

          @Override
          public void onError(String message) {
            if (message != null)
              binding.previewDescription.post(
                  () -> MarkwonHelper.setMarkdown(binding.previewDescription, message));
          }
        });
  }

  private void setupTabs() {
    binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Editor"));
    binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Widgets"));
    binding.tabLayout.addOnTabSelectedListener(
        new com.google.android.material.tabs.TabLayout.OnTabSelectedListener() {
          @Override
          public void onTabSelected(com.google.android.material.tabs.TabLayout.Tab tab) {
            updateTabVisibility(tab.getPosition() == 0);
          }

          @Override
          public void onTabUnselected(com.google.android.material.tabs.TabLayout.Tab tab) {}

          @Override
          public void onTabReselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
        });
    updateTabVisibility(true);
  }

  private void updateTabVisibility(boolean editor) {
    if (binding == null) return;
    binding.editorContent.setVisibility(editor ? View.VISIBLE : View.GONE);
    binding.widgetContent.setVisibility(editor ? View.GONE : View.VISIBLE);
  }

  private void buildWidgetSample(ViewGroup parent) {
    LayoutInflater inflater = LayoutInflater.from(requireContext());
    widgetRoot = inflater.inflate(R.layout.widget_theme_preview_sample, parent, false);
    parent.addView(widgetRoot);
  }

  private File out = null;

  private void startDownload() {
    String url = theme.linkdownload();
    if (url == null || url.isEmpty()) {
      Toast.makeText(requireContext(), R.string.themes_download_failed, Toast.LENGTH_SHORT).show();
      dismiss();
      return;
    }
    binding.previewProgressContainer.setVisibility(View.VISIBLE);
    binding.previewProgressContainer.setAlpha(0f);
    binding.previewProgressContainer.animate().alpha(1f).setDuration(250).start();
    binding.previewProgress.setProgress(0);
    binding.previewProgressText.setText("0%");

    new Thread(
            () -> {
              boolean ok = false;

              try {
                File dir = new File(requireContext().getCacheDir(), "theme_preview");
                clearCache(dir);
                dir.mkdirs();

                int dot = url.lastIndexOf('/');
                String fileName = dot >= 0 ? url.substring(dot + 1) : "preview.gth";
                if (!fileName.endsWith(".gth")) {
                  fileName = fileName + ".gth";
                }
                out = new File(dir, "preview.gth");
                download(url, out);

                String themeDirUrl =
                    dot >= 0 ? url.substring(0, url.lastIndexOf('/') + 1) : ThemesApi.REPO_BASE;
                downloadBackground(out, themeDirUrl);
                ok = out.exists();
              } catch (Exception e) {
                ok = false;
              }
              boolean success = ok;
              requireActivity()
                  .runOnUiThread(
                      () -> {
                        binding
                            .previewProgressContainer
                            .animate()
                            .alpha(0f)
                            .setDuration(300)
                            .setListener(
                                new AnimatorListenerAdapter() {
                                  @Override
                                  public void onAnimationEnd(Animator animation) {
                                    binding.previewProgressContainer.setVisibility(View.GONE);
                                  }
                                })
                            .start();
                        if (success && out != null) {
                          applyPreview(out);
                        } else {
                          Toast.makeText(
                                  requireContext(),
                                  R.string.themes_download_failed,
                                  Toast.LENGTH_SHORT)
                              .show();
                        }
                      });
            })
        .start();
  }

  private void clearCache(File dir) {
    if (dir == null || !dir.exists()) return;
    File[] files = dir.listFiles();
    if (files == null) return;
    for (File f : files) {
      f.delete();
    }
  }

  private void download(String url, File out) throws Exception {
    Request request = new Request.Builder().url(url).get().build();
    try (Response response = client.newCall(request).execute()) {
      if (!response.isSuccessful() || response.body() == null) {
        throw new IllegalStateException("bad response");
      }
      long total = response.body().contentLength();
      try (FileOutputStream fos = new FileOutputStream(out);
          InputStream is = response.body().byteStream()) {
        byte[] buffer = new byte[8192];
        int read;
        long done = 0;
        while ((read = is.read(buffer)) != -1) {
          fos.write(buffer, 0, read);
          done += read;
          if (total > 0) {
            int percent = (int) ((done * 100) / total);
            int p = percent;
            binding.previewProgressContainer.post(
                () -> {
                  binding.previewProgress.setProgress(p);
                  binding.previewProgressText.setText(p + "%");
                });
          }
        }
      }
    }
  }

  private void downloadBackground(File themeFile, String themeDirUrl) {
    try {
      String json = FileIOUtils.readFile2String(themeFile);
      JsonObject root = JsonParser.parseString(json).getAsJsonObject();
      if (!root.has("widget")) return;
      JsonObject widget = root.getAsJsonObject("widget");
      if (!widget.has("imagepath")) return;
      String imagepath = widget.get("imagepath").getAsString();
      if (imagepath == null || imagepath.isEmpty()) return;
      if (imagepath.startsWith("http")
          || imagepath.startsWith("/")
          || imagepath.startsWith("content:")
          || imagepath.startsWith("file:")) {
        return;
      }
      while (imagepath.startsWith("../")) {
        imagepath = imagepath.substring(3);
      }
      if (imagepath.startsWith("./")) {
        imagepath = imagepath.substring(2);
      }
      String bgUrl = themeDirUrl + imagepath;
      String bgName = new File(bgUrl).getName();
      File bg = new File(themeFile.getParentFile(), bgName);
      Request request = new Request.Builder().url(bgUrl).get().build();
      try (Response response = client.newCall(request).execute()) {
        if (response.isSuccessful() && response.body() != null) {
          try (OutputStream fos = new FileOutputStream(bg);
              InputStream is = response.body().byteStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
              fos.write(buffer, 0, read);
            }
          }
        }
      }
    } catch (Exception ignored) {
    }
  }

  private void applyPreview(File themeFile) {
    String json = FileIOUtils.readFile2String(themeFile);
    if (json == null || json.isEmpty()) {
      Toast.makeText(requireContext(), R.string.themes_download_failed, Toast.LENGTH_SHORT).show();
      return;
    }
    try {
      JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
      ThemeRefResolver.resolveJson(obj);
      GhostTheme t = new Gson().fromJson(obj, GhostTheme.class);
      if (t == null) throw new IllegalStateException();
      if (t.getActivity() == null) t.setActivity(new ActivityTheme());
      if (t.getEditor() == null) t.setEditor(new EditorTheme());
      if (t.getWidget() == null) t.setWidget(new WidgetTheme());
      if (t.getMaterial3() == null) t.setMaterial3(new MaterialTheme());

      appliedTheme = t;
      appliedThemeFile = themeFile;
      themeReady = true;

      applyEditorTheme(t);
      applyWidgetTheme(t);
      applyBackground(t, themeFile);

      binding.fabInstall.setEnabled(true);
      binding.fabInstall.setAlpha(1f);
    } catch (Exception e) {
      Toast.makeText(requireContext(), R.string.themes_download_failed, Toast.LENGTH_SHORT).show();
    }
  }

  private void applyEditorTheme(GhostTheme theme) {
    EditorTheme t = theme.getEditor();
    var scheme = editorPreview.getColorScheme();
    scheme.setColor(GhostColorScheme.LINE_DIVIDER, parseColor(t.getLineDivider()));
    scheme.setColor(GhostColorScheme.LINE_NUMBER, parseColor(t.getLineNumber()));
    scheme.setColor(
        GhostColorScheme.LINE_NUMBER_BACKGROUND, parseColor(t.getLineNumberBackground()));
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

    setSampleCode(0);
    editorPreview.setHighlightCurrentLine(true);
    editorPreview.invalidate();
  }

  private void applyWidgetTheme(GhostTheme theme) {
    if (widgetRoot == null) return;
    WidgetTheme w = theme.getWidget();
    MaterialTheme m = theme.getMaterial3();

    int surface = color(w.getSurface(), m != null ? m.getSurface() : null);
    int accent = color(w.getAccent(), m != null ? m.getPrimary() : null);
    int text = color(w.getText(), m != null ? m.getOnSurface() : null);
    int bg = color(w.getBackground(), m != null ? m.getBackground() : null);
    int hint = color(w.getHint(), m != null ? m.getOnSurfaceVariant() : null);
    int imageTint = color(w.getImageTint(), m != null ? m.getOnSurface() : null);
    int onSurface = color(null, m != null ? m.getOnSurface() : null);

    if (bg != Color.TRANSPARENT) widgetRoot.setBackgroundColor(bg);

    applyWidgetRecursively(widgetRoot, surface, accent, text, hint, imageTint, onSurface);
  }

  private void applyWidgetRecursively(
      View v, int surface, int accent, int text, int hint, int imageTint, int onSurface) {
    if (v instanceof com.google.android.material.materialswitch.MaterialSwitch) {
      ((com.google.android.material.materialswitch.MaterialSwitch) v)
          .setButtonTintList(ColorStateList.valueOf(accent));
      ((com.google.android.material.materialswitch.MaterialSwitch) v).setThumbTintList(null);
    } else if (v instanceof com.google.android.material.checkbox.MaterialCheckBox) {
      ((com.google.android.material.checkbox.MaterialCheckBox) v)
          .setButtonTintList(ColorStateList.valueOf(accent));
    } else if (v instanceof com.google.android.material.slider.Slider) {
      ((com.google.android.material.slider.Slider) v)
          .setThumbTintList(ColorStateList.valueOf(accent));
      ((com.google.android.material.slider.Slider) v)
          .setTickTintList(ColorStateList.valueOf(accent));
    } else if (v instanceof com.google.android.material.appbar.MaterialToolbar) {
      ((com.google.android.material.appbar.MaterialToolbar) v).setBackgroundColor(surface);
      ((com.google.android.material.appbar.MaterialToolbar) v).setTitleTextColor(text);
    } else if (v instanceof com.google.android.material.floatingactionbutton.FloatingActionButton) {
      com.google.android.material.floatingactionbutton.FloatingActionButton fab =
          (com.google.android.material.floatingactionbutton.FloatingActionButton) v;
      WidgetTheme w = appliedTheme != null ? appliedTheme.getWidget() : null;
      if (w != null) {
        if (w.getFabBackground() != null) {
          fab.setBackgroundTintList(ColorStateList.valueOf(parseColor(w.getFabBackground())));
        }
        if (w.getFabIcon() != null) {
          fab.setColorFilter(parseColor(w.getFabIcon()));
        } else {
          fab.setColorFilter(imageTint);
        }
      }
    } else if (v instanceof com.google.android.material.tabs.TabLayout) {
      ((com.google.android.material.tabs.TabLayout) v).setBackgroundColor(surface);
      ((com.google.android.material.tabs.TabLayout) v).setSelectedTabIndicatorColor(accent);
    } else if (v instanceof com.google.android.material.bottomnavigation.BottomNavigationView) {
      ((com.google.android.material.bottomnavigation.BottomNavigationView) v)
          .setBackgroundColor(surface);
    } else if (v instanceof com.google.android.material.textfield.TextInputLayout) {
      ((com.google.android.material.textfield.TextInputLayout) v).setBoxStrokeColor(accent);
    } else if (v instanceof com.google.android.material.button.MaterialButton) {
      ((com.google.android.material.button.MaterialButton) v)
          .setBackgroundTintList(ColorStateList.valueOf(accent));
      ((com.google.android.material.button.MaterialButton) v).setTextColor(onSurface);
    } else if (v instanceof android.widget.TextView) {
      ((android.widget.TextView) v).setTextColor(text);
    }

    if (v instanceof ViewGroup) {
      for (int i = 0; i < ((ViewGroup) v).getChildCount(); i++) {
        applyWidgetRecursively(
            ((ViewGroup) v).getChildAt(i), surface, accent, text, hint, imageTint, onSurface);
      }
    }
  }

  private void applyBackground(GhostTheme theme, File themeFile) {
    WidgetTheme widget = theme.getWidget();
    ActivityTheme activity = theme.getActivity();
    String imagePath = widget != null ? widget.getImagepath() : null;

    int bgColor = Color.TRANSPARENT;
    if (activity != null && activity.getBackground() != null) {
      bgColor = parseColor(activity.getBackground());
    }
    View root = getView();
    if (root != null) {
      root.setBackgroundColor(bgColor);
    }

    boolean hasImage = imagePath != null && !imagePath.isEmpty();
    ViewChilder bgMedia = binding.ivBackgroundImage;
    if (bgMedia != null) {
      bgMedia.setVisibility(hasImage ? View.VISIBLE : View.GONE);
      if (hasImage) {
        String loadPath = ThemeMediaPath.resolve(themeFile.getAbsolutePath(), imagePath);
        float blur = widget.getBlursize();
        bgMedia.load(loadPath != null ? loadPath : imagePath, blur);
      } else {
        bgMedia.clear();
      }
    }
  }

  private void installTheme(View root) {
    if (!themeReady || appliedThemeFile == null || !appliedThemeFile.exists()) {
      Toast.makeText(requireContext(), R.string.themes_download_failed, Toast.LENGTH_SHORT).show();
      return;
    }
    try {
      String name = theme.name() + ".gth";
      File dir = new File(Environment.getExternalStorageDirectory(), "ghostide/themes");
      dir.mkdirs();
      File target = new File(dir, name);
      FileUtils.copy(appliedThemeFile.getAbsolutePath(), target.getAbsolutePath());
      new ThemeManager(requireContext()).setThemeFromFile(target.getAbsolutePath());
      Toast.makeText(requireContext(), R.string.themes_applied, Toast.LENGTH_SHORT).show();
      EventBus.getDefault().post(new ThemeInstalledEvent());
      dismiss();
    } catch (Exception e) {
      Toast.makeText(requireContext(), R.string.themes_download_failed, Toast.LENGTH_SHORT).show();
    }
  }

  private void setSampleCode(int position) {
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
                + "<head><title>Sample</title></head>\n"
                + "<body><h1>Hello</h1><p>World</p></body>\n"
                + "</html>";
        editorPreview.setEditorLanguage(new HtmlLanguage(getContext(), ""));
        break;
      case 2:
        code =
            "function greet(name) {\n"
                + "    return `Hello, ${name}!`;\n"
                + "}\n"
                + "const result = greet('User');\n"
                + "console.log(result);";
        editorPreview.setEditorLanguage(new JsLanguage(getContext(), ""));
        break;
      default:
        code = "#include <iostream>\n" + "using namespace std;\n" + "int main() { return 0; }";
        editorPreview.setEditorLanguage(new CppLanguage(getContext()));
        break;
    }
    editorPreview.setText(code);
  }

  @Override
  public void onDestroyView() {
    if (binding != null && binding.ivBackgroundImage != null) {
      binding.ivBackgroundImage.clear();
    }
    binding = null;
    super.onDestroyView();
  }

  private int color(String direct, String fallback) {
    if (direct != null) {
      int c = parseColor(direct);
      if (c != Color.TRANSPARENT) return c;
    }
    if (fallback != null) return parseColor(fallback);
    return Color.TRANSPARENT;
  }

  private int parseColor(String color) {
    if (color == null) return Color.TRANSPARENT;
    try {
      return Color.parseColor(color);
    } catch (Exception e) {
      return Color.TRANSPARENT;
    }
  }
}
