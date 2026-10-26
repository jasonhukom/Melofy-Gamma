package com.melofy.app.player;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.OptIn;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;

import com.melofy.app.model.Track;

import java.util.ArrayList;
import java.util.List;

public class MelofyPlayerManager {

    public interface PlayerStateListener {
        void onPlayerStateChanged(Track currentTrack, boolean isPlaying, long positionMs, long durationMs, boolean isShuffle, boolean isRepeat, boolean isLoading);
    }

    private final ExoPlayer player;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<PlayerStateListener> listeners = new ArrayList<>();

    private Track currentTrack = null;
    private List<Track> playlist = new ArrayList<>();
    private boolean isShuffle = false;
    private boolean isRepeat = false;
    private boolean isLoading = false;

    private Runnable progressRunnable;

    public MelofyPlayerManager(Context context) {
        player = new ExoPlayer.Builder(context.getApplicationContext()).build();

        player.addListener(new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                notifyListeners();
                if (isPlaying) {
                    startProgressTracker();
                } else {
                    stopProgressTracker();
                }
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                isLoading = (playbackState == Player.STATE_BUFFERING);
                notifyListeners();

                if (playbackState == Player.STATE_ENDED) {
                    onSongEnded();
                }
            }
        });
    }

    public void addListener(PlayerStateListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(PlayerStateListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        long pos = player.getCurrentPosition();
        long dur = player.getDuration() > 0 ? player.getDuration() : (currentTrack != null ? currentTrack.getDurationMs() : 0L);
        boolean playing = player.isPlaying();

        handler.post(() -> {
            for (PlayerStateListener listener : new ArrayList<>(listeners)) {
                listener.onPlayerStateChanged(currentTrack, playing, pos, dur, isShuffle, isRepeat, isLoading);
            }
        });
    }

    public void playTrack(Track track, List<Track> newPlaylist) {
        this.currentTrack = track;
        if (newPlaylist != null && !newPlaylist.isEmpty()) {
            this.playlist = new ArrayList<>(newPlaylist);
        } else {
            this.playlist = new ArrayList<>();
            this.playlist.add(track);
        }

        Uri uri = Uri.parse(track.getDisplayAudioUri());
        MediaItem mediaItem = MediaItem.fromUri(uri);
        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();
        notifyListeners();
    }

    public void togglePlayPause() {
        if (currentTrack == null && !playlist.isEmpty()) {
            playTrack(playlist.get(0), playlist);
            return;
        }
        if (player.isPlaying()) {
            player.pause();
        } else {
            player.play();
        }
    }

    public void seekTo(long positionMs) {
        player.seekTo(positionMs);
        notifyListeners();
    }

    public void skipNext() {
        if (currentTrack == null || playlist.isEmpty()) return;
        int currentIndex = playlist.indexOf(currentTrack);
        if (currentIndex != -1 && currentIndex < playlist.size() - 1) {
            playTrack(playlist.get(currentIndex + 1), playlist);
        } else if (!playlist.isEmpty()) {
            playTrack(playlist.get(0), playlist);
        }
    }

    public void skipPrevious() {
        if (currentTrack == null || playlist.isEmpty()) return;
        int currentIndex = playlist.indexOf(currentTrack);
        if (currentIndex > 0) {
            playTrack(playlist.get(currentIndex - 1), playlist);
        } else {
            playTrack(playlist.get(playlist.size() - 1), playlist);
        }
    }

    public void toggleShuffle() {
        isShuffle = !isShuffle;
        player.setShuffleModeEnabled(isShuffle);
        notifyListeners();
    }

    public void toggleRepeat() {
        isRepeat = !isRepeat;
        player.setRepeatMode(isRepeat ? Player.REPEAT_MODE_ONE : Player.REPEAT_MODE_OFF);
        notifyListeners();
    }

    private void onSongEnded() {
        if (isRepeat) {
            player.seekTo(0);
            player.play();
        } else {
            skipNext();
        }
    }

    private void startProgressTracker() {
        stopProgressTracker();
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                notifyListeners();
                handler.postDelayed(this, 500);
            }
        };
        handler.post(progressRunnable);
    }

    private void stopProgressTracker() {
        if (progressRunnable != null) {
            handler.removeCallbacks(progressRunnable);
            progressRunnable = null;
        }
    }

    public Track getCurrentTrack() {
        return currentTrack;
    }

    public boolean isPlaying() {
        return player.isPlaying();
    }

    public long getPositionMs() {
        return player.getCurrentPosition();
    }

    public long getDurationMs() {
        return player.getDuration() > 0 ? player.getDuration() : (currentTrack != null ? currentTrack.getDurationMs() : 0L);
    }

    public boolean isShuffle() {
        return isShuffle;
    }

    public boolean isRepeat() {
        return isRepeat;
    }

    public void release() {
        stopProgressTracker();
        player.release();
    }
}
