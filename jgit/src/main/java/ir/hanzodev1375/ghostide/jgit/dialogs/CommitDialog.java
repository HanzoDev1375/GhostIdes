package ir.hanzodev1375.ghostide.jgit.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.snackbar.Snackbar;

import ir.hanzodev1375.ghostide.codeeditors.setting.PreferencesUtils;
import ir.hanzodev1375.ghostide.jgit.R;
import ir.hanzodev1375.components.sheet.customitemsheet.ui.DialogCompat;
public class CommitDialog extends DialogFragment {

  private EditText etMessage, etAuthor, etEmail;
  private OnCommitListener listener;
  private PreferencesUtils utils;

  public interface OnCommitListener {
    void onCommit(String message, String author, String email);
  }

  public void setOnCommitListener(OnCommitListener listener) {
    this.listener = listener;
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {

    utils = new PreferencesUtils(requireContext());

    View view = requireActivity().getLayoutInflater().inflate(R.layout.dialog_commit, null);

    etMessage = view.findViewById(R.id.etCommitMessage);
    etAuthor = view.findViewById(R.id.etAuthor);
    etEmail = view.findViewById(R.id.etEmail);

    String savedName = utils.getGitCommitName();
    String savedEmail = utils.getGitCommitEmail();

    if (savedName != null && !savedName.trim().isEmpty()) {
      etAuthor.setText(savedName);
      etAuthor.setVisibility(View.GONE);
    } else {
      etAuthor.setVisibility(View.VISIBLE);

      etAuthor.addTextChangedListener(
          new TextWatcher() {
            @Override
            public void afterTextChanged(Editable e) {
              utils.setGitCommitName(e.toString());
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
          });
    }

    if (savedEmail != null && !savedEmail.trim().isEmpty()) {
      etEmail.setText(savedEmail);
      etEmail.setVisibility(View.GONE);
    } else {
      etEmail.setVisibility(View.VISIBLE);

      etEmail.addTextChangedListener(
          new TextWatcher() {
            @Override
            public void afterTextChanged(Editable e) {
              utils.setGitCommitEmail(e.toString());
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
          });
    }

    return new DialogCompat(requireActivity())
        .setTitle("Commit Changes")
        .setView(view)
        .setPositiveButton(
            "Commit",
            (dialog, which) -> {
              if (listener != null) {
                listener.onCommit(
                    etMessage.getText().toString().trim(),
                    etAuthor.getText().toString().trim(),
                    etEmail.getText().toString().trim());
              }
            })
        .setNegativeButton("Cancel", null)
        .setNeutralButton(
            "Clear Data",
            (dialog, which) -> {
              utils.setRemovedDataCommit();
              etAuthor.setVisibility(View.VISIBLE);
              etEmail.setVisibility(View.VISIBLE);
              etAuthor.setText("");
              etEmail.setText("");
              Snackbar.make(
                      requireActivity().findViewById(android.R.id.content),
                      "Developer information removed",
                      Snackbar.LENGTH_LONG)
                  .show();
            })
        .create();
  }
}
