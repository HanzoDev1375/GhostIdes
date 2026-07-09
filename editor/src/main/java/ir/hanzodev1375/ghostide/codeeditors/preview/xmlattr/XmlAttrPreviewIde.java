package ir.hanzodev1375.ghostide.codeeditors.preview.xmlattr;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import ir.hanzodev1375.ghostide.codeeditors.R;
import ir.hanzodev1375.ghostide.codeeditors.langs.xml.XmlLanguage;
import ir.hanzodev1375.ghostide.codeeditors.preview.EditorPopUp;
import ir.hanzodev1375.ghostide.codeeditors.preview.Match;

import io.github.rosemoe.sora.event.SelectionChangeEvent;
import io.github.rosemoe.sora.widget.CodeEditor;
import io.github.rosemoe.sora.widget.base.EditorPopupWindow;

public final class XmlAttrPreviewIde {

  private static final long PROCESS_DELAY_MS = 100;

  private final CodeEditor editor;
  private long lastProcessTime = 0;
  private String lastAttribute = "";
  private EditorPopupWindow activePopup;

  public XmlAttrPreviewIde(CodeEditor editor) {
    this.editor = editor;
  }

  public void attach() {
    // if (isXmlLang())
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

  boolean isXmlLang() {
    return editor.getEditorLanguage() instanceof XmlLanguage;
  }

  private void handleSelectionChange(SelectionChangeEvent event) {
    if (editor.getCursor().isSelected()) {
      dismissPopup();
      lastAttribute = "";
      return;
    }

    int line = event.getLeft().getLine();
    int column = event.getLeft().getColumn();
    String lineText = editor.getText().getLineString(line);

    Match match = XmlAttrRefUtils.findAttrAtPosition(lineText, column);

    if (match == null) {
      dismissPopup();
      lastAttribute = "";
      return;
    }

    XmlAttrDoc doc = XmlAttrDocRepository.getInstance(editor.getContext()).getDoc(match.path);

    if (doc == null) {
      dismissPopup();
      lastAttribute = "";
      return;
    }

    if (match.path.equals(lastAttribute) && activePopup != null && activePopup.isShowing()) {
      return;
    }

    lastAttribute = match.path;
    showPreview(doc);
  }

  // ── Popup (XML layout based) ────────────────────────────────────────────

  private void showPreview(XmlAttrDoc doc) {
    dismissPopup();
    activePopup = EditorPopUp.showCustomViewAtCursor(editor, buildView(doc));
  }

  private View buildView(XmlAttrDoc doc) {
    View root =
        LayoutInflater.from(editor.getContext())
            .inflate(R.layout.editor_xml_attr_preview, null, false);

    TextView nameView = root.findViewById(R.id.xml_attr_name);
    TextView descView = root.findViewById(R.id.xml_attr_description);
    TextView codeLabelView = root.findViewById(R.id.xml_attr_code_label);
    TextView codeView = root.findViewById(R.id.xml_attr_code);
    TextView exampleLabelView = root.findViewById(R.id.xml_attr_example_label);
    TextView exampleView = root.findViewById(R.id.xml_attr_example);

    nameView.setText(doc.attribute);
    bindOrHide(descView, doc.description);
    bindOrHide(codeLabelView, codeView, doc.code);
    bindOrHide(exampleLabelView, exampleView, doc.example);

    return root;
  }

  private void bindOrHide(TextView view, String text) {
    if (text == null || text.trim().isEmpty()) {
      view.setVisibility(View.GONE);
      return;
    }
    view.setVisibility(View.VISIBLE);
    view.setText(text);
  }

  private void bindOrHide(TextView label, TextView value, String text) {
    if (text == null || text.trim().isEmpty()) {
      label.setVisibility(View.GONE);
      value.setVisibility(View.GONE);
      return;
    }
    label.setVisibility(View.VISIBLE);
    value.setVisibility(View.VISIBLE);
    value.setText(text);
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
