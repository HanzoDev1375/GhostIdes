package ir.theme.themeeditor;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.gson.Gson;
import ir.hanzodev1375.ghostide.R;
import ir.theme.GhostTheme;
import ir.theme.internal.ThemeRefResolver;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Lets the user pick a relative {@code @block.key} reference for a color row. Entries are shown
 * with their resolved color swatch so choosing feels visual, not textual.
 */
public class ThemeReferenceBottomSheet extends BottomSheetDialogFragment {

  public interface OnRefPicked {
    void onRefPicked(String reference);
  }

  public static class Entry implements Serializable {
    public String block;
    public String key;
    public String title;

    public Entry(String block, String key, String title) {
      this.block = block;
      this.key = key;
      this.title = title;
    }
  }

  private static final String ARG_ENTRIES = "entries";
  private static final String ARG_THEME_JSON = "theme_json";

  private List<Entry> entries = new ArrayList<>();
  private GhostTheme theme;
  private OnRefPicked listener;
  private RefAdapter adapter;

  public static ThemeReferenceBottomSheet newInstance(List<Entry> entries, GhostTheme theme) {
    ThemeReferenceBottomSheet sheet = new ThemeReferenceBottomSheet();
    Bundle args = new Bundle();
    args.putSerializable(ARG_ENTRIES, new ArrayList<>(entries));
    args.putString(ARG_THEME_JSON, theme != null ? new Gson().toJson(theme) : null);
    sheet.setArguments(args);
    return sheet;
  }

  public void setOnRefPicked(@Nullable OnRefPicked listener) {
    this.listener = listener;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getArguments() != null) {
      Object raw = getArguments().getSerializable(ARG_ENTRIES);
      if (raw instanceof List) entries = (List<Entry>) raw;
      String json = getArguments().getString(ARG_THEME_JSON);
      if (json != null) {
        try {
          theme = new Gson().fromJson(json, GhostTheme.class);
        } catch (Exception ignored) {
        }
      }
    }
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.bottom_sheet_theme_refs, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    RecyclerView list = view.findViewById(R.id.refList);
    list.setLayoutManager(new LinearLayoutManager(requireContext()));
    adapter = new RefAdapter();
    list.setAdapter(adapter);
  }

  private class RefAdapter extends RecyclerView.Adapter<RefAdapter.Holder> {

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
      View v =
          LayoutInflater.from(parent.getContext()).inflate(R.layout.item_theme_ref_row, parent, false);
      return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
      Entry e = entries.get(position);
      holder.title.setText(e.title != null && !e.title.isEmpty() ? e.title : e.key);
      holder.subtitle.setText("@" + e.block + "." + e.key);
      String resolved = ThemeRefResolver.resolve(theme, e.block, e.key);
      int color;
      try {
        color = Color.parseColor(resolved);
      } catch (Exception ex) {
        color = Color.BLACK;
      }
      GradientDrawable gd = new GradientDrawable();
      gd.setColor(color);
      gd.setStroke(1, Color.WHITE);
      gd.setCornerRadius(16f);
      holder.swatch.setBackground(gd);
      holder.itemView.setOnClickListener(
          v -> {
            if (listener != null) listener.onRefPicked("@" + e.block + "." + e.key);
            dismiss();
          });
    }

    @Override
    public int getItemCount() {
      return entries.size();
    }

    class Holder extends RecyclerView.ViewHolder {
      TextView title;
      TextView subtitle;
      View swatch;

      Holder(@NonNull View itemView) {
        super(itemView);
        title = itemView.findViewById(R.id.title);
        subtitle = itemView.findViewById(R.id.subtitle);
        swatch = itemView.findViewById(R.id.swatch);
      }
    }
  }
}