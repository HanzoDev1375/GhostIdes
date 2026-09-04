package ir.hanzodev1375.components.searchdata.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import ir.hanzodev1375.ghostide.materialfileicon.core.FileIconHelper;
import java.util.ArrayList;
import java.util.List;
import ir.hanzodev1375.components.R;
import ir.hanzodev1375.components.searchdata.interfaces.OnLineClickListener;
import ir.hanzodev1375.components.searchdata.model.FileSearchResult;
import ir.theme.M3Theme;

public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.VH> {
  private final List<FileSearchResult> items = new ArrayList<>();
  private OnLineClickListener listener;

  public void setListener(OnLineClickListener listener) {
    this.listener = listener;
  }

  public void submitList(List<FileSearchResult> list) {
    items.clear();
    if (list != null) items.addAll(list);
    notifyDataSetChanged();
  }

  public void clear() {
    int size = items.size();
    items.clear();
    notifyItemRangeRemoved(0, size);
  }

  @NonNull
  @Override
  public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View v =
        LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_search_result, parent, false);
    return new VH(v);
  }

  @Override
  public void onBindViewHolder(@NonNull VH holder, int position) {
    FileSearchResult result = items.get(position);
    holder.tvFileName.setText(result.getFileName());
    holder.tvFilePath.setText(result.getFilePath());
    Glide.with(holder.ivFileIcon)
        .load(new FileIconHelper(result.getFilePath()).getFileIcon())
        .into(holder.ivFileIcon);
    holder.itemView.setOnClickListener(
        v -> {
          if (listener != null) listener.onFileClick(result);
        });
    if (result.hasContentMatches()) {
      holder.rvContentMatches.setVisibility(View.VISIBLE);
      holder.rvContentMatches.setLayoutManager(
          new LinearLayoutManager(holder.itemView.getContext()));
      holder.rvContentMatches.setAdapter(
          new ContentMatchAdapter(result.getContentMatches(), result.getFilePath(), listener));
    } else {
      holder.rvContentMatches.setVisibility(View.GONE);
    }
    M3Theme.listCard(holder.itemView);
  }

  @Override
  public int getItemCount() {
    return items.size();
  }

  static class VH extends RecyclerView.ViewHolder {
    ImageView ivFileIcon;
    TextView tvFileName, tvFilePath;
    RecyclerView rvContentMatches;

    VH(@NonNull View itemView) {
      super(itemView);
      ivFileIcon = itemView.findViewById(R.id.ivFileIcon);
      tvFileName = itemView.findViewById(R.id.tvFileName);
      tvFilePath = itemView.findViewById(R.id.tvFilePath);
      rvContentMatches = itemView.findViewById(R.id.rvContentMatches);
      M3Theme.text(tvFileName,tvFilePath);
      
    }
  }
}
