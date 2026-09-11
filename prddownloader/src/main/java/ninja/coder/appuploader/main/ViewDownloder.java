package ninja.coder.appuploader.main;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.animation.ValueAnimator;
import android.animation.ArgbEvaluator;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import com.downloader.Error;
import com.downloader.OnDownloadListener;
import com.downloader.OnPauseListener;
import com.downloader.OnProgressListener;
import com.downloader.OnStartOrResumeListener;
import com.downloader.PRDownloader;
import com.downloader.Progress;
import com.downloader.Status;
import com.downloader.databinding.LayoutDownloderChildBinding;
import androidx.core.content.FileProvider;
import java.io.File;
import java.util.HashMap;
import java.util.Locale;

public class ViewDownloder extends RelativeLayout {

  private HashMap<String, Object> gsonItem;
  protected LayoutDownloderChildBinding child;
  private OnClick onclick;
  private int id;
  private String fileName;

  public ViewDownloder(Context c) {
    super(c);
  }

  public ViewDownloder(Context c, AttributeSet set) {
    super(c, set);
    init();
  }

  void init() {

    removeAllViews();
    child = LayoutDownloderChildBinding.inflate(LayoutInflater.from(getContext()));
    var param = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    int[] colors = {0xFF81C784, 0xFF5DB895, 0xFF53B2AA};
    var gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.TL_BR, colors);
    child.getRoot().setBackground(gradientDrawable);
    ValueAnimator colorAnimation = ValueAnimator.ofArgb(colors[0], colors[1]);
    colorAnimation.setDuration(2000);
    colorAnimation.setRepeatCount(ValueAnimator.INFINITE);
    colorAnimation.setRepeatMode(ValueAnimator.REVERSE);
    colorAnimation.setEvaluator(new ArgbEvaluator());
    colorAnimation.addUpdateListener(
        animator -> {
          int animatedValue = (int) animator.getAnimatedValue();
          gradientDrawable.setColors(new int[] {animatedValue, colors[1]});
        });
    colorAnimation.start();
    child.progrssdownload.setVisibility(View.GONE);
    addView(child.getRoot(), param);
    child.tvname.setTextColor(Color.BLACK);
    child.progrssdownload.setWaveAmplitude(1);
    child.progrssdownload.setWavelength(20);
    child.progrssdownload.setWaveSpeed(19);
  }

  public void setTitle(CharSequence title) {
    child.tvname.setText(title);
  }

  public void setSizeTitle(CharSequence text) {
    child.tvsize.setText(text);
  }

  public void setDownload(String url, String name) {

    this.fileName = name;
    child.iconfake.setVisibility(View.GONE);
    child.progrssdownload.setVisibility(View.VISIBLE);
    child.iconfake.setImageResource(android.R.drawable.arrow_down_float);
    if (Status.RUNNING == PRDownloader.getStatus(id)) {
      PRDownloader.pause(id);

      return;
    }
    if (Status.PAUSED == PRDownloader.getStatus(id)) {
      PRDownloader.resume(id);
      return;
    }
    child.getRoot().setEnabled(false);
    child.progrssdownload.setIndeterminate(true);

    File file = new File("/storage/emulated/0/ghostide/apk/" + name);
    if (file.exists()) {
      child.view.setVisibility(View.GONE);
      child.installApk.setVisibility(View.VISIBLE);
      child.installApk.setOnClickListener(
          it -> {
            installApk();
          });
    } else {
      id =
          PRDownloader.download(url, "/storage/emulated/0/ghostide/apk/", name)
              .build()
              .setOnStartOrResumeListener(
                  new OnStartOrResumeListener() {

                    @Override
                    public void onStartOrResume() {
                      child.progrssdownload.setIndeterminate(false);
                      child.getRoot().setEnabled(true);
                    }
                  })
              .setOnPauseListener(
                  new OnPauseListener() {

                    @Override
                    public void onPause() {
                      child.progrssdownload.setIndeterminate(true);
                    }
                  })
              .setOnProgressListener(
                  new OnProgressListener() {
                    @Override
                    public void onProgress(Progress progress) {
                      child.progrssdownload.setVisibility(View.VISIBLE);
                      child.iconfake.setVisibility(View.GONE);
                      long progressPercent = progress.currentBytes * 100 / progress.totalBytes;
                      child.progrssdownload.setProgressCompat((int) progressPercent, false);
                      child.tvname.setText(
                          getProgressDisplayLine(progress.currentBytes, progress.totalBytes));
                      child.progrssdownload.setIndeterminate(false);
                    }
                  })
              .start(
                  new OnDownloadListener() {

                    @Override
                    public void onDownloadComplete() {
                      child.tvname.setText("endWork");
                      child.view.setVisibility(View.GONE);
                      child.installApk.setVisibility(View.VISIBLE);
                      new Handler(Looper.getMainLooper())
                          .postDelayed(
                              () -> {
                                if (child.installApk.isShown() || child.installApk.isEnabled()) {
                                  installApk();
                                }
                              },
                              600);
                    }

                    @Override
                    public void onError(Error error) {}
                  });
    }
  }

  public void setOnClick(OnClick onclick) {
    this.onclick = onclick;
    child.getRoot().setOnClickListener(c -> onclick.onClick(c));
  }

  public interface OnClick {
    void onClick(View v);
  }

  public interface OnInstallApkListener {
    void onInstallApk(File apkFile, String fileName);
  }

  private OnInstallApkListener installApkListener;

  public void setOnInstallApkListener(OnInstallApkListener listener) {
    this.installApkListener = listener;
  }

  public String getProgressDisplayLine(long currentBytes, long totalBytes) {
    return getBytesToMBString(currentBytes) + "/" + getBytesToMBString(totalBytes);
  }

  private String getBytesToMBString(long bytes) {
    return String.format(Locale.ENGLISH, "%.2fMb", bytes / (1024.00 * 1024.00));
  }

  @SuppressWarnings("deprecation")
  void installApk() {
    File apkFile = new File("/storage/emulated/0/ghostide/apk/" + fileName);
    if (installApkListener != null) {
      installApkListener.onInstallApk(apkFile, fileName);
    } else if (getContext() instanceof Activity) {
      new ApkInstallerCompat((Activity) getContext(), apkFile).install();
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
        && !getContext().getPackageManager().canRequestPackageInstalls()) {
      var intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
      intent.setData(Uri.parse("package:" + getContext().getPackageName()));
      getContext().startActivity(intent);
    } else {
      try {
        Uri apkUri =
            FileProvider.getUriForFile(
                getContext(), getContext().getPackageName() + ".fileprovider", apkFile);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        getContext().startActivity(intent);
      } catch (Exception ignored) {
      }
    }
  }
}
