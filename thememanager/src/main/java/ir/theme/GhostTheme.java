package ir.theme;

public class GhostTheme {

  private ActivityTheme activity;

  private EditorTheme editor;

  private WidgetTheme widget;
 
  public ActivityTheme getActivity() {
    return activity;
  }

  public void setActivity(ActivityTheme activity) {
    this.activity = activity;
  }

  public EditorTheme getEditor() {
    return editor;
  }

  public void setEditor(EditorTheme editor) {
    this.editor = editor;
  }

  public WidgetTheme getWidget() {
    return widget;
  }

  public void setWidget(WidgetTheme widget) {
    this.widget = widget;
  }
}