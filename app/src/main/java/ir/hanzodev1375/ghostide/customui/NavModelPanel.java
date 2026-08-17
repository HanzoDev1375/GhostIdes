package ir.hanzodev1375.ghostide.customui;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.skydoves.powermenu.PowerMenu;
import com.skydoves.powermenu.PowerMenuItem;
import ir.hanzodev1375.components.TextInputDialogFragment;
import ir.hanzodev1375.ghostide.adapters.NavAdapter;
import ir.hanzodev1375.ghostide.models.NavModel;
import ir.hanzodev1375.ghostide.R;
import ir.hanzodev1375.ghostide.utils.ObjectUtil;
import ir.hanzodev1375.ghostide.utils.StorageUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NavModelPanel extends RecyclerView {

  public static final String STORAGE_EMULATED =
      File.separator + "storage" + File.separator + "emulated";
  public static final String STORAGE_EMULATED_0 = STORAGE_EMULATED + File.separator + "0";

  /** وقتی کاربر می‌خواد به یک مسیر دلخواه بره صدا زده می‌شه. */
  public interface OnNavigateListener {
    void onNavigate(String path);
  }

  private final List<NavModel> breadCrumbs = new ArrayList<>();
  private NavAdapter adapter;
  private boolean visible;
  private OnNavigateListener onNavigateListener;

  public NavModelPanel(Context context) {
    this(context, null);
  }

  public NavModelPanel(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public NavModelPanel(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    init(context);
  }

  private void init(Context context) {
    adapter = new NavAdapter();
    setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
    setAdapter(adapter);
    visible = true;
    adapter.setOnItemLongClickListener(
        (view, model, pos) -> {
          PowerMenu menu = ObjectUtil.stepMenu(view.getContext(), view);
          menu.addItem(new PowerMenuItem(getContext().getString(R.string.goto_dir)));
          menu.setOnMenuItemClickListener(
              (index, item) -> {
                if (index == 0) showGoToDirDialog();
              });
          ObjectUtil.showFixPos(menu, view);
          return false;
        });
  }

  public void setOnNavigateListener(OnNavigateListener listener) {
    this.onNavigateListener = listener;
  }

  /** دیالوگ «رفتن به مسیر» — کاربر هر مسیری خواست تایپ می‌کنه و فایل‌منیجر می‌ره. */
  public void showGoToDirDialog() {
    Context context = getContext();
    if (!(context instanceof FragmentActivity)) return;
    TextInputDialogFragment.newInstance(
            context.getString(R.string.goto_dir),
            context.getString(R.string.goto_dir_hint),
            currentPath())
        .setCallback(this::navigateToPath)
        .show(((FragmentActivity) context).getSupportFragmentManager(), "goto_dir");
  }

  private void navigateToPath(String path) {
    String trimmed = path == null ? "" : path.trim();
    if (trimmed.isEmpty()) return;
    File file = new File(trimmed);
    if (!file.exists()) {
      Toast.makeText(
              getContext(),
              getContext().getString(R.string.goto_dir_not_found, trimmed),
              Toast.LENGTH_SHORT)
          .show();
      return;
    }
    if (!file.isDirectory()) {
      Toast.makeText(
              getContext(),
              getContext().getString(R.string.goto_dir_not_directory, trimmed),
              Toast.LENGTH_SHORT)
          .show();
      return;
    }
    if (onNavigateListener != null) onNavigateListener.onNavigate(file.getAbsolutePath());
  }

  private String currentPath() {
    if (breadCrumbs.isEmpty()) return null;
    return breadCrumbs.get(breadCrumbs.size() - 1).getFilePath();
  }

  public NavAdapter getAdapter() {
    return this.adapter;
  }

  public void setFile(File file) {
    if (visible && file != null) {
      breadCrumbs.clear();

      List<StorageUtils.StorageEntry> volumes = StorageUtils.getStorageVolumes(getContext());

      while (file != null) {
        if (file.getPath().equals(STORAGE_EMULATED)) {
          break;
        }

        var breadCrumb = NavModel.fileTonav(file);

        if (breadCrumb != null) {
          String volumeLabel = labelForVolume(volumes, breadCrumb.getFilePath());
          if (breadCrumb.getFilePath().equals(STORAGE_EMULATED_0)) {
            breadCrumb.setName(getDeviceStorageName());
          } else if (volumeLabel != null) {
            breadCrumb.setName(volumeLabel);
          }
          breadCrumbs.add(breadCrumb);
          if (volumeLabel != null) {
            // Reached a storage volume root (internal or SD card); stop climbing further.
            break;
          }
          file = file.getParentFile();
        }
      }

      Collections.reverse(breadCrumbs);
      adapter.notifyDataSetChanged();
      adapter.submitList(breadCrumbs);
      scrollToPosition(adapter.getItemCount() - 1);
    }
  }

  private String labelForVolume(List<StorageUtils.StorageEntry> volumes, String path) {
    for (var entry : volumes) {
      if (entry.path.equals(path)) {
        return entry.removable ? getContext().getString(R.string.sd_card_label) : null;
      }
    }
    return null;
  }

  public void setVisible(boolean enabled) {
    setVisibility(enabled ? View.VISIBLE : View.GONE);
    this.visible = enabled;
  }

  String getDeviceStorageName() {
    String manufacturer = Build.MANUFACTURER;
    String model = Build.MODEL;

    if (model.startsWith(manufacturer)) {
      return capitalize(model);
    } else {
      return capitalize(manufacturer) + " " + model;
    }
  }

  private String capitalize(String s) {
    if (s == null || s.isEmpty()) {
      return "";
    }
    char first = s.charAt(0);
    if (Character.isUpperCase(first)) {
      return s;
    } else {
      return Character.toUpperCase(first) + s.substring(1);
    }
  }
}
