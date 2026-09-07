package ir.hanzodev1375.components.store.data;

import android.content.Context;

import java.util.List;

import ir.hanzodev1375.components.store.api.ThemesApi;
import ir.hanzodev1375.components.store.model.ThemeItem;

public class ThemesRepository {

  public void fetch(Context context, Callback<List<ThemeItem>> callback) {
    ThemesApi.fetchThemes(
        context,
        new ThemesApi.Callbacks() {
          @Override
          public void onSuccess(List<ThemeItem> themes) {
            if (callback != null) callback.onSuccess(themes);
          }

          @Override
          public void onError(String message) {
            if (callback != null) callback.onError(message);
          }
        });
  }

  public interface Callback<T> {
    void onSuccess(T data);

    void onError(String message);
  }
}
