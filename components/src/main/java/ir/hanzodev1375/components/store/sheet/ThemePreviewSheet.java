package ir.hanzodev1375.components.store.sheet;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.viewpager2.widget.ViewPager2;
import com.blankj.utilcode.util.FileIOUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.noties.markwon.Markwon;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import ir.hanzodev1375.components.R;
import ir.hanzodev1375.components.sheet.BaseBlurBottomSheet;
import ir.hanzodev1375.components.store.adapter.ThemeImageAdapter;
import ir.hanzodev1375.components.store.api.ThemesApi;
import ir.hanzodev1375.components.store.model.ThemeItem;
import ir.hanzodev1375.components.store.event.ThemeInstalledEvent;
import ir.theme.M3Theme;
import ir.theme.ThemeManager;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.greenrobot.eventbus.EventBus;

public class ThemePreviewSheet extends BaseBlurBottomSheet {

  private static final String ARG_NAME = "arg_name";
  private static final String ARG_IMAGE1 = "arg_image1";
  private static final String ARG_IMAGE2 = "arg_image2";
  private static final String ARG_IMAGE3 = "arg_image3";
  private static final String ARG_DEV = "arg_dev";
  private static final String ARG_VERSION = "arg_version";
  private static final String ARG_DOWNLOAD = "arg_download";
  private static final String ARG_DOC = "arg_doc";

  private static final OkHttpClient client =
      new OkHttpClient.Builder()
          .connectTimeout(15, TimeUnit.SECONDS)
          .readTimeout(60, TimeUnit.SECONDS)
          .build();

  private ThemeImageAdapter imageAdapter;
  private int currentPage;

  public static ThemePreviewSheet newInstance(ThemeItem theme) {
    ThemePreviewSheet sheet = new ThemePreviewSheet();
    Bundle args = new Bundle();
    args.putString(ARG_NAME, theme.name());
    args.putString(ARG_IMAGE1, theme.image1());
    args.putString(ARG_IMAGE2, theme.image2());
    args.putString(ARG_IMAGE3, theme.image3());
    args.putString(ARG_DEV, theme.devname());
    args.putInt(ARG_VERSION, theme.version());
    args.putString(ARG_DOWNLOAD, theme.linkdownload());
    args.putString(ARG_DOC, theme.doc());
    sheet.setArguments(args);
    return sheet;
  }

  @Override
  protected void onContentReady(ViewGroup contentContainer) {
    Bundle args = getArguments();
    if (args == null) return;

    Context context = requireContext();
    View v =
        LayoutInflater.from(context).inflate(R.layout.sheet_theme_preview, contentContainer, false);
    contentContainer.addView(v);

    TextView title = v.findViewById(R.id.previewTitle);
    TextView subtitle = v.findViewById(R.id.previewSubtitle);
    TextView descriptionLabel = v.findViewById(R.id.previewDescriptionLabel);
    TextView description = v.findViewById(R.id.previewDescription);
    ViewPager2 pager = v.findViewById(R.id.previewPager);
    View prev = v.findViewById(R.id.previewPrev);
    View next = v.findViewById(R.id.previewNext);
    FloatingActionButton install = v.findViewById(R.id.previewInstall);
    LinearLayout progressContainer = v.findViewById(R.id.previewProgressContainer);
    ProgressBar progress = v.findViewById(R.id.previewProgress);
    TextView progressText = v.findViewById(R.id.previewProgressText);

    ImageView prevIcon = v.findViewById(R.id.previewPrevIcon);
    ImageView nextIcon = v.findViewById(R.id.previewNextIcon);
    Integer navColor = M3Theme.onSurface();
    if (navColor != null) {
      prevIcon.setImageTintList(ColorStateList.valueOf(navColor));
      nextIcon.setImageTintList(ColorStateList.valueOf(navColor));
    } else {
      Integer primary = M3Theme.primary();
      if (primary != null) {
        prevIcon.setImageTintList(ColorStateList.valueOf(primary));
        nextIcon.setImageTintList(ColorStateList.valueOf(primary));
      }
    }

    String name = args.getString(ARG_NAME, "");
    String dev = args.getString(ARG_DEV, "");
    int version = args.getInt(ARG_VERSION, 0);
    title.setText(name);
    subtitle.setText(
        context.getString(R.string.themes_dev_version, dev != null ? dev : "", version));

    descriptionLabel.setText(context.getString(R.string.themes_description));
    loadDescription(context, args.getString(ARG_DOC), description);

    imageAdapter = new ThemeImageAdapter();
    pager.setAdapter(imageAdapter);
    pager.setOffscreenPageLimit(1);
    pager.setPageTransformer(new DepthPageTransformer());
    pager.registerOnPageChangeCallback(
        new ViewPager2.OnPageChangeCallback() {
          @Override
          public void onPageSelected(int position) {
            currentPage = position;
            updateNav();
          }
        });

    List<String> images = new ArrayList<>(3);
    addImage(images, args.getString(ARG_IMAGE1));
    addImage(images, args.getString(ARG_IMAGE2));
    addImage(images, args.getString(ARG_IMAGE3));
    imageAdapter.setUrls(images.toArray(new String[0]));
    updateNav();

    prev.setOnClickListener(inner -> pager.setCurrentItem(pager.getCurrentItem() - 1, true));
    next.setOnClickListener(inner -> pager.setCurrentItem(pager.getCurrentItem() + 1, true));
    install.setOnClickListener(
        inner ->
            startDownload(
                v,
                args.getString(ARG_DOWNLOAD),
                install,
                progressContainer,
                progress,
                progressText));

    M3Theme.text(title, subtitle, descriptionLabel, progressText);
    M3Theme.fab(install);
    M3Theme.applyShallow(v);
  }

  private void loadDescription(Context context, String url, TextView description) {
    if (url == null || url.isEmpty()) {
      description.setText(context.getString(R.string.themes_description_failed));
      return;
    }
    Markwon markwon = Markwon.create(context);
    ThemesApi.fetchText(
        context,
        url,
        new ThemesApi.TextCallbacks() {
          @Override
          public void onSuccess(String content) {
            if (binding == null || content == null || content.isEmpty()) {
              description.setText(context.getString(R.string.themes_description_failed));
            } else {
              markwon.setMarkdown(description, content);
            }
          }

          @Override
          public void onError(String message) {
            description.setText(
                message != null ? message : context.getString(R.string.themes_description_failed));
          }
        });
  }

  private void addImage(List<String> out, String path) {
    if (path == null || path.isEmpty()) return;
    out.add(path.startsWith("http") ? path : ThemesApi.REPO_BASE + path);
  }

  private void updateNav() {
    if (imageAdapter == null) return;
    View root = getView();
    if (root == null) return;
    int count = imageAdapter.getItemCount();
    View prev = root.findViewById(R.id.previewPrev);
    View next = root.findViewById(R.id.previewNext);
    prev.setVisibility(currentPage == 0 ? View.INVISIBLE : View.VISIBLE);
    next.setVisibility(currentPage >= count - 1 ? View.INVISIBLE : View.VISIBLE);
  }

  private void startDownload(
      View root,
      String url,
      FloatingActionButton install,
      LinearLayout progressContainer,
      ProgressBar progress,
      TextView progressText) {
    if (url == null || url.isEmpty()) return;
    Context context = root.getContext();
    install.setEnabled(false);
    install.setVisibility(View.INVISIBLE);
    progressContainer.setAlpha(0f);
    progressContainer.setVisibility(View.VISIBLE);
    progressContainer.animate().alpha(1f).setDuration(250).start();
    progress.setProgress(0);
    progressText.setText("0%");

    new Thread(
            () -> {
              String appliedMsg = null;
              boolean applied = false;
              File out = null;
              try {
                int dot = url.lastIndexOf('/');
                String fileName = dot >= 0 ? url.substring(dot + 1) : "theme.gth";
                if (!fileName.endsWith(".gth")) {
                  fileName = fileName + ".gth";
                }
                File dir = new File(Environment.getExternalStorageDirectory(), "ghostide/themes");
                dir.mkdirs();
                out = new File(dir, fileName);
                Request request = new Request.Builder().url(url).get().build();
                try (Response response = client.newCall(request).execute()) {
                  if (response.isSuccessful() && response.body() != null) {
                    long total = response.body().contentLength();
                    try (FileOutputStream fos = new FileOutputStream(out);
                        InputStream is = response.body().byteStream()) {
                      byte[] buffer = new byte[8192];
                      int read;
                      long done = 0;
                      while ((read = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, read);
                        done += read;
                        publishProgress(
                            root, progress, progressText, total, done, progressContainer);
                      }
                    }
                  }
                }
                if (out != null && out.exists()) {
                  int lastSlash = url.lastIndexOf('/');
                  String themeDirUrl =
                      lastSlash >= 0 ? url.substring(0, lastSlash + 1) : ThemesApi.REPO_BASE;
                  File bg = downloadBackground(out, themeDirUrl);
                  new ThemeManager(context).setThemeFromFile(out.getAbsolutePath());
                  applied = true;
                  appliedMsg =
                      bg != null
                          ? context.getString(R.string.themes_applied)
                          : context.getString(R.string.themes_background_failed);
                }
              } catch (Exception e) {
                applied = false;
              }
              boolean success = applied;
              String message = appliedMsg;
              requireActivity()
                  .runOnUiThread(
                      () -> {
                        progressContainer
                            .animate()
                            .alpha(0f)
                            .setDuration(400)
                            .setListener(
                                new AnimatorListenerAdapter() {
                                  @Override
                                  public void onAnimationEnd(Animator animation) {
                                    progressContainer.setVisibility(View.GONE);
                                  }
                                })
                            .start();
                        install.setEnabled(true);
                        install.setVisibility(View.VISIBLE);
                        int res =
                            success ? R.string.themes_applied : R.string.themes_download_failed;
                        Toast.makeText(
                                context,
                                message != null ? message : context.getString(res),
                                Toast.LENGTH_SHORT)
                            .show();
                          if(success) {
                          	EventBus.getDefault().post(new ThemeInstalledEvent());
                          }  
                      });
            })
        .start();
  }

  private void publishProgress(
      View root,
      ProgressBar progress,
      TextView progressText,
      long total,
      long done,
      LinearLayout container) {
    if (total <= 0 || root.getContext() == null) return;
    int percent = (int) ((done * 100) / total);
    root.post(
        () -> {
          if (progress == null || progressText == null) return;
          if (container.getVisibility() != View.VISIBLE) {
            container.setVisibility(View.VISIBLE);
          }
          progress.setProgress(percent);
          progressText.setText(percent + "%");
        });
  }

  /**
   * The .gth theme stores its background image in "widget.imagepath" as a relative path (e.g.
   * ./wallpaper.png). That image lives in the theme's repo folder next to the .gth file. Download
   * it into the same local folder so the theme's reference resolves on device.
   */
  private File downloadBackground(File themeFile, String themeDirUrl) {
    try {
      String json = FileIOUtils.readFile2String(themeFile);
      JsonObject root = JsonParser.parseString(json).getAsJsonObject();
      if (!root.has("widget")) return null;
      JsonObject widget = root.getAsJsonObject("widget");
      if (!widget.has("imagepath")) return null;
      String imagepath = widget.get("imagepath").getAsString();
      if (imagepath == null || imagepath.isEmpty()) return null;

      if (imagepath.startsWith("http")
          || imagepath.startsWith("/")
          || imagepath.startsWith("content:")
          || imagepath.startsWith("file:")) {
        return null;
      }
      while (imagepath.startsWith("../")) {
        imagepath = imagepath.substring(3);
      }
      if (imagepath.startsWith("./")) {
        imagepath = imagepath.substring(2);
      }
      String bgUrl = themeDirUrl + imagepath;
      String bgName = new File(bgUrl).getName();
      File dir = themeFile.getParentFile();
      File bg = new File(dir, bgName);
      Request request = new Request.Builder().url(bgUrl).get().build();
      try (Response response = client.newCall(request).execute()) {
        if (response.isSuccessful() && response.body() != null) {
          try (OutputStream fos = new FileOutputStream(bg);
              InputStream is = response.body().byteStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
              fos.write(buffer, 0, read);
            }
          }
          return bg;
        }
      }
    } catch (Exception e) {
      // background is optional; keep the theme anyway
    }
    return null;
  }

  /** Slide + scale animation when swiping between preview pages. */
  private static class DepthPageTransformer implements ViewPager2.PageTransformer {
    @Override
    public void transformPage(View page, float position) {
      if (position < -1f) {
        page.setAlpha(0f);
      } else if (position <= 0f) {
        page.setAlpha(1f);
        page.setTranslationX(0f);
        page.setScaleX(1f);
        page.setScaleY(1f);
      } else if (position <= 1f) {
        page.setAlpha(1f - position);
        page.setTranslationX(-position * page.getWidth());
        page.setScaleX(1f - 0.15f * position);
        page.setScaleY(1f - 0.15f * position);
      } else {
        page.setAlpha(0f);
      }
    }
  }
}
