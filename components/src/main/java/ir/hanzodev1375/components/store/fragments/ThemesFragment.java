package ir.hanzodev1375.components.store.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ir.hanzodev1375.components.R;
import ir.hanzodev1375.components.store.adapter.ThemesAdapter;
import ir.hanzodev1375.components.store.model.ThemeItem;
import ir.hanzodev1375.components.store.sheet.ThemePreviewSheet;
import ir.hanzodev1375.components.store.viewmodel.ThemesViewModel;
import ir.theme.M3Theme;

public class ThemesFragment extends Fragment {

  private RecyclerView list;
  private View progress;
  private TextView errorText;
  private ImageView emptyIcon;
  private TextView emptyText;
  private ThemesAdapter adapter;
  private ThemesViewModel viewModel;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_themes, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    list = view.findViewById(R.id.themesList);
    progress = view.findViewById(R.id.progressBar);
    errorText = view.findViewById(R.id.errorText);
    emptyIcon = view.findViewById(R.id.emptyIcon);
    emptyText = view.findViewById(R.id.emptyText);

    list.setLayoutManager(new LinearLayoutManager(requireContext()));
    adapter = new ThemesAdapter(requireContext(), new ArrayList<>(), this::onThemeClick);
    list.setAdapter(adapter);

    viewModel =
        new ViewModelProvider(
                requireActivity(),
                new ViewModelProvider.AndroidViewModelFactory(requireActivity().getApplication()))
            .get(ThemesViewModel.class);

    viewModel.getThemes().observe(getViewLifecycleOwner(), this::onThemes);
    viewModel.getIsLoading().observe(getViewLifecycleOwner(), this::onLoading);
    viewModel.getError().observe(getViewLifecycleOwner(), this::onError);

    if (adapter.getItemCount() == 0) {
      viewModel.load();
    }
    M3Theme.applyTopLevel(view);
  }

  private void onThemes(List<ThemeItem> themes) {
    adapter.updateItems(themes);
    boolean empty = themes == null || themes.isEmpty();
    emptyIcon.setVisibility(empty ? View.VISIBLE : View.GONE);
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

  private void onThemeClick(ThemeItem theme, int position) {
    if (theme == null) return;
    ThemePreviewSheet.newInstance(theme)
        .show(requireActivity().getSupportFragmentManager(), "theme_preview");
  }
}
