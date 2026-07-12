package ir.hanzodev1375.ghostide.terminal.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.color.MaterialColors;
import ir.hanzodev1375.ghostide.R;
import ir.hanzodev1375.ghostide.databinding.ItemTerminalTabBinding;
import ir.hanzodev1375.ghostide.terminal.TerminalTab;
import java.util.List;

/** آدابتور نوار تب‌های سشن ترمینال (مثل تب‌های مرورگر، کنار هم و اسکرول‌شونده افقی). */
public class TerminalTabAdapter extends RecyclerView.Adapter<TerminalTabAdapter.ViewHolder> {

  public interface Listener {
    void onTabSelected(int position);

    void onTabClosed(int position);
  }

  private final List<TerminalTab> tabs;
  private final Listener listener;
  private int selectedPosition = 0;

  public TerminalTabAdapter(List<TerminalTab> tabs, Listener listener) {
    this.tabs = tabs;
    this.listener = listener;
    setHasStableIds(true);
  }

  public void setSelectedPosition(int position) {
    int previous = selectedPosition;
    selectedPosition = position;
    if (previous >= 0 && previous < tabs.size()) notifyItemChanged(previous);
    if (selectedPosition >= 0 && selectedPosition < tabs.size())
      notifyItemChanged(selectedPosition);
  }

  @Override
  public long getItemId(int position) {
    return tabs.get(position).id;
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    ItemTerminalTabBinding binding =
        ItemTerminalTabBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
    return new ViewHolder(binding);
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    TerminalTab tab = tabs.get(position);
    holder.binding.tabTitle.setText(tab.getDisplayTitle());

    boolean isSelected = position == selectedPosition;
    holder.binding.selectionIndicator.setVisibility(isSelected ? View.VISIBLE : View.INVISIBLE);
    holder.binding.tabTitle.setTypeface(
        null, isSelected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
    int textColor =
        isSelected
            ? MaterialColors.getColor(
                holder.itemView, R.attr.colorPrimary)
            : MaterialColors.getColor(
                holder.itemView, R.attr.colorOnSurfaceVariant);
    holder.binding.tabTitle.setTextColor(textColor);

    holder.itemView.setOnClickListener(
        v -> {
          int pos = holder.getBindingAdapterPosition();
          if (pos != RecyclerView.NO_POSITION) listener.onTabSelected(pos);
        });
    holder.binding.tabClose.setOnClickListener(
        v -> {
          int pos = holder.getBindingAdapterPosition();
          if (pos != RecyclerView.NO_POSITION) listener.onTabClosed(pos);
        });
  }

  @Override
  public int getItemCount() {
    return tabs.size();
  }

  class ViewHolder extends RecyclerView.ViewHolder {
    ItemTerminalTabBinding binding;

    ViewHolder(ItemTerminalTabBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }
  }
}
