package com.skyrevo.skylyrics.render;

import com.skyrevo.skylyrics.SkyLyricsClient;
import com.skyrevo.skylyrics.core.LyricsCue;
import com.skyrevo.skylyrics.core.LyricsPlayer;
import com.skyrevo.skylyrics.integration.MediaStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.DeltaTracker;

public final class SkyLyricsHud {
    private SkyLyricsHud() {}

    public static void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (SkyLyricsClient.CONFIG == null || !SkyLyricsClient.CONFIG.enabled()) return;
        LyricsPlayer player = SkyLyricsClient.PLAYER;
        if (player.hasTrack() && SkyLyricsClient.CONFIG.worldLyrics()) return;
        if (!player.hasTrack()) {
            MediaStatus media = SkyLyricsClient.CURRENT_MEDIA;
            if (media != null && media.available()) drawNowPlaying(graphics, media);
            return;
        }

        int index = player.currentCueIndex();
        LyricsCue current = player.cueAt(index);
        if (current == null || current.text().isBlank()) return;
        Minecraft minecraft = Minecraft.getInstance();
        int width = graphics.guiWidth(), height = graphics.guiHeight(), centerX = width / 2;
        int accent = SkyLyricsClient.CONFIG.accentColor(), secondary = SkyLyricsClient.CONFIG.secondaryColor();
        int mainY = Math.round(height * SkyLyricsClient.CONFIG.verticalPosition());
        float scale = SkyLyricsClient.CONFIG.scale();
        int style = SkyLyricsClient.CONFIG.textStyle();
        int mainColor = switch (style) {
            case 1 -> 0xFFFFF0DE; case 2 -> 0xFFFFFFFF; case 3 -> secondary;
            case 4 -> 0xFFFFE6C1; case 5 -> 0xFFFFD58A; default -> accent;
        };
        int panelAlpha = SkyLyricsClient.CONFIG.showBackdrop() ? SkyLyricsClient.CONFIG.opacity() : 0;
        if (panelAlpha > 0) {
            int panelTop=mainY-Math.round(30*scale), panelBottom=mainY+Math.round(46*scale);
            int panelLeft=Math.max(10,width/2-Math.min(width/2-12,250));
            int panelRight=Math.min(width-10,width/2+Math.min(width/2-12,250));
            graphics.fill(panelLeft,panelTop,panelRight,panelBottom,argb(panelAlpha,16,13,29));
            graphics.fill(panelLeft,panelTop,panelRight,panelTop+2,withAlpha(secondary,Math.min(180,panelAlpha+55)));
        }
        String currentText=fit(current.text(),minecraft,width,scale);
        String previousText=null,nextText=null;
        if(SkyLyricsClient.CONFIG.showContextLines()) {
            LyricsCue previous=player.cueAt(index-1), next=player.cueAt(index+1);
            if(previous!=null) previousText=fit(previous.text(),minecraft,width,scale);
            if(next!=null) nextText=fit(next.text(),minecraft,width,scale);
        }
        graphics.pose().pushMatrix();
        graphics.pose().translate(centerX,mainY);
        graphics.pose().scale(scale,scale);
        if(SkyLyricsClient.CONFIG.glowEnabled()) {
            int glow=withAlpha(accent,72);
            graphics.centeredText(minecraft.font,currentText,0,-1,glow);
            graphics.centeredText(minecraft.font,currentText,0,1,glow);
            graphics.centeredText(minecraft.font,currentText,-1,0,glow);
            graphics.centeredText(minecraft.font,currentText,1,0,glow);
        }
        graphics.centeredText(minecraft.font,currentText,0,0,mainColor);
        graphics.pose().pushMatrix();
        graphics.pose().scale(0.84f,0.84f);
        if(previousText!=null) graphics.centeredText(minecraft.font,previousText,0,-27,withAlpha(0xFFE5E9F4,155));
        if(nextText!=null) graphics.centeredText(minecraft.font,nextText,0,27,withAlpha(0xFFD8DDE9,135));
        String title=player.track().title()+"  ·  "+player.track().artist();
        if(title.length()>58) title=title.substring(0,55)+"...";
        graphics.centeredText(minecraft.font,title,0,-50,withAlpha(0xFFFFFFFF,185));
        graphics.pose().popMatrix(); graphics.pose().popMatrix();
        int barWidth=Math.min(220,Math.max(110,width/4)), barX=centerX-barWidth/2, barY=mainY+Math.round(43*scale);
        graphics.fill(barX,barY,barX+barWidth,barY+2,argb(90,255,255,255));
        int progress=player.durationMs()<=0?0:(int)(barWidth*(double)player.positionMs()/player.durationMs());
        graphics.fill(barX,barY,barX+Math.min(barWidth,progress),barY+2,accent);
        String status=(player.isPlaying()?"SYNC  ":"PAUSED  ")+LyricsPlayer.formatTime(player.positionMs())
                +" / "+LyricsPlayer.formatTime(player.durationMs());
        graphics.centeredText(minecraft.font,status,centerX,barY+5,withAlpha(0xFFE5E5EF,170));
    }

    private static void drawNowPlaying(GuiGraphicsExtractor graphics,MediaStatus media) {
        Minecraft minecraft=Minecraft.getInstance();
        int width=graphics.guiWidth(),centerX=width/2;
        String title=media.title().isBlank()?"Unknown track":media.title();
        String artist=media.artist().isBlank()?media.displaySource():media.artist();
        if(minecraft.font.width(title)>width-48) {
            while(!title.isEmpty()&&minecraft.font.width(title+"…")>width-48) title=title.substring(0,title.length()-1);
            title+="…";
        }
        int y=Math.round(graphics.guiHeight()*SkyLyricsClient.CONFIG.verticalPosition());
        graphics.fill(Math.max(10,centerX-220),y-28,Math.min(width-10,centerX+220),y+32,0xB9101020);
        graphics.centeredText(minecraft.font,"NOW PLAYING  ·  "+media.displaySource(),centerX,y-21,0xFFBDA9FF);
        graphics.centeredText(minecraft.font,title,centerX,y-5,0xFFFFFFFF);
        graphics.centeredText(minecraft.font,artist,centerX,y+10,0xFFCBC7D8);
        if(media.isSpotify()) graphics.centeredText(minecraft.font,"Track information only",centerX,y+23,0xFF9B96AA);
        else if(SkyLyricsClient.MEDIA_NOTICE!=null&&!SkyLyricsClient.MEDIA_NOTICE.isBlank())
            graphics.centeredText(minecraft.font,SkyLyricsClient.MEDIA_NOTICE,centerX,y+23,0xFF9B96AA);
    }

    private static String fit(String text,Minecraft minecraft,int screenWidth,float scale) {
        int maxWidth=Math.max(80,(int)(screenWidth*0.84f/Math.max(0.75f,scale)));
        if(minecraft.font.width(text)<=maxWidth) return text;
        String clipped=text;
        while(!clipped.isEmpty()&&minecraft.font.width(clipped+"…")>maxWidth) clipped=clipped.substring(0,clipped.length()-1);
        return clipped.isEmpty()?"…":clipped+"…";
    }
    private static int argb(int a,int r,int g,int b) { return (clamp(a)<<24)|(clamp(r)<<16)|(clamp(g)<<8)|clamp(b); }
    private static int withAlpha(int c,int a) { return (clamp(a)<<24)|(c&0x00FFFFFF); }
    private static int clamp(int n) { return Math.max(0,Math.min(255,n)); }
}
