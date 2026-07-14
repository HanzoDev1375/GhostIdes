package ir.hanzodev1375.ghostide.refactor.rename.ui;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import ir.hanzodev1375.ghostide.databinding.ItemPreviewEntryBinding;
import ir.hanzodev1375.ghostide.refactor.rename.model.PreviewEntry;

public final class PreviewEntryAdapter
    extends ListAdapter<PreviewEntry, PreviewEntryAdapter.ViewHolder> {

  private static final DiffUtil.ItemCallback<PreviewEntry> DIFF_CALLBACK =
      new DiffUtil.ItemCallback<PreviewEntry>() {
        @Override
        public boolean areItemsTheSame(
            @NonNull PreviewEntry oldItem, @NonNull PreviewEntry newItem) {
          return oldItem.getFilePath().equals(newItem.getFilePath())
              && oldItem.getCategory().equals(newItem.getCategory());
        }

        @Override
        public boolean areContentsTheSame(
            @NonNull PreviewEntry oldItem, @NonNull PreviewEntry newItem) {
          return oldItem.getChangeCount() == newItem.getChangeCount()
              && oldItem.getDescription().equals(newItem.getDescription());
        }
      };

  public PreviewEntryAdapter() {
    super(DIFF_CALLBACK);
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    ItemPreviewEntryBinding binding =
        ItemPreviewEntryBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
    return new ViewHolder(binding);
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    holder.bind(getItem(position));
  }

  static final class ViewHolder extends RecyclerView.ViewHolder {

    private final ItemPreviewEntryBinding binding;

    ViewHolder(ItemPreviewEntryBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }

    void bind(PreviewEntry entry) {
      binding.categoryText.setText(entry.getCategory());
      binding.descriptionText.setText(entry.getDescription());
      binding.filePathText.setText(entry.getFilePath());
      binding.countText.setText(String.valueOf(entry.getChangeCount()));
    }
  }
}
