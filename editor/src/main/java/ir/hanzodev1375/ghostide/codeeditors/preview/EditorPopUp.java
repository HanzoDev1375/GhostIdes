package ir.hanzodev1375.ghostide.codeeditors.preview;

import android.graphics.drawable.GradientDrawable;
import android.view.View;

import io.github.rosemoe.sora.widget.CodeEditor;
import io.github.rosemoe.sora.widget.base.EditorPopupWindow;
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme;
import ir.hanzodev1375.ghostide.codeeditors.ui.CustomEditorAutoCompletion;

public class EditorPopUp {

  private EditorPopUp() {}

  public static EditorPopupWindow showCustomViewAtCursor(CodeEditor editor, View customView) {
    try {
      EditorPopupWindow popupWindow =
          new EditorPopupWindow(
              editor,
              EditorPopupWindow.FEATURE_SCROLL_AS_CONTENT
                  | EditorPopupWindow.FEATURE_SHOW_OUTSIDE_VIEW_ALLOWED);

      popupWindow.setContentView(customView);
      popupWindow.getPopup().setOutsideTouchable(true);

      customView.measure(
          View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
          View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));

      int width = customView.getMeasuredWidth();
      int height = customView.getMeasuredHeight();
      popupWindow.setSize(width, height);

      var selection = editor.getCursor().left();
      float charX = editor.getCharOffsetX(selection.getLine(), selection.getColumn());
      float charY =
          editor.getCharOffsetY(selection.getLine(), selection.getColumn()) - editor.getRowHeight();

      var locationBuffer = new int[2];
      editor.getLocationInWindow(locationBuffer);
      float restAbove = charY + locationBuffer[1];
      float restBottom = editor.getHeight() - charY - editor.getRowHeight();

      boolean completionShowing = false;
      try {
        CustomEditorAutoCompletion completion =
            editor.getComponent(CustomEditorAutoCompletion.class);
        completionShowing = completion != null && completion.isShowing();
      } catch (Exception ignored) {
      }

      float windowY;
      if (restAbove > restBottom || completionShowing) {
        windowY = charY - popupWindow.getHeight();
      } else {
        windowY = charY + editor.getRowHeight() * 1.5f;
      }

      if (completionShowing && windowY < 0) {
        popupWindow.dismiss();
        return popupWindow;
      }

      var colorScheme = editor.getColorScheme();
      GradientDrawable draw = new GradientDrawable();
      draw.setColor(colorScheme.getColor(EditorColorScheme.COMPLETION_WND_BACKGROUND));
      draw.setStroke(2, colorScheme.getColor(EditorColorScheme.COMPLETION_WND_CORNER));
      draw.setCornerRadius(20);
      customView.setBackground(draw);

      float windowX = Math.max(charX - popupWindow.getWidth() / 2f, 0f);
      popupWindow.setLocationAbsolutely((int) windowX, (int) windowY);
      popupWindow.show();

      if (editor.getCursor().isSelected()) {
        popupWindow.dismiss();
      }

      return popupWindow;

    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }
}
