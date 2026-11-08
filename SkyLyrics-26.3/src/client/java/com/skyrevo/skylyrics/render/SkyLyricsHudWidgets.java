package com.skyrevo.skylyrics.render;

import com.skyrevo.skylyrics.SkyLyricsClient;
import com.skyrevo.skylyrics.core.MusicHistory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import java.util.List;

/** Independent live HUD cards backed by the actual local player and history state. */
public final class SkyLyricsHudWidgets {
    private static final int VISUALIZER_WIDTH=232, VISUALIZER_HEIGHT=76;
    private static final int RECENT_WIDTH=238, RECENT_HEIGHT=92;
    private static final int STATS_WIDTH=174, STATS_HEIGHT=72;
    private SkyLyricsHudWidgets() {}

    public static void extract(GuiGraphicsExtractor g) {
        if(SkyLyricsClient.CONFIG==null||!SkyLyricsClient.CONFIG.enabled())return;
        if(SkyLyricsClient.CONFIG.visualizerHudEnabled())drawVisualizer(g);
        if(SkyLyricsClient.CONFIG.recentTracksHudEnabled())drawRecentTracks(g);
        if(SkyLyricsClient.CONFIG.listeningStatsHudEnabled())drawListeningStats(g);
    }

    private static void drawVisualizer(GuiGraphicsExtractor g) {
        Minecraft mc=Minecraft.getInstance();
        int w=Math.min(VISUALIZER_WIDTH,g.guiWidth()-12),h=VISUALIZER_HEIGHT;
        int x=left(g.guiWidth(),w,SkyLyricsClient.CONFIG.visualizerHudX());
        int y=top(g.guiHeight(),h,SkyLyricsClient.CONFIG.visualizerHudY());
        int accent=SkyLyricsClient.CONFIG.accentColor();
        boolean local=SkyLyricsClient.AUDIO.hasFile();
        boolean demo=SkyLyricsClient.isDemoActive();
        boolean playing=local?SkyLyricsClient.AUDIO.isPlaying():demo&&SkyLyricsClient.PLAYER.isPlaying();
        float level=local?SkyLyricsClient.AUDIO.visualizerLevel():0.0f;
        String mode=ui(local?(playing?"PCM AUDIO · LIVE":"PCM AUDIO · PAUSED")
                :demo?"PREVIEW TRANSPORT":"WAITING FOR TRACK");
        g.fill(x,y,x+w,y+h,0xD90D0B17);
        g.outline(x,y,w,h,withAlpha(accent,180));
        g.fill(x+2,y+2,x+w-2,y+4,withAlpha(accent,210));
        g.text(mc.font,ui("AUDIO VISUALIZER"),x+10,y+9,0xFFEDE5FF,true);
        g.text(mc.font,mode,x+10,y+22,0xFFBDB5CE,false);
        int baseline=y+57,bars=Math.max(8,Math.min(38,(w-22)/5));
        long clock=System.nanoTime();
        for(int i=0;i<bars;i++){
            double a=0.5+0.5*Math.sin(clock/125_000_000.0+i*0.54);
            double b=0.5+0.5*Math.sin(clock/213_000_000.0+i*0.23);
            double contour=0.18+0.82*(0.64*a+0.36*b);
            double amplitude=local?level:(demo&&playing?0.16:0.035);
            int bh=playing?2+(int)Math.round((3.0+amplitude*28.0)*contour):2+(int)Math.round(2.0*contour);
            int bx=x+11+i*5;
            g.fill(bx,baseline-bh,bx+3,baseline+bh/3,withAlpha(accent,playing?205:95));
        }
        g.fill(x+10,y+66,x+w-10,y+67,0x554D465C);
    }

    private static void drawRecentTracks(GuiGraphicsExtractor g) {
        Minecraft mc=Minecraft.getInstance();
        int w=Math.min(RECENT_WIDTH,g.guiWidth()-12),h=RECENT_HEIGHT;
        int x=left(g.guiWidth(),w,SkyLyricsClient.CONFIG.recentTracksHudX());
        int y=top(g.guiHeight(),h,SkyLyricsClient.CONFIG.recentTracksHudY());
        int accent=SkyLyricsClient.CONFIG.secondaryColor();
        g.fill(x,y,x+w,y+h,0xD90D0B17);g.outline(x,y,w,h,0xAA8C6EB5);
        g.fill(x+2,y+2,x+w-2,y+4,withAlpha(accent,210));
        g.text(mc.font,ui("RECENTLY PLAYED"),x+10,y+9,0xFFEDE5FF,true);
        List<MusicHistory.Entry> entries=SkyLyricsClient.HISTORY==null?List.of():SkyLyricsClient.HISTORY.recent(3);
        if(entries.isEmpty()){
            g.text(mc.font,ui("No listening history yet"),x+10,y+42,0xFFBDB5CE,false);
            g.text(mc.font,ui("Play a song to populate this card"),x+10,y+57,0xFF817B91,false);return;
        }
        for(int i=0;i<entries.size();i++){
            MusicHistory.Entry e=entries.get(i);
            String label=clip(e.title()+" · "+e.artist(),mc,w-20);
            int rowY=y+27+i*19;
            g.text(mc.font,label,x+10,rowY,0xFFF7F2FF,i==0);
            g.text(mc.font,timeAgo(e.playedAtEpochMs()),x+10,rowY+9,0xFF9F96B1,false);
        }
    }

    private static void drawListeningStats(GuiGraphicsExtractor g) {
        Minecraft mc=Minecraft.getInstance();
        int w=Math.min(STATS_WIDTH,g.guiWidth()-12),h=STATS_HEIGHT;
        int x=left(g.guiWidth(),w,SkyLyricsClient.CONFIG.listeningStatsHudX());
        int y=top(g.guiHeight(),h,SkyLyricsClient.CONFIG.listeningStatsHudY());
        int accent=SkyLyricsClient.CONFIG.accentColor();
        long plays=SkyLyricsClient.HISTORY==null?0L:SkyLyricsClient.HISTORY.totalPlays();
        int unique=SkyLyricsClient.HISTORY==null?0:SkyLyricsClient.HISTORY.uniqueTrackCount();
        g.fill(x,y,x+w,y+h,0xD90D0B17);g.outline(x,y,w,h,0xAA8C6EB5);
        g.fill(x+2,y+2,x+w-2,y+4,withAlpha(accent,210));
        g.text(mc.font,ui("LISTENING STATS"),x+10,y+9,0xFFEDE5FF,true);
        g.text(mc.font,Long.toString(plays),x+10,y+30,accent,true);
        g.text(mc.font,ui("PLAYS"),x+10,y+45,0xFFBDB5CE,false);
        g.text(mc.font,Integer.toString(unique),x+92,y+30,0xFFFFD9A1,true);
        g.text(mc.font,ui("UNIQUE"),x+92,y+45,0xFFBDB5CE,false);
    }
    private static int left(int screen,int panel,float center){return Math.max(4,Math.min(screen-panel-4,Math.round(screen*center)-panel/2));}
    private static int top(int screen,int panel,float value){return Math.max(4,Math.min(screen-panel-4,Math.round(screen*value)));}
    private static String clip(String value,Minecraft mc,int max){
        String result=value==null?"":value;
        while(!result.isEmpty()&&mc.font.width(result)>max)result=result.substring(0,result.length()-1);
        if(result.length()>1&&!result.equals(value))result=result.substring(0,result.length()-1)+"…";
        return result;
    }
    private static String timeAgo(long epoch){
        long s=Math.max(0L,(System.currentTimeMillis()-epoch)/1000L);
        if(s<60)return ui("just now");
        if(s<3600)return (s/60)+(SkyLyricsClient.CONFIG.isEnglish()?" min ago":" menit lalu");
        if(s<86400)return (s/3600)+(SkyLyricsClient.CONFIG.isEnglish()?" hr ago":" jam lalu");
        return (s/86400)+(SkyLyricsClient.CONFIG.isEnglish()?" d ago":" hari lalu");
    }
    private static String ui(String english){
        if(SkyLyricsClient.CONFIG==null||SkyLyricsClient.CONFIG.isEnglish())return english;
        return switch(english){
            case "AUDIO VISUALIZER" -> "VISUALISASI AUDIO";
            case "PCM AUDIO · LIVE" -> "AUDIO PCM · LANGSUNG";
            case "PCM AUDIO · PAUSED" -> "AUDIO PCM · DIJEDA";
            case "PREVIEW TRANSPORT" -> "TRANSPORT PRATINJAU";
            case "WAITING FOR TRACK" -> "MENUNGGU LAGU";
            case "RECENTLY PLAYED" -> "BARU DIPUTAR";
            case "No listening history yet" -> "Belum ada riwayat pemutaran";
            case "Play a song to populate this card" -> "Putar lagu untuk mengisi kartu ini";
            case "LISTENING STATS" -> "STATISTIK PEMUTARAN";
            case "PLAYS" -> "DIPUTAR";
            case "UNIQUE" -> "UNIK";
            case "just now" -> "baru saja";
            default -> english;
        };
    }
    private static int withAlpha(int c,int a){return (Math.max(0,Math.min(255,a))<<24)|(c&0x00FFFFFF);}
}
