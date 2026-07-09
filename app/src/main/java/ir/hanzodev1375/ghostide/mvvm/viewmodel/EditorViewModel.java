package ir.hanzodev1375.ghostide.mvvm.viewmodel;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.blankj.utilcode.util.FileIOUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import io.github.rosemoe.sora.text.Content;
import io.github.rosemoe.sora.text.ContentIO;

public class EditorViewModel extends AndroidViewModel {
  private MutableLiveData<Content> text = new MutableLiveData<>();
  private MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
  private MutableLiveData<String> currentPath = new MutableLiveData<>();

  public EditorViewModel(Application app) {
    super(app);
  }

  public LiveData<Content> getText() {
    return text;
  }

  public LiveData<Boolean> getLoading() {
    return loading;
  }

  public LiveData<String> getCurrentPath() {
    return currentPath;
  }

  public void loadFile(String filePath) {
    loading.setValue(true);
    currentPath.setValue(filePath);

    new Thread(
            () -> {
              try {
                File file = new File(filePath);
                if (!file.exists()) {

                  Content emptyContent = new Content();
                  text.postValue(emptyContent);
                  loading.postValue(false);
                  return;
                }

                try (FileInputStream fis = new FileInputStream(file)) {
                  Content content = ContentIO.createFrom(fis);
                  text.postValue(content);
                }
              } catch (Exception e) {
                e.printStackTrace();
                text.postValue(null);
              } finally {
                loading.postValue(false);
              }
            })
        .start();
  }

  public void saveFile(String textContent) {
    String path = currentPath.getValue();
    if (path == null || path.isEmpty()) {
      Log.e("EditorViewModel", "مسیر فایل وجود ندارد");
      return;
    }
    saveFile(path, textContent, null);
  }

  public void saveFile(String filePath, String textContent) {
    saveFile(filePath, textContent, null);
  }

  public void saveFile(String filePath, String textContent, @Nullable Runnable onSaved) {
    if (filePath == null || filePath.isEmpty()) {
      Log.e("EditorViewModel", "مسیر فایل وجود ندارد");
      return;
    }
    currentPath.setValue(filePath);
    Handler mainHandler = new Handler(Looper.getMainLooper());
    new Thread(
            () -> {
              try {
                boolean success = FileIOUtils.writeFileFromString(filePath, textContent, false);
                if (success) {
                  Log.d("EditorViewModel", "فایل ذخیره شد: " + filePath);
                } else {
                  Log.e("EditorViewModel", "خطا در ذخیره فایل: " + filePath);
                }
              } catch (Exception e) {
                e.printStackTrace();
              } finally {
                if (onSaved != null) {
                  mainHandler.post(onSaved);
                }
              }
            })
        .start();
  }
}
