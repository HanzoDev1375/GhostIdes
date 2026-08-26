package ir.hanzodev1375.ghostide.utils;

import android.app.Activity;
import android.os.Environment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import ir.hanzodev1375.ghostide.R;
import androidx.appcompat.app.AlertDialog;
import com.downloader.PRDownloader;
import com.downloader.OnDownloadListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.util.Locale;

public final class WebDownloadHelper {

  private static final String DEFAULT_SUBDIR = "Download/GhostIDE";

  private WebDownloadHelper() {}

  public static void handleDownload(
      Activity activity,
      String url,
      String contentDisposition,
      String mimetype,
      long contentLength) {

    if (activity == null || activity.isFinishing() || TextUtils.isEmpty(url)) return;

    if (url.startsWith("blob:") || url.startsWith("data:")) {
      Toast.makeText(activity, "This link type can't be downloaded", Toast.LENGTH_SHORT).show();
      return;
    }

    if (!PermissionUtils.hasManageStoragePermission(activity)) {
      Toast.makeText(activity, "Grant storage access, then try again", Toast.LENGTH_LONG).show();
      PermissionUtils.requestManageStoragePermission(activity);
      return;
    }

    showConfirmDialog(activity, url, contentDisposition, mimetype, contentLength);
  }

  private static void showConfirmDialog(
      Activity activity,
      String url,
      String contentDisposition,
      String mimetype,
      long contentLength) {

    File dir = targetDir();
    if (!dir.exists() && !dir.mkdirs()) {
      Toast.makeText(activity, "Can't create download folder", Toast.LENGTH_SHORT).show();
      return;
    }

    String suggestedName = uniqueTarget(dir, resolveFileName(url, contentDisposition, mimetype));

    View view = LayoutInflater.from(activity).inflate(R.layout.dialog_download_confirm, null);

    TextView tvFrom = view.findViewById(R.id.tvFrom);
    tvFrom.setText("From: " + shorten(hostOf(url), 46));

    TextView tvTo = view.findViewById(R.id.tvTo);
    tvTo.setText("To: " + dir.getAbsolutePath());

    TextInputEditText etName = view.findViewById(R.id.etName);
    etName.setText(suggestedName);

    TextView tvSize = view.findViewById(R.id.tvSize);
    if (contentLength > 0) {
      tvSize.setText("Size: " + formatBytes(contentLength));
      tvSize.setVisibility(View.VISIBLE);
    }

    new MaterialAlertDialogBuilder(activity)
        .setTitle("Download")
        .setView(view)
        .setPositiveButton(
            "Download",
            (d, w) -> {
              String name = etName.getText().toString().trim();
              if (name.isEmpty()) name = suggestedName;
              start(activity, url, dir, uniqueTarget(dir, name));
            })
        .setNegativeButton("Cancel", null)
        .show();
  }

  private static void start(final Activity activity, String url, final File dir, String fileName) {

    View view = LayoutInflater.from(activity).inflate(R.layout.dialog_download_progress, null);

    LinearProgressIndicator progressBar = view.findViewById(R.id.progressBar);
    final TextView tvProgress = view.findViewById(R.id.tvProgress);

    final AlertDialog progressDialog =
        new MaterialAlertDialogBuilder(activity)
            .setTitle(fileName)
            .setView(view)
            .setCancelable(false)
            .setNegativeButton(
                "Cancel",
                (d, w) -> {
                  Object tag = progressBar.getTag();
                  if (tag instanceof Integer) PRDownloader.cancel((Integer) tag);
                })
            .create();
    progressDialog.show();

    final long[] lastTime = {System.currentTimeMillis()};
    final long[] lastBytes = {0};
    final boolean[] cancelled = {false};

    int reqId =
        PRDownloader.download(url, dir.getAbsolutePath(), fileName)
            .build()
            .setOnProgressListener(
                progress -> {
                  if (activity.isFinishing()) return;
                  long now = System.currentTimeMillis();
                  float speed = 0;
                  if (now - lastTime[0] > 400) {
                    long dt = now - lastTime[0];
                    if (dt > 0 && progress.currentBytes >= lastBytes[0]) {
                      speed = (progress.currentBytes - lastBytes[0]) / (dt / 1000f);
                    }
                    lastTime[0] = now;
                    lastBytes[0] = progress.currentBytes;
                  }
                  String speedTxt =
                      speed > 0 ? String.format(Locale.US, "%.1f MB/s", speed / 1048576f) : "–";
                  if (progress.totalBytes <= 0) {
                    progressBar.setIndeterminate(true);
                    tvProgress.setText(formatBytes(progress.currentBytes) + "  ·  " + speedTxt);
                  } else {
                    int percent = (int) (progress.currentBytes * 100 / progress.totalBytes);
                    progressBar.setIndeterminate(false);
                    progressBar.setProgress(percent);
                    tvProgress.setText(
                        percent
                            + "%  ·  "
                            + formatBytes(progress.currentBytes)
                            + " / "
                            + formatBytes(progress.totalBytes)
                            + "  ·  "
                            + speedTxt);
                  }
                })
            .start(
                new OnDownloadListener() {
                  @Override
                  public void onDownloadComplete() {
                    if (!activity.isFinishing() && progressDialog.isShowing())
                      progressDialog.dismiss();
                    Toast.makeText(
                            activity,
                            "Saved: " + new File(dir, fileName).getAbsolutePath(),
                            Toast.LENGTH_LONG)
                        .show();
                  }

                  @Override
                  public void onError(com.downloader.Error error) {
                    if (!activity.isFinishing() && progressDialog.isShowing())
                      progressDialog.dismiss();
                    if (cancelled[0]) return;
                    String detail =
                        error != null && error.getServerErrorMessage() != null
                            ? error.getServerErrorMessage()
                            : "unknown";
                    Toast.makeText(activity, "Download failed: " + detail, Toast.LENGTH_LONG)
                        .show();
                  }
                });

    progressBar.setTag(reqId);
  }

  private static File targetDir() {
    return new File(Environment.getExternalStorageDirectory(), DEFAULT_SUBDIR);
  }

  private static String resolveFileName(String url, String contentDisposition, String mimetype) {
    if (!TextUtils.isEmpty(contentDisposition)) {
      java.util.regex.Matcher m =
          java.util.regex.Pattern.compile(
                  "filename\\*?=(?:UTF-8'')?\"?([^\";]+)", java.util.regex.Pattern.CASE_INSENSITIVE)
              .matcher(contentDisposition);
      if (m.find() && !TextUtils.isEmpty(m.group(1))) {
        return sanitize(m.group(1));
      }
    }
    String clean = url.split("\\?")[0];
    int slash = clean.lastIndexOf('/');
    if (slash >= 0 && slash < clean.length() - 1) {
      String seg = sanitize(clean.substring(slash + 1));
      if (!seg.isEmpty() && seg.contains(".")) return seg;
    }
    String base = "download_" + System.currentTimeMillis();
    String ext = extFromMime(mimetype);
    return ext.isEmpty() ? base : base + ext;
  }

  private static String sanitize(String s) {
    return s.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
  }

  private static String uniqueTarget(File dir, String name) {
    File f = new File(dir, name);
    if (!f.exists()) return name;
    String plain = name;
    String ext = "";
    int dot = name.lastIndexOf('.');
    if (dot > 0) {
      plain = name.substring(0, dot);
      ext = name.substring(dot);
    }
    for (int i = 1; i < 999; i++) {
      String candidate = plain + "(" + i + ")" + ext;
      if (!new File(dir, candidate).exists()) return candidate;
    }
    return "download_" + System.currentTimeMillis() + ext;
  }

  private static String extFromMime(String mime) {
    if (mime == null) return "";
    switch (mime.toLowerCase(Locale.US)) {
      case "application/vnd.android.package-archive":
        return ".apk";
      case "application/zip":
        return ".zip";
      case "application/x-rar-compressed":
        return ".rar";
      case "application/gzip":
        return ".gz";
      case "application/x-7z-compressed":
        return ".7z";
      case "application/pdf":
        return ".pdf";
      case "text/html":
        return ".html";
      case "text/plain":
        return ".txt";
      case "text/css":
        return ".css";
      case "text/csv":
        return ".csv";
      case "application/json":
        return ".json";
      case "image/png":
        return ".png";
      case "image/jpeg":
        return ".jpg";
      case "image/gif":
        return ".gif";
      case "image/webp":
        return ".webp";
      case "video/mp4":
        return ".mp4";
      case "audio/mpeg":
        return ".mp3";
      case "audio/ogg":
        return ".ogg";
      default:
        return "";
    }
  }

  private static String hostOf(String url) {
    try {
      return android.net.Uri.parse(url).getHost() != null
          ? android.net.Uri.parse(url).getHost()
          : url;
    } catch (Exception e) {
      return url;
    }
  }

  private static String shorten(String s, int max) {
    if (s == null) return "";
    return s.length() <= max ? s : s.substring(0, max - 3) + "...";
  }

  private static String formatBytes(long b) {
    if (b < 1024) return b + " B";
    if (b < 1048576) return String.format(Locale.US, "%.1f KB", b / 1024f);
    if (b < 1073741824L) return String.format(Locale.US, "%.1f MB", b / 1048576f);
    return String.format(Locale.US, "%.2f GB", b / 1073741824f);
  }
}
