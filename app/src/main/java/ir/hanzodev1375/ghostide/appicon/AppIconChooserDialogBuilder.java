package ir.hanzodev1375.ghostide.appicon;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import ir.hanzodev1375.ghostide.R;

/**
 * Builder for the "pick a launcher icon" dialog, mirroring ThemeChooserDialogBuilder's shape.
 *
 * <p>new AppIconChooserDialogBuilder(this) .setTitle(R.string.pref_app_icon)
 * .setPositiveButton(R.string.ok, icon -> AppIconManager.applyIcon(this, icon))
 * .setNegativeButton(R.string.cancel) .create() .show();
 */
public class AppIconChooserDialogBuilder {

  private final Context context;
  private MaterialAlertDialogBuilder builder;
  private AppIconAdapter iconAdapter;

  public AppIconChooserDialogBuilder(Context context) {
    this.context = context;
    createDialog();
  }

  private void createDialog() {
    View dialogView = LayoutInflater.from(context).inflate(R.layout.recyclerview, null);
    RecyclerView recyclerView = dialogView.findViewById(R.id.recycler_view);

    iconAdapter = new AppIconAdapter(AppIconManager.getCurrentIcon(context));
    recyclerView.setLayoutManager(new GridLayoutManager(context, 3));
    recyclerView.setAdapter(iconAdapter);

    builder = new MaterialAlertDialogBuilder(context).setView(dialogView);
  }

  public AppIconChooserDialogBuilder setTitle(@StringRes int res) {
    builder.setTitle(res);
    return this;
  }

  public AppIconChooserDialogBuilder setPositiveButton(String text, OnClickListener listener) {
    builder.setPositiveButton(
        text, (dialog, which) -> listener.onClick(iconAdapter.getCheckedIcon()));
    return this;
  }

  public AppIconChooserDialogBuilder setPositiveButton(
      @StringRes int res, OnClickListener listener) {
    return setPositiveButton(context.getString(res), listener);
  }

  public AppIconChooserDialogBuilder setNegativeButton(@StringRes int res) {
    builder.setNegativeButton(res, null);
    return this;
  }

  public AlertDialog create() {
    return builder.create();
  }

  public interface OnClickListener {
    void onClick(AppIcon icon);
  }
}
