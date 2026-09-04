package ir.hanzodev1375.components;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.textfield.TextInputEditText;
import ir.hanzodev1375.components.utils.AndroidUtils;
import ir.hanzodev1375.components.utils.SoftInputLifeCycleObserver;
import java.lang.ref.WeakReference;
import ir.hanzodev1375.components.sheet.customitemsheet.ui.DialogCompat;
import ir.theme.M3Theme;
public class RenameDialogFragment extends DialogFragment {
  public static final String TAG = RenameDialogFragment.class.getSimpleName();

  public interface OnRenameFilesInterface {
    void onRename(@NonNull String prefix, @Nullable String extension);
  }

  private static final String ARG_NAME = "name";

  @NonNull
  public static RenameDialogFragment getInstance(
      @Nullable String name, @Nullable OnRenameFilesInterface renameFilesInterface) {
    RenameDialogFragment fragment = new RenameDialogFragment();
    Bundle args = new Bundle();
    args.putString(ARG_NAME, name);
    fragment.setArguments(args);
    fragment.setOnRenameFilesInterface(renameFilesInterface);
    return fragment;
  }

  @Nullable private OnRenameFilesInterface mOnRenameFilesInterface;
  private View mDialogView;
  private TextInputEditText mEditText;

  @NonNull
  @Override
  public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
    String name = getArguments() != null ? requireArguments().getString(ARG_NAME) : null;
    mDialogView = View.inflate(requireActivity(), R.layout.dialog_rename, null);
    mEditText = mDialogView.findViewById(R.id.rename);
    mEditText.setText(name);
    if (name != null) {
      int lastIndex = name.lastIndexOf('.');
      if (lastIndex != -1 || lastIndex == name.length() - 1) {
        mEditText.setSelection(0, lastIndex);
      } else {
        mEditText.selectAll();
      }
    }
    return new DialogCompat(requireActivity())
        .setTitle(requireActivity().getString(R.string.dialog_rename, " ?"))
        .setMessage(requireActivity().getString(R.string.dialog_rename_massges,name +"?"))
        .setView(mDialogView)
        .setPositiveButton(
            android.R.string.ok,
            (dialog, which) -> {
              Editable editable = mEditText.getText();
              if (!TextUtils.isEmpty(editable) && mOnRenameFilesInterface != null) {
                String newName = editable.toString();
                String prefix = AndroidUtils.trimPathExtension(newName);
                String extension = AndroidUtils.getPathExtension(newName, false);
                mOnRenameFilesInterface.onRename(prefix, extension);
              }
            })
        .setNegativeButton(android.R.string.cancel, null)
        .create();
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    M3Theme.apply(mDialogView);
    return mDialogView;
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    getLifecycle().addObserver(new SoftInputLifeCycleObserver(new WeakReference<>(mEditText)));
  }

  public void setOnRenameFilesInterface(@Nullable OnRenameFilesInterface renameFilesInterface) {
    mOnRenameFilesInterface = renameFilesInterface;
  }
}
