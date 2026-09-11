package ninja.coder.appuploader.main;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import androidx.activity.result.ActivityResultLauncher;
import androidx.core.content.FileProvider;
import java.io.File;
import ir.hanzodev1375.components.sheet.customitemsheet.ui.DialogCompat;

/**
 * Installs an APK through the system package installer.
 *
 * <p>All functions must be called from the main/UI thread. A single {@link
 * ActivityResultLauncher} for {@link Settings#ACTION_MANAGE_UNKNOWN_APP_SOURCES}, registered once
 * by the hosting activity, is reused to ask for the "allow installs from this app" permission and
 * to resume the install automatically when the user comes back.
 */
public class ApkInstallerCompat {

  public static final String TAG = "ApkInstallerCompat";

  private final Activity mActivity;
  private final File mApkFile;
  private final ActivityResultLauncher<Intent> mInstallPermissionLauncher;
  private final Runnable mOnPermissionDenied;

  public ApkInstallerCompat(Activity activity, File apkFile) {
    this(activity, apkFile, null, null);
  }

  public ApkInstallerCompat(
      Activity activity,
      File apkFile,
      ActivityResultLauncher<Intent> installPermissionLauncher,
      Runnable onPermissionDenied) {
    mActivity = activity;
    mApkFile = apkFile;
    mInstallPermissionLauncher = installPermissionLauncher;
    mOnPermissionDenied = onPermissionDenied;
  }

  /** Must be called from the main/UI thread. */
  public void install() {
    if (mActivity == null || mApkFile == null) {
      return;
    }
    if (!mApkFile.getName().toLowerCase().endsWith(".apk") || !mApkFile.exists()) {
      showError("Error", "APK file not found:\n" + mApkFile.getAbsolutePath());
      return;
    }
    if (!isInstallingFromUnknownSourcesAllowed()) {
      requestUnknownSourcesPermission();
      return;
    }
    launchSystemInstaller();
  }

  private void launchSystemInstaller() {
    try {
      Uri apkUri =
          FileProvider.getUriForFile(
              mActivity, mActivity.getPackageName() + ".fileprovider", mApkFile);
      Intent intent = new Intent(Intent.ACTION_VIEW);
      intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
      intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
      mActivity.startActivity(intent);
    } catch (Exception e) {
      Log.e(TAG, "Error installing APK: ", e);
      showError("Installation Error", e.getMessage() == null ? "Unknown error" : e.getMessage());
    }
  }

  private void requestUnknownSourcesPermission() {
    Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
    intent.setData(Uri.parse("package:" + mActivity.getPackageName()));

    if (mInstallPermissionLauncher != null) {
      new DialogCompat(mActivity)
          .setTitle("Permission Required")
          .setMessage("Please allow installing apps from this source to continue")
          .setCancelable(false)
          .setPositiveButton(
              "Ok",
              (dialog, which) -> {
                dialog.dismiss();
                mInstallPermissionLauncher.launch(intent);
              })
          .setNegativeButton(
              "Cancel",
              (dialog, which) -> {
                dialog.dismiss();
                if (mOnPermissionDenied != null) {
                  mOnPermissionDenied.run();
                }
              })
          .show();
    } else {
      mActivity.startActivity(intent);
    }
  }

  private boolean isInstallingFromUnknownSourcesAllowed() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      return mActivity.getPackageManager().canRequestPackageInstalls();
    }
    return Settings.Secure.getInt(
            mActivity.getContentResolver(), Settings.Secure.INSTALL_NON_MARKET_APPS, 0)
        == 1;
  }

  private void showError(String title, String message) {
    new DialogCompat(mActivity)
        .setTitle(title)
        .setMessage(message)
        .setCancelable(false)
        .setPositiveButton("OK", null)
        .show();
  }
}