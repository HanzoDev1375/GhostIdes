package ir.hanzodev1375.ghostide.appicon;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import ir.hanzodev1375.ghostide.R;
import java.util.Arrays;
import java.util.List;

/** Grid adapter showing every {@link AppIcon} with a checkmark on the selected one. */
public class AppIconAdapter extends RecyclerView.Adapter<AppIconAdapter.ViewHolder> {

  private final List<AppIcon> icons = Arrays.asList(AppIcon.values());
  private int checkedPosition;

  public AppIconAdapter(AppIcon current) {
    setHasStableIds(true);
    int index = icons.indexOf(current);
    checkedPosition = index >= 0 ? index : 0;
  }

  public AppIcon getCheckedIcon() {
    return icons.get(checkedPosition);
  }

  @Override
  public long getItemId(int position) {
    return icons.get(position).ordinal();
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view =
        LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app_icon, parent, false);
    return new ViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    AppIcon icon = icons.get(position);
    holder.iconView.setImageResource(icon.previewRes);
    holder.labelView.setText(icon.labelRes);
    holder.checkView.setVisibility(position == checkedPosition ? View.VISIBLE : View.INVISIBLE);
  }

  @Override
  public int getItemCount() {
    return icons.size();
  }

  class ViewHolder extends RecyclerView.ViewHolder {
    ImageView iconView;
    ImageView checkView;
    TextView labelView;

    ViewHolder(View itemView) {
      super(itemView);
      iconView = itemView.findViewById(R.id.icon_view);
      checkView = itemView.findViewById(R.id.icon_check);
      labelView = itemView.findViewById(R.id.icon_label);
      itemView.setOnClickListener(
          v -> {
            int last = checkedPosition;
            checkedPosition = getBindingAdapterPosition();
            notifyItemChanged(last);
            notifyItemChanged(checkedPosition);
          });
    }
  }
}
