package ir.hanzodev1375.ghostide.codeeditors.preview.htmltag;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme;
import io.noties.markwon.AbstractMarkwonPlugin;
import io.noties.markwon.Markwon;
import io.noties.markwon.core.CorePlugin;
import io.noties.markwon.core.MarkwonTheme;
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin;
import io.noties.markwon.ext.tables.TablePlugin;
import io.noties.markwon.ext.tasklist.TaskListPlugin;
import io.noties.markwon.html.HtmlPlugin;
import io.noties.markwon.image.ImagesPlugin;
import io.noties.markwon.linkify.LinkifyPlugin;

import ir.hanzodev1375.ghostide.codeeditors.R;
import ir.hanzodev1375.ghostide.codeeditors.colorscheme.GhostColorScheme;
import ir.hanzodev1375.ghostide.codeeditors.preview.EditorPopUp;
import ir.hanzodev1375.ghostide.codeeditors.preview.Match;

import io.github.rosemoe.sora.event.SelectionChangeEvent;
import io.github.rosemoe.sora.widget.CodeEditor;
import io.github.rosemoe.sora.widget.base.EditorPopupWindow;

public class HtmlTagPreviewIde {

  private static final long PROCESS_DELAY_MS = 100;

  private static volatile Markwon markwonInstance;

  private final CodeEditor editor;
  private long lastProcessTime = 0;
  private String lastTag = "";
  private EditorPopupWindow activePopup;

  public HtmlTagPreviewIde(CodeEditor editor) {
    this.editor = editor;
  }

  public void attach() {
    editor.subscribeEvent(
        SelectionChangeEvent.class,
        (event, unsubscribe) -> {
          if (event.getCause() != SelectionChangeEvent.CAUSE_TAP) return;

          long now = System.currentTimeMillis();
          if (now - lastProcessTime < PROCESS_DELAY_MS) return;
          lastProcessTime = now;

          editor.post(
              () -> {
                try {
                  handleSelectionChange(event);
                } catch (Exception e) {
                  dismissPopup();
                }
              });
        });
  }

  private void handleSelectionChange(SelectionChangeEvent event) {
    if (editor.getCursor().isSelected()) {
      dismissPopup();
      lastTag = "";
      return;
    }

    int line = event.getLeft().getLine();
    int column = event.getLeft().getColumn();
    String lineText = editor.getText().getLineString(line);

    Match match = HtmlTagRefUtils.findTagAtPosition(lineText, column);

    if (match == null) {
      dismissPopup();
      lastTag = "";
      return;
    }

    HtmlTagDoc doc = HtmlTagDocRepository.getInstance(editor.getContext()).getDoc(match.path);

    if (doc == null) {
      dismissPopup();
      lastTag = "";
      return;
    }

    if (match.path.equalsIgnoreCase(lastTag) && activePopup != null && activePopup.isShowing()) {
      return;
    }

    lastTag = match.path;
    showPreview(doc);
  }

  private void showPreview(HtmlTagDoc doc) {
    dismissPopup();
    activePopup = EditorPopUp.showCustomViewAtCursor(editor, buildView(doc));
  }

  private View buildView(HtmlTagDoc doc) {
    View root =
        LayoutInflater.from(editor.getContext())
            .inflate(R.layout.editor_html_tag_preview, null, false);

    TextView nameView = root.findViewById(R.id.html_tag_name);
    TextView contentView = root.findViewById(R.id.html_tag_content);
    EditorColorScheme colors = editor.getColorScheme();
    nameView.setTextColor(colors.getColor(GhostColorScheme.COMPLETION_WND_TEXT_PRIMARY));

    nameView.setText("<" + doc.name + ">");
    getMarkwon(editor.getContext()).setMarkdown(contentView, doc.markdown);

    return root;
  }

  private Markwon getMarkwon(Context context) {
    if (markwonInstance == null) {
      synchronized (HtmlTagPreviewIde.class) {
        if (markwonInstance == null) {
          Context appContext = context.getApplicationContext();

          markwonInstance =
              Markwon.builder(appContext)
                  .usePlugin(
                      new AbstractMarkwonPlugin() {
                        @Override
                        public void configureTheme(@NonNull MarkwonTheme.Builder builder) {
                          EditorColorScheme c = editor.getColorScheme();
                          builder
                              .linkColor(c.getColor(GhostColorScheme.COMPLETION_WND_TEXT_SECONDARY))
                              .codeTextColor(
                                  c.getColor(GhostColorScheme.COMPLETION_WND_TEXT_PRIMARY))
                              .codeBackgroundColor(
                                  c.getColor(GhostColorScheme.COMPLETION_WND_BACKGROUND))
                              .listItemColor(
                                  c.getColor(GhostColorScheme.COMPLETION_WND_TEXT_SECONDARY))
                              .thematicBreakColor(
                                  c.getColor(GhostColorScheme.COMPLETION_WND_CORNER))
                              .bulletListItemStrokeWidth(2)
                              .headingBreakColor(
                                  c.getColor(GhostColorScheme.COMPLETION_WND_TEXT_MATCHED))
                              .isLinkUnderlined(false);
                        }
                      })
                  .usePlugin(StrikethroughPlugin.create())
                  .usePlugin(LinkifyPlugin.create())
                  .usePlugin(HtmlPlugin.create())
                  .usePlugin(TablePlugin.create(appContext))
                  .usePlugin(TaskListPlugin.create(appContext))
                  .usePlugin(ImagesPlugin.create())
                  .build();
        }
      }
    }
    return markwonInstance;
  }

  private void dismissPopup() {
    if (activePopup != null) {
      try {
        activePopup.dismiss();
      } catch (Exception ignored) {
      }
      activePopup = null;
    }
  }
}
