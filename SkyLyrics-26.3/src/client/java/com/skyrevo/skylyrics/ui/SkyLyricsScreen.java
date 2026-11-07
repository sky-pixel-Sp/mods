package com.skyrevo.skylyrics.ui;

import com.skyrevo.skylyrics.SkyLyricsClient;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import com.skyrevo.skylyrics.core.LyricsCue;
import com.skyrevo.skylyrics.core.LyricsPlayer;
import com.skyrevo.skylyrics.core.SkyPresetCatalog;
import com.skyrevo.skylyrics.core.SkyFontCatalog;
import com.skyrevo.skylyrics.render.SkyEffectController;
import com.skyrevo.skylyrics.render.SkyLyricsWorldText;
import com.skyrevo.skylyrics.render.SkyLyricsTypography;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class SkyLyricsScreen extends Screen {
    private enum Tab { OVERVIEW, MUSIC, LIBRARY, HISTORY, ONLINE, LYRICS, STYLES, SKIES, HUD, KEYBINDS, PERFORMANCE, SETTINGS, GUIDE }
    private static boolean firstOpen = true;
    private final Tab tab;
    private int left, top, panelWidth, panelHeight, sideWidth, contentLeft, contentWidth;
    private static int libraryPage;
    private KeyMapping pendingKeyBinding;
    private Button pendingKeyBindingButton;
    private int navStep = 34;
    private int navHeight = 27;

    public SkyLyricsScreen() { this(Tab.OVERVIEW); }
    private SkyLyricsScreen(Tab tab) {
        super(Component.literal("SkyLyrics by Sky"));
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
        navStep = Math.min(34, Math.max(24, (panelHeight - 78) / Tab.values().length));
        navHeight = Math.max(20, Math.min(27, navStep - 4));

        String[] labels = {"OVERVIEW","MUSIC CENTER","OFFLINE LIBRARY","HISTORY & STATS","ONLINE MUSIC","LYRICS","STYLES","SKIES","HUD","KEYBINDS","PERFORMANCE","SETTINGS","QUICK GUIDE"};
        Tab[] tabs = Tab.values();
        for (int i=0;i<tabs.length;i++) {
            Tab next = tabs[i];
            Button item = addRenderableWidget(Button.builder(Component.literal(labels[i]), b -> open(next))
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
            case ONLINE -> online(y);
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
        navigate("ONLINE MUSIC",Tab.ONLINE,contentLeft,y+114,half,32);
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
            }).bounds(contentLeft+(i%cols)*(w+gap),y+39+(i/cols)*(h+7),w,h).build());
            card.active=!preset.id().equals(SkyLyricsClient.CONFIG.skyPreset());
        }
    }

    private void music(int y) {
        int gap = 8, half = (contentWidth - gap) / 2;
        action("IMPORT SONG  +", () -> MusicFilePicker.chooseAudio(path -> {
            if (path != null) SkyLyricsClient.importAndPlayAudio(path);
        }), contentLeft, y + 14, half, 34);
        navigate("OPEN OFFLINE LIBRARY",Tab.LIBRARY,
                contentLeft + half + gap, y + 14, half, 34);
        action("PREVIOUS  ◀", SkyLyricsClient::previousLocalTrack,
                contentLeft, y + 62, (contentWidth - 3 * gap) / 4, 34);
        action("PLAY / PAUSE", SkyLyricsClient::togglePlayback,
                contentLeft + (contentWidth - 3 * gap) / 4 + gap, y + 62,
                (contentWidth - 3 * gap) / 4, 34);
        action("STOP", SkyLyricsClient::stopPlayback,
                contentLeft + 2 * ((contentWidth - 3 * gap) / 4 + gap), y + 62,
                (contentWidth - 3 * gap) / 4, 34);
        action("NEXT  ▶", SkyLyricsClient::nextLocalTrack,
                contentLeft + 3 * ((contentWidth - 3 * gap) / 4 + gap), y + 62,
                (contentWidth - 3 * gap) / 4, 34);
        action("SEEK −10s", () -> SkyLyricsClient.seekPlayback(-10_000L),
                contentLeft, y + 110, half, 30);
        action("SEEK +10s", () -> SkyLyricsClient.seekPlayback(10_000L),
                contentLeft + half + gap, y + 110, half, 30);
        action("VOLUME −", () -> SkyLyricsClient.adjustPlaybackVolume(-0.08f),
                contentLeft, y + 152, half, 30);
        action("VOLUME +", () -> SkyLyricsClient.adjustPlaybackVolume(0.08f),
                contentLeft + half + gap, y + 152, half, 30);
        action("IMPORT / MATCH LRC", () -> MusicFilePicker.chooseLyrics(path -> {
            if (path != null) SkyLyricsClient.importLyricsFile(path);
        }), contentLeft, y + 194, contentWidth, 30);
    }

    private void library(int y) {
        int gap = 8, half = (contentWidth - gap) / 2;
        action("IMPORT AUDIO FILE", () -> MusicFilePicker.chooseAudio(path -> {
            if (path != null) SkyLyricsClient.importAndPlayAudio(path);
        }), contentLeft, y + 10, half, 30);
        action("IMPORT .LRC LYRICS", () -> MusicFilePicker.chooseLyrics(path -> {
            if (path != null) SkyLyricsClient.importLyricsFile(path);
        }), contentLeft + half + gap, y + 10, half, 30);
        action("OPEN MUSIC FOLDER", SkyLyricsClient::openMusicFolder,
                contentLeft, y + 48, contentWidth, 27);

        var tracks = SkyLyricsClient.MUSIC_LIBRARY.listAudio();
        int pageSize = 6;
        int pages = Math.max(1, (tracks.size() + pageSize - 1) / pageSize);
        libraryPage = Math.max(0, Math.min(libraryPage, pages - 1));
        int start = libraryPage * pageSize;
        for (int i = start; i < Math.min(tracks.size(), start + pageSize); i++) {
            var track = tracks.get(i);
            String name = track.getFileName().toString();
            if (name.length() > 42) name = name.substring(0, 39) + "...";
            final var selected = track;
            action("▶  " + name, () -> SkyLyricsClient.playLocalTrack(selected),
                    contentLeft, y + 90 + (i - start) * 38, contentWidth, 31);
        }
        action("◀ PREVIOUS", () -> { libraryPage = Math.max(0, libraryPage - 1); },
                contentLeft, y + 90 + pageSize * 38, half, 27);
        action("NEXT ▶", () -> { libraryPage = Math.min(pages - 1, libraryPage + 1); },
                contentLeft + half + gap, y + 90 + pageSize * 38, half, 27);
    }

    private void history(int y) {
        int gap = 8;
        int half = (contentWidth - gap) / 2;
        navigate("OPEN OFFLINE LIBRARY",Tab.LIBRARY,
                contentLeft, y + 10, half, 30);
        action("CLEAR HISTORY", SkyLyricsClient.HISTORY::clear,
                contentLeft + half + gap, y + 10, half, 30);
    }

    private void online(int y) {
        int gap = 8, half = (contentWidth - gap) / 2;
        action("OPEN YOUTUBE MUSIC", () -> SkyLyricsClient.openOnlineProvider("youtube_music"),
                contentLeft, y + 18, half, 36);
        action("OPEN SPOTIFY WEB", () -> SkyLyricsClient.openOnlineProvider("spotify"),
                contentLeft + half + gap, y + 18, half, 36);
        action("PLAY / PAUSE", () -> SkyLyricsClient.controlOnline("play_pause"),
                contentLeft, y + 72, half, 31);
        action("NEXT TRACK ▶", () -> SkyLyricsClient.controlOnline("next"),
                contentLeft + half + gap, y + 72, half, 31);
        action("PREVIOUS ◀", () -> SkyLyricsClient.controlOnline("previous"),
                contentLeft, y + 113, half, 31);
        action("SEEK −10s", () -> SkyLyricsClient.controlOnline("seek_back"),
                contentLeft + half + gap, y + 113, half, 31);
        action("SEEK +10s", () -> SkyLyricsClient.controlOnline("seek_forward"),
                contentLeft, y + 154, half, 31);
        action("VOLUME −", () -> SkyLyricsClient.controlOnline("volume_down"),
                contentLeft + half + gap, y + 154, half, 31);
        action("VOLUME +", () -> SkyLyricsClient.controlOnline("volume_up"),
                contentLeft, y + 195, half, 31);
        action("PAUSE ONLINE PLAYER", () -> SkyLyricsClient.controlOnline("pause"),
                contentLeft + half + gap, y + 195, half, 31);
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
        String key = waiting ? "PRESS KEY OR MOUSE BUTTON  ·  ESC CANCEL"
                : mapping.getTranslatedKeyMessage().getString();
        return Component.literal(label + "  [" + key + "]");
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
                pendingKeyBindingButton.setMessage(Component.literal(
                        pendingKeyBindingButton.getMessage().getString().replace(
                                "  [PRESS KEY OR MOUSE BUTTON  ·  ESC CANCEL]", "  [CANCELLED]")));
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
    }

    private void guide(int y) {
        navigate("OPEN MUSIC CENTER",Tab.MUSIC,contentLeft,y+22,contentWidth,29);
        navigate("OPEN LYRICS & DISPLAY",Tab.LYRICS,contentLeft,y+62,contentWidth,29);
        navigate("OPEN ONLINE MUSIC",Tab.ONLINE,contentLeft,y+102,contentWidth,29);
    }

    private void navigate(String label,Tab next,int x,int y,int w,int h) {
        addRenderableWidget(Button.builder(Component.literal(label), b -> open(next))
                .bounds(x,y,w,h).build());
    }

    private void screenAction(String label,Runnable operation,int x,int y,int w,int h) {
        addRenderableWidget(Button.builder(Component.literal(label), b -> operation.run())
                .bounds(x,y,w,h).build());
    }

    private void action(String label,Runnable operation,int x,int y,int w,int h) {
        addRenderableWidget(Button.builder(Component.literal(label), b -> {
            operation.run();
            open(tab);
        }).bounds(x,y,w,h).build());
    }
    private static String onOff(boolean value) { return value ? "ON" : "OFF"; }

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
    public void extractBackground(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float delta) {
        super.extractBackground(graphics,mouseX,mouseY,delta);
        graphics.fill(0,0,width,height,0x6B060711);
        graphics.fill(left,top,left+panelWidth,top+panelHeight,0xF00D0C17);
        graphics.outline(left,top,panelWidth,panelHeight,0xFF67518D);
        double phase = System.nanoTime() / 950_000_000.0;
        float pulse = (float) (0.5 + 0.5 * Math.sin(phase));
        graphics.fill(left, top, left + panelWidth, top + 3, mixColor(0xFF7F68D8, 0xFFE18BCB, pulse));
        graphics.fill(left,top+56,left+sideWidth,top+panelHeight-10,0xE9090812);
        graphics.fill(left+sideWidth,top+56,left+sideWidth+1,top+panelHeight-10,0xFF3A2C55);
        if (tab == Tab.OVERVIEW) {
            int cardTop = top + 258;
            int cardBottom = top + panelHeight - 18;
            graphics.fill(contentLeft, cardTop, contentLeft + contentWidth, cardBottom, 0xD012101E);
            graphics.outline(contentLeft, cardTop, contentWidth, cardBottom - cardTop, 0xFF493A5F);
            graphics.fill(contentLeft, cardTop, contentLeft + 3, cardBottom, 0xFFB38BFF);
        }
        if(tab==Tab.SKIES) {
            int cols=4,gap=6,w=(contentWidth-gap*(cols-1))/cols,h=43, y=top+112+39;
            for(int i=0;i<SkyPresetCatalog.ALL.size();i++) {
                SkyPresetCatalog.Preset p=SkyPresetCatalog.ALL.get(i);
                int x=contentLeft+(i%cols)*(w+gap), cy=y+(i/cols)*(h+7);
                graphics.fill(x,cy,x+w,cy+4,p.topColor()|0xFF000000);
                graphics.fill(x,cy+4,x+w,cy+7,p.nebulaColor()|0xFF000000);
            }
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float delta) {
        super.extractRenderState(graphics,mouseX,mouseY,delta);
        graphics.text(font,"SKYLYRICS",left+16,top+16,0xFFFFFFFF,true);
        graphics.text(font,"BY SKY  ·  CINEMATIC MUSIC EXPERIENCE",left+16,top+33,0xFFC7B9E8,false);
        graphics.text(font,tab.name(),contentLeft,top+78,0xFFFFFFFF,true);
        int activeY = top + 70 + tab.ordinal() * navStep;
        graphics.fill(left + 10, activeY + 2, left + 13, activeY + navHeight - 2, 0xFFB58BFF);
        boolean ready = SkyLyricsClient.PLAYER.hasTrack();
        graphics.fill(left + panelWidth - 142, top + 17, left + panelWidth - 20, top + 38,
                ready ? 0xFF20372F : 0xFF30283B);
        graphics.text(font, ready ? "LYRICS READY" : "WAITING FOR MUSIC",
                left + panelWidth - 132, top + 24, ready ? 0xFF9BE7BF : 0xFFD2BFEF, true);
        if(tab==Tab.OVERVIEW) {
            graphics.text(font,"SKY PRESET  "+SkyPresetCatalog.byId(SkyLyricsClient.CONFIG.skyPreset()).name()
                    +"   ·   ENGINE "+onOff(SkyLyricsClient.CONFIG.skyEnabled()),
                    contentLeft,top+160,0xFFC6A0FF,true);
            graphics.text(font,"CINEMATIC PREVIEW",contentLeft+14,top+277,0xFFBDA7E8,true);
            if(SkyLyricsClient.PLAYER.hasTrack()) {
                graphics.text(font,SkyLyricsClient.PLAYER.track().title(),contentLeft+14,top+299,0xFFFFFFFF,true);
                graphics.text(font,SkyLyricsClient.PLAYER.track().artist(),contentLeft+14,top+318,0xFFBDB7CD,false);
                LyricsCue cue=SkyLyricsClient.PLAYER.currentCue();
                if(cue!=null) graphics.text(font,cue.text(),contentLeft+14,top+344,SkyLyricsClient.CONFIG.accentColor(),true);
                graphics.text(font,(SkyLyricsClient.PLAYER.isPlaying()?"PLAYING":"PAUSED")
                        +"  ·  "+LyricsPlayer.formatTime(SkyLyricsClient.PLAYER.positionMs())
                        +" / "+LyricsPlayer.formatTime(SkyLyricsClient.PLAYER.durationMs()),
                        contentLeft+14,top+368,0xFFD4D1E2,false);
            } else {
                graphics.text(font,"Press PLAY CINEMATIC PREVIEW to test the 3D lyrics offline.",
                        contentLeft+14,top+305,0xFFBDB7CD,false);
                graphics.text(font,"No browser extension, server resource pack or music account needed.",
                        contentLeft+14,top+324,0xFF9690A8,false);
            }
            if(SkyLyricsClient.MEDIA_NOTICE!=null&&!SkyLyricsClient.MEDIA_NOTICE.isBlank())
                graphics.text(font,SkyLyricsClient.MEDIA_NOTICE,contentLeft+14,top+393,0xFF9D96B3,false);
        } else if (tab == Tab.MUSIC) {
            if (SkyLyricsClient.AUDIO.hasFile()) {
                graphics.text(font, SkyLyricsClient.AUDIO.currentFile().getFileName().toString(),
                        contentLeft, top + 285, 0xFFFFFFFF, true);
                graphics.text(font, SkyLyricsClient.AUDIO.status(), contentLeft, top + 309, 0xFFC6A0FF, false);
                graphics.text(font, LyricsPlayer.formatTime(SkyLyricsClient.AUDIO.positionMs()) + " / "
                        + LyricsPlayer.formatTime(SkyLyricsClient.AUDIO.durationMs()),
                        contentLeft, top + 334, 0xFFD4D1E2, false);
                graphics.text(font, "Volume " + Math.round(SkyLyricsClient.AUDIO.volume() * 100) + "%",
                        contentLeft, top + 358, 0xFFD4D1E2, false);
            } else if (SkyLyricsClient.CURRENT_MEDIA != null) {
                graphics.text(font, SkyLyricsClient.CURRENT_MEDIA.title(), contentLeft, top + 285, 0xFFFFFFFF, true);
                graphics.text(font, SkyLyricsClient.CURRENT_MEDIA.displaySource() + " · "
                        + SkyLyricsClient.CURRENT_MEDIA.artist(), contentLeft, top + 309, 0xFFC6A0FF, false);
            } else {
                graphics.text(font, "Choose a local song or open an online provider.", contentLeft, top + 300, 0xFFD4D1E2, false);
            }
            graphics.text(font, SkyLyricsClient.MEDIA_NOTICE, contentLeft, top + 388, 0xFF9D96B3, false);
        } else if (tab == Tab.LIBRARY) {
            graphics.text(font, "OFFLINE LIBRARY  ·  " + SkyLyricsClient.MUSIC_LIBRARY.listAudio().size() + " audio files",
                    contentLeft, top + 218, 0xFFC6A0FF, true);
            graphics.text(font, "Supported: MP3 · OGG · WAV · AIFF · AU", contentLeft, top + 241, 0xFFD4D1E2, false);
            graphics.text(font, "Matching .lrc files are loaded automatically by filename.", contentLeft, top + 263, 0xFFBDB7CD, false);
            graphics.text(font, SkyLyricsClient.MUSIC_LIBRARY.musicDirectory().toString(), contentLeft, top + 286, 0xFF9690A8, false);
        } else if (tab == Tab.HISTORY) {
            graphics.text(font, "LISTENING OVERVIEW", contentLeft, top + 112, 0xFFC6A0FF, true);
            graphics.text(font, "PLAY SESSIONS  ·  " + SkyLyricsClient.HISTORY.totalPlays(),
                    contentLeft, top + 141, 0xFFFFFFFF, true);
            graphics.text(font, "UNIQUE TRACKS IN RECENT HISTORY  ·  " + SkyLyricsClient.HISTORY.uniqueTrackCount(),
                    contentLeft, top + 164, 0xFFD4D1E2, false);
            graphics.text(font, "RECENTLY PLAYED", contentLeft, top + 210, 0xFFC6A0FF, true);
            var entries = SkyLyricsClient.HISTORY.recent(7);
            if (entries.isEmpty()) {
                graphics.text(font, "No listening sessions yet. Play a local or supported online track.",
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
            graphics.text(font, "History stays in config/skylyrics/history.json; audio is never stored here.",
                    contentLeft, top + panelHeight - 28, 0xFF9690A8, false);
        } else if (tab == Tab.ONLINE) {
            var media = SkyLyricsClient.CURRENT_MEDIA;
            graphics.text(font, media != null ? "CONNECTED  ·  " + media.displaySource() : "WAITING FOR BROWSER PLAYER",
                    contentLeft, top + 244, media != null ? 0xFF9BE7BF : 0xFFFFD58A, true);
            if (media != null) {
                graphics.text(font, media.title(), contentLeft, top + 271, 0xFFFFFFFF, true);
                graphics.text(font, media.artist(), contentLeft, top + 293, 0xFFBDB7CD, false);
                graphics.text(font, LyricsPlayer.formatTime(media.positionMs()) + " / "
                        + LyricsPlayer.formatTime(media.durationMs()), contentLeft, top + 315, 0xFFD4D1E2, false);
            }
            graphics.text(font, "Browser extension required for remote controls and synced metadata.",
                    contentLeft, top + 354, 0xFFD4D1E2, false);
            graphics.text(font, SkyLyricsClient.MEDIA_NOTICE, contentLeft, top + 378, 0xFF9D96B3, false);
        } else if (tab == Tab.KEYBINDS) {
            graphics.text(font, "EDIT CONTROLS DIRECTLY FROM THIS SCREEN",
                    contentLeft, top + 360, 0xFFFFFFFF, true);
            graphics.text(font, "Click a control above, then press a keyboard key or mouse button.",
                    contentLeft, top + 383, 0xFFD4D1E2, false);
            graphics.text(font, "Press ESC while capturing to cancel. Defaults can be restored above.",
                    contentLeft, top + 406, 0xFFBDB7CD, false);
        } else if(tab==Tab.SKIES) {
            graphics.text(font,"20 original animated, depth-masked sky presets",contentLeft,top+98,0xFFBDB7CD,false);
            graphics.text(font,"Active: "+SkyPresetCatalog.byId(SkyLyricsClient.CONFIG.skyPreset()).name(),
                    contentLeft,top+panelHeight-28,SkyLyricsClient.CONFIG.accentColor(),true);
        } else if(tab==Tab.LYRICS) {
            String title=SkyLyricsClient.PLAYER.hasTrack()?SkyLyricsClient.PLAYER.track().title():"No track loaded";
            graphics.text(font,title,contentLeft,top+302,0xFFFFFFFF,true);
            LyricsCue cue=SkyLyricsClient.PLAYER.currentCue();
            if(cue!=null) graphics.text(font,cue.text(),contentLeft,top+324,SkyLyricsClient.CONFIG.accentColor(),true);
            graphics.text(font,"Sync "+LyricsPlayer.formatTime(SkyLyricsClient.PLAYER.positionMs())+
                    " / "+LyricsPlayer.formatTime(SkyLyricsClient.PLAYER.durationMs()),contentLeft,top+346,0xFFD4D1E2,false);
            graphics.text(font,"Manual sync offset: "+SkyLyricsClient.PLAYER.manualOffsetMs()+" ms",
                    contentLeft,top+370,0xFFBDB7CD,false);
        } else if(tab==Tab.STYLES) {
            graphics.text(font, "Style: " + SkyLyricsClient.CONFIG.textStyleName()
                    + "  ·  Glow: " + onOff(SkyLyricsClient.CONFIG.glowEnabled()),
                    contentLeft, top + 245, SkyLyricsClient.CONFIG.accentColor(), true);
            graphics.centeredText(font, SkyLyricsTypography.ordered("Dream it. Sing it."),
                    contentLeft + contentWidth / 2, top + 275, 0xFFFFFFFF);
            graphics.text(font, "Font: " + SkyLyricsClient.CONFIG.fontFamilyName(),
                    contentLeft, top + 312, 0xFFBDB7CD, false);
        } else if(tab==Tab.HUD) {
            graphics.text(font,"World text distance: "+String.format("%.1f",SkyLyricsClient.CONFIG.worldDistance())+" blocks",
                    contentLeft,top+235,0xFFD4D1E2,false);
            graphics.text(font,"HUD vertical position: "+Math.round(SkyLyricsClient.CONFIG.verticalPosition()*100)+"%",
                    contentLeft,top+258,0xFFD4D1E2,false);
            graphics.text(font,"Now Playing position: "+Math.round(SkyLyricsClient.CONFIG.nowPlayingHudX()*100)+"% X",
                    contentLeft,top+281,0xFFD4D1E2,false);
            graphics.text(font,"Elytra lyrics mode: "+onOff(SkyLyricsClient.CONFIG.elytraLyricsEnabled()),
                    contentLeft,top+304,0xFFBDB7CD,false);
            graphics.text(font,"Drag and drop HUD panels in the Layout Editor.",
                    contentLeft,top+326,0xFFBDB7CD,false);
        } else if(tab==Tab.PERFORMANCE) {
            graphics.text(font,"Only clear background pixels are replaced by the sky shader.",contentLeft,top+190,0xFFD4D1E2,false);
            graphics.text(font,"Terrain, entities, and HUD pixels are passed through.",contentLeft,top+213,0xFFD4D1E2,false);
            graphics.text(font,"Rendering uses Minecraft's post-effect pipeline.",contentLeft,top+236,0xFFD4D1E2,false);
        } else if(tab==Tab.SETTINGS) {
            graphics.text(font,"Saved to config/skylyrics.properties",contentLeft,top+220,0xFFD4D1E2,false);
            graphics.text(font,"Keys: O menu · H lyrics · K play/pause",contentLeft,top+244,0xFFD4D1E2,false);
        } else if(tab==Tab.GUIDE) {
            graphics.text(font,"QUICK START  ·  SKYLYRICS BY SKY",contentLeft,top+190,0xFFFFFFFF,true);
            graphics.text(font,"1. Open Music Center, choose Import Song, and select your audio file.",contentLeft,top+218,0xFFD4D1E2,false);
            graphics.text(font,"2. Open Lyrics and import a matching .LRC file for timed lyrics.",contentLeft,top+242,0xFFD4D1E2,false);
            graphics.text(font,"3. Choose Floating World Text or Screen HUD in Lyrics settings.",contentLeft,top+266,0xFFD4D1E2,false);
            graphics.text(font,"4. Online: open Spotify / YouTube Music in Chrome or Edge and load the extension.",contentLeft,top+290,0xFFD4D1E2,false);
            graphics.text(font,"Keys: O menu · H lyric HUD · K play/pause · N next · B previous.",contentLeft,top+329,0xFFC6A0FF,true);
            graphics.text(font,"Local files play inside Minecraft; browser music continues in the browser.",contentLeft,top+355,0xFFBDB7CD,false);
            graphics.text(font,"Spotify Web supplies track metadata and controls; synced Spotify lyrics stay disabled.",contentLeft,top+379,0xFF9690A8,false);
        }
    }
}
