package ir.hanzodev1375.ghostide.codeeditors.stringres;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import ir.hanzodev1375.ghostide.codeeditors.R;

public final class ExtractStringResourceDialog {

  public interface OnConfirmListener {
    void onConfirm(@NonNull String name, @NonNull String value);
  }

  private ExtractStringResourceDialog() {}

  public static void show(
      @NonNull Context context,
      @NonNull String suggestedName,
      @NonNull String initialValue,
      @NonNull OnConfirmListener listener) {

    View view = LayoutInflater.from(context).inflate(R.layout.dialog_extract_string_resource, null);

    TextInputEditText nameInput = view.findViewById(R.id.extractStringName);
    TextInputEditText valueInput = view.findViewById(R.id.extractStringValue);

    nameInput.setText(suggestedName);
    valueInput.setText(initialValue);

    new MaterialAlertDialogBuilder(context)
        .setTitle(R.string.extract_string_dialog_title)
        .setView(view)
        .setPositiveButton(
            R.string.extract_string_confirm,
            (dialog, which) -> {
              String name = String.valueOf(nameInput.getText()).trim();
              String value = String.valueOf(valueInput.getText());
              if (name.isEmpty()) {
                Toast.makeText(
                        context,
                        context.getString(R.string.extract_string_name_empty),
                        Toast.LENGTH_SHORT)
                    .show();
                return;
              }
              listener.onConfirm(name, value);
            })
        .setNegativeButton(R.string.extract_string_cancel, null)
        .show();
  }
}
