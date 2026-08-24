package ir.hanzodev1375.ghostide.utils.zip;

import android.os.Handler;
import android.os.Looper;
import ir.hanzodev1375.ghostide.models.ZipInfo;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ZipOperationManager {

  private final ExecutorService executor = Executors.newSingleThreadExecutor();
  private final Handler mainHandler = new Handler(Looper.getMainLooper());

  public interface Callback {
    void onSuccess(String message);

    void onError(String error);
  }

  public interface ProgressCallback extends Callback {
    void onProgress(int percent, String fileName);
  }

  public interface ZipInfoCallback {
    void onInfo(ZipInfo info);

    void onError(String error);
  }

  public void extractSingle(String zipPath, String entryPath, String destDir, Callback cb) {
    executor.execute(
        () -> {
          try {
            ZipFile zip = new ZipFile(zipPath);
            File dest = new File(destDir);
            if (!dest.exists()) dest.mkdirs();
            zip.extractFile(entryPath, destDir);
            post(cb, true, " extracted");
          } catch (ZipException e) {
            post(cb, false, e.getMessage());
          }
        });
  }

  public void extractMultiple(
      String zipPath, List<String> entryPaths, String destDir, ProgressCallback cb) {
    executor.execute(
        () -> {
          try {
            ZipFile zip = new ZipFile(zipPath);
            File dest = new File(destDir);
            if (!dest.exists()) dest.mkdirs();
            int total = entryPaths.size();
            for (int i = 0; i < total; i++) {
              String entry = entryPaths.get(i);
              zip.extractFile(entry, destDir);
              int percent = (int) (((i + 1) / (float) total) * 100);
              String name =
                  entry.contains("/") ? entry.substring(entry.lastIndexOf('/') + 1) : entry;
              final int p = percent;
              final String n = name;
              mainHandler.post(() -> cb.onProgress(p, n));
            }
            post(cb, true, total + " File extracted");
          } catch (ZipException e) {
            post(cb, false, e.getMessage());
          }
        });
  }

  public void extractAll(String zipPath, String destDir, ProgressCallback cb) {
    executor.execute(
        () -> {
          try {
            ZipFile zip = new ZipFile(zipPath);
            List<FileHeader> headers = zip.getFileHeaders();
            File dest = new File(destDir);
            if (!dest.exists()) dest.mkdirs();
            int total = headers.size();
            for (int i = 0; i < total; i++) {
              zip.extractFile(headers.get(i), destDir);
              int percent = (int) (((i + 1) / (float) total) * 100);
              final int p = percent;
              final String n = headers.get(i).getFileName();
              mainHandler.post(() -> cb.onProgress(p, n));
            }
            post(cb, true, "All files extracted");
          } catch (ZipException e) {
            post(cb, false, e.getMessage());
          }
        });
  }

  public void renameEntry(String zipPath, String oldEntryPath, String newName, Callback cb) {
    executor.execute(() -> {
        try {
            ZipFile zip = new ZipFile(zipPath);
            String onlyName = newName.contains("/")
                ? newName.substring(newName.lastIndexOf('/') + 1)
                : newName;
            boolean isDirectory = oldEntryPath.endsWith("/") || oldEntryPath.endsWith("\\");
            int slash = Math.max(oldEntryPath.lastIndexOf('/'), oldEntryPath.lastIndexOf('\\'));
            String parentPrefix = slash >= 0 ? oldEntryPath.substring(0, slash + 1) : "";
            String newEntryPath = parentPrefix + onlyName + (isDirectory ? "/" : "");
            zip.renameFile(oldEntryPath, newEntryPath);
            post(cb, true, "Name is Changed");
        } catch (ZipException e) {
            post(cb, false, e.getMessage());
        }
    });
}

  public void deleteEntries(String zipPath, List<String> entryPaths, Callback cb) {
    executor.execute(
        () -> {
          try {
            ZipFile zip = new ZipFile(zipPath);
            zip.removeFiles(entryPaths);
            post(cb, true, entryPaths.size() + "Removed item done");
          } catch (ZipException e) {
            post(cb, false, e.getMessage());
          }
        });
  }

  public void deleteEntries(String zipPath, String entryPaths, Callback cb) {
    executor.execute(
        () -> {
          try {
            ZipFile zip = new ZipFile(zipPath);
            List<String> ls = new ArrayList<>();
            ls.add(entryPaths);
            for (var it : ls) {
              zip.removeFile(zip.getFileHeader(it));
              post(cb, true, it);
            }
          } catch (ZipException e) {
            post(cb, false, e.getMessage());
          }
        });
  }

  public void addFile(String zipPath, String filePath, String targetFolder, Callback cb) {
    executor.execute(
        () -> {
          try {
            ZipFile zip = new ZipFile(zipPath);
            ZipParameters params = new ZipParameters();
            params.setCompressionMethod(CompressionMethod.DEFLATE);
            params.setCompressionLevel(CompressionLevel.NORMAL);
            if (targetFolder != null && !targetFolder.isEmpty()) {
              params.setFileNameInZip(targetFolder + "/" + new File(filePath).getName());
            }
            zip.addFile(new File(filePath), params);
            post(cb, true, "add to File Done");
          } catch (ZipException e) {
            post(cb, false, e.getMessage());
          }
        });
  }

  public void getZipInfo(String zipPath, ZipInfoCallback cb) {
    executor.execute(
        () -> {
          try {
            ZipFile zip = new ZipFile(zipPath);
            List<FileHeader> headers = zip.getFileHeaders();
            long totalUncompressed = 0;
            long totalCompressed = 0;
            int fileCount = 0;
            int dirCount = 0;
            for (FileHeader h : headers) {
              if (h.isDirectory()) dirCount++;
              else {
                fileCount++;
                totalUncompressed += h.getUncompressedSize();
                totalCompressed += h.getCompressedSize();
              }
            }
            int ratio =
                totalUncompressed > 0
                    ? (int) (100 - (totalCompressed * 100.0 / totalUncompressed))
                    : 0;
            final ZipInfo info =
                new ZipInfo(
                    fileCount,
                    dirCount,
                    totalUncompressed,
                    totalCompressed,
                    ratio,
                    zip.isEncrypted());
            mainHandler.post(() -> cb.onInfo(info));
          } catch (ZipException e) {
            mainHandler.post(() -> cb.onError(e.getMessage()));
          }
        });
  }

  private void post(Callback cb, boolean success, String msg) {
    mainHandler.post(
        () -> {
          if (success) cb.onSuccess(msg);
          else cb.onError(msg);
        });
  }

  public void shutdown() {
    executor.shutdownNow();
  }
}
