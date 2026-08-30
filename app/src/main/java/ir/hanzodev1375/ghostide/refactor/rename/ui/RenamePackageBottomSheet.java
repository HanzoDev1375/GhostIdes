package ir.hanzodev1375.ghostide.refactor.rename.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.color.MaterialColors;
import ir.hanzodev1375.components.sheet.BaseBlurBottomSheet;
import ir.hanzodev1375.components.utils.GlassColors;
import com.google.android.material.snackbar.Snackbar;
import ir.hanzodev1375.ghostide.R;
import ir.hanzodev1375.ghostide.databinding.BottomSheetRenamePackageBinding;
import ir.hanzodev1375.ghostide.refactor.rename.model.PreviewResult;
import ir.hanzodev1375.ghostide.refactor.rename.model.RenameProgress;
import ir.hanzodev1375.ghostide.refactor.rename.model.ValidationResult;
import java.util.Collections;
import ir.hanzodev1375.components.sheet.customitemsheet.ui.DialogCompat;
public final class RenamePackageBottomSheet extends BaseBlurBottomSheet {

  public static final String TAG = "RenamePackageBottomSheet";
  private static final String ARG_MODULE_ROOT = "module_root";
  private static final String ARG_OLD_PACKAGE = "old_package";

  private BottomSheetRenamePackageBinding binding;
  private RenamePackageViewModel viewModel;
  private PreviewEntryAdapter previewAdapter;
  private OnPackageRenamedListener listener;

  public interface OnPackageRenamedListener {
    void onPackageRenamed(String oldPackage, String newPackage);
  }

  public static RenamePackageBottomSheet newInstance(String moduleRootPath, String oldPackage) {
    RenamePackageBottomSheet sheet = new RenamePackageBottomSheet();
    Bundle args = new Bundle();
    args.putString(ARG_MODULE_ROOT, moduleRootPath);
    args.putString(ARG_OLD_PACKAGE, oldPackage);
    sheet.setArguments(args);
    return sheet;
  }

  public void setOnPackageRenamedListener(OnPackageRenamedListener listener) {
    this.listener = listener;
  }

  @Override
  protected void onContentReady(@NonNull ViewGroup contentContainer) {
    binding = BottomSheetRenamePackageBinding.inflate(LayoutInflater.from(requireContext()));
    contentContainer.addView(binding.getRoot());
    GlassColors.setBackgroundAlpha(
        binding.warningCard,
        MaterialColors.getColor(binding.getRoot(), com.google.android.material.R.attr.colorSecondaryContainer),
        140);
    viewModel = new ViewModelProvider(this).get(RenamePackageViewModel.class);

    Bundle args = getArguments();
    String moduleRoot = args != null ? args.getString(ARG_MODULE_ROOT) : null;
    String oldPackage = args != null ? args.getString(ARG_OLD_PACKAGE) : null;

    if (moduleRoot == null || oldPackage == null) {
      dismiss();
      return;
    }

    binding.currentPackageValue.setText(oldPackage);
    previewAdapter = new PreviewEntryAdapter();
    binding.previewRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
    binding.previewRecyclerView.setAdapter(previewAdapter);
    binding.previewRecyclerView.setNestedScrollingEnabled(false);

    binding.cancelButton.setOnClickListener(v -> dismiss());
    binding.renameButton.setEnabled(false);
    binding.renameButton.setOnClickListener(v -> viewModel.requestRename());

    binding.newPackageInput.addTextChangedListener(
        new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {}

          @Override
          public void afterTextChanged(Editable editable) {
            viewModel.onNewPackageChanged(editable.toString());
          }
        });

    observeViewModel();
    viewModel.start(moduleRoot, oldPackage);
  }

  private void observeViewModel() {
    viewModel.getUiState().observe(getViewLifecycleOwner(), this::renderState);
    viewModel.getInputValidation().observe(getViewLifecycleOwner(), this::renderValidation);
    viewModel.getPreviewResult().observe(getViewLifecycleOwner(), this::renderPreview);
    viewModel.getProgress().observe(getViewLifecycleOwner(), this::renderProgress);
    viewModel
        .getErrorMessage()
        .observe(
            getViewLifecycleOwner(),
            message -> {
              if (message != null) {
                binding.validationErrorText.setText(message);
                binding.validationErrorText.setVisibility(View.VISIBLE);
              }
            });
  }

  private void renderState(RenamePackageViewModel.UiState state) {
    if (state == null) {
      return;
    }
    switch (state) {
      case SCANNING -> {
        binding.progressIndicator.setVisibility(View.VISIBLE);
        binding.newPackageInputLayout.setEnabled(false);
        binding.renameButton.setEnabled(false);
      }
      case INVALID_STRUCTURE -> {
        binding.progressIndicator.setVisibility(View.GONE);
        binding.newPackageInputLayout.setEnabled(false);
        binding.renameButton.setEnabled(false);
      }
      case READY, VALIDATING_INPUT -> {
        binding.progressIndicator.setVisibility(View.GONE);
        binding.newPackageInputLayout.setEnabled(true);
        binding.renameButton.setEnabled(false);
      }
      case BUILDING_PREVIEW -> {
        binding.progressIndicator.setVisibility(View.VISIBLE);
        binding.renameButton.setEnabled(false);
      }
      case PREVIEW_READY -> {
        binding.progressIndicator.setVisibility(View.GONE);
        binding.renameButton.setEnabled(viewModel.canConfirmRename());
      }
      case CONFIRMING -> showConfirmationDialog();
      case EXECUTING -> {
        binding.progressIndicator.setVisibility(View.VISIBLE);
        binding.newPackageInputLayout.setEnabled(false);
        binding.renameButton.setEnabled(false);
        setCancelable(false);
        binding.cancelButton.setOnClickListener(v -> viewModel.cancelActiveOperation());
      }
      case SUCCESS -> {
        binding.progressIndicator.setVisibility(View.GONE);
        if (listener != null) {
          listener.onPackageRenamed(viewModel.getOldPackage(), textOf());
        }
        showSnackbar(getString(R.string.rename_package_success));
        dismiss();
      }
      case ERROR -> {
        binding.progressIndicator.setVisibility(View.GONE);
        setCancelable(true);
        binding.renameButton.setEnabled(false);
      }
      case CANCELLED -> {
        binding.progressIndicator.setVisibility(View.GONE);
        setCancelable(true);
        binding.renameButton.setEnabled(viewModel.canConfirmRename());
      }
      default -> {}
    }
  }

  private void renderValidation(ValidationResult result) {
    if (result == null || result.isValid()) {
      binding.validationErrorText.setVisibility(View.GONE);
      return;
    }
    binding.validationErrorText.setText(result.getFirstError());
    binding.validationErrorText.setVisibility(View.VISIBLE);
  }

  private void renderPreview(PreviewResult result) {
    if (result == null) {
      binding.previewSection.setVisibility(View.GONE);
      binding.warningCard.setVisibility(View.GONE);
      previewAdapter.submitList(Collections.emptyList());
      return;
    }
    binding.previewSection.setVisibility(View.VISIBLE);
    binding.previewSummaryText.setText(
        getString(
            R.string.rename_package_preview_summary,
            result.getJavaFilesAffected(),
            result.getKotlinFilesAffected(),
            result.getDirectoriesAffected(),
            result.getTotalChanges()));
    binding.warningCard.setVisibility(
        result.isManifestAffected() || result.isGradleAffected() ? View.VISIBLE : View.GONE);
    previewAdapter.submitList(result.getEntries());
    binding.renameButton.setEnabled(viewModel.canConfirmRename());
  }

  private void renderProgress(RenameProgress renameProgress) {
    if (renameProgress == null) {
      return;
    }
    binding.statusText.setText(describePhase(renameProgress.getPhase()));
    binding.statusText.setVisibility(View.VISIBLE);
  }

  private String describePhase(RenameProgress.Phase phase) {
    return switch (phase) {
      case SCANNING -> getString(R.string.rename_package_status_scanning);
      case VALIDATING -> getString(R.string.rename_package_status_validating);
      case BUILDING_PREVIEW -> getString(R.string.rename_package_status_preview);
      case BACKING_UP -> getString(R.string.rename_package_status_backup);
      case REWRITING_JAVA, REWRITING_KOTLIN -> getString(R.string.rename_package_status_rewriting);
      case REWRITING_MANIFEST, REWRITING_GRADLE -> getString(R.string.rename_package_status_config);
      case MOVING_FILES -> getString(R.string.rename_package_status_moving);
      case DELETING_EMPTY_DIRECTORIES -> getString(R.string.rename_package_status_cleanup);
      case ROLLING_BACK -> getString(R.string.rename_package_status_rollback);
      case COMPLETED -> getString(R.string.rename_package_status_completed);
    };
  }

  private void showConfirmationDialog() {
    PreviewResult result = viewModel.getPreviewResult().getValue();
    String message =
        result != null
            ? getString(
                R.string.rename_package_confirm_message,
                viewModel.getOldPackage(),
                textOf(),
                result.getTotalChanges())
            : "";
    new DialogCompat(requireContext())
        .setTitle(R.string.rename_package_confirm_title)
        .setMessage(message)
        .setPositiveButton(R.string.rename, (dialog, which) -> viewModel.confirmRename())
        .setNegativeButton(R.string.cancel, (dialog, which) -> viewModel.cancelConfirmation())
        .setOnCancelListener(dialog -> viewModel.cancelConfirmation())
        .show();
  }

  private void showSnackbar(String message) {
    if (getActivity() == null) {
      return;
    }
    View root = getActivity().findViewById(android.R.id.content);
    Snackbar.make(root, message, Snackbar.LENGTH_LONG).show();
  }

  private String textOf() {
    return binding.newPackageInput.getText() != null
        ? binding.newPackageInput.getText().toString()
        : "";
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }
}
