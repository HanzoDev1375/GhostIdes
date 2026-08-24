package ir.hanzodev1375.components.childern;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import java.io.IOException;

public class VideoChild implements IChild, DefaultLifecycleObserver {

  private final Context context;
  private final TextureView textureView;
  private final String path;
  private final LifecycleOwner owner;

  private MediaPlayer mediaPlayer;
  private Surface surface;
  private boolean prepared;
  private boolean resumed;

  public VideoChild(Context context, String path, LifecycleOwner owner) {
    this.context = context;
    this.path = path;
    this.owner = owner;
    this.textureView = new TextureView(context);
    this.textureView.setLayoutParams(
        new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    this.textureView.setOpaque(true);
    this.textureView.setSurfaceTextureListener(surfaceListener);
    // Delivers onResume() immediately when the owner is already resumed.
    owner.getLifecycle().addObserver(this);
  }

  @Override
  public View view() {
    return textureView;
  }

  @Override
  public String pathTheme() {
    return path;
  }

  @Override
  public void release() {
    owner.getLifecycle().removeObserver(this);
    resumed = false;
    teardown();
    textureView.setSurfaceTextureListener(null);
  }

  @Override
  public void onResume(@NonNull LifecycleOwner lifecycleOwner) {
    resumed = true;
    playIfReady();
  }

  @Override
  public void onPause(@NonNull LifecycleOwner lifecycleOwner) {
    resumed = false;
    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
      mediaPlayer.pause();
    }
  }

  @Override
  public void onDestroy(@NonNull LifecycleOwner lifecycleOwner) {
    release();
  }

  private final TextureView.SurfaceTextureListener surfaceListener =
      new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture st, int width, int height) {
          surface = new Surface(st);
          initPlayer();
        }

        @Override
        public void onSurfaceTextureSizeChanged(
            @NonNull SurfaceTexture st, int width, int height) {}

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture st) {
          teardown();
          return true;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture st) {}
      };

  private void initPlayer() {
    if (mediaPlayer != null || surface == null) return;
    try {
      mediaPlayer = new MediaPlayer();
      mediaPlayer.setAudioAttributes(
          new AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_MEDIA)
              .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
              .build());
      mediaPlayer.setVolume(0f, 0f);
      mediaPlayer.setLooping(true);
      mediaPlayer.setScreenOnWhilePlaying(false);
      mediaPlayer.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT);
      setDataSource();
      mediaPlayer.setSurface(surface);
      mediaPlayer.setOnPreparedListener(
          mp -> {
            prepared = true;
            playIfReady();
          });
      mediaPlayer.setOnErrorListener(
          (mp, what, extra) -> {
            teardown();
            return true;
          });
      mediaPlayer.prepareAsync();
    } catch (IOException | IllegalStateException | IllegalArgumentException e) {
      teardown();
    }
  }

  private void setDataSource() throws IOException {
    if (path != null && path.startsWith("content:")) {
      mediaPlayer.setDataSource(context, Uri.parse(path));
    } else {
      mediaPlayer.setDataSource(path);
    }
  }

  private void playIfReady() {
    if (prepared && resumed && mediaPlayer != null && !mediaPlayer.isPlaying()) {
      mediaPlayer.start();
    }
  }

  /** Releases player and surface; safe to call multiple times. */
  private void teardown() {
    prepared = false;
    if (mediaPlayer != null) {
      try {
        mediaPlayer.stop();
      } catch (IllegalStateException ignored) {
      }
      mediaPlayer.release();
      mediaPlayer = null;
    }
    if (surface != null) {
      surface.release();
      surface = null;
    }
  }
}
