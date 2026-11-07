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
    private SkyLyricsHud() {}

    public static void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (SkyLyricsClient.CONFIG == null || !SkyLyricsClient.CONFIG.enabled()) return;

        MediaStatus media = SkyLyricsClient.CURRENT_MEDIA;
        if (media != null && media.available()) drawNowPlaying(graphics, media);

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

    /** Compact, always-visible Now Playing card; independent of the 3D lyric mode. */
    private static void drawNowPlaying(GuiGraphicsExtractor graphics, MediaStatus media) {
        Minecraft minecraft = Minecraft.getInstance();
        int screenWidth = graphics.guiWidth();
        int panelWidth = Math.min(318, screenWidth - 24);
        if (panelWidth < 180) return;

        int panelHeight = 76;
        int x = 12;
        int y = graphics.guiHeight() - panelHeight - 12;
        int accent = SkyLyricsClient.CONFIG.accentColor();
        graphics.fill(x, y, x + panelWidth, y + panelHeight, 0xD90C0A18);
        graphics.outline(x, y, panelWidth, panelHeight, 0xFF65528C);
        graphics.fill(x, y, x + panelWidth, y + 2, accent);

        int artX = x + 9;
        int artY = y + 10;
        int artSize = 54;
        if (AlbumArtManager.hasTexture()) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, AlbumArtManager.TEXTURE_ID,
                    artX, artY, 0.0F, 0.0F, artSize, artSize, artSize, artSize);
        } else {
            graphics.fill(artX, artY, artX + artSize, artY + artSize, 0xFF241D36);
            graphics.centeredText(minecraft.font, "♪", artX + artSize / 2, artY + 19, 0xFFC6A0FF);
        }

        int textX = artX + artSize + 10;
        int maxTextWidth = panelWidth - (textX - x) - 10;
        int pulse = media.playing()
                ? 150 + (int) (55 * (0.5 + 0.5 * Math.sin(System.nanoTime() / 280_000_000.0)))
                : 125;
        graphics.fill(textX, y + 12, textX + 5, y + 17, withAlpha(0xFF7FE0B0, pulse));
        graphics.text(minecraft.font, "NOW PLAYING  ·  " + media.displaySource(),
                textX + 10, y + 10, 0xFFBDA9FF, true);
        graphics.text(minecraft.font, fit(media.title().isBlank() ? "Unknown track" : media.title(),
                minecraft, maxTextWidth, 1.0f), textX + 10, y + 26, 0xFFFFFFFF, true);
        graphics.text(minecraft.font, fit(media.artist().isBlank() ? " " : media.artist(),
                minecraft, maxTextWidth, 1.0f), textX + 10, y + 40, 0xFFCBC7D8, false);

        int progressX = textX + 10;
        int progressWidth = Math.max(30, x + panelWidth - 10 - progressX);
        int progressY = y + 61;
        graphics.fill(progressX, progressY, progressX + progressWidth, progressY + 2, 0x663F3A50);
        int progress = media.durationMs() <= 0 ? 0 : (int) (progressWidth * Math.max(0.0,
                Math.min(1.0, (double) media.positionMs() / media.durationMs())));
        graphics.fill(progressX, progressY, progressX + progress, progressY + 2, accent);

        String time = LyricsPlayer.formatTime(media.positionMs()) + " / "
                + (media.durationMs() > 0 ? LyricsPlayer.formatTime(media.durationMs()) : "--:--");
        graphics.text(minecraft.font, time, x + panelWidth - minecraft.font.width(time) - 10,
                y + 48, 0xFF9690AA, false);
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
