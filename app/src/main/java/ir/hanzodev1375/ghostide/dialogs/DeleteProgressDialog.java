package ir.hanzodev1375.ghostide.dialogs;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import ir.hanzodev1375.ghostide.R;
import ir.hanzodev1375.ghostide.mvvm.viewmodel.FileViewModel;
import ir.hanzodev1375.components.sheet.customitemsheet.ui.DialogCompat;
public class DeleteProgressDialog {

  private final AlertDialog dialog;
  private final TextView tvFileName;
  private final TextView tvCount;
  private final LinearProgressIndicator progressBar;

  public DeleteProgressDialog(Context context) {
    View view = LayoutInflater.from(context).inflate(R.layout.dialog_delete_progress, null);

    tvFileName = view.findViewById(R.id.tvFileName);
    tvCount = view.findViewById(R.id.tvCount);
    progressBar = view.findViewById(R.id.progressBar);

    dialog =
        new DialogCompat(context)
            .setView(view)
            .setNeutralButton("HIDE", (d, w) -> d.dismiss())
            .setCancelable(false)
            .create();
  }

  public void show() {
    if (!dialog.isShowing()) dialog.show();
  }

  public void dismiss() {
    if (dialog.isShowing()) dialog.dismiss();
  }

  public boolean isShowing() {
    return dialog.isShowing();
  }

  public void update(FileViewModel.DeleteProgress p) {
    if (!dialog.isShowing()) return;
    tvFileName.setText(p.fileName);
    tvCount.setText(p.deleted + " / " + p.total);
    if (p.total > 0) {
      int percent = (int) (p.deleted * 100.0 / p.total);
      progressBar.setProgressCompat(percent, true);
    }
  }
}
