package ir.hanzodev1375.ghostide.postman.adapter;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ir.hanzodev1375.ghostide.databinding.ItemSavedRequestBinding;
import ir.hanzodev1375.ghostide.postman.model.SavedRequest;
import ir.hanzodev1375.ghostide.postman.util.UiUtils;
import ir.theme.M3Theme;

public class SavedRequestAdapter extends RecyclerView.Adapter<SavedRequestAdapter.ViewHolder> {

  public interface Listener {
    void onItemClick(SavedRequest request);

    void onDeleteClick(SavedRequest request);
  }

  private final List<SavedRequest> items;
  private final Listener listener;

  public SavedRequestAdapter(List<SavedRequest> items, Listener listener) {
    this.items = items;
    this.listener = listener;
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    ItemSavedRequestBinding binding =
        ItemSavedRequestBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
    return new ViewHolder(binding);
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    holder.bind(items.get(position));
    M3Theme.listCard(holder.itemView);
  }

  @Override
  public int getItemCount() {
    return items.size();
  }

  class ViewHolder extends RecyclerView.ViewHolder {
    private final ItemSavedRequestBinding binding;

    ViewHolder(ItemSavedRequestBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }

    void bind(SavedRequest request) {
      binding.methodBadge.setText(request.method);
      int color = UiUtils.methodColor(binding.getRoot().getContext(), request.method);
      binding.methodBadge.setBackgroundTintList(ColorStateList.valueOf(color));
      binding.nameText.setText(request.name);
      binding.urlText.setText(request.url);
      binding.getRoot().setOnClickListener(v -> listener.onItemClick(request));
      binding.deleteButton.setOnClickListener(v -> listener.onDeleteClick(request));
    }
  }
}
