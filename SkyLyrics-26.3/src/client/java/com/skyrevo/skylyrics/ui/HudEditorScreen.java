package com.skyrevo.skylyrics.ui;

import com.skyrevo.skylyrics.SkyLyricsClient;
import com.skyrevo.skylyrics.core.MusicHistory;
import com.skyrevo.skylyrics.render.AlbumArtManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import java.util.List;

/** Drag-and-drop layout studio for five independently configurable HUD cards. */
public final class HudEditorScreen extends Screen {
    private enum Element { NONE,NOW_PLAYING,LYRICS,VISUALIZER,RECENT_TRACKS,LISTENING_STATS }
    private record Box(int x,int y,int width,int height){}
    private final Screen parent;
    private Element dragging=Element.NONE;
    private float originX,originY;
    public HudEditorScreen(Screen parent){super(Component.literal(ui("SkyLyrics HUD Layout Studio")));this.parent=parent;}

    @Override protected void init(){
        int gap=4,bw=Math.max(66,(width-32-gap*2)/3),start=(width-(bw*3+gap*2))/2,y=height-59;
        addToggle("VIS "+onOff(SkyLyricsClient.CONFIG.visualizerHudEnabled()),start,y,bw,SkyLyricsClient.CONFIG::toggleVisualizerHudEnabled);
        addToggle("RECENT "+onOff(SkyLyricsClient.CONFIG.recentTracksHudEnabled()),start+bw+gap,y,bw,SkyLyricsClient.CONFIG::toggleRecentTracksHudEnabled);
        addToggle("STATS "+onOff(SkyLyricsClient.CONFIG.listeningStatsHudEnabled()),start+(bw+gap)*2,y,bw,SkyLyricsClient.CONFIG::toggleListeningStatsHudEnabled);
        int aw=Math.min(112,Math.max(82,(width-36)/2-5)),ag=8,ax=(width-(aw*2+ag))/2,ay=height-31;
        addRenderableWidget(Button.builder(Component.literal(ui("RESET ALL")),b->{SkyLyricsClient.CONFIG.resetHudPositions();dragging=Element.NONE;}).bounds(ax,ay,aw,22).build());
        addRenderableWidget(Button.builder(Component.literal(ui("DONE")),b->onClose()).bounds(ax+aw+ag,ay,aw,22).build());
    }
    private void addToggle(String label,int x,int y,int w,Runnable action){
        addRenderableWidget(Button.builder(Component.literal(ui(label)),b->{action.run();if(minecraft!=null)minecraft.gui.setScreen(new HudEditorScreen(parent));}).bounds(x,y,w,22).build());
    }

    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float delta){
        super.extractBackground(g,mx,my,delta);g.fill(0,0,width,height,0xE9080712);
        for(int x=0;x<width;x+=32)g.fill(x,0,x+1,height,0x182F2947);
        for(int y=0;y<height;y+=32)g.fill(0,y,width,y+1,0x182F2947);
        drawNow(g);drawLyrics(g);drawVisualizer(g);drawRecent(g);drawStats(g);
        g.fill(0,height-64,width,height,0xD90A0913);g.fill(0,height-64,width,height-63,0xFF59436F);
    }
    private void drawNow(GuiGraphicsExtractor g){
        Box b=box(SkyLyricsClient.CONFIG.nowPlayingHudX(),SkyLyricsClient.CONFIG.nowPlayingHudY(),380,58);
        g.fill(b.x,b.y,b.x+b.width,b.y+b.height,0xE00D0B18);g.outline(b.x,b.y,b.width,b.height,border(Element.NOW_PLAYING,true));
        g.fill(b.x+2,b.y+2,b.x+b.width-2,b.y+4,0xFFB58BFF);g.fill(b.x+10,b.y+11,b.x+46,b.y+47,0xFF29203D);
        if(AlbumArtManager.hasTexture()) {
            g.blit(RenderPipelines.GUI_TEXTURED,AlbumArtManager.TEXTURE_ID,b.x+10,b.y+11,0.0F,0.0F,36,36,36,36);
        } else {
            g.centeredText(font,"♪",b.x+28,b.y+23,0xFFFFD9FC);
        }
        g.text(font,ui("NOW PLAYING · SKYLYRICS"),b.x+56,b.y+10,0xFFCDBAF7,true);
        String title;
        if(SkyLyricsClient.AUDIO.hasFile() && SkyLyricsClient.AUDIO.currentFile()!=null) {
            title=SkyLyricsClient.AUDIO.currentFile().getFileName().toString();
        } else if(SkyLyricsClient.CURRENT_MEDIA!=null && SkyLyricsClient.CURRENT_MEDIA.available()) {
            title=SkyLyricsClient.CURRENT_MEDIA.title();
        } else if(SkyLyricsClient.PLAYER.hasTrack()) {
            title=SkyLyricsClient.PLAYER.track().title();
        } else {
            title=ui("Drag this card to move it");
        }
        g.text(font,title,b.x+56,b.y+24,0xFFFFFCF7,true);g.text(font,ui("Player / artwork / waveform"),b.x+56,b.y+39,0xFFBDB7CD,false);
    }
    private void drawLyrics(GuiGraphicsExtractor g){
        int cy=Math.round(height*SkyLyricsClient.CONFIG.verticalPosition());
        Box b=box(SkyLyricsClient.CONFIG.lyricsHudX(),(cy-30.0f)/Math.max(1,height),Math.min(520,width-32),76);
        g.fill(b.x,b.y,b.x+b.width,b.y+b.height,0xA9161226);g.outline(b.x,b.y,b.width,b.height,border(Element.LYRICS,true));
        g.fill(b.x,b.y,b.x+b.width,b.y+2,0xFFEA8DDA);
        String lyric=SkyLyricsClient.PLAYER.currentCue()==null?ui("Drag this lyric panel"):SkyLyricsClient.PLAYER.currentCue().text();
        if(lyric.length()>64)lyric=lyric.substring(0,61)+"...";
        g.centeredText(font,lyric,b.x+b.width/2,b.y+38,SkyLyricsClient.CONFIG.accentColor());
        g.centeredText(font,ui("LYRICS HUD · KARAOKE / CONTEXT LINES"),b.x+b.width/2,b.y+13,0xFFCAC1DA);
    }
    private void drawVisualizer(GuiGraphicsExtractor g){
        Box b=box(SkyLyricsClient.CONFIG.visualizerHudX(),SkyLyricsClient.CONFIG.visualizerHudY(),232,76);
        g.fill(b.x,b.y,b.x+b.width,b.y+b.height,0xD90D0B17);g.outline(b.x,b.y,b.width,b.height,border(Element.VISUALIZER,SkyLyricsClient.CONFIG.visualizerHudEnabled()));
        g.fill(b.x+2,b.y+2,b.x+b.width-2,b.y+4,0xFFC6A0FF);g.text(font,ui("AUDIO VISUALIZER"),b.x+10,b.y+10,0xFFEDE5FF,true);
        g.text(font,SkyLyricsClient.CONFIG.visualizerHudEnabled()?ui("PCM / TRANSPORT MODE"):ui("HIDDEN · ENABLE BELOW"),b.x+10,b.y+23,0xFFBDB5CE,false);
        int base=b.y+57;long phase=System.nanoTime();
        for(int i=0;i<34;i++){int bx=b.x+10+i*6,h=3+(int)Math.round((0.5+0.5*Math.sin(phase/140_000_000.0+i*0.55))*16);if(bx+3<b.x+b.width-8)g.fill(bx,base-h,bx+3,base+2,0xFFD0A9FF);}
    }
    private void drawRecent(GuiGraphicsExtractor g){
        Box b=box(SkyLyricsClient.CONFIG.recentTracksHudX(),SkyLyricsClient.CONFIG.recentTracksHudY(),238,92);
        g.fill(b.x,b.y,b.x+b.width,b.y+b.height,0xD90D0B17);g.outline(b.x,b.y,b.width,b.height,border(Element.RECENT_TRACKS,SkyLyricsClient.CONFIG.recentTracksHudEnabled()));
        g.fill(b.x+2,b.y+2,b.x+b.width-2,b.y+4,0xFFE18BCB);g.text(font,ui("RECENTLY PLAYED"),b.x+10,b.y+10,0xFFEDE5FF,true);
        List<MusicHistory.Entry> entries=SkyLyricsClient.HISTORY==null?List.of():SkyLyricsClient.HISTORY.recent(3);
        if(entries.isEmpty())g.text(font,ui("No listening history yet"),b.x+10,b.y+43,0xFFBDB5CE,false);
        else for(int i=0;i<entries.size();i++){MusicHistory.Entry e=entries.get(i);String t=e.title()+" · "+e.artist();while(font.width(t)>b.width-20&&!t.isEmpty())t=t.substring(0,t.length()-1);g.text(font,t,b.x+10,b.y+28+i*18,0xFFF7F2FF,i==0);}
    }
    private void drawStats(GuiGraphicsExtractor g){
        Box b=box(SkyLyricsClient.CONFIG.listeningStatsHudX(),SkyLyricsClient.CONFIG.listeningStatsHudY(),174,72);
        long plays=SkyLyricsClient.HISTORY==null?0L:SkyLyricsClient.HISTORY.totalPlays();int unique=SkyLyricsClient.HISTORY==null?0:SkyLyricsClient.HISTORY.uniqueTrackCount();
        g.fill(b.x,b.y,b.x+b.width,b.y+b.height,0xD90D0B17);g.outline(b.x,b.y,b.width,b.height,border(Element.LISTENING_STATS,SkyLyricsClient.CONFIG.listeningStatsHudEnabled()));
        g.fill(b.x+2,b.y+2,b.x+b.width-2,b.y+4,0xFFC6A0FF);g.text(font,ui("LISTENING STATS"),b.x+10,b.y+10,0xFFEDE5FF,true);
        g.text(font,ui("PLAYS")+"  "+plays,b.x+10,b.y+32,0xFFE9DFFF,true);g.text(font,ui("UNIQUE")+"  "+unique,b.x+10,b.y+47,0xFFFFD9A1,false);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
        super.extractRenderState(g,mx,my,delta);g.text(font,ui("HUD LAYOUT STUDIO"),18,16,0xFFFFFFFF,true);
        g.text(font,ui("Drag a panel with left-click. Double-click one panel to reset it."),18,34,0xFFD4D1E2,false);
        g.text(font,ui("Right-click while dragging cancels. Toggle optional cards below."),18,49,0xFFBDB7CD,false);
        String s=ui(switch(dragging){case NOW_PLAYING->"MOVING · NOW PLAYING";case LYRICS->"MOVING · LYRICS HUD";case VISUALIZER->"MOVING · VISUALIZER";case RECENT_TRACKS->"MOVING · RECENT TRACKS";case LISTENING_STATS->"MOVING · LISTENING STATS";default->"READY · FIVE INDEPENDENT WIDGETS";});
        g.text(font,s,18,height-80,dragging==Element.NONE?0xFF9BE7BF:0xFFFFD58A,true);
        g.text(font,ui("VIS / RECENT / STATS toggles control what appears in-game"),18,height-68,0xFFD4D1E2,false);
    }
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean doubleClick){
        if(e.button()==1&&dragging!=Element.NONE){restoreDragOrigin();dragging=Element.NONE;return true;}
        if(e.button()!=0)return super.mouseClicked(e,doubleClick);Element target=hit(e.x(),e.y());if(target==Element.NONE)return super.mouseClicked(e,doubleClick);
        if(doubleClick){reset(target);return true;}dragging=target;originX=anchorX(target);originY=anchorY(target);return true;
    }
    @Override public boolean mouseDragged(MouseButtonEvent e,double dx,double dy){
        if(dragging==Element.NONE||e.button()!=0)return super.mouseDragged(e,dx,dy);
        Box b=bounds(dragging);float minX=b.width/(2f*Math.max(1,width)),maxX=1f-minX,minY=4f/Math.max(1,height),maxY=1f-(b.height+4f)/Math.max(1,height);
        preview(dragging,clamp(anchorX(dragging)+(float)dx/Math.max(1,width),minX,maxX),clamp(anchorY(dragging)+(float)dy/Math.max(1,height),minY,maxY));return true;
    }
    @Override public boolean mouseReleased(MouseButtonEvent e){if(dragging!=Element.NONE){SkyLyricsClient.CONFIG.saveHudPositions();dragging=Element.NONE;return true;}return super.mouseReleased(e);}
    @Override public boolean keyPressed(KeyEvent e){if(e.isEscape()){onClose();return true;}return super.keyPressed(e);}
    private Element hit(double x,double y){Element[] order={Element.LYRICS,Element.NOW_PLAYING,Element.RECENT_TRACKS,Element.LISTENING_STATS,Element.VISUALIZER};for(Element e:order){Box b=bounds(e);if(x>=b.x&&x<=b.x+b.width&&y>=b.y&&y<=b.y+b.height)return e;}return Element.NONE;}
    private Box bounds(Element e){return switch(e){
        case NOW_PLAYING->box(SkyLyricsClient.CONFIG.nowPlayingHudX(),SkyLyricsClient.CONFIG.nowPlayingHudY(),380,58);
        case LYRICS->{int cy=Math.round(height*SkyLyricsClient.CONFIG.verticalPosition());yield box(SkyLyricsClient.CONFIG.lyricsHudX(),(cy-30f)/Math.max(1,height),Math.min(520,width-32),76);}
        case VISUALIZER->box(SkyLyricsClient.CONFIG.visualizerHudX(),SkyLyricsClient.CONFIG.visualizerHudY(),232,76);
        case RECENT_TRACKS->box(SkyLyricsClient.CONFIG.recentTracksHudX(),SkyLyricsClient.CONFIG.recentTracksHudY(),238,92);
        case LISTENING_STATS->box(SkyLyricsClient.CONFIG.listeningStatsHudX(),SkyLyricsClient.CONFIG.listeningStatsHudY(),174,72);
        default->new Box(0,0,0,0);};}
    private Box box(float cx,float ty,int dw,int dh){int w=Math.max(1,Math.min(dw,width-12)),h=Math.min(dh,Math.max(1,height-72));int x=Math.max(4,Math.min(width-w-4,Math.round(width*cx)-w/2));int y=Math.max(4,Math.min(height-h-68,Math.round(height*ty)));return new Box(x,y,w,h);}
    private void preview(Element e,float x,float y){switch(e){
        case NOW_PLAYING->SkyLyricsClient.CONFIG.previewNowPlayingHudPosition(x,y);
        case LYRICS->SkyLyricsClient.CONFIG.previewLyricsHudPosition(x,y+30f/Math.max(1,height));
        case VISUALIZER->SkyLyricsClient.CONFIG.previewVisualizerHudPosition(x,y);
        case RECENT_TRACKS->SkyLyricsClient.CONFIG.previewRecentTracksHudPosition(x,y);
        case LISTENING_STATS->SkyLyricsClient.CONFIG.previewListeningStatsHudPosition(x,y);
        default->{}}}
    private float anchorX(Element e){return switch(e){case NOW_PLAYING->SkyLyricsClient.CONFIG.nowPlayingHudX();case LYRICS->SkyLyricsClient.CONFIG.lyricsHudX();case VISUALIZER->SkyLyricsClient.CONFIG.visualizerHudX();case RECENT_TRACKS->SkyLyricsClient.CONFIG.recentTracksHudX();case LISTENING_STATS->SkyLyricsClient.CONFIG.listeningStatsHudX();default->0.5f;};}
    private float anchorY(Element e){return switch(e){case NOW_PLAYING->SkyLyricsClient.CONFIG.nowPlayingHudY();case LYRICS->SkyLyricsClient.CONFIG.verticalPosition()-30f/Math.max(1,height);case VISUALIZER->SkyLyricsClient.CONFIG.visualizerHudY();case RECENT_TRACKS->SkyLyricsClient.CONFIG.recentTracksHudY();case LISTENING_STATS->SkyLyricsClient.CONFIG.listeningStatsHudY();default->0.5f;};}
    private void reset(Element e){switch(e){
        case NOW_PLAYING->SkyLyricsClient.CONFIG.setNowPlayingHudPosition(0.5f,0.025f);
        case LYRICS->SkyLyricsClient.CONFIG.setLyricsHudPosition(0.5f,0.38f);
        case VISUALIZER->SkyLyricsClient.CONFIG.setVisualizerHudPosition(0.5f,0.80f);
        case RECENT_TRACKS->SkyLyricsClient.CONFIG.setRecentTracksHudPosition(0.15f,0.66f);
        case LISTENING_STATS->SkyLyricsClient.CONFIG.setListeningStatsHudPosition(0.85f,0.66f);
        default->{}}}
    private void restoreDragOrigin(){preview(dragging,originX,originY);SkyLyricsClient.CONFIG.saveHudPositions();}
    private int border(Element e,boolean enabled){return !enabled?0x66817B91:dragging==e?0xFFFFD58A:0xFF8067A7;}
    private static String onOff(boolean v){return ui(v?"ON":"OFF");}
    private static String ui(String english){
        if(SkyLyricsClient.CONFIG==null||SkyLyricsClient.CONFIG.isEnglish())return english;
        if(english.startsWith("VIS "))return "VISUALISASI "+english.substring("VIS ".length());
        if(english.startsWith("RECENT "))return "TERBARU "+english.substring("RECENT ".length());
        if(english.startsWith("STATS "))return "STATISTIK "+english.substring("STATS ".length());
        return switch(english){
            case "SkyLyrics HUD Layout Studio" -> "Studio Tata Letak HUD SkyLyrics";
            case "RESET ALL" -> "ATUR ULANG SEMUA";
            case "DONE" -> "SELESAI";
            case "RECENT " -> "TERBARU ";
            case "STATS " -> "STATISTIK ";
            case "NOW PLAYING · SKYLYRICS" -> "SEDANG DIPUTAR · SKYLYRICS";
            case "Drag this card to move it" -> "Geser kartu ini untuk memindahkannya";
            case "Player / artwork / waveform" -> "Pemutar / sampul / gelombang audio";
            case "Drag this lyric panel" -> "Geser panel lirik ini";
            case "LYRICS HUD · KARAOKE / CONTEXT LINES" -> "HUD LIRIK · KARAOKE / BARIS KONTEKS";
            case "AUDIO VISUALIZER" -> "VISUALISASI AUDIO";
            case "PCM / TRANSPORT MODE" -> "MODE PCM / TRANSPORT";
            case "HIDDEN · ENABLE BELOW" -> "TERSEMBUNYI · AKTIFKAN DI BAWAH";
            case "RECENTLY PLAYED" -> "BARU DIPUTAR";
            case "No listening history yet" -> "Belum ada riwayat pemutaran";
            case "LISTENING STATS" -> "STATISTIK PEMUTARAN";
            case "PLAYS" -> "DIPUTAR";
            case "UNIQUE" -> "UNIK";
            case "HUD LAYOUT STUDIO" -> "STUDIO TATA LETAK HUD";
            case "Drag a panel with left-click. Double-click one panel to reset it." -> "Geser panel dengan klik kiri. Klik dua kali panel untuk mengatur ulang.";
            case "Right-click while dragging cancels. Toggle optional cards below." -> "Klik kanan saat menggeser untuk membatalkan. Atur kartu tambahan di bawah.";
            case "MOVING · NOW PLAYING" -> "MEMINDAHKAN · SEDANG DIPUTAR";
            case "MOVING · LYRICS HUD" -> "MEMINDAHKAN · HUD LIRIK";
            case "MOVING · VISUALIZER" -> "MEMINDAHKAN · VISUALISASI";
            case "MOVING · RECENT TRACKS" -> "MEMINDAHKAN · LAGU TERBARU";
            case "MOVING · LISTENING STATS" -> "MEMINDAHKAN · STATISTIK";
            case "READY · FIVE INDEPENDENT WIDGETS" -> "SIAP · LIMA WIDGET TERPISAH";
            case "VIS / RECENT / STATS toggles control what appears in-game" -> "Tombol VIS / TERBARU / STATISTIK mengatur kartu yang tampil di game";
            case "ON" -> "AKTIF";
            case "OFF" -> "NONAKTIF";
            default -> english;
        };
    }
    private static float clamp(float v,float min,float max){return Math.max(min,Math.min(max,v));}
    @Override public void onClose(){SkyLyricsClient.CONFIG.saveHudPositions();if(minecraft!=null)minecraft.gui.setScreen(parent);}
}
