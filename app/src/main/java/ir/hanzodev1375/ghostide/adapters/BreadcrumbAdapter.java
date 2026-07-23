package ir.hanzodev1375.ghostide.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import ir.hanzodev1375.ghostide.codeeditors.langs.lsp.model.BreadcrumbItem;
import java.util.ArrayList;
import java.util.List;

import ir.hanzodev1375.ghostide.R;

public class BreadcrumbAdapter extends RecyclerView.Adapter<BreadcrumbAdapter.ViewHolder> {

  public interface OnItemClickListener {
    void onBreadcrumbClick(BreadcrumbItem item);
  }

  private final List<BreadcrumbItem> items = new ArrayList<>();
  private OnItemClickListener listener;

  public void setOnItemClickListener(OnItemClickListener listener) {
    this.listener = listener;
  }

  public void setItems(List<BreadcrumbItem> newItems) {
    items.clear();
    if (newItems != null) items.addAll(newItems);
    notifyDataSetChanged();
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view =
        LayoutInflater.from(parent.getContext()).inflate(R.layout.item_breadcrumb, parent, false);
    return new ViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    var item = items.get(position);
    holder.name.setText(item.getName());
    holder.separator.setVisibility(position == 0 ? View.GONE : View.VISIBLE);
    holder.itemView.setOnClickListener(
        v -> {
          if (listener != null) listener.onBreadcrumbClick(item);
        });
  }

  @Override
  public int getItemCount() {
    return items.size();
  }

  static class ViewHolder extends RecyclerView.ViewHolder {

    final TextView name;
    final TextView separator;

    ViewHolder(@NonNull View itemView) {
      super(itemView);
      name = itemView.findViewById(R.id.tvBreadcrumbName);
      separator = itemView.findViewById(R.id.tvBreadcrumbSeparator);
    }
  }
}
