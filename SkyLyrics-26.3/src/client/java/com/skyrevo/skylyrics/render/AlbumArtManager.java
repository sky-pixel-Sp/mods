package com.skyrevo.skylyrics.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.skyrevo.skylyrics.SkyLyricsClient;
import com.skyrevo.skylyrics.integration.MediaStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/** Downloads only allow-listed cover images; never requests audio or playback controls. */
public final class AlbumArtManager {
    public static final Identifier TEXTURE_ID =
            Identifier.fromNamespaceAndPath(SkyLyricsClient.MOD_ID, "dynamic/album_art");
    private static final int MAX_IMAGE_BYTES = 1_500_000;
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    private static final AtomicLong GENERATION = new AtomicLong();
    private static volatile String requestedUrl = "";
    private static volatile boolean ready;

    private AlbumArtManager() {}

    public static void update(MediaStatus media) {
        String url = media == null ? "" : media.artworkUrl();
        if (url.equals(requestedUrl)) return;
        requestedUrl = url;
        long generation = GENERATION.incrementAndGet();
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> {
            if (generation != GENERATION.get()) return;
            if (ready) client.getTextureManager().release(TEXTURE_ID);
            ready = false;
        });
        if (url.isBlank() || !isAllowedArtworkUrl(url)) return;

        CompletableFuture.supplyAsync(() -> download(url)).whenComplete((image, error) ->
                client.execute(() -> {
                    if (generation != GENERATION.get()) {
                        if (image != null) image.close();
                        return;
                    }
                    if (error != null || image == null) {
                        ready = false;
                        SkyLyricsClient.LOGGER.debug("Album art unavailable for current media session.");
                        return;
                    }
                    if (image.getWidth() > 1024 || image.getHeight() > 1024
                            || image.getWidth() < 16 || image.getHeight() < 16) {
                        image.close();
                        ready = false;
                        return;
                    }
                    try {
                        DynamicTexture texture = new DynamicTexture(() -> "SkyLyrics album artwork", image);
                        client.getTextureManager().register(TEXTURE_ID, texture);
                        ready = true;
                    } catch (RuntimeException exception) {
                        image.close();
                        ready = false;
                        SkyLyricsClient.LOGGER.debug("Could not register album artwork texture.", exception);
                    }
                }));
    }

    public static boolean hasTexture() { return ready; }

    private static NativeImage download(String value) {
        try {
            URI uri = URI.create(value);
            if (!isAllowedArtworkUri(uri)) return null;
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(8))
                    .header("Accept", "image/jpeg,image/png")
                    .header("User-Agent", "SkyLyrics/0.5.0")
                    .GET()
                    .build();
            HttpResponse<InputStream> response = HTTP.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream stream = response.body()) {
                if (response.statusCode() != 200) return null;
                String contentType = response.headers().firstValue("Content-Type").orElse("")
                        .toLowerCase(Locale.ROOT);
                if (!contentType.startsWith("image/jpeg") && !contentType.startsWith("image/png")) return null;
                long declaredLength = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
                if (declaredLength > MAX_IMAGE_BYTES) return null;
                byte[] bytes = stream.readNBytes(MAX_IMAGE_BYTES + 1);
                if (bytes.length == 0 || bytes.length > MAX_IMAGE_BYTES) return null;
                return NativeImage.read(new ByteArrayInputStream(bytes));
            }
        } catch (Exception exception) {
            return null;
        }
    }

    private static boolean isAllowedArtworkUrl(String value) {
        try { return isAllowedArtworkUri(URI.create(value)); }
        catch (IllegalArgumentException ignored) { return false; }
    }

    private static boolean isAllowedArtworkUri(URI uri) {
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                || uri.getUserInfo() != null || uri.getPort() != -1) return false;
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        return host.equals("i.ytimg.com")
                || host.endsWith(".ytimg.com")
                || host.equals("yt3.ggpht.com")
                || host.endsWith(".googleusercontent.com")
                || host.equals("i.scdn.co")
                || host.endsWith(".scdn.co")
                || host.endsWith(".spotifycdn.com");
    }
}
