package ir.hanzodev1375.components.sheet.customitemsheet.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import ir.hanzodev1375.components.R;
import ir.hanzodev1375.components.sheet.customitemsheet.callback.OnSheetListener;
import ir.hanzodev1375.components.sheet.customitemsheet.model.SheetModel;
import java.util.List;

public class SheetAdapter extends RecyclerView.Adapter<SheetAdapter.Holder> {
  private List<SheetModel> listSheetModel;
  private OnSheetListener listener;

  public SheetAdapter(List<SheetModel> listSheetModel, OnSheetListener listener) {
    this.listSheetModel = listSheetModel;
    this.listener = listener;
  }

  public void setOnClickListener(OnSheetListener listener) {
    this.listener = listener;
  }

  public static class Holder extends RecyclerView.ViewHolder {
    private TextView name;
    private ImageView icon;

    public Holder(View v) {
      super(v);
      name = v.findViewById(R.id.sheetText);
      icon = v.findViewById(R.id.sheeticon);
    }

    public void bind(SheetModel model) {
      name.setText(model.name());
      if (model.icon() == 0) {
        icon.setVisibility(View.GONE);
      } else {
        icon.setVisibility(View.VISIBLE);
        icon.setImageResource(model.icon());
      }
    }
  }

  @Override
  public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
    return new Holder(
        LayoutInflater.from(parent.getContext())
            .inflate(R.layout.layout_itemsheet_custom, parent, false));
  }

  @Override
  public void onBindViewHolder(Holder holder, int pos) {
    holder.bind(listSheetModel.get(pos));
    holder.itemView.setOnClickListener(
        v -> {
          if (pos != RecyclerView.NO_POSITION) {
            listener.call(listSheetModel.get(pos), pos, v);
          }
        });
  }

  @Override
  public int getItemCount() {
    return listSheetModel.size();
  }
}
