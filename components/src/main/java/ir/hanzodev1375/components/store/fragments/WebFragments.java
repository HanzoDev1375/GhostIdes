package ir.hanzodev1375.components.store.fragments;

import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ir.hanzodev1375.components.R;
import ir.hanzodev1375.components.SearchLayout;
import ir.hanzodev1375.components.store.adapter.WebStoreAdapter;
import ir.hanzodev1375.components.store.model.WebStore;
import ir.hanzodev1375.components.store.sheet.WebProjectPreviewSheet;
import ir.hanzodev1375.components.store.viewmodel.WebStoreViewModel;
import ir.theme.M3Theme;

public class WebFragments extends Fragment implements WebStoreAdapter.OnClickItemListener {

  private RecyclerView rv;
  private ProgressBar progressBar;
  private TextView errorText;
  private SearchLayout searchLayout;
  private WebStoreAdapter adapter;
  private WebStoreViewModel viewModel;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_web, container, false);
    rv = view.findViewById(R.id.recyclerView);
    progressBar = view.findViewById(R.id.progressBar);
    errorText = view.findViewById(R.id.errorText);
    searchLayout = view.findViewById(R.id.searchLayout);

    searchLayout.setIconClose(R.drawable.ic_close_24);
    searchLayout.setIconSearch(R.drawable.outline_search24);
    searchLayout.show();

    return view;
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    GridLayoutManager manager = new GridLayoutManager(requireContext(), 1);
    rv.setLayoutManager(manager);
    rv.addItemDecoration(
        new RecyclerView.ItemDecoration() {
          @Override
          public void getItemOffsets(
              Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            outRect.top = 8;
            outRect.bottom = 8;
            int recyclerWidth = parent.getWidth();
            int itemWidth =
                (int) (300 * parent.getContext().getResources().getDisplayMetrics().density);
            int margin = Math.max(0, (recyclerWidth - itemWidth) / 2);
            outRect.left = margin;
            outRect.right = margin;
          }
        });

    adapter = new WebStoreAdapter(new ArrayList<>(), this);
    rv.setAdapter(adapter);

    viewModel =
        new ViewModelProvider(
                this,
                new ViewModelProvider.AndroidViewModelFactory(requireActivity().getApplication()))
            .get(WebStoreViewModel.class);

    viewModel.getStores().observe(getViewLifecycleOwner(), this::onStores);
    viewModel.getIsLoading().observe(getViewLifecycleOwner(), this::onLoading);
    viewModel.getError().observe(getViewLifecycleOwner(), this::onError);

    searchLayout.setOnTextChangedListener(
        text -> viewModel.search(text == null ? "" : text));

    viewModel.load();
    M3Theme.applyTopLevel(view);
  }

  private void onStores(List<WebStore> stores) {
    adapter.updateData(stores);
  }

  private void onLoading(Boolean loading) {
    progressBar.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
    if (Boolean.FALSE.equals(loading)) {
      errorText.setVisibility(View.GONE);
    }
  }

  private void onError(String message) {
    errorText.setText(message);
    errorText.setVisibility(message == null || message.isEmpty() ? View.GONE : View.VISIBLE);
  }

  @Override
  public void click(View v, int pos, WebStore model) {
    if (model == null) return;
    WebProjectPreviewSheet.newInstance(model)
        .show(requireActivity().getSupportFragmentManager(), "web_project_preview");
  }
}