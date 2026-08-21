package ir.hanzodev1375.components.sheet.customitemsheet.callback;

import android.view.View;
import ir.hanzodev1375.components.sheet.customitemsheet.model.SheetModel;

public interface OnSheetListener {
  void call(SheetModel model, int pos, View view);
}
