package ir.hanzodev1375.ghostide.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import java.io.File;
import ir.hanzodev1375.ghostide.R;
import ir.hanzodev1375.ghostide.utils.MusicPlayerManager;

public class MusicPlayerBottomSheetFragment extends BottomSheetDialogFragment implements MusicPlayerManager.PlaybackListener {
    private TextView songTitle;
    private SeekBar seekBar;
    private ImageButton btnPlayPause;
    private MusicPlayerManager playerManager;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateSeekBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_music_player, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        songTitle = view.findViewById(R.id.songTitle);
        seekBar = view.findViewById(R.id.seekBar);
        btnPlayPause = view.findViewById(R.id.btnPlayPause);
        ImageButton btnPrev = view.findViewById(R.id.btnPrevious);
        ImageButton btnNext = view.findViewById(R.id.btnNext);

        playerManager = MusicPlayerManager.getInstance(requireContext());
        playerManager.addPlaybackListener(this);

        String currentPath = playerManager.getCurrentSongPath();
        if (currentPath != null) {
            songTitle.setText(new File(currentPath).getName());
            updatePlayPauseButton();
            if (playerManager.isPlaying()) {
                startUpdatingSeekBar();
            }
        }

        btnPlayPause.setOnClickListener(v -> {
            if (playerManager.isPlaying()) {
                playerManager.pause();
            } else {
                if (playerManager.getCurrentSongPath() != null) {
                    playerManager.resume();
                } else {
                    dismiss();
                }
            }
            updatePlayPauseButton();
        });

        btnPrev.setOnClickListener(v -> {});
        btnNext.setOnClickListener(v -> {});

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    playerManager.seekTo(progress);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    @Override
    public void onPlaybackStarted(String path) {
        songTitle.setText(new File(path).getName());
        updatePlayPauseButton();
        seekBar.setMax(playerManager.getDuration());
        startUpdatingSeekBar();
    }

    @Override
    public void onPlaybackPaused() {
        updatePlayPauseButton();
        stopUpdatingSeekBar();
    }

    @Override
    public void onPlaybackResumed() {
        updatePlayPauseButton();
        startUpdatingSeekBar();
    }

    @Override
    public void onPlaybackStopped() {
        updatePlayPauseButton();
        stopUpdatingSeekBar();
        dismiss();
    }

    @Override
    public void onPlaybackCompleted() {
        updatePlayPauseButton();
        stopUpdatingSeekBar();
        dismiss();
    }

    @Override
    public void onPositionChanged(int position) {
        seekBar.setProgress(position);
    }

    private void updatePlayPauseButton() {
        if (playerManager.isPlaying()) {
            btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
        } else {
            btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
        }
    }

    private void startUpdatingSeekBar() {
        if (updateSeekBar != null) stopUpdatingSeekBar();
        updateSeekBar = new Runnable() {
            @Override
            public void run() {
                if (playerManager.isPlaying()) {
                    seekBar.setProgress(playerManager.getCurrentPosition());
                    handler.postDelayed(this, 500);
                }
            }
        };
        handler.post(updateSeekBar);
    }

    private void stopUpdatingSeekBar() {
        if (updateSeekBar != null) {
            handler.removeCallbacks(updateSeekBar);
            updateSeekBar = null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        playerManager.removePlaybackListener(this);
        stopUpdatingSeekBar();
    }
}
