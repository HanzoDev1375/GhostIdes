package ir.hanzodev1375.ghostide.ai.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.List;

import ir.hanzodev1375.ghostide.ai.R;
import ir.hanzodev1375.ghostide.ai.model.AttachedFile;
import ir.theme.M3Theme;

public class AttachedFilesAdapter
    extends RecyclerView.Adapter<AttachedFilesAdapter.FileViewHolder> {

  public interface OnRemoveListener {
    void onRemove(int position);
  }

  private final List<AttachedFile> files;
  private final OnRemoveListener listener;

  public AttachedFilesAdapter(List<AttachedFile> files, OnRemoveListener listener) {
    this.files = files;
    this.listener = listener;
  }

  @NonNull
  @Override
  public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View v =
        LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_attached_file, parent, false);
    return new FileViewHolder(v);
  }

  @Override
  public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
    AttachedFile file = files.get(position);

    if (file.isImage()) {
      holder.ivPreview.setVisibility(View.VISIBLE);
      holder.tvFileName.setVisibility(View.GONE);
      holder.ivFileIcon.setVisibility(View.GONE);

      Glide.with(holder.itemView.getContext())
          .load(file.getUri())
          .diskCacheStrategy(DiskCacheStrategy.NONE)
          .skipMemoryCache(true)
          .centerCrop()
          .into(holder.ivPreview);
    } else {
      holder.ivPreview.setVisibility(View.GONE);
      holder.tvFileName.setVisibility(View.VISIBLE);
      holder.ivFileIcon.setVisibility(View.VISIBLE);
      holder.tvFileName.setText(file.getName());
    }

    holder.btnRemove.setOnClickListener(
        v -> {
          int pos = holder.getBindingAdapterPosition();
          if (pos != RecyclerView.NO_ID) {
            listener.onRemove(pos);
          }
        });
    M3Theme.listCard(holder.itemView);
  }

  @Override
  public int getItemCount() {
    return files.size();
  }

  static class FileViewHolder extends RecyclerView.ViewHolder {
    ImageView ivPreview;
    ImageView ivFileIcon;
    TextView tvFileName;
    ImageButton btnRemove;

    FileViewHolder(@NonNull View itemView) {
      super(itemView);
      ivPreview = itemView.findViewById(R.id.iv_file_preview);
      ivFileIcon = itemView.findViewById(R.id.iv_file_icon);
      tvFileName = itemView.findViewById(R.id.tv_file_name);
      btnRemove = itemView.findViewById(R.id.btn_remove_file);
    }
  }
}