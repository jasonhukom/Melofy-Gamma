package com.melofy.app.data

import android.content.Context
import android.content.SharedPreferences
import com.melofy.app.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class TrackRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("melofy_tracks_prefs", Context.MODE_PRIVATE)

    private val _tracksFlow = MutableStateFlow<List<Track>>(emptyList())
    val tracksFlow: StateFlow<List<Track>> = _tracksFlow.asStateFlow()

    private val _likedTrackIds = MutableStateFlow<Set<String>>(emptySet())
    val likedTrackIds: StateFlow<Set<String>> = _likedTrackIds.asStateFlow()

    private val _downloadingTrackIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadingTrackIds: StateFlow<Set<String>> = _downloadingTrackIds.asStateFlow()

    init {
        loadLikedTrackIds()
        initInitialTracks()
    }

    private val initialSampleTracks = listOf(
        Track(
            id = "melofy_1",
            title = "Midnight Horizon",
            artist = "Lofi Sunset",
            album = "Chill Vibes Vol. 1",
            coverUrl = "https://picsum.photos/id/1025/400/400",
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
            durationMs = 372000L,
            genre = "Lo-Fi",
            playCount = 1420500L,
            rank = 1
        ),
        Track(
            id = "melofy_2",
            title = "Neon Synthwave Drift",
            artist = "Aetherium",
            album = "Cyber Dreams",
            coverUrl = "https://picsum.photos/id/1084/400/400",
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
            durationMs = 423000L,
            genre = "Electronic",
            playCount = 982100L,
            rank = 2
        ),
        Track(
            id = "melofy_3",
            title = "Acoustic Morning Coffee",
            artist = "Luna & The Pines",
            album = "Unplugged Sessions",
            coverUrl = "https://picsum.photos/id/1062/400/400",
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
            durationMs = 344000L,
            genre = "Pop",
            playCount = 850300L,
            rank = 3
        ),
        Track(
            id = "melofy_4",
            title = "Ethereal Echoes",
            artist = "Solaris Wave",
            album = "Starlight Odyssey",
            coverUrl = "https://picsum.photos/id/1069/400/400",
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
            durationMs = 302000L,
            genre = "Ambient",
            playCount = 740120L,
            rank = 4
        ),
        Track(
            id = "melofy_5",
            title = "Urban Rhythm Groove",
            artist = "Metro Beatmakers",
            album = "City Lights",
            coverUrl = "https://picsum.photos/id/1074/400/400",
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
            durationMs = 360000L,
            genre = "Hip-Hop",
            playCount = 620400L,
            rank = 5
        ),
        Track(
            id = "melofy_6",
            title = "Velvet Afterdark",
            artist = "Kira Sound",
            album = "Smooth Midnight",
            coverUrl = "https://picsum.photos/id/1080/400/400",
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3",
            durationMs = 285000L,
            genre = "R&B",
            playCount = 512000L,
            rank = 6
        )
    )

    private fun initInitialTracks() {
        val downloadedMap = loadDownloadedTracksMap()
        val likedSet = _likedTrackIds.value

        val mergedTracks = initialSampleTracks.map { track ->
            val localPath = downloadedMap[track.id]
            val isDownloaded = !localPath.isNullOrEmpty() && File(localPath).exists()
            track.copy(
                isDownloaded = isDownloaded,
                localFilePath = if (isDownloaded) localPath else null,
                isLiked = likedSet.contains(track.id)
            )
        }
        _tracksFlow.value = mergedTracks
    }

    fun toggleLike(trackId: String) {
        val currentSet = _likedTrackIds.value.toMutableSet()
        if (currentSet.contains(trackId)) {
            currentSet.remove(trackId)
        } else {
            currentSet.add(trackId)
        }
        _likedTrackIds.value = currentSet
        saveLikedTrackIds(currentSet)

        _tracksFlow.value = _tracksFlow.value.map { track ->
            if (track.id == trackId) {
                track.copy(isLiked = currentSet.contains(trackId))
            } else {
                track
            }
        }
    }

    suspend fun downloadTrack(track: Track): Boolean = withContext(Dispatchers.IO) {
        val currentDownloading = _downloadingTrackIds.value.toMutableSet()
        currentDownloading.add(track.id)
        _downloadingTrackIds.value = currentDownloading

        try {
            val downloadsDir = File(context.filesDir, "downloads")
            if (!downloadsDir.exists()) downloadsDir.mkdirs()

            val fileName = "melofy_${track.id}.mp3"
            val outputFile = File(downloadsDir, fileName)

            val url = URL(track.audioUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.connect()

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val inputStream = connection.inputStream
                val outputStream = FileOutputStream(outputFile)
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }
                outputStream.flush()
                outputStream.close()
                inputStream.close()

                saveDownloadedTrack(track.id, outputFile.absolutePath)

                _tracksFlow.value = _tracksFlow.value.map {
                    if (it.id == track.id) {
                        it.copy(isDownloaded = true, localFilePath = outputFile.absolutePath)
                    } else it
                }
                return@withContext true
            } else {
                return@withContext false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        } finally {
            val updatedDownloading = _downloadingTrackIds.value.toMutableSet()
            updatedDownloading.remove(track.id)
            _downloadingTrackIds.value = updatedDownloading
        }
    }

    suspend fun removeDownload(trackId: String): Boolean = withContext(Dispatchers.IO) {
        val downloadedMap = loadDownloadedTracksMap().toMutableMap()
        val path = downloadedMap[trackId]
        if (!path.isNullOrEmpty()) {
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
            downloadedMap.remove(trackId)
            saveDownloadedTracksMap(downloadedMap)

            _tracksFlow.value = _tracksFlow.value.map {
                if (it.id == trackId) {
                    it.copy(isDownloaded = false, localFilePath = null)
                } else it
            }
            return@withContext true
        }
        return@withContext false
    }

    private fun loadLikedTrackIds() {
        val set = prefs.getStringSet("liked_track_ids", emptySet()) ?: emptySet()
        _likedTrackIds.value = set
    }

    private fun saveLikedTrackIds(set: Set<String>) {
        prefs.edit().putStringSet("liked_track_ids", set).apply()
    }

    private fun loadDownloadedTracksMap(): Map<String, String> {
        val jsonStr = prefs.getString("downloaded_tracks_json", "{}") ?: "{}"
        val map = mutableMapOf<String, String>()
        try {
            val jsonObject = JSONObject(jsonStr)
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = jsonObject.getString(key)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return map
    }

    private fun saveDownloadedTrack(trackId: String, localPath: String) {
        val map = loadDownloadedTracksMap().toMutableMap()
        map[trackId] = localPath
        saveDownloadedTracksMap(map)
    }

    private fun saveDownloadedTracksMap(map: Map<String, String>) {
        val jsonObject = JSONObject()
        map.forEach { (key, value) -> jsonObject.put(key, value) }
        prefs.edit().putString("downloaded_tracks_json", jsonObject.toString()).apply()
    }
}
