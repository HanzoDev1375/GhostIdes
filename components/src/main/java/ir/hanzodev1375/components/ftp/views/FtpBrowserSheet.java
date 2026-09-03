package ir.hanzodev1375.components.ftp.views;

import android.app.Dialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import ir.hanzodev1375.components.views.GhostToast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import ir.hanzodev1375.components.sheet.BaseBlurBottomSheet;
import ir.hanzodev1375.components.utils.GlassColors;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.skydoves.powermenu.MenuAnimation;
import com.skydoves.powermenu.PowerMenu;
import com.skydoves.powermenu.PowerMenuItem;

import ir.hanzodev1375.components.ftp.interfaces.RemoteClient;
import ir.hanzodev1375.components.ftp.adapter.FtpBrowserAdapter;
import ir.hanzodev1375.components.ftp.model.FtpEntry;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ir.hanzodev1375.components.R;
import ir.hanzodev1375.components.sheet.customitemsheet.ui.DialogCompat;
public class FtpBrowserSheet extends BaseBlurBottomSheet {

  public static final String TAG = "FtpBrowserSheet";

  private RemoteClient client;
  private String host;
  private boolean isSftp;
  private String currentPath = "/";
  private final Stack<String> pathStack = new Stack<>();

  private FtpBrowserAdapter adapter;
  private LinearProgressIndicator progressBar;
  private TextView tvPath;
  private LinearLayout llEmpty;
  private RecyclerView rvFiles;

  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  public interface OnDownloadListener {
    void onDownload(String remotePath, String fileName);
  }

  private OnDownloadListener downloadListener;

  public void setOnDownloadListener(OnDownloadListener l) {
    downloadListener = l;
  }

  public static FtpBrowserSheet newInstance(RemoteClient client, String host, boolean isSftp) {
    FtpBrowserSheet sheet = new FtpBrowserSheet();
    sheet.client = client;
    sheet.host = host;
    sheet.isSftp = isSftp;
    return sheet;
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
    BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
    dialog.setOnShowListener(
        d -> {
          FrameLayout bottomSheet =
              ((BottomSheetDialog) d)
                  .findViewById(R.id.design_bottom_sheet);
          if (bottomSheet != null) {
            BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);
            bottomSheet.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
          }
        });
    return dialog;
  }

  @Override
  protected void onContentReady(@NonNull ViewGroup contentContainer) {
    View view = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_ftp_browser, null, false);
    contentContainer.addView(view);

    MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
    tvPath = view.findViewById(R.id.tvPath);
    GlassColors.setBackgroundAlpha(
        (View) tvPath.getParent(),
        MaterialColors.getColor(tvPath, R.attr.colorSurfaceVariant),
        150);
    progressBar = view.findViewById(R.id.progressBar);
    rvFiles = view.findViewById(R.id.rvFiles);
    llEmpty = view.findViewById(R.id.llEmpty);

    toolbar.setTitle((isSftp ? "SFTP" : "FTP") + " — " + host);
    toolbar.setNavigationOnClickListener(v -> dismiss());

    adapter = new FtpBrowserAdapter();
    rvFiles.setLayoutManager(new LinearLayoutManager(requireContext()));
    rvFiles.setAdapter(adapter);

    adapter.setOnItemClick(
        entry -> {
          if (entry.isDirectory()) {
            pathStack.push(currentPath);
            navigateTo(entry.getPath());
          }
        });

    adapter.setOnMoreClick((entry, anchor) -> showEntryMenu(entry, anchor));

    // back press
    toolbar.setOnMenuItemClickListener(item -> false);

    loadPath("/");
  }

  private void navigateTo(String path) {
    currentPath = path;
    loadPath(path);
  }

  private void loadPath(String path) {
    showProgress(true);
    tvPath.setText(path);
    executor.execute(
        () -> {
          try {
            List<FtpEntry> entries = client.listFiles(path);
            if (getActivity() == null) return;
            getActivity()
                .runOnUiThread(
                    () -> {
                      if (!isAdded()) return;
                      showProgress(false);
                      adapter.submitList(entries);
                      llEmpty.setVisibility(entries.isEmpty() ? View.VISIBLE : View.GONE);
                      rvFiles.setVisibility(entries.isEmpty() ? View.GONE : View.VISIBLE);
                    });
          } catch (Exception e) {
            if (getActivity() == null) return;
            getActivity()
                .runOnUiThread(
                    () -> {
                      if (!isAdded()) return;
                      showProgress(false);
                      GhostToast.makeText(
                              requireContext(),
                              getString(R.string.ftp_error_list, e.getMessage()),
                              GhostToast.LENGTH_SHORT)
                          .show();
                    });
          }
        });
  }

  private void showEntryMenu(FtpEntry entry, View anchor) {
    PowerMenu menu = new PowerMenu.Builder(anchor.getContext()).setIsMaterial(true).build();
    if (!entry.isDirectory()) {
      menu.addItem(new PowerMenuItem(getString(R.string.ftp_menu_download))); // 0
    }
    menu.addItem(new PowerMenuItem(getString(R.string.ftp_menu_delete))); // 1
    menu.addItem(new PowerMenuItem(getString(R.string.ftp_menu_rename))); // 2

    menu.setMenuColor(
        MaterialColors.getColor(
            anchor, R.attr.colorSurface, 0));
    menu.setTextColor(
      MaterialColors.getColor(
            anchor,R.attr.colorOnSurface, 0));
    menu.setShowBackground(false);
    menu.setAutoDismiss(true);
    menu.setMenuRadius(30f);
    menu.setAnimation(MenuAnimation.FADE);

    menu.setOnMenuItemClickListener(
        (index, item) -> {
          if (!entry.isDirectory()) {
            if (index == 0) downloadEntry(entry);
            else if (index == 1) confirmDelete(entry);
            else if (index == 2) showRenameDialog(entry);
          } else {
            if (index == 0) confirmDelete(entry);
            else if (index == 1) showRenameDialog(entry);
          }
        });

    int[] loc = new int[2];
    anchor.getLocationOnScreen(loc);
    menu.showAtLocation(
        anchor,
        Gravity.TOP | Gravity.START,
        loc[0],
        loc[1] + anchor.getHeight());
  }

  private void downloadEntry(FtpEntry entry) {
    if (downloadListener != null) {
      downloadListener.onDownload(entry.getPath(), entry.getName());
    }
  }

  private void confirmDelete(FtpEntry entry) {
    new DialogCompat(requireContext())
        .setTitle(getString(R.string.ftp_menu_delete))
        .setMessage(getString(R.string.ftp_delete_confirm, entry.getName()))
        .setPositiveButton(
            getString(android.R.string.ok),
            (d, w) -> {
              showProgress(true);
              executor.execute(
                  () -> {
                    try {
                      client.delete(entry.getPath());
                      if (getActivity() != null) {
                        getActivity()
                            .runOnUiThread(
                                () -> {
                                  if (!isAdded()) return;
                                  showProgress(false);
                                  loadPath(currentPath);
                                });
                      }
                    } catch (Exception e) {
                      if (getActivity() != null) {
                        getActivity()
                            .runOnUiThread(
                                () -> {
                                  if (!isAdded()) return;
                                  showProgress(false);
                                  GhostToast.makeText(
                                          requireContext(),
                                          getString(R.string.ftp_error_delete, e.getMessage()),
                                          GhostToast.LENGTH_SHORT)
                                      .show();
                                });
                      }
                    }
                  });
            })
        .setNegativeButton(getString(android.R.string.cancel), null)
        .show();
  }

  private void showRenameDialog(FtpEntry entry) {
    android.widget.EditText et = new android.widget.EditText(requireContext());
    et.setText(entry.getName());
    et.setSelectAllOnFocus(true);

    new DialogCompat(requireContext())
        .setTitle(getString(R.string.ftp_menu_rename))
        .setView(et)
        .setPositiveButton(
            getString(android.R.string.ok),
            (d, w) -> {
              String newName = et.getText().toString().trim();
              if (newName.isEmpty()) return;
              String parentPath = currentPath.endsWith("/") ? currentPath : currentPath + "/";
              String newPath = parentPath + newName;
              showProgress(true);
              executor.execute(
                  () -> {
                    try {
                      client.rename(entry.getPath(), newPath);
                      if (getActivity() != null) {
                        getActivity()
                            .runOnUiThread(
                                () -> {
                                  if (!isAdded()) return;
                                  showProgress(false);
                                  loadPath(currentPath);
                                });
                      }
                    } catch (Exception e) {
                      if (getActivity() != null) {
                        getActivity()
                            .runOnUiThread(
                                () -> {
                                  if (!isAdded()) return;
                                  showProgress(false);
                                  GhostToast.makeText(
                                          requireContext(),
                                          getString(R.string.ftp_error_rename, e.getMessage()),
                                          GhostToast.LENGTH_SHORT)
                                      .show();
                                });
                      }
                    }
                  });
            })
        .setNegativeButton(getString(android.R.string.cancel), null)
        .show();
  }

  public boolean onBackPressed() {
    if (!pathStack.isEmpty()) {
      currentPath = pathStack.pop();
      loadPath(currentPath);
      return true;
    }
    return false;
  }

  private void showProgress(boolean show) {
    if (progressBar != null) progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    executor.execute(
        () -> {
          if (client != null) client.disconnect();
        });
    executor.shutdownNow();
  }

  public void show(FragmentManager manager) {
    show(manager, TAG);
  }
}
