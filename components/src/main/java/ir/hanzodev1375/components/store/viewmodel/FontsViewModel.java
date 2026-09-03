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

import ir.hanzodev1375.components.store.data.FontsRepository;
import ir.hanzodev1375.components.store.model.FontInfo;

public class FontsViewModel extends AndroidViewModel {

  private final FontsRepository repository = new FontsRepository();
  private final ExecutorService io = Executors.newSingleThreadExecutor();

  private final MutableLiveData<List<FontInfo>> fonts = new MutableLiveData<>();
  private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
  private final MutableLiveData<String> error = new MutableLiveData<>(null);
  private final MutableLiveData<Set<String>> downloading = new MutableLiveData<>(new HashSet<>());
  private final MutableLiveData<Set<String>> downloaded = new MutableLiveData<>(new HashSet<>());
  private final MutableLiveData<String> message = new MutableLiveData<>(null);

  public FontsViewModel(@NonNull Application application) {
    super(application);
  }

  public LiveData<List<FontInfo>> getFonts() {
    return fonts;
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

  public void search(String query) {
    if (Boolean.TRUE.equals(isLoading.getValue())) {
      return;
    }
    isLoading.setValue(true);
    error.setValue(null);
    repository.search(
        getApplication(),
        query,
        new FontsRepository.Callback<List<FontInfo>>() {
          @Override
          public void onSuccess(List<FontInfo> data) {
            isLoading.setValue(false);
            fonts.setValue(data);
          }

          @Override
          public void onError(String msg) {
            isLoading.setValue(false);
            error.setValue(msg);
          }
        });
  }

  public void download(FontInfo font) {
    if (font == null || font.family == null) return;
    if (downloading.getValue().contains(font.family)) return;
    addToSet(downloading, font.family);
    io.execute(
        () -> {
          try {
            boolean ok = repository.downloadFont(font, repository.getFontsDir());
            if (ok) {
              addToSet(downloaded, font.family);
              message.postValue(getApplication().getString(ir.hanzodev1375.components.R.string.fonts_download_folder));
            } else {
              message.postValue(
                  getApplication().getString(ir.hanzodev1375.components.R.string.fonts_download_failed));
            }
          } catch (Exception e) {
            message.postValue(
                getApplication().getString(ir.hanzodev1375.components.R.string.fonts_download_failed));
          } finally {
            removeFromSet(downloading, font.family);
          }
        });
  }

  public void clearMessage() {
    message.setValue(null);
  }

  private void addToSet(MutableLiveData<Set<String>> liveData, String family) {
    Set<String> copy = new HashSet<>(liveData.getValue() != null ? liveData.getValue() : new HashSet<>());
    copy.add(family);
    liveData.postValue(copy);
  }

  private void removeFromSet(MutableLiveData<Set<String>> liveData, String family) {
    Set<String> copy = new HashSet<>(liveData.getValue() != null ? liveData.getValue() : new HashSet<>());
    copy.remove(family);
    liveData.postValue(copy);
  }

  @Override
  protected void onCleared() {
    super.onCleared();
    io.shutdownNow();
  }
}
