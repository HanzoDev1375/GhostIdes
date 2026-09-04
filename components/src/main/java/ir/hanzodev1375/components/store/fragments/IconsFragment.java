package ir.hanzodev1375.components.store.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

import ir.hanzodev1375.components.R;
import ir.hanzodev1375.components.store.adapter.IconsAdapter;
import ir.hanzodev1375.components.store.api.IconsApi;
import ir.hanzodev1375.components.store.model.IconInfo;
import ir.hanzodev1375.components.store.viewmodel.IconsViewModel;
import ir.theme.M3Theme;

public class IconsFragment extends Fragment {

  private static final long SEARCH_DELAY = 300;
  private final Handler searchHandler = new Handler(Looper.getMainLooper());
  private Runnable searchRunnable;

  private RecyclerView list;
  private ProgressBar progress;
  private TextView errorText;
  private TextView emptyText;
  private AutoCompleteTextView styleSpinner;
  private TextInputEditText searchInput;
  private IconsAdapter adapter;
  private IconsViewModel viewModel;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_icons, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    list = view.findViewById(R.id.iconsList);
    progress = view.findViewById(R.id.progressBar);
    errorText = view.findViewById(R.id.errorText);
    emptyText = view.findViewById(R.id.emptyText);
    styleSpinner = view.findViewById(R.id.styleSpinner);
    searchInput = view.findViewById(R.id.searchInput);

    list.setLayoutManager(new LinearLayoutManager(requireContext()));
    adapter = new IconsAdapter(new ArrayList<>(), this::onDownloadClick);
    list.setAdapter(adapter);

    viewModel =
        new ViewModelProvider(
                requireActivity(),
                new ViewModelProvider.AndroidViewModelFactory(requireActivity().getApplication()))
            .get(IconsViewModel.class);

    viewModel.getIcons().observe(getViewLifecycleOwner(), this::onIcons);
    viewModel.getIsLoading().observe(getViewLifecycleOwner(), this::onLoading);
    viewModel.getError().observe(getViewLifecycleOwner(), this::onError);
    viewModel.getDownloading().observe(getViewLifecycleOwner(), adapter::setDownloading);
    viewModel.getDownloaded().observe(getViewLifecycleOwner(), adapter::setDownloaded);
    viewModel.getMessage().observe(getViewLifecycleOwner(), this::onMessage);

    setupStyleSpinner();

    searchInput.addTextChangedListener(
        new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {}

          @Override
          public void afterTextChanged(Editable s) {
            if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
            searchRunnable =
                () -> viewModel.search(s == null ? "" : s.toString());
            searchHandler.postDelayed(searchRunnable, SEARCH_DELAY);
          }
        });

    if (adapter.getItemCount() == 0) {
      viewModel.search("");
    }
    M3Theme.applyTopLevel(view);
  }

  private void setupStyleSpinner() {
    String[] labels = new String[IconsApi.STYLE_COUNT];
    labels[IconsApi.STYLE_FILLED] = getString(R.string.icons_style_filled);
    labels[IconsApi.STYLE_OUTLINED] = getString(R.string.icons_style_outlined);
    labels[IconsApi.STYLE_ROUND] = getString(R.string.icons_style_round);
    labels[IconsApi.STYLE_SHARP] = getString(R.string.icons_style_sharp);
    labels[IconsApi.STYLE_TWO_TONE] = getString(R.string.icons_style_two_tone);

    ArrayAdapter<String> spinnerAdapter =
        new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, labels);
    styleSpinner.setAdapter(spinnerAdapter);
    styleSpinner.setText(labels[IconsApi.STYLE_OUTLINED], false);
    adapter.setStyle(IconsApi.STYLE_OUTLINED);
    styleSpinner.setOnItemClickListener(
        (parent, v, position, id) -> {
          viewModel.setStyle(position);
          adapter.setStyle(position);
          viewModel.search(searchInput.getText() == null ? "" : searchInput.getText().toString());
        });
  }

  private void onIcons(List<IconInfo> icons) {
    adapter.updateItems(icons);
    boolean empty = icons == null || icons.isEmpty();
    emptyText.setVisibility(empty ? View.VISIBLE : View.GONE);
    list.setVisibility(empty ? View.GONE : View.VISIBLE);
  }

  private void onLoading(Boolean loading) {
    boolean show = Boolean.TRUE.equals(loading);
    progress.setVisibility(show ? View.VISIBLE : View.GONE);
    if (!show) {
      errorText.setVisibility(View.GONE);
    }
  }

  private void onError(String message) {
    errorText.setText(message);
    errorText.setVisibility(message == null || message.isEmpty() ? View.GONE : View.VISIBLE);
  }

  private void onMessage(String message) {
    if (message == null || message.isEmpty()) return;
    Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show();
  }

  private void onDownloadClick(IconInfo icon, int position) {
    viewModel.download(icon);
  }
}
