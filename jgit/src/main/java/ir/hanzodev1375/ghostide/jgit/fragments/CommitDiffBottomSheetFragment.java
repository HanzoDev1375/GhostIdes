package ir.hanzodev1375.ghostide.jgit.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import ir.hanzodev1375.components.sheet.BaseBlurBottomSheet;
import ir.hanzodev1375.ghostide.jgit.R;
import ir.hanzodev1375.ghostide.jgit.diff.GitDiffViewer;
import ir.hanzodev1375.ghostide.jgit.jgitandroid.datamanager.GitViewModel;
import ir.hanzodev1375.ghostide.jgit.jgitandroid.model.CommitInfo;
import ir.theme.M3Theme;

public class CommitDiffBottomSheetFragment extends BaseBlurBottomSheet {

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

  @Override
  protected void onContentReady(@NonNull ViewGroup contentContainer) {
    viewModel = new ViewModelProvider(requireActivity()).get(GitViewModel.class);

    View view =
        LayoutInflater.from(requireContext())
            .inflate(R.layout.bottom_sheet_commit_diff, contentContainer, false);
    contentContainer.addView(view);

    diffViewer = view.findViewById(R.id.diffViewer);
    progressBar = view.findViewById(R.id.progressBar);
    emptyText = view.findViewById(R.id.emptyText);
    M3Theme.apply(view);
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

  private void showDiff(String diff) {
    progressBar.setVisibility(View.GONE);
    if (TextUtils.isEmpty(diff) || "No changes in this commit.".equals(diff)) {
      diffViewer.setVisibility(View.GONE);
      emptyText.setVisibility(View.VISIBLE);
      emptyText.setText("Commit not found.".equals(diff) ? diff : "No changes to display");
    } else {
      emptyText.setVisibility(View.GONE);
      diffViewer.setVisibility(View.VISIBLE);
      diffViewer.parseDiffOutput(diff);
      diffViewer.applyMaterial3();
    }
  }
}
