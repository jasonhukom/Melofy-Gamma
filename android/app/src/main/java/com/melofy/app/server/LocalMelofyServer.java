package com.melofy.app.server;

import android.content.res.AssetManager;

import com.melofy.app.data.YouTubeMusicClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lightweight embedded loopback HTTP server (127.0.0.1:<ephemeralPort>) for the Android app.
 * Serves the shared Lyra Melofy SPA frontend and provides keyless YouTube Music /api/search
 * and /api/random endpoints so the APK works out of the box with no external server required.
 */
public class LocalMelofyServer {

    public interface AssetLoader {
        byte[] load(String relativePath) throws IOException;
    }

    private static final Pattern LOAD_STATIC_PATTERN = Pattern.compile("\\{%\\s*load\\s+static\\s*%\\}\\r?\\n?");
    private static final Pattern STATIC_TAG_PATTERN = Pattern.compile("\\{%\\s*static\\s+['\"]([^'\"]+)['\"]\\s*%\\}");

    private final AssetLoader assetLoader;
    private final YouTubeMusicClient ytMusicClient;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    private ServerSocket serverSocket;
    private volatile boolean running = false;
    private int port = 0;
    private byte[] renderedIndexBytes;

    public LocalMelofyServer(AssetManager assetManager) {
        this(relativePath -> {
            String clean = relativePath.replaceFirst("^/+", "");
            try (InputStream is = assetManager.open("www/" + clean)) {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                byte[] chunk = new byte[8192];
                int n;
                while ((n = is.read(chunk)) != -1) {
                    buffer.write(chunk, 0, n);
                }
                return buffer.toByteArray();
            }
        }, new YouTubeMusicClient());
    }

    public LocalMelofyServer(AssetLoader assetLoader, YouTubeMusicClient ytMusicClient) {
        this.assetLoader = assetLoader;
        this.ytMusicClient = ytMusicClient;
    }

    public synchronized String start() throws IOException {
        if (running && serverSocket != null) {
            return getBaseUrl();
        }
        byte[] rawIndex = assetLoader.load("index.html");
        String rendered = renderIndexHtml(new String(rawIndex, StandardCharsets.UTF_8));
        this.renderedIndexBytes = rendered.getBytes(StandardCharsets.UTF_8);

        this.serverSocket = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"));
        this.port = serverSocket.getLocalPort();
        this.running = true;

        Thread acceptThread = new Thread(() -> {
            while (running) {
                try {
                    Socket client = serverSocket.accept();
                    executor.execute(() -> handleClient(client));
                } catch (IOException ignored) {
                    if (!running) break;
                }
            }
        }, "MelofyLocalServer");
        acceptThread.setDaemon(true);
        acceptThread.start();
        return getBaseUrl();
    }

    public String getBaseUrl() {
        return "http://127.0.0.1:" + port;
    }

    public synchronized void stop() {
        running = false;
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {}
            serverSocket = null;
        }
        executor.shutdownNow();
    }

    public static String renderIndexHtml(String rawHtml) {
        String withoutLoad = LOAD_STATIC_PATTERN.matcher(rawHtml).replaceAll("");
        Matcher m = STATIC_TAG_PATTERN.matcher(withoutLoad);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, Matcher.quoteReplacement("/static/" + m.group(1)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private void handleClient(Socket socket) {
        try (Socket s = socket) {
            s.setSoTimeout(15_000);
            BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
            OutputStream out = s.getOutputStream();

            String requestLine = reader.readLine();
            if (requestLine == null || requestLine.isEmpty()) return;

            String headerLine;
            while ((headerLine = reader.readLine()) != null && !headerLine.isEmpty()) {
                // Drain HTTP headers
            }

            String[] parts = requestLine.split(" ");
            if (parts.length < 2) {
                writeResponse(out, 400, "text/plain; charset=utf-8", "Bad Request".getBytes(StandardCharsets.UTF_8));
                return;
            }

            String target = parts[1];
            String path = target;
            String queryStr = "";
            int qIdx = target.indexOf('?');
            if (qIdx >= 0) {
                path = target.substring(0, qIdx);
                queryStr = target.substring(qIdx + 1);
            }
            path = URLDecoder.decode(path, "UTF-8");

            if ("/api/health".equals(path)) {
                JSONObject ok = new JSONObject();
                ok.put("ok", true);
                ok.put("mode", "android");
                writeJson(out, 200, ok.toString());
                return;
            }

            if ("/api/search".equals(path)) {
                Map<String, String> params = parseQueryParams(queryStr);
                String q = params.getOrDefault("q", "").trim();
                if (q.isEmpty()) {
                    writeJson(out, 400, "{\"error\":\"Missing q parameter\"}");
                    return;
                }
                try {
                    JSONArray results = ytMusicClient.search(q, 20);
                    JSONObject body = new JSONObject();
                    body.put("query", q);
                    body.put("results", results);
                    writeJson(out, 200, body.toString());
                } catch (Exception e) {
                    writeJson(out, 502, "{\"error\":\"Search is unavailable right now. Please try again in a moment.\"}");
                }
                return;
            }

            if ("/api/random".equals(path)) {
                try {
                    JSONObject picks = ytMusicClient.randomPicks(20);
                    writeJson(out, 200, picks.toString());
                } catch (Exception e) {
                    writeJson(out, 502, "{\"error\":\"Could not fetch picks right now.\"}");
                }
                return;
            }

            if ("/api/config".equals(path)) {
                writeJson(out, 200, "{\"firebase\":null,\"youtubeLinking\":false,\"android\":true}");
                return;
            }

            if ("/api/auth/me".equals(path)) {
                writeJson(out, 200, "{\"signedIn\":false}");
                return;
            }

            if (path.startsWith("/static/")) {
                String rel = sanitizeRelPath(path.substring("/static/".length()));
                if (rel == null) {
                    writeResponse(out, 403, "text/plain; charset=utf-8", "Forbidden".getBytes(StandardCharsets.UTF_8));
                    return;
                }
                try {
                    byte[] data = assetLoader.load(rel);
                    writeResponse(out, 200, mimeTypeFor(rel), data);
                } catch (IOException e) {
                    writeResponse(out, 404, "text/plain; charset=utf-8", "Not found".getBytes(StandardCharsets.UTF_8));
                }
                return;
            }

            if ("/favicon.ico".equals(path) || "/favicon.svg".equals(path) || "/site.webmanifest".equals(path)) {
                String rel = path.substring(1);
                try {
                    byte[] data = assetLoader.load(rel);
                    writeResponse(out, 200, mimeTypeFor(rel), data);
                    return;
                } catch (IOException ignored) {}
            }

            writeResponse(out, 200, "text/html; charset=utf-8", renderedIndexBytes);
        } catch (Exception ignored) {
        }
    }

    private static String sanitizeRelPath(String raw) {
        if (raw == null || raw.isEmpty() || raw.contains("..")) return null;
        return raw.replaceFirst("^/+", "");
    }

    private static Map<String, String> parseQueryParams(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isEmpty()) return map;
        for (String pair : query.split("&")) {
            int idx = pair.indexOf('=');
            try {
                if (idx > 0) {
                    String k = URLDecoder.decode(pair.substring(0, idx), "UTF-8");
                    String v = URLDecoder.decode(pair.substring(idx + 1), "UTF-8");
                    map.put(k, v);
                }
            } catch (Exception ignored) {}
        }
        return map;
    }

    private static String mimeTypeFor(String path) {
        String lower = path.toLowerCase(Locale.US);
        if (lower.endsWith(".html")) return "text/html; charset=utf-8";
        if (lower.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (lower.endsWith(".css")) return "text/css; charset=utf-8";
        if (lower.endsWith(".json")) return "application/json; charset=utf-8";
        if (lower.endsWith(".webmanifest")) return "application/manifest+json; charset=utf-8";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".ico")) return "image/x-icon";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".webp")) return "image/webp";
        return "application/octet-stream";
    }

    private static void writeJson(OutputStream out, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        String headers = "HTTP/1.1 " + status + " " + statusText(status) + "\r\n"
                + "Content-Type: application/json; charset=utf-8\r\n"
                + "Cache-Control: no-store\r\n"
                + "Content-Length: " + bytes.length + "\r\n"
                + "Connection: close\r\n\r\n";
        out.write(headers.getBytes(StandardCharsets.US_ASCII));
        out.write(bytes);
        out.flush();
    }

    private static void writeResponse(OutputStream out, int status, String contentType, byte[] body) throws IOException {
        String headers = "HTTP/1.1 " + status + " " + statusText(status) + "\r\n"
                + "Content-Type: " + contentType + "\r\n"
                + "Content-Length: " + body.length + "\r\n"
                + "Connection: close\r\n\r\n";
        out.write(headers.getBytes(StandardCharsets.US_ASCII));
        out.write(body);
        out.flush();
    }

    private static String statusText(int status) {
        if (status == 200) return "OK";
        if (status == 400) return "Bad Request";
        if (status == 403) return "Forbidden";
        if (status == 404) return "Not Found";
        if (status == 502) return "Bad Gateway";
        return "Error";
    }
}
