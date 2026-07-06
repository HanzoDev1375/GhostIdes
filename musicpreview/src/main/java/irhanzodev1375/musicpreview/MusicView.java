package irhanzodev1375.musicpreview;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Gravity;
import android.widget.FrameLayout;
import com.google.android.material.color.MaterialColors;
import irhanzodev1375.musicpreview.databinding.MusicLayoutBinding;
import java.io.File;

public class MusicView extends FrameLayout {

  private static final int SEEK_STEP_MS = 10000;

  private MusicLayoutBinding bind;
  private Music music;
  private String musicPath;
  private MediaPlayerListener externalListener;

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
    stepBackground(rootMusicView);
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
    gd.setStroke(3, MaterialColors.getColor(v, R.attr.colorOnSurface));
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
        isPlaying ? R.drawable.icon_pause_round : R.drawable.icon_play_arrow_round);
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
      bind.nameartist.setText(music.getNameArtist());
    } catch (Exception err) {
      bind.nameartist.setText("");
    }

    if (music.getImageBitmap() != null) {
      bind.musiccaver.setImageBitmap(music.getImageBitmap());
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
}
