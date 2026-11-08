package com.skyrevo.skylyrics.ui;

import com.skyrevo.skylyrics.SkyLyricsClient;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import com.skyrevo.skylyrics.core.LyricsCue;
import com.skyrevo.skylyrics.core.MusicLibraryState;
import com.skyrevo.skylyrics.core.LyricsPlayer;
import com.skyrevo.skylyrics.core.SkyPresetCatalog;
import com.skyrevo.skylyrics.core.SkyFontCatalog;
import com.skyrevo.skylyrics.render.SkyEffectController;
import com.skyrevo.skylyrics.render.SkyLyricsWorldText;
import com.skyrevo.skylyrics.render.SkyLyricsTypography;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;

public final class SkyLyricsScreen extends Screen {
    private enum Tab { OVERVIEW, MUSIC, LIBRARY, HISTORY, LYRICS, STYLES, SKIES, HUD, KEYBINDS, PERFORMANCE, SETTINGS, GUIDE }
    private static boolean firstOpen = true;
    private final Tab tab;
    private int left, top, panelWidth, panelHeight, sideWidth, contentLeft, contentWidth;
    private static int libraryPage;
    private static String librarySearchQuery = "";
    private EditBox librarySearchBox;
    private EditBox playlistNameBox;
    private KeyMapping pendingKeyBinding;
    private Button pendingKeyBindingButton;
    private int navStep = 34;
    private int navHeight = 27;

    public SkyLyricsScreen() { this(Tab.MUSIC); }
    private SkyLyricsScreen(Tab tab) {
        super(Component.literal(ui("SkyLyrics by Sky")));
        this.tab = tab;
    }

    @Override
    protected void init() {
        if (firstOpen) {
            SkyEffectController.refreshFromConfig();
            firstOpen = false;
        }
        panelWidth = Math.min(800, width - 16);
        panelHeight = Math.min(500, height - 16);
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        sideWidth = Math.min(170, panelWidth / 4);
        contentLeft = left + sideWidth + 18;
        contentWidth = panelWidth - sideWidth - 34;
        navStep = Math.max(16, Math.min(34, (panelHeight - 78) / Tab.values().length));
        navHeight = Math.max(13, Math.min(27, navStep - 4));

        String[] labels = {"OVERVIEW","MUSIC CENTER","OFFLINE LIBRARY","HISTORY & STATS","LYRICS","STYLES","SKIES","HUD","KEYBINDS","PERFORMANCE","SETTINGS","QUICK GUIDE"};
        Tab[] tabs = Tab.values();
        for (int i=0;i<tabs.length;i++) {
            Tab next = tabs[i];
            Button item = addRenderableWidget(Button.builder(Component.literal(ui(labels[i])), b -> open(next))
                    .bounds(left+10, top+70+i*navStep, sideWidth-20, navHeight).build());
            item.active = tab != next;
        }
        addRenderableWidget(Button.builder(Component.literal("×"), b -> onClose())
                .bounds(left+panelWidth-34, top+12, 22, 22).build());

        int y = top + 112;
        switch (tab) {
            case OVERVIEW -> overview(y);
            case MUSIC -> music(y);
            case LIBRARY -> library(y);
            case HISTORY -> history(y);
            case LYRICS -> lyrics(y);
            case STYLES -> styles(y);
            case SKIES -> skies(y);
            case HUD -> hud(y);
            case KEYBINDS -> keybinds(y);
            case PERFORMANCE -> performance(y);
            case SETTINGS -> settings(y);
            case GUIDE -> guide(y);
        }
    }

    private void overview(int y) {
        int gap=8, half=(contentWidth-gap)/2;
        action("SKY ENGINE  "+onOff(SkyLyricsClient.CONFIG.skyEnabled()), () -> {
            SkyLyricsClient.CONFIG.toggleSkyEnabled(); SkyEffectController.refreshFromConfig();
        }, contentLeft,y+22,half,32);
        action("LYRIC DISPLAY  "+onOff(SkyLyricsClient.CONFIG.enabled()),
                SkyLyricsClient.CONFIG::toggleEnabled,contentLeft+half+gap,y+22,half,32);
        navigate("OPEN MUSIC CENTER",Tab.MUSIC,contentLeft,y+68,half,32);
        navigate("OFFLINE LIBRARY",Tab.LIBRARY,contentLeft+half+gap,y+68,half,32);
        navigate("LYRICS STUDIO",Tab.LYRICS,contentLeft+half+gap,y+114,half,32);
        action(SkyLyricsClient.isDemoActive() ? "STOP VISUAL PREVIEW" : "PLAY VISUAL PREVIEW",
                () -> {
                    if (SkyLyricsClient.isDemoActive()) SkyLyricsClient.stopDemo();
                    else SkyLyricsClient.startDemo();
                }, contentLeft, y+160, contentWidth, 30);
    }

    private void skies(int y) {
        action("SKY ENGINE  "+onOff(SkyLyricsClient.CONFIG.skyEnabled()), () -> {
            SkyLyricsClient.CONFIG.toggleSkyEnabled(); SkyEffectController.refreshFromConfig();
        }, contentLeft,y,Math.min(190,contentWidth),25);
        int cols=4,gap=6,w=(contentWidth-gap*(cols-1))/cols,h=43;
        for(int i=0;i<SkyPresetCatalog.ALL.size();i++) {
            SkyPresetCatalog.Preset preset=SkyPresetCatalog.ALL.get(i);
            Button card=addRenderableWidget(Button.builder(Component.literal(preset.name()), b -> {
                SkyLyricsClient.CONFIG.setSkyPreset(preset.id());
                SkyLyricsClient.CONFIG.setSkyEnabled(true);
                SkyEffectController.refreshFromConfig();
                open(Tab.SKIES);
            }).bounds(contentLeft+(i%cols)*(w+gap),y+118+(i/cols)*50,w,h).build());
            card.active=!preset.id().equals(SkyLyricsClient.CONFIG.skyPreset());
        }
    }

    private void music(int y) {
        int gap = 8, half = (contentWidth - gap) / 2;
        int transportWidth = (contentWidth - 3 * gap) / 4;

        screenAction("IMPORT SONG  +", () -> MusicFilePicker.chooseAudio(this, path -> {
            if (path != null) SkyLyricsClient.importAndPlayAudio(path);
        }), contentLeft, y + 14, half, 34);
        navigate("OPEN OFFLINE LIBRARY", Tab.LIBRARY,
                contentLeft + half + gap, y + 14, half, 34);

        action("PREVIOUS  ◀", SkyLyricsClient::previousLocalTrack,
                contentLeft, y + 62, transportWidth, 36);
        action("PLAY / PAUSE", SkyLyricsClient::togglePlayback,
                contentLeft + transportWidth + gap, y + 62, transportWidth, 36);
        action("STOP", SkyLyricsClient::stopPlayback,
                contentLeft + 2 * (transportWidth + gap), y + 62, transportWidth, 36);
        action("NEXT  ▶", SkyLyricsClient::nextLocalTrack,
                contentLeft + 3 * (transportWidth + gap), y + 62, transportWidth, 36);

        action("SEEK BACK  10s", () -> SkyLyricsClient.seekPlayback(-10_000L),
                contentLeft, y + 110, half, 30);
        action("SEEK FORWARD  10s", () -> SkyLyricsClient.seekPlayback(10_000L),
                contentLeft + half + gap, y + 110, half, 30);
        action("VOLUME  −", () -> SkyLyricsClient.adjustPlaybackVolume(-0.08f),
                contentLeft, y + 150, half, 30);
        action("VOLUME  +", () -> SkyLyricsClient.adjustPlaybackVolume(0.08f),
                contentLeft + half + gap, y + 150, half, 30);
        screenAction("IMPORT / MATCH LRC", () -> MusicFilePicker.chooseLyrics(this, path -> {
            if (path != null) SkyLyricsClient.importLyricsFile(path);
        }), contentLeft, y + 192, contentWidth, 30);
    }

    private void library(int y) {
        int gap = 8, half = (contentWidth - gap) / 2;
        screenAction("IMPORT AUDIO FILE", () -> MusicFilePicker.chooseAudio(this, path -> {
            if (path != null) {
                SkyLyricsClient.importAndPlayAudio(path);
                open(Tab.LIBRARY);
            }
        }), contentLeft, y + 10, half, 28);
        screenAction("IMPORT .LRC LYRICS", () -> MusicFilePicker.chooseLyrics(this, path -> {
            if (path != null) {
                SkyLyricsClient.importLyricsFile(path);
                open(Tab.LIBRARY);
            }
        }), contentLeft + half + gap, y + 10, half, 28);

        int searchButtonWidth = Math.min(76, Math.max(58, contentWidth / 6));
        librarySearchBox = new EditBox(font, contentLeft, y + 46,
                contentWidth - searchButtonWidth - 6, 22, Component.literal(ui("SEARCH LIBRARY")));
        librarySearchBox.setMaxLength(64);
        librarySearchBox.setValue(librarySearchQuery);
        librarySearchBox.setHint(Component.literal(ui("Type a song name...")));
        addRenderableWidget(librarySearchBox);
        screenAction("SEARCH", () -> {
            librarySearchQuery = librarySearchBox.getValue().strip();
            libraryPage = 0;
            open(Tab.LIBRARY);
        }, contentLeft + contentWidth - searchButtonWidth, y + 46, searchButtonWidth, 22);

        MusicLibraryState state = SkyLyricsClient.LIBRARY_STATE;
        String filterLabel = switch (state.filterMode()) {
            case ALL -> "ALL";
            case FAVORITES -> "FAVORITES";
            case PLAYLIST -> "PLAYLIST";
        };
        String activePlaylist = state.activePlaylistName().isBlank() ? "NONE" : state.activePlaylistName();
        action("FILTER  ·  " + filterLabel, state::cycleFilter,
                contentLeft, y + 76, half, 24);
        action("PLAYLIST  ·  " + activePlaylist, state::cyclePlaylist,
                contentLeft + half + gap, y + 76, half, 24);

        action("SHUFFLE  ·  " + onOff(state.shuffleEnabled()), state::toggleShuffle,
                contentLeft, y + 106, half, 24);
        action("REPEAT  ·  " + state.repeatMode().name(), state::cycleRepeatMode,
                contentLeft + half + gap, y + 106, half, 24);

        List<Path> scopedTracks = state.filteredTracks(SkyLyricsClient.MUSIC_LIBRARY.listAudio());
        List<Path> tracks = MusicLibraryState.search(scopedTracks, librarySearchQuery);
        int pageSize = 5;
        int pages = Math.max(1, (tracks.size() + pageSize - 1) / pageSize);
        libraryPage = Math.max(0, Math.min(libraryPage, pages - 1));
        int start = libraryPage * pageSize;
        int rowY = y + 142;
        int favoriteWidth = Math.min(34, Math.max(28, contentWidth / 16));
        int playlistWidth = Math.min(44, Math.max(36, contentWidth / 13));
        int rowGap = 5;
        int playWidth = contentWidth - favoriteWidth - playlistWidth - rowGap * 2;

        for (int i = start; i < Math.min(tracks.size(), start + pageSize); i++) {
            Path track = tracks.get(i);
            String name = track.getFileName().toString();
            int maxChars = Math.max(16, playWidth / 7);
            if (name.length() > maxChars) name = name.substring(0, maxChars - 3) + "...";
            final Path selected = track;
            int currentRowY = rowY + (i - start) * 29;
            action("▶  " + name, () -> SkyLyricsClient.playLocalTrack(selected),
                    contentLeft, currentRowY, playWidth, 25);
            action(state.isFavorite(track) ? "★" : "☆", () -> state.toggleFavorite(selected),
                    contentLeft + playWidth + rowGap, currentRowY, favoriteWidth, 25);
            String playlistAction = state.isInActivePlaylist(track) ? "−" : "+";
            Button playlistButton = addRenderableWidget(Button.builder(Component.literal(playlistAction), b -> {
                if (state.playlistNames().isEmpty()) {
                    SkyLyricsClient.MEDIA_NOTICE = "Create a playlist first to organise this track.";
                    open(Tab.LIBRARY);
                } else {
                    state.toggleTrackInActivePlaylist(selected);
                    open(Tab.LIBRARY);
                }
            }).bounds(contentLeft + playWidth + rowGap + favoriteWidth + rowGap,
                    currentRowY, playlistWidth, 25).build());
            playlistButton.active = !state.playlistNames().isEmpty();
        }

        action("◀ PREVIOUS", () -> { libraryPage = Math.max(0, libraryPage - 1); },
                contentLeft, y + 292, half, 23);
        action("NEXT ▶", () -> { libraryPage = Math.min(pages - 1, libraryPage + 1); },
                contentLeft + half + gap, y + 292, half, 23);

        playlistNameBox = new EditBox(font, contentLeft, y + 324,
                contentWidth - 94, 21, Component.literal(ui("NEW PLAYLIST NAME")));
        playlistNameBox.setMaxLength(40);
        playlistNameBox.setHint(Component.literal(ui("New playlist name")));
        addRenderableWidget(playlistNameBox);
        screenAction("CREATE", () -> {
            boolean created = state.createPlaylist(playlistNameBox.getValue());
            SkyLyricsClient.MEDIA_NOTICE = created ? "Playlist created." : "Use a new playlist name (up to 40 characters).";
            open(Tab.LIBRARY);
        }, contentLeft + contentWidth - 88, y + 324, 88, 21);
        action("DELETE ACTIVE PLAYLIST", () -> {
            boolean deleted = state.deleteActivePlaylist();
            SkyLyricsClient.MEDIA_NOTICE = deleted ? "Playlist deleted." : "No active playlist to delete.";
        }, contentLeft, y + 353, contentWidth, 23);
    }

    private void history(int y) {
        int gap = 8;
        int half = (contentWidth - gap) / 2;
        navigate("OPEN OFFLINE LIBRARY",Tab.LIBRARY,
                contentLeft, y + 10, half, 30);
        action("CLEAR HISTORY", SkyLyricsClient.HISTORY::clear,
                contentLeft + half + gap, y + 10, half, 30);
    }

    private void keybinds(int y) {
        int rowHeight = 30;
        int rowGap = 8;
        bindButton("OPEN MUSIC CENTER", SkyLyricsClient.openMenuKeyMapping(),
                contentLeft, y + 10, contentWidth, rowHeight);
        bindButton("TOGGLE LYRIC HUD", SkyLyricsClient.toggleHudKeyMapping(),
                contentLeft, y + 10 + (rowHeight + rowGap), contentWidth, rowHeight);
        bindButton("PLAY / PAUSE", SkyLyricsClient.playPauseKeyMapping(),
                contentLeft, y + 10 + 2 * (rowHeight + rowGap), contentWidth, rowHeight);
        bindButton("NEXT TRACK", SkyLyricsClient.nextTrackKeyMapping(),
                contentLeft, y + 10 + 3 * (rowHeight + rowGap), contentWidth, rowHeight);
        bindButton("PREVIOUS TRACK", SkyLyricsClient.previousTrackKeyMapping(),
                contentLeft, y + 10 + 4 * (rowHeight + rowGap), contentWidth, rowHeight);
        action("RESTORE DEFAULT KEYS", SkyLyricsClient::resetKeyBindings,
                contentLeft, y + 10 + 5 * (rowHeight + rowGap), contentWidth, rowHeight);
    }

    private void bindButton(String label, KeyMapping mapping, int x, int y, int w, int h) {
        Button button = addRenderableWidget(Button.builder(keyBindingMessage(label, mapping, false), b -> {
            pendingKeyBinding = mapping;
            pendingKeyBindingButton = b;
            b.setMessage(keyBindingMessage(label, mapping, true));
        }).bounds(x, y, w, h).build());
    }

    private static Component keyBindingMessage(String label, KeyMapping mapping, boolean waiting) {
        String key = waiting ? ui("PRESS KEY OR MOUSE BUTTON  ·  ESC CANCEL")
                : mapping.getTranslatedKeyMessage().getString();
        return Component.literal(ui(label) + "  [" + key + "]");
    }

    private void applyPendingKeyBinding(InputConstants.Key key) {
        if (pendingKeyBinding == null) return;
        KeyMapping target = pendingKeyBinding;
        pendingKeyBinding = null;
        target.setKey(key);
        KeyMapping.resetMapping();
        if (minecraft != null) minecraft.options.save();
        if (pendingKeyBindingButton != null) {
            String label = pendingKeyBindingButton.getMessage().getString();
            int marker = label.indexOf("  [");
            if (marker >= 0) label = label.substring(0, marker);
            pendingKeyBindingButton.setMessage(keyBindingMessage(label, target, false));
        }
        pendingKeyBindingButton = null;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (pendingKeyBinding != null) {
            if (event.isEscape()) {
                String prompt = "  [" + ui("PRESS KEY OR MOUSE BUTTON  ·  ESC CANCEL") + "]";
                String cancelled = "  [" + ui("CANCELLED") + "]";
                String currentMessage = pendingKeyBindingButton.getMessage().getString();
                pendingKeyBindingButton.setMessage(Component.literal(currentMessage.replace(prompt, cancelled)));
                pendingKeyBinding = null;
                pendingKeyBindingButton = null;
                return true;
            }
            applyPendingKeyBinding(InputConstants.getKey(event));
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (pendingKeyBinding != null) {
            applyPendingKeyBinding(InputConstants.Type.MOUSE.getOrCreate(event.button()));
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void lyrics(int y) {
        int h=29, step=36;
        action("LYRICS  "+onOff(SkyLyricsClient.CONFIG.enabled()),SkyLyricsClient.CONFIG::toggleEnabled,contentLeft,y+12,contentWidth,h);
        action("DISPLAY  "+(SkyLyricsClient.CONFIG.worldLyrics()?"FLOATING WORLD TEXT":"SCREEN HUD"),() -> {
            SkyLyricsClient.CONFIG.toggleWorldLyrics(); SkyLyricsWorldText.resetAnchor();
        },contentLeft,y+12+step,contentWidth,h);
        action("FOLLOW VIEW  "+onOff(SkyLyricsClient.CONFIG.followView()),() -> {
            SkyLyricsClient.CONFIG.toggleFollowView(); SkyLyricsWorldText.resetAnchor();
        },contentLeft,y+12+step*2,contentWidth,h);
        action("WORD-BY-WORD REVEAL  "+onOff(SkyLyricsClient.CONFIG.wordByWord()),
                SkyLyricsClient.CONFIG::toggleWordByWord,contentLeft,y+12+step*3,contentWidth,h);
        action("WORLD TEXT SIZE  −",()->SkyLyricsClient.CONFIG.adjustWorldTextScale(-0.01f),
                contentLeft,y+12+step*4,(contentWidth-8)/2,25);
        action("WORLD TEXT SIZE  +",()->SkyLyricsClient.CONFIG.adjustWorldTextScale(0.01f),
                contentLeft+(contentWidth-8)/2+8,y+12+step*4,(contentWidth-8)/2,25);
        action("SYNC OFFSET  −250 ms",()->SkyLyricsClient.PLAYER.adjustOffset(-250L),
                contentLeft,y+12+step*5,(contentWidth-16)/3,25);
        action("SYNC OFFSET  +250 ms",()->SkyLyricsClient.PLAYER.adjustOffset(250L),
                contentLeft+(contentWidth-16)/3+8,y+12+step*5,(contentWidth-16)/3,25);
        action("RESET OFFSET",SkyLyricsClient.PLAYER::resetOffset,
                contentLeft+2*((contentWidth-16)/3+8),y+12+step*5,(contentWidth-16)/3,25);
    }

    private void styles(int y) {
        String[] names = {"Lavender", "Dreamy", "Comic", "Script", "Ice", "Royal"};
        int cols = 3, gap = 8, w = (contentWidth - gap * 2) / 3;
        for (int i = 0; i < names.length; i++) {
            final int index = i;
            Button styleButton = addRenderableWidget(Button.builder(Component.literal(names[i]), b -> {
                SkyLyricsClient.CONFIG.setTextStyle(index);
                open(Tab.STYLES);
            }).bounds(contentLeft + (i % cols) * (w + gap), y + 16 + (i / cols) * 41, w, 32).build());
            styleButton.active = SkyLyricsClient.CONFIG.textStyle() != i;
        }

        action("GLOW  " + onOff(SkyLyricsClient.CONFIG.glowEnabled()),
                SkyLyricsClient.CONFIG::toggleGlow, contentLeft, y + 101, contentWidth, 26);
        action("TEXT SIZE  −", () -> SkyLyricsClient.CONFIG.adjustWorldTextScale(-0.002f),
                contentLeft, y + 135, (contentWidth - 8) / 2, 25);
        action("TEXT SIZE  +", () -> SkyLyricsClient.CONFIG.adjustWorldTextScale(0.002f),
                contentLeft + (contentWidth - 8) / 2 + 8, y + 135, (contentWidth - 8) / 2, 25);

        action("NOW PLAYING STYLE  ·  "+SkyLyricsClient.CONFIG.nowPlayingStyleName(),
                SkyLyricsClient.CONFIG::cycleNowPlayingStyle,contentLeft,y+165,contentWidth,25);

        int fontCols = 4, fontGap = 6;
        int fontWidth = (contentWidth - fontGap * (fontCols - 1)) / fontCols;
        for (int i = 0; i < SkyFontCatalog.ALL.size(); i++) {
            SkyFontCatalog.Family family = SkyFontCatalog.ALL.get(i);
            Button fontButton = addRenderableWidget(Button.builder(Component.literal(family.name()), b -> {
                SkyLyricsClient.CONFIG.setFontFamily(family.id());
                open(Tab.STYLES);
            }).bounds(contentLeft + (i % fontCols) * (fontWidth + fontGap),
                    y + 220 + (i / fontCols) * 35, fontWidth, 29).build());
            fontButton.active = !family.id().equals(SkyLyricsClient.CONFIG.fontFamily());
        }
    }

    private void hud(int y) {
        action("WORLD TEXT  "+onOff(SkyLyricsClient.CONFIG.worldLyrics()),() -> {
            SkyLyricsClient.CONFIG.toggleWorldLyrics(); SkyLyricsWorldText.resetAnchor();
        },contentLeft,y+18,contentWidth,29);
        action("FOLLOW VIEW  "+onOff(SkyLyricsClient.CONFIG.followView()),() -> {
            SkyLyricsClient.CONFIG.toggleFollowView(); SkyLyricsWorldText.resetAnchor();
        },contentLeft,y+56,contentWidth,29);
        action("PREVIOUS / NEXT LINES  "+onOff(SkyLyricsClient.CONFIG.showContextLines()),
                SkyLyricsClient.CONFIG::toggleContextLines,contentLeft,y+94,contentWidth,29);
        action("HUD BACKDROP  "+onOff(SkyLyricsClient.CONFIG.showBackdrop()),
                SkyLyricsClient.CONFIG::toggleBackdrop,contentLeft,y+132,contentWidth,29);
        action("MOVE HUD UP",()->SkyLyricsClient.CONFIG.adjustVerticalPosition(-0.025f),
                contentLeft,y+170,(contentWidth-8)/2,25);
        action("MOVE HUD DOWN",()->SkyLyricsClient.CONFIG.adjustVerticalPosition(0.025f),
                contentLeft+(contentWidth-8)/2+8,y+170,(contentWidth-8)/2,25);
        action("ELYTRA LYRICS MODE  "+onOff(SkyLyricsClient.CONFIG.elytraLyricsEnabled()),
                SkyLyricsClient.CONFIG::toggleElytraLyricsEnabled,contentLeft,y+208,contentWidth,27);
        screenAction("OPEN HUD LAYOUT STUDIO · 5 WIDGETS  ↗", () -> this.minecraft.gui.setScreen(new HudEditorScreen(this)),
                contentLeft, y + 244, contentWidth, 30);
    }

    private void performance(int y) {
        action("DISABLE SKY EFFECT",()->{
            SkyLyricsClient.CONFIG.setSkyEnabled(false); SkyEffectController.refreshFromConfig();
        },contentLeft,y+25,contentWidth,29);
        action("RESTORE SELECTED SKY",()->{
            SkyLyricsClient.CONFIG.setSkyEnabled(true); SkyEffectController.refreshFromConfig();
        },contentLeft,y+65,contentWidth,29);
    }

    private void settings(int y) {
        action("SKY ENGINE  "+onOff(SkyLyricsClient.CONFIG.skyEnabled()),() -> {
            SkyLyricsClient.CONFIG.toggleSkyEnabled(); SkyEffectController.refreshFromConfig();
        },contentLeft,y+18,contentWidth,29);
        action("LYRIC DISPLAY  "+onOff(SkyLyricsClient.CONFIG.enabled()),
                SkyLyricsClient.CONFIG::toggleEnabled,contentLeft,y+56,contentWidth,29);
        action("WORD REVEAL  "+onOff(SkyLyricsClient.CONFIG.wordByWord()),
                SkyLyricsClient.CONFIG::toggleWordByWord,contentLeft,y+94,contentWidth,29);
        action("GLOW  "+onOff(SkyLyricsClient.CONFIG.glowEnabled()),
                SkyLyricsClient.CONFIG::toggleGlow,contentLeft,y+132,contentWidth,29);
        action("LANGUAGE  ·  "+SkyLyricsClient.CONFIG.languageName(),
                SkyLyricsClient.CONFIG::toggleLanguage,contentLeft,y+170,contentWidth,29);
    }

    private void guide(int y) {
        int gap = 8;
        int width = (contentWidth - 2 * gap) / 3;
        navigate("MUSIC CENTER",Tab.MUSIC,contentLeft,y+18,width,30);
        navigate("LYRICS",Tab.LYRICS,contentLeft+width+gap,y+18,width,30);
        navigate("LISTENING HISTORY",Tab.HISTORY,contentLeft+2*(width+gap),y+18,width,30);
    }

    private void navigate(String label,Tab next,int x,int y,int w,int h) {
        addRenderableWidget(Button.builder(Component.literal(ui(label)), b -> open(next))
                .bounds(x,y,w,h).build());
    }

    private void screenAction(String label,Runnable operation,int x,int y,int w,int h) {
        addRenderableWidget(Button.builder(Component.literal(ui(label)), b -> operation.run())
                .bounds(x,y,w,h).build());
    }

    private void action(String label,Runnable operation,int x,int y,int w,int h) {
        addRenderableWidget(Button.builder(Component.literal(ui(label)), b -> {
            operation.run();
            open(tab);
        }).bounds(x,y,w,h).build());
    }
    private static String onOff(boolean value) { return value ? "ON" : "OFF"; }

    private static String ui(String english) {
        if (SkyLyricsClient.CONFIG.isEnglish()) return english;
        // Dynamic toggle labels are composed from a translated ON/OFF state.
        if (english.startsWith("SKY ENGINE  ")) return "MESIN LANGIT  " + english.substring("SKY ENGINE  ".length());
        if (english.startsWith("LYRIC DISPLAY  ")) return "TAMPILAN LIRIK  " + english.substring("LYRIC DISPLAY  ".length());
        if (english.startsWith("LYRICS  ")) return "LIRIK  " + english.substring("LYRICS  ".length());
        if (english.startsWith("DISPLAY  ")) return "TAMPILAN  " + english.substring("DISPLAY  ".length());
        if (english.startsWith("FOLLOW VIEW  ")) return "IKUTI PANDANGAN  " + english.substring("FOLLOW VIEW  ".length());
        if (english.startsWith("WORD-BY-WORD REVEAL  ")) return "TAMPILKAN PER KATA  " + english.substring("WORD-BY-WORD REVEAL  ".length());
        if (english.startsWith("WORD REVEAL  ")) return "TAMPILKAN PER KATA  " + english.substring("WORD REVEAL  ".length());
        if (english.startsWith("GLOW  ")) return "CAHAYA  " + english.substring("GLOW  ".length());
        if (english.startsWith("WORLD TEXT  ")) return "TEKS DUNIA  " + english.substring("WORLD TEXT  ".length());
        if (english.startsWith("PREVIOUS / NEXT LINES  ")) return "BARIS SEBELUM / SESUDAH  " + english.substring("PREVIOUS / NEXT LINES  ".length());
        if (english.startsWith("HUD BACKDROP  ")) return "LATAR HUD  " + english.substring("HUD BACKDROP  ".length());
        if (english.startsWith("ELYTRA LYRICS MODE  ")) return "MODE LIRIK ELYTRA  " + english.substring("ELYTRA LYRICS MODE  ".length());
        if (english.startsWith("NOW PLAYING STYLE  ·  ")) return "GAYA SEDANG DIPUTAR  ·  " + switch (english.substring("NOW PLAYING STYLE  ·  ".length())) {
            case "Bordered" -> "Berbingkai";
            case "Borderless" -> "Tanpa bingkai";
            case "Compact" -> "Ringkas";
            default -> english.substring("NOW PLAYING STYLE  ·  ".length());
        };
        if (english.startsWith("LANGUAGE  ·  ")) return "BAHASA  ·  " + (SkyLyricsClient.CONFIG.isEnglish() ? "English" : "Indonesia");
        return switch (english) {
            case "FILTER  ·  ALL" -> "FILTER  ·  SEMUA";
            case "FILTER  ·  FAVORITES" -> "FILTER  ·  FAVORIT";
            case "FILTER  ·  PLAYLIST" -> "FILTER  ·  PLAYLIST";
            case "SEARCH" -> "CARI";
            case "CREATE" -> "BUAT";
            case "DELETE ACTIVE PLAYLIST" -> "HAPUS PLAYLIST AKTIF";
            case "SHUFFLE  ·  ON" -> "ACAK  ·  AKTIF";
            case "SHUFFLE  ·  OFF" -> "ACAK  ·  NONAKTIF";
            case "REPEAT  ·  OFF" -> "ULANGI  ·  MATI";
            case "REPEAT  ·  ALL" -> "ULANGI  ·  SEMUA";
            case "REPEAT  ·  ONE" -> "ULANGI  ·  SATU";
            case "LOCAL ONLY" -> "LOKAL SAJA";
            case "Type a song name..." -> "Ketik nama lagu...";
            case "New playlist name" -> "Nama playlist baru";
            case "SEARCH LIBRARY" -> "CARI DI PUSTAKA";
            case "No tracks in this view. Import audio or change the filter." -> "Tidak ada lagu pada tampilan ini. Impor audio atau ubah filter.";
            case "No matching tracks. Try another search." -> "Tidak ada hasil. Coba kata pencarian lain.";

            case "SkyLyrics by Sky" -> "SkyLyrics oleh Sky";
            case "OVERVIEW" -> "RINGKASAN";
            case "MUSIC CENTER" -> "PUSAT MUSIK";
            case "OFFLINE LIBRARY" -> "PUSTAKA OFFLINE";
            case "HISTORY & STATS" -> "RIWAYAT & STATISTIK";
            case "LYRICS" -> "LIRIK";
            case "STYLES" -> "GAYA";
            case "SKIES" -> "LANGIT";
            case "KEYBINDS" -> "TOMBOL";
            case "PERFORMANCE" -> "PERFORMA";
            case "SETTINGS" -> "PENGATURAN";
            case "QUICK GUIDE" -> "PANDUAN CEPAT";
            case "SKY ENGINE  ON" -> "MESIN LANGIT  AKTIF";
            case "SKY ENGINE  OFF" -> "MESIN LANGIT  NONAKTIF";
            case "LYRIC DISPLAY  ON" -> "TAMPILAN LIRIK  AKTIF";
            case "LYRIC DISPLAY  OFF" -> "TAMPILAN LIRIK  NONAKTIF";
            case "WORD REVEAL  ON" -> "TAMPILKAN PER KATA  AKTIF";
            case "WORD REVEAL  OFF" -> "TAMPILKAN PER KATA  NONAKTIF";
            case "OPEN MUSIC CENTER" -> "BUKA PUSAT MUSIK";
            case "OPEN OFFLINE LIBRARY" -> "BUKA PUSTAKA OFFLINE";
            case "LYRICS STUDIO" -> "STUDIO LIRIK";
            case "PLAY VISUAL PREVIEW" -> "PUTAR PRATINJAU";
            case "STOP VISUAL PREVIEW" -> "HENTIKAN PRATINJAU";
            case "IMPORT SONG  +" -> "IMPOR LAGU  +";
            case "PREVIOUS  ◀" -> "SEBELUMNYA  ◀";
            case "PLAY / PAUSE" -> "PUTAR / JEDA";
            case "NEXT  ▶" -> "BERIKUTNYA  ▶";
            case "STOP" -> "BERHENTI";
            case "SEEK −10s" -> "GESER −10 dtk";
            case "SEEK +10s" -> "GESER +10 dtk";
            case "IMPORT / MATCH LRC" -> "IMPOR / COCOKKAN LRC";
            case "IMPORT AUDIO FILE" -> "IMPOR FILE AUDIO";
            case "IMPORT .LRC LYRICS" -> "IMPOR LIRIK .LRC";
            case "OPEN MUSIC FOLDER" -> "BUKA FOLDER MUSIK";
            case "◀ PREVIOUS" -> "◀ SEBELUMNYA";
            case "NEXT ▶" -> "BERIKUTNYA ▶";
            case "CLEAR HISTORY" -> "HAPUS RIWAYAT";
            case "NEXT TRACK ▶" -> "LAGU BERIKUTNYA ▶";
            case "PREVIOUS ◀" -> "LAGU SEBELUMNYA ◀";
            case "TOGGLE LYRIC HUD" -> "UBAH HUD LIRIK";
            case "NEXT TRACK" -> "LAGU BERIKUTNYA";
            case "PREVIOUS TRACK" -> "LAGU SEBELUMNYA";
            case "RESTORE DEFAULT KEYS" -> "PULIHKAN TOMBOL DEFAULT";
            case "PRESS KEY OR MOUSE BUTTON  ·  ESC CANCEL" -> "TEKAN TOMBOL / MOUSE  ·  ESC BATAL";
            case "CANCELLED" -> "DIBATALKAN";
            case "LYRICS  ON" -> "LIRIK  AKTIF";
            case "LYRICS  OFF" -> "LIRIK  NONAKTIF";
            case "DISPLAY  FLOATING WORLD TEXT" -> "TAMPILAN  TEKS DUNIA MELAYANG";
            case "DISPLAY  SCREEN HUD" -> "TAMPILAN  HUD LAYAR";
            case "FOLLOW VIEW  ON" -> "IKUTI PANDANGAN  AKTIF";
            case "FOLLOW VIEW  OFF" -> "IKUTI PANDANGAN  NONAKTIF";
            case "WORD-BY-WORD REVEAL  ON" -> "TAMPILKAN PER KATA  AKTIF";
            case "WORD-BY-WORD REVEAL  OFF" -> "TAMPILKAN PER KATA  NONAKTIF";
            case "WORLD TEXT SIZE  −" -> "UKURAN TEKS DUNIA  −";
            case "WORLD TEXT SIZE  +" -> "UKURAN TEKS DUNIA  +";
            case "SYNC OFFSET  −250 ms" -> "GESER SINKRON  −250 ms";
            case "SYNC OFFSET  +250 ms" -> "GESER SINKRON  +250 ms";
            case "RESET OFFSET" -> "ATUR ULANG OFFSET";
            case "GLOW  ON" -> "CAHAYA  AKTIF";
            case "GLOW  OFF" -> "CAHAYA  NONAKTIF";
            case "TEXT SIZE  −" -> "UKURAN TEKS  −";
            case "TEXT SIZE  +" -> "UKURAN TEKS  +";
            case "NOW PLAYING STYLE  ·  Bordered" -> "GAYA SEDANG DIPUTAR  ·  Berbingkai";
            case "NOW PLAYING STYLE  ·  Borderless" -> "GAYA SEDANG DIPUTAR  ·  Tanpa bingkai";
            case "NOW PLAYING STYLE  ·  Compact" -> "GAYA SEDANG DIPUTAR  ·  Ringkas";
            case "WORLD TEXT  ON" -> "TEKS DUNIA  AKTIF";
            case "WORLD TEXT  OFF" -> "TEKS DUNIA  NONAKTIF";
            case "PREVIOUS / NEXT LINES  ON" -> "BARIS SEBELUM / SESUDAH  AKTIF";
            case "PREVIOUS / NEXT LINES  OFF" -> "BARIS SEBELUM / SESUDAH  NONAKTIF";
            case "HUD BACKDROP  ON" -> "LATAR HUD  AKTIF";
            case "HUD BACKDROP  OFF" -> "LATAR HUD  NONAKTIF";
            case "MOVE HUD UP" -> "GESER HUD KE ATAS";
            case "MOVE HUD DOWN" -> "GESER HUD KE BAWAH";
            case "ELYTRA LYRICS MODE  ON" -> "MODE LIRIK ELYTRA  AKTIF";
            case "ELYTRA LYRICS MODE  OFF" -> "MODE LIRIK ELYTRA  NONAKTIF";
            case "OPEN HUD LAYOUT STUDIO · 5 WIDGETS  ↗" -> "BUKA EDITOR TATA LETAK HUD · 5 WIDGET  ↗";
            case "DISABLE SKY EFFECT" -> "MATIKAN EFEK LANGIT";
            case "RESTORE SELECTED SKY" -> "AKTIFKAN LANGIT PILIHAN";
            case "ON" -> "AKTIF";
            case "OFF" -> "NONAKTIF";
            case "LYRICS READY" -> "LIRIK SIAP";
            case "WAITING FOR MUSIC" -> "MENUNGGU MUSIK";
            case "PLAYING" -> "SEDANG DIPUTAR";
            case "PAUSED" -> "DIJEDA";
            case "Volume " -> "Volume ";
            case "CINEMATIC MUSIC EXPERIENCE" -> "PENGALAMAN MUSIK SINEMATIK";
            case "BY SKY  ·  CINEMATIC MUSIC EXPERIENCE" -> "OLEH SKY  ·  MUSIK SINEMATIK";
            case "CINEMATIC PREVIEW" -> "PRATINJAU SINEMATIK";
            case "SUPPORTED: MP3 · OGG · WAV · AIFF · AU" -> "FORMAT: MP3 · OGG · WAV · AIFF · AU";
            case "Matching .lrc files are loaded automatically by filename." -> "File .lrc yang cocok dimuat otomatis berdasarkan nama.";
            case "LISTENING OVERVIEW" -> "RINGKASAN PEMUTARAN";
            case "PLAY SESSIONS" -> "SESI PEMUTARAN";
            case "UNIQUE TRACKS IN RECENT HISTORY" -> "LAGU UNIK DI RIWAYAT TERBARU";
            case "RECENTLY PLAYED" -> "BARU DIPUTAR";
            case "EDIT CONTROLS DIRECTLY FROM THIS SCREEN" -> "UBAH KONTROL LANGSUNG DARI LAYAR INI";
            case "Click a control above, then press a keyboard key or mouse button." -> "Klik kontrol di atas, lalu tekan tombol keyboard atau mouse.";
            case "Press ESC while capturing to cancel. Defaults can be restored above." -> "Tekan ESC untuk batal. Tombol default dapat dipulihkan di atas.";
            case "20 original animated, depth-masked sky presets" -> "20 preset langit animasi dengan mask kedalaman";
            case "20 SKY PRESETS · AUTHENTIC NASA/JPL PHOTO PREVIEWS" -> "20 PRESET LANGIT · FOTO ASTRONOMI ASLI NASA/JPL";
            case "ACTIVE PRESET: " -> "PRESET AKTIF: ";
            case "No track loaded" -> "Belum ada lagu";
            case "SYNC " -> "SINKRON ";
            case "Manual sync offset: " -> "Offset sinkron manual: ";
            case "Style: " -> "Gaya: ";
            case "Font: " -> "Font: ";
            case "Active: " -> "Aktif: ";
            case "World text distance: " -> "Jarak teks dunia: ";
            case "HUD vertical position: " -> "Posisi vertikal HUD: ";
            case "Now Playing position: " -> "Posisi Now Playing: ";
            case "Elytra lyrics mode: " -> "Mode lirik Elytra: ";
            case "Drag and drop HUD panels in the Layout Editor." -> "Geser panel HUD di Editor Tata Letak.";
            case "Only clear background pixels are replaced by the sky shader." -> "Hanya piksel latar kosong yang diganti shader langit.";
            case "Terrain, entities, and HUD pixels are passed through." -> "Terrain, entitas, dan piksel HUD tetap ditampilkan.";
            case "Rendering uses Minecraft's post-effect pipeline." -> "Rendering memakai pipeline post-effect Minecraft.";
            case "Saved to config/skylyrics.properties" -> "Tersimpan di config/skylyrics.properties";
            case "Keys: O menu · H lyrics · K play/pause" -> "Tombol: O menu · H lirik · K putar/jeda";
            case "QUICK GUIDE · SKYLYRICS BY SKY" -> "PANDUAN CEPAT · SKYLYRICS OLEH SKY";
            case "1. MUSIC CENTER → IMPORT SONG → CHOOSE MP3 / OGG / WAV." -> "1. PUSAT MUSIK → IMPOR LAGU → PILIH MP3 / OGG / WAV.";
            case "2. LYRICS → IMPORT / MATCH LRC → CHOOSE THE MATCHING .LRC FILE." -> "2. LIRIK → IMPOR / COCOKKAN LRC → PILIH FILE .LRC YANG SESUAI.";
            case "3. CHOOSE FLOATING WORLD TEXT (3D) OR SCREEN HUD (2D)." -> "3. PILIH TEKS DUNIA MELAYANG (3D) ATAU HUD LAYAR (2D).";
            case "KEYS: O MENU · H LYRICS · K PLAY/PAUSE · N NEXT · B PREVIOUS." -> "TOMBOL: O MENU · H LIRIK · K PUTAR/JEDA · N BERIKUTNYA · B SEBELUMNYA.";
            default -> english;
        };
    }


    private static int mixColor(int a, int b, float t) {
        int alpha = (a >>> 24) & 255;
        int ar = (a >>> 16) & 255, ag = (a >>> 8) & 255, ab = a & 255;
        int br = (b >>> 16) & 255, bg = (b >>> 8) & 255, bb = b & 255;
        int r = Math.round(ar + (br - ar) * t);
        int g = Math.round(ag + (bg - ag) * t);
        int blue = Math.round(ab + (bb - ab) * t);
        return (alpha << 24) | (r << 16) | (g << 8) | blue;
    }
    private void open(Tab next) { this.minecraft.gui.setScreen(new SkyLyricsScreen(next)); }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float delta) {
        super.extractBackground(graphics,mouseX,mouseY,delta);
        graphics.fill(0,0,width,height,0x70090D12);
        graphics.fill(left,top,left+panelWidth,top+panelHeight,0xF0151A21);
        graphics.outline(left,top,panelWidth,panelHeight,0xFF46515E);
        graphics.fill(left,top,left+panelWidth,top+3,0xFF9DAFC2);
        graphics.fill(left,top+56,left+sideWidth,top+panelHeight-10,0xE911161D);
        graphics.fill(left+sideWidth,top+56,left+sideWidth+1,top+panelHeight-10,0xFF343E49);
        if (tab == Tab.OVERVIEW) {
            int cardTop = top + 258;
            int cardBottom = top + panelHeight - 18;
            graphics.fill(contentLeft, cardTop, contentLeft + contentWidth, cardBottom, 0xD012101E);
            graphics.outline(contentLeft, cardTop, contentWidth, cardBottom - cardTop, 0xFF493A5F);
            graphics.fill(contentLeft, cardTop, contentLeft + 3, cardBottom, 0xFFB38BFF);
        }
        if (tab == Tab.MUSIC) {
            int cardTop = top + 346;
            int cardBottom = top + panelHeight - 12;
            graphics.fill(contentLeft, cardTop, contentLeft + contentWidth, cardBottom, 0xD91C232C);
            graphics.outline(contentLeft, cardTop, contentWidth, cardBottom - cardTop, 0xFF3C4A58);
            graphics.fill(contentLeft + 2, cardTop + 2, contentLeft + 4, cardBottom - 2, 0xFF9DAFC2);
        }
        if(tab==Tab.SKIES) {
            String artwork = SkyPresetCatalog.artworkFor(SkyLyricsClient.CONFIG.skyPreset());
            Identifier preview = Identifier.fromNamespaceAndPath(
                    SkyLyricsClient.MOD_ID, "textures/gui/sky_gallery/" + artwork + ".jpg");
            int previewY = top + 146;
            graphics.blit(RenderPipelines.GUI_TEXTURED, preview,
                    contentLeft, previewY, 0.0F, 0.0F, contentWidth, 72, 1600, 900);
            graphics.fill(contentLeft, previewY, contentLeft + contentWidth, previewY + 72, 0x3A080711);
            graphics.fill(contentLeft, previewY, contentLeft + contentWidth, previewY + 3,
                    SkyPresetCatalog.byId(SkyLyricsClient.CONFIG.skyPreset()).horizonColor() | 0xFF000000);
            int cols=4,gap=6,w=(contentWidth-gap*(cols-1))/cols,h=43, gridY=top+112+118;
            for(int i=0;i<SkyPresetCatalog.ALL.size();i++) {
                SkyPresetCatalog.Preset p=SkyPresetCatalog.ALL.get(i);
                int x=contentLeft+(i%cols)*(w+gap), cy=gridY+(i/cols)*50;
                graphics.fill(x,cy,x+w,cy+4,p.topColor()|0xFF000000);
                graphics.fill(x,cy+4,x+w,cy+7,p.nebulaColor()|0xFF000000);
            }
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float delta) {
        super.extractRenderState(graphics,mouseX,mouseY,delta);
        graphics.text(font,ui("SKYLYRICS"),left+16,top+16,0xFFFFFFFF,true);
        graphics.text(font,ui("LOCAL MUSIC  ·  SYNCHRONIZED LYRICS"),left+16,top+33,0xFFAFBAC6,false);
        graphics.text(font,ui(tab == Tab.GUIDE ? "QUICK GUIDE" : tab == Tab.MUSIC ? "MUSIC CENTER" : tab.name()),contentLeft,top+78,0xFFFFFFFF,true);
        int activeY = top + 70 + tab.ordinal() * navStep;
        graphics.fill(left + 10, activeY + 2, left + 13, activeY + navHeight - 2, 0xFF9DAFC2);
        boolean ready = SkyLyricsClient.PLAYER.hasTrack();
        graphics.fill(left + panelWidth - 142, top + 17, left + panelWidth - 20, top + 38,
                ready ? 0xFF20372F : 0xFF30283B);
        graphics.text(font, ui(ready ? "LYRICS READY" : "WAITING FOR MUSIC"),
                left + panelWidth - 132, top + 24, ready ? 0xFF9BE7BF : 0xFFD2BFEF, true);
        if(tab==Tab.OVERVIEW) {
            graphics.text(font,ui("SKY PRESET")+"  "+SkyPresetCatalog.byId(SkyLyricsClient.CONFIG.skyPreset()).name()
                    +"   ·   ENGINE "+onOff(SkyLyricsClient.CONFIG.skyEnabled()),
                    contentLeft,top+160,0xFFC6A0FF,true);
            graphics.text(font,ui("CINEMATIC PREVIEW"),contentLeft+14,top+277,0xFFBDA7E8,true);
            if(SkyLyricsClient.PLAYER.hasTrack()) {
                graphics.text(font,SkyLyricsClient.PLAYER.track().title(),contentLeft+14,top+299,0xFFFFFFFF,true);
                graphics.text(font,SkyLyricsClient.PLAYER.track().artist(),contentLeft+14,top+318,0xFFBDB7CD,false);
                LyricsCue cue=SkyLyricsClient.PLAYER.currentCue();
                if(cue!=null) graphics.text(font,cue.text(),contentLeft+14,top+344,SkyLyricsClient.CONFIG.accentColor(),true);
                graphics.text(font,ui(SkyLyricsClient.PLAYER.isPlaying()?"PLAYING":"PAUSED")
                        +"  ·  "+LyricsPlayer.formatTime(SkyLyricsClient.PLAYER.positionMs())
                        +" / "+LyricsPlayer.formatTime(SkyLyricsClient.PLAYER.durationMs()),
                        contentLeft+14,top+368,0xFFD4D1E2,false);
            } else {
                graphics.text(font,ui("Press PLAY CINEMATIC PREVIEW to test the 3D lyrics offline."),
                        contentLeft+14,top+305,0xFFBDB7CD,false);
                graphics.text(font,ui("No music account, browser, or server resource pack is needed."),
                        contentLeft+14,top+324,0xFF9690A8,false);
            }
            if(SkyLyricsClient.MEDIA_NOTICE!=null&&!SkyLyricsClient.MEDIA_NOTICE.isBlank())
                graphics.text(font,SkyLyricsClient.MEDIA_NOTICE,contentLeft+14,top+393,0xFF9D96B3,false);
        } else if (tab == Tab.MUSIC) {
            graphics.text(font, "NOW PLAYING", contentLeft + 14, top + 360, 0xFFAFBFCE, true);
            if (SkyLyricsClient.AUDIO.hasFile()) {
                String title = SkyLyricsClient.AUDIO.currentFile().getFileName().toString();
                if (title.length() > 58) title = title.substring(0, 55) + "...";
                graphics.text(font, title, contentLeft + 14, top + 382, 0xFFF1F4F7, true);
                graphics.text(font, SkyLyricsClient.AUDIO.status(), contentLeft + 14, top + 402, 0xFFB8C8D7, false);
                graphics.text(font, LyricsPlayer.formatTime(SkyLyricsClient.AUDIO.positionMs()) + " / "
                        + LyricsPlayer.formatTime(SkyLyricsClient.AUDIO.durationMs()),
                        contentLeft + 14, top + 424, 0xFFD0D9E2, false);
                graphics.text(font, "VOLUME  " + Math.round(SkyLyricsClient.AUDIO.volume() * 100) + "%",
                        contentLeft + 14, top + 446, 0xFFAAB7C4, false);
                if (SkyLyricsClient.PLAYER.hasTrack() && SkyLyricsClient.PLAYER.currentCue() != null) {
                    graphics.text(font, SkyLyricsClient.PLAYER.currentCue().text(),
                            contentLeft + Math.min(contentWidth - 180, 280), top + 446,
                            SkyLyricsClient.CONFIG.accentColor(), true);
                }
            } else {
                graphics.text(font, "No song selected", contentLeft + 14, top + 384, 0xFFE4EAF0, true);
                graphics.text(font, "Import a song to play it directly inside Minecraft.",
                        contentLeft + 14, top + 406, 0xFFAAB7C4, false);
                graphics.text(font, "No browser or external music player is required.",
                        contentLeft + 14, top + 428, 0xFF8F9CAA, false);
            }
            graphics.text(font, SkyLyricsClient.MEDIA_NOTICE, contentLeft + 14, top + 466, 0xFF81909F, false);
        } else if (tab == Tab.LIBRARY) {
            List<Path> scopedTracks = SkyLyricsClient.LIBRARY_STATE.filteredTracks(SkyLyricsClient.MUSIC_LIBRARY.listAudio());
            List<Path> visibleTracks = MusicLibraryState.search(scopedTracks, librarySearchQuery);
            graphics.text(font, visibleTracks.size() + " / " + SkyLyricsClient.MUSIC_LIBRARY.listAudio().size()
                    + " TRACKS  ·  " + ui("LOCAL ONLY"), contentLeft, top + 105, 0xFFE2E9F0, true);
            if (visibleTracks.isEmpty()) {
                String emptyMessage = librarySearchQuery.isBlank()
                        ? "No tracks in this view. Import audio or change the filter."
                        : "No matching tracks. Try another search.";
                graphics.text(font, ui(emptyMessage), contentLeft, top + 280, 0xFFB7C1CB, false);
            }
            graphics.text(font, ui("FAVORITE  ★   ·   + ADD TO PLAYLIST   ·   SHUFFLE / REPEAT APPLY TO THIS QUEUE"),
                    contentLeft, top + 480, 0xFF8E9BA8, false);
        } else if (tab == Tab.HISTORY) {
            graphics.text(font,ui("LISTENING OVERVIEW"), contentLeft, top + 112, 0xFFC6A0FF, true);
            graphics.text(font, ui("PLAY SESSIONS") + "  ·  " + SkyLyricsClient.HISTORY.totalPlays(),
                    contentLeft, top + 141, 0xFFFFFFFF, true);
            graphics.text(font, ui("UNIQUE TRACKS IN RECENT HISTORY") + "  ·  " + SkyLyricsClient.HISTORY.uniqueTrackCount(),
                    contentLeft, top + 164, 0xFFD4D1E2, false);
            graphics.text(font,ui("RECENTLY PLAYED"), contentLeft, top + 210, 0xFFC6A0FF, true);
            var entries = SkyLyricsClient.HISTORY.recent(7);
            if (entries.isEmpty()) {
                graphics.text(font,ui("No listening sessions yet. Play a local or supported online track."),
                        contentLeft, top + 238, 0xFFBDB7CD, false);
            } else {
                for (int i = 0; i < entries.size(); i++) {
                    var entry = entries.get(i);
                    int rowY = top + 238 + i * 27;
                    String title = entry.title();
                    if (title.length() > 48) title = title.substring(0, 45) + "...";
                    String subtitle = entry.artist() + "  ·  " + entry.source();
                    if (subtitle.length() > 64) subtitle = subtitle.substring(0, 61) + "...";
                    graphics.text(font, (i + 1) + ". " + title, contentLeft, rowY, 0xFFFFFFFF, true);
                    graphics.text(font, subtitle, contentLeft + 12, rowY + 13, 0xFFBDB7CD, false);
                }
            }
            graphics.text(font,ui("History stays in config/skylyrics/history.json; audio is never stored here."),
                    contentLeft, top + panelHeight - 28, 0xFF9690A8, false);
        } else if (tab == Tab.KEYBINDS) {
            graphics.text(font,ui("EDIT CONTROLS DIRECTLY FROM THIS SCREEN"),
                    contentLeft, top + 360, 0xFFFFFFFF, true);
            graphics.text(font,ui("Click a control above, then press a keyboard key or mouse button."),
                    contentLeft, top + 383, 0xFFD4D1E2, false);
            graphics.text(font,ui("Press ESC while capturing to cancel. Defaults can be restored above."),
                    contentLeft, top + 406, 0xFFBDB7CD, false);
        } else if(tab==Tab.SKIES) {
            graphics.text(font,ui("20 SKY PRESETS · AUTHENTIC NASA/JPL PHOTO PREVIEWS"),
                    contentLeft,top+138,0xFFE8DFFF,true);
            graphics.text(font,ui("ACTIVE PRESET: ")+SkyPresetCatalog.byId(SkyLyricsClient.CONFIG.skyPreset()).name(),
                    contentLeft,top+panelHeight-18,SkyLyricsClient.CONFIG.accentColor(),true);
        } else if(tab==Tab.LYRICS) {
            String title=SkyLyricsClient.PLAYER.hasTrack()?SkyLyricsClient.PLAYER.track().title():"No track loaded";
            graphics.text(font,title,contentLeft,top+302,0xFFFFFFFF,true);
            LyricsCue cue=SkyLyricsClient.PLAYER.currentCue();
            if(cue!=null) graphics.text(font,cue.text(),contentLeft,top+324,SkyLyricsClient.CONFIG.accentColor(),true);
            graphics.text(font,ui("SYNC ")+LyricsPlayer.formatTime(SkyLyricsClient.PLAYER.positionMs())+
                    " / "+LyricsPlayer.formatTime(SkyLyricsClient.PLAYER.durationMs()),contentLeft,top+346,0xFFD4D1E2,false);
            graphics.text(font,ui("Manual sync offset: ")+SkyLyricsClient.PLAYER.manualOffsetMs()+" ms",
                    contentLeft,top+370,0xFFBDB7CD,false);
        } else if(tab==Tab.STYLES) {
            graphics.text(font, ui("Style: ") + SkyLyricsClient.CONFIG.textStyleName()
                    + "  ·  Glow: " + onOff(SkyLyricsClient.CONFIG.glowEnabled()),
                    contentLeft, top + 245, SkyLyricsClient.CONFIG.accentColor(), true);
            graphics.centeredText(font, SkyLyricsTypography.ordered("Dream it. Sing it."),
                    contentLeft + contentWidth / 2, top + 275, 0xFFFFFFFF);
            graphics.text(font, ui("Font: ") + SkyLyricsClient.CONFIG.fontFamilyName(),
                    contentLeft, top + 312, 0xFFBDB7CD, false);
        } else if(tab==Tab.HUD) {
            graphics.text(font,ui("World text distance: ")+String.format("%.1f",SkyLyricsClient.CONFIG.worldDistance())+" blocks",
                    contentLeft,top+235,0xFFD4D1E2,false);
            graphics.text(font,ui("HUD vertical position: ")+Math.round(SkyLyricsClient.CONFIG.verticalPosition()*100)+"%",
                    contentLeft,top+258,0xFFD4D1E2,false);
            graphics.text(font,ui("Now Playing position: ")+Math.round(SkyLyricsClient.CONFIG.nowPlayingHudX()*100)+"% X",
                    contentLeft,top+281,0xFFD4D1E2,false);
            graphics.text(font,ui("Elytra lyrics mode: ")+onOff(SkyLyricsClient.CONFIG.elytraLyricsEnabled()),
                    contentLeft,top+304,0xFFBDB7CD,false);
            graphics.text(font,ui("Drag and drop HUD panels in the Layout Editor."),
                    contentLeft,top+326,0xFFBDB7CD,false);
        } else if(tab==Tab.PERFORMANCE) {
            graphics.text(font,ui("Only clear background pixels are replaced by the sky shader."),contentLeft,top+190,0xFFD4D1E2,false);
            graphics.text(font,ui("Terrain, entities, and HUD pixels are passed through."),contentLeft,top+213,0xFFD4D1E2,false);
            graphics.text(font,ui("Rendering uses Minecraft's post-effect pipeline."),contentLeft,top+236,0xFFD4D1E2,false);
        } else if(tab==Tab.SETTINGS) {
            graphics.text(font,ui("Saved to config/skylyrics.properties"),contentLeft,top+220,0xFFD4D1E2,false);
            graphics.text(font,ui("Keys: O menu · H lyrics · K play/pause"),contentLeft,top+244,0xFFD4D1E2,false);
        } else if(tab==Tab.GUIDE) {
            graphics.text(font,ui("QUICK GUIDE · SKYLYRICS BY SKY"),contentLeft,top+190,0xFFFFFFFF,true);
            graphics.text(font,ui("1. MUSIC CENTER → IMPORT SONG → CHOOSE MP3 / OGG / WAV."),contentLeft,top+218,0xFFD4D1E2,false);
            graphics.text(font,ui("2. LYRICS → IMPORT / MATCH LRC → CHOOSE THE MATCHING .LRC FILE."),contentLeft,top+242,0xFFD4D1E2,false);
            graphics.text(font,ui("3. CHOOSE FLOATING WORLD TEXT (3D) OR SCREEN HUD (2D)."),contentLeft,top+266,0xFFD4D1E2,false);
            graphics.text(font,ui("KEYS: O MENU · H LYRICS · K PLAY/PAUSE · N NEXT · B PREVIOUS."),contentLeft,top+350,0xFFC6A0FF,true);
            graphics.text(font,ui("LOCAL AUDIO PLAYS DIRECTLY INSIDE MINECRAFT."),contentLeft,top+376,0xFFBDB7CD,false);
        }
    }
}
