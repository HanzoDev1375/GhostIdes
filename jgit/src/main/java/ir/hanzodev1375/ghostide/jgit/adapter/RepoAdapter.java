package ir.hanzodev1375.ghostide.jgit.adapter;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.listitem.ListItemCardView;
import com.google.android.material.listitem.ListItemViewHolder;
import ir.hanzodev1375.ghostide.jgit.R;
import ir.hanzodev1375.ghostide.jgit.model.GitHubRepo;
import java.util.List;

public class RepoAdapter extends RecyclerView.Adapter<RepoAdapter.VH> {

  private final List<GitHubRepo> list;

  public interface OnItemClickListener {
    void onClick(GitHubRepo repo);
  }

  private OnItemClickListener listener;

  public RepoAdapter(List<GitHubRepo> list) {
    this.list = list;
  }

  public void setOnItemClickListener(OnItemClickListener l) {
    this.listener = l;
  }

  @NonNull
  @Override
  public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_repo, parent, false);
    return new VH(v);
  }

  @Override
  public void onBindViewHolder(@NonNull VH h, int position) {
    GitHubRepo repo = list.get(position);
    h.bind(position, getItemCount());
    h.name.setText(repo.getName());
    h.stars.setText(String.valueOf(repo.getStars()));

    if (repo.getDescription() != null && !repo.getDescription().isEmpty()) {
      h.desc.setVisibility(View.VISIBLE);
      h.desc.setText(repo.getDescription());
    } else {
      h.desc.setVisibility(View.GONE);
    }

    if (repo.getLanguage() != null && !repo.getLanguage().isEmpty()) {
      h.langDot.setVisibility(View.VISIBLE);
      h.language.setVisibility(View.VISIBLE);
      h.language.setText(repo.getLanguage());
    } else {
      h.langDot.setVisibility(View.GONE);
      h.language.setVisibility(View.GONE);
    }

    h.ivFork.setVisibility(repo.isFork() ? View.VISIBLE : View.GONE);
    h.tvFork.setVisibility(repo.isFork() ? View.VISIBLE : View.GONE);
    h.chipPrivate.setVisibility(repo.isPrivate() ? View.VISIBLE : View.GONE);
    h.repoIcon.setImageResource(
        repo.isFork() ? R.drawable.fork_right_24px : R.drawable.deployed_code_24px);

    h.itemView.setOnClickListener(
        v -> {
          if (listener != null) listener.onClick(repo);
        });
  }

  @Override
  public int getItemCount() {
    return list.size();
  }

  static class VH extends ListItemViewHolder {
    ImageView repoIcon, langDot, ivFork;
    TextView name, desc, language, stars, tvFork;
    Chip chipPrivate;
    ListItemCardView cardrepo;

    VH(View v) {
      super(v);
      repoIcon = v.findViewById(R.id.ivRepoIcon);
      name = v.findViewById(R.id.tvRepoName);
      desc = v.findViewById(R.id.tvDescription);
      langDot = v.findViewById(R.id.ivLangDot);
      language = v.findViewById(R.id.tvLanguage);
      stars = v.findViewById(R.id.tvStars);
      ivFork = v.findViewById(R.id.ivFork);
      tvFork = v.findViewById(R.id.tvFork);
      chipPrivate = v.findViewById(R.id.chipPrivate);
      cardrepo = v.findViewById(R.id.cardrepo);
      cardrepo.setCardBackgroundColor(
          ColorStateList.valueOf(
              ColorUtils.setAlphaComponent(
                  MaterialColors.getColor(cardrepo, R.attr.colorSurfaceContainer), 128)));
    }
  }
}
