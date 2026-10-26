package com.melofy.app.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import com.melofy.app.model.Track;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TrackRepository {

    public interface RepositoryListener {
        void onDataChanged();
    }

    private final Context context;
    private final SharedPreferences prefs;
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final List<Track> tracks = new ArrayList<>();
    private final Set<String> likedTrackIds = new HashSet<>();
    private final Set<String> downloadingTrackIds = ConcurrentHashMap.newKeySet();
    private final List<RepositoryListener> listeners = new ArrayList<>();

    public TrackRepository(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences("melofy_tracks_prefs_java", Context.MODE_PRIVATE);

        loadLikedTrackIds();
        initInitialTracks();
    }

    public void addListener(RepositoryListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(RepositoryListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        mainHandler.post(() -> {
            for (RepositoryListener listener : new ArrayList<>(listeners)) {
                listener.onDataChanged();
            }
        });
    }

    private void initInitialTracks() {
        tracks.clear();

        List<Track> sampleList = new ArrayList<>();
        sampleList.add(new Track(
                "melofy_1",
                "Midnight Horizon",
                "Lofi Sunset",
                "Chill Vibes Vol. 1",
                "https://picsum.photos/id/1025/400/400",
                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                372000L,
                "Lo-Fi",
                1420500L,
                1
        ));
        sampleList.add(new Track(
                "melofy_2",
                "Neon Synthwave Drift",
                "Aetherium",
                "Cyber Dreams",
                "https://picsum.photos/id/1084/400/400",
                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
                423000L,
                "Electronic",
                982100L,
                2
        ));
        sampleList.add(new Track(
                "melofy_3",
                "Acoustic Morning Coffee",
                "Luna & The Pines",
                "Unplugged Sessions",
                "https://picsum.photos/id/1062/400/400",
                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
                344000L,
                "Pop",
                850300L,
                3
        ));
        sampleList.add(new Track(
                "melofy_4",
                "Ethereal Echoes",
                "Solaris Wave",
                "Starlight Odyssey",
                "https://picsum.photos/id/1069/400/400",
                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
                302000L,
                "Ambient",
                740120L,
                4
        ));
        sampleList.add(new Track(
                "melofy_5",
                "Urban Rhythm Groove",
                "Metro Beatmakers",
                "City Lights",
                "https://picsum.photos/id/1074/400/400",
                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
                360000L,
                "Hip-Hop",
                620400L,
                5
        ));
        sampleList.add(new Track(
                "melofy_6",
                "Velvet Afterdark",
                "Kira Sound",
                "Smooth Midnight",
                "https://picsum.photos/id/1080/400/400",
                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3",
                285000L,
                "R&B",
                512000L,
                6
        ));

        Map<String, String> downloadedMap = loadDownloadedTracksMap();

        for (Track track : sampleList) {
            String localPath = downloadedMap.get(track.getId());
            boolean isDownloaded = localPath != null && !localPath.isEmpty() && new File(localPath).exists();
            track.setDownloaded(isDownloaded);
            track.setLocalFilePath(isDownloaded ? localPath : null);
            track.setLiked(likedTrackIds.contains(track.getId()));
            tracks.add(track);
        }
    }

    public List<Track> getAllTracks() {
        return new ArrayList<>(tracks);
    }

    public List<Track> getDownloadedTracks() {
        List<Track> result = new ArrayList<>();
        for (Track track : tracks) {
            if (track.isDownloaded()) {
                result.add(track);
            }
        }
        return result;
    }

    public Set<String> getLikedTrackIds() {
        return new HashSet<>(likedTrackIds);
    }

    public Set<String> getDownloadingTrackIds() {
        return new HashSet<>(downloadingTrackIds);
    }

    public void toggleLike(String trackId) {
        if (likedTrackIds.contains(trackId)) {
            likedTrackIds.remove(trackId);
        } else {
            likedTrackIds.add(trackId);
        }
        saveLikedTrackIds();

        for (Track track : tracks) {
            if (track.getId().equals(trackId)) {
                track.setLiked(likedTrackIds.contains(trackId));
            }
        }
        notifyListeners();
    }

    public void downloadTrack(Track track) {
        if (downloadingTrackIds.contains(track.getId())) return;

        downloadingTrackIds.add(track.getId());
        notifyListeners();

        executorService.execute(() -> {
            boolean success = false;
            try {
                File downloadsDir = new File(context.getFilesDir(), "downloads");
                if (!downloadsDir.exists()) downloadsDir.mkdirs();

                File outputFile = new File(downloadsDir, "melofy_" + track.getId() + ".mp3");

                URL url = new URL(track.getAudioUrl());
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                conn.connect();

                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    InputStream inputStream = conn.getInputStream();
                    FileOutputStream outputStream = new FileOutputStream(outputFile);
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    outputStream.flush();
                    outputStream.close();
                    inputStream.close();

                    saveDownloadedTrack(track.getId(), outputFile.getAbsolutePath());

                    for (Track t : tracks) {
                        if (t.getId().equals(track.getId())) {
                            t.setDownloaded(true);
                            t.setLocalFilePath(outputFile.getAbsolutePath());
                        }
                    }
                    success = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                downloadingTrackIds.remove(track.getId());
                notifyListeners();
            }
        });
    }

    public void removeDownload(Track track) {
        executorService.execute(() -> {
            Map<String, String> downloadedMap = loadDownloadedTracksMap();
            String path = downloadedMap.remove(track.getId());
            if (path != null) {
                File file = new File(path);
                if (file.exists()) {
                    file.delete();
                }
                saveDownloadedTracksMap(downloadedMap);

                for (Track t : tracks) {
                    if (t.getId().equals(track.getId())) {
                        t.setDownloaded(false);
                        t.setLocalFilePath(null);
                    }
                }
                notifyListeners();
            }
        });
    }

    private void loadLikedTrackIds() {
        Set<String> set = prefs.getStringSet("liked_track_ids", new HashSet<>());
        likedTrackIds.clear();
        if (set != null) {
            likedTrackIds.addAll(set);
        }
    }

    private void saveLikedTrackIds() {
        prefs.edit().putStringSet("liked_track_ids", likedTrackIds).apply();
    }

    private Map<String, String> loadDownloadedTracksMap() {
        String jsonStr = prefs.getString("downloaded_tracks_json", "{}");
        Map<String, String> map = new ConcurrentHashMap<>();
        try {
            JSONObject jsonObject = new JSONObject(jsonStr);
            Iterator<String> keys = jsonObject.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                map.put(key, jsonObject.getString(key));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return map;
    }

    private void saveDownloadedTrack(String trackId, String localPath) {
        Map<String, String> map = loadDownloadedTracksMap();
        map.put(trackId, localPath);
        saveDownloadedTracksMap(map);
    }

    private void saveDownloadedTracksMap(Map<String, String> map) {
        JSONObject jsonObject = new JSONObject();
        try {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                jsonObject.put(entry.getKey(), entry.getValue());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        prefs.edit().putString("downloaded_tracks_json", jsonObject.toString()).apply();
    }
}
