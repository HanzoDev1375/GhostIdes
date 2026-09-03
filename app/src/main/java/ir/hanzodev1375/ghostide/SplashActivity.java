package ir.hanzodev1375.ghostide;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import ir.hanzodev1375.ghostide.activity.BaseCompat;
import ir.hanzodev1375.ghostide.activity.FileManagerActivity;
import ir.hanzodev1375.ghostide.utils.ObjectUtil;
import ir.hanzodev1375.ghostide.utils.PermissionUtils;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import ir.hanzodev1375.components.views.GhostToast;

public class SplashActivity extends BaseCompat {

  private ActivityResultLauncher<Intent> manageStorageLauncher;
  private boolean started = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_splash);

    manageStorageLauncher =
        registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
              if (PermissionUtils.hasManageStoragePermission(this)) startFileManager();
              else {
                GhostToast.makeText(this, "برای مدیریت فایل به مجوز نیاز است", GhostToast.LENGTH_LONG).show();
                finish();
              }
            });
    ImageView logo = findViewById(R.id.logo);
    logo.setAlpha(0f);
    logo.animate()
        .alpha(1f)
        .setDuration(800)
        .withEndAction(() -> logo.postDelayed(this::checkPermissionsAndStart, 1200))
        .start();
  }

  private void checkPermissionsAndStart() {
    if (!PermissionUtils.hasPermissions(this)) {
      PermissionUtils.requestPermissions(this);
      PermissionUtils.requestManageStoragePermission(this, manageStorageLauncher);
    } else {
      startFileManager();
    }
  }

  private void startFileManager() {
    if (started) return;
    started = true;
    ImageView logo = findViewById(R.id.logo);
    Intent intent = new Intent(this, FileManagerActivity.class);
    startActivityWithSharedElement(intent, logo, ObjectUtil.TRANSITION_LOGO);
    finish();
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (!started
        && PermissionUtils.hasPermissions(this)
        && PermissionUtils.hasManageStoragePermission(this)) startFileManager();
  }
}
