package com.skyrevo.skylyrics.render;

import com.skyrevo.skylyrics.SkyLyricsClient;
import com.skyrevo.skylyrics.core.LyricsCue;
import com.skyrevo.skylyrics.core.LyricsPlayer;
import com.skyrevo.skylyrics.integration.MediaStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.util.FormattedCharSequence;

public final class SkyLyricsHud {
    private static final float[] WAVE_HEIGHTS = {0.45f, 0.75f, 1.0f, 0.68f, 0.50f};

    private SkyLyricsHud() {}

    public static void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (SkyLyricsClient.CONFIG == null || !SkyLyricsClient.CONFIG.enabled()) return;

        MediaStatus media = SkyLyricsClient.CURRENT_MEDIA;
        if (media != null && media.available()) {
            drawNowPlaying(graphics, media);
        } else if (SkyLyricsClient.isDemoActive() && SkyLyricsClient.PLAYER.hasTrack()) {
            drawDemoNowPlaying(graphics);
        } else if (SkyLyricsClient.AUDIO.hasFile()) {
            drawOfflineNowPlaying(graphics);
        }

        LyricsPlayer player = SkyLyricsClient.PLAYER;
        if (!player.hasTrack() || SkyLyricsClient.CONFIG.worldLyrics()) return;

        int index = player.currentCueIndex();
        LyricsCue current = player.cueAt(index);
        if (current == null || current.text().isBlank()) return;

        Minecraft minecraft = Minecraft.getInstance();
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        int centerX = width / 2;
        int accent = SkyLyricsClient.CONFIG.accentColor();
        int secondary = SkyLyricsClient.CONFIG.secondaryColor();
        int mainY = Math.round(height * SkyLyricsClient.CONFIG.verticalPosition());
        float scale = SkyLyricsClient.CONFIG.scale();
        int style = SkyLyricsClient.CONFIG.textStyle();
        int mainColor = switch (style) {
            case 1 -> 0xFFFFF0DE;
            case 2 -> 0xFFFFFFFF;
            case 3 -> secondary;
            case 4 -> 0xFFFFE6C1;
            case 5 -> 0xFFFFD58A;
            default -> accent;
        };

        int panelAlpha = SkyLyricsClient.CONFIG.showBackdrop() ? SkyLyricsClient.CONFIG.opacity() : 0;
        if (panelAlpha > 0) {
            int panelTop = mainY - Math.round(30 * scale);
            int panelBottom = mainY + Math.round(46 * scale);
            int panelLeft = Math.max(10, width / 2 - Math.min(width / 2 - 12, 250));
            int panelRight = Math.min(width - 10, width / 2 + Math.min(width / 2 - 12, 250));
            graphics.fill(panelLeft, panelTop, panelRight, panelBottom, argb(panelAlpha, 16, 13, 29));
            graphics.fill(panelLeft, panelTop, panelRight, panelTop + 2,
                    withAlpha(secondary, Math.min(180, panelAlpha + 55)));
        }

        FormattedCharSequence currentText = fit(current.text(), minecraft, width, scale);
        FormattedCharSequence previousText = null;
        FormattedCharSequence nextText = null;
        if (SkyLyricsClient.CONFIG.showContextLines()) {
            LyricsCue previous = player.cueAt(index - 1);
            LyricsCue next = player.cueAt(index + 1);
            if (previous != null) previousText = fit(previous.text(), minecraft, width, scale);
            if (next != null) nextText = fit(next.text(), minecraft, width, scale);
        }

        graphics.pose().pushMatrix();
        graphics.pose().translate(centerX, mainY);
        graphics.pose().scale(scale, scale);
        if (SkyLyricsClient.CONFIG.glowEnabled()) {
            int glow = withAlpha(accent, 72);
            graphics.centeredText(minecraft.font, currentText, 0, -1, glow);
            graphics.centeredText(minecraft.font, currentText, 0, 1, glow);
            graphics.centeredText(minecraft.font, currentText, -1, 0, glow);
            graphics.centeredText(minecraft.font, currentText, 1, 0, glow);
        }
        graphics.centeredText(minecraft.font, currentText, 0, 0, mainColor);

        graphics.pose().pushMatrix();
        graphics.pose().scale(0.84f, 0.84f);
        if (previousText != null) {
            graphics.centeredText(minecraft.font, previousText, 0, -27, withAlpha(0xFFE5E9F4, 155));
        }
        if (nextText != null) {
            graphics.centeredText(minecraft.font, nextText, 0, 27, withAlpha(0xFFD8DDE9, 135));
        }
        String trackTitle = player.track().title() + "  ·  " + player.track().artist();
        if (trackTitle.length() > 58) trackTitle = trackTitle.substring(0, 55) + "...";
        graphics.centeredText(minecraft.font, SkyLyricsTypography.ordered(trackTitle), 0, -50,
                withAlpha(0xFFFFFFFF, 185));
        graphics.pose().popMatrix();
        graphics.pose().popMatrix();

        int barWidth = Math.min(220, Math.max(110, width / 4));
        int barX = centerX - barWidth / 2;
        int barY = mainY + Math.round(43 * scale);
        graphics.fill(barX, barY, barX + barWidth, barY + 2, argb(90, 255, 255, 255));
        int progress = player.durationMs() <= 0 ? 0
                : (int) (barWidth * (double) player.positionMs() / player.durationMs());
        graphics.fill(barX, barY, barX + Math.min(barWidth, progress), barY + 2, accent);
        String status = (player.isPlaying() ? "SYNC  " : "PAUSED  ")
                + LyricsPlayer.formatTime(player.positionMs()) + " / "
                + LyricsPlayer.formatTime(player.durationMs());
        graphics.centeredText(minecraft.font, status, centerX, barY + 5, withAlpha(0xFFE5E5EF, 170));
    }

    /** Compact top-centre music pill; independent of the 3D lyric mode. */
    private static void drawNowPlaying(GuiGraphicsExtractor graphics, MediaStatus media) {
        drawPlayerPill(
                graphics,
                media.displaySource(),
                media.title(),
                media.artist(),
                media.positionMs(),
                media.durationMs(),
                media.playing(),
                AlbumArtManager.hasTexture()
        );
    }

    private static void drawOfflineNowPlaying(GuiGraphicsExtractor graphics) {
        var file = SkyLyricsClient.AUDIO.currentFile();
        String title = file == null ? "Offline track" : file.getFileName().toString();
        drawPlayerPill(
                graphics,
                "OFFLINE LIBRARY",
                title,
                SkyLyricsClient.AUDIO.status(),
                SkyLyricsClient.AUDIO.positionMs(),
                SkyLyricsClient.AUDIO.durationMs(),
                SkyLyricsClient.AUDIO.isPlaying(),
                false
        );
    }

    private static void drawDemoNowPlaying(GuiGraphicsExtractor graphics) {
        LyricsPlayer player = SkyLyricsClient.PLAYER;
        drawPlayerPill(
                graphics,
                "SKYLYRICS DEMO",
                player.track().title(),
                player.track().artist(),
                player.positionMs(),
                player.durationMs(),
                player.isPlaying(),
                false
        );
    }

    private static void drawPlayerPill(
            GuiGraphicsExtractor graphics,
            String source,
            String title,
            String artist,
            long positionMs,
            long durationMs,
            boolean playing,
            boolean hasArtwork
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        int screenWidth = graphics.guiWidth();
        int panelWidth = Math.min(380, screenWidth - 24);
        if (panelWidth < 220) return;

        int panelHeight = 58;
        int x = (screenWidth - panelWidth) / 2;
        int y = 12;
        int accent = hasArtwork ? AlbumArtManager.dominantColor() : SkyLyricsClient.CONFIG.accentColor();

        // Translucent compact panel with a restrained top accent, not a large opaque HUD card.
        graphics.fill(x, y, x + panelWidth, y + panelHeight, 0xD60B0A13);
        graphics.outline(x, y, panelWidth, panelHeight, 0xAA75618F);
        graphics.fill(x + 2, y + 2, x + panelWidth - 2, y + 3, withAlpha(accent, 190));

        int artX = x + 9;
        int artY = y + 10;
        int artSize = 36;
        if (hasArtwork) {
            if (SkyLyricsClient.CONFIG.glowEnabled()) {
                int glowAlpha = 22 + (int) (26 * (0.5 + 0.5 *
                        Math.sin(System.nanoTime() / 310_000_000.0)));
                graphics.fill(artX - 4, artY - 4, artX + artSize + 4, artY + artSize + 4,
                        withAlpha(accent, glowAlpha));
                graphics.outline(artX - 2, artY - 2, artSize + 4, artSize + 4,
                        withAlpha(accent, Math.min(180, glowAlpha * 3)));
            }
            graphics.blit(RenderPipelines.GUI_TEXTURED, AlbumArtManager.TEXTURE_ID,
                    artX, artY, 0.0F, 0.0F, artSize, artSize, artSize, artSize);
        } else {
            graphics.fill(artX, artY, artX + artSize, artY + artSize, 0xFF211A31);
            graphics.centeredText(minecraft.font, "♪", artX + artSize / 2, artY + 12, 0xFFE4C8FF);
        }

        int textX = artX + artSize + 10;
        int waveWidth = 32;
        int rightInset = 10;
        int maxTextWidth = Math.max(80, panelWidth - (textX - x) - waveWidth - rightInset - 10);
        int pulse = playing
                ? 150 + (int) (55 * (0.5 + 0.5 * Math.sin(System.nanoTime() / 280_000_000.0)))
                : 120;

        graphics.fill(textX, y + 11, textX + 5, y + 16, withAlpha(0xFF8BE2B5, pulse));
        String sourceLabel = source == null || source.isBlank() ? "MUSIC" : source;
        if (sourceLabel.length() > 18) sourceLabel = sourceLabel.substring(0, 18);
        graphics.text(minecraft.font, "NOW PLAYING  ·  " + sourceLabel,
                textX + 10, y + 8, 0xFFCBB8F4, true);
        graphics.text(minecraft.font, fit(title == null || title.isBlank() ? "Unknown track" : title,
                minecraft, maxTextWidth, 1.0f), textX + 10, y + 22, 0xFFFFFCF7, true);
        graphics.text(minecraft.font, fit(artist == null ? "" : artist,
                minecraft, maxTextWidth, 1.0f), textX + 10, y + 37, 0xFFC9C4D5, false);

        // A small animated waveform echoes the music instead of adding another large panel.
        int waveX = x + panelWidth - 27;
        int waveCenter = y + 28;
        boolean localAudio = "OFFLINE LIBRARY".equals(source);
        // Offline bars react to measured PCM amplitude. Browser mode remains a low-intensity
        // transport animation because the extension intentionally never captures audio bytes.
        float audioEnergy = localAudio ? SkyLyricsClient.AUDIO.visualizerLevel() : (playing ? 0.22f : 0.0f);
        for (int i = 0; i < 5; i++) {
            float seed = WAVE_HEIGHTS[i];
            float rhythm = (float) (0.55 + 0.45 *
                    (0.5 + 0.5 * Math.sin(System.nanoTime() / 165_000_000.0 + i * 1.4)));
            int barHeight = playing
                    ? 2 + Math.round((3.0f + audioEnergy * 18.0f) * seed * rhythm)
                    : 3;
            int bx = waveX + i * 4;
            graphics.fill(bx, waveCenter - barHeight / 2, bx + 2, waveCenter + (barHeight + 1) / 2,
                    withAlpha(accent, playing ? 225 : 120));
        }

        int progressX = x + 9;
        int progressWidth = panelWidth - 18;
        int progressY = y + panelHeight - 4;
        graphics.fill(progressX, progressY, progressX + progressWidth, progressY + 2, 0x66423A50);
        int progress = durationMs <= 0 ? 0 : (int) (progressWidth * Math.max(0.0,
                Math.min(1.0, (double) positionMs / durationMs)));
        graphics.fill(progressX, progressY, progressX + progress, progressY + 2, accent);
    }

    private static FormattedCharSequence fit(String value, Minecraft minecraft, int screenWidth, float scale) {
        int maxWidth = Math.max(80, (int) (screenWidth * 0.84f / Math.max(0.75f, scale)));
        String clipped = value == null ? "" : value;
        FormattedCharSequence sequence = SkyLyricsTypography.ordered(clipped);
        while (!clipped.isEmpty() && minecraft.font.width(sequence) > maxWidth) {
            clipped = clipped.substring(0, clipped.length() - 1);
            sequence = SkyLyricsTypography.ordered(clipped + "…");
        }
        return sequence;
    }

    private static int argb(int alpha, int red, int green, int blue) {
        return (clamp(alpha) << 24) | (clamp(red) << 16) | (clamp(green) << 8) | clamp(blue);
    }

    private static int withAlpha(int color, int alpha) {
        return (clamp(alpha) << 24) | (color & 0x00FFFFFF);
    }

    private static int clamp(int value) { return Math.max(0, Math.min(255, value)); }
}
