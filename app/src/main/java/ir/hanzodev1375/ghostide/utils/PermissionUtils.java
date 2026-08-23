package ir.hanzodev1375.ghostide.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class PermissionUtils {
  public static final int REQUEST_CODE = 1001;

  // برای اندروید 15 نیاز به مجوز مدیریت فایل کامل
  public static String[] getRequiredPermissions() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      return new String[] {
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VIDEO,
        Manifest.permission.READ_MEDIA_AUDIO
      };
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      return new String[] {Manifest.permission.READ_EXTERNAL_STORAGE};
    } else {
      return new String[] {
        Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE
      };
    }
  }

  // بررسی مجوز کامل مدیریت فایل برای اندروید 15
  public static boolean hasManageStoragePermission(Context context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      return Environment.isExternalStorageManager();
    }
    return true;
  }

  // درخواست مجوز کامل مدیریت فایل
  public static void requestManageStoragePermission(
      Activity activity, ActivityResultLauncher<Intent> launcher) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
      Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
      intent.setData(Uri.parse("package:" + activity.getPackageName()));
      launcher.launch(intent);
    }
  }

  public static void requestManageStoragePermission(Activity activity) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
      Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
      intent.setData(Uri.parse("package:" + activity.getPackageName()));
      activity.startActivity(intent);
    }
  }

  public static boolean hasPermissions(Context context) {
    for (String perm : getRequiredPermissions()) {
      if (ContextCompat.checkSelfPermission(context, perm) != PackageManager.PERMISSION_GRANTED)
        return false;
    }

    // برای اندروید 11+ نیاز به مجوز مدیریت فایل
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
      return false;
    }

    return true;
  }

  public static void requestPermissions(Activity activity) {
    List<String> missing = new ArrayList<>();
    for (String perm : getRequiredPermissions()) {
      if (ContextCompat.checkSelfPermission(activity, perm) != PackageManager.PERMISSION_GRANTED)
        missing.add(perm);
    }

    if (!missing.isEmpty()) {
      ActivityCompat.requestPermissions(activity, missing.toArray(new String[0]), REQUEST_CODE);
    }

    // درخواست مجوز مدیریت فایل
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
      Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
      intent.setData(Uri.parse("package:" + activity.getPackageName()));
      activity.startActivityForResult(intent, REQUEST_CODE + 1);
    }
  }
}
