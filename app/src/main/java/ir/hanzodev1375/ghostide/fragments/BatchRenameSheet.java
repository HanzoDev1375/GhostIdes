package ir.hanzodev1375.ghostide.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import ir.hanzodev1375.components.views.GhostToast;
import androidx.annotation.NonNull;
import ir.hanzodev1375.components.sheet.BaseBlurBottomSheet;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;
import ir.hanzodev1375.components.utils.GlassColors;
import ir.hanzodev1375.ghostide.R;
import ir.hanzodev1375.ghostide.enums.FileState;
import ir.hanzodev1375.ghostide.models.FileManagerModel;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class BatchRenameSheet extends BaseBlurBottomSheet {

  public static final String TAG = "BatchRenameSheet";
  private static final String ARG_FILES = "files";

  private List<FileManagerModel> files;
  private OnRenameListener listener;

  public interface OnRenameListener {
    void onRename(
        List<FileManagerModel> files,
        String pattern,
        String findText,
        String replaceText,
        boolean useRegex);
  }

  public static BatchRenameSheet newInstance(List<FileManagerModel> files) {
    BatchRenameSheet sheet = new BatchRenameSheet();
    Bundle args = new Bundle();
    ArrayList<String> paths = new ArrayList<>();
    ArrayList<String> names = new ArrayList<>();
    for (FileManagerModel f : files) {
      paths.add(f.getPath());
      names.add(f.getName());
    }
    args.putStringArrayList("paths", paths);
    args.putStringArrayList("names", names);
    sheet.setArguments(args);
    sheet.files = files;
    return sheet;
  }

  public void setOnRenameListener(OnRenameListener listener) {
    this.listener = listener;
  }

  @Override
  protected void onContentReady(@NonNull ViewGroup contentContainer) {
    View view =
        LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_batch_rename, null, false);
    contentContainer.addView(view);

    if (files == null && getArguments() != null) {
      ArrayList<String> paths = getArguments().getStringArrayList("paths");
      ArrayList<String> names = getArguments().getStringArrayList("names");
      files = new ArrayList<>();
      if (paths != null && names != null) {
        for (int i = 0; i < paths.size(); i++) {
          FileManagerModel model =
              new FileManagerModel(paths.get(i), names.get(i), FileState.NONE, 0);
          files.add(model);
        }
      }
    }

    TextInputEditText etPattern = view.findViewById(R.id.et_pattern);
    TextInputEditText etFind = view.findViewById(R.id.et_find);
    TextInputEditText etReplace = view.findViewById(R.id.et_replace);
    MaterialTextView tvPreview = view.findViewById(R.id.tv_preview);
    GlassColors.setBackgroundAlpha(
        tvPreview,
        MaterialColors.getColor(tvPreview, com.google.android.material.R.attr.colorSurfaceVariant),
        150);
    MaterialButton btnPreview = view.findViewById(R.id.btn_preview);
    MaterialButton btnApply = view.findViewById(R.id.btn_apply);
    MaterialButton btnCancel = view.findViewById(R.id.btn_cancel);
    com.google.android.material.switchmaterial.SwitchMaterial switchRegex =
        view.findViewById(R.id.switch_regex);

    // Default pattern
    etPattern.setText("{P}{S}");
    btnPreview.setVisibility(View.GONE);

    TextWatcher watcher =
        new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {}

          @Override
          public void afterTextChanged(Editable s) {
            updatePreview(etPattern, etFind, etReplace, switchRegex, tvPreview);
          }
        };

    etPattern.addTextChangedListener(watcher);
    etFind.addTextChangedListener(watcher);
    etReplace.addTextChangedListener(watcher);
    switchRegex.setOnCheckedChangeListener(
        (btn, checked) -> updatePreview(etPattern, etFind, etReplace, switchRegex, tvPreview));

    btnPreview.setOnClickListener(
        v -> updatePreview(etPattern, etFind, etReplace, switchRegex, tvPreview));

    btnCancel.setOnClickListener(v -> dismiss());

    btnApply.setOnClickListener(
        v -> {
          String pattern =
              etPattern.getText() != null ? etPattern.getText().toString().trim() : "{P}{S}";
          String find = etFind.getText() != null ? etFind.getText().toString() : "";
          String replace = etReplace.getText() != null ? etReplace.getText().toString() : "";
          boolean useRegex = switchRegex.isChecked();

          if (pattern.isEmpty()) {
            GhostToast.makeText(requireContext(), "Pattern cannot be empty", GhostToast.LENGTH_SHORT).show();
            return;
          }

          if (listener != null) {
            listener.onRename(files, pattern, find, replace, useRegex);
          }
          applyRename(pattern, find, replace, useRegex);
          dismiss();
        });
    updatePreview(etPattern, etFind, etReplace, switchRegex, tvPreview);
  }

  private void updatePreview(
      TextInputEditText etPattern,
      TextInputEditText etFind,
      TextInputEditText etReplace,
      com.google.android.material.switchmaterial.SwitchMaterial switchRegex,
      MaterialTextView tvPreview) {

    if (files == null || files.isEmpty()) return;

    String pattern = etPattern.getText() != null ? etPattern.getText().toString() : "{P}{S}";
    String find = etFind.getText() != null ? etFind.getText().toString() : "";
    String replace = etReplace.getText() != null ? etReplace.getText().toString() : "";
    boolean useRegex = switchRegex.isChecked();

    StringBuilder sb = new StringBuilder();
    int count = 0;
    for (FileManagerModel model : files) {
      if (count >= 5) {
        sb.append("...\n");
        break;
      }
      String newName = buildNewName(model.getName(), pattern, find, replace, useRegex, count);
      sb.append(model.getName()).append("  →  ").append(newName).append("\n");
      count++;
    }

    tvPreview.setText(sb.toString().trim());
  }

  private String buildNewName(
      String original, String pattern, String find, String replace, boolean useRegex, int index) {
    // Split prefix and suffix
    String prefix;
    String suffix;
    int dotIndex = original.lastIndexOf('.');
    if (dotIndex > 0 && !new File(original).isDirectory()) {
      prefix = original.substring(0, dotIndex);
      suffix = original.substring(dotIndex); // e.g. ".txt"
    } else {
      prefix = original;
      suffix = "";
    }

    // Replace placeholders
    String result =
        pattern
            .replace("{P}", prefix)
            .replace("{S}", suffix)
            .replace("{N}", String.valueOf(index))
            .replace("{z" + index + "}", String.format("%02d", index));

    // Handle {zN} pattern generically (Java 8 compatible)
    java.util.regex.Matcher zMatcher = Pattern.compile("\\{z(\\d+)\\}").matcher(result);
    StringBuffer zSb = new StringBuffer();
    while (zMatcher.find()) {
      try {
        int base = Integer.parseInt(zMatcher.group(1));
        zMatcher.appendReplacement(zSb, String.format("%02d", base + index));
      } catch (Exception e) {
        zMatcher.appendReplacement(zSb, zMatcher.group(0));
      }
    }
    zMatcher.appendTail(zSb);
    result = zSb.toString();

    // Apply find/replace
    if (!find.isEmpty()) {
      if (useRegex) {
        try {
          result = result.replaceAll(find, replace);
        } catch (PatternSyntaxException e) {
          // Invalid regex, ignore
        }
      } else {
        result = result.replace(find, replace);
      }
    }

    return result;
  }

  private void applyRename(String pattern, String find, String replace, boolean useRegex) {
    if (files == null) return;
    int success = 0;
    int index = 0;
    for (FileManagerModel model : files) {
      String newName = buildNewName(model.getName(), pattern, find, replace, useRegex, index);
      File oldFile = new File(model.getPath());
      File newFile = new File(oldFile.getParent(), newName);
      if (oldFile.renameTo(newFile)) {
        success++;
      }
      index++;
    }
    int finalSuccess = success;
    if (getActivity() != null) {
      getActivity()
          .runOnUiThread(
              () ->
                  GhostToast.makeText(
                          requireContext(),
                          finalSuccess + " item(s) renamed successfully",
                          GhostToast.LENGTH_SHORT)
                      .show());
    }
  }
}
