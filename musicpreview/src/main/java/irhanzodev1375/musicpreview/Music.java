package irhanzodev1375.musicpreview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import java.io.File;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class Music {

  private MediaPlayerListener mpl;
  private Timer _timer;
  private TimerTask timer;
  private MediaPlayer mediaplayer;
  private final Handler handler = new Handler(Looper.getMainLooper());
  private final Context classContext;
  private Bitmap bitmap;
  private final String path;

  public Music(Context context, String path) {
    classContext = context;
    this.path = path;
    _timer = new Timer();
    timer =
        new TimerTask() {
          @Override
          public void run() {
            handler.post(
                () -> {
                  if (mediaplayer != null && mediaplayer.isPlaying() && mpl != null) {
                    mpl.isPlaying(mediaplayer.getCurrentPosition());
                  }
                });
          }
        };
    _timer.scheduleAtFixedRate(timer, 0, 100);
  }

  public void setPathSource(File file) {
    mediaplayer = MediaPlayer.create(classContext, Uri.fromFile(file));
    attachCompletionListener();
    try {
      setImageBitmap(Uri.fromFile(file));
    } catch (Exception err) {

    }
  }

  public void setUrlSource(String urlSource) {
    mediaplayer = MediaPlayer.create(classContext, Uri.parse(urlSource));
    attachCompletionListener();
    try {
      setImageBitmap(urlSource);
    } catch (Exception err) {

    }
  }

  private void attachCompletionListener() {
    if (mediaplayer != null) {
      mediaplayer.setOnCompletionListener(
          mp -> {
            if (mpl != null) {
              mpl.onComplete();
            }
          });
    }
  }

  public void setMediaPlayerListener(MediaPlayerListener mpl) {
    this.mpl = mpl;
  }

  public void start() {
    if (mediaplayer != null) {
      mediaplayer.start();
      if (mpl != null) {
        mpl.onStart();
      }
    }
  }

  public void pause() {
    if (mediaplayer != null && mediaplayer.isPlaying()) {
      mediaplayer.pause();
      if (mpl != null) {
        mpl.onPause();
      }
    }
  }

  public void release() {
    if (mediaplayer != null) {
      mediaplayer.release();
      mediaplayer = null;
    }
    if (timer != null) {
      timer.cancel();
      timer = null;
    }
    if (_timer != null) {
      _timer.purge();
      _timer.cancel();
      _timer = null;
    }
  }

  public boolean isPlaying() {
    return mediaplayer != null && mediaplayer.isPlaying();
  }

  public void setLooping(boolean isLooping) {
    if (mediaplayer != null) {
      mediaplayer.setLooping(isLooping);
    }
  }

  public int getCurrentDuration() {
    return mediaplayer != null ? mediaplayer.getCurrentPosition() : 0;
  }

  public int getDuration() {
    return mediaplayer != null ? mediaplayer.getDuration() : 0;
  }

  public void seekTo(int seekToValue) {
    if (mediaplayer != null) {
      mediaplayer.seekTo(seekToValue);
    }
  }

  public MediaPlayer getMediaPlayer() {
    return mediaplayer;
  }

  public Bitmap getImageBitmap() {
    return bitmap;
  }

  public void setImageBitmap(Uri uri) throws Exception {
    MediaMetadataRetriever mmr = new MediaMetadataRetriever();
    try {
      mmr.setDataSource(classContext, uri);
      byte[] data = mmr.getEmbeddedPicture();
      if (data != null) {
        bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      mmr.release();
    }
  }

  public void setImageBitmap(String url) throws Exception {
    MediaMetadataRetriever mmr = new MediaMetadataRetriever();
    try {
      mmr.setDataSource(url, new HashMap<String, String>());
      byte[] data = mmr.getEmbeddedPicture();
      if (data != null) {
        bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      mmr.release();
    }
  }

  public String getNameArtist() throws Exception {
    MediaMetadataRetriever meta = new MediaMetadataRetriever();
    try {
      meta.setDataSource(path);
      String artist = meta.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
      return artist != null ? artist : classContext.getString(R.string.music_preview_no_artist);
    } catch (Exception e) {
      e.printStackTrace();
      return classContext.getString(R.string.music_preview_load_error);
    } finally {
      meta.release();
    }
  }

  public String getNameAlbom() throws Exception {
    MediaMetadataRetriever meta = new MediaMetadataRetriever();
    try {
      meta.setDataSource(path);
      String album = meta.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
      return album != null ? album : classContext.getString(R.string.music_preview_no_album);
    } catch (Exception e) {
      e.printStackTrace();
      return classContext.getString(R.string.music_preview_load_error);
    } finally {
      meta.release();
    }
  }

  public void setSpeed(float speed) {
    if (mediaplayer != null && Build.VERSION.SDK_INT >= 23) {
      mediaplayer.setPlaybackParams(mediaplayer.getPlaybackParams().setSpeed(speed));
    }
  }

  public void setSpeed(int speed) {
    setSpeed((float) speed);
  }

  public void setSpeed(double speed) {
    setSpeed((float) speed);
  }
}
