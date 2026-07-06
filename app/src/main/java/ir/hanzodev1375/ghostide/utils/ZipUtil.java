package ir.hanzodev1375.ghostide.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import ir.hanzodev1375.ghostide.R;
import ir.hanzodev1375.ghostide.activity.FileManagerActivity;

public class ZipUtil {

  private static final ExecutorService executor = Executors.newFixedThreadPool(2);
  private static final Handler mainHandler = new Handler(Looper.getMainLooper());
  private static AlertDialog progressDialog;
  private static ProgressBar progressBar;
  private static TextView tvFileName, tvPercent, tvDetails;
  private static String defaultName = "archive.zip";

  // Order must match the labels set on zipspinner in showZipDialog()
  private static final CompressionLevel[] COMPRESSION_LEVELS = {
    CompressionLevel.FASTEST, CompressionLevel.NORMAL, CompressionLevel.MAXIMUM
  };

  private static CompressionLevel getSelectedCompressionLevel(Spinner spinner) {
    int position = spinner.getSelectedItemPosition();
    if (position < 0 || position >= COMPRESSION_LEVELS.length) {
      return CompressionLevel.NORMAL;
    }
    return COMPRESSION_LEVELS[position];
  }

  private static long calculateTotalSize(List<File> files) {
    long total = 0;
    for (File f : files) {
      total += getSize(f);
    }
    return total;
  }

  private static long getSize(File f) {
    if (f.isFile()) return f.length();
    long size = 0;
    File[] children = f.listFiles();
    if (children != null) {
      for (File child : children) {
        size += getSize(child);
      }
    }
    return size;
  }

  private static void showProgressDialog(Context context, long totalBytes) {
    View view = LayoutInflater.from(context).inflate(R.layout.dialog_zip_progress, null);
    progressBar = view.findViewById(R.id.progressBar);
    tvFileName = view.findViewById(R.id.tvZipFileName);
    tvPercent = view.findViewById(R.id.tvZipPercent);
    tvDetails = view.findViewById(R.id.tvZipDetails);
    progressBar.setMax(100);
    progressDialog =
        new MaterialAlertDialogBuilder(context)
            .setTitle(R.string.creating_zip)
            .setView(view)
            .setCancelable(false)
            .create();
    progressDialog.show();
    String totalStr = formatSize(totalBytes);
    tvFileName.setText(R.string.preparing);
    tvPercent.setText("0%");
    tvDetails.setText(String.format(Locale.getDefault(), "0 / %s", totalStr));
  }

  private static void updateProgress(String fileName, long processedBytes, long totalBytes) {
    if (progressDialog == null || !progressDialog.isShowing()) return;
    int percent = (int) (processedBytes * 100 / totalBytes);
    progressBar.setProgress(percent);
    tvPercent.setText(percent + "%");
    tvFileName.setText(fileName);
    String processedStr = formatSize(processedBytes);
    String totalStr = formatSize(totalBytes);
    tvDetails.setText(String.format(Locale.getDefault(), "%s / %s", processedStr, totalStr));
  }

  private static String formatSize(long bytes) {
    if (bytes >= 1024 * 1024 * 1024) {
      return String.format(Locale.getDefault(), "%.2f GB", bytes / (1024.0 * 1024 * 1024));
    } else if (bytes >= 1024 * 1024) {
      return String.format(Locale.getDefault(), "%.2f MB", bytes / (1024.0 * 1024));
    } else if (bytes >= 1024) {
      return String.format(Locale.getDefault(), "%.2f KB", bytes / 1024.0);
    } else {
      return bytes + " B";
    }
  }

  private static void dismissProgressDialog() {
    if (progressDialog != null && progressDialog.isShowing()) {
      progressDialog.dismiss();
      progressDialog = null;
    }
  }

  private static void zipFilesWithProgress(
      List<File> sources,
      File destination,
      String password,
      Context context,
      Runnable onSuccess,
      CompressionLevel compressionLevel) {
    executor.execute(
        () -> {
          try {
            long totalBytes = calculateTotalSize(sources);
            mainHandler.post(() -> showProgressDialog(context, totalBytes));

            ZipParameters parameters = new ZipParameters();
            parameters.setCompressionMethod(CompressionMethod.DEFLATE);
            parameters.setCompressionLevel(compressionLevel);
            if (password != null && !password.isEmpty()) {
              parameters.setEncryptFiles(true);
              parameters.setEncryptionMethod(EncryptionMethod.AES);
              parameters.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);
            }

            try (ZipFile zipFile =
                new ZipFile(destination, password != null ? password.toCharArray() : null)) {
              AtomicLong processedBytes = new AtomicLong(0);
              for (File src : sources) {
                addToZip(zipFile, src, parameters, processedBytes, totalBytes);
              }
            }

            mainHandler.post(
                () -> {
                  dismissProgressDialog();
                  if (onSuccess != null) onSuccess.run();
                  if (context instanceof FileManagerActivity) {
                    ((FileManagerActivity) context).refreshFileList();
                  }
                });

          } catch (Exception e) {
            mainHandler.post(
                () -> {
                  dismissProgressDialog();
                  Toast.makeText(
                          context,
                          context.getString(R.string.zip_error) + e.getMessage(),
                          Toast.LENGTH_LONG)
                      .show();
                });
          }
        });
  }

  private static void addToZip(
      ZipFile zipFile, File file, ZipParameters params, AtomicLong processed, long total)
      throws Exception {
    if (file.isDirectory()) {
      File[] children = file.listFiles();
      if (children != null) {
        for (File child : children) {
          addToZip(zipFile, child, params, processed, total);
        }
      }
    } else {
      zipFile.addFile(file, params);
      long added = file.length();
      long newProcessed = processed.addAndGet(added);
      mainHandler.post(() -> updateProgress(file.getName(), newProcessed, total));
    }
  }

  public static void showZipDialog(Context context, List<File> selectedFiles) {
    if (selectedFiles == null || selectedFiles.isEmpty()) {
      Toast.makeText(context, R.string.no_items_selected, Toast.LENGTH_SHORT).show();
      return;
    }

    View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_create_zip, null);
    TextInputLayout tilName = dialogView.findViewById(R.id.til_zip_name);
    TextInputEditText etName = dialogView.findViewById(R.id.et_zip_name);
    TextInputLayout tilPassword = dialogView.findViewById(R.id.til_zip_password);
    TextInputEditText etPassword = dialogView.findViewById(R.id.et_zip_password);
    SwitchMaterial swPassword = dialogView.findViewById(R.id.sw_zip_password);
    Spinner zipspinner = dialogView.findViewById(R.id.zipspinner);
    ArrayAdapter<String> compressionAdapter =
        new ArrayAdapter<>(
            context,
            android.R.layout.simple_spinner_item,
            new String[] {"سریع‌ترین", "عادی", "حداکثر"});
    compressionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    zipspinner.setAdapter(compressionAdapter);
    zipspinner.setSelection(1); // default: NORMAL

    tilPassword.setVisibility(View.GONE);
    swPassword.setOnCheckedChangeListener(
        (btn, isChecked) -> {
          tilPassword.setVisibility(isChecked ? View.VISIBLE : View.GONE);
          if (!isChecked) etPassword.setText("");
        });

    if (selectedFiles.size() == 1) {
      String name = selectedFiles.get(0).getName();
      int dot = name.lastIndexOf('.');
      if (dot > 0) name = name.substring(0, dot);
      defaultName = name + ".zip";
    }
    etName.setText(defaultName);

    MaterialAlertDialogBuilder builder =
        new MaterialAlertDialogBuilder(context)
            .setTitle(R.string.create_zip_title)
            .setView(dialogView)
            .setPositiveButton(R.string.create, null)
            .setNegativeButton(R.string.cancel, null);

    AlertDialog dialog = builder.create();
    dialog.show();
    dialog
        .getButton(AlertDialog.BUTTON_POSITIVE)
        .setOnClickListener(
            v -> {
              String zipName = etName.getText().toString().trim();
              if (zipName.isEmpty()) zipName = defaultName;
              if (!zipName.endsWith(".zip")) zipName += ".zip";
              File parentDir = selectedFiles.get(0).getParentFile();
              File zipFile = new File(parentDir, zipName);
              String password = swPassword.isChecked() ? etPassword.getText().toString() : null;
              if (password != null && password.isEmpty()) password = null;
              dialog.dismiss();
              zipFilesWithProgress(
                  selectedFiles,
                  zipFile,
                  password,
                  context,
                  () -> {
                    Toast.makeText(context, R.string.zip_created_success, Toast.LENGTH_LONG).show();
                  },
                  getSelectedCompressionLevel(zipspinner));
            });
  }
}
