package ir.hanzodev1375.components.colors.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.blankj.utilcode.util.ClipboardUtils;
import com.blankj.utilcode.util.ToastUtils;
import ir.hanzodev1375.components.colors.model.ColorItem;
import ir.hanzodev1375.components.R;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ColorAdapter extends RecyclerView.Adapter<ColorAdapter.ColorViewHolder> {

  private Context context;
  private List<ColorItem> fullList = new ArrayList<>();
  private List<ColorItem> filteredList = new ArrayList<>();

  public ColorAdapter(Context context) {
    this.context = context;
  }

  public void submitList(List<ColorItem> items) {
    fullList.clear();
    if (items != null) fullList.addAll(items);
    filteredList.clear();
    filteredList.addAll(fullList);
    notifyDataSetChanged();
  }

  public void filter(String query) {
    filteredList.clear();
    if (query == null || query.trim().isEmpty()) {
      filteredList.addAll(fullList);
    } else {
      String lower = query.trim().toLowerCase(Locale.ROOT).replace("#", "");
      for (ColorItem item : fullList) {
        boolean nameMatch =
            item.getName() != null && item.getName().toLowerCase(Locale.ROOT).contains(lower);
        boolean hexMatch =
            item.getHex() != null
                && item.getHex().toLowerCase(Locale.ROOT).replace("#", "").contains(lower);
        if (nameMatch || hexMatch) {
          filteredList.add(item);
        }
      }
    }
    notifyDataSetChanged();
  }

  @NonNull
  @Override
  public ColorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(context).inflate(R.layout.item_color_card, parent, false);
    return new ColorViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull ColorViewHolder holder, int position) {
    holder.bind(filteredList.get(position));
  }

  @Override
  public int getItemCount() {
    return filteredList.size();
  }

  class ColorViewHolder extends RecyclerView.ViewHolder {

    private View colorPreview;
    private TextView nameText;
    private TextView hexText;
    private View copyButton;

    ColorViewHolder(@NonNull View itemView) {
      super(itemView);
      colorPreview = itemView.findViewById(R.id.viewColorPreview);
      nameText = itemView.findViewById(R.id.textColorName);
      hexText = itemView.findViewById(R.id.textColorHex);
      copyButton = itemView.findViewById(R.id.btnCopy);
    }

    void bind(ColorItem item) {
      int color;
      try {
        color = Color.parseColor(item.getHex());
      } catch (Exception e) {
        color = Color.GRAY;
      }
      colorPreview.setBackgroundColor(color);
      nameText.setText(item.getName());
      hexText.setText(item.getHex());

      View.OnClickListener copyAction =
          v -> {
            ClipboardUtils.copyText(item.getHex());
            ToastUtils.showShort("Copy it: " + item.getHex());
          };
      copyButton.setOnClickListener(copyAction);
      itemView.setOnClickListener(copyAction);
    }
  }
}
