package ir.hanzodev1375.components.store.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ir.hanzodev1375.components.R;
import ir.hanzodev1375.components.store.data.PluginRepository;
import ir.hanzodev1375.components.store.event.PluginInstallEvent;
import ir.hanzodev1375.components.store.event.PluginInstalledListEvent;
import ir.hanzodev1375.components.store.event.PluginResultEvent;
import ir.hanzodev1375.components.store.event.PluginSetupRequestEvent;
import ir.hanzodev1375.components.store.model.PluginItem;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class PluginStoreViewModel extends AndroidViewModel {

  private final PluginRepository repository = new PluginRepository();
  private final ExecutorService io = Executors.newSingleThreadExecutor();

  private final List<PluginItem> allPlugins = new ArrayList<>();
  private String query = "";

  private final MutableLiveData<List<PluginItem>> plugins =
      new MutableLiveData<>(new ArrayList<>());
  private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
  private final MutableLiveData<String> error = new MutableLiveData<>(null);
  private final MutableLiveData<String> message = new MutableLiveData<>(null);
  private final MutableLiveData<Set<String>> busy = new MutableLiveData<>(new HashSet<>());
  private final MutableLiveData<Set<String>> installed = new MutableLiveData<>(new HashSet<>());

  public PluginStoreViewModel(@NonNull Application application) {
    super(application);
    if (!EventBus.getDefault().isRegistered(this)) {
      EventBus.getDefault().register(this);
    }
  }

  @Override
  protected void onCleared() {
    EventBus.getDefault().unregister(this);
    io.shutdownNow();
    super.onCleared();
  }

  public LiveData<List<PluginItem>> getPlugins() {
    return plugins;
  }

  public LiveData<Boolean> getIsLoading() {
    return isLoading;
  }

  public LiveData<String> getError() {
    return error;
  }

  public LiveData<String> getMessage() {
    return message;
  }

  public LiveData<Set<String>> getBusy() {
    return busy;
  }

  public LiveData<Set<String>> getInstalled() {
    return installed;
  }

  public void loadPlugins() {
    if (Boolean.TRUE.equals(isLoading.getValue())) {
      return;
    }
    isLoading.setValue(true);
    error.setValue(null);
    repository.fetchPlugins(
        new PluginRepository.Callback<List<PluginItem>>() {
          @Override
          public void onSuccess(List<PluginItem> data) {
            isLoading.setValue(false);
            allPlugins.clear();
            if (data != null) {
              allPlugins.addAll(data);
            }
            applyFilter();
          }

          @Override
          public void onError(String msg) {
            isLoading.setValue(false);
            error.setValue(msg);
          }
        });
  }

  public void search(String q) {
    query = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
    applyFilter();
  }

  private void applyFilter() {
    List<PluginItem> filtered = new ArrayList<>();
    for (PluginItem item : allPlugins) {
      if (query.isEmpty()
          || (item.name() != null
              && item.name().toLowerCase(Locale.ROOT).contains(query))) {
        filtered.add(item);
      }
    }
    plugins.setValue(filtered);
  }

  /** Ask the host to install the plugin; optionally download its source first. */
  public void install(PluginItem item, boolean withSource) {
    if (item == null || isBusy(item)) {
      return;
    }
    markBusy(item, true);
    if (withSource) {
      io.execute(
          () ->
              repository.downloadSource(
                  item,
                  new PluginRepository.Callback<File>() {
                    @Override
                    public void onSuccess(File dir) {
                      postInstall(item);
                      message.setValue(
                          getString(R.string.pluginstore_source_saved, dir.getAbsolutePath()));
                    }

                    @Override
                    public void onError(String msg) {
                      message.setValue(getString(R.string.pluginstore_source_error, msg));
                      postInstall(item);
                    }
                  }));
    } else {
      postInstall(item);
    }
  }

  private void postInstall(PluginItem item) {
    EventBus.getDefault().post(new PluginInstallEvent(item));
  }

  public void requestSetup(PluginItem item) {
    if (item == null) return;
    EventBus.getDefault().post(new PluginSetupRequestEvent(item));
  }

  @Subscribe(threadMode = ThreadMode.MAIN)
  public void onInstallResult(PluginResultEvent event) {
    if (event.plugin == null) {
      return;
    }
    markBusy(event.plugin, false);
    if (event.success) {
      addInstalled(event.message);
      message.setValue(getString(R.string.pluginstore_installed, event.message));
    } else {
      message.setValue(getString(R.string.pluginstore_install_error, event.message));
    }
  }

  @Subscribe(threadMode = ThreadMode.MAIN)
  public void onInstalledList(PluginInstalledListEvent event) {
    if (event.installedIds == null) {
      return;
    }
    Set<String> copy =
        new HashSet<>(installed.getValue() != null ? installed.getValue() : new HashSet<>());
    copy.addAll(event.installedIds);
    installed.setValue(copy);
  }

  public void clearMessage() {
    message.setValue(null);
  }

  private boolean isBusy(PluginItem item) {
    return busy.getValue() != null && busy.getValue().contains(item.name());
  }

  private void markBusy(PluginItem item, boolean value) {
    Set<String> copy = new HashSet<>(busy.getValue() != null ? busy.getValue() : new HashSet<>());
    if (value) {
      copy.add(item.name());
    } else {
      copy.remove(item.name());
    }
    busy.postValue(copy);
  }

  private void addInstalled(String name) {
    Set<String> copy =
        new HashSet<>(installed.getValue() != null ? installed.getValue() : new HashSet<>());
    copy.add(name);
    installed.postValue(copy);
  }

  private String getString(int res, Object... args) {
    return getApplication().getString(res, args);
  }
}
