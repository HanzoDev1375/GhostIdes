package ir.hanzodev1375.ghostide.refactor.renameclass.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import ir.hanzodev1375.components.sheet.BaseBlurBottomSheet;
import ir.hanzodev1375.components.utils.GlassColors;
import ir.theme.M3Theme;
import com.google.android.material.snackbar.Snackbar;
import ir.hanzodev1375.ghostide.R;
import ir.hanzodev1375.ghostide.databinding.BottomSheetRenameClassBinding;
import ir.hanzodev1375.ghostide.refactor.rename.model.PreviewEntry;
import ir.hanzodev1375.ghostide.refactor.rename.model.RenameProgress;
import ir.hanzodev1375.ghostide.refactor.rename.model.ValidationResult;
import ir.hanzodev1375.ghostide.refactor.rename.ui.PreviewEntryAdapter;
import ir.hanzodev1375.ghostide.refactor.renameclass.ClassFileTarget;
import ir.hanzodev1375.ghostide.refactor.renameclass.ClassScanResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import ir.hanzodev1375.components.sheet.customitemsheet.ui.DialogCompat;
public final class RenameClassBottomSheet extends BaseBlurBottomSheet {

  public static final String TAG = "RenameClassBottomSheet";
  private static final String ARG_PROJECT_ROOT = "project_root";
  private static final String ARG_TARGET_FILE = "target_file";
  private static final String ARG_OLD_CLASS_NAME = "old_class_name";

  private BottomSheetRenameClassBinding binding;
  private RenameClassViewModel viewModel;
  private PreviewEntryAdapter previewAdapter;

  public static RenameClassBottomSheet newInstance(
      String projectRootPath, String targetFilePath, String oldClassName) {
    RenameClassBottomSheet sheet = new RenameClassBottomSheet();
    Bundle args = new Bundle();
    args.putString(ARG_PROJECT_ROOT, projectRootPath);
    args.putString(ARG_TARGET_FILE, targetFilePath);
    args.putString(ARG_OLD_CLASS_NAME, oldClassName);
    sheet.setArguments(args);
    return sheet;
  }

  @Override
  protected void onContentReady(@NonNull ViewGroup contentContainer) {
    binding = BottomSheetRenameClassBinding.inflate(LayoutInflater.from(requireContext()));
    contentContainer.addView(binding.getRoot());
    GlassColors.setBackgroundAlpha(
        binding.warningCard,
        fallback(M3Theme.errorContainer(), 0),
        140);
    viewModel = new ViewModelProvider(this).get(RenameClassViewModel.class);

    Bundle args = getArguments();
    String projectRoot = args != null ? args.getString(ARG_PROJECT_ROOT) : null;
    String targetFile = args != null ? args.getString(ARG_TARGET_FILE) : null;
    String oldClassName = args != null ? args.getString(ARG_OLD_CLASS_NAME) : null;

    if (projectRoot == null || targetFile == null || oldClassName == null) {
      dismiss();
      return;
    }

    binding.currentClassValue.setText(oldClassName);
    previewAdapter = new PreviewEntryAdapter();
    binding.previewRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
    binding.previewRecyclerView.setAdapter(previewAdapter);
    binding.previewRecyclerView.setNestedScrollingEnabled(false);

    binding.cancelButton.setOnClickListener(v -> dismiss());
    binding.renameButton.setEnabled(false);
    binding.renameButton.setOnClickListener(v -> viewModel.requestRename());

    binding.newClassNameInput.addTextChangedListener(
        new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {}

          @Override
          public void afterTextChanged(Editable editable) {
            viewModel.onNewClassNameChanged(editable.toString());
          }
        });

    observeViewModel();
    viewModel.start(projectRoot, targetFile, oldClassName);
    M3Theme.apply(binding.getRoot());
  }

  private void observeViewModel() {
    viewModel.getUiState().observe(getViewLifecycleOwner(), this::renderState);
    viewModel.getInputValidation().observe(getViewLifecycleOwner(), this::renderValidation);
    viewModel.getScanResult().observe(getViewLifecycleOwner(), this::renderScanResult);
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

  private void renderState(RenameClassViewModel.UiState state) {
    if (state == null) {
      return;
    }
    switch (state) {
      case SCANNING -> {
        binding.progressIndicator.setVisibility(View.VISIBLE);
        binding.newClassNameInputLayout.setEnabled(false);
        binding.renameButton.setEnabled(false);
      }
      case PREVIEW_READY -> {
        binding.progressIndicator.setVisibility(View.GONE);
        binding.newClassNameInputLayout.setEnabled(true);
        binding.renameButton.setEnabled(viewModel.canConfirmRename());
      }
      case CONFIRMING -> showConfirmationDialog();
      case EXECUTING -> {
        binding.progressIndicator.setVisibility(View.VISIBLE);
        binding.newClassNameInputLayout.setEnabled(false);
        binding.renameButton.setEnabled(false);
        setCancelable(false);
      }
      case SUCCESS -> {
        binding.progressIndicator.setVisibility(View.GONE);
        showSnackbar(getString(R.string.rename_class_success));
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
    } else {
      binding.validationErrorText.setText(result.getFirstError());
      binding.validationErrorText.setVisibility(View.VISIBLE);
    }
    binding.renameButton.setEnabled(viewModel.canConfirmRename());
  }

  private void renderScanResult(ClassScanResult result) {
    if (result == null) {
      binding.previewSection.setVisibility(View.GONE);
      binding.warningCard.setVisibility(View.GONE);
      previewAdapter.submitList(Collections.emptyList());
      return;
    }
    binding.previewSection.setVisibility(View.VISIBLE);
    binding.previewSummaryText.setText(
        getString(R.string.rename_class_preview_summary, result.getTargets().size()));
    List<PreviewEntry> entries = new ArrayList<>();
    for (ClassFileTarget target : result.getTargets()) {
      entries.add(
          new PreviewEntry(
              target.shouldRewriteUnqualified() ? "Reference" : "Qualified reference",
              "Will be updated",
              target.getFile().getPath(),
              1));
    }
    previewAdapter.submitList(entries);
    if (!result.getAmbiguousFiles().isEmpty()) {
      binding.warningCard.setVisibility(View.VISIBLE);
      binding.warningText.setText(
          getString(R.string.rename_class_ambiguous_warning, result.getAmbiguousFiles().size()));
    } else {
      binding.warningCard.setVisibility(View.GONE);
    }
    binding.renameButton.setEnabled(viewModel.canConfirmRename());
  }

  private void renderProgress(RenameProgress renameProgress) {
    if (renameProgress == null) {
      return;
    }
    binding.statusText.setText(renameProgress.getPhase().name());
    binding.statusText.setVisibility(View.VISIBLE);
  }

  private void showConfirmationDialog() {
    ClassScanResult result = viewModel.getScanResult().getValue();
    int count = result != null ? result.getTargets().size() : 0;
    String message =
        getString(
            R.string.rename_class_confirm_message, viewModel.getOldClassName(), textOf(), count);
    new DialogCompat(requireContext())
        .setTitle(R.string.rename_class_confirm_title)
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
    return binding.newClassNameInput.getText() != null
        ? binding.newClassNameInput.getText().toString()
        : "";
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }

  private static int fallback(Integer value, int def) {
    return value != null ? value : def;
  }
}
