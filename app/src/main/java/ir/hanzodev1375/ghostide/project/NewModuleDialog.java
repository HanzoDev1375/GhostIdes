package ir.hanzodev1375.ghostide.project;

import android.app.AlertDialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import ir.theme.M3Theme;
import java.util.ArrayList;
import java.util.List;
import ir.hanzodev1375.ghostide.R;
import ir.hanzodev1375.components.sheet.customitemsheet.ui.DialogCompat;
public class NewModuleDialog {

  public interface OnModuleCreated {
    void onCreated(String modulePath);
  }

  private final Context context;
  private final String projectRootPath;
  private final OnModuleCreated callback;
  private ModuleCreator mod;

  public NewModuleDialog(Context context, String projectRootPath, OnModuleCreated callback) {
    this.context = context;
    this.projectRootPath = projectRootPath;
    this.callback = callback;
  }

  public void show() {
    View view = LayoutInflater.from(context).inflate(R.layout.dialog_new_module, null);

    TextInputLayout tilName = view.findViewById(R.id.tilModuleName);
    TextInputEditText etName = view.findViewById(R.id.etModuleName);
    TextInputLayout tilPkg = view.findViewById(R.id.tilModulePackage);
    TextInputEditText etPkg = view.findViewById(R.id.etModulePackage);
    TextView tvRoot = view.findViewById(R.id.tvProjectRoot);
    CheckBox box = view.findViewById(R.id.box);

    TextInputLayout tilLibs = view.findViewById(R.id.tilLibraries);
    TextInputEditText etLibs = view.findViewById(R.id.etLibraries);
    ChipGroup chipGroupModules = view.findViewById(R.id.chipGroupModules);

    List<String> existingModules = ModuleScanner.scanModules(projectRootPath);
    for (String moduleName : existingModules) {
      Chip chip = new Chip(context);
      chip.setText(moduleName);
      chip.setCheckable(true);
      chip.setChecked(false);
      chipGroupModules.addView(chip);
    }
    M3Theme.applyTopLevel(view);

    tvRoot.setText(context.getString(R.string.module_project_root, projectRootPath));

    final boolean[] pkgEditedByUser = {false};

    etPkg.addTextChangedListener(
        new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence s, int st, int c, int a) {}

          @Override
          public void afterTextChanged(Editable s) {}

          @Override
          public void onTextChanged(CharSequence s, int st, int b, int c) {
            if (etPkg.hasFocus()) pkgEditedByUser[0] = true;
          }
        });

    etName.addTextChangedListener(
        new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence s, int st, int c, int a) {}

          @Override
          public void onTextChanged(CharSequence s, int st, int b, int c) {}

          @Override
          public void afterTextChanged(android.text.Editable s) {
            if (pkgEditedByUser[0]) return;
            String slug =
                s.toString()
                    .trim()
                    .toLowerCase()
                    .replaceAll("[^a-z0-9_]", "_")
                    .replaceAll("^_+|_+$", "");
            etPkg.setText("com.example." + slug);
          }
        });

    var dialog =
        new DialogCompat(context)
            .setTitle(R.string.module_new_title)
            .setView(view)
            .setPositiveButton(R.string.module_create, null)
            .setNegativeButton(R.string.cancel, null)
            .create();

    dialog.setOnShowListener(
        d -> {
          dialog
              .getButton(AlertDialog.BUTTON_POSITIVE)
              .setOnClickListener(
                  v -> {
                    String name =
                        etName.getText() != null ? etName.getText().toString().trim() : "";
                    if (name.isEmpty()) {
                      tilName.setError(context.getString(R.string.module_error_name_empty));
                      return;
                    }
                    if (!name.matches("[a-zA-Z][a-zA-Z0-9_-]*")) {
                      tilName.setError(context.getString(R.string.module_error_name_invalid));
                      return;
                    }
                    tilName.setError(null);

                    String pkg = etPkg.getText() != null ? etPkg.getText().toString().trim() : "";
                    if (pkg.isEmpty() || !isValidPackage(pkg)) {
                      tilPkg.setError(context.getString(R.string.module_error_pkg_invalid));
                      return;
                    }
                    tilPkg.setError(null);

                    String libsRaw =
                        etLibs != null && etLibs.getText() != null
                            ? etLibs.getText().toString().trim()
                            : "";
                    List<LibraryManager.Library> libraries = parseLibraries(libsRaw, tilLibs);
                    if (libraries == null) return;

                    dialog.dismiss();

                    List<String> selectedModules = new ArrayList<>();
                    for (int i = 0; i < chipGroupModules.getChildCount(); i++) {
                      View child = chipGroupModules.getChildAt(i);
                      if (child instanceof Chip && ((Chip) child).isChecked()) {
                        selectedModules.add(((Chip) child).getText().toString());
                      }
                    }

                    var progress =
                        new DialogCompat(context)
                            .setMessage(R.string.module_creating)
                            .setCancelable(false)
                            .create();
                    progress.show();

                    mod = new ModuleCreator(context);
                    mod.setDslType(box.isChecked() ? DslType.KOTLIN : DslType.GROOVY);
                    mod.setLibraries(libraries);
                    mod.setSelectedModules(selectedModules);
                    mod.create(
                        projectRootPath,
                        name,
                        pkg,
                        new ModuleCreator.OnModuleResult() {
                          @Override
                          public void onSuccess(String modulePath) {
                            progress.dismiss();
                            if (callback != null) callback.onCreated(modulePath);
                          }

                          @Override
                          public void onError(String message) {
                            progress.dismiss();
                            new DialogCompat(context)
                                .setTitle(R.string.module_error_title)
                                .setMessage(message)
                                .setPositiveButton(R.string.ok, null)
                                .show();
                          }
                        });
                  });
        });

    dialog.show();
  }

  /**
   * رشته ورودی کاربر را به لیستی از {@link LibraryManager.Library} تبدیل می‌کند.
   *
   * <p>فرمت پشتیبانی‌شده (هر خط یا با کاما جدا شده):
   *
   * <pre>
   *   group:artifact:version
   *   مثال: com.squareup.retrofit2:retrofit:2.11.0
   * </pre>
   *
   * @return لیست کتابخانه‌ها، یا لیست خالی اگر ورودی خالی باشد. اگر فرمت نادرست باشد {@code null}
   *     برمی‌گرداند و خطا را در {@code tilLibs} نشان می‌دهد.
   */
  private List<LibraryManager.Library> parseLibraries(String raw, TextInputLayout tilLibs) {
    List<LibraryManager.Library> result = new ArrayList<>();

    if (raw.isEmpty()) return result;

    String[] entries = raw.split("[,\n]+");

    for (String entry : entries) {
      String trimmed = entry.trim();
      if (trimmed.isEmpty()) continue;

      String[] parts = trimmed.split(":");
      if (parts.length != 3) {
        if (tilLibs != null) {
          tilLibs.setError(context.getString(R.string.module_error_lib_format, trimmed));
        }
        return null;
      }

      String group = parts[0].trim();
      String artifact = parts[1].trim();
      String version = parts[2].trim();

      if (group.isEmpty() || artifact.isEmpty() || version.isEmpty()) {
        if (tilLibs != null) {
          tilLibs.setError(context.getString(R.string.module_error_lib_format, trimmed));
        }
        return null;
      }

      String alias = artifact;
      result.add(new LibraryManager.Library(alias, group, artifact, version));
    }

    if (tilLibs != null) tilLibs.setError(null);
    return result;
  }

  private boolean isValidPackage(String pkg) {
    if (!pkg.contains(".")) return false;
    for (String seg : pkg.split("\\.", -1)) {
      if (seg.isEmpty() || !Character.isLetter(seg.charAt(0))) return false;
      if (!seg.matches("[a-zA-Z][a-zA-Z0-9_]*")) return false;
    }
    return true;
  }
}
