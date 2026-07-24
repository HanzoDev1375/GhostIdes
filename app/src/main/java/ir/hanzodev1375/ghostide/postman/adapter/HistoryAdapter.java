package ir.hanzodev1375.ghostide.postman.adapter;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import ir.hanzodev1375.ghostide.databinding.ItemHistoryPostmanBinding;
import java.util.List;
import ir.hanzodev1375.ghostide.postman.model.HistoryItem;
import ir.hanzodev1375.ghostide.postman.util.TimeUtils;
import ir.hanzodev1375.ghostide.postman.util.UiUtils;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

  public interface Listener {
    void onItemClick(HistoryItem item);

    void onDeleteClick(HistoryItem item);
  }

  private final List<HistoryItem> items;
  private final Listener listener;

  public HistoryAdapter(List<HistoryItem> items, Listener listener) {
    this.items = items;
    this.listener = listener;
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    ItemHistoryPostmanBinding binding =
        ItemHistoryPostmanBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
    return new ViewHolder(binding);
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    holder.bind(items.get(position));
  }

  @Override
  public int getItemCount() {
    return items.size();
  }

  class ViewHolder extends RecyclerView.ViewHolder {
    private final ItemHistoryPostmanBinding binding;
    

    ViewHolder(ItemHistoryPostmanBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }

    void bind(HistoryItem item) {
      binding.methodBadge.setText(item.method);
      int methodColor = UiUtils.methodColor(binding.getRoot().getContext(), item.method);
      binding.methodBadge.setBackgroundTintList(ColorStateList.valueOf(methodColor));

      binding.urlText.setText(item.url);

      if (item.statusCode > 0) {
        binding.statusChip.setText(String.valueOf(item.statusCode));
        int statusColor = UiUtils.statusColor(binding.getRoot().getContext(), item.statusCode);
        binding.statusChip.setBackgroundTintList(ColorStateList.valueOf(statusColor));
      } else {
        binding.statusChip.setText("failed");
        binding.statusChip.setBackgroundTintList(
            ColorStateList.valueOf(UiUtils.statusColor(binding.getRoot().getContext(), 500)));
      }

      String meta =
          TimeUtils.formatDuration(item.timeMs) + " · " + TimeUtils.relativeTime(item.timestamp);
      binding.metaText.setText(meta);

      binding.getRoot().setOnClickListener(v -> listener.onItemClick(item));
      binding.deleteButton.setOnClickListener(v -> listener.onDeleteClick(item));
    }
    
  }
}
