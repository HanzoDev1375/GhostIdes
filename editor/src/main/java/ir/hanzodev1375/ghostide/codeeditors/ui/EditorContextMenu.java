package ir.hanzodev1375.ghostide.codeeditors.ui;

import android.view.Menu;
import io.github.rosemoe.sora.event.CreateContextMenuEvent;
import io.github.rosemoe.sora.widget.component.EditorContextMenuCreator;
import ir.hanzodev1375.ghostide.codeeditors.IdeEditor;
import ir.hanzodev1375.ghostide.codeeditors.R;

/**
 * منوی راست‌کلیک ادیتور؛ آیتم‌های پیش‌فرض (انتخاب همه/کپی/برش/چسباندن) را نگه می‌دارد و اکشن‌های
 * ویرایشی اضافه می‌کند که با تپ طولانی چندباره داخل ادیتور ظاهر می‌شوند.
 */
public class EditorContextMenu extends EditorContextMenuCreator {

  private final IdeEditor editor;

  public EditorContextMenu(IdeEditor editor) {
    super(editor);
    this.editor = editor;
  }

  @Override
  public void onCreateContextMenu(CreateContextMenuEvent event) {
    super.onCreateContextMenu(event);
    if (!editor.isEditable()) {
      return;
    }
    Menu menu = event.getMenu();
    menu.add(0, 1, 0, editor.getContext().getString(R.string.editor_indent))
        .setOnMenuItemClickListener(
            item -> {
              editor.indentOrCommitTab();
              return true;
            });
    menu.add(0, 2, 0, editor.getContext().getString(R.string.editor_unindent))
        .setOnMenuItemClickListener(
            item -> {
              editor.unindentSelection();
              return true;
            });
    menu.add(0, 3, 0, editor.getContext().getString(R.string.editor_duplicate_line))
        .setOnMenuItemClickListener(
            item -> {
              editor.setDuplicateLine();
              return true;
            });
    menu.add(0, 4, 0, editor.getContext().getString(R.string.editor_select_current_word))
        .setOnMenuItemClickListener(
            item -> {
              editor.selectCurrentWord();
              return true;
            });
    menu.add(0, 5, 0, editor.getContext().getString(R.string.editor_toggle_comment))
        .setOnMenuItemClickListener(
            item -> {
              editor.toggleCommentForCurrentLine();
              return true;
            });
  }
}