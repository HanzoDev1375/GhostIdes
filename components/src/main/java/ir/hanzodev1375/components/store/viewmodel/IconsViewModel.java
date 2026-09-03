package ir.hanzodev1375.components.store.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ir.hanzodev1375.components.R;
import ir.hanzodev1375.components.store.api.IconsApi;
import ir.hanzodev1375.components.store.data.IconsRepository;
import ir.hanzodev1375.components.store.model.IconInfo;

public class IconsViewModel extends AndroidViewModel {

  private final IconsRepository repository = new IconsRepository();
  private final ExecutorService io = Executors.newSingleThreadExecutor();

  private int style = IconsApi.STYLE_OUTLINED;
  private int searchSeq;

  private final MutableLiveData<List<IconInfo>> icons = new MutableLiveData<>();
  private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
  private final MutableLiveData<String> error = new MutableLiveData<>(null);
  private final MutableLiveData<Set<String>> downloading = new MutableLiveData<>(new HashSet<>());
  private final MutableLiveData<Set<String>> downloaded = new MutableLiveData<>(new HashSet<>());
  private final MutableLiveData<String> message = new MutableLiveData<>(null);

  public IconsViewModel(@NonNull Application application) {
    super(application);
  }

  public LiveData<List<IconInfo>> getIcons() {
    return icons;
  }

  public LiveData<Boolean> getIsLoading() {
    return isLoading;
  }

  public LiveData<String> getError() {
    return error;
  }

  public LiveData<Set<String>> getDownloading() {
    return downloading;
  }

  public LiveData<Set<String>> getDownloaded() {
    return downloaded;
  }

  public LiveData<String> getMessage() {
    return message;
  }

  public int getStyle() {
    return style;
  }

  public void setStyle(int newStyle) {
    if (newStyle == style || newStyle < 0 || newStyle >= IconsApi.STYLE_COUNT) return;
    style = newStyle;
    downloaded.setValue(new HashSet<>());
  }

  public void search(String query) {
    final int seq = ++searchSeq;
    isLoading.setValue(true);
    error.setValue(null);
    repository.search(
        getApplication(),
        query,
        new IconsRepository.Callback<List<IconInfo>>() {
          @Override
          public void onSuccess(List<IconInfo> data) {
            if (seq != searchSeq) return;
            isLoading.setValue(false);
            icons.setValue(data);
          }

          @Override
          public void onError(String msg) {
            if (seq != searchSeq) return;
            isLoading.setValue(false);
            error.setValue(msg);
          }
        });
  }

  public void download(IconInfo icon) {
    if (icon == null || icon.name == null) return;
    String key = icon.name + "_s" + style;
    if (downloading.getValue().contains(key)) return;
    addToSet(downloading, key);
    int downloadStyle = style;
    io.execute(
        () -> {
          boolean ok = false;
          try {
            ok = repository.downloadIcon(icon, downloadStyle, repository.getIconsDir());
          } catch (Exception e) {
            ok = false;
          }
          String msg =
              getApplication()
                  .getString(ok ? R.string.icons_download_folder : R.string.icons_download_failed);
          message.postValue(msg);
          if (ok) {
            addToSet(downloaded, key);
          }
          removeFromSet(downloading, key);
        });
  }

  public void clearMessage() {
    message.setValue(null);
  }

  private void addToSet(MutableLiveData<Set<String>> liveData, String key) {
    Set<String> copy = new HashSet<>(liveData.getValue() != null ? liveData.getValue() : new HashSet<>());
    copy.add(key);
    liveData.postValue(copy);
  }

  private void removeFromSet(MutableLiveData<Set<String>> liveData, String key) {
    Set<String> copy = new HashSet<>(liveData.getValue() != null ? liveData.getValue() : new HashSet<>());
    copy.remove(key);
    liveData.postValue(copy);
  }

  @Override
  protected void onCleared() {
    super.onCleared();
    io.shutdownNow();
  }
}
