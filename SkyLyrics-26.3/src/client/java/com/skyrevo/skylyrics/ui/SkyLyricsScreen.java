package com.skyrevo.skylyrics.ui;

import com.skyrevo.skylyrics.SkyLyricsClient;
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

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Path;

public final class SkyLyricsScreen extends Screen {
    private enum Tab { OVERVIEW, PLAYER, SKIES, LYRICS, STYLES, HUD, PERFORMANCE, SETTINGS, INFORMATION }
    private static boolean firstOpen = true;
    private final Tab tab;
    private int left, top, panelWidth, panelHeight, sideWidth, contentLeft, contentWidth;

    public SkyLyricsScreen() { this(Tab.OVERVIEW); }
    private SkyLyricsScreen(Tab tab) {
        super(Component.literal("SkyLyrics Studio"));
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

        String[] labels = {"HOME","PLAYER","SKIES","LYRICS","STYLES","HUD","PERFORMANCE","SETTINGS","INFO"};
        Tab[] tabs = Tab.values();
        for (int i=0;i<tabs.length;i++) {
            Tab next = tabs[i];
            Button item = addRenderableWidget(Button.builder(Component.literal(labels[i]), b -> open(next))
                    .bounds(left+10, top+70+i*34, sideWidth-20, 27).build());
            item.active = tab != next;
        }
        addRenderableWidget(Button.builder(Component.literal("×"), b -> onClose())
                .bounds(left+panelWidth-34, top+12, 22, 22).build());

        int y = top + 112;
        switch (tab) {
            case OVERVIEW -> overview(y);
            case PLAYER -> player(y);
            case SKIES -> skies(y);
            case LYRICS -> lyrics(y);
            case STYLES -> styles(y);
            case HUD -> hud(y);
            case PERFORMANCE -> performance(y);
            case SETTINGS -> settings(y);
            case INFORMATION -> information(y);
        }
    }

    private void overview(int y) {
        int gap=8, half=(contentWidth-gap)/2;
        action("SKY ENGINE  "+onOff(SkyLyricsClient.CONFIG.skyEnabled()), () -> {
            SkyLyricsClient.CONFIG.toggleSkyEnabled(); SkyEffectController.refreshFromConfig();
        }, contentLeft,y+22,half,32);
        action("LYRIC DISPLAY  "+onOff(SkyLyricsClient.CONFIG.enabled()),
                SkyLyricsClient.CONFIG::toggleEnabled,contentLeft+half+gap,y+22,half,32);
        action("OPEN SKY PRESETS",()->open(Tab.SKIES),contentLeft,y+68,half,32);
        action("OPEN LYRICS STUDIO",()->open(Tab.LYRICS),contentLeft+half+gap,y+68,half,32);
        action(SkyLyricsClient.isDemoActive() ? "STOP CINEMATIC PREVIEW" : "PLAY CINEMATIC PREVIEW",
                () -> {
                    if (SkyLyricsClient.isDemoActive()) SkyLyricsClient.stopDemo();
                    else SkyLyricsClient.startDemo();
                }, contentLeft, y+114, contentWidth, 30);
    }

    private void player(int y) {
        int gap = 8;
        int half = (contentWidth - gap) / 2;
        action("OPEN LOCAL LYRICS FOLDER", this::openLyricsFolder,
                contentLeft, y + 8, contentWidth, 32);
        action("PREVIOUS LINE", SkyLyricsClient.PLAYER::previousCue,
                contentLeft, y + 52, half, 34);
        action(SkyLyricsClient.PLAYER.isPlaying() ? "PAUSE TIMELINE" : "PLAY TIMELINE",
                SkyLyricsClient.PLAYER::toggle, contentLeft + half + gap, y + 52, half, 34);
        action("SEEK −10 SEC", () -> SkyLyricsClient.PLAYER.seekBy(-10_000),
                contentLeft, y + 94, half, 30);
        action("SEEK +10 SEC", () -> SkyLyricsClient.PLAYER.seekBy(10_000),
                contentLeft + half + gap, y + 94, half, 30);
        action("NEXT LINE", SkyLyricsClient.PLAYER::nextCue,
                contentLeft, y + 132, contentWidth, 30);

        java.util.List<Path> tracks = SkyLyricsClient.LIBRARY.listTracks();
        int listTop = y + 184;
        for (int i = 0; i < Math.min(5, tracks.size()); i++) {
            Path track = tracks.get(i);
            action("LOAD  ·  " + track.getFileName(),
                    () -> SkyLyricsClient.loadTrack(track),
                    contentLeft, listTop + i * 34, contentWidth, 28);
        }
    }

    private void openLyricsFolder() {
        try {
            if (!Desktop.isDesktopSupported()) {
                SkyLyricsClient.MEDIA_NOTICE = "Open the lyrics folder manually: " + SkyLyricsClient.LIBRARY.directory();
                return;
            }
            Desktop.getDesktop().open(SkyLyricsClient.LIBRARY.directory().toFile());
            SkyLyricsClient.MEDIA_NOTICE = "Local lyrics folder opened.";
        } catch (IOException | UnsupportedOperationException exception) {
            SkyLyricsClient.MEDIA_NOTICE = "Could not open lyrics folder: " + exception.getMessage();
        }
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

        int fontCols = 4, fontGap = 6;
        int fontWidth = (contentWidth - fontGap * (fontCols - 1)) / fontCols;
        for (int i = 0; i < SkyFontCatalog.ALL.size(); i++) {
            SkyFontCatalog.Family family = SkyFontCatalog.ALL.get(i);
            Button fontButton = addRenderableWidget(Button.builder(Component.literal(family.name()), b -> {
                SkyLyricsClient.CONFIG.setFontFamily(family.id());
                open(Tab.STYLES);
            }).bounds(contentLeft + (i % fontCols) * (fontWidth + fontGap),
                    y + 184 + (i / fontCols) * 35, fontWidth, 29).build());
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

    private void information(int y) {
        action("PLAYER OVERVIEW",()->open(Tab.OVERVIEW),contentLeft,y+22,contentWidth,29);
        action("LYRICS CONTROLS",()->open(Tab.LYRICS),contentLeft,y+62,contentWidth,29);
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
        graphics.text(font,"CINEMATIC MUSIC EXPERIENCE",left+16,top+33,0xFFC7B9E8,false);
        graphics.text(font,tab.name(),contentLeft,top+78,0xFFFFFFFF,true);
        int activeY = top + 70 + tab.ordinal() * 34;
        graphics.fill(left + 10, activeY + 2, left + 13, activeY + 25, 0xFFB58BFF);
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
        } else if(tab==Tab.PLAYER) {\n            graphics.text(font, "LOCAL LYRICS LIBRARY", contentLeft, top + 98, 0xFFC6A0FF, true);\n            if (SkyLyricsClient.PLAYER.hasTrack()) {\n                graphics.text(font, SkyLyricsClient.PLAYER.track().title(), contentLeft, top + 124, 0xFFFFFFFF, true);\n                graphics.text(font, SkyLyricsClient.PLAYER.track().artist(), contentLeft, top + 143, 0xFFBDB7CD, false);\n                LyricsCue cue = SkyLyricsClient.PLAYER.currentCue();\n                if (cue != null) graphics.text(font, cue.text(), contentLeft, top + 164, SkyLyricsClient.CONFIG.accentColor(), true);\n                graphics.text(font, (SkyLyricsClient.PLAYER.isPlaying() ? "TIMELINE RUNNING" : "TIMELINE PAUSED") + "  ·  " + LyricsPlayer.formatTime(SkyLyricsClient.PLAYER.positionMs()) + " / " + LyricsPlayer.formatTime(SkyLyricsClient.PLAYER.durationMs()), contentLeft, top + 184, 0xFFD4D1E2, false);\n            } else {\n                graphics.text(font, "No LRC file loaded.", contentLeft, top + 128, 0xFFD4D1E2, false);\n            }\n            graphics.text(font, "Offline: load downloaded .lrc files from the folder above.", contentLeft, top + 218, 0xFFBDB7CD, false);\n            graphics.text(font, "Online: browser media metadata can sync supported YouTube lyrics.", contentLeft, top + 238, 0xFFBDB7CD, false);\n            graphics.text(font, "Audio playback is not yet provided by this timeline controller.", contentLeft, top + 260, 0xFFFFC27A, false);\n            graphics.text(font, "Touch or click the buttons; no chat commands are needed.", contentLeft, top + 282, 0xFF9BE7BF, false);\n        } else if(tab==Tab.SKIES) {
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
        } else if(tab==Tab.PERFORMANCE) {
            graphics.text(font,"Only clear background pixels are replaced by the sky shader.",contentLeft,top+190,0xFFD4D1E2,false);
            graphics.text(font,"Terrain, entities, and HUD pixels are passed through.",contentLeft,top+213,0xFFD4D1E2,false);
            graphics.text(font,"Rendering uses Minecraft's post-effect pipeline.",contentLeft,top+236,0xFFD4D1E2,false);
        } else if(tab==Tab.SETTINGS) {
            graphics.text(font,"Saved to config/skylyrics.properties",contentLeft,top+220,0xFFD4D1E2,false);
            graphics.text(font,"Open Options → Controls → SkyLyrics to rebind O, H and K.",contentLeft,top+244,0xFFD4D1E2,false);
        } else if(tab==Tab.INFORMATION) {
            graphics.text(font,"Fabric client mod · Minecraft Java 26.3",contentLeft,top+190,0xFFFFFFFF,true);
            graphics.text(font,"YouTube / YouTube Music: browser media + synced lyric lookup.",contentLeft,top+216,0xFFD4D1E2,false);
            graphics.text(font,"Spotify: Now Playing metadata only.",contentLeft,top+240,0xFFD4D1E2,false);
            graphics.text(font,"Local LRC timeline works offline; audio playback is a separate feature.",contentLeft,top+264,0xFFD4D1E2,false);
        }
    }
}
