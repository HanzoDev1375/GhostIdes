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
public class CopyProgressDialog {

  private final AlertDialog dialog;
  private final TextView tvTitle;
  private final TextView tvFileName;
  private final TextView tvFrom;
  private final TextView tvTo;
  private final TextView tvRemaining;
  private final TextView tvSpeed;
  private final LinearProgressIndicator progressBar;
  private boolean isMoveMode = false;

  public CopyProgressDialog(Context context) {
    View view = LayoutInflater.from(context).inflate(R.layout.dialog_copy_progress, null);

    tvTitle     = view.findViewById(R.id.tvTitle);
    tvFileName  = view.findViewById(R.id.tvFileName);
    tvFrom      = view.findViewById(R.id.tvFrom);
    tvTo        = view.findViewById(R.id.tvTo);
    tvRemaining = view.findViewById(R.id.tvRemaining);
    tvSpeed     = view.findViewById(R.id.tvSpeed);
    progressBar = view.findViewById(R.id.progressBar);

    dialog = new DialogCompat(context)
        .setView(view)
        .setNeutralButton("HIDE", (d, w) -> d.dismiss())
        .setCancelable(false)
        .create();
  }

  // قبل از show صدا بزن تا title درست باشه
  public void setMoveMode(boolean isMove) {
    this.isMoveMode = isMove;
    if (tvTitle != null) tvTitle.setText(isMove ? "Moving..." : "Copying...");
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

  public void update(FileViewModel.CopyProgress p) {
    if (!dialog.isShowing()) return;

    tvTitle.setText(isMoveMode ? "Moving..." : "Copying...");
    tvFileName.setText(p.fileName);
    tvFrom.setText(p.fromDir);
    tvTo.setText(p.toDir);

    String size = formatSize(p.bytesCopied) + "/" + formatSize(p.totalBytes);
    tvRemaining.setText(p.remaining + " (" + size + ")");
    tvSpeed.setText(formatSize(p.speedBps) + "/s");

    if (p.totalBytes > 0) {
      int percent = (int) (p.bytesCopied * 100 / p.totalBytes);
      progressBar.setProgressCompat(percent, true);
    }
  }

  private String formatSize(long bytes) {
    if (bytes < 1024)                return bytes + " B";
    if (bytes < 1024 * 1024)         return String.format("%.1f KB", bytes / 1024.0);
    if (bytes < 1024L * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
    return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
  }
}
