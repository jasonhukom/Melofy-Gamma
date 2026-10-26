package com.melofy.app.model;

import java.io.File;
import java.util.Objects;

public class Track {
    private String id;
    private String title;
    private String artist;
    private String album;
    private String coverUrl;
    private String audioUrl;
    private long durationMs;
    private boolean isDownloaded;
    private String localFilePath;
    private boolean isLiked;
    private long playCount;
    private String genre;
    private int rank;

    public Track(String id, String title, String artist, String album, String coverUrl, String audioUrl, long durationMs, String genre, long playCount, int rank) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.album = album != null ? album : "";
        this.coverUrl = coverUrl;
        this.audioUrl = audioUrl;
        this.durationMs = durationMs;
        this.genre = genre != null ? genre : "Pop";
        this.playCount = playCount;
        this.rank = rank;
        this.isDownloaded = false;
        this.localFilePath = null;
        this.isLiked = false;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }

    public String getAlbum() { return album; }
    public void setAlbum(String album) { this.album = album; }

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    public String getAudioUrl() { return audioUrl; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }

    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }

    public boolean isDownloaded() { return isDownloaded; }
    public void setDownloaded(boolean downloaded) { isDownloaded = downloaded; }

    public String getLocalFilePath() { return localFilePath; }
    public void setLocalFilePath(String localFilePath) { this.localFilePath = localFilePath; }

    public boolean isLiked() { return isLiked; }
    public void setLiked(boolean liked) { isLiked = liked; }

    public long getPlayCount() { return playCount; }
    public void setPlayCount(long playCount) { this.playCount = playCount; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }

    public String getDisplayAudioUri() {
        if (isDownloaded && localFilePath != null && !localFilePath.isEmpty()) {
            File file = new File(localFilePath);
            if (file.exists()) {
                return localFilePath;
            }
        }
        return audioUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Track track = (Track) o;
        return Objects.equals(id, track.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
