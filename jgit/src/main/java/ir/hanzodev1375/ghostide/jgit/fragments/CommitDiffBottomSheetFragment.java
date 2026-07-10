package ir.hanzodev1375.ghostide.jgit.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import ir.hanzodev1375.ghostide.jgit.R;
import ir.hanzodev1375.ghostide.jgit.diff.GitDiffViewer;
import ir.hanzodev1375.ghostide.jgit.jgitandroid.datamanager.GitViewModel;
import ir.hanzodev1375.ghostide.jgit.jgitandroid.model.CommitInfo;

public class CommitDiffBottomSheetFragment extends BottomSheetDialogFragment {

  private static final String ARG_HASH = "commit_hash";
  private static final String ARG_SHORT_HASH = "commit_short_hash";
  private static final String ARG_MESSAGE = "commit_message";

  private GitViewModel viewModel;
  private GitDiffViewer diffViewer;
  private ProgressBar progressBar;
  private TextView emptyText;

  public static CommitDiffBottomSheetFragment newInstance(CommitInfo commit) {
    return newInstance(commit.getHash(), commit.getShortHash(), commit.getMessage());
  }

  public static CommitDiffBottomSheetFragment newInstance(
      String hash, String shortHash, String message) {
    CommitDiffBottomSheetFragment fragment = new CommitDiffBottomSheetFragment();
    Bundle args = new Bundle();
    args.putString(ARG_HASH, hash);
    args.putString(ARG_SHORT_HASH, shortHash);
    args.putString(ARG_MESSAGE, message);
    fragment.setArguments(args);
    return fragment;
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.bottom_sheet_commit_diff, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    viewModel = new ViewModelProvider(requireActivity()).get(GitViewModel.class);

    diffViewer = view.findViewById(R.id.diffViewer);
    progressBar = view.findViewById(R.id.progressBar);
    emptyText = view.findViewById(R.id.emptyText);

    TextView tvHash = view.findViewById(R.id.tvCommitHash);
    TextView tvMessage = view.findViewById(R.id.tvCommitMessage);

    Bundle args = getArguments();
    String hash = args != null ? args.getString(ARG_HASH) : null;
    tvHash.setText(args != null ? args.getString(ARG_SHORT_HASH) : "");
    tvMessage.setText(args != null ? args.getString(ARG_MESSAGE) : "");

    viewModel.commitDiff.observe(getViewLifecycleOwner(), this::showDiff);

    if (!TextUtils.isEmpty(hash)) {
      progressBar.setVisibility(View.VISIBLE);
      diffViewer.setVisibility(View.GONE);
      emptyText.setVisibility(View.GONE);
      viewModel.loadCommitDiff(hash);
    }
  }

  @Override
  public void onStart() {
    super.onStart();
    Dialog dialog = getDialog();
    if (dialog == null) {
      return;
    }
    View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
    if (bottomSheet != null) {
      BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
      behavior.setSkipCollapsed(true);
      behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }
  }

  private void showDiff(String diff) {
    progressBar.setVisibility(View.GONE);
    if (TextUtils.isEmpty(diff) || "No changes in this commit.".equals(diff)) {
      diffViewer.setVisibility(View.GONE);
      emptyText.setVisibility(View.VISIBLE);
      emptyText.setText(
          "Commit not found.".equals(diff) ? diff : "No changes to display");
    } else {
      emptyText.setVisibility(View.GONE);
      diffViewer.setVisibility(View.VISIBLE);
      diffViewer.parseDiffOutput(diff);
      diffViewer.applyMaterial3();
    }
  }
}
