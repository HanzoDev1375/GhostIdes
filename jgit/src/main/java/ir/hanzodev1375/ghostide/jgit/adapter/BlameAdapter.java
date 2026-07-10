package ir.hanzodev1375.ghostide.jgit.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.textview.MaterialTextView;
import java.util.ArrayList;
import java.util.List;
import ir.hanzodev1375.ghostide.jgit.R;
import ir.hanzodev1375.ghostide.jgit.jgitandroid.model.BlameInfo;

public class BlameAdapter extends RecyclerView.Adapter<BlameAdapter.VH> {
  private List<BlameInfo> list = new ArrayList<>();
  private OnBlameLineClickListener listener;

  public interface OnBlameLineClickListener {
    void onClick(BlameInfo blame);
  }

  public void setOnBlameLineClickListener(OnBlameLineClickListener listener) {
    this.listener = listener;
  }

  public void submitList(List<BlameInfo> l) {
    list = l != null ? l : new ArrayList<>();
    notifyDataSetChanged();
  }

  @NonNull
  @Override
  public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    return new VH(
        LayoutInflater.from(parent.getContext()).inflate(R.layout.item_blame, parent, false));
  }

  @Override
  public void onBindViewHolder(@NonNull VH h, int pos) {
    BlameInfo b = list.get(pos);
    h.tvLine.setText(String.valueOf(b.getLineNumber()));
    h.tvHash.setText(b.getShortHash());
    h.tvAuthor.setText(b.getAuthor());
    h.tvContent.setText(b.getContent());
    h.itemView.setOnClickListener(
        v -> {
          if (listener != null) listener.onClick(b);
        });
  }

  @Override
  public int getItemCount() {
    return list.size();
  }

  static class VH extends RecyclerView.ViewHolder {
    MaterialTextView tvLine, tvHash, tvAuthor, tvContent;

    VH(View v) {
      super(v);
      tvLine = v.findViewById(R.id.tvBlameLineNum);
      tvHash = v.findViewById(R.id.tvBlameHash);
      tvAuthor = v.findViewById(R.id.tvBlameAuthor);
      tvContent = v.findViewById(R.id.tvBlameContent);
    }
  }
}
