package com.skyrevo.skylyrics.ui;

import com.skyrevo.skylyrics.SkyLyricsClient;
import com.skyrevo.skylyrics.core.LyricsPlayer;
import com.skyrevo.skylyrics.integration.MediaStatus;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.List;

public final class SkyLyricsScreen extends Screen {
    private enum Tab { PLAYER, LIBRARY, SKIES, STYLES, SETTINGS }
    private final Tab tab;

    public SkyLyricsScreen() { this(Tab.PLAYER); }
    private SkyLyricsScreen(Tab tab) {
        super(Component.literal("SkyLyrics"));
        this.tab = tab;
    }

    @Override
    protected void init() {
        int panelLeft = width / 2 - 190;
        int top = Math.max(8, (height - 320) / 2);
        int tabY = top + 43;
        addRenderableWidget(Button.builder(Component.literal("PLAYER"), b -> open(Tab.PLAYER))
                .bounds(panelLeft + 8, tabY, 68, 20).build());
        addRenderableWidget(Button.builder(Component.literal("LIBRARY"), b -> open(Tab.LIBRARY))
                .bounds(panelLeft + 80, tabY, 68, 20).build());
        addRenderableWidget(Button.builder(Component.literal("SKIES"), b -> open(Tab.SKIES))
                .bounds(panelLeft + 152, tabY, 68, 20).build());
        addRenderableWidget(Button.builder(Component.literal("STYLES"), b -> open(Tab.STYLES))
                .bounds(panelLeft + 224, tabY, 68, 20).build());
        addRenderableWidget(Button.builder(Component.literal("SETTINGS"), b -> open(Tab.SETTINGS))
                .bounds(panelLeft + 296, tabY, 76, 20).build());

        int contentY = tabY + 44;
        int left = panelLeft + 18;
        if (tab == Tab.PLAYER) {
            if (SkyLyricsClient.PLAYER.isExternalClock()) {
                addRenderableWidget(Button.builder(Component.literal("SYNC - 250 MS"), b -> SkyLyricsClient.PLAYER.adjustOffset(-250))
                        .bounds(left + 8, contentY + 54, 105, 22).build());
                addRenderableWidget(Button.builder(Component.literal("SYNC + 250 MS"), b -> SkyLyricsClient.PLAYER.adjustOffset(250))
                        .bounds(left + 122, contentY + 54, 105, 22).build());
                addRenderableWidget(Button.builder(Component.literal("RESET OFFSET"), b -> SkyLyricsClient.PLAYER.resetOffset())
                        .bounds(left + 236, contentY + 54, 118, 22).build());
            } else {
                addRenderableWidget(Button.builder(Component.literal("PLAY / PAUSE"), b -> SkyLyricsClient.PLAYER.toggle())
                        .bounds(left, contentY + 54, 108, 22).build());
                addRenderableWidget(Button.builder(Component.literal("- 5 SEC"), b -> SkyLyricsClient.PLAYER.seekBy(-5_000))
                        .bounds(left + 116, contentY + 54, 72, 22).build());
                addRenderableWidget(Button.builder(Component.literal("+ 5 SEC"), b -> SkyLyricsClient.PLAYER.seekBy(5_000))
                        .bounds(left + 196, contentY + 54, 72, 22).build());
                addRenderableWidget(Button.builder(Component.literal("RESTART"), b -> SkyLyricsClient.PLAYER.restart())
                        .bounds(left + 276, contentY + 54, 78, 22).build());
                addRenderableWidget(Button.builder(Component.literal("OFFSET - 50 MS"), b -> SkyLyricsClient.PLAYER.adjustOffset(-50))
                        .bounds(left + 22, contentY + 88, 145, 20).build());
                addRenderableWidget(Button.builder(Component.literal("OFFSET + 50 MS"), b -> SkyLyricsClient.PLAYER.adjustOffset(50))
                        .bounds(left + 183, contentY + 88, 145, 20).build());
            }
        } else if (tab == Tab.LIBRARY) {
            addRenderableWidget(Button.builder(Component.literal("RELOAD LIBRARY"), b -> open(Tab.LIBRARY))
                    .bounds(left + 224, contentY - 4, 130, 20).build());
            List<Path> tracks = SkyLyricsClient.LIBRARY.listTracks();
            int row = 0;
            for (Path track : tracks) {
                if (row >= 5) break;
                final Path selected = track;
                String label = track.getFileName().toString();
                if (label.length() > 29) label = label.substring(0, 26) + "...";
                addRenderableWidget(Button.builder(Component.literal("LOAD  " + label), b -> {
                    SkyLyricsClient.loadTrack(selected);
                    open(Tab.PLAYER);
                }).bounds(left + 4, contentY + 24 + row * 27, 340, 21).build());
                row++;
            }
        } else if (tab == Tab.SKIES) {
            String[] themes = {"Golden Hour", "Aurora", "Violet Dream", "Ocean", "Monochrome"};
            for (int i = 0; i < themes.length; i++) {
                final int selected = i;
                addRenderableWidget(Button.builder(Component.literal(themes[i]), b -> {
                    SkyLyricsClient.CONFIG.setTheme(selected);
                    open(Tab.SKIES);
                }).bounds(left + (i % 2) * 178, contentY + 22 + (i / 2) * 30, 166, 22).build());
            }
            addRenderableWidget(Button.builder(Component.literal("TOGGLE AMBIENT BAND"), b -> SkyLyricsClient.CONFIG.toggleBackdrop())
                    .bounds(left + 8, contentY + 116, 330, 21).build());
            addRenderableWidget(Button.builder(Component.literal("BAND OPACITY  -"), b -> SkyLyricsClient.CONFIG.adjustOpacity(-12))
                    .bounds(left + 8, contentY + 145, 155, 21).build());
            addRenderableWidget(Button.builder(Component.literal("BAND OPACITY  +"), b -> SkyLyricsClient.CONFIG.adjustOpacity(12))
                    .bounds(left + 183, contentY + 145, 155, 21).build());
        } else if (tab == Tab.STYLES) {
            String[] styles = {"Aurora Glow", "Cinema Soft", "Clean Minimal", "Dream Glow", "Classic Shadow"};
            for (int i = 0; i < styles.length; i++) {
                final int selected = i;
                addRenderableWidget(Button.builder(Component.literal(styles[i]), b -> {
                    SkyLyricsClient.CONFIG.setTextStyle(selected);
                    open(Tab.STYLES);
                }).bounds(left + (i % 2) * 178, contentY + 22 + (i / 2) * 30, 166, 22).build());
            }
            addRenderableWidget(Button.builder(Component.literal("TEXT SIZE  -"), b -> SkyLyricsClient.CONFIG.adjustScale(-0.05f))
                    .bounds(left + 10, contentY + 116, 155, 21).build());
            addRenderableWidget(Button.builder(Component.literal("TEXT SIZE  +"), b -> SkyLyricsClient.CONFIG.adjustScale(0.05f))
                    .bounds(left + 183, contentY + 116, 155, 21).build());
        } else {
            addRenderableWidget(Button.builder(Component.literal("TOGGLE LYRIC DISPLAY"), b -> SkyLyricsClient.CONFIG.toggleEnabled())
                    .bounds(left + 8, contentY + 16, 330, 22).build());
            addRenderableWidget(Button.builder(Component.literal("TOGGLE PREVIOUS / NEXT LINES"), b -> SkyLyricsClient.CONFIG.toggleContextLines())
                    .bounds(left + 8, contentY + 46, 330, 22).build());
            addRenderableWidget(Button.builder(Component.literal("MOVE LYRICS UP"), b -> SkyLyricsClient.CONFIG.adjustVerticalPosition(-0.025f))
                    .bounds(left + 8, contentY + 76, 155, 22).build());
            addRenderableWidget(Button.builder(Component.literal("MOVE LYRICS DOWN"), b -> SkyLyricsClient.CONFIG.adjustVerticalPosition(0.025f))
                    .bounds(left + 183, contentY + 76, 155, 22).build());
            addRenderableWidget(Button.builder(Component.literal("BACKDROP OPACITY  -"), b -> SkyLyricsClient.CONFIG.adjustOpacity(-12))
                    .bounds(left + 8, contentY + 106, 155, 22).build());
            addRenderableWidget(Button.builder(Component.literal("BACKDROP OPACITY  +"), b -> SkyLyricsClient.CONFIG.adjustOpacity(12))
                    .bounds(left + 183, contentY + 106, 155, 22).build());
        }

        addRenderableWidget(Button.builder(Component.literal("CLOSE"), b -> onClose())
                .bounds(panelLeft + 278, top + 292, 78, 20).build());
    }

    private void open(Tab next) {
        this.minecraft.gui.setScreen(new SkyLyricsScreen(next));
    }

    @Override
    protected void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractBackground(graphics, mouseX, mouseY, delta);
        int left = width / 2 - 190;
        int top = Math.max(8, (height - 320) / 2);
        graphics.fill(left, top, left + 380, top + 320, 0xE9141423);
        graphics.outline(left, top, 380, 320, 0xFF65528C);
        graphics.fill(left, top, left + 380, top + 3, SkyLyricsClient.CONFIG.accentColor());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        int left = width / 2 - 190;
        int top = Math.max(8, (height - 320) / 2);
        graphics.text(font, "SKYLYRICS", left + 16, top + 12, 0xFFFFFFFF, true);
        graphics.text(font, "A lyric layer for your Minecraft sky", left + 16, top + 27, 0xFFB8B5C9, false);

        int contentY = top + 114;
        if (tab == Tab.PLAYER) {
            MediaStatus media = SkyLyricsClient.CURRENT_MEDIA;
            if (SkyLyricsClient.PLAYER.hasTrack()) {
                graphics.text(font, SkyLyricsClient.PLAYER.track().title(), left + 20, contentY - 19, 0xFFFFFFFF, true);
                graphics.text(font, SkyLyricsClient.PLAYER.track().artist(), left + 20, contentY - 5, 0xFFB8B5C9, false);
                graphics.text(font, (SkyLyricsClient.PLAYER.isExternalClock() ? "MEDIA SYNC" :
                                (SkyLyricsClient.PLAYER.isPlaying() ? "SYNC RUNNING" : "READY / PAUSED"))
                        + "    " + LyricsPlayer.formatTime(SkyLyricsClient.PLAYER.positionMs())
                        + " / " + LyricsPlayer.formatTime(SkyLyricsClient.PLAYER.durationMs()),
                        left + 20, contentY + 20, SkyLyricsClient.CONFIG.accentColor(), true);
            } else if (media != null && media.available()) {
                graphics.text(font, media.title().isBlank() ? "Unknown track" : media.title(),
                        left + 20, contentY - 19, 0xFFFFFFFF, true);
                graphics.text(font, media.artist() + "  ·  " + media.displaySource(),
                        left + 20, contentY - 5, 0xFFB8B5C9, false);
                String nowPlayingStatus = media.isSpotify() ? "NOW PLAYING  ·  TRACK INFO ONLY"
                        : "NOW PLAYING  " + LyricsPlayer.formatTime(media.positionMs())
                        + " / " + LyricsPlayer.formatTime(media.durationMs());
                graphics.text(font, nowPlayingStatus, left + 20, contentY + 20,
                        SkyLyricsClient.CONFIG.accentColor(), true);
            } else {
                graphics.text(font, "No active music session detected", left + 20, contentY, 0xFFFFB7A8, true);
            }
            String bridgeNotice = SkyLyricsClient.MEDIA_NOTICE == null ? "" : SkyLyricsClient.MEDIA_NOTICE;
            if (bridgeNotice.length() > 55) bridgeNotice = bridgeNotice.substring(0, 52) + "...";
            graphics.text(font, bridgeNotice, left + 20, contentY + 44, 0xFFB8B5C9, false);
            graphics.text(font, "Bridge: http://127.0.0.1:38173  |  Sync offset: "
                    + SkyLyricsClient.PLAYER.manualOffsetMs() + " ms",
                    left + 20, contentY + 118, 0xFFB8B5C9, false);
        } else if (tab == Tab.LIBRARY) {
            graphics.text(font, "LOCAL LYRICS LIBRARY", left + 18, contentY - 17, 0xFFFFFFFF, true);
            graphics.text(font, "Folder: config/skylyrics/lyrics/", left + 18, contentY + 2, 0xFFB8B5C9, false);
            graphics.text(font, "YouTube can search synced LRC automatically; local LRC is fallback.", left + 18, contentY + 15, 0xFFB8B5C9, false);
            graphics.text(font, "Showing up to 5 tracks. Add more .lrc files to the folder anytime.", left + 18, top + 258, 0xFF8F8DA2, false);
        } else if (tab == Tab.SKIES) {
            graphics.text(font, "SKY AMBIENCE", left + 18, contentY - 17, 0xFFFFFFFF, true);
            graphics.text(font, "Palette: " + SkyLyricsClient.CONFIG.themeName(), left + 18, contentY - 2,
                    SkyLyricsClient.CONFIG.accentColor(), true);
            graphics.text(font, "The ambient band is a subtle overlay behind lyric text, not a world-sky replacement.",
                    left + 18, top + 258, 0xFF8F8DA2, false);
        } else if (tab == Tab.STYLES) {
            graphics.text(font, "LYRIC TYPOGRAPHY", left + 18, contentY - 17, 0xFFFFFFFF, true);
            graphics.text(font, "Active: " + SkyLyricsClient.CONFIG.textStyleName(), left + 18, contentY - 2,
                    SkyLyricsClient.CONFIG.accentColor(), true);
            graphics.text(font, "Text scale: " + String.format("%.2fx", SkyLyricsClient.CONFIG.scale()),
                    left + 18, contentY + 96, 0xFFB8B5C9, false);
        } else {
            graphics.text(font, "DISPLAY & POSITION", left + 18, contentY - 17, 0xFFFFFFFF, true);
            graphics.text(font, "Lyrics: " + (SkyLyricsClient.CONFIG.enabled() ? "ON" : "OFF")
                    + "  | Context lines: " + (SkyLyricsClient.CONFIG.showContextLines() ? "ON" : "OFF"),
                    left + 18, contentY - 1, 0xFFB8B5C9, false);
            graphics.text(font, "Vertical position: " + Math.round(SkyLyricsClient.CONFIG.verticalPosition() * 100) + "% screen height",
                    left + 18, contentY + 14, 0xFFB8B5C9, false);
            graphics.text(font, "Local HUD; media metadata uses the loopback bridge.",
                    left + 18, top + 260, 0xFF8F8DA2, false);
        }
    }
}
