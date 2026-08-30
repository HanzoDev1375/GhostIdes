package ir.hanzodev1375.ghostide.project;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import ir.hanzodev1375.ghostide.R;
import ir.hanzodev1375.components.sheet.customitemsheet.ui.DialogCompat;
public class NewProjectDialog {

  public interface OnProjectCreated {
    void onCreated(String projectPath);
  }

  private final Context context;
  private final String savePath;
  private final OnProjectCreated callback;
  private AlertDialog dialog;
  private String projectPath;

  public NewProjectDialog(Context context, String savePath, OnProjectCreated callback) {
    this.context = context;
    this.savePath = savePath;
    this.callback = callback;
  }

  public void show() {
    View view = LayoutInflater.from(context).inflate(R.layout.dialog_new_project, null);

    ChipGroup chipGroup = view.findViewById(R.id.chipGroupType);
    TextInputLayout tilName = view.findViewById(R.id.tilProjectName);
    TextInputEditText etName = view.findViewById(R.id.etProjectName);
    TextInputLayout tilPkg = view.findViewById(R.id.tilPackageName);
    TextInputEditText etPkg = view.findViewById(R.id.etPackageName);
    view.findViewById(R.id.chipHtml).performClick();

    dialog =
        new DialogCompat(context)
            .setTitle(R.string.project_new_title)
            .setView(view)
            .setPositiveButton(R.string.project_create, null)
            .setNegativeButton(R.string.cancel, null)
            .create();

    dialog.setOnShowListener(
        d -> {
          dialog
              .getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
              .setOnClickListener(
                  v -> {
                    String name =
                        etName.getText() != null ? etName.getText().toString().trim() : "";
                    if (name.isEmpty()) {
                      tilName.setError(context.getString(R.string.project_error_name_empty));
                      return;
                    }
                    tilName.setError(null);

                    ProjectCreator.ProjectType type = getSelectedType(chipGroup);

                    String pkg = "";
                    if (type == ProjectCreator.ProjectType.JAVA) {
                      pkg = etPkg.getText() != null ? etPkg.getText().toString().trim() : "";
                      if (pkg.isEmpty() || !isValidPackage(pkg)) {
                        tilPkg.setError(context.getString(R.string.project_error_pkg_invalid));
                        return;
                      }
                      tilPkg.setError(null);
                    }

                    dialog.dismiss();

                    final String finalPkg = pkg;
                    new ProjectCreator(context)
                        .create(
                            type,
                            name,
                            finalPkg,
                            savePath,
                            new ProjectCreator.OnCreateResult() {
                              @Override
                              public void onSuccess(String projectPath) {
                                if (callback != null) callback.onCreated(projectPath);
                                projectPath = projectPath;
                              }

                              @Override
                              public void onError(String message) {
                                new DialogCompat(context)
                                    .setTitle(R.string.project_error_title)
                                    .setMessage(message)
                                    .setPositiveButton(R.string.ok, null)
                                    .show();
                              }
                            });
                  });
        });

    dialog.show();
    chipGroup.setOnCheckedStateChangeListener(
        (group, checkedIds) -> {
          boolean isJava = !checkedIds.isEmpty() && checkedIds.get(0) == R.id.chipJava;
          tilPkg.setVisibility(isJava ? View.VISIBLE : View.GONE);
          ProjectCreator.ProjectType type = getSelectedType(chipGroup);
          if (type == ProjectCreator.ProjectType.ANDROIDMODULE) {
            dialog.dismiss();
            new NewModuleDialog(
                    context,
                    savePath,
                    (path) -> {
                      callback.onCreated(path);
                    })
                .show();
          }
        });
  }

  private ProjectCreator.ProjectType getSelectedType(ChipGroup group) {
    int id = group.getCheckedChipId();
    if (id == R.id.chipNodejs) return ProjectCreator.ProjectType.NODEJS;
    if (id == R.id.chipJava) return ProjectCreator.ProjectType.JAVA;
    if (id == R.id.chipFlutter) return ProjectCreator.ProjectType.FLUTTER;
    if (id == R.id.chipPython) return ProjectCreator.ProjectType.PYTHON;
    if (id == R.id.chipPythonC) return ProjectCreator.ProjectType.PYTHON_C;
    if (id == R.id.chipPhp) return ProjectCreator.ProjectType.PHP;
    if (id == R.id.chipC) return ProjectCreator.ProjectType.C;
    if (id == R.id.chipCpp) return ProjectCreator.ProjectType.CPP;
    if (id == R.id.chipRuby) return ProjectCreator.ProjectType.RUBY;
    if (id == R.id.chipAndroidModule) return ProjectCreator.ProjectType.ANDROIDMODULE;

    return ProjectCreator.ProjectType.HTML;
  }

  /** package must have at least one dot, each segment starts with a letter */
  private boolean isValidPackage(String pkg) {
    if (!pkg.contains(".")) return false;
    for (String segment : pkg.split("\\.", -1)) {
      if (segment.isEmpty() || !Character.isLetter(segment.charAt(0))) return false;
      if (!segment.matches("[a-zA-Z][a-zA-Z0-9_]*")) return false;
    }
    return true;
  }
}
