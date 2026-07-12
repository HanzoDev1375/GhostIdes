package ir.hanzodev1375.ghostide.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.TooltipCompat;
import androidx.recyclerview.widget.RecyclerView;
import ir.hanzodev1375.ghostide.interfaces.OnItemClickListener;
import ir.hanzodev1375.ghostide.models.ToolbarModel;
import ir.theme.ThemeManager;
import ir.theme.ThemeUtils;
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
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    lp.setMargins(marginInPx, 0, marginInPx, 0);
    imageView.setLayoutParams(lp);

    imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
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
      ThemeManager manager = new ThemeManager(context);
      ThemeUtils theme = new ThemeUtils(manager);
      theme.applyImageView(icon);
      icon.setImageResource(model.getIcon());
      icon.setOnClickListener(
          v -> {
            if (clickListener != null) {
              clickListener.onClick(v, model, getBindingAdapterPosition());
            }
          });
      icon.setVisibility(model.isShowVisblityItem() ? View.VISIBLE : View.GONE);
      icon.setOnLongClickListener(
          v -> {
            TooltipCompat.setTooltipText(v, model.getTag());
            return false;
          });
    }
  }
}
