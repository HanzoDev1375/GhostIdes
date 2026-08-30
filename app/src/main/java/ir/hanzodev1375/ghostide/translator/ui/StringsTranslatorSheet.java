package ir.hanzodev1375.ghostide.translator.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import ir.hanzodev1375.components.sheet.BaseBlurBottomSheet;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import ir.hanzodev1375.ghostide.R;
import ir.hanzodev1375.ghostide.databinding.BottomSheetTranslatorBinding;
import ir.hanzodev1375.ghostide.translator.model.AndroidLanguage;
import ir.hanzodev1375.ghostide.translator.model.StringEntry;
import ir.hanzodev1375.ghostide.translator.util.StringsXmlParser;
import ir.hanzodev1375.ghostide.translator.viewmodel.TranslatorViewModel;

public class StringsTranslatorSheet extends BaseBlurBottomSheet {
  public static final String TAG = "StringsTranslatorSheet";
  private static final String ARG_ROOT = "root_path";
  private BottomSheetTranslatorBinding binding;
  private TranslatorViewModel viewModel;
  private final List<AndroidLanguage> selectedLanguages = new ArrayList<>();
  private final List<StringEntry> sourceTranslatableEntries = new ArrayList<>();
  private File foundStringsXml = null;

  public static StringsTranslatorSheet newInstance(String rootPath) {
    StringsTranslatorSheet sheet = new StringsTranslatorSheet();
    Bundle args = new Bundle();
    args.putString(ARG_ROOT, rootPath);
    sheet.setArguments(args);
    return sheet;
  }

  @Override
  protected void onContentReady(@NonNull ViewGroup contentContainer) {
    binding = BottomSheetTranslatorBinding.inflate(LayoutInflater.from(requireContext()));
    contentContainer.addView(binding.getRoot());
    viewModel = new ViewModelProvider(this).get(TranslatorViewModel.class);
    setupButtons();
    observeViewModel();
    checkAndShowPath();
  }

  private void checkAndShowPath() {
    String root = getArguments() != null ? getArguments().getString(ARG_ROOT) : null;
    if (root == null) {
      binding.tvStringsPath.setText("Project dir not found");
      binding.tvStringsPath.setTextColor(Color.parseColor("#F44336"));
      buildLanguageChips();
      return;
    }
    foundStringsXml = findStringsXml(new File(root));
    if (foundStringsXml != null) {
      binding.tvStringsPath.setText("✓ " + foundStringsXml.getAbsolutePath());
      binding.tvStringsPath.setTextColor(Color.parseColor("#4CAF50"));
      loadSourceEntries();
      buildLanguageChips();
      binding.btnTranslate.setEnabled(!selectedLanguages.isEmpty());
    } else {
      binding.tvStringsPath.setText("✗ strings.xml not found: " + root);
      binding.tvStringsPath.setTextColor(Color.parseColor("#F44336"));
      buildLanguageChips();
      binding.btnTranslate.setEnabled(false);
    }
  }

  private void loadSourceEntries() {
    sourceTranslatableEntries.clear();
    try {
      List<StringEntry> all = StringsXmlParser.parse(foundStringsXml);
      for (StringEntry e : all) {
        if (e.translatable) sourceTranslatableEntries.add(e);
      }
    } catch (Exception ignored) {
      // If the source can't be parsed yet, chips will just show with no badge.
    }
  }

  private void buildLanguageChips() {
    binding.chipGroupLanguages.removeAllViews();
    String resDir = resDirOrNull();
    int total = sourceTranslatableEntries.size();
    for (AndroidLanguage lang : AndroidLanguage.ALL) {
      Chip chip = new Chip(requireContext());
      chip.setCheckable(true);
      chip.setCheckedIconVisible(true);
      chip.setTag(lang);
      chip.setText(buildChipLabel(lang, resDir, total));
      chip.setOnCheckedChangeListener(
          (btn, checked) -> {
            AndroidLanguage l = (AndroidLanguage) btn.getTag();
            if (checked) {
              if (!selectedLanguages.contains(l)) selectedLanguages.add(l);
            } else selectedLanguages.remove(l);
            binding.btnTranslate.setEnabled(
                !selectedLanguages.isEmpty() && foundStringsXml != null);
          });
      binding.chipGroupLanguages.addView(chip);
    }
    binding.btnTranslate.setEnabled(false);
  }

  private String buildChipLabel(AndroidLanguage lang, String resDir, int total) {
    if (resDir == null || total == 0) return lang.displayName;
    int existing = countExistingTranslations(resDir, lang);
    if (existing <= 0) return lang.displayName;
    if (existing >= total) return lang.displayName + "  ✓";
    return lang.displayName + "  (" + existing + "/" + total + ")";
  }

  private int countExistingTranslations(String resDir, AndroidLanguage lang) {
    File target = new File(resDir + "/" + lang.androidFolder + "/strings.xml");
    if (!target.exists()) return 0;
    try {
      Set<String> sourceKeys = new HashSet<>();
      for (StringEntry e : sourceTranslatableEntries) sourceKeys.add(e.name);
      List<StringEntry> existing = StringsXmlParser.parse(target);
      int count = 0;
      for (StringEntry e : existing) {
        if (sourceKeys.contains(e.name) && e.value != null && !e.value.trim().isEmpty()) {
          count++;
        }
      }
      return count;
    } catch (Exception e) {
      return 0;
    }
  }

  private String resDirOrNull() {
    if (foundStringsXml == null) return null;
    File valuesDir = foundStringsXml.getParentFile();
    if (valuesDir == null || valuesDir.getParentFile() == null) return null;
    return valuesDir.getParentFile().getAbsolutePath();
  }

  private void setupButtons() {
    binding.btnSelectAll.setOnClickListener(
        v -> {
          selectedLanguages.clear();
          for (int i = 0; i < binding.chipGroupLanguages.getChildCount(); i++) {
            ((Chip) binding.chipGroupLanguages.getChildAt(i)).setChecked(true);
          }
        });
    binding.btnDeselectAll.setOnClickListener(
        v -> {
          selectedLanguages.clear();
          for (int i = 0; i < binding.chipGroupLanguages.getChildCount(); i++) {
            ((Chip) binding.chipGroupLanguages.getChildAt(i)).setChecked(false);
          }
          binding.btnTranslate.setEnabled(false);
        });
    binding.btnTranslate.setOnClickListener(v -> startTranslation());
    binding.btnCancel.setOnClickListener(
        v -> {
          viewModel.cancel();
          setUiRunning(false);
        });
  }

  private void startTranslation() {
    if (selectedLanguages.isEmpty() || foundStringsXml == null) return;
    String resDir = foundStringsXml.getParentFile().getParentFile().getAbsolutePath();
    setUiRunning(true);
    binding.tvLog.setText("");
    binding.progressBar.setProgress(0);
    viewModel.startTranslation(foundStringsXml, resDir, new ArrayList<>(selectedLanguages));
  }

  private File findStringsXml(File dir) {
    if (dir == null || !dir.exists()) return null;
    if (dir.isFile()) {
      if (dir.getName().equals("strings.xml")
          && dir.getParentFile() != null
          && dir.getParentFile().getName().equals("values")) {
        return dir;
      }
      return null;
    }
    String name = dir.getName();
    if (name.equals("build") || name.equals(".git") || name.startsWith(".")) return null;
    File[] children = dir.listFiles();
    if (children == null) return null;
    for (File child : children) {
      File found = findStringsXml(child);
      if (found != null) return found;
    }
    return null;
  }

  private void observeViewModel() {
    viewModel
        .getState()
        .observe(
            getViewLifecycleOwner(),
            s -> {
              switch (s) {
                case RUNNING:
                  setUiRunning(true);
                  break;
                case DONE:
                  setUiRunning(false);
                  binding.progressBar.setProgress(100);
                  showSnackbar(getString(R.string.translator_done));
                  break;
                case CANCELLED:
                  setUiRunning(false);
                  showSnackbar(getString(R.string.translator_cancelled));
                  break;
                case ERROR:
                  setUiRunning(false);
                  break;
                default:
                  break;
              }
            });
    viewModel
        .getProgress()
        .observe(
            getViewLifecycleOwner(),
            p -> {
              if (p == null) return;
              binding.progressBar.setProgress(p.getPercent());
              binding.tvProgressLabel.setText(
                  p.getPercent() + "% — " + p.languageFolder + " / " + p.currentKey);
            });
    viewModel
        .getLastCompletedFolder()
        .observe(
            getViewLifecycleOwner(),
            folder -> {
              if (folder == null) return;
              binding.tvLog.setText(binding.tvLog.getText() + "✓ " + folder + "\n");
              binding.scrollLog.post(() -> binding.scrollLog.fullScroll(View.FOCUS_DOWN));
            });
    viewModel
        .getErrorMessage()
        .observe(
            getViewLifecycleOwner(),
            msg -> {
              if (msg != null && !msg.isEmpty()) showSnackbar("Error: " + msg);
            });
  }

  private void setUiRunning(boolean running) {
    binding.btnTranslate.setVisibility(running ? View.GONE : View.VISIBLE);
    binding.btnCancel.setVisibility(running ? View.VISIBLE : View.GONE);
    binding.chipGroupLanguages.setEnabled(!running);
    binding.btnSelectAll.setEnabled(!running);
    binding.btnDeselectAll.setEnabled(!running);
    binding.layoutProgress.setVisibility(running ? View.VISIBLE : View.GONE);
  }

  private void showSnackbar(String msg) {
    if (getView() != null) Snackbar.make(getView(), msg, Snackbar.LENGTH_LONG).show();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }
}
