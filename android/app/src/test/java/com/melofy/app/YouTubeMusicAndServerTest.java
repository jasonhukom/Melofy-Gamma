package com.melofy.app;

import com.melofy.app.data.YouTubeMusicClient;
import com.melofy.app.server.LocalMelofyServer;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class YouTubeMusicAndServerTest {

    private static String buildSampleInnerTubeJson(String videoId, String title, String artist, String thumbUrl) throws Exception {
        JSONObject renderer = new JSONObject()
                .put("playlistItemData", new JSONObject().put("videoId", videoId))
                .put("flexColumns", new JSONArray()
                        .put(new JSONObject().put("musicResponsiveListItemFlexColumnRenderer",
                                new JSONObject().put("text", new JSONObject().put("runs",
                                        new JSONArray().put(new JSONObject().put("text", title))))))
                        .put(new JSONObject().put("musicResponsiveListItemFlexColumnRenderer",
                                new JSONObject().put("text", new JSONObject().put("runs", new JSONArray()
                                        .put(new JSONObject().put("text", artist)
                                                .put("navigationEndpoint", new JSONObject().put("browseEndpoint",
                                                        new JSONObject().put("browseId", "UC_artist"))))
                                        .put(new JSONObject().put("text", " • "))
                                        .put(new JSONObject().put("text", "4:08")))))))
                .put("thumbnail", new JSONObject().put("musicThumbnailRenderer",
                        new JSONObject().put("thumbnail", new JSONObject().put("thumbnails",
                                new JSONArray().put(new JSONObject().put("url", thumbUrl))))));

        JSONObject shelf = new JSONObject().put("musicShelfRenderer",
                new JSONObject().put("contents", new JSONArray().put(
                        new JSONObject().put("musicResponsiveListItemRenderer", renderer))));

        JSONObject tab = new JSONObject().put("tabRenderer",
                new JSONObject().put("content", new JSONObject().put("sectionListRenderer",
                        new JSONObject().put("contents", new JSONArray().put(shelf)))));

        return new JSONObject().put("contents", new JSONObject().put("tabbedSearchResultsRenderer",
                new JSONObject().put("tabs", new JSONArray().put(tab)))).toString();
    }

    @Test
    public void parseInnerTubeSongsExtractsValidTracks() throws Exception {
        String sampleJson = buildSampleInnerTubeJson(
                "4D7u5KF7SP8", "Get Lucky", "Daft Punk", "https://example.com/thumb.jpg");

        JSONArray songs = YouTubeMusicClient.parseInnerTubeSongs(sampleJson, 10);
        assertEquals(1, songs.length());
        JSONObject song = songs.getJSONObject(0);
        assertEquals("4D7u5KF7SP8", song.getString("id"));
        assertEquals("Get Lucky", song.getString("title"));
        assertEquals("Daft Punk", song.getString("artist"));
        assertEquals("https://example.com/thumb.jpg", song.getString("thumbnail"));
    }

    @Test
    public void localServerServesFrontendAndHealthEndpoints() throws Exception {
        Path frontendDir = Paths.get("..", "..", "backend", "frontend").normalize();
        assertTrue("Expected backend/frontend at " + frontendDir.toAbsolutePath(), Files.exists(frontendDir.resolve("index.html")));

        String sampleResponse = buildSampleInnerTubeJson(
                "testVid123", "Test Track", "Test Artist", "https://example.com/t.jpg");

        YouTubeMusicClient mockClient = new YouTubeMusicClient() {
            @Override
            protected String postSearchRequest(String query) {
                return sampleResponse;
            }
        };

        LocalMelofyServer server = new LocalMelofyServer(relativePath -> {
            Path target = frontendDir.resolve(relativePath).normalize();
            if (!target.startsWith(frontendDir) || !Files.exists(target)) {
                throw new IOException("Missing: " + relativePath);
            }
            return Files.readAllBytes(target);
        }, mockClient);

        String baseUrl = server.start();
        try {
            String html = httpGet(baseUrl + "/");
            assertFalse(html.contains("{%"));
            assertTrue(html.contains("/static/js/player.js"));

            String js = httpGet(baseUrl + "/static/js/player.js");
            assertTrue(js.contains("onYouTubeIframeAPIReady"));

            JSONObject health = new JSONObject(httpGet(baseUrl + "/api/health"));
            assertTrue(health.getBoolean("ok"));
            assertEquals("android", health.getString("mode"));

            JSONObject searchRes = new JSONObject(httpGet(baseUrl + "/api/search?q=lofi"));
            assertEquals("lofi", searchRes.getString("query"));
            assertEquals(1, searchRes.getJSONArray("results").length());
            assertEquals("testVid123", searchRes.getJSONArray("results").getJSONObject(0).getString("id"));

            JSONObject randomRes = new JSONObject(httpGet(baseUrl + "/api/random"));
            assertTrue(randomRes.has("seed"));
            assertEquals(1, randomRes.getJSONArray("results").length());
        } finally {
            server.stop();
        }
    }

    private static String httpGet(String urlStr) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        assertEquals(200, conn.getResponseCode());
        try (InputStream is = conn.getInputStream()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) != -1) out.write(buf, 0, n);
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }
}
