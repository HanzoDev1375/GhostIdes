package ir.hanzodev1375.ghostide.postman.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ir.hanzodev1375.ghostide.databinding.ItemCollectionBinding;
import ir.hanzodev1375.ghostide.postman.model.RequestCollection;

public class CollectionAdapter extends RecyclerView.Adapter<CollectionAdapter.ViewHolder> {

  public interface Listener {
    void onItemClick(RequestCollection collection);

    void onDeleteClick(RequestCollection collection);
  }

  private final List<RequestCollection> items;
  private final Listener listener;

  public CollectionAdapter(List<RequestCollection> items, Listener listener) {
    this.items = items;
    this.listener = listener;
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    ItemCollectionBinding binding =
        ItemCollectionBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
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
    private final ItemCollectionBinding binding;

    ViewHolder(ItemCollectionBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }

    void bind(RequestCollection collection) {
      binding.nameText.setText(collection.name);
      int count = collection.requestCount;
      binding.countText.setText(count == 1 ? "1 request" : count + " requests");
      binding.getRoot().setOnClickListener(v -> listener.onItemClick(collection));
      binding.deleteButton.setOnClickListener(v -> listener.onDeleteClick(collection));
    }
  }
}
