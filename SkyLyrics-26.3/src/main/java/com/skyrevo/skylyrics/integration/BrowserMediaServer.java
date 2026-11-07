package com.skyrevo.skylyrics.integration;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.util.Locale;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Receives metadata from the SkyLyrics browser extension over a loopback-only HTTP server.
 * No audio bytes or remote-control commands are accepted.
 */
public final class BrowserMediaServer implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger("skylyrics-browser-media");
    public static final int PORT = 38173;
    private static final int MAX_HEADER_BYTES = 16_384;
    private static final int MAX_BODY_BYTES = 32_768;
    private static final long STALE_AFTER_MS = Duration.ofSeconds(5).toMillis();

    private final Map<Integer, MediaStatus> browserSessions = new ConcurrentHashMap<>();
    private volatile boolean running;
    private volatile ServerSocket serverSocket;
    private volatile String statusMessage = "Browser connector not connected.";

    public void start() {
        if (running) return;
        running = true;
        Thread serverThread = new Thread(this::serve, "SkyLyrics-Browser-Media-Server");
        serverThread.setDaemon(true);
        serverThread.start();
    }

    private void serve() {
        try (ServerSocket socket = new ServerSocket()) {
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), PORT), 16);
            serverSocket = socket;
            statusMessage = "Listening on 127.0.0.1:" + PORT;
            LOGGER.info("Browser media endpoint started on 127.0.0.1:{}", PORT);
            while (running) {
                Socket client = socket.accept();
                Thread handler = new Thread(() -> handle(client), "SkyLyrics-Media-Request");
                handler.setDaemon(true);
                handler.start();
            }
        } catch (IOException exception) {
            if (running) {
                statusMessage = "Local media port unavailable: " + exception.getMessage();
                LOGGER.warn("Could not start local media endpoint: {}", exception.getMessage());
            }
        } finally {
            running = false;
            serverSocket = null;
        }
    }

    private void handle(Socket socket) {
        try (socket) {
            socket.setSoTimeout(2_000);
            InputStream input = socket.getInputStream();
            OutputStream output = socket.getOutputStream();
            byte[] headerBytes = readHeaders(input);
            if (headerBytes == null) {
                respond(output, 400, "Bad Request", "text/plain", "Bad request");
                return;
            }
            String headers = new String(headerBytes, StandardCharsets.US_ASCII);
            String[] lines = headers.split("\\r\\n");
            if (lines.length == 0) {
                respond(output, 400, "Bad Request", "text/plain", "Bad request");
                return;
            }
            String[] request = lines[0].split(" ");
            if (request.length < 2) {
                respond(output, 400, "Bad Request", "text/plain", "Bad request");
                return;
            }
            String method = request[0].toUpperCase();
            String path = request[1].split("\\?", 2)[0];
            int contentLength = 0;
            String origin = "";
            for (String line : lines) {
                if (line.regionMatches(true, 0, "Content-Length:", 0, "Content-Length:".length())) {
                    try { contentLength = Integer.parseInt(line.substring(line.indexOf(':') + 1).trim()); }
                    catch (NumberFormatException ignored) { contentLength = -1; }
                } else if (line.regionMatches(true, 0, "Origin:", 0, "Origin:".length())) {
                    origin = line.substring(line.indexOf(':') + 1).trim();
                }
            }

            if (method.equals("GET") && path.equals("/health")) {
                respond(output, 200, "OK", "application/json", "{\"ok\":true,\"service\":\"SkyLyrics\"}");
                return;
            }
            if (method.equals("GET") && path.equals("/v1/status")) {
                MediaStatus current = latest();
                String payload = current == null ? "{\"available\":false}" : toJson(current);
                respond(output, 200, "OK", "application/json", payload);
                return;
            }
            if (method.equals("POST") && path.equals("/v1/browser-session")) {
                // Only Chromium extension origins are accepted; ordinary websites cannot inject media state.
                if (!isBrowserExtensionOrigin(origin)) {
                    respond(output, 403, "Forbidden", "application/json", "{\"error\":\"extension origin required\"}");
                    return;
                }
                if (contentLength < 0 || contentLength > MAX_BODY_BYTES) {
                    respond(output, 413, "Payload Too Large", "application/json", "{\"error\":\"invalid content length\"}");
                    return;
                }
                byte[] body = input.readNBytes(contentLength);
                if (body.length != contentLength) {
                    respond(output, 400, "Bad Request", "application/json", "{\"error\":\"incomplete request body\"}");
                    return;
                }
                acceptBrowserPayload(new String(body, StandardCharsets.UTF_8));
                respond(output, 204, "No Content", "text/plain", "");
                return;
            }
            respond(output, 404, "Not Found", "text/plain", "Not found");
        } catch (Exception exception) {
            // Disconnected browser tabs or game clients are expected and must not stop the listener.
        }
    }

    private static byte[] readHeaders(InputStream input) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int state = 0;
        while (bytes.size() < MAX_HEADER_BYTES) {
            int value = input.read();
            if (value < 0) return null;
            bytes.write(value);
            state = switch (state) {
                case 0 -> value == '\r' ? 1 : 0;
                case 1 -> value == '\n' ? 2 : value == '\r' ? 1 : 0;
                case 2 -> value == '\r' ? 3 : 0;
                case 3 -> value == '\n' ? 4 : 0;
                default -> state;
            };
            if (state == 4) return bytes.toByteArray();
        }
        return null;
    }

    private void acceptBrowserPayload(String payload) {
        JsonObject json = JsonParser.parseString(payload).getAsJsonObject();
        String source = string(json, "source");
        if (!source.equals("youtube_music") && !source.equals("youtube") && !source.equals("spotify_web")) return;
        int tabId = (int) Math.max(-1, Math.min(Integer.MAX_VALUE, number(json, "tabId")));
        MediaStatus status = new MediaStatus(
                true,
                bool(json, "playing"),
                source,
                clean(string(json, "title")),
                clean(string(json, "artist")),
                clean(string(json, "album")),
                safeArtworkUrl(string(json, "artworkUrl")),
                Math.max(0, number(json, "positionMs")),
                Math.max(0, number(json, "durationMs")),
                System.currentTimeMillis());
        if (status.title().isBlank()) return;
        browserSessions.put(tabId, status);
        statusMessage = "Connected: " + status.displaySource();
    }

    public MediaStatus latest() {
        long now = System.currentTimeMillis();
        browserSessions.entrySet().removeIf(entry -> now - entry.getValue().updatedAtEpochMs() > STALE_AFTER_MS);
        MediaStatus playing = browserSessions.values().stream().filter(MediaStatus::playing)
                .max(java.util.Comparator.comparingLong(MediaStatus::updatedAtEpochMs)).orElse(null);
        if (playing != null) return playing;
        MediaStatus paused = browserSessions.values().stream()
                .max(java.util.Comparator.comparingLong(MediaStatus::updatedAtEpochMs)).orElse(null);
        if (paused == null && running) statusMessage = "Listening on 127.0.0.1:" + PORT + "; waiting for a music tab.";
        return paused;
    }

    public String statusMessage() { return statusMessage; }

    private static boolean isBrowserExtensionOrigin(String origin) {
        return origin != null && origin.matches("chrome-extension://[a-p]{32}");
    }

    private static String toJson(MediaStatus media) {
        return "{\"available\":true,\"playing\":" + media.playing()
                + ",\"source\":\"" + escape(media.source()) + "\""
                + ",\"title\":\"" + escape(media.title()) + "\""
                + ",\"artist\":\"" + escape(media.artist()) + "\""
                + ",\"album\":\"" + escape(media.album()) + "\""
                + ",\"artworkUrl\":\"" + escape(media.artworkUrl()) + "\""
                + ",\"positionMs\":" + media.positionMs()
                + ",\"durationMs\":" + media.durationMs()
                + ",\"updatedAtEpochMs\":" + media.updatedAtEpochMs() + "}";
    }

    private static String safeArtworkUrl(String value) {
        if (value == null || value.isBlank() || value.length() > 2_048) return "";
        try {
            URI uri = URI.create(value.strip());
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                    || uri.getUserInfo() != null || uri.getPort() != -1) return "";
            String host = uri.getHost().toLowerCase(Locale.ROOT);
            boolean allowed = host.equals("i.ytimg.com")
                    || host.endsWith(".ytimg.com")
                    || host.equals("yt3.ggpht.com")
                    || host.endsWith(".googleusercontent.com")
                    || host.equals("i.scdn.co")
                    || host.endsWith(".scdn.co")
                    || host.endsWith(".spotifycdn.com");
            return allowed ? uri.toASCIIString() : "";
        } catch (IllegalArgumentException ignored) {
            return "";
        }
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\r", "\\r").replace("\n", "\\n");
    }
    private static String clean(String value) {
        if (value == null) return "";
        String clean = value.strip();
        return clean.length() > 300 ? clean.substring(0, 300) : clean;
    }
    private static String string(JsonObject json, String key) {
        try { return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : ""; }
        catch (RuntimeException ignored) { return ""; }
    }
    private static long number(JsonObject json, String key) {
        try { return json.has(key) ? json.get(key).getAsLong() : 0; }
        catch (RuntimeException ignored) { return 0; }
    }
    private static boolean bool(JsonObject json, String key) {
        try { return json.has(key) && json.get(key).getAsBoolean(); }
        catch (RuntimeException ignored) { return false; }
    }

    private static void respond(OutputStream output, int status, String phrase, String contentType, String body)
            throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        String header = "HTTP/1.1 " + status + " " + phrase + "\r\n"
                + "Content-Type: " + contentType + "; charset=utf-8\r\n"
                + "Content-Length: " + bytes.length + "\r\n"
                + "Cache-Control: no-store\r\n"
                + "Connection: close\r\n\r\n";
        output.write(header.getBytes(StandardCharsets.US_ASCII));
        output.write(bytes);
        output.flush();
    }

    @Override public void close() {
        running = false;
        try { if (serverSocket != null) serverSocket.close(); }
        catch (IOException ignored) { }
        browserSessions.clear();
    }
}
