package ir.hanzodev1375.components.store.fragments;

import android.os.Bundle;
import android.text.Html;
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

import java.util.List;

import ir.hanzodev1375.components.R;
import ir.hanzodev1375.components.sheet.customitemsheet.ui.LiquidGlassDialogBuilderJava;
import ir.hanzodev1375.components.store.adapter.PluginStoreAdapter;
import ir.hanzodev1375.components.store.api.PluginStoreApi;
import ir.hanzodev1375.components.store.event.PluginStoreEvent;
import ir.hanzodev1375.components.store.model.PluginDoc;
import ir.hanzodev1375.components.store.model.PluginItem;
import ir.hanzodev1375.components.store.viewmodel.PluginStoreViewModel;
import ir.theme.M3Theme;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class PluginStoreFragment extends Fragment {

  private RecyclerView list;
  private ProgressBar progress;
  private TextView errorText;
  private TextView emptyText;
  private PluginStoreAdapter adapter;
  private PluginStoreViewModel viewModel;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_plugin_store, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    list = view.findViewById(R.id.pluginList);
    progress = view.findViewById(R.id.progressBar);
    errorText = view.findViewById(R.id.errorText);
    emptyText = view.findViewById(R.id.emptyText);

    list.setLayoutManager(new LinearLayoutManager(requireContext()));
    adapter = new PluginStoreAdapter();
    list.setAdapter(adapter);

    viewModel =
        new ViewModelProvider(
                requireActivity(),
                new ViewModelProvider.AndroidViewModelFactory(requireActivity().getApplication()))
            .get(PluginStoreViewModel.class);

    viewModel.getPlugins().observe(getViewLifecycleOwner(), this::onPlugins);
    viewModel.getIsLoading().observe(getViewLifecycleOwner(), this::onLoading);
    viewModel.getError().observe(getViewLifecycleOwner(), this::onError);
    viewModel.getBusy().observe(getViewLifecycleOwner(), adapter::setBusy);
    viewModel.getInstalled().observe(getViewLifecycleOwner(), adapter::setInstalled);
    viewModel.getMessage().observe(getViewLifecycleOwner(), this::onMessage);

    viewModel.loadPlugins();
    M3Theme.applyTopLevel(view);
  }

  @Override
  public void onStart() {
    super.onStart();
    EventBus.getDefault().register(this);
  }

  @Override
  public void onStop() {
    EventBus.getDefault().unregister(this);
    super.onStop();
  }

  @Subscribe(threadMode = ThreadMode.MAIN)
  public void onPluginStoreEvent(PluginStoreEvent event) {
    if (event.plugin == null) {
      return;
    }
    fetchDocAndShowDialog(event.plugin);
  }

  private void fetchDocAndShowDialog(PluginItem item) {
    PluginStoreApi.fetchDoc(
        item.doc(),
        new PluginStoreApi.DocCallback() {
          @Override
          public void onSuccess(PluginDoc doc) {
            showInstallDialog(item, doc);
          }

          @Override
          public void onError(String message) {
            showInstallDialog(item, new PluginDoc("", "", ""));
          }
        });
  }

  private void showInstallDialog(PluginItem item, PluginDoc doc) {
    String note =
        doc.note() == null || doc.note().trim().isEmpty()
            ? requireContext().getString(R.string.pluginstore_no_description)
            : doc.note();

    new LiquidGlassDialogBuilderJava(requireContext())
        .setTitle(item.name())
        .setMessage(Html.fromHtml(note, Html.FROM_HTML_MODE_COMPACT))
        .setPositiveButton(
            R.string.pluginstore_install_and_source,
            (dialog, which) -> viewModel.install(item, true))
        .setNegativeButton(
            R.string.pluginstore_install_only,
            (dialog, which) -> viewModel.install(item, false))
        .setNeutralButton(android.R.string.cancel, null)
        .show();
  }

  private void onPlugins(List<PluginItem> plugins) {
    adapter.updateItems(plugins);
    boolean empty = plugins == null || plugins.isEmpty();
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
    errorText.setVisibility(
        message == null || message.trim().isEmpty() ? View.GONE : View.VISIBLE);
  }

  private void onMessage(String message) {
    if (message == null || message.trim().isEmpty()) {
      return;
    }
    Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show();
  }
}