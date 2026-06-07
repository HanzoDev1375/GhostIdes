package ir.hanzodev1375.ghostide.utils;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MusicPlayerManager {
    private static MusicPlayerManager instance;
    private Context context;
    private MediaPlayer mediaPlayer;
    private String currentSongPath;
    private boolean isPlaying;
    private int currentPosition;
    private List<PlaybackListener> listeners = new ArrayList<>();

    private MusicPlayerManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized MusicPlayerManager getInstance(Context context) {
        if (instance == null) {
            instance = new MusicPlayerManager(context);
        }
        return instance;
    }

    public void play(String path) {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.release();
            }
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(path);
            mediaPlayer.prepare();
            mediaPlayer.start();
            currentSongPath = path;
            isPlaying = true;
            notifyPlaybackStarted();
            setupCompletionListener();
            setupPositionUpdater();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            isPlaying = false;
            notifyPlaybackPaused();
        }
    }

    public void resume() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            isPlaying = true;
            notifyPlaybackResumed();
        }
    }

    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
            isPlaying = false;
            currentSongPath = null;
            notifyPlaybackStopped();
        }
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public String getCurrentSongPath() {
        return currentSongPath;
    }

    public int getDuration() {
        if (mediaPlayer != null) {
            return mediaPlayer.getDuration();
        }
        return 0;
    }

    public int getCurrentPosition() {
        if (mediaPlayer != null && isPlaying) {
            currentPosition = mediaPlayer.getCurrentPosition();
        }
        return currentPosition;
    }

    public void seekTo(int position) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(position);
            currentPosition = position;
        }
    }

    private void setupCompletionListener() {
        if (mediaPlayer != null) {
            mediaPlayer.setOnCompletionListener(mp -> {
                isPlaying = false;
                currentSongPath = null;
                notifyPlaybackCompleted();
            });
        }
    }

    private void setupPositionUpdater() {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && isPlaying) {
                    currentPosition = mediaPlayer.getCurrentPosition();
                    notifyPositionChanged(currentPosition);
                    handler.postDelayed(this, 500);
                }
            }
        }, 500);
    }

    public void addPlaybackListener(PlaybackListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removePlaybackListener(PlaybackListener listener) {
        listeners.remove(listener);
    }

    private void notifyPlaybackStarted() {
        for (PlaybackListener l : listeners) {
            l.onPlaybackStarted(currentSongPath);
        }
    }

    private void notifyPlaybackPaused() {
        for (PlaybackListener l : listeners) {
            l.onPlaybackPaused();
        }
    }

    private void notifyPlaybackResumed() {
        for (PlaybackListener l : listeners) {
            l.onPlaybackResumed();
        }
    }

    private void notifyPlaybackStopped() {
        for (PlaybackListener l : listeners) {
            l.onPlaybackStopped();
        }
    }

    private void notifyPlaybackCompleted() {
        for (PlaybackListener l : listeners) {
            l.onPlaybackCompleted();
        }
    }

    private void notifyPositionChanged(int position) {
        for (PlaybackListener l : listeners) {
            l.onPositionChanged(position);
        }
    }

    public interface PlaybackListener {
        void onPlaybackStarted(String path);
        void onPlaybackPaused();
        void onPlaybackResumed();
        void onPlaybackStopped();
        void onPlaybackCompleted();
        void onPositionChanged(int position);
    }
}
