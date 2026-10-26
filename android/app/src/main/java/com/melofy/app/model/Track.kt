package com.melofy.app.model

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val coverUrl: String,
    val audioUrl: String,
    val durationMs: Long = 180000L,
    val isDownloaded: Boolean = false,
    val localFilePath: String? = null,
    val isLiked: Boolean = false,
    val playCount: Long = 0L,
    val genre: String = "Pop",
    val rank: Int = 0
) {
    val displayAudioUri: String
        get() = if (isDownloaded && !localFilePath.isNullOrEmpty()) {
            localFilePath
        } else {
            audioUrl
        }
}
