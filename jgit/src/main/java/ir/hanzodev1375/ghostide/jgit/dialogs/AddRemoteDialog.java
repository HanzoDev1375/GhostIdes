package ir.hanzodev1375.ghostide.jgit.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import ir.hanzodev1375.ghostide.jgit.R;
import ir.hanzodev1375.components.sheet.customitemsheet.ui.DialogCompat;
public class AddRemoteDialog extends DialogFragment {
  private OnRemoteAddListener listener;

  public interface OnRemoteAddListener {
    void onAdd(String name, String url);
  }

  public void setOnRemoteAddListener(OnRemoteAddListener listener) {
    this.listener = listener;
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    View view = requireActivity().getLayoutInflater().inflate(R.layout.dialog_add_remote, null);
    EditText etName = view.findViewById(R.id.etRemoteName);
    EditText etUrl = view.findViewById(R.id.etRemoteUrl);

    return new DialogCompat(requireActivity())
        .setTitle("Add Remote")
        .setView(view)
        .setPositiveButton(
            "Add",
            (dialog, which) -> {
              if (listener != null) {
                listener.onAdd(etName.getText().toString(), etUrl.getText().toString());
              }
            })
        .setNegativeButton("Cancel", null)
        .create();
  }
}
