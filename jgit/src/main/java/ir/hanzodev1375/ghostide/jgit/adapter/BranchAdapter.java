package ir.hanzodev1375.ghostide.jgit.adapter;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import ir.hanzodev1375.ghostide.jgit.R;
import ir.theme.M3Theme;

public class BranchAdapter extends RecyclerView.Adapter<BranchAdapter.ViewHolder> {
  private List<String> branches = new ArrayList<>();
  private String currentBranch;
  private OnBranchActionListener listener;

  public interface OnBranchActionListener {
    void onCheckout(String branchName);
    void onDelete(String branchName);
    void onMerge(String branchName);
    void onRebase(String branchName);
  }

  public void setOnBranchActionListener(OnBranchActionListener listener) {
    this.listener = listener;
  }

  public void submitList(List<String> list, String current) {
    branches = list != null ? list : new ArrayList<>();
    currentBranch = current;
    notifyDataSetChanged();
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view =
        LayoutInflater.from(parent.getContext()).inflate(R.layout.item_branch, parent, false);
    return new ViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    String branch = branches.get(position);
    holder.tvBranchName.setText(branch);
    if (branch.equals(currentBranch)) {
      holder.tvBranchName.append(" (current)");
      holder.btnCheckout.setEnabled(false);
    } else {
      holder.btnCheckout.setEnabled(true);
    }

    holder.btnCheckout.setOnClickListener(
        v -> {
          if (listener != null) listener.onCheckout(branch);
        });
    holder.btnDelete.setOnClickListener(
        v -> {
          if (listener != null && !branch.equals(currentBranch)) listener.onDelete(branch);
        });
    if (holder.btnMerge != null) {
      holder.btnMerge.setOnClickListener(v -> {
        if (listener != null && !branch.equals(currentBranch)) listener.onMerge(branch);
      });
      holder.btnMerge.setEnabled(!branch.equals(currentBranch));
    }
    if (holder.btnRebase != null) {
      holder.btnRebase.setOnClickListener(v -> {
        if (listener != null && !branch.equals(currentBranch)) listener.onRebase(branch);
      });
      holder.btnRebase.setEnabled(!branch.equals(currentBranch));
    }
    M3Theme.listCard(holder.itemView);
    Integer error = M3Theme.error();
    if (error != null) holder.btnDelete.setBackgroundTintList(ColorStateList.valueOf(error));
  }

  @Override
  public int getItemCount() {
    return branches.size();
  }

  static class ViewHolder extends RecyclerView.ViewHolder {
    TextView tvBranchName;
    Button btnCheckout, btnDelete, btnMerge, btnRebase;

    ViewHolder(View itemView) {
      super(itemView);
      tvBranchName = itemView.findViewById(R.id.tvBranchName);
      btnCheckout = itemView.findViewById(R.id.btnCheckout);
      btnDelete = itemView.findViewById(R.id.btnDelete);
      btnMerge = itemView.findViewById(R.id.btnMerge);
      btnRebase = itemView.findViewById(R.id.btnRebase);
    }
  }
}
