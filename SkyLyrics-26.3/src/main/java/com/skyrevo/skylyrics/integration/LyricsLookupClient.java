package com.skyrevo.skylyrics.integration;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.skyrevo.skylyrics.core.LyricsTrack;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;

/** Optional synced-LRC lookup. Called only for a detected YouTube or YouTube Music browser session. */
public final class LyricsLookupClient {
    private long lastRequestStartedAtMs;
    private long retryNotBeforeMs;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4))
            .build();

    public synchronized LyricsTrack lookup(MediaStatus media) throws IOException, InterruptedException {
        if (media == null || !media.isYouTubeSource()) {
            throw new IOException("Pencarian lirik otomatis hanya diaktifkan untuk sesi YouTube / YouTube Music.");
        }
        if (media.title().isBlank() || media.artist().isBlank()) {
            throw new IOException("Judul atau artis belum tersedia dari pemutar musik.");
        }

        JsonObject match = null;
        long durationSeconds = Math.round(media.durationMs() / 1000.0);
        if (durationSeconds >= 1 && durationSeconds <= 3600) {
            String query = "track_name=" + encode(media.title())
                    + "&artist_name=" + encode(media.artist())
                    + (media.album().isBlank() ? "" : "&album_name=" + encode(media.album()))
                    + "&duration=" + durationSeconds;
            HttpResponse<String> exact = get("https://lrclib.net/api/get?" + query);
            if (exact.statusCode() == 200) match = JsonParser.parseString(exact.body()).getAsJsonObject();
            else if (exact.statusCode() != 404) throw providerError(exact);
        }

        // Fallback to the search endpoint and accept only an exact title + artist match.
        if (match == null) {
            String q = encode(media.title() + " " + media.artist());
            HttpResponse<String> search = get("https://lrclib.net/api/search?q=" + q);
            if (search.statusCode() == 429) throw providerError(search);
            if (search.statusCode() != 200) throw providerError(search);
            JsonArray results = JsonParser.parseString(search.body()).getAsJsonArray();
            for (JsonElement element : results) {
                if (!element.isJsonObject()) continue;
                JsonObject candidate = element.getAsJsonObject();
                String candidateTitle = value(candidate, "trackName");
                String candidateArtist = value(candidate, "artistName");
                long candidateDuration = number(candidate, "duration");
                boolean durationMatches = durationSeconds < 1 || candidateDuration < 1
                        || Math.abs(candidateDuration - durationSeconds) <= 2;
                if (normalize(candidateTitle).equals(normalize(media.title()))
                        && normalize(candidateArtist).equals(normalize(media.artist()))
                        && durationMatches) {
                    match = candidate;
                    break;
                }
            }
        }

        if (match == null) {
            throw new IOException("Lirik bertimestamp untuk lagu ini belum ditemukan. Kamu bisa memasukkan file .lrc sendiri.");
        }
        String synced = value(match, "syncedLyrics");
        if (synced.isBlank()) {
            throw new IOException("Lagu ditemukan, tetapi lirik tersinkronisasi tidak tersedia.");
        }
        return LyricsTrack.fromLrcText(synced, media.title(), media.artist());
    }

    private HttpResponse<String> get(String url) throws IOException, InterruptedException {
        throttleRequests();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .header("Accept", "application/json")
                .header("User-Agent", "SkyLyrics/0.2.0 (Minecraft client mod)")
                .GET().build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private void throttleRequests() throws InterruptedException {
        long now = System.currentTimeMillis();
        long nextAllowed = Math.max(retryNotBeforeMs, lastRequestStartedAtMs + 350);
        if (lastRequestStartedAtMs == 0) nextAllowed = Math.max(retryNotBeforeMs, now);
        long waitMs = nextAllowed - now;
        if (waitMs > 0) Thread.sleep(waitMs);
        lastRequestStartedAtMs = System.currentTimeMillis();
    }

    private IOException providerError(HttpResponse<?> response) {
        if (response.statusCode() == 429) {
            long retrySeconds = response.headers().firstValue("Retry-After")
                    .flatMap(value -> {
                        try { return java.util.Optional.of(Long.parseLong(value.trim())); }
                        catch (NumberFormatException ignored) { return java.util.Optional.empty(); }
                    }).orElse(5L);
            retrySeconds = Math.max(1, Math.min(300, retrySeconds));
            retryNotBeforeMs = Math.max(retryNotBeforeMs, System.currentTimeMillis() + retrySeconds * 1000L);
            return new IOException("LRCLIB membatasi permintaan. Coba lagi dalam " + retrySeconds + " detik.");
        }
        return new IOException("Penyedia lirik merespons HTTP " + response.statusCode() + ".");
    }
    private static String value(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : "";
    }
    private static long number(JsonObject json, String key) {
        try { return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsLong() : 0; }
        catch (RuntimeException ignored) { return 0; }
    }
    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]", "").strip();
    }
    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
