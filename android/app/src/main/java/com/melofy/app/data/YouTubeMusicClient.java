package com.melofy.app.data;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Keyless YouTube Music search client using YouTube Music's InnerTube WEB_REMIX API.
 * Mirrors backend/core/youtube.py and desktop/src/ytmusic.js so Android searches and plays
 * real YouTube Music tracks without needing an API key or external backend server.
 */
public class YouTubeMusicClient {

    private static final String INNERTUBE_SEARCH_URL = "https://music.youtube.com/youtubei/v1/search?prettyPrint=false";
    private static final String SONGS_PARAMS = "EgWKAQIIAWoMEAMQBBAJEAoQBRAV";
    private static final long CACHE_TTL_MS = 600_000L;

    private static final Pattern DURATION_PATTERN = Pattern.compile("^\\d+:\\d{2}(:\\d{2})?$");
    private static final Pattern PLAYS_PATTERN = Pattern.compile("^\\d+(\\.\\d+)?[KMB]?\\s+(plays|views|listeners)$", Pattern.CASE_INSENSITIVE);

    public static final String[] RANDOM_SEED_QUERIES = {
            "pop hits", "hip hop", "rock anthems", "electronic dance",
            "chill lofi beats", "r&b soul", "indie music", "k-pop",
            "jazz standards", "classical piano", "acoustic guitar", "reggae",
            "latin music", "afrobeats", "synthwave", "folk music", "indonesian pop"
    };

    private static final class CacheEntry {
        final long timestampMs;
        final JSONArray songs;

        CacheEntry(long timestampMs, JSONArray songs) {
            this.timestampMs = timestampMs;
            this.songs = songs;
        }
    }

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public JSONArray search(String query, int limit) throws Exception {
        String normalized = query == null ? "" : query.trim();
        if (normalized.isEmpty()) {
            return new JSONArray();
        }

        String cacheKey = normalized.toLowerCase() + "::" + limit;
        CacheEntry hit = cache.get(cacheKey);
        if (hit != null && (System.currentTimeMillis() - hit.timestampMs) < CACHE_TTL_MS) {
            return hit.songs;
        }

        String rawResponse = postSearchRequest(normalized);
        JSONArray songs = parseInnerTubeSongs(rawResponse, limit);
        if (songs.length() > 0) {
            cache.put(cacheKey, new CacheEntry(System.currentTimeMillis(), songs));
        }
        return songs;
    }

    public JSONObject randomPicks(int limit) throws Exception {
        String seed = RANDOM_SEED_QUERIES[random.nextInt(RANDOM_SEED_QUERIES.length)];
        JSONArray songs = search(seed, limit);
        JSONObject result = new JSONObject();
        result.put("seed", seed);
        result.put("results", songs);
        return result;
    }

    protected String postSearchRequest(String query) throws Exception {
        URL url = new URL(INNERTUBE_SEARCH_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(12_000);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36");
        conn.setRequestProperty("Origin", "https://music.youtube.com");
        conn.setRequestProperty("Referer", "https://music.youtube.com/");
        conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9");

        JSONObject client = new JSONObject();
        client.put("clientName", "WEB_REMIX");
        client.put("clientVersion", "1.20241118.01.00");
        client.put("hl", "en");
        client.put("gl", "US");

        JSONObject context = new JSONObject();
        context.put("client", client);

        JSONObject payload = new JSONObject();
        payload.put("context", context);
        payload.put("query", query);
        payload.put("params", SONGS_PARAMS);

        byte[] bodyBytes = payload.toString().getBytes(StandardCharsets.UTF_8);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(bodyBytes);
        }

        int status = conn.getResponseCode();
        InputStream stream = status >= 200 && status < 300 ? conn.getInputStream() : conn.getErrorStream();
        if (stream == null) {
            throw new IllegalStateException("InnerTube HTTP " + status);
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        if (status < 200 || status >= 300) {
            throw new IllegalStateException("InnerTube HTTP " + status);
        }
        return sb.toString();
    }

    public static JSONArray parseInnerTubeSongs(String rawJson, int limit) throws Exception {
        JSONArray songs = new JSONArray();
        Set<String> seen = new HashSet<>();

        JSONObject root = new JSONObject(rawJson);
        JSONObject contentsRoot = root.optJSONObject("contents");
        if (contentsRoot == null) return songs;
        JSONObject tabbed = contentsRoot.optJSONObject("tabbedSearchResultsRenderer");
        if (tabbed == null) return songs;
        JSONArray tabs = tabbed.optJSONArray("tabs");
        if (tabs == null) return songs;

        for (int t = 0; t < tabs.length(); t++) {
            JSONObject tabObj = tabs.optJSONObject(t);
            if (tabObj == null) continue;
            JSONObject tabRenderer = tabObj.optJSONObject("tabRenderer");
            if (tabRenderer == null) continue;
            JSONObject content = tabRenderer.optJSONObject("content");
            if (content == null) continue;
            JSONObject sectionList = content.optJSONObject("sectionListRenderer");
            if (sectionList == null) continue;
            JSONArray sections = sectionList.optJSONArray("contents");
            if (sections == null) continue;

            for (int s = 0; s < sections.length(); s++) {
                JSONObject secObj = sections.optJSONObject(s);
                if (secObj == null) continue;
                JSONObject shelf = secObj.optJSONObject("musicShelfRenderer");
                if (shelf == null) continue;
                JSONArray items = shelf.optJSONArray("contents");
                if (items == null) continue;

                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.optJSONObject(i);
                    if (item == null) continue;
                    JSONObject renderer = item.optJSONObject("musicResponsiveListItemRenderer");
                    if (renderer == null) continue;

                    String videoId = extractVideoId(renderer);
                    if (videoId.isEmpty() || seen.contains(videoId)) continue;
                    seen.add(videoId);

                    JSONArray cols = renderer.optJSONArray("flexColumns");
                    String title = extractColumnText(cols, 0);
                    String artist = extractArtists(cols);
                    String thumbnail = extractThumbnail(renderer);

                    JSONObject song = new JSONObject();
                    song.put("id", videoId);
                    song.put("title", title.isEmpty() ? "Untitled" : title);
                    song.put("artist", artist.isEmpty() ? "YouTube Music" : artist);
                    song.put("thumbnail", thumbnail);
                    songs.put(song);

                    if (songs.length() >= limit) {
                        return songs;
                    }
                }
            }
        }
        return songs;
    }

    private static String extractVideoId(JSONObject renderer) {
        JSONObject playlistData = renderer.optJSONObject("playlistItemData");
        if (playlistData != null) {
            String vid = playlistData.optString("videoId", "");
            if (!vid.isEmpty()) return vid;
        }
        JSONObject overlay = renderer.optJSONObject("overlay");
        if (overlay == null) return "";
        JSONObject thumbOverlay = overlay.optJSONObject("musicItemThumbnailOverlayRenderer");
        if (thumbOverlay == null) return "";
        JSONObject content = thumbOverlay.optJSONObject("content");
        if (content == null) return "";
        JSONObject playBtn = content.optJSONObject("musicPlayButtonRenderer");
        if (playBtn == null) return "";
        JSONObject nav = playBtn.optJSONObject("playNavigationEndpoint");
        if (nav == null) return "";
        JSONObject watch = nav.optJSONObject("watchEndpoint");
        return watch != null ? watch.optString("videoId", "") : "";
    }

    private static JSONArray getColumnRuns(JSONArray cols, int index) {
        if (cols == null || index >= cols.length()) return null;
        JSONObject col = cols.optJSONObject(index);
        if (col == null) return null;
        JSONObject flex = col.optJSONObject("musicResponsiveListItemFlexColumnRenderer");
        if (flex == null) return null;
        JSONObject text = flex.optJSONObject("text");
        return text != null ? text.optJSONArray("runs") : null;
    }

    private static String extractColumnText(JSONArray cols, int index) {
        JSONArray runs = getColumnRuns(cols, index);
        if (runs == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < runs.length(); i++) {
            JSONObject r = runs.optJSONObject(i);
            if (r != null) sb.append(r.optString("text", ""));
        }
        return sb.toString().trim();
    }

    private static String extractArtists(JSONArray cols) {
        JSONArray runs = getColumnRuns(cols, 1);
        if (runs == null || runs.length() == 0) return "";

        List<String> tagged = new ArrayList<>();
        for (int i = 0; i < runs.length(); i++) {
            JSONObject r = runs.optJSONObject(i);
            if (r == null) continue;
            JSONObject nav = r.optJSONObject("navigationEndpoint");
            JSONObject browse = nav != null ? nav.optJSONObject("browseEndpoint") : null;
            if (browse == null) continue;
            String browseId = browse.optString("browseId", "");
            JSONObject cfg = browse.optJSONObject("browseEndpointContextSupportedConfigs");
            JSONObject musicCfg = cfg != null ? cfg.optJSONObject("browseEndpointContextMusicConfig") : null;
            String pageType = musicCfg != null ? musicCfg.optString("pageType", "") : "";
            if ("MUSIC_PAGE_TYPE_ARTIST".equals(pageType) || browseId.startsWith("UC")) {
                String text = r.optString("text", "").trim();
                if (!text.isEmpty()) tagged.add(text);
            }
        }
        if (!tagged.isEmpty()) {
            return String.join(", ", tagged);
        }

        List<String> fallback = new ArrayList<>();
        for (int i = 0; i < runs.length(); i++) {
            JSONObject r = runs.optJSONObject(i);
            if (r == null) continue;
            String text = r.optString("text", "").trim();
            if (text.isEmpty()) continue;
            if ("•".equals(text) || "·".equals(text)) {
                if (!fallback.isEmpty()) break;
                continue;
            }
            if (fallback.isEmpty() && ("Song".equalsIgnoreCase(text) || "Video".equalsIgnoreCase(text)
                    || "Single".equalsIgnoreCase(text) || "EP".equalsIgnoreCase(text)
                    || "Album".equalsIgnoreCase(text))) {
                continue;
            }
            if (DURATION_PATTERN.matcher(text).matches() || PLAYS_PATTERN.matcher(text).matches()) {
                break;
            }
            if ("&".equals(text) || ",".equals(text) || "feat.".equals(text) || "ft.".equals(text)) {
                continue;
            }
            fallback.add(text);
        }
        if (!fallback.isEmpty()) {
            return String.join(", ", fallback);
        }
        return extractColumnText(cols, 1);
    }

    private static String extractThumbnail(JSONObject renderer) {
        JSONObject thumbObj = renderer.optJSONObject("thumbnail");
        if (thumbObj == null) return "";
        JSONObject musicThumb = thumbObj.optJSONObject("musicThumbnailRenderer");
        if (musicThumb == null) return "";
        JSONObject inner = musicThumb.optJSONObject("thumbnail");
        if (inner == null) return "";
        JSONArray thumbs = inner.optJSONArray("thumbnails");
        if (thumbs == null || thumbs.length() == 0) return "";
        JSONObject last = thumbs.optJSONObject(thumbs.length() - 1);
        return last != null ? last.optString("url", "") : "";
    }
}
