package ir.hanzodev1375.ghostide.activity;

import android.app.WallpaperManager;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.palette.graphics.Palette;
import androidx.viewpager2.widget.ViewPager2;
import com.example.liquidglass.LiquidGlassView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import ir.hanzodev1375.ghostide.R;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import ir.hanzodev1375.ghostide.adapters.ImagePagerAdapter;
import ir.hanzodev1375.ghostide.databinding.ActivityImageViewerBinding;
import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import jp.wasabeef.blurry.Blurry;

public class ImageViewerActivity extends BaseCompat {

  public static final String EXTRA_IMAGE_URIS = "extra_image_uris";
  public static final String EXTRA_CURRENT_INDEX = "extra_current_index";
  private ActivityImageViewerBinding bind;

  private ViewPager2 viewPager;
  private ImageView ivBlurBg;
  private TextView tvCounter;
  private LinearLayout topBar, bottomBar;
  private List<Uri> uriList = new ArrayList<>();
  private ImagePagerAdapter adapter;
  private ImageButton btnSettings,
      btnGallery,
      btnNext,
      btnInfo,
      btnRotate,
      btnZoom,
      btnSave,
      btnShare;

  private void setupGlassBackdrop(LiquidGlassView glass) {
    glass.setBackdropSource(bind.contentArea);
    glass.setEnableDynamicBackground(true);
    glass.setEnableChromaticAberration(true);
    glass.setEnableEdgeHighlight(true);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    bind = ActivityImageViewerBinding.inflate(getLayoutInflater());
    setContentView(bind.getRoot());
    enableEdgeToEdge();
    ivBlurBg = bind.ivBlurBg;
    viewPager = bind.viewPager;
    tvCounter = bind.tvCounter;
    topBar = bind.topBar;
    bottomBar = bind.bottomBar;
    setupGlassBackdrop(bind.btnSettingsg);
    setupGlassBackdrop(bind.btnGalleryg);
    setupGlassBackdrop(bind.btnNextg);
    setupGlassBackdrop(bind.btnInfog);
    setupGlassBackdrop(bind.btnRotateg);
    setupGlassBackdrop(bind.btnZoomg);
    setupGlassBackdrop(bind.btnSaveg);
    setupGlassBackdrop(bind.btnShareg);
    btnSettings = bind.btnSettings;
    btnGallery = bind.btnGallery;
    btnNext = bind.btnNext;
    btnInfo = bind.btnInfo;
    btnRotate = bind.btnRotate;
    btnZoom = bind.btnZoom;
    btnSave = bind.btnSave;
    btnShare = bind.btnShare;
    btnGallery.setOnClickListener(v -> showWallpaperOptionsDialog());
    Intent intent = getIntent();
    if (intent != null) {
      if (intent.hasExtra(EXTRA_IMAGE_URIS)) {
        Object extra = intent.getSerializableExtra(EXTRA_IMAGE_URIS);
        if (extra instanceof ArrayList) {
          ArrayList<String> strings = (ArrayList<String>) extra;
          if (strings != null) {
            for (String s : strings) {
              File file = new File(s);
              if (file.exists()) {
                uriList.add(Uri.fromFile(file));
              }
            }
          }
        } else if (extra instanceof String) {
          String s = (String) extra;
          File file = new File(s);
          if (file.exists()) {
            uriList.add(Uri.fromFile(file));
          }
        }
      } else if (intent.getData() != null) {
        uriList.add(intent.getData());
      }
    }

    if (uriList.isEmpty()) {
      Toast.makeText(this, "No image to show", Toast.LENGTH_SHORT).show();
      finish();
      return;
    }

    adapter = new ImagePagerAdapter(this, uriList);
    viewPager.setAdapter(adapter);
    viewPager.setOffscreenPageLimit(1);
    int startIndex = intent.getIntExtra(EXTRA_CURRENT_INDEX, 0);
    if (startIndex < 0 || startIndex >= uriList.size()) startIndex = 0;
    viewPager.setCurrentItem(startIndex, false);
    updateCounter(startIndex);
    loadDynamicColorsAndBlur(startIndex);

    viewPager.registerOnPageChangeCallback(
        new ViewPager2.OnPageChangeCallback() {
          @Override
          public void onPageSelected(int position) {
            updateCounter(position);
            loadDynamicColorsAndBlur(position);
          }
        });

    btnSettings.setOnClickListener(
        v -> Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show());
    btnNext.setOnClickListener(
        v -> {
          finish();
        });
    btnInfo.setOnClickListener(v -> showImageInfo());
    btnRotate.setOnClickListener(
        v -> Toast.makeText(this, "Rotate not implemented", Toast.LENGTH_SHORT).show());
    btnZoom.setOnClickListener(
        v -> Toast.makeText(this, "Zoom not implemented", Toast.LENGTH_SHORT).show());
    btnSave.setOnClickListener(
        v -> {
          try {
            saveCurrentImage();
          } catch (Exception err) {
            Log.e(getClass().getName(), err.getMessage());
          }
        });
    btnShare.setOnClickListener(v -> shareCurrentImage());
  }

  private void enableEdgeToEdge() {
    WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
    getWindow().setNavigationBarColor(Color.TRANSPARENT);
    getWindow().setStatusBarColor(Color.TRANSPARENT);
    ViewCompat.setOnApplyWindowInsetsListener(
        bind.getRoot(),
        (v, insets) -> {
          Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
          Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
          topBar.setPadding(
              topBar.getPaddingLeft(),
              bars.top + dp(12),
              topBar.getPaddingRight(),
              topBar.getPaddingBottom());
          int bottomInset = Math.max(bars.bottom, ime.bottom);
          bottomBar.setPadding(
              bottomBar.getPaddingLeft(),
              bottomBar.getPaddingTop(),
              bottomBar.getPaddingRight(),
              bottomInset + dp(16));
          return insets;
        });
  }

  private int dp(int value) {
    return (int) (value * getResources().getDisplayMetrics().density);
  }

  private void updateCounter(int pos) {
    tvCounter.setText((pos + 1) + "·" + uriList.size());
  }

  private void loadDynamicColorsAndBlur(int position) {
    if (uriList.isEmpty()) return;
    Glide.with(this)
        .asBitmap()
        .load(uriList.get(position))
        .into(
            new CustomTarget<Bitmap>() {
              @Override
              public void onResourceReady(
                  @NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                ivBlurBg.setScaleType(ImageView.ScaleType.CENTER_CROP);
                Blurry.with(ImageViewerActivity.this)
                    .radius(20)
                    .sampling(8)
                    .async()
                    .from(resource)
                    .into(ivBlurBg);
                Palette.from(resource)
                    .generate(
                        palette -> {
                          int defaultColor = Color.parseColor("#1E1E1E");
                          int vibrant = palette.getVibrantColor(defaultColor);
                          int darkVibrant = palette.getDarkVibrantColor(defaultColor);
                          int lightMuted = palette.getLightMutedColor(Color.WHITE);
                          int bgColor = vibrant != defaultColor ? vibrant : darkVibrant;
                          int iconTint = lightMuted;
                          if (Math.abs(Color.red(bgColor) - Color.red(iconTint)) < 50
                              && Math.abs(Color.green(bgColor) - Color.green(iconTint)) < 50
                              && Math.abs(Color.blue(bgColor) - Color.blue(iconTint)) < 50) {
                            iconTint = Color.WHITE;
                          }
                          applyIconTint(iconTint);
                        });
              }

              @Override
              public void onLoadCleared(@Nullable Drawable placeholder) {}
            });
  }

  private void applyIconTint(int color) {

    btnSettings.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
    btnGallery.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
    btnNext.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
    btnInfo.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
    btnRotate.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
    btnZoom.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
    btnSave.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
    btnShare.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      btnSettings.setImageTintList(ColorStateList.valueOf(color));
      btnGallery.setImageTintList(ColorStateList.valueOf(color));
      btnNext.setImageTintList(ColorStateList.valueOf(color));
      btnInfo.setImageTintList(ColorStateList.valueOf(color));
      btnRotate.setImageTintList(ColorStateList.valueOf(color));
      btnZoom.setImageTintList(ColorStateList.valueOf(color));
      btnSave.setImageTintList(ColorStateList.valueOf(color));
      btnShare.setImageTintList(ColorStateList.valueOf(color));
    }

    tvCounter.setTextColor(color);
  }

  private void showImageInfo() {
    if (uriList.isEmpty()) return;
    Uri uri = uriList.get(viewPager.getCurrentItem());
    Toast.makeText(this, "URI: " + uri.toString(), Toast.LENGTH_LONG).show();
  }

  private void saveCurrentImage() {
    if (uriList.isEmpty()) return;
    Uri imageUri = uriList.get(viewPager.getCurrentItem());
    new Thread(
            () -> {
              try {
                Bitmap bitmap = Glide.with(this).asBitmap().load(imageUri).submit().get();
                if (bitmap == null) return;
                ContentValues values = new ContentValues();
                values.put(
                    MediaStore.Images.Media.DISPLAY_NAME,
                    "img_" + System.currentTimeMillis() + ".jpg");
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                values.put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/ImageViewer");
                Uri collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                Uri saveUri = getContentResolver().insert(collection, values);
                if (saveUri != null) {
                  try (OutputStream oss = getContentResolver().openOutputStream(saveUri)) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, oss);
                  }
                  runOnUiThread(
                      () -> Toast.makeText(this, "Saved to Gallery", Toast.LENGTH_SHORT).show());
                } else {
                  runOnUiThread(
                      () -> Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show());
                }
              } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(
                    () -> Toast.makeText(this, "Error saving", Toast.LENGTH_SHORT).show());
              }
            })
        .start();
  }

  private void shareCurrentImage() {
    if (uriList.isEmpty()) return;
    Uri imageUri = uriList.get(viewPager.getCurrentItem());
    Intent shareIntent = new Intent(Intent.ACTION_SEND);
    shareIntent.setType("image/*");
    shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
    startActivity(Intent.createChooser(shareIntent, "Share Image"));
  }

  private void showWallpaperOptionsDialog() {
    String[] options = {
      getString(R.string.wallpaper_option_home),
      getString(R.string.wallpaper_option_lock),
      getString(R.string.wallpaper_option_both)
    };

    new MaterialAlertDialogBuilder(this)
        .setTitle(R.string.wallpaper_dialog_title)
        .setItems(
            options,
            (dialog, which) -> {
              switch (which) {
                case 0:
                  applyWallpaper(WallpaperManager.FLAG_SYSTEM);
                  break;
                case 1:
                  applyWallpaper(WallpaperManager.FLAG_LOCK);
                  break;
                case 2:
                  applyWallpaper(WallpaperManager.FLAG_SYSTEM | WallpaperManager.FLAG_LOCK);
                  break;
              }
            })
        .setNegativeButton(R.string.cancel, null)
        .show();
  }

  private void applyWallpaper(int flags) {
    if (uriList.isEmpty()) return;
    Uri imageUri = uriList.get(viewPager.getCurrentItem());

    new Thread(
            () -> {
              try {
                Bitmap bitmap = Glide.with(this).asBitmap().load(imageUri).submit().get();
                if (bitmap == null) return;

                WallpaperManager wm = WallpaperManager.getInstance(this);
                wm.setBitmap(bitmap, null, true, flags);

                runOnUiThread(
                    () ->
                        Toast.makeText(this, R.string.wallpaper_set_success, Toast.LENGTH_SHORT)
                            .show());
              } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(
                    () ->
                        Toast.makeText(this, R.string.wallpaper_set_failed, Toast.LENGTH_SHORT)
                            .show());
              }
            })
        .start();
  }
}
