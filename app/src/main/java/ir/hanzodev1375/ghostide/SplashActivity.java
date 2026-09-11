package ir.hanzodev1375.ghostide;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.ImageView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import ir.hanzodev1375.ghostide.activity.BaseCompat;
import ir.hanzodev1375.ghostide.activity.FileManagerActivity;
import ir.hanzodev1375.ghostide.onboarding.OnboardingActivity;
import ir.hanzodev1375.ghostide.onboarding.OnboardingPrefs;
import ir.hanzodev1375.ghostide.utils.ObjectUtil;
import ir.hanzodev1375.ghostide.utils.PermissionUtils;
import ir.hanzodev1375.components.views.GhostToast;
import ir.theme.M3Theme;
import java.util.ArrayList;
import java.util.List;

public class SplashActivity extends BaseCompat {

  private static final int REQ_RUNTIME = 1001;
  private static final int REQ_NOTIFICATION = 2030;

  private ActivityResultLauncher<Intent> storageLauncher;
  private ActivityResultLauncher<Intent> installAppsLauncher;
  private boolean started = false;
  private boolean storageShown = false;
  private boolean installShown = false;
  private boolean runtimeShown = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_splash);

    storageLauncher =
        registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> handlePermissionsAndStart());

    installAppsLauncher =
        registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> handlePermissionsAndStart());

    ImageView logo = findViewById(R.id.logo);
    logo.setAlpha(0f);
    logo
        .animate()
        .alpha(1f)
        .setDuration(800)
        .withEndAction(() -> logo.postDelayed(this::handlePermissionsAndStart, 1200))
        .start();

    M3Theme.apply(findViewById(android.R.id.content));
  }

  private void handlePermissionsAndStart() {
    if (started) return;

    if (!PermissionUtils.hasManageStoragePermission(this)) {
      if (storageShown) {
        GhostToast.makeText(
                this,
                "برای استفاده از اپ، مجوز دسترسی به فایل‌ها لازم است",
                GhostToast.LENGTH_LONG)
            .show();
        finish();
        return;
      }
      storageShown = true;
      PermissionUtils.requestManageStoragePermission(this, storageLauncher);
      return;
    }

    if (!PermissionUtils.hasPermissions(this)) {
      PermissionUtils.requestPermissions(this);
      return;
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      boolean notified =
          ContextCompat.checkSelfPermission(
                  this, Manifest.permission.POST_NOTIFICATIONS)
              == PackageManager.PERMISSION_GRANTED;
      if (!notified) {
        requestPermissions(new String[] {Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFICATION);
        return;
      }
    }

    if (!getPackageManager().canRequestPackageInstalls()) {
      if (installShown) return;
      installShown = true;
      Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
      intent.setData(Uri.parse("package:" + getPackageName()));
      installAppsLauncher.launch(intent);
      return;
    }

    startFileManager();
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode, String[] permissions, int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == REQ_NOTIFICATION) handlePermissionsAndStart();
  }

  private void startFileManager() {
    if (started) return;
    started = true;
    ImageView logo = findViewById(R.id.logo);
    Intent intent;
    if (OnboardingPrefs.shouldShow(this)) {
      intent = new Intent(this, OnboardingActivity.class);
    } else {
      intent = new Intent(this, FileManagerActivity.class);
      startActivityWithSharedElement(intent, logo, ObjectUtil.TRANSITION_LOGO);
      finish();
      return;
    }
    startActivityWithSharedElement(intent, logo, ObjectUtil.TRANSITION_LOGO);
    finish();
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (!started) handlePermissionsAndStart();
  }
}