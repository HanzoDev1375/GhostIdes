package ir.hanzodev1375.ghostide.postman.adapter;

import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ir.hanzodev1375.ghostide.databinding.ItemKeyValueBinding;
import ir.hanzodev1375.ghostide.postman.model.KeyValueItem;
import ir.hanzodev1375.ghostide.postman.util.SimpleTextWatcher;

/**
 * Backs the editable rows in Params / Headers / Form-body. Same adapter, three different backing
 * lists — the hints are the only thing that differ.
 */
public class KeyValueAdapter extends RecyclerView.Adapter<KeyValueAdapter.ViewHolder> {

  public interface OnChangeListener {
    void onChanged();
  }

  private final List<KeyValueItem> items;
  private OnChangeListener listener;

  public KeyValueAdapter(List<KeyValueItem> items) {
    this.items = items;
  }

  public void setOnChangeListener(OnChangeListener listener) {
    this.listener = listener;
  }

  public void addRow() {
    items.add(new KeyValueItem());
    notifyItemInserted(items.size() - 1);
  }

  private void removeAt(int position) {
    if (position < 0 || position >= items.size()) return;
    items.remove(position);
    notifyItemRemoved(position);
    if (listener != null) listener.onChanged();
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    ItemKeyValueBinding binding =
        ItemKeyValueBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
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
    private final ItemKeyValueBinding binding;
    private TextWatcher keyWatcher;
    private TextWatcher valueWatcher;

    ViewHolder(ItemKeyValueBinding binding) {
      super(binding.getRoot());
      this.binding = binding;

      binding.deleteButton.setOnClickListener(
          v -> {
            int pos = getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) removeAt(pos);
          });

      binding.enabledCheckbox.setOnCheckedChangeListener(
          (buttonView, isChecked) -> {
            int pos = getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
              items.get(pos).setEnabled(isChecked);
              if (listener != null) listener.onChanged();
            }
          });
    }

    void bind(KeyValueItem item) {
      if (keyWatcher != null) binding.keyInput.removeTextChangedListener(keyWatcher);
      if (valueWatcher != null) binding.valueInput.removeTextChangedListener(valueWatcher);

      binding.keyInput.setText(item.getKey());
      binding.valueInput.setText(item.getValue());
      binding.enabledCheckbox.setChecked(item.isEnabled());

      keyWatcher =
          new SimpleTextWatcher(
              text -> {
                int pos = getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) items.get(pos).setKey(text);
              });
      valueWatcher =
          new SimpleTextWatcher(
              text -> {
                int pos = getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) items.get(pos).setValue(text);
              });

      binding.keyInput.addTextChangedListener(keyWatcher);
      binding.valueInput.addTextChangedListener(valueWatcher);
    }
  }
}
