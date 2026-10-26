package com.melofy.app.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.melofy.app.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PlayerUiState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playlist: List<Track> = emptyList(),
    val isShuffle: Boolean = false,
    val isRepeat: Boolean = false,
    val isLoading: Boolean = false
)

class MelofyPlayerManager(context: Context) {

    private val player: ExoPlayer = ExoPlayer.Builder(context).build()
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var progressTrackerJob: Job? = null

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
                if (isPlaying) {
                    startProgressTracker()
                } else {
                    stopProgressTracker()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val isLoading = playbackState == Player.STATE_BUFFERING
                val durationMs = if (player.duration > 0) player.duration else _uiState.value.currentTrack?.durationMs ?: 0L
                _uiState.value = _uiState.value.copy(
                    isLoading = isLoading,
                    durationMs = durationMs
                )
                if (playbackState == Player.STATE_ENDED) {
                    onSongEnded()
                }
            }
        })
    }

    fun playTrack(track: Track, newPlaylist: List<Track> = _uiState.value.playlist) {
        val playlist = if (newPlaylist.isEmpty()) listOf(track) else newPlaylist
        _uiState.value = _uiState.value.copy(
            currentTrack = track,
            playlist = playlist,
            positionMs = 0L,
            durationMs = track.durationMs
        )

        val uri = Uri.parse(track.displayAudioUri)
        val mediaItem = MediaItem.fromUri(uri)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    fun togglePlayPause() {
        if (_uiState.value.currentTrack == null && _uiState.value.playlist.isNotEmpty()) {
            playTrack(_uiState.value.playlist.first())
            return
        }
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
        _uiState.value = _uiState.value.copy(positionMs = positionMs)
    }

    fun skipNext() {
        val state = _uiState.value
        val current = state.currentTrack ?: return
        val playlist = state.playlist
        if (playlist.isEmpty()) return

        val currentIndex = playlist.indexOfFirst { it.id == current.id }
        if (currentIndex != -1 && currentIndex < playlist.size - 1) {
            playTrack(playlist[currentIndex + 1], playlist)
        } else if (playlist.isNotEmpty()) {
            playTrack(playlist.first(), playlist)
        }
    }

    fun skipPrevious() {
        val state = _uiState.value
        val current = state.currentTrack ?: return
        val playlist = state.playlist
        if (playlist.isEmpty()) return

        val currentIndex = playlist.indexOfFirst { it.id == current.id }
        if (currentIndex > 0) {
            playTrack(playlist[currentIndex - 1], playlist)
        } else {
            playTrack(playlist.last(), playlist)
        }
    }

    fun toggleShuffle() {
        val newShuffle = !_uiState.value.isShuffle
        _uiState.value = _uiState.value.copy(isShuffle = newShuffle)
        player.shuffleModeEnabled = newShuffle
    }

    fun toggleRepeat() {
        val newRepeat = !_uiState.value.isRepeat
        _uiState.value = _uiState.value.copy(isRepeat = newRepeat)
        player.repeatMode = if (newRepeat) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    private fun onSongEnded() {
        if (_uiState.value.isRepeat) {
            player.seekTo(0)
            player.play()
        } else {
            skipNext()
        }
    }

    private fun startProgressTracker() {
        stopProgressTracker()
        progressTrackerJob = scope.launch {
            while (true) {
                val currentPos = player.currentPosition
                val duration = if (player.duration > 0) player.duration else _uiState.value.currentTrack?.durationMs ?: 0L
                _uiState.value = _uiState.value.copy(
                    positionMs = currentPos,
                    durationMs = duration
                )
                delay(500)
            }
        }
    }

    private fun stopProgressTracker() {
        progressTrackerJob?.cancel()
        progressTrackerJob = null
    }

    fun release() {
        stopProgressTracker()
        player.release()
    }
}
