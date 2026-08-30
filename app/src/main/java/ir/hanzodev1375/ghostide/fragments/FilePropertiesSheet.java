package ir.hanzodev1375.ghostide.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import com.blankj.utilcode.util.ThreadUtils;
import com.blankj.utilcode.util.ThreadUtils.SimpleTask;
import com.google.android.material.color.MaterialColors;
import ir.hanzodev1375.components.sheet.BaseBlurBottomSheet;
import ir.hanzodev1375.components.utils.GlassColors;
import ir.hanzodev1375.ghostide.R;
import ir.hanzodev1375.ghostide.databinding.BottomSheetFilePropertiesBinding;
import ir.hanzodev1375.ghostide.databinding.ItemPropsRowBinding;
import ir.hanzodev1375.ghostide.models.FileManagerModel;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;

public class FilePropertiesSheet extends BaseBlurBottomSheet {

  public static final String TAG = "FilePropertiesSheet";

  private static final String KEY_PATHS = "key_paths";
  private static final String KEY_NAMES = "key_names";
  private static final String KEY_MODIFIED = "key_modified";

  private BottomSheetFilePropertiesBinding binding;
  private final List<SimpleTask<?>> runningTasks = new ArrayList<>();

  public static FilePropertiesSheet newInstance(@NonNull List<FileManagerModel> items) {
    int size = items.size();
    String[] paths = new String[size];
    String[] names = new String[size];
    long[] modified = new long[size];

    for (int i = 0; i < size; i++) {
      paths[i] = items.get(i).getPath();
      names[i] = items.get(i).getName();
      modified[i] = items.get(i).getLastModified();
    }

    Bundle args = new Bundle();
    args.putStringArray(KEY_PATHS, paths);
    args.putStringArray(KEY_NAMES, names);
    args.putLongArray(KEY_MODIFIED, modified);

    FilePropertiesSheet sheet = new FilePropertiesSheet();
    sheet.setArguments(args);
    return sheet;
  }

  @Override
  protected void onContentReady(@NonNull ViewGroup contentContainer) {
    binding = BottomSheetFilePropertiesBinding.inflate(LayoutInflater.from(requireContext()));
    contentContainer.addView(binding.getRoot());

    String[] paths = requireArguments().getStringArray(KEY_PATHS);
    String[] names = requireArguments().getStringArray(KEY_NAMES);
    long[] modified = requireArguments().getLongArray(KEY_MODIFIED);

    if (paths == null || paths.length == 0) {
      dismiss();
      return;
    }

    binding.btnClose.setOnClickListener(v -> dismiss());

    int cardBg =
        MaterialColors.getColor(binding.getRoot(), com.google.android.material.R.attr.colorSurfaceContainerHigh);
    for (int cardId : new int[] {R.id.card_single, R.id.card_system, R.id.card_analysis, R.id.card_multi}) {
      View card = binding.getRoot().findViewById(cardId);
      if (card != null) GlassColors.setBackgroundAlpha(card, cardBg, 150);
    }

    if (paths.length == 1) {
      bindSingle(paths[0], names[0], modified[0]);
    } else {
      bindMulti(paths);
    }
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    for (SimpleTask<?> task : runningTasks) {
      ThreadUtils.cancel(task);
    }
    runningTasks.clear();
    binding = null;
  }

  private void bindSingle(@NonNull String path, @NonNull String name, long lastModified) {
    binding.tvSheetTitle.setText(R.string.props_title_single);
    binding.layoutSingle.setVisibility(View.VISIBLE);
    binding.layoutMulti.setVisibility(View.GONE);
    File file = new File(path);
    bindRow(binding.rowName.getRoot(), R.drawable.outline_badge, R.string.props_label_name, name);

    bindRow(
        binding.rowLocation.getRoot(),
        R.drawable.ic_folder_open,
        R.string.props_label_location,
        file.getParent() != null ? file.getParent() : "-");

    bindRow(
        binding.rowType.getRoot(),
        R.drawable.ic_fileicon,
        R.string.props_label_type,
        file.isDirectory()
            ? getString(R.string.props_type_folder)
            : getString(R.string.props_type_file, getExtension(name)));

    bindRow(
        binding.rowModified.getRoot(),
        R.drawable.outline_shield,
        R.string.props_label_modified,
        new SimpleDateFormat("yyyy/MM/dd  HH:mm:ss", Locale.getDefault())
            .format(new Date(lastModified)));

    bindRow(
        binding.rowOwner.getRoot(),
        R.drawable.outline_person,
        R.string.props_label_owner,
        resolveOwner(path));

    bindRow(
        binding.rowPermissions.getRoot(),
        R.drawable.outline_shield,
        R.string.props_label_permissions,
        resolvePermissions(file));
    String calculating = getString(R.string.props_calculating);
    bindRow(
        binding.rowSize.getRoot(),
        R.drawable.outline_storage,
        R.string.props_label_size,
        calculating);
    bindRow(
        binding.rowContents.getRoot(),
        R.drawable.folder,
        R.string.props_label_contents,
        calculating);

    // ── تسک سایز: روی IO ThreadPool ──
    SimpleTask<String> sizeTask =
        new SimpleTask<String>() {
          @Override
          public String doInBackground() {
            return formatSize(file.isDirectory() ? getFolderSize(file) : file.length());
          }

          @Override
          public void onSuccess(String result) {
            if (binding == null) return;
            bindRow(
                binding.rowSize.getRoot(),
                R.drawable.outline_storage,
                R.string.props_label_size,
                result);
          }
        };

    AtomicInteger liveFiles = new AtomicInteger(0);
    AtomicInteger liveDirs = new AtomicInteger(0);

    SimpleTask<String> countTask =
        new SimpleTask<String>() {
          @Override
          public String doInBackground() {
            if (!file.isDirectory()) return null;
            countContentsLive(
                file,
                liveFiles,
                liveDirs,
                (f, d) -> {
                  if (binding == null) return;
                  String live = getString(R.string.props_calculating, f, d);
                  ThreadUtils.runOnUiThread(
                      () -> {
                        if (binding == null) return;
                        bindRow(
                            binding.rowContents.getRoot(),
                            R.drawable.folder,
                            R.string.props_label_contents,
                            live);
                      });
                });
            return getString(R.string.props_contents_format, liveFiles.get(), liveDirs.get());
          }

          @Override
          public void onSuccess(String result) {
            if (binding == null) return;
            String final_ =
                result != null ? result : getString(R.string.props_contents_format, 1, 0);
            bindRow(
                binding.rowContents.getRoot(),
                R.drawable.folder,
                R.string.props_label_contents,
                final_);
          }
        };

    runningTasks.add(sizeTask);
    runningTasks.add(countTask);
    ThreadUtils.executeByIo(sizeTask);
    ThreadUtils.executeByIo(countTask);
  }

  private void bindMulti(@NonNull String[] paths) {
    binding.tvSheetTitle.setText(R.string.props_title_multi);
    binding.layoutSingle.setVisibility(View.GONE);
    binding.layoutMulti.setVisibility(View.VISIBLE);

    String calculating = getString(R.string.props_calculating);

    bindRow(
        binding.rowSelectedCount.getRoot(),
        R.drawable.outline_check_box,
        R.string.props_label_selected,
        getString(R.string.props_selected_count, paths.length));

    bindRow(
        binding.rowTotalSize.getRoot(),
        R.drawable.outline_storage,
        R.string.props_label_total_size,
        calculating);

    bindRow(
        binding.rowTotalContents.getRoot(),
        R.drawable.ic_folder_open,
        R.string.props_label_total_contents,
        calculating);
    AtomicLong totalBytes = new AtomicLong(0L);
    AtomicInteger fileCount = new AtomicInteger(0);
    AtomicInteger dirCount = new AtomicInteger(0);
    AtomicInteger doneCount = new AtomicInteger(0);
    int total = paths.length;
    for (String p : paths) {
      SimpleTask<Void> task =
          new SimpleTask<Void>() {
            @Override
            public Void doInBackground() {
              File f = new File(p);
              if (f.isDirectory()) {
                totalBytes.addAndGet(getFolderSize(f));
                dirCount.incrementAndGet();
              } else {
                totalBytes.addAndGet(f.length());
                fileCount.incrementAndGet();
              }
              return null;
            }

            @Override
            public void onSuccess(Void result) {
              if (doneCount.incrementAndGet() == total && binding != null) {
                bindRow(
                    binding.rowTotalSize.getRoot(),
                    R.drawable.outline_storage,
                    R.string.props_label_total_size,
                    formatSize(totalBytes.get()));
                bindRow(
                    binding.rowTotalContents.getRoot(),
                    R.drawable.ic_folder_open,
                    R.string.props_label_total_contents,
                    getString(R.string.props_contents_format, fileCount.get(), dirCount.get()));
              }
            }
          };
      runningTasks.add(task);
      ThreadUtils.executeByIo(task);
    }
  }

  private void bindRow(@NonNull View rowView, int iconRes, int labelRes, @NonNull String value) {
    ItemPropsRowBinding row = ItemPropsRowBinding.bind(rowView);
    row.ivRowIcon.setImageResource(iconRes);
    row.tvRowLabel.setText(labelRes);
    row.tvRowValue.setText(value);
  }

  private long getFolderSize(@NonNull File dir) {
    File[] children = dir.listFiles();
    if (children == null) return 0L;
    long size = 0L;
    for (File child : children) {
      size += child.isDirectory() ? getFolderSize(child) : child.length();
    }
    return size;
  }

  interface CountProgressCallback {
    void onProgress(int files, int dirs);
  }

  // هر بار که یه آیتم شمرده میشه callback صدا زده میشه → UI آپدیت میشه
  private void countContentsLive(
      @NonNull File dir,
      @NonNull AtomicInteger files,
      @NonNull AtomicInteger dirs,
      @NonNull CountProgressCallback cb) {
    File[] children = dir.listFiles();
    if (children == null) return;
    for (File child : children) {
      if (child.isDirectory()) {
        dirs.incrementAndGet();
        cb.onProgress(files.get(), dirs.get());
        countContentsLive(child, files, dirs, cb);
      } else {
        files.incrementAndGet();
        cb.onProgress(files.get(), dirs.get());
      }
    }
  }

  @NonNull
  private String formatSize(long bytes) {
    if (bytes <= 0L) return "0 B";
    String[] units = {"B", "KiB", "MiB", "GiB", "TiB"};
    int idx = Math.min((int) (Math.log(bytes) / Math.log(1024)), units.length - 1);
    return new DecimalFormat("0.#").format(bytes / Math.pow(1024, idx)) + " " + units[idx];
  }

  @NonNull
  private String getExtension(@NonNull String name) {
    int dot = name.lastIndexOf('.');
    return (dot > 0 && dot < name.length() - 1)
        ? name.substring(dot + 1).toUpperCase(Locale.getDefault())
        : "-";
  }

  @NonNull
  private String resolvePermissions(@NonNull File file) {
    StringBuilder sb = new StringBuilder();
    if (file.canRead()) sb.append('r');
    if (file.canWrite()) sb.append('w');
    if (file.canExecute()) sb.append('x');
    return sb.length() > 0 ? sb.toString() : "-";
  }

  @NonNull
  private String resolveOwner(@NonNull String path) {
    try {
      var view = Files.getFileAttributeView(Paths.get(path), FileOwnerAttributeView.class);
      if (view != null) return view.getOwner().getName();
    } catch (Exception ignored) {
    }
    return "u0_a" + (android.os.Process.myUid() - 10000);
  }
}
