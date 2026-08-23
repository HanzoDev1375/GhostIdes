package ir.hanzodev1375.ghostide.mvvm.viewmodel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import ir.hanzodev1375.ghostide.utils.FileUtil;
import ir.hanzodev1375.ghostide.utils.PathManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import android.os.Environment;
import ir.hanzodev1375.ghostide.enums.FileState;
import ir.hanzodev1375.ghostide.ide.ui.api.FileEvent;
import ir.hanzodev1375.ghostide.ide.ui.api.IdeEvents;
import ir.hanzodev1375.ghostide.models.FileManagerModel;
import ir.hanzodev1375.ghostide.codeeditors.setting.PreferencesUtils;

public class FileViewModel extends AndroidViewModel {

  private static final int BUFFER_SIZE = 256 * 1024;

  private MutableLiveData<List<FileManagerModel>> filesLiveData = new MutableLiveData<>();
  private MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
  private MutableLiveData<String> currentPath = new MutableLiveData<>();
  private MutableLiveData<CopyProgress> copyProgress = new MutableLiveData<>();
  private MutableLiveData<DeleteProgress> deleteProgress = new MutableLiveData<>();
  private PathManager pathManager;
  private FileState fileState = FileState.NONE;
  private PreferencesUtils prefs;

  private final ExecutorService ioExecutor =
      Executors.newFixedThreadPool(Math.min(4, Runtime.getRuntime().availableProcessors()));

  public static class CopyProgress {
    public final String fileName;
    public final String fromDir;
    public final String toDir;
    public final int remaining;
    public final long bytesCopied;
    public final long totalBytes;
    public final long speedBps;
    public final boolean isRunning;

    public CopyProgress(
        String fileName,
        String fromDir,
        String toDir,
        int remaining,
        long bytesCopied,
        long totalBytes,
        long speedBps,
        boolean isRunning) {
      this.fileName = fileName;
      this.fromDir = fromDir;
      this.toDir = toDir;
      this.remaining = remaining;
      this.bytesCopied = bytesCopied;
      this.totalBytes = totalBytes;
      this.speedBps = speedBps;
      this.isRunning = isRunning;
    }
  }

  private static class DirStats {
    long totalBytes = 0;
    int totalFiles = 0;
  }

  public static class DeleteProgress {
    public final String fileName;
    public final int deleted;
    public final int total;
    public final boolean isRunning;

    public DeleteProgress(String fileName, int deleted, int total, boolean isRunning) {
      this.fileName = fileName;
      this.deleted = deleted;
      this.total = total;
      this.isRunning = isRunning;
    }
  }

  public FileViewModel(Application app) {
    super(app);
    prefs = new PreferencesUtils(app.getApplicationContext());
    pathManager = new PathManager(app.getApplicationContext());
    String initialPath;
    if (pathManager.isSaveEnabled()) {
      String savedPath = pathManager.getLastPath();
      initialPath =
          FileUtil.isExists(savedPath)
              ? savedPath
              : Environment.getExternalStorageDirectory().getAbsolutePath();
    } else {
      initialPath = Environment.getExternalStorageDirectory().getAbsolutePath();
    }
    currentPath.setValue(initialPath);
    loadFiles(initialPath);
  }

  public LiveData<List<FileManagerModel>> getFiles() {
    return filesLiveData;
  }

  public LiveData<Boolean> getIsLoading() {
    return isLoading;
  }

  public LiveData<String> getCurrentPath() {
    return currentPath;
  }

  public LiveData<CopyProgress> getCopyProgress() {
    return copyProgress;
  }

  public LiveData<DeleteProgress> getDeleteProgress() {
    return deleteProgress;
  }

  public void savePath(boolean save) {
    pathManager.savePath(save);
    if (save) pathManager.setLastPath(currentPath.getValue());
    else pathManager.clearLastPath();
  }

  public void navigateTo(String path) {
    File file = new File(path);
    if (file.exists() && file.isDirectory()) {
      currentPath.setValue(path);
      loadFiles(path);
      if (pathManager.isSaveEnabled()) pathManager.setLastPath(path);
    }
  }

  public void navigateUp() {
    String parent = new File(currentPath.getValue()).getParent();
    if (parent != null) navigateTo(parent);
  }

  public void loadFiles(String dirPath) {
    isLoading.postValue(true);
    ioExecutor.execute(
        () -> {
          List<FileManagerModel> list = new ArrayList<>();
          File dir = new File(dirPath);
          File[] files = dir.listFiles();
          if (files != null) {
            Arrays.sort(
                files,
                Comparator.comparing(File::isDirectory)
                    .reversed()
                    .thenComparing(File::getName, String.CASE_INSENSITIVE_ORDER));
            for (File file : files) {
              String name = file.getName();
              if (prefs.isShowHiddenFiles() || !name.startsWith(".")) {
                list.add(
                    new FileManagerModel(
                        file.getAbsolutePath(), name, fileState, file.lastModified()));
              }
            }
          }
          filesLiveData.postValue(list);
          isLoading.postValue(false);
        });
  }

  public void renameFile(FileManagerModel model, String newName) {
    File oldFile = new File(model.getPath());
    File newFile = new File(oldFile.getParent(), newName);
    if (oldFile.renameTo(newFile)) {
      model.setState(FileState.RENAME);
      IdeEvents.post(FileEvent.renamed(oldFile.getAbsolutePath(), newFile.getAbsolutePath()));
      loadFiles(currentPath.getValue());
    }
  }

  public void deleteFile(FileManagerModel model) {
    File file = new File(model.getPath());
    if (file.delete()) {
      IdeEvents.post(FileEvent.deleted(file.getAbsolutePath()));
      loadFiles(currentPath.getValue());
    }
  }

  public void deleteFiles(List<FileManagerModel> items) {
    ioExecutor.execute(
        () -> {
          DirStats stats = new DirStats();
          for (FileManagerModel item : items) collectStats(new File(item.getPath()), stats);
          int total = stats.totalFiles;
          int[] deleted = {0};

          List<Future<?>> futures = new ArrayList<>();
          for (FileManagerModel item : items) {
            futures.add(
                ioExecutor.submit(
                    () -> deleteRecursiveWithProgress(new File(item.getPath()), deleted, total)));
          }
          for (Future<?> f : futures) {
            try {
              f.get();
            } catch (Exception ignored) {
            }
          }

          deleteProgress.postValue(new DeleteProgress("", 0, 0, false));
          for (FileManagerModel item : items) {
            IdeEvents.post(FileEvent.deleted(item.getPath()));
          }
          loadFiles(currentPath.getValue());
        });
  }

  private void deleteRecursiveWithProgress(File file, int[] deleted, int total) {
    if (file.isDirectory()) {
      File[] children = file.listFiles();
      if (children != null) {
        for (File child : children) deleteRecursiveWithProgress(child, deleted, total);
      }
      file.delete();
    } else {
      file.delete();
      deleted[0]++;
      deleteProgress.postValue(new DeleteProgress(file.getName(), deleted[0], total, true));
    }
  }

  private void deleteRecursive(File file) {
    if (file.isDirectory()) {
      File[] children = file.listFiles();
      if (children != null) for (File child : children) deleteRecursive(child);
    }
    file.delete();
  }

  public void pasteFiles(
      List<FileManagerModel> sources, String destDir, boolean isCut, OnPasteComplete callback) {
    ioExecutor.execute(
        () -> {
          boolean success = true;
          File destFolder = new File(destDir);
          if (!destFolder.exists()) destFolder.mkdirs();

          DirStats stats = new DirStats();
          for (FileManagerModel model : sources) collectStats(new File(model.getPath()), stats);

          long[] bytesCopied = {0};
          int[] remaining = {stats.totalFiles};
          long[] lastTime = {System.currentTimeMillis()};
          long[] lastBytes = {0};

          String fromDir = new File(sources.get(0).getPath()).getParent();
          String fromName = (fromDir != null) ? new File(fromDir).getName() : "Unknown";
          String toName = new File(destDir).getName();

          for (FileManagerModel model : sources) {
            File src = new File(model.getPath());
            File dest = new File(destFolder, src.getName());

            if (isCut) {

              if (!src.renameTo(dest)) {

                copyRecursiveFast(
                    src,
                    dest,
                    bytesCopied,
                    remaining,
                    lastTime,
                    lastBytes,
                    stats.totalBytes,
                    fromName,
                    toName);
                deleteRecursive(src);
              } else {

                remaining[0]--;
                copyProgress.postValue(
                    new CopyProgress(
                        src.getName(),
                        fromName,
                        toName,
                        remaining[0],
                        stats.totalBytes,
                        stats.totalBytes,
                        0,
                        remaining[0] > 0));
              }
            } else {

              copyRecursiveFast(
                  src,
                  dest,
                  bytesCopied,
                  remaining,
                  lastTime,
                  lastBytes,
                  stats.totalBytes,
                  fromName,
                  toName);
            }
          }

          copyProgress.postValue(new CopyProgress("", "", "", 0, 0, 0, 0, false));
          loadFiles(currentPath.getValue());
          if (callback != null) {
            new Handler(Looper.getMainLooper()).post(() -> callback.onComplete(success));
          }
        });
  }

  private void collectStats(File f, DirStats stats) {
    if (f.isFile()) {
      stats.totalBytes += f.length();
      stats.totalFiles++;
    } else {
      File[] children = f.listFiles();
      if (children != null) for (File c : children) collectStats(c, stats);
    }
  }

  private void copyRecursiveFast(
      File src,
      File dest,
      long[] bytesCopied,
      int[] remaining,
      long[] lastTime,
      long[] lastBytes,
      long totalBytes,
      String fromDir,
      String toDir) {
    try {
      if (src.isDirectory()) {
        if (!dest.exists()) dest.mkdirs();
        File[] children = src.listFiles();
        if (children != null) {
          for (File child : children) {
            copyRecursiveFast(
                child,
                new File(dest, child.getName()),
                bytesCopied,
                remaining,
                lastTime,
                lastBytes,
                totalBytes,
                fromDir,
                toDir);
          }
        }
      } else {

        try (FileChannel inCh = new FileInputStream(src).getChannel();
            FileChannel outCh = new FileOutputStream(dest).getChannel()) {

          long fileSize = inCh.size();
          long transferred = 0;
          long chunkSize = BUFFER_SIZE;

          while (transferred < fileSize) {
            long count = Math.min(chunkSize, fileSize - transferred);
            long n = inCh.transferTo(transferred, count, outCh);
            if (n <= 0) break;
            transferred += n;
            bytesCopied[0] += n;

            long now = System.currentTimeMillis();
            long elapsed = now - lastTime[0];
            if (elapsed >= 300) {
              long speed = (bytesCopied[0] - lastBytes[0]) * 1000 / Math.max(elapsed, 1);
              lastTime[0] = now;
              lastBytes[0] = bytesCopied[0];
              copyProgress.postValue(
                  new CopyProgress(
                      src.getName(),
                      fromDir,
                      toDir,
                      remaining[0],
                      bytesCopied[0],
                      totalBytes,
                      speed,
                      true));
            }
          }
        }
        remaining[0]--;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void createFolder(String folderName) {
    String currentDir = currentPath.getValue();
    if (currentDir == null) return;
    File newFolder = new File(currentDir, folderName);
    if (!newFolder.exists()) {
      newFolder.mkdirs();
      loadFiles(currentDir);
    }
  }

  public void createFile(String fileName) {
    String currentDir = currentPath.getValue();
    if (currentDir == null) return;
    File newFile = new File(currentDir, fileName);
    if (!newFile.exists()) {
      try {
        newFile.createNewFile();
        loadFiles(currentDir);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  @Override
  protected void onCleared() {
    super.onCleared();
    ioExecutor.shutdown();
  }

  public interface OnPasteComplete {
    void onComplete(boolean success);
  }
}
