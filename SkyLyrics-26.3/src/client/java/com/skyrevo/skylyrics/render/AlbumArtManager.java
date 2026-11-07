package com.skyrevo.skylyrics.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.skyrevo.skylyrics.SkyLyricsClient;
import com.skyrevo.skylyrics.integration.MediaStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
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
    private static final int FALLBACK_COLOR = 0xFFC6A0FF;
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    private static final AtomicLong GENERATION = new AtomicLong();
    private static volatile String requestedUrl = "";
    private static volatile boolean ready;
    private static volatile int dominantColor = FALLBACK_COLOR;

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
            dominantColor = FALLBACK_COLOR;
        });
        if (url.isBlank() || !isAllowedArtworkUrl(url)) return;

        CompletableFuture.supplyAsync(() -> download(url)).whenComplete((art, error) ->
                client.execute(() -> {
                    if (generation != GENERATION.get()) {
                        if (art != null) art.image().close();
                        return;
                    }
                    if (error != null || art == null) {
                        ready = false;
                        dominantColor = FALLBACK_COLOR;
                        SkyLyricsClient.LOGGER.debug("Album art unavailable for current media session.");
                        return;
                    }
                    NativeImage image = art.image();
                    if (image.getWidth() > 1024 || image.getHeight() > 1024
                            || image.getWidth() < 16 || image.getHeight() < 16) {
                        image.close();
                        ready = false;
                        dominantColor = FALLBACK_COLOR;
                        return;
                    }
                    try {
                        DynamicTexture texture = new DynamicTexture(() -> "SkyLyrics album artwork", image);
                        client.getTextureManager().register(TEXTURE_ID, texture);
                        dominantColor = art.dominantColor();
                        ready = true;
                    } catch (RuntimeException exception) {
                        image.close();
                        ready = false;
                        dominantColor = FALLBACK_COLOR;
                        SkyLyricsClient.LOGGER.debug("Could not register album artwork texture.", exception);
                    }
                }));
    }

    public static boolean hasTexture() { return ready; }

    /** Accent sampled from the current cover art, or the default accent when no cover is loaded. */
    public static int dominantColor() { return dominantColor; }

    private static DownloadedArt download(String value) {
        try {
            URI uri = URI.create(value);
            if (!isAllowedArtworkUri(uri)) return null;
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(8))
                    .header("Accept", "image/jpeg,image/png")
                    .header("User-Agent", "SkyLyrics/0.6.0")
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

                BufferedImage sampled = ImageIO.read(new ByteArrayInputStream(bytes));
                int color = sampled == null ? FALLBACK_COLOR : sampleArtworkColor(sampled);
                NativeImage nativeImage = NativeImage.read(new ByteArrayInputStream(bytes));
                return new DownloadedArt(nativeImage, color);
            }
        } catch (Exception exception) {
            return null;
        }
    }

    private static int sampleArtworkColor(BufferedImage image) {
        int stepX = Math.max(1, image.getWidth() / 32);
        int stepY = Math.max(1, image.getHeight() / 32);
        double red = 0.0, green = 0.0, blue = 0.0, weightTotal = 0.0;
        for (int y = 0; y < image.getHeight(); y += stepY) {
            for (int x = 0; x < image.getWidth(); x += stepX) {
                int argb = image.getRGB(x, y);
                int alpha = (argb >>> 24) & 0xFF;
                if (alpha < 128) continue;
                int r = (argb >>> 16) & 0xFF;
                int g = (argb >>> 8) & 0xFF;
                int b = argb & 0xFF;
                float[] hsb = Color.RGBtoHSB(r, g, b, null);
                double weight = (0.30 + hsb[1]) * (0.45 + 0.55 * hsb[2]);
                red += r * weight;
                green += g * weight;
                blue += b * weight;
                weightTotal += weight;
            }
        }
        if (weightTotal <= 0.0) return FALLBACK_COLOR;
        int r = clamp((int) Math.round(red / weightTotal));
        int g = clamp((int) Math.round(green / weightTotal));
        int b = clamp((int) Math.round(blue / weightTotal));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
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

    private static int clamp(int value) { return Math.max(0, Math.min(255, value)); }

    private record DownloadedArt(NativeImage image, int dominantColor) {}
}
