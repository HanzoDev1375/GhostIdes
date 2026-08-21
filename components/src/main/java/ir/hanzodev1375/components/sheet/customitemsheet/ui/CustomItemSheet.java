package ir.hanzodev1375.components.sheet.customitemsheet.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import ir.hanzodev1375.components.R;
import ir.hanzodev1375.components.sheet.BaseSheet;
import ir.hanzodev1375.components.sheet.customitemsheet.callback.OnSheetListener;
import ir.hanzodev1375.components.sheet.customitemsheet.model.SheetModel;
import java.util.ArrayList;
import java.util.List;

public class CustomItemSheet extends BaseSheet {
  private SheetAdapter adapter;
  private List<SheetModel> listSheetModel = new ArrayList<>();
  private OnSheetListener listener;

  public CustomItemSheet(Context c) {
    super(c);
    init();
  }

  public CustomItemSheet(Context c, int style) {
    super(c, style);
    init();
  }

  void init() {
    listSheetModel.clear();
    adapter = new SheetAdapter(listSheetModel, listener);
    View v = LayoutInflater.from(getContext()).inflate(R.layout.layout_customsheet_rv, null, false);
    RecyclerView rv = v.findViewById(R.id.rv);
    rv.setLayoutManager(new LinearLayoutManager(getContext()));
    rv.setAdapter(adapter);
    setContentView(v);
  }

  public List<SheetModel> getListSheetModel() {
    return this.listSheetModel;
  }

  public CustomItemSheet setListSheetModel(List<SheetModel> listSheetModel) {
    this.listSheetModel = listSheetModel;
    return this;
  }

  public OnSheetListener getOnClickListener() {
    return this.listener;
  }

  public CustomItemSheet setOnClickListener(OnSheetListener listener) {
    this.listener = listener;
    adapter.setOnClickListener(listener);
    return this;
  }

  public CustomItemSheet add(String name, int icon) {
    listSheetModel.add(new SheetModel(name, icon));
    adapter.notifyDataSetChanged();
    return this;
  }

  public CustomItemSheet add(String name) {
    listSheetModel.add(new SheetModel(name, 0));
    adapter.notifyDataSetChanged();
    return this;
  }
}
