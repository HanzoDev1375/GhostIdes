package ir.hanzodev1375.components.store.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ir.hanzodev1375.components.store.api.WebStoreApi;
import ir.hanzodev1375.components.store.model.WebStore;

public class WebStoreViewModel extends AndroidViewModel {

  private final List<WebStore> allStores = new ArrayList<>();
  private String query = "";

  private final MutableLiveData<List<WebStore>> stores = new MutableLiveData<>(new ArrayList<>());
  private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
  private final MutableLiveData<String> error = new MutableLiveData<>(null);

  public WebStoreViewModel(@NonNull Application application) {
    super(application);
  }

  public LiveData<List<WebStore>> getStores() {
    return stores;
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
    WebStoreApi.fetchWebStores(
        new WebStoreApi.Callbacks() {
          @Override
          public void onSuccess(List<WebStore> data) {
            isLoading.setValue(false);
            allStores.clear();
            if (data != null) {
              allStores.addAll(data);
            }
            applyFilter();
          }

          @Override
          public void onError(String message) {
            isLoading.setValue(false);
            error.setValue(message);
          }
        });
  }

  public void search(String q) {
    query = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
    applyFilter();
  }

  private void applyFilter() {
    List<WebStore> filtered = new ArrayList<>();
    for (WebStore store : allStores) {
      if (query.isEmpty()
          || (store.getName() != null
              && store.getName().toLowerCase(Locale.ROOT).contains(query))) {
        filtered.add(store);
      }
    }
    stores.setValue(filtered);
  }
}