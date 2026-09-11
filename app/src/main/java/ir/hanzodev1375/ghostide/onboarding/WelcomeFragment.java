package ir.hanzodev1375.ghostide.onboarding;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.card.MaterialCardView;
import ir.hanzodev1375.ghostide.R;
import ir.hanzodev1375.ghostide.databinding.FragmentWelcomeBinding;
import ir.hanzodev1375.ghostide.databinding.ItemOnboardingPermissionBinding;
import ir.hanzodev1375.ghostide.utils.PermissionUtils;
import ir.theme.M3Theme;

public class WelcomeFragment extends Fragment {

  private static final int REQ_NOTIFICATION = 2030;

  private FragmentWelcomeBinding binding;
  private ActivityResultLauncher<Intent> storageLauncher;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding = FragmentWelcomeBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    styleColors();

    bindRow(
        binding.rowFiles,
        R.drawable.ic_onboarding_perm_files,
        R.string.onboarding_perm_files_title,
        R.string.onboarding_perm_files_desc,
        this::requestFiles);
    bindRow(
        binding.rowNotifications,
        R.drawable.ic_onboarding_perm_notifications,
        R.string.onboarding_perm_notifications_title,
        R.string.onboarding_perm_notifications_desc,
        this::requestNotifications);
    bindRow(
        binding.rowInstall,
        R.drawable.ic_onboarding_perm_install,
        R.string.onboarding_perm_install_title,
        R.string.onboarding_perm_install_desc,
        this::requestInstallApps);

    storageLauncher =
        registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> refreshGranted());
    refreshGranted();
  }

  @Override
  public void onResume() {
    super.onResume();
    if (binding != null) {
      refreshGranted();
      animateIn();
    }
  }

  private void animateIn() {
    OnboardingAnim.scaleIn(binding.cardIcon, 0);
    OnboardingAnim.slideUpFade(binding.tvHeader, 150);
    OnboardingAnim.slideUpFade(binding.tvDesc, 250);
    OnboardingAnim.revealCard(binding.cardPermissions, 350);
    OnboardingAnim.dividerExpand(binding.divider1, 480);
    OnboardingAnim.dividerExpand(binding.divider2, 560);
    OnboardingAnim.slideRightFade(binding.rowFiles.getRoot(), 420);
    OnboardingAnim.slideRightFade(binding.rowNotifications.getRoot(), 520);
    OnboardingAnim.slideRightFade(binding.rowInstall.getRoot(), 620);
  }

  private void styleColors() {
    int fallback = 0xFF202020;
    tintCard(binding.cardIcon, fallback(M3Theme.surfaceContainerHigh(), fallback));
    tintIcon(binding.ivIcon, fallback(M3Theme.primary(), fallback));
    binding.tvHeader.setTextColor(fallback(M3Theme.onSurface(), fallback));
    binding.tvDesc.setTextColor(fallback(M3Theme.onSurfaceVariant(), fallback));
    tintCard(binding.cardPermissions, fallback(M3Theme.surfaceContainerLow(), fallback));
    binding.divider1.setBackgroundColor(fallback(M3Theme.outlineVariant(), fallback));
    binding.divider2.setBackgroundColor(fallback(M3Theme.outlineVariant(), fallback));
  }

  private void bindRow(
      ItemOnboardingPermissionBinding row, int iconRes, int titleRes, int descRes, Runnable action) {
    int fallback = 0xFF202020;
    row.ivPermIcon.setImageResource(iconRes);
    tintIcon(row.ivPermIcon, fallback(M3Theme.onPrimaryContainer(), fallback));
    tintCard(row.permIconContainer, fallback(M3Theme.primaryContainer(), fallback));
    row.tvPermTitle.setText(titleRes);
    row.tvPermTitle.setTextColor(fallback(M3Theme.onSurface(), fallback));
    row.tvPermDesc.setText(descRes);
    row.tvPermDesc.setTextColor(fallback(M3Theme.onSurfaceVariant(), fallback));
    row.btnGrant.setOnClickListener(v -> action.run());
  }

  private void refreshGranted() {
    int fallback = 0xFF202020;
    setGranted(binding.rowFiles, PermissionUtils.hasManageStoragePermission(requireContext()));
    boolean notified =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
            || ContextCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    setGranted(binding.rowNotifications, notified);
    setGranted(binding.rowInstall, requireContext().getPackageManager().canRequestPackageInstalls());
    binding.tvHeader.setTextColor(fallback(M3Theme.onSurface(), fallback));
  }

  private void setGranted(ItemOnboardingPermissionBinding row, boolean granted) {
    int fallback = 0xFF202020;
    row.btnGrant.setText(getString(granted ? R.string.onboarding_perm_granted : R.string.onboarding_perm_grant));
    row.btnGrant.setEnabled(!granted);
    row.btnGrant.setAlpha(granted ? 0.6f : 1f);
    row.btnGrant.setStrokeColor(ColorStateList.valueOf(fallback(M3Theme.outline(), fallback)));
    row.btnGrant.setTextColor(fallback(granted ? M3Theme.primary() : M3Theme.onSurface(), fallback));
  }

  private void requestFiles() {
    if (PermissionUtils.hasManageStoragePermission(requireContext())) return;
    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
    intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
    storageLauncher.launch(intent);
  }

  private void requestNotifications() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return;
    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
        == PackageManager.PERMISSION_GRANTED) return;
    requestPermissions(new String[] {Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFICATION);
  }

  private void requestInstallApps() {
    Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
    intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
    startActivity(intent);
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == REQ_NOTIFICATION && binding != null) refreshGranted();
  }

  private static void tintCard(MaterialCardView card, int color) {
    if (card != null) card.setCardBackgroundColor(color);
  }

  private static void tintIcon(ImageView icon, int color) {
    if (icon != null) icon.setColorFilter(color);
  }

  private static int fallback(Integer value, int def) {
    return value != null ? value : def;
  }
}