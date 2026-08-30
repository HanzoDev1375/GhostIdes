package ir.hanzodev1375.components;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.textfield.TextInputEditText;
import ir.hanzodev1375.components.utils.SoftInputLifeCycleObserver;
import java.lang.ref.WeakReference;
import ir.hanzodev1375.components.sheet.customitemsheet.ui.DialogCompat;
public class TextInputDialogFragment extends DialogFragment {

  private static final String ARG_TITLE = "title";
  private static final String ARG_HINT = "hint";
  private static final String ARG_PRELOAD = "preload";

  public interface InputCallback {
    void onInput(@NonNull String text);
  }

  private InputCallback callback;
  private TextInputEditText editText;

  public static TextInputDialogFragment newInstance(
      @Nullable String title, @Nullable String hint, @Nullable String preloadText) {
    TextInputDialogFragment frag = new TextInputDialogFragment();
    Bundle args = new Bundle();
    args.putString(ARG_TITLE, title);
    args.putString(ARG_HINT, hint);
    args.putString(ARG_PRELOAD, preloadText);
    frag.setArguments(args);
    return frag;
  }

  public TextInputDialogFragment setCallback(InputCallback callback) {
    this.callback = callback;
    return this;
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
    Bundle args = getArguments();
    String title = args != null ? args.getString(ARG_TITLE) : "ورود متن";
    String hint = args != null ? args.getString(ARG_HINT) : null;
    String preload = args != null ? args.getString(ARG_PRELOAD) : null;

    View view = LayoutInflater.from(requireActivity()).inflate(R.layout.dialog_rename, null);
    editText = view.findViewById(R.id.rename);
    if (hint != null) editText.setHint(hint);
    if (preload != null) {
      editText.setText(preload);
      editText.selectAll();
    }

    return new DialogCompat(requireActivity())
        .setTitle(title)
        .setView(view)
        .setPositiveButton(
            android.R.string.ok,
            (dialog, which) -> {
              Editable ed = editText.getText();
              if (ed != null && !TextUtils.isEmpty(ed.toString()) && callback != null) {
                callback.onInput(ed.toString());
              }
            })
        .setNegativeButton(android.R.string.cancel, null)
        .create();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    getLifecycle().addObserver(new SoftInputLifeCycleObserver(new WeakReference<>(editText)));
  }
}
