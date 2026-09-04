package ir.hanzodev1375.components.store.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import ir.hanzodev1375.components.store.adapter.FontsAdapter;
import ir.hanzodev1375.components.store.model.FontInfo;
import ir.hanzodev1375.components.store.viewmodel.FontsViewModel;
import ir.theme.M3Theme;

public class FontsFragment extends Fragment {

  private RecyclerView list;
  private ProgressBar progress;
  private TextView errorText;
  private TextView emptyText;
  private TextInputEditText searchInput;
  private FontsAdapter adapter;
  private FontsViewModel viewModel;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_fonts, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    list = view.findViewById(R.id.fontsList);
    progress = view.findViewById(R.id.progressBar);
    errorText = view.findViewById(R.id.errorText);
    emptyText = view.findViewById(R.id.emptyText);
    searchInput = view.findViewById(R.id.searchInput);

    list.setLayoutManager(new LinearLayoutManager(requireContext()));
    adapter = new FontsAdapter(new ArrayList<>(), this::onDownloadClick);
    list.setAdapter(adapter);

    viewModel =
        new ViewModelProvider(
                requireActivity(),
                new ViewModelProvider.AndroidViewModelFactory(requireActivity().getApplication()))
            .get(FontsViewModel.class);

    viewModel.getFonts().observe(getViewLifecycleOwner(), this::onFonts);
    viewModel.getIsLoading().observe(getViewLifecycleOwner(), this::onLoading);
    viewModel.getError().observe(getViewLifecycleOwner(), this::onError);
    viewModel.getDownloading().observe(getViewLifecycleOwner(), adapter::setDownloading);
    viewModel.getDownloaded().observe(getViewLifecycleOwner(), adapter::setDownloaded);
    viewModel.getMessage().observe(getViewLifecycleOwner(), this::onMessage);

    searchInput.addTextChangedListener(
        new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {}

          @Override
          public void afterTextChanged(Editable s) {
            viewModel.search(s == null ? "" : s.toString());
          }
        });

    if (adapter.getItemCount() == 0) {
      viewModel.search("");
    }
    M3Theme.applyTopLevel(view);
  }

  private void onFonts(List<FontInfo> fonts) {
    adapter.updateItems(fonts);
    boolean empty = fonts == null || fonts.isEmpty();
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

  private void onDownloadClick(FontInfo font, int position) {
    viewModel.download(font);
  }
}
