package com.termux.view.completion;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.textview.MaterialTextView;
import com.termux.view.R;
import java.util.ArrayList;
import java.util.List;
import ir.theme.M3Theme;

/**
 * {@link RecyclerView.Adapter} for showing terminal completion items. Colors are applied through
 * {@link M3Theme}.
 */
public class TerminalCompletionAdapter extends RecyclerView.Adapter<TerminalCompletionAdapter.VH> {

  /** Callback for when the user selects / clicks an item. */
  public interface OnItemClickListener {
    void onItemClick(TerminalCompletionItem item, int position);
  }

  private final List<TerminalCompletionItem> items = new ArrayList<>();
  private OnItemClickListener listener;
  private int currentSelection = -1;

  public void setOnItemClickListener(OnItemClickListener listener) {
    this.listener = listener;
  }

  /** Replace all items and refresh the list. */
  public void setItems(List<TerminalCompletionItem> newItems) {
    items.clear();
    if (newItems != null) {
      items.addAll(newItems);
    }
    currentSelection = -1;
    notifyDataSetChanged();
  }

  public void setCurrentSelection(int position) {
    if (currentSelection != position) {
      int old = currentSelection;
      currentSelection = position;
      if (old >= 0 && old < items.size()) {
        notifyItemChanged(old);
      }
      if (position >= 0 && position < items.size()) {
        notifyItemChanged(position);
      }
    }
  }

  public int getCurrentSelection() {
    return currentSelection;
  }

  public TerminalCompletionItem getItem(int position) {
    if (position >= 0 && position < items.size()) {
      return items.get(position);
    }
    return null;
  }

  public List<TerminalCompletionItem> getItems() {
    return new ArrayList<>(items);
  }

  /* ------------------------------ RecyclerView ------------------------------ */

  @NonNull
  @Override
  public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view =
        LayoutInflater.from(parent.getContext())
            .inflate(R.layout.terminal_completion_item, parent, false);
    return new VH(view);
  }

  @Override
  public void onBindViewHolder(@NonNull VH holder, int position) {
    TerminalCompletionItem item = items.get(position);
    if (item == null) {
      return;
    }

    holder.label.setText(item.getLabel());
    M3Theme.text(holder.label);

    if (item.getDescription() != null && !item.getDescription().isEmpty()) {
      holder.desc.setText(item.getDescription());
      holder.desc.setVisibility(View.VISIBLE);
      M3Theme.text(holder.desc);
    } else {
      holder.desc.setVisibility(View.GONE);
    }

    holder.icon.setImageDrawable(TerminalCompletionIconDrawable.forKind(item.getKind()));

    boolean selected = position == currentSelection;
    M3Theme.applyShallow(holder.root);
    if (selected) {
      holder.root.setBackgroundColor(
          M3Theme.primaryContainer() != null ? M3Theme.primaryContainer() : 0x22FFFFFF);
    }

    holder.root.setOnClickListener(
        v -> {
          if (listener != null) {
            listener.onItemClick(item, holder.getBindingAdapterPosition());
          }
        });
  }

  @Override
  public int getItemCount() {
    return items.size();
  }

  /* --------------------------------- VH --------------------------------- */

  public static class VH extends RecyclerView.ViewHolder {
    final View root;
    final ImageView icon;
    final MaterialTextView label;
    final MaterialTextView desc;

    public VH(@NonNull View itemView) {
      super(itemView);
      root = itemView;
      icon = itemView.findViewById(R.id.completion_item_icon);
      label = itemView.findViewById(R.id.completion_item_label);
      desc = itemView.findViewById(R.id.completion_item_desc);
    }
  }
}
