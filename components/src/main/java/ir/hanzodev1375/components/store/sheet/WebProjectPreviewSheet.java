package ir.hanzodev1375.components.store.sheet;

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

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import ir.hanzodev1375.components.R;
import ir.hanzodev1375.components.sheet.BaseBlurBottomSheet;
import ir.hanzodev1375.components.store.adapter.ThemeImageAdapter;
import ir.hanzodev1375.components.store.model.WebStore;
import ir.theme.M3Theme;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class WebProjectPreviewSheet extends BaseBlurBottomSheet {

  private static final String ARG_NAME = "arg_name";
  private static final String ARG_SCREEN1 = "arg_screen1";
  private static final String ARG_SCREEN2 = "arg_screen2";
  private static final String ARG_SCREEN3 = "arg_screen3";
  private static final String ARG_ZIP = "arg_zip";

  private static final OkHttpClient client =
      new OkHttpClient.Builder()
          .connectTimeout(15, TimeUnit.SECONDS)
          .readTimeout(60, TimeUnit.SECONDS)
          .build();

  private ThemeImageAdapter imageAdapter;
  private int currentPage;

  public static WebProjectPreviewSheet newInstance(WebStore store) {
    WebProjectPreviewSheet sheet = new WebProjectPreviewSheet();
    Bundle args = new Bundle();
    args.putString(ARG_NAME, store.getName());
    args.putString(ARG_SCREEN1, store.getScreen1());
    args.putString(ARG_SCREEN2, store.getScreen2());
    args.putString(ARG_SCREEN3, store.getScreen3());
    args.putString(ARG_ZIP, store.getZipFile());
    sheet.setArguments(args);
    return sheet;
  }

  @Override
  protected void onContentReady(ViewGroup contentContainer) {
    Bundle args = getArguments();
    if (args == null) return;

    Context context = requireContext();
    View v =
        LayoutInflater.from(context).inflate(R.layout.sheet_web_preview, contentContainer, false);
    contentContainer.addView(v);

    TextView title = v.findViewById(R.id.previewTitle);
    TextView subtitle = v.findViewById(R.id.previewSubtitle);
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

    String name = args.getString(ARG_NAME);
    title.setText(name != null ? name : context.getString(R.string.store_no_items));

    int imageCount = countImages(args);
    subtitle.setText(
        context.getString(R.string.webstore_images, imageCount));

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
    addImage(images, args.getString(ARG_SCREEN1));
    addImage(images, args.getString(ARG_SCREEN2));
    addImage(images, args.getString(ARG_SCREEN3));
    if (images.isEmpty()) {
      addImage(images, args.getString(ARG_SCREEN1));
    }
    imageAdapter.setUrls(images.toArray(new String[0]));
    currentPage = 0;
    updateNav();

    prev.setOnClickListener(inner -> pager.setCurrentItem(pager.getCurrentItem() - 1, true));
    next.setOnClickListener(inner -> pager.setCurrentItem(pager.getCurrentItem() + 1, true));
    install.setOnClickListener(
        inner ->
            startDownload(
                v,
                args.getString(ARG_ZIP),
                install,
                progressContainer,
                progress,
                progressText));

    M3Theme.text(title, subtitle, progressText);
    M3Theme.fab(install);
    M3Theme.applyShallow(v);
  }

  private int countImages(Bundle args) {
    int count = 0;
    if (hasText(args.getString(ARG_SCREEN1))) count++;
    if (hasText(args.getString(ARG_SCREEN2))) count++;
    if (hasText(args.getString(ARG_SCREEN3))) count++;
    return count;
  }

  private boolean hasText(String s) {
    return s != null && !s.isEmpty();
  }

  private void addImage(List<String> out, String path) {
    if (path == null || path.isEmpty()) return;
    out.add(path);
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
              boolean ok = false;
              String msg = null;
              try {
                int dot = url.lastIndexOf('/');
                String fileName = dot >= 0 ? url.substring(dot + 1) : "project.zip";
                if (!fileName.endsWith(".zip")) {
                  fileName = fileName + ".zip";
                }
                File dir = new File(Environment.getExternalStorageDirectory(), "ghostide/projects");
                dir.mkdirs();
                File out = new File(dir, fileName);
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
                ok = out.exists() && out.length() > 0;
                msg =
                    context.getString(
                        ok ? R.string.webstore_download_done : R.string.webstore_download_failed);
              } catch (Exception e) {
                ok = false;
                msg = context.getString(R.string.webstore_download_failed);
              }
              boolean success = ok;
              String message = msg;
              requireActivity()
                  .runOnUiThread(
                      () -> {
                        progressContainer.animate().alpha(0f).setDuration(400).start();
                        progressContainer.setVisibility(View.GONE);
                        install.setEnabled(true);
                        install.setVisibility(View.VISIBLE);
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                        if (success) {
                          dismiss();
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