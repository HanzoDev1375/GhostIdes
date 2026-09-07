package ir.hanzodev1375.components.store.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;

import ir.hanzodev1375.components.store.data.ThemesRepository;
import ir.hanzodev1375.components.store.model.ThemeItem;

public class ThemesViewModel extends AndroidViewModel {

  private final ThemesRepository repository = new ThemesRepository();

  private final MutableLiveData<List<ThemeItem>> themes = new MutableLiveData<>(new ArrayList<>());
  private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
  private final MutableLiveData<String> error = new MutableLiveData<>(null);

  public ThemesViewModel(@NonNull Application application) {
    super(application);
  }

  public LiveData<List<ThemeItem>> getThemes() {
    return themes;
  }

  public LiveData<Boolean> getIsLoading() {
    return isLoading;
  }

  public LiveData<String> getError() {
    return error;
  }

  public void load() {
    if (Boolean.TRUE.equals(isLoading.getValue())) {
      return;
    }
    isLoading.setValue(true);
    error.setValue(null);
    repository.fetch(
        getApplication(),
        new ThemesRepository.Callback<List<ThemeItem>>() {
          @Override
          public void onSuccess(List<ThemeItem> data) {
            isLoading.setValue(false);
            themes.setValue(data == null ? new ArrayList<>() : data);
          }

          @Override
          public void onError(String msg) {
            isLoading.setValue(false);
            error.setValue(msg);
          }
        });
  }
}
