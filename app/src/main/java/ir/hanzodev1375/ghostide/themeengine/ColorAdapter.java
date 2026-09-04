package ir.hanzodev1375.ghostide.themeengine;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import ir.hanzodev1375.ghostide.R;
import ir.theme.M3Theme;

public class ColorAdapter extends RecyclerView.Adapter<ColorAdapter.ViewHolder> {

  private final List<Integer> colorArray;
  private int checkedPosition = -1;

  public ColorAdapter(List<Integer> colorArray) {
    this.colorArray = colorArray;
    setHasStableIds(true);
  }

  public int getCheckedPosition() {
    return checkedPosition;
  }

  public void setCheckedPosition(Theme theme) {
    int lastChecked = checkedPosition;
    checkedPosition = theme.ordinal();
    notifyItemChanged(lastChecked);
    notifyItemChanged(checkedPosition);
  }

  @Override
  public long getItemId(int position) {
    return colorArray.get(position);
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view =
        LayoutInflater.from(parent.getContext()).inflate(R.layout.item_color, parent, false);
    return new ViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    int color = colorArray.get(position);
    holder.colorView.setBackgroundColorRes(color);
    if (checkedPosition == position) {
      holder.colorView.setImageResource(R.drawable.ic_round_check);
    } else {
      holder.colorView.setImageResource(0);
    }
    M3Theme.listCard(holder.itemView);
  }

  @Override
  public int getItemCount() {
    return colorArray.size();
  }

  class ViewHolder extends RecyclerView.ViewHolder {
    CircleImageView colorView;

    ViewHolder(View itemView) {
      super(itemView);
      colorView = itemView.findViewById(R.id.color_view);
      colorView.setOnClickListener(
          v -> {
            int lastChecked = checkedPosition;
            checkedPosition = getBindingAdapterPosition();
            colorView.setImageResource(R.drawable.ic_round_check);
            notifyItemChanged(lastChecked);
          });
    }
  }
}
