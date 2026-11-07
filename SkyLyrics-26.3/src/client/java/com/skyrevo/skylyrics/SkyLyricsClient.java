package com.skyrevo.skylyrics;

import com.mojang.blaze3d.platform.InputConstants;
import com.skyrevo.skylyrics.core.LyricsLibrary;
import com.skyrevo.skylyrics.core.MusicLibrary;
import com.skyrevo.skylyrics.core.MusicHistory;
import com.skyrevo.skylyrics.core.LyricsPlayer;
import com.skyrevo.skylyrics.core.LyricsTrack;
import com.skyrevo.skylyrics.core.SkyLyricsConfig;
import com.skyrevo.skylyrics.integration.LyricsLookupClient;
import com.skyrevo.skylyrics.integration.MediaStatus;
import com.skyrevo.skylyrics.integration.BrowserMediaServer;
import com.skyrevo.skylyrics.render.SkyLyricsHud;
import com.skyrevo.skylyrics.render.SkyEffectController;
import com.skyrevo.skylyrics.render.AlbumArtManager;
import com.skyrevo.skylyrics.audio.LocalAudioEngine;
import com.skyrevo.skylyrics.ui.SkyLyricsScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.awt.Desktop;
import java.net.URI;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public final class SkyLyricsClient implements ClientModInitializer {
    public static final String MOD_ID = "skylyrics";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static SkyLyricsConfig CONFIG;
    public static LyricsLibrary LIBRARY;
    public static MusicLibrary MUSIC_LIBRARY;
    public static MusicHistory HISTORY;
    public static final LocalAudioEngine AUDIO = new LocalAudioEngine();
    public static final LyricsPlayer PLAYER = new LyricsPlayer();
    public static final BrowserMediaServer MEDIA_BRIDGE = new BrowserMediaServer();
    public static volatile MediaStatus CURRENT_MEDIA;
    public static volatile String MEDIA_NOTICE = "Browser connector not connected";
    private static final LyricsLookupClient LYRICS_LOOKUP = new LyricsLookupClient();
    private static String activeMediaIdentity = "";
    private static String lastHistoryIdentity = "";
    private static boolean lookupRunning;
    private static long lookupGeneration;
    private static long lastExternalTimestamp;
    private static Object lastAppliedSkyLevel;
    private static boolean demoActive;
    private static boolean localAudioMode;

    private static final String DEMO_LRC = """
            [ti:Golden Hour Preview]
            [ar:SkyLyrics Studio]
            [offset:0]
            [00:00.00]Golden light is waking
            [00:03.20]Across the quiet horizon
            [00:06.40]Every color finds its way
            [00:09.60]We are floating through this moment
            [00:12.80]Let the evening open wide
            [00:16.00]Stars are writing over us
            [00:19.20]Breathe in the silver silence
            [00:22.40]Feel the world begin to glow
            [00:25.60]Hold this feeling close
            [00:28.80]We are right where light meets sky
            [00:32.00]And the dream goes on
            [00:35.20]Golden hour never fades
            """;

    private static KeyMapping openMenuKey;
    private static KeyMapping toggleHudKey;
    private static KeyMapping playPauseKey;
    private static KeyMapping nextTrackKey;
    private static KeyMapping previousTrackKey;

    @Override
    public void onInitializeClient() {
        Path configDirectory = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir();
        CONFIG = new SkyLyricsConfig(configDirectory);
        LIBRARY = new LyricsLibrary(configDirectory);
        MUSIC_LIBRARY = new MusicLibrary(configDirectory);
        HISTORY = new MusicHistory(configDirectory);
        MEDIA_BRIDGE.start();

        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(MOD_ID, "main")
        );
        openMenuKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.skylyrics.open_menu", InputConstants.Type.KEYBOARD, InputConstants.KEY_O, category));
        toggleHudKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.skylyrics.toggle_hud", InputConstants.Type.KEYBOARD, InputConstants.KEY_H, category));
        playPauseKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.skylyrics.play_pause", InputConstants.Type.KEYBOARD, InputConstants.KEY_K, category));
        nextTrackKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.skylyrics.next_track", InputConstants.Type.KEYBOARD, InputConstants.KEY_N, category));
        previousTrackKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.skylyrics.previous_track", InputConstants.Type.KEYBOARD, InputConstants.KEY_B, category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level == null) {
                lastAppliedSkyLevel = null;
            } else if (client.level != lastAppliedSkyLevel) {
                lastAppliedSkyLevel = client.level;
                SkyEffectController.refreshFromConfig();
                LOGGER.info("Applied SkyLyrics sky preset: {}", CONFIG.skyPreset());
            }
            if (!demoActive) updateMediaIntegration();
            syncLocalAudioClock();
            while (openMenuKey.consumeClick()) {
                client.gui.setScreen(new SkyLyricsScreen());
            }
            while (toggleHudKey.consumeClick()) {
                CONFIG.toggleEnabled();
            }
            while (playPauseKey.consumeClick()) {
                togglePlayback();
            }
            while (nextTrackKey.consumeClick()) nextLocalTrack();
            while (previousTrackKey.consumeClick()) previousLocalTrack();
        });

        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(MOD_ID, "lyrics_hud"),
                SkyLyricsHud::extract
        );

        LOGGER.info("SkyLyrics loaded: {} local LRC track(s) found.", LIBRARY.listTracks().size());
    }

    private static void updateMediaIntegration() {
        if (demoActive || localAudioMode) return;
        MediaStatus media = MEDIA_BRIDGE.latest();
        CURRENT_MEDIA = media;
        AlbumArtManager.update(media);
        if (media == null) {
            if (!activeMediaIdentity.isEmpty()) {
                lookupGeneration++;
                lookupRunning = false;
            }
            if (activeMediaIdentity.startsWith("youtube_music|") || activeMediaIdentity.startsWith("youtube|")) PLAYER.clear();
            activeMediaIdentity = "";
            lastHistoryIdentity = "";
            lastExternalTimestamp = 0;
            MEDIA_NOTICE = MEDIA_BRIDGE.statusMessage();
            return;
        }

        String identity = media.identity();
        if (!identity.equals(activeMediaIdentity)) {
            activeMediaIdentity = identity;
            lastExternalTimestamp = 0;
            lookupGeneration++;
            lookupRunning = false;
            if (media.isSpotify()) {
                PLAYER.clear();
                MEDIA_NOTICE = "Spotify connected: metadata only; synced lyrics are disabled.";
            } else if (media.isYouTubeSource()) {
                PLAYER.clear();
                MEDIA_NOTICE = "YouTube detected: looking for synced lyrics…";
                requestYouTubeLyrics(media, lookupGeneration);
            } else {
                MEDIA_NOTICE = "Media detected. Use a local .lrc file for lyric timing.";
            }
        }

        if (media.playing() && !identity.equals(lastHistoryIdentity)) {
            HISTORY.recordPlay(media.title(), media.artist(), media.displaySource());
            lastHistoryIdentity = identity;
        }

        if (media.isYouTubeSource() && PLAYER.hasTrack()
                && media.updatedAtEpochMs() != lastExternalTimestamp) {
            PLAYER.syncExternal(media.positionMs(), media.durationMs(), media.playing());
            lastExternalTimestamp = media.updatedAtEpochMs();
        }
    }

    private static void requestYouTubeLyrics(MediaStatus media, long generation) {
        if (lookupRunning) return;
        lookupRunning = true;
        String requestedIdentity = media.identity();
        CompletableFuture.supplyAsync(() -> {
            try {
                return LYRICS_LOOKUP.lookup(media);
            } catch (Exception exception) {
                throw new java.util.concurrent.CompletionException(exception);
            }
        }).whenComplete((track, error) -> Minecraft.getInstance().execute(() -> {
            if (generation != lookupGeneration) return;
            lookupRunning = false;
            MediaStatus current = CURRENT_MEDIA;
            if (current == null || !requestedIdentity.equals(current.identity())) return;
            if (error == null && track != null) {
                PLAYER.load(track);
                PLAYER.syncExternal(current.positionMs(), current.durationMs(), current.playing());
                lastExternalTimestamp = current.updatedAtEpochMs();
                MEDIA_NOTICE = "Synced lyrics loaded automatically.";
                LOGGER.info("Loaded synced lyrics for {} - {}", track.title(), track.artist());
            } else {
                Throwable cause = error == null ? null : error.getCause();
                MEDIA_NOTICE = cause == null ? "No synced lyrics were found. Add an LRC file manually."
                        : cause.getMessage() == null ? "Lyrics lookup failed." : cause.getMessage();
                LOGGER.info("Automatic lyrics lookup: {}", MEDIA_NOTICE);
            }
        }));
    }

    public static void loadFirstAvailableTrack() {
        for (Path path : LIBRARY.listTracks()) {
            try {
                PLAYER.load(LyricsTrack.load(path));
                return;
            } catch (IOException ex) {
                LOGGER.warn("Skipping invalid LRC file {}: {}", path.getFileName(), ex.getMessage());
            }
        }
    }

    public static void startDemo() {
        try {
            AUDIO.clear();
            localAudioMode = false;
            demoActive = true;
            activeMediaIdentity = "";
            CURRENT_MEDIA = null;
            AlbumArtManager.update(null);
            PLAYER.load(LyricsTrack.fromLrcText(
                    DEMO_LRC,
                    "Golden Hour Preview",
                    "SkyLyrics Studio"
            ));
            PLAYER.play();
            MEDIA_NOTICE = "Cinematic preview playing. Use the Music Center buttons to stop it.";
            LOGGER.info("Started built-in SkyLyrics cinematic preview.");
        } catch (IOException exception) {
            demoActive = false;
            MEDIA_NOTICE = "Could not start the built-in preview: " + exception.getMessage();
            LOGGER.warn("Could not start SkyLyrics demo preview.", exception);
        }
    }

    public static void stopDemo() {
        if (!demoActive) return;
        demoActive = false;
        PLAYER.clear();
        activeMediaIdentity = "";
        lastExternalTimestamp = 0L;
        MEDIA_NOTICE = "Preview stopped.";
    }

    public static boolean isDemoActive() {
        return demoActive;
    }

    private static void syncLocalAudioClock() {
        if (AUDIO.hasFile() && PLAYER.hasTrack() && !demoActive) {
            PLAYER.syncExternal(AUDIO.positionMs(), AUDIO.durationMs(), AUDIO.isPlaying());
        }
    }

    public static boolean importAndPlayAudio(Path source) {
        try {
            Path imported = MUSIC_LIBRARY.importAudio(source);
            playLocalTrack(imported);
            return true;
        } catch (IOException exception) {
            MEDIA_NOTICE = exception.getMessage();
            LOGGER.warn("Could not import audio file: {}", exception.getMessage());
            return false;
        }
    }

    public static boolean importLyricsFile(Path source) {
        try {
            Path imported = MUSIC_LIBRARY.importLyrics(source);
            boolean loaded = loadTrack(imported);
            MEDIA_NOTICE = loaded ? "Offline lyrics imported." : MEDIA_NOTICE;
            return loaded;
        } catch (IOException exception) {
            MEDIA_NOTICE = exception.getMessage();
            LOGGER.warn("Could not import LRC file: {}", exception.getMessage());
            return false;
        }
    }

    public static void playLocalTrack(Path file) {
        if (file == null) return;
        stopDemo();
        localAudioMode = true;
        activeMediaIdentity = "";
        CURRENT_MEDIA = null;
        AlbumArtManager.update(null);
        AUDIO.play(file);
        HISTORY.recordPlay(file.getFileName().toString(), "Local file", "Offline Library");
        lastHistoryIdentity = "";
        Path matchingLyrics = MUSIC_LIBRARY.matchingLyrics(file);
        if (matchingLyrics != null) {
            try {
                PLAYER.load(LyricsTrack.load(matchingLyrics));
                MEDIA_NOTICE = "Offline audio + matching LRC loaded.";
            } catch (IOException exception) {
                PLAYER.clear();
                MEDIA_NOTICE = "Audio loaded; matching LRC could not be read: " + exception.getMessage();
            }
        } else {
            PLAYER.clear();
            MEDIA_NOTICE = "Offline audio playing. Import a matching .lrc file to show synchronized lyrics.";
        }
    }

    public static void togglePlayback() {
        if (AUDIO.hasFile()) {
            AUDIO.toggle();
            return;
        }
        if (CURRENT_MEDIA != null && CURRENT_MEDIA.available()) {
            MEDIA_BRIDGE.enqueueControl("play_pause");
            return;
        }
        PLAYER.toggle();
    }

    public static void stopPlayback() {
        AUDIO.stop();
        if (CURRENT_MEDIA != null && CURRENT_MEDIA.available()) MEDIA_BRIDGE.enqueueControl("pause");
        if (demoActive) stopDemo();
    }

    public static void seekPlayback(long deltaMs) {
        if (AUDIO.hasFile()) AUDIO.seekBy(deltaMs);
        else if (CURRENT_MEDIA != null && CURRENT_MEDIA.available()) {
            MEDIA_BRIDGE.enqueueControl(deltaMs >= 0 ? "seek_forward" : "seek_back");
        } else PLAYER.seekBy(deltaMs);
    }

    public static void adjustPlaybackVolume(float delta) {
        if (AUDIO.hasFile()) AUDIO.adjustVolume(delta);
        else if (CURRENT_MEDIA != null && CURRENT_MEDIA.available()) {
            MEDIA_BRIDGE.enqueueControl(delta >= 0 ? "volume_up" : "volume_down");
        }
    }

    public static void nextLocalTrack() {
        if (!localAudioMode && CURRENT_MEDIA != null && CURRENT_MEDIA.available()) controlOnline("next");
        else moveLocalTrack(1);
    }
    public static void previousLocalTrack() {
        if (!localAudioMode && CURRENT_MEDIA != null && CURRENT_MEDIA.available()) controlOnline("previous");
        else moveLocalTrack(-1);
    }

    private static void moveLocalTrack(int direction) {
        var tracks = MUSIC_LIBRARY.listAudio();
        if (tracks.isEmpty()) {
            MEDIA_NOTICE = "Offline library is empty. Import a song first.";
            return;
        }
        Path current = AUDIO.currentFile();
        int index = current == null ? -1 : tracks.indexOf(current);
        int next = index < 0 ? (direction > 0 ? 0 : tracks.size() - 1)
                : Math.floorMod(index + direction, tracks.size());
        playLocalTrack(tracks.get(next));
    }

    public static void controlOnline(String command) {
        if (!MEDIA_BRIDGE.enqueueControl(command)) {
            MEDIA_NOTICE = "Open YouTube Music or Spotify in the browser and connect the SkyLyrics extension first.";
        }
    }

    public static void openOnlineProvider(String provider) {
        localAudioMode = false;
        AUDIO.clear();
        PLAYER.clear();
        CURRENT_MEDIA = null;
        activeMediaIdentity = "";
        lookupGeneration++;
        lookupRunning = false;
        String url = switch (provider) {
            case "spotify" -> "https://open.spotify.com/";
            default -> "https://music.youtube.com/";
        };
        try {
            if (!Desktop.isDesktopSupported()) throw new IOException("Desktop browser launching is unavailable.");
            Desktop.getDesktop().browse(URI.create(url));
            MEDIA_NOTICE = "Opened " + ("spotify".equals(provider) ? "Spotify" : "YouTube Music") + " in your browser.";
        } catch (Exception exception) {
            MEDIA_NOTICE = "Could not open browser: " + exception.getMessage();
        }
    }

    public static void openMusicFolder() {
        try {
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(MUSIC_LIBRARY.musicDirectory().toFile());
        } catch (Exception exception) {
            MEDIA_NOTICE = "Music folder: " + MUSIC_LIBRARY.musicDirectory();
        }
    }

    public static boolean loadTrack(Path path) {
        try {
            MediaStatus media = CURRENT_MEDIA;
            if (media != null && media.isSpotify()) {
                MEDIA_NOTICE = "Spotify metadata only: lyric synchronization is disabled.";
                return false;
            }
            lookupGeneration++;
            lookupRunning = false;
            demoActive = false;
            if (media != null) activeMediaIdentity = media.identity();
            PLAYER.load(LyricsTrack.load(path));
            if (media != null && media.isYouTubeSource()) {
                PLAYER.syncExternal(media.positionMs(), media.durationMs(), media.playing());
                lastExternalTimestamp = media.updatedAtEpochMs();
            }
            MEDIA_NOTICE = "Local LRC loaded.";
            return true;
        } catch (IOException ex) {
            LOGGER.warn("Could not load LRC file {}: {}", path, ex.getMessage());
            Minecraft client = Minecraft.getInstance();
            if (client.player != null) {
                client.player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "[SkyLyrics] " + ex.getMessage()));
            }
            return false;
        }
    }
}
