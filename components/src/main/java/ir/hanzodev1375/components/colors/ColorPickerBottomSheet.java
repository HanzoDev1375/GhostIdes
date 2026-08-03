package ir.hanzodev1375.components.colors;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.blankj.utilcode.util.ThreadUtils;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import ir.hanzodev1375.components.colors.adapter.ColorAdapter;
import ir.hanzodev1375.components.colors.model.ColorItem;
import ir.hanzodev1375.components.sheet.BaseBlurBottomSheet;
import ir.hanzodev1375.components.R;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ColorPickerBottomSheet extends BaseBlurBottomSheet {

  private RecyclerView recyclerView;
  private TextInputEditText searchEditText;
  private View emptyState;
  private ColorAdapter adapter;

  @Override
  protected void onContentReady(ViewGroup contentContainer) {
    View v =
        LayoutInflater.from(requireContext())
            .inflate(R.layout.bottom_sheet_color_picker, contentContainer, false);
    contentContainer.addView(
        v,
        new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    recyclerView = contentContainer.findViewById(R.id.recyclerColors);
    searchEditText = contentContainer.findViewById(R.id.editSearch);
    emptyState = contentContainer.findViewById(R.id.emptyState);

    adapter = new ColorAdapter(requireContext());
    recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
    recyclerView.setAdapter(adapter);

    loadColors();

    searchEditText.addTextChangedListener(
        new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {
            adapter.filter(s.toString());
            updateEmptyState();
          }

          @Override
          public void afterTextChanged(Editable s) {}
        });
  }

  private void loadColors() {
    Context appContext = requireContext().getApplicationContext();
    ThreadUtils.executeByIo(
        new ThreadUtils.SimpleTask<List<ColorItem>>() {
          @Override
          public List<ColorItem> doInBackground() throws Exception {
            try (InputStream is = appContext.getAssets().open("colors/color.json");
                InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
              Type type = new TypeToken<List<ColorItem>>() {}.getType();
              return new Gson().fromJson(reader, type);
            }
          }

          @Override
          public void onSuccess(List<ColorItem> result) {
            if (binding == null) return;
            adapter.submitList(result);
            updateEmptyState();
          }

          @Override
          public void onFail(Throwable t) {
            t.printStackTrace();
          }
        });
  }

  private void updateEmptyState() {
    if (emptyState == null || adapter == null) return;
    emptyState.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
  }
}
