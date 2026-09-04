package irhanzodev1375.musicpreview;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.google.android.material.color.MaterialColors;
import ir.theme.M3Theme;
import irhanzodev1375.musicpreview.databinding.MusicLayoutBinding;
import java.io.File;
import java.util.Map;

public class MusicView extends FrameLayout implements MusicPlayerBottomSheetFragment.MusicControl {

  private static final int SEEK_STEP_MS = 10000;

  private MusicLayoutBinding bind;
  private Music music;
  private String musicPath;
  private MediaPlayerListener externalListener;
  private Runnable onMusicClickListener;
  private String songName = "";
  private final Handler mainHandler = new Handler(Looper.getMainLooper());

  public MusicView(Context c) {
    super(c);
    init();
  }

  public MusicView(Context c, AttributeSet set) {
    super(c, set);
    init();
  }

  private void init() {
    bind = MusicLayoutBinding.inflate(LayoutInflater.from(getContext()), this, true);
    View rootMusicView = bind.getRoot();
    FrameLayout.LayoutParams params =
        new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER);
    rootMusicView.setLayoutParams(params);

    bind.play.setOnClickListener(v -> togglePlayback());
    bind.previous.setOnClickListener(v -> seekBackward());
    bind.next.setOnClickListener(v -> seekForward());
    rootMusicView.setOnClickListener(
        v -> {
          if (onMusicClickListener != null) {
            onMusicClickListener.run();
          }
        });
    stepBackground(rootMusicView);
    M3Theme.apply(this);
  }

  public void setOnMusicClickListener(Runnable listener) {
    this.onMusicClickListener = listener;
  }

  public void setSongName(String name) {
    this.songName = name;
  }

  public String getSongName() {
    return this.songName;
  }

  private void togglePlayback() {
    if (music == null) {
      return;
    }
    if (music.isPlaying()) {
      music.pause();
    } else {
      music.start();
    }
  }

  void stepBackground(View v) {
    var gd = new GradientDrawable();
    gd.setShape(GradientDrawable.RECTANGLE);
    gd.setColor(MaterialColors.getColor(v, R.attr.colorSurface));
    gd.setStroke(3, MaterialColors.getColor(v, R.attr.colorOutline));
    gd.setCornerRadius(25f);
    v.setBackground(gd);
  }

  private void seekBackward() {
    if (music == null) {
      return;
    }
    int target = music.getCurrentDuration() - SEEK_STEP_MS;
    music.seekTo(Math.max(target, 0));
  }

  private void seekForward() {
    if (music == null) {
      return;
    }
    int target = music.getCurrentDuration() + SEEK_STEP_MS;
    music.seekTo(Math.min(target, music.getDuration()));
  }

  private void updatePlayIcon(boolean isPlaying) {
    bind.play.setImageResource(
        isPlaying ? R.drawable.icon_pause_round : R.drawable.icon_play_arrow_round); //fix 
  }

  public String getMusicPath() {
    return this.musicPath;
  }

  public void setMusicPath(String musicPath) {
    this.musicPath = musicPath;
    if (music != null) {
      music.release();
    }
    music = new Music(getContext(), musicPath);
    music.setMediaPlayerListener(
        new MediaPlayerListener() {
          @Override
          public void isPlaying(int currentDuration) {
            if (externalListener != null) {
              externalListener.isPlaying(currentDuration);
            }
          }

          @Override
          public void onPause() {
            updatePlayIcon(false);
            if (externalListener != null) {
              externalListener.onPause();
            }
          }

          @Override
          public void onStart() {
            updatePlayIcon(true);
            if (externalListener != null) {
              externalListener.onStart();
            }
          }

          @Override
          public void onComplete() {
            updatePlayIcon(false);
            if (externalListener != null) {
              externalListener.onComplete();
            }
          }
        });
    if (musicPath != null
        && (musicPath.startsWith("http://") || musicPath.startsWith("https://"))) {
      music.setUrlSource(musicPath);
    } else if (musicPath != null) {
      music.setPathSource(new File(musicPath));
    }
    updatePlayIcon(false);
    try {
      String artist = music.getNameArtist();
      bind.nameartist.setText(artist);
    } catch (Exception err) {
      bind.nameartist.setText("");
    }

    if (music.getImageBitmap() != null) {
      bind.musiccaver.setImageBitmap(music.getImageBitmap());
      applyPaletteFromBitmap(music.getImageBitmap());
    }
  }

  public void setMediaPlayerListener(MediaPlayerListener listener) {
    this.externalListener = listener;
  }

  public void release() {
    if (music != null) {
      music.release();
      music = null;
    }
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    release();
  }

  @Override
  public void play() {
    if (music != null) music.start();
  }

  @Override
  public void pause() {
    if (music != null) music.pause();
  }

  @Override
  public boolean isPlaying() {
    return music != null && music.isPlaying();
  }

  @Override
  public void seekTo(int position) {
    if (music != null) music.seekTo(position);
  }

  @Override
  public int getCurrentPosition() {
    return music != null ? music.getCurrentDuration() : 0;
  }

  @Override
  public int getDuration() {
    return music != null ? music.getDuration() : 0;
  }

  @Override
  public Bitmap getAlbumArt() {
    return music != null ? music.getImageBitmap() : null;
  }

  @Override
  public String getArtistName() {
    if (music == null) return null;
    try {
      return music.getNameArtist();
    } catch (Exception e) {
      return null;
    }
  }

  private void applyPaletteFromBitmap(Bitmap bitmap) {
    ColorPaletteUtils.generateFromBitmap(
        bitmap,
        (lightColors, darkColors) -> {
          boolean isDark = isNightMode();
          Map<String, Integer> palette = isDark ? darkColors : lightColors;
          if (palette != null && !palette.isEmpty()) {
            mainHandler.post(() -> applyColorsFromPalette(palette));
          }
        });
  }

  private boolean isNightMode() {
    int nightModeFlags =
        getContext().getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
    return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
  }

  private void applyColorsFromPalette(Map<String, Integer> palette) {
    Integer surface = palette.get("surface");
    Integer onSurface = palette.get("onSurface");
    Integer primary = palette.get("primary");
    Integer outline = palette.get("outline");

    if (surface == null) surface = MaterialColors.getColor(this, R.attr.colorSurface, Color.DKGRAY);
    if (onSurface == null)
      onSurface = MaterialColors.getColor(this, R.attr.colorOnSurface, Color.WHITE);
    if (primary == null) primary = MaterialColors.getColor(this, R.attr.colorPrimary, Color.BLUE);
    if (outline == null) outline = MaterialColors.getColor(this, R.attr.colorOutline, Color.GRAY);

    View rootMusicView = bind.getRoot();
    var gd = new GradientDrawable();
    gd.setShape(GradientDrawable.RECTANGLE);
    gd.setColor(surface);
    gd.setStroke(3, outline);
    gd.setCornerRadius(25f);
    rootMusicView.setBackground(gd);

    bind.nameartist.setTextColor(onSurface);
    bind.play.setColorFilter(primary);
    bind.previous.setColorFilter(primary);
    bind.next.setColorFilter(primary);
  }
}
