package ir.hanzodev1375.ghostide.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.TooltipCompat;
import androidx.recyclerview.widget.RecyclerView;
import ir.theme.M3Theme;
import ir.hanzodev1375.ghostide.interfaces.OnItemClickListener;
import ir.hanzodev1375.ghostide.models.ToolbarModel;
import java.util.List;

public class ToolbarListAdapter extends RecyclerView.Adapter<ToolbarListAdapter.VH> {

  private List<ToolbarModel> listModel;
  private OnItemClickListener<ToolbarModel> clickListener;
  private Context context;

  public ToolbarListAdapter(
      List<ToolbarModel> listModel,
      OnItemClickListener<ToolbarModel> clickListener,
      Context context) {
    this.listModel = listModel;
    this.clickListener = clickListener;
    this.context = context;
  }

  @NonNull
  @Override
  public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    ImageView imageView = new ImageView(parent.getContext());
    float density = parent.getContext().getResources().getDisplayMetrics().density;
    int marginInPx = (int) (3 * density + 0.5f);
    RecyclerView.LayoutParams lp =
        new RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
    lp.setMargins(marginInPx, 0, marginInPx, 0);
    imageView.setLayoutParams(lp);

    imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
    imageView.setPadding((int) (2 * density + 0.5f), 0, (int) (2 * density + 0.5f), 0);
    return new VH(imageView);
  }

  @Override
  public void onBindViewHolder(@NonNull VH holder, int position) {
    ToolbarModel model = listModel.get(position);
    holder.bind(model, clickListener);
  }

  @Override
  public int getItemCount() {
    return listModel == null ? 0 : listModel.size();
  }

  class VH extends RecyclerView.ViewHolder {
    private ImageView icon;

    public VH(@NonNull ImageView itemView) {
      super(itemView);
      this.icon = itemView;
    }

    public void bind(ToolbarModel model, OnItemClickListener<ToolbarModel> clickListener) {
      int tint = fallback(M3Theme.onSurface(), Color.parseColor("#8A000000"));
      icon.setColorFilter(tint);
      icon.setImageResource(model.getIcon());
      icon.setOnClickListener(
          v -> {
            if (clickListener != null) {
              clickListener.onClick(v, model, getBindingAdapterPosition());
            }
          });
      if (model.isShowVisblityItem()) {
        icon.setEnabled(true);
        icon.setAlpha(1f);
      } else {
        icon.setEnabled(false);
        icon.setAlpha(0.5f);
      }
      icon.setOnLongClickListener(
          v -> {
            TooltipCompat.setTooltipText(v, model.getTag());
            return false;
          });
    }
  }

  private static int fallback(Integer value, int def) {
    return value != null ? value : def;
  }
}
