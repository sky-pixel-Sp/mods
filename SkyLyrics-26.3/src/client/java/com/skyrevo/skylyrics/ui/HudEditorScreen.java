package com.skyrevo.skylyrics.ui;

import com.skyrevo.skylyrics.SkyLyricsClient;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Simple drag-and-drop layout editor for the two independent SkyLyrics HUD
 * elements. Coordinates are normalized so layouts survive resolution changes.
 */
public final class HudEditorScreen extends Screen {
    private enum Element { NONE, NOW_PLAYING, LYRICS }

    private final Screen parent;
    private Element dragging = Element.NONE;
    private float originX;
    private float originY;

    public HudEditorScreen(Screen parent) {
        super(Component.literal("SkyLyrics HUD Layout Studio"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int buttonY = height - 34;
        addRenderableWidget(Button.builder(Component.literal("RESET ALL"), button -> {
            SkyLyricsClient.CONFIG.resetHudPositions();
            dragging = Element.NONE;
        }).bounds(width / 2 - 118, buttonY, 108, 22).build());
        addRenderableWidget(Button.builder(Component.literal("DONE"), button -> onClose())
                .bounds(width / 2 + 10, buttonY, 108, 22).build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractBackground(graphics, mouseX, mouseY, delta);
        graphics.fill(0, 0, width, height, 0xE9080712);

        for (int x = 0; x < width; x += 32) {
            graphics.fill(x, 0, x + 1, height, 0x182F2947);
        }
        for (int y = 0; y < height; y += 32) {
            graphics.fill(0, y, width, y + 1, 0x182F2947);
        }

        drawNowPlayingPreview(graphics);
        drawLyricsPreview(graphics);

        graphics.fill(0, height - 48, width, height, 0xD90A0913);
        graphics.fill(0, height - 48, width, height - 47, 0xFF59436F);
    }

    private void drawNowPlayingPreview(GuiGraphicsExtractor graphics) {
        int panelWidth = Math.min(380, width - 24);
        int panelHeight = 58;
        int center = Math.round(width * SkyLyricsClient.CONFIG.nowPlayingHudX());
        int x = Math.max(4, Math.min(width - panelWidth - 4, center - panelWidth / 2));
        int y = Math.round(height * SkyLyricsClient.CONFIG.nowPlayingHudY());
        y = Math.max(4, Math.min(height - panelHeight - 52, y));

        graphics.fill(x, y, x + panelWidth, y + panelHeight, 0xE00D0B18);
        graphics.outline(x, y, panelWidth, panelHeight,
                dragging == Element.NOW_PLAYING ? 0xFFFFD58A : 0xFF8067A7);
        graphics.fill(x + 2, y + 2, x + panelWidth - 2, y + 4, 0xFFB58BFF);
        graphics.fill(x + 10, y + 11, x + 46, y + 47, 0xFF29203D);
        graphics.centeredText(font, "♪", x + 28, y + 23, 0xFFFFD9FC);
        graphics.text(font, "NOW PLAYING  ·  SKYLYRICS", x + 56, y + 10, 0xFFCDBAF7, true);
        String title = SkyLyricsClient.PLAYER.hasTrack()
                ? SkyLyricsClient.PLAYER.track().title() : "Drag this card to move it";
        graphics.text(font, title, x + 56, y + 24, 0xFFFFFCF7, true);
        graphics.text(font, "Player / artwork / waveform", x + 56, y + 39, 0xFFBDB7CD, false);
    }

    private void drawLyricsPreview(GuiGraphicsExtractor graphics) {
        int panelWidth = Math.min(520, width - 32);
        int center = Math.round(width * SkyLyricsClient.CONFIG.lyricsHudX());
        int x = Math.max(4, Math.min(width - panelWidth - 4, center - panelWidth / 2));
        int mainY = Math.round(height * SkyLyricsClient.CONFIG.verticalPosition());
        int top = Math.max(4, Math.min(height - 86, mainY - 30));
        int bottom = Math.min(height - 50, top + 76);

        graphics.fill(x, top, x + panelWidth, bottom, 0xA9161226);
        graphics.outline(x, top, panelWidth, bottom - top,
                dragging == Element.LYRICS ? 0xFFFFD58A : 0xFF8067A7);
        graphics.fill(x, top, x + panelWidth, top + 2, 0xFFEA8DDA);

        String lyric = SkyLyricsClient.PLAYER.currentCue() == null
                ? "Drag this lyric panel to choose its position"
                : SkyLyricsClient.PLAYER.currentCue().text();
        if (lyric.length() > 64) lyric = lyric.substring(0, 61) + "...";
        graphics.centeredText(font, lyric, center, top + 38, SkyLyricsClient.CONFIG.accentColor());
        graphics.centeredText(font, "LYRICS HUD  ·  KARAOKE / CONTEXT LINES",
                center, top + 13, 0xFFCAC1DA);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.text(font, "HUD LAYOUT STUDIO", 18, 16, 0xFFFFFFFF, true);
        graphics.text(font, "Drag a panel with the left mouse button. Double-click a panel to reset it.",
                18, 34, 0xFFD4D1E2, false);
        graphics.text(font, "Right-click while dragging to cancel. Positions are saved when you release.",
                18, 49, 0xFFBDB7CD, false);

        String selected = switch (dragging) {
            case NOW_PLAYING -> "MOVING  ·  NOW PLAYING";
            case LYRICS -> "MOVING  ·  LYRICS HUD";
            default -> "READY  ·  DRAG EITHER PANEL";
        };
        graphics.text(font, selected, 18, height - 63,
                dragging == Element.NONE ? 0xFF9BE7BF : 0xFFFFD58A, true);
        graphics.text(font, "Now Playing X " + Math.round(SkyLyricsClient.CONFIG.nowPlayingHudX() * 100)
                + "%  Y " + Math.round(SkyLyricsClient.CONFIG.nowPlayingHudY() * 100) + "%",
                width - 190, height - 63, 0xFFD4D1E2, false);
        graphics.text(font, "Lyrics X " + Math.round(SkyLyricsClient.CONFIG.lyricsHudX() * 100)
                + "%  Y " + Math.round(SkyLyricsClient.CONFIG.verticalPosition() * 100) + "%",
                width - 190, height - 49, 0xFFD4D1E2, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 1 && dragging != Element.NONE) {
            restoreDragOrigin();
            dragging = Element.NONE;
            return true;
        }
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);

        Element target = hitTest(event.x(), event.y());
        if (target == Element.NONE) return super.mouseClicked(event, doubleClick);

        if (doubleClick) {
            resetElement(target);
            return true;
        }

        dragging = target;
        if (target == Element.NOW_PLAYING) {
            originX = SkyLyricsClient.CONFIG.nowPlayingHudX();
            originY = SkyLyricsClient.CONFIG.nowPlayingHudY();
        } else {
            originX = SkyLyricsClient.CONFIG.lyricsHudX();
            originY = SkyLyricsClient.CONFIG.verticalPosition();
        }
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (dragging == Element.NONE || event.button() != 0) {
            return super.mouseDragged(event, deltaX, deltaY);
        }

        if (dragging == Element.NOW_PLAYING) {
            float cardWidth = Math.min(380, width - 24);
            float minX = cardWidth / (2.0f * Math.max(1, width));
            float nextX = clamp(SkyLyricsClient.CONFIG.nowPlayingHudX() + (float) deltaX / Math.max(1, width),
                    minX, 1.0f - minX);
            float nextY = clamp(SkyLyricsClient.CONFIG.nowPlayingHudY() + (float) deltaY / Math.max(1, height),
                    0.0f, Math.max(0.0f, 1.0f - 58.0f / Math.max(1, height)));
            SkyLyricsClient.CONFIG.previewNowPlayingHudPosition(nextX, nextY);
        } else {
            float panelWidth = Math.min(520, width - 32);
            float minX = panelWidth / (2.0f * Math.max(1, width));
            float nextX = clamp(SkyLyricsClient.CONFIG.lyricsHudX() + (float) deltaX / Math.max(1, width),
                    minX, 1.0f - minX);
            float minY = 30.0f / Math.max(1, height);
            float maxY = 1.0f - 46.0f / Math.max(1, height);
            float nextY = clamp(SkyLyricsClient.CONFIG.verticalPosition() + (float) deltaY / Math.max(1, height),
                    minY, maxY);
            SkyLyricsClient.CONFIG.previewLyricsHudPosition(nextX, nextY);
        }
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging != Element.NONE) {
            SkyLyricsClient.CONFIG.saveHudPositions();
            dragging = Element.NONE;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isEscape()) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    private Element hitTest(double mouseX, double mouseY) {
        int nowWidth = Math.min(380, width - 24);
        int nowHeight = 58;
        int nowCenter = Math.round(width * SkyLyricsClient.CONFIG.nowPlayingHudX());
        int nowX = Math.max(4, Math.min(width - nowWidth - 4, nowCenter - nowWidth / 2));
        int nowY = Math.max(4, Math.min(height - nowHeight - 52,
                Math.round(height * SkyLyricsClient.CONFIG.nowPlayingHudY())));
        if (inside(mouseX, mouseY, nowX, nowY, nowWidth, nowHeight)) return Element.NOW_PLAYING;

        int lyricWidth = Math.min(520, width - 32);
        int lyricCenter = Math.round(width * SkyLyricsClient.CONFIG.lyricsHudX());
        int lyricX = Math.max(4, Math.min(width - lyricWidth - 4, lyricCenter - lyricWidth / 2));
        int mainY = Math.round(height * SkyLyricsClient.CONFIG.verticalPosition());
        int lyricY = Math.max(4, Math.min(height - 86, mainY - 30));
        if (inside(mouseX, mouseY, lyricX, lyricY, lyricWidth, 76)) return Element.LYRICS;
        return Element.NONE;
    }

    private void resetElement(Element element) {
        if (element == Element.NOW_PLAYING) {
            SkyLyricsClient.CONFIG.setNowPlayingHudPosition(0.5f, 0.025f);
        } else if (element == Element.LYRICS) {
            SkyLyricsClient.CONFIG.setLyricsHudPosition(0.5f, 0.38f);
        }
    }

    private void restoreDragOrigin() {
        if (dragging == Element.NOW_PLAYING) {
            SkyLyricsClient.CONFIG.previewNowPlayingHudPosition(originX, originY);
        } else if (dragging == Element.LYRICS) {
            SkyLyricsClient.CONFIG.previewLyricsHudPosition(originX, originY);
        }
        SkyLyricsClient.CONFIG.saveHudPositions();
    }

    private static boolean inside(double x, double y, int left, int top, int width, int height) {
        return x >= left && x <= left + width && y >= top && y <= top + height;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public void onClose() {
        SkyLyricsClient.CONFIG.saveHudPositions();
        if (minecraft != null) minecraft.gui.setScreen(parent);
    }
}
